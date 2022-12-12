package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.vladmihalcea.hibernate.type.range.Range;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.VersatoreFactory;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDetail;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QDoc;
import it.bologna.ausl.model.entities.versatore.QSessioneVersamento;
import it.bologna.ausl.model.entities.versatore.QVersamento;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento.StatoSessioneVersamento;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento.TipologiaVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.model.entities.versatore.Versamento.StatoVersamento;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;

/**
 *
 * @author gdm
 * 
 * Job che effettua tutti i versamenti dell'azienda
 * I parametri per effettuare il versamento sono definiti nella classe VersatoreJobWorkerData
 * reperisce i documenti da versare da tutti gli archivi da varsare, i docs stessi da versare e i versamenti in errore ritentabile
 */
@MasterjobsWorker
public class VersatoreJobWorker extends JobWorker<VersatoreJobWorkerData> {
    private static final Logger log = LoggerFactory.getLogger(VersatoreJobWorker.class);
    
    @Autowired
    private VersatoreFactory versatoreFactory;
    
    private ZonedDateTime startSessioneVersamento;
    
    /* mappa che ha come chiave l'idArchivio e
     * come valore una mappa che contiene tutti i docs con il loro rispettivo VersamentoDocInformation.
     * Serve per poter calcolare lo statoUltimoVersamento del fascicolo, 
     * dopo che sono stati effettuati tutti i versamenti dei suoi docs
    */
    private Map<Integer, Map<Integer, VersamentoDocInformation>> archiviDocs;
    
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in doWork()");
        startSessioneVersamento = ZonedDateTime.now();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        
        TipologiaVersamento tipologiaVersamento = getWorkerData().getForzatura()? TipologiaVersamento.FORZATURA: TipologiaVersamento.GIORNALIERO;
        
        Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare = new HashMap<>();
        versamentiDaEffettuare = addDocsDaVersareDaArchivi(versamentiDaEffettuare, tipologiaVersamento, queryFactory);
        versamentiDaEffettuare = addDocsDaVersareDaDocs(versamentiDaEffettuare, tipologiaVersamento, queryFactory);
        versamentiDaEffettuare = addDocsDaRitentare(versamentiDaEffettuare, tipologiaVersamento, queryFactory);
        
        SessioneVersamento sessioneInCorso = getSessioneInCorso(queryFactory);
        if (versamentiDaEffettuare != null && !versamentiDaEffettuare.isEmpty()) {
        
            // se c'è una sessione in corso, mi devo attaccare a quella, altrimenti ne apro una nuova
            SessioneVersamento sessioneVersamento;
            if (sessioneInCorso != null) {
                sessioneVersamento = sessioneInCorso;
                versamentiDaEffettuare = removeDocGiaVersati(versamentiDaEffettuare, sessioneInCorso.getId(), queryFactory);
            } else {
                sessioneVersamento = openNewSessioneVersamento(tipologiaVersamento);
            }
            
            List<VersatoreDocThread> versatoreDocThreads = buildVersatoreDocThreadsList(versamentiDaEffettuare, sessioneVersamento);
            StatoSessioneVersamento statoSessioneVersamento = executeAllVersatoreDocThreads(versatoreDocThreads);

            updateStatoVersamentoFascicoli(queryFactory);

            closeSessioneVersamento(statoSessioneVersamento, sessioneVersamento);
        } else {
            // se c'è una sessione in corso, ma non ci sono versamenti (caso molto strano) chiudo semplicemente la sessione
            if (sessioneInCorso != null) {
                closeSessioneVersamento(StatoSessioneVersamento.DONE, sessioneInCorso);
            }
        }
        
        return null;
    }
    
    /**
     * torna un'eventuale sessione non ancora terminata (ad esempio se è andato giù internauta durante il versamento)
     * @param queryFactory
     * @return la sessione in corso, se c'è, altrimenti null
     */
    private SessioneVersamento getSessioneInCorso(JPAQueryFactory queryFactory) {
        return transactionTemplate.execute(a -> {
            return queryFactory
                .select(QSessioneVersamento.sessioneVersamento)
                .from(QSessioneVersamento.sessioneVersamento)
                .where(
                    QSessioneVersamento.sessioneVersamento.idAzienda.id.eq(getWorkerData().getIdAzienda()).and(
                    QSessioneVersamento.sessioneVersamento.stato.eq(StatoSessioneVersamento.RUNNING.toString())).and(
                    QSessioneVersamento.sessioneVersamento.azione.eq(getWorkerData().getAzioneVersamento().toString()))
                )
                .fetchOne();
            }
        );
    }
    
    /**
     * Costruisce e salva su DB una nuova sessione di versamento della tipologia passata
     * @param tipologiaVersamento la tipologia del versamento
     * @return l'entità SessioneVersamento salvata
     */
    private SessioneVersamento openNewSessioneVersamento(TipologiaVersamento tipologiaVersamento) {
        SessioneVersamento sessioneVersamento = transactionTemplate.execute(a -> {
            SessioneVersamento newSessioneVersamento = buildSessioneVersamento(tipologiaVersamento);
                entityManager.persist(newSessioneVersamento);
                return newSessioneVersamento;
            });
        return sessioneVersamento;
    }
    
    /**
     * Chiude la sessione di versamento aggiornando lo stato e il time_interval settandoci la data di fine
     * @param statoSessioneVersamento lo stato da settare sulla sessione versamento
     * @param sessioneVersamento la sessione da chiudere
     */
    private void closeSessioneVersamento(StatoSessioneVersamento statoSessioneVersamento, SessioneVersamento sessioneVersamento) {
        sessioneVersamento.setStato(statoSessioneVersamento);
        sessioneVersamento.setTimeInterval(Range.open(sessioneVersamento.getTimeInterval().lower(), ZonedDateTime.now()));
        transactionTemplate.executeWithoutResult(a -> entityManager.persist(statoSessioneVersamento));
    }
    
    /**
     * Rimuove, dalla mappa dei doc da versare, quelli già versati nella sessione.
     * Se un doc ha più versamenti (es. è in 2 fascicoli che vengono chiusi), allora si controlla ognuno e nel caso si tiene
     * sono quello non fatto
     * @param versamentiDaEffettuare i doc da versare estrapolati con le query
     * @param idSessioneVersamento l'id della sessione in corso
     * @param queryFactory
     * @return la mappa dei versamenti da effettuare, alla quale sono stati rimossi quelli già versati (è la stessa mappa  che viene passata come paraemtro)
     */
    private Map<Integer, List<VersamentoDocInformation>> removeDocGiaVersati(Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare, Integer idSessioneVersamento, JPAQueryFactory queryFactory) {
        
        // prendo i versamenti effettuati nella sessione
        List<Tuple> versamentiEffettuati = transactionTemplate.execute(a -> {
            return queryFactory
                    .select(QVersamento.versamento.idDoc.id, QVersamento.versamento.idArchivio.id)
                    .from(QVersamento.versamento)
                    .where(QVersamento.versamento.idSessioneVersamento.id.eq(idSessioneVersamento))
                    .fetch();
        });
        
        // se ne trovo qualcuno lo devo rimuovere da quelli da effettuare
        if (versamentiEffettuati != null && !versamentiEffettuati.isEmpty()) {
            
            // per ogni versamento trovato, devo controllare se è presente in quelli da effettuare e nel caso rimuoverlo
            for (Tuple versamentoEffettuato : versamentiEffettuati) {
                Integer idDocVersamentoEffettuato = versamentoEffettuato.get(QVersamento.versamento.idDoc.id);
                Integer idArchivioVersamentoEffettuato = versamentoEffettuato.get(QVersamento.versamento.idArchivio.id);
                // per prima cosa estraggo dalla mappa i versamenti del doc del versamento effettuati
                List<VersamentoDocInformation> versamentiDocDaEffettuare = versamentiDaEffettuare.get(idDocVersamentoEffettuato);
                // dovrei trovarne almeno uno, mi aspetto di entrare sempre in questo if
                if (versamentiDocDaEffettuare != null) {
                    // rimuovo dalla lista dei versamenti del doc, quelli già fatti
                    List<VersamentoDocInformation> versamentiDocDaEffettuareFiltrati = 
                            // quelli già fatti sono quelli che hanno lo stesso idDoc e lo stesso idArchivio
                            // siccome l'idArchivio può non esserci, devo controllare i null
                            versamentiDocDaEffettuare.stream().filter(versamentoDocDaEffettuare -> 
                                !versamentoDocDaEffettuare.getIdDoc().equals(idDocVersamentoEffettuato) ||
                                versamentoDocDaEffettuare.getIdArchivio() == null && idArchivioVersamentoEffettuato != null ||
                                versamentoDocDaEffettuare.getIdArchivio() != null && idArchivioVersamentoEffettuato == null || (
                                    versamentoDocDaEffettuare.getIdArchivio() != null && idArchivioVersamentoEffettuato != null && 
                                    !versamentoDocDaEffettuare.getIdArchivio().equals(idArchivioVersamentoEffettuato)
                                )    
                    ).collect(Collectors.toList());
                    // se ho rimosso tutti i versamento per il doc, rimuovo completamente il doc dalla mappa
                    if (versamentiDocDaEffettuareFiltrati == null || versamentiDocDaEffettuareFiltrati.isEmpty()) {
                        versamentiDaEffettuare.remove(idDocVersamentoEffettuato);
                    } else {
                        versamentiDaEffettuare.put(idDocVersamentoEffettuato, versamentiDocDaEffettuareFiltrati);
                    }
                }
            }
        }
        return versamentiDaEffettuare;
    }
    
    /**
     * Lancia i threads per il versamento e attende che tutti abbiamo finito
     * NB: il massimo di threads contemporanei è indicato del parametro "poolSize" del job
     * @param versatoreDocThreads
     * @return torna lo stato finale della sessione, dopo che tutti i threads hanno terminato di versare
     * @throws MasterjobsWorkerException 
     */
    private StatoSessioneVersamento executeAllVersatoreDocThreads(List<VersatoreDocThread> versatoreDocThreads) throws MasterjobsWorkerException {
        Integer poolSize = getWorkerData().getPoolSize();
        ExecutorService executoreService = Executors.newFixedThreadPool(poolSize);
        
        List<VersamentoDocInformation> allResults = new ArrayList<>();
        try {
            // fa partire tutti i threads (massimo poolSize in contemporanea) e attende il risultato
            List<Future<List<VersamentoDocInformation>>> threadsResult = executoreService.invokeAll(versatoreDocThreads);
            
            // tutti i threads hanno finito, ciclo tutti i risultati
            for (Future<List<VersamentoDocInformation>> threadResult : threadsResult) {
                // la lista conterrà i versamenti effettuati per il doc in questa sessione (per infocert sarà solo 1)
                List<VersamentoDocInformation> versamentiThread = threadResult.get();
                for (VersamentoDocInformation versamentoThread : versamentiThread) {
                    // aggiungo il versamento nella lista di tutti i versamenti effettuati nella sessione
                    allResults.add(versamentoThread);
                    /* se il versamento è associato a un archivio, popolo la mappa degli archiviDocs
                     * la chiave è l'idArchivio
                     * il valore è la mappa con tutti i docs:
                     * - se è il primo doc per quell'archivio la mappa sarà vuota per cui la creo
                     * - poi ci metto dentro il doc corrispondente
                    */
                    if (versamentoThread.getIdArchivio() != null) {
                        // estraggo la mappa dei doc per questo archivio
                        Map<Integer, VersamentoDocInformation> docsArchivio = archiviDocs.get(versamentoThread.getIdArchivio());
                        // se è null è il primo doc dell'archivio, creo la mappa e la inserisco nella mappa superiore
                        if (docsArchivio == null) {
                            docsArchivio = new HashMap<>();
                            archiviDocs.put(versamentoThread.getIdArchivio(), docsArchivio);
                        }
                        // inserisco il doc nella mappa
                        docsArchivio.put(versamentoThread.getIdDoc(), versamentoThread);
                    }
                }
            }
            // calcolo lo stato della sessione, basandomi sugli stati dei versamenti effettuati
            StatoSessioneVersamento statoSessioneVersamento = getStatoSessioneVersamentoFromVersamenti(allResults);
            return statoSessioneVersamento;
        } catch (InterruptedException ex) {
            String errorMessage = "ricevuto un InterruptedException errore nell'attesa del completamento dei thread di versamento";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        } catch (Throwable ex) {
            String errorMessage = "errore nell'attesa del completamento dei thread di versamento";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
    }
    
    /**
     * Costruisce i threads capaci di effettuare i versamenti
     * @param versamentiDaEffettuare mappa contenente tutti i doc da versare
     * @param sessioneVersamento la sessione a cui attaccare i versamenti
     * @return la lista dei threads costruiti
     */
    private List<VersatoreDocThread> buildVersatoreDocThreadsList(Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare, SessioneVersamento sessioneVersamento) {
        VersatoreDocs versatoreDocsInstance = versatoreFactory.getVersatoreDocsInstance(getWorkerData().getHostId());
        
        Persona personaForzatura = getPersonaForzatura();
        
        List<VersatoreDocThread> versatoreDocThreads = new ArrayList<>();
        
        /* cicla sulla mappa dei doc da versare e per ognuno costruisce un thread.
         * Nella mappa, per ogni documento c'è la lista dei suoi versamenti.
         *  è una lista perché un documento può essere versato più volte, ad esempio se si stanno versando 2 fascicoli
         *  in cui lo stesso doc è presente. In questo caso i versamenti li effettuerà lo stesso thread
        */
        for (Integer idDocVersamentoDaEffettuare : versamentiDaEffettuare.keySet()) {
            List<VersamentoDocInformation> versamentoDaEffettuare = versamentiDaEffettuare.get(idDocVersamentoDaEffettuare);
            
            VersatoreDocThread versatoreDocThread = new VersatoreDocThread(
                    versamentoDaEffettuare,
                    sessioneVersamento,
                    personaForzatura,
                    versatoreDocsInstance,
                    transactionTemplate,
                    entityManager);
            versatoreDocThreads.add(versatoreDocThread);
        }
        return versatoreDocThreads;
    }
    
    /**
     * Reperisce l'entità Persona che forza il versamento
     * @return l'entità Persona che forza il versamento, oppure null se non è stata passata dei dati del job
     */
    private Persona getPersonaForzatura() {
        Persona personaForzatura = null;
        if (getWorkerData().getIdPersonaForzatura() != null) {
            personaForzatura = transactionTemplate.execute(a -> {
                return entityManager.find(Persona.class, getWorkerData().getIdPersonaForzatura());
            });
        }
        return personaForzatura;
    }
    
    /**
     * Per ogni fascicolo versato, fa un update con commit dello statoUltimoVersamento, calcolandolo dagli stati dei doc versati
     * Viene anche settato a null lo statoVersamento, che sta ad indicare lo stato del prossimo versamento:
     *  dato che il versamento è finito lo stato del prossimo non c'è
     * @param queryFactory 
     */
    private void updateStatoVersamentoFascicoli(JPAQueryFactory queryFactory) {
        for (Integer idArchivio : archiviDocs.keySet()) {
            Map<Integer, VersamentoDocInformation> versamentoDocs = archiviDocs.get(idArchivio);
            StatoVersamento statoVersamentoFascicolo = getStatoVersamentoFascicoloFromStatoVersamentiDocs(versamentoDocs);
            transactionTemplate.executeWithoutResult(a -> {
                queryFactory
                    .update(QArchivio.archivio)
                    .setNull(QArchivio.archivio.statoVersamento)
                    .where(QArchivio.archivio.id.eq(idArchivio))
                    .execute();
                queryFactory
                    .update(QArchivioDetail.archivioDetail)
                    .set(QArchivioDetail.archivioDetail.statoUltimoVersamento, statoVersamentoFascicolo.toString())
                    .set(QArchivioDetail.archivioDetail.dataUltimoVersamento, startSessioneVersamento)
                    .where(QArchivioDetail.archivioDetail.id.eq(idArchivio))
                    .execute();
            });
        }
    }
    
    /**
     * Calcola lo stato del versamento del fascicolo in base agli stati dei versamenti dei documenti al suo interno
     * @param versamentoDocs la mappa dei soli doc all'interno del fascicolo
     * @return lo stato del versamento del fascicolo
     */
    private StatoVersamento getStatoVersamentoFascicoloFromStatoVersamentiDocs(Map<Integer, VersamentoDocInformation> versamentoDocs) {
        StatoVersamento res = null;
        for (Integer idDoc : versamentoDocs.keySet()) {
            VersamentoDocInformation versamentoDoc = versamentoDocs.get(idDoc);
            if (res == null) {
                res = versamentoDoc.getStatoVersamento();
            } else if (versamentoDoc.getStatoVersamento() == StatoVersamento.ERRORE_RITENTABILE) {
                res = StatoVersamento.ERRORE_RITENTABILE;
            } else if (res == StatoVersamento.IN_CARICO) {
                if (versamentoDoc.getStatoVersamento() == StatoVersamento.ERRORE) {
                    res = Versamento.StatoVersamento.IN_CARICO_CON_ERRORI;
                }
            } else if (versamentoDoc.getStatoVersamento() == StatoVersamento.ERRORE || versamentoDoc.getStatoVersamento() == StatoVersamento.ERRORE_FORZABILE) {
                res = versamentoDoc.getStatoVersamento();
            }
        }
        
        return res;
    }
    
    /**
     * Calcola lo stato della sessione in base agli stati dei versamenti effettuati
     * La sessionesarà completa se tutti i versamenti sono andati a buon fine (o sono stati presi in carico), negli altri casi
     * sarà parziale
     * @param versamenti
     * @return lo stato della sessione in base agli stati dei versamenti effettuati
     */
    private StatoSessioneVersamento getStatoSessioneVersamentoFromVersamenti(List<VersamentoDocInformation> versamenti) {
        StatoSessioneVersamento res;
        boolean allVersatoOrInCarico = !versamenti.stream().anyMatch(p -> 
                p.getStatoVersamento() == Versamento.StatoVersamento.PARZIALE ||
                        p.getStatoVersamento() == Versamento.StatoVersamento.AGGIORNARE ||
                        p.getStatoVersamento() == Versamento.StatoVersamento.VERSARE ||
                        p.getStatoVersamento() == Versamento.StatoVersamento.ERRORE ||
                        p.getStatoVersamento() == Versamento.StatoVersamento.IN_CARICO_CON_ERRORI ||
                        p.getStatoVersamento() == Versamento.StatoVersamento.ERRORE_RITENTABILE ||
                        p.getStatoVersamento() == Versamento.StatoVersamento.ERRORE_FORZABILE);
        
        if (allVersatoOrInCarico) {
            res = StatoSessioneVersamento.DONE;
        } else {
            res = StatoSessioneVersamento.PARTIALLY;
        }
        return res;
    }
    
    /**
     * Costruisce l'entità SessioneVersamento della tipologia passata
     * @param tipologiaVersamento la tipologia della SessioneVersamento da costruire
     * @return l'entità SessioneVersamento
     */
    private SessioneVersamento buildSessioneVersamento(TipologiaVersamento tipologiaVersamento) {
        Azienda azienda = entityManager.find(Azienda.class, getWorkerData().getIdAzienda());
        SessioneVersamento sessioneVersamento = new SessioneVersamento();
        sessioneVersamento.setStato(StatoSessioneVersamento.RUNNING);
        sessioneVersamento.setTipologia(tipologiaVersamento);
        sessioneVersamento.setAzione(getWorkerData().getAzioneVersamento());
        sessioneVersamento.setIdAzienda(azienda);
        Range<ZonedDateTime> timeInterval = Range.openInfinite(startSessioneVersamento);
        sessioneVersamento.setTimeInterval(timeInterval);
        return sessioneVersamento;
    }
    
    /**
     * Prende tutti i docs che nella sessione passata sono andati in stato ERRORE_RITENTABILE e li aggiunge alla mappa
     * versamentiDaEffettuare passata in input
     * Vengono reperiti basandosi sui versamenti precedenti, che sono in ERRORE_RITENTABILE.
     * Viene anche settato il versamento precedente, sul nuovo versamento da effettuare
     * NB: Vengono presi i versamenti solo dell'azienda a cui il job fa riferimento
     * @param versamentiDaEffettuare la mappa che contiene i docs da versare
     * @param tipologiaVersamento la tipologia del versamento che si vuole effettuare
     * @param queryFactory
     * @return la mappa dei versamentiDaEffettuare (è la stessa passata in input)
     */
    private Map<Integer, List<VersamentoDocInformation>> addDocsDaRitentare(Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare, TipologiaVersamento tipologiaVersamento, JPAQueryFactory queryFactory) {
    
    /*
     * Prende tutti i versamenti in stato ERRORE_RITENTABILE delle sessioni dell'azienda del job
     * Per ogni versamento ne estrae l'id, l'id del doc e l'id del fascicolo  
    */    
    List<Tuple> versamentiRitentabili = queryFactory
        .select(QVersamento.versamento.id, QVersamento.versamento.idDoc.id, QVersamento.versamento.idArchivio.id)
        .from(QVersamento.versamento)
        .join(QSessioneVersamento.sessioneVersamento).on(
                QVersamento.versamento.idSessioneVersamento.id.eq(QSessioneVersamento.sessioneVersamento.id)
        )
        .where(
                QVersamento.versamento.stato.eq(Versamento.StatoVersamento.ERRORE_RITENTABILE.toString()).and(
                QVersamento.versamento.ignora.eq(false)).and(
                QSessioneVersamento.sessioneVersamento.idAzienda.id.eq(getWorkerData().getIdAzienda()))
        ).fetch();
        for (Tuple versamentoRitentabile : versamentiRitentabili) {
            /*
             * per ognuno dei versamenti estratti, controllo se il doc era già presente nei versamenti da effettuare
             *  per lo stesso fasciolo.
             *  Se lo trovo allora non lo inserisco nuovamente, ma setto il versamento estratto come VersamentoPrecedente
             *   In questo modo, così da evere l'informazione per attaccare il nuovo versamento al precedente e settare il
             *   precedente da ignorare
             *  Se non lo trovo, allora lo aggiungo, settandoci sempre il VersamentoPrecedente
            */
            Integer idVersamento = versamentoRitentabile.get(QVersamento.versamento.id);
            Integer idDoc = versamentoRitentabile.get(QVersamento.versamento.idDoc.id);
            Integer idArchivio = versamentoRitentabile.get(QVersamento.versamento.idArchivio.id);
            List<VersamentoDocInformation> versamenti = versamentiDaEffettuare.get(idDoc);
            boolean versamentoDaInserire = true;
            if (versamenti == null) { // se non trovo versamenti per il doc, creo la lista vuota
                versamenti = new ArrayList<>();
                versamentiDaEffettuare.put(idDoc, versamenti);
            } else {
                /*
                 * se trovo dei versamenti, controllo se c'è il versamento con lo stesso fascicolo
                 * se lo trovo, setto solo il versamento precedente, altrimenti setto che è da inserire
                */
                for (VersamentoDocInformation v : versamenti) {
                    if ((idArchivio == null && v.getIdArchivio() == null) || 
                        (idArchivio != null && v.getIdArchivio() != null && v.getIdArchivio().equals(idArchivio))) {
                        v.setVersamentoPrecedente(idVersamento);
                        versamentoDaInserire = false;
                        break;
                    }
                }
            }
            
            // se il versamento è da inserire lo inserisco nella lista
            if (versamentoDaInserire) {
                VersamentoDocInformation versamentoDocInformation = buildVersamentoDocInformation(idDoc, idArchivio, tipologiaVersamento);
                versamentoDocInformation.setVersamentoPrecedente(idVersamento);
                versamenti.add(versamentoDocInformation);
            }
        }
        
        return versamentiDaEffettuare;
    }
    
    /**
     * aggiunge alla mappa dei versamenti da effettuare i doc da versare:
     *  i doc da versare sono quelli in stato VERSARE o AGGIORNARE.
     *  Prende solo i doc dell'azienda a cui il job fa riferimento
     * @param versamentiDaEffettuare la mappa dei versamenti da effettuare
     * @param tipologiaVersamento la tipologia del versamento
     * @param queryFactory
     * @return la mappa dei versamentiDaEffettuare (è la stessa passata in input)
     */
    private Map<Integer, List<VersamentoDocInformation>> addDocsDaVersareDaDocs(Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare, TipologiaVersamento tipologiaVersamento, JPAQueryFactory queryFactory) {
        QDoc qDoc = QDoc.doc;
        BooleanExpression filter = 
            qDoc.statoVersamento.isNotNull().and(
            qDoc.statoVersamento.in(
                    Arrays.asList(
                            Versamento.StatoVersamento.VERSARE.toString(), 
                            Versamento.StatoVersamento.AGGIORNARE.toString()))
            ).and(
                qDoc.idAzienda.id.eq(getWorkerData().getIdAzienda())
            );
        List<Integer> docsIdDaVersare = queryFactory.select(qDoc.id).from(qDoc).where(filter).fetch();
        
        for (Integer docIdDaVersare : docsIdDaVersare) {
            VersamentoDocInformation versamentoDocInformation = buildVersamentoDocInformation(docIdDaVersare, null, tipologiaVersamento);
            List<VersamentoDocInformation> versamenti = versamentiDaEffettuare.get(docIdDaVersare);
            if (versamenti == null) {
                versamenti = new ArrayList<>();
                versamentiDaEffettuare.put(docIdDaVersare, versamenti);
            }
            versamenti.add(versamentoDocInformation);
        }
        return versamentiDaEffettuare;
    }
    
    /**
     * aggiunge alla mappa dei versamenti da effettuare i doc all'interno dei fascicoli da versare:
     *  Verranno aggiunti tutti i doc all'interno dei fascicolo da versare, indipendentemente dal loro sato versamento.
     *  I fascicolo da versare sono tutti i fascicoli in stato VERSARE o AGGIORNARE
     *  Prende solo i doc dell'azienda a cui il fascicolo fa riferimento
     * @param versamentiDaEffettuare la mappa dei versamentiDaEffettuare
     * @param tipologiaVersamento la tipologia del versamento
     * @param queryFactory
     * @return la mappa dei versamentiDaEffettuare (è la stessa passata in input)
     */
    private Map<Integer, List<VersamentoDocInformation>> addDocsDaVersareDaArchivi(Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare, TipologiaVersamento tipologiaVersamento, JPAQueryFactory queryFactory) {
        QArchivio qArchivio = QArchivio.archivio;
        
        // filtro per estrarre i fascicoli
        BooleanExpression filterArchiviDaVersare = 
            qArchivio.statoVersamento.isNotNull().and(
            qArchivio.statoVersamento.in(
                    Arrays.asList(
                            Versamento.StatoVersamento.VERSARE.toString(), 
                            Versamento.StatoVersamento.AGGIORNARE.toString()))
            ).and(
                qArchivio.idAzienda.id.eq(getWorkerData().getIdAzienda())
            );
        
        // estrae prima tutti i fascicoli stato da versare dell'azienda indicata nel job
        List<Integer> archiviIdDaVersare = queryFactory.select(qArchivio.id).from(qArchivio).where(filterArchiviDaVersare).fetch();
        
        // per ogno fascicolo estrae i doc al suo interno e inserisce il versamento nella mappa, indipendentemente dallo stato del doc
        for (Integer idArchivioDaVersare : archiviIdDaVersare) {
            QArchivioDoc qArchivioDoc = QArchivioDoc.archivioDoc;
            List<Integer> docsIdDaVersare = queryFactory
                .select(qArchivioDoc.idDoc.id)
                .from(qArchivioDoc)
                .where(qArchivioDoc.idArchivio.id.eq(idArchivioDaVersare)).fetch();
            for (Integer docIdDaVersare : docsIdDaVersare) {
                VersamentoDocInformation versamentoDocInformation = buildVersamentoDocInformation(docIdDaVersare, idArchivioDaVersare, tipologiaVersamento);
                List<VersamentoDocInformation> versamentiDoc = versamentiDaEffettuare.get(docIdDaVersare);
                if (versamentiDoc == null) {
                    versamentiDoc = new ArrayList<>();
                    versamentiDaEffettuare.put(docIdDaVersare, versamentiDoc);
                }
                versamentiDoc.add(versamentoDocInformation);
            }
        }
        
        return versamentiDaEffettuare;
    }
    
    /**
     * Crea l'oggetto VersamentoDocInformation, necessario al versatore per poter effettuare il versamento
     * E' possibile passare un archivio per poter legare il doc a quell'archivio. 
     *  Sarà poi il plugin del versatore a decidere come trattare l'informazione.
     * @param idDoc il doc da versare
     * @param idArchivio l'archivio a cui associare il doc (possibile anche null)
     * @param tipologiaVersamento la tipologia del versamento da effettuare
     * @return l'oggetto VersamentoDocInformation
     */
    private VersamentoDocInformation buildVersamentoDocInformation(Integer idDoc, Integer idArchivio, TipologiaVersamento tipologiaVersamento) {
        VersamentoDocInformation versamentoDocInformation = new VersamentoDocInformation();
        versamentoDocInformation.setTipologiaVersamento(tipologiaVersamento);
        versamentoDocInformation.setIdDoc(idDoc);
        versamentoDocInformation.setIdArchivio(idArchivio);
        return versamentoDocInformation;
    }
}
