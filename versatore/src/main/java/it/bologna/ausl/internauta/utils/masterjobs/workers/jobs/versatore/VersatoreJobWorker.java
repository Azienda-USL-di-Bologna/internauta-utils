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
import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.VersatoreFactory;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDetail;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QDoc;
import it.bologna.ausl.model.entities.scripta.QDocDetail;
import it.bologna.ausl.model.entities.versatore.QSessioneVersamento;
import it.bologna.ausl.model.entities.versatore.QVersamento;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento.StatoSessioneVersamento;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento.TipologiaVersamento;
import static it.bologna.ausl.model.entities.versatore.SessioneVersamento.TipologiaVersamento.FORZATURA;
import static it.bologna.ausl.model.entities.versatore.SessioneVersamento.TipologiaVersamento.RITENTA;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.model.entities.versatore.Versamento.StatoVersamento;
import static it.bologna.ausl.model.entities.versatore.Versamento.StatoVersamento.ERRORE;
import static it.bologna.ausl.model.entities.versatore.Versamento.StatoVersamento.ERRORE_RITENTABILE;
import static it.bologna.ausl.model.entities.versatore.Versamento.StatoVersamento.IN_CARICO;
import static it.bologna.ausl.model.entities.versatore.Versamento.StatoVersamento.IN_CARICO_CON_ERRORI;
import static it.bologna.ausl.model.entities.versatore.Versamento.StatoVersamento.VERSATO;
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
import org.springframework.data.util.Pair;
import org.springframework.transaction.TransactionDefinition;

/**
 *
 * @author gdm
 * 
 * Job che effettua tutti i versamenti dell'azienda
 * I parametri per effettuare il versamento sono definiti nella classe VersatoreJobWorkerData
 * reperisce i docs da versare dagli archivi, e dai docs stessi
 * inotrle reperisce anche i versamenti non ancora competati o in errore ritentabile
 */
@MasterjobsWorker
public class VersatoreJobWorker extends JobWorker<VersatoreJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(VersatoreJobWorker.class);
    
    @Autowired
    private VersatoreFactory versatoreFactory;
    
    private ZonedDateTime startSessioneVersamento;
    
    /* 
    mappa che ha come chiave l'idArchivio e
    come valore una mappa che contiene tutti i docs con il loro rispettivo VersamentoDocInformation.
    Serve per poter calcolare lo statoUltimoVersamento del fascicolo, 
    dopo che sono stati effettuati tutti i versamenti dei suoi docs
    */
    private final Map<Integer, Map<Integer, VersamentoDocInformation>> archiviDocs = new HashMap<>();
    
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in doWork()");
        startSessioneVersamento = ZonedDateTime.now();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        
        // setto il transactionTemplate in modo che tutte le volte che lo si usa apra una transazione e la committi
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        
        TipologiaVersamento tipologiaVersamento = getWorkerData().getTipologia();
                
        // calcolo tutti versamenti da effettaure per vari docs e li aggiungo alla mappa
        Map<Integer, List<VersamentoDocInformation>> versamentiDaProcessare = new HashMap<>();
        
        if (tipologiaVersamento == TipologiaVersamento.GIORNALIERO) {
            // aggiungo i docs numerati dei fascicoli da versare, cioè quelli veicolati da un fascicolo (es. fascicolo che viene chiuso)
            versamentiDaProcessare = addDocsDaProcessareDaArchivi(versamentiDaProcessare, tipologiaVersamento, queryFactory);
            // aggiungo i docs da versare, non dipendenti dal fascicolo
            versamentiDaProcessare = addDocsDaProcessareDaDocs(versamentiDaProcessare, tipologiaVersamento, queryFactory);
            // aggiungo anche i docs reperiti dai versamenti ancora non competati o che sono da ritentare
            versamentiDaProcessare = addDocsDaRitentare(versamentiDaProcessare, tipologiaVersamento, queryFactory);
        } else {
            versamentiDaProcessare = addDocsDaVersare(getWorkerData().getIdDocsDaVersare(), tipologiaVersamento, queryFactory);
        }
        
        SessioneVersamento sessioneInCorso = getSessioneInCorso(queryFactory);
        
        StatoSessioneVersamento statoSessioneVersamento = null;
        
        if (versamentiDaProcessare != null && !versamentiDaProcessare.isEmpty()) {
        
            // se c'è una sessione in corso, mi devo attaccare a quella, altrimenti ne apro una nuova
            SessioneVersamento sessioneVersamento;
            if (sessioneInCorso != null) {
                sessioneVersamento = sessioneInCorso;
                statoSessioneVersamento = removeDocGiaVersatiAndGetStatoSessioneAttuale(versamentiDaProcessare, sessioneInCorso.getId(), queryFactory);
            } else {
                sessioneVersamento = openNewSessioneVersamento(tipologiaVersamento);
            }
            
            List<VersatoreDocThread> versatoreDocThreads;
            try {
                versatoreDocThreads = buildVersatoreDocThreadsList(versamentiDaProcessare, sessioneVersamento);
            } catch (Throwable ex) {
                final String message = "errore nella creazione dei threads di versamento";
                log.error(message, ex);
                throw new MasterjobsWorkerException(message, ex);
            }
            List<VersamentoDocInformation> allResults = executeAllVersatoreDocThreads(versatoreDocThreads);

            // calcolo lo stato della sessione, basandomi sugli stati dei versamenti effettuati
            if (statoSessioneVersamento != StatoSessioneVersamento.PARTIALLY) {
                statoSessioneVersamento = getStatoSessioneVersamentoFromVersamenti(allResults);
            }
            
            //se è presente almeno un versamento legato al fascicolo ne aggiorna lo stato ultimo versamento del fascicolo
            updateStatoVersamentoFascicoli(queryFactory);

            closeSessioneVersamento(sessioneVersamento.getId(), statoSessioneVersamento, sessioneVersamento.getTimeInterval(), queryFactory);
        } else {
            // se c'è una sessione in corso, ma non ci sono versamenti (caso molto strano) chiudo semplicemente la sessione
            if (sessioneInCorso != null) {
                closeSessioneVersamento(sessioneInCorso.getId(), StatoSessioneVersamento.DONE, sessioneInCorso.getTimeInterval(), queryFactory);
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
                    QSessioneVersamento.sessioneVersamento.stato.eq(StatoSessioneVersamento.RUNNING.toString()))
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
     * @param idSessioneVersamento id della sessione da chiudere
     * @param statoSessioneVersamento lo stato da settare sulla sessione versamento
     * @param actualInterval il time_interval della sessione
     * @param queryFactory
     */
    private void closeSessioneVersamento(Integer idSessioneVersamento, StatoSessioneVersamento statoSessioneVersamento, 
            Range<ZonedDateTime> actualInterval, JPAQueryFactory queryFactory) {
        transactionTemplate.executeWithoutResult(a -> {
            Range<ZonedDateTime> newInterval = Range.open(actualInterval.lower(), ZonedDateTime.now());
            queryFactory
                .update(QSessioneVersamento.sessioneVersamento)
                .set(QSessioneVersamento.sessioneVersamento.stato, statoSessioneVersamento.toString())
                .set(QSessioneVersamento.sessioneVersamento.timeInterval, newInterval)
                .where(QSessioneVersamento.sessioneVersamento.id.eq(idSessioneVersamento))
                .execute();   
        });
    }
    
    /**
     * Rimuove, dalla mappa dei doc da versare, quelli già versati nella sessione.
     * Se un doc ha più versamenti (es. è in 2 fascicoli che vengono chiusi), allora si controlla ognuno e nel caso si tiene
     * sono quello non fatto
     * @param versamentiDaEffettuare i doc da versare estrapolati con le query
     * @param idSessioneVersamento l'id della sessione in corso
     * @param queryFactory
     * @return lo stato attuale della sessione
     */
    private StatoSessioneVersamento removeDocGiaVersatiAndGetStatoSessioneAttuale(Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare, Integer idSessioneVersamento, JPAQueryFactory queryFactory) {
        
        List<VersamentoDocInformation> versamentiPerCalcoloStatoSessione = new ArrayList<>();
        
        // prendo i versamenti effettuati nella sessione
        List<Tuple> versamentiEffettuati = transactionTemplate.execute(a -> {
            return queryFactory
                    .select(
                            QVersamento.versamento.idDoc.id, 
                            QVersamento.versamento.idArchivio.id, 
                            QVersamento.versamento.stato,
                            QVersamento.versamento.forzabile,
                            QVersamento.versamento.forzabileConcordato)
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
                StatoVersamento statoVersamentoEffettuato = StatoVersamento.valueOf(versamentoEffettuato.get(QVersamento.versamento.stato));
                Boolean forzabileVersamentoEffettuato = versamentoEffettuato.get(QVersamento.versamento.forzabile);
                // potrebbe essere comunque forzabile ma crittografico
                Boolean forzabileConcordatoVersamentoEffettuato = versamentoEffettuato.get(QVersamento.versamento.forzabileConcordato);
                
                
                /*
                per calcolare lo stato della sessione in corso, leggo tutti gli stati devi versamenti e creo un oggetto
                VersamentoDocInformation con solo lo stato, in modo da utilizzare la funzione getStatoSessioneVersamentoFromVersamenti
                per calolare lo stato attuale della sessione
                */
                VersamentoDocInformation versamentoDocInformation = new VersamentoDocInformation();
                versamentoDocInformation.setStatoVersamento(statoVersamentoEffettuato);
                versamentoDocInformation.setIdDoc(idDocVersamentoEffettuato);
                versamentoDocInformation.setIdArchivio(idArchivioVersamentoEffettuato);
                versamentoDocInformation.setForzabile(forzabileVersamentoEffettuato);
                versamentoDocInformation.setForzabileConcordato(forzabileConcordatoVersamentoEffettuato);
                versamentoDocInformation.setParams(getWorkerData().getParams());
                versamentiPerCalcoloStatoSessione.add(versamentoDocInformation);
                
                /*
                Ho bisogno anche di popolare la mappa archiviDocs, perché per poter scrivere correttamente lo stato di versamento
                del fascicolo, devo conoscere gli stati dei precedenti versamenti effettuati.
                */
                addInArchiviDocs(idArchivioVersamentoEffettuato, versamentoDocInformation);
                
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
        return getStatoSessioneVersamentoFromVersamenti(versamentiPerCalcoloStatoSessione);
    }
    
    /**
     * Aggiunge il versamentoDocInformation alla mappa archiviDocs.
     * Se idArchivio è null non fa nulla
     * @param idArchivio
     * @param versamentoDocInformation 
     */
    private void addInArchiviDocs(Integer idArchivio, VersamentoDocInformation versamentoDocInformation) {
        if (idArchivio != null) {
            // estraggo la mappa dei doc per questo archivio
            Map<Integer, VersamentoDocInformation> docsArchivio = archiviDocs.get(idArchivio);
            // se è null è il primo doc dell'archivio, creo la mappa e la inserisco nella mappa superiore
            if (docsArchivio == null) {
                docsArchivio = new HashMap<>();
                archiviDocs.put(idArchivio, docsArchivio);
            }
            // inserisco il doc nella mappa
            docsArchivio.put(versamentoDocInformation.getIdDoc(), versamentoDocInformation);
        }
    }
    
    /**
     * Lancia i threads per il versamento e attende che tutti abbiamo finito
     * NB: il massimo di threads contemporanei è indicato del parametro "poolSize" del job
     * @param versatoreDocThreads
     * @return torna lo stato finale della sessione, dopo che tutti i threads hanno terminato di versare
     * @throws MasterjobsWorkerException 
     */
    private List<VersamentoDocInformation> executeAllVersatoreDocThreads(List<VersatoreDocThread> versatoreDocThreads) throws MasterjobsWorkerException {
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
                    /* 
                    se il versamento è associato a un archivio, popolo la mappa degli archiviDocs
                    la chiave è l'idArchivio
                    il valore è la mappa con tutti i docs:
                     - se è il primo doc per quell'archivio la mappa sarà vuota per cui la creo
                     - poi ci metto dentro il doc corrispondente
                    */
                    addInArchiviDocs(versamentoThread.getIdArchivio(), versamentoThread);
                }
            }
            return allResults;
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
    private List<VersatoreDocThread> buildVersatoreDocThreadsList(Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare, SessioneVersamento sessioneVersamento) throws VersatoreProcessingException {
        VersatoreDocs versatoreDocsInstance = versatoreFactory.getVersatoreDocsInstance(getWorkerData().getHostId());
        
        Persona personaForzatura = getPersonaForzatura();
        
        List<VersatoreDocThread> versatoreDocThreads = new ArrayList<>();
        
        /*
        cicla sulla mappa dei doc da versare e per ognuno costruisce un thread.
        Nella mappa, per ogni documento c'è la lista dei suoi versamenti.
         E' una lista perché un documento può essere versato più volte, ad esempio se si stanno versando 2 fascicoli
         in cui lo stesso doc è presente. In questo caso i versamenti li effettuerà lo stesso thread
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
        if (archiviDocs != null && !archiviDocs.isEmpty()) {
            for (Integer idArchivio : archiviDocs.keySet()) {
                Map<Integer, VersamentoDocInformation> versamentoDocs = archiviDocs.get(idArchivio);
                Pair<StatoVersamento, Boolean> statoVersamentoFascicoloAndForzatura = getStatoVersamentoFascicoloAndForzaturaFromStatoVersamentiDocs(versamentoDocs);
                StatoVersamento statoVersamentoFascicolo = statoVersamentoFascicoloAndForzatura.getFirst();
                Boolean forzatura = statoVersamentoFascicoloAndForzatura.getSecond();
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
                        .set(QArchivioDetail.archivioDetail.versamentoForzabile, forzatura)
                        .where(QArchivioDetail.archivioDetail.id.eq(idArchivio))
                        .execute();
                });
            }
        }
    }
    
    /**
     * Calcola lo stato del versamento del fascicolo e la forzatura in base agli stati dei versamenti dei documenti al suo interno
     * @param versamentoDocs la mappa dei soli doc all'interno del fascicolo
     * @return una coppia (Pair) che contiene come primo elemento lo stato del versamento del fascicolo e come secondo 
     *  la forzatura (cioè se il documento ha un versamento forzabile)
     */
    private Pair<StatoVersamento, Boolean> getStatoVersamentoFascicoloAndForzaturaFromStatoVersamentiDocs(Map<Integer, VersamentoDocInformation> versamentoDocs) {
        StatoVersamento statoVersamentoFascicolo = null;
        boolean versamentoForzabile = false;
        for (Integer idDoc : versamentoDocs.keySet()) {
            VersamentoDocInformation versamentoDoc = versamentoDocs.get(idDoc);
            versamentoForzabile = versamentoForzabile || versamentoDoc.getForzabile() || versamentoDoc.getForzabileConcordato();
            
            if (statoVersamentoFascicolo == null) {
                statoVersamentoFascicolo = versamentoDoc.getStatoVersamento();
            } else {
                switch (versamentoDoc.getStatoVersamento()) {
                    case ERRORE:
                        switch (statoVersamentoFascicolo) {
                            case IN_CARICO:
                                statoVersamentoFascicolo = StatoVersamento.IN_CARICO_CON_ERRORI;
                                break;
                            case VERSATO:
                                statoVersamentoFascicolo = StatoVersamento.ERRORE;
                                break;
                        }
                        break;
                    case ERRORE_RITENTABILE:
                        switch (statoVersamentoFascicolo) {
                            case IN_CARICO:
                            case IN_CARICO_CON_ERRORI:
                                statoVersamentoFascicolo = StatoVersamento.IN_CARICO_CON_ERRORI_RITENTABILI;
                                break;
                            case VERSATO:
                            case ERRORE:
                                statoVersamentoFascicolo = StatoVersamento.ERRORE_RITENTABILE;
                                break;
                        }
                        break;
                    case IN_CARICO:
                        switch (statoVersamentoFascicolo) {
                            case ERRORE:
                                statoVersamentoFascicolo = StatoVersamento.IN_CARICO_CON_ERRORI;
                                break;
                            case ERRORE_RITENTABILE:
                                statoVersamentoFascicolo = StatoVersamento.IN_CARICO_CON_ERRORI_RITENTABILI;
                                break;
                            case VERSATO:
                                statoVersamentoFascicolo = StatoVersamento.IN_CARICO;
                                break;
                        }
                        break;
                }
            }
        }
        
        return Pair.of(statoVersamentoFascicolo, versamentoForzabile);
    }
    
    /**
     * Calcola lo stato della sessione in base agli stati dei versamenti effettuati
     * La sessionesarà completa se tutti i versamenti sono andati a buon fine (o sono stati presi in carico), negli altri casi
     * sarà parziale
     * @param versamenti
     * @return lo stato della sessione in base agli stati dei versamenti effettuati
     */
    private StatoSessioneVersamento getStatoSessioneVersamentoFromVersamenti(List<VersamentoDocInformation> versamenti) {
        StatoSessioneVersamento res = StatoSessioneVersamento.DONE;
        if (versamenti != null && !versamenti.isEmpty()) {
            boolean allVersatoOrInCarico = !versamenti.stream().anyMatch(p -> 
                    p.getStatoVersamento() == Versamento.StatoVersamento.PARZIALE ||
                    p.getStatoVersamento() == Versamento.StatoVersamento.AGGIORNARE ||
                    p.getStatoVersamento() == Versamento.StatoVersamento.VERSARE ||
                    p.getStatoVersamento() == Versamento.StatoVersamento.ERRORE ||
                    p.getStatoVersamento() == Versamento.StatoVersamento.IN_CARICO_CON_ERRORI ||
                    p.getStatoVersamento() == Versamento.StatoVersamento.IN_CARICO_CON_ERRORI_RITENTABILI ||
                    p.getStatoVersamento() == Versamento.StatoVersamento.ERRORE_RITENTABILE);

            if (allVersatoOrInCarico) {
                res = StatoSessioneVersamento.DONE;
            } else {
                res = StatoSessioneVersamento.PARTIALLY;
            }
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
//        sessioneVersamento.setAzione(getWorkerData().getAzioneVersamento());
        sessioneVersamento.setIdAzienda(azienda);
        Range<ZonedDateTime> timeInterval = Range.openInfinite(startSessioneVersamento);
        sessioneVersamento.setTimeInterval(timeInterval);
        return sessioneVersamento;
    }
    
    /**
     * Prende tutti i docs su cui ritentare il versamento e li aggiunge alla mappa versamentiDaProcessare passata in input.
     * Questi doc sono quelli il cui versamento precedente non è andato a buon fine, ossia nello stato:
     * ERRORE, IN_CARICO, IN_CARICO_CON_ERRORI, IN_CARICO_CON_ERRORI_RITENTABILI o ERRORE_RITENTABILE.
     * 
     * Un doc va in ERRORE se c'è almeno un versamento(suo o dei suoi allegati) che è andato in ERRORE e non ci sono 
     * versamenti in ERRORE_RITENTABILE o IN_CARICO o IN_CARICO_CON_ERRORI o IN_CARICO_CON_ERRORI_RITENTABILI
     * Un doc va in ERRORE_RITENTABILE se c'è almeno un versamento(suo o dei suoi allegati) che è andato in ERRORE_RITENTABILE
     * Un doc va in IN_CARICO (cioè il versamento non si può ancora considerare completato) se c'è almeno un versamento(suo o 
     * dei suoi allegati) che è andato in IN_CARICO
     * Un doc va in IN_CARICO_CON_ERRORI se c'è almeno un versamento(suo o dei suoi allegati) che è andato in ERRORE e
     * c'è n'è almeno uno IN_CARICO
     * Un doc va in IN_CARICO_CON_ERRORI_RITENTABILI se c'è almeno un versamento(suo o dei suoi allegati) che è andato in 
     * ERRORE_RITENTABILE e ce n'è almeno uno IN_CARICO.
     * Viene anche settato il versamento precedente, sul nuovo versamento da effettuare
     * NB: Vengono presi i versamenti solo dell'azienda a cui il job fa riferimento
     * @param versamentiDaProcessare la mappa dei versamenti da effettuare alla quale verranno aggiunti i doc
     * @param tipologiaVersamento la tipologia del versamento che si vuole effettuare
     * @param queryFactory
     * @return la mappa dei versamenti da effettuare (è la stessa passata in input)
     */
    private Map<Integer, List<VersamentoDocInformation>> addDocsDaRitentare(Map<Integer, List<VersamentoDocInformation>> versamentiDaProcessare, TipologiaVersamento tipologiaVersamento, JPAQueryFactory queryFactory) {
    
        /*
        Prende tutti i versamenti da ritentare delle sessioni dell'azienda del job
        i versamenti da ritentare sono quelli negli stati:
        IN_CARICO_CON_ERRORI_RITENTABILI e ERRORE_RITENTABILE, IN_CARICO, IN_CARICO_CON_ERRORI
        Per ogni versamento ne estrae l'id, l'id del doc e l'id del fascicolo
        */
        BooleanExpression filter = 
            QVersamento.versamento.ignora.eq(false).and(
            QSessioneVersamento.sessioneVersamento.idAzienda.id.eq(getWorkerData().getIdAzienda())).and(
            QVersamento.versamento.stato.in(
                Arrays.asList(
                    Versamento.StatoVersamento.ERRORE.toString(), 
                    Versamento.StatoVersamento.ERRORE_RITENTABILE.toString(),
                    Versamento.StatoVersamento.IN_CARICO.toString(),
                    Versamento.StatoVersamento.IN_CARICO_CON_ERRORI.toString(),
                    Versamento.StatoVersamento.IN_CARICO_CON_ERRORI_RITENTABILI.toString()
                )
            ));
        List<Tuple> versamentiRitentabili = queryFactory
            .select(
                QVersamento.versamento.id, 
                QVersamento.versamento.idDoc.id, 
                QVersamento.versamento.idArchivio.id, 
                QVersamento.versamento.stato)
            .from(QVersamento.versamento)
            .join(QSessioneVersamento.sessioneVersamento).on(
                QVersamento.versamento.idSessioneVersamento.id.eq(QSessioneVersamento.sessioneVersamento.id)
            )
            .where(filter)
            .fetch();
        for (Tuple versamentoRitentabile : versamentiRitentabili) {
            /*
            per ognuno dei versamenti estratti, controllo se il doc era già presente nei versamenti da effettuare
             per lo stesso fasciolo.
             Se lo trovo allora non lo inserisco nuovamente, ma setto il versamento estratto come VersamentoPrecedente
              In questo modo, così da evere l'informazione per attaccare il nuovo versamento al precedente e settare il
              precedente da ignorare
             Se non lo trovo, allora lo aggiungo, settandoci sempre il versamento precedente e il suo stato
            */
            Integer idVersamento = versamentoRitentabile.get(QVersamento.versamento.id);
            Integer idDoc = versamentoRitentabile.get(QVersamento.versamento.idDoc.id);
            Integer idArchivio = versamentoRitentabile.get(QVersamento.versamento.idArchivio.id);
            StatoVersamento statoVersamento = StatoVersamento.valueOf(versamentoRitentabile.get(QVersamento.versamento.stato));
            
            List<VersamentoDocInformation> versamenti = versamentiDaProcessare.get(idDoc);
            boolean versamentoDaInserire = true;
            if (versamenti == null) { // se non trovo versamenti per il doc, creo la lista vuota
                versamenti = new ArrayList<>();
                versamentiDaProcessare.put(idDoc, versamenti);
            } else {
                /*
                se trovo dei versamenti, controllo se c'è il versamento con lo stesso fascicolo
                se lo trovo, setto solo il versamento precedente e il suo stato, altrimenti setto che è da inserire
                */
                for (VersamentoDocInformation v : versamenti) {
                    if ((idArchivio == null && v.getIdArchivio() == null) || 
                        (idArchivio != null && v.getIdArchivio() != null && v.getIdArchivio().equals(idArchivio))) {
                        v.setIdVersamentoPrecedente(idVersamento);
                        v.setStatoVersamentoPrecedente(statoVersamento);
                        versamentoDaInserire = false;
                        break;
                    }
                }
            }
            
            // se il versamento è da inserire lo inserisco nella lista
            if (versamentoDaInserire) {
                VersamentoDocInformation versamentoDocInformation = buildVersamentoDocInformation(idDoc, idArchivio, tipologiaVersamento);
                versamentoDocInformation.setIdVersamentoPrecedente(idVersamento);
                versamentoDocInformation.setStatoVersamentoPrecedente(statoVersamento);
                versamentoDocInformation.setPrimoVersamento(false);
                versamenti.add(versamentoDocInformation);
            }
        }
        
        return versamentiDaProcessare;
    }
    
    /**
     * aggiunge alla mappa dei versamenti da effettuare i doc da versare, indipendentemente dai fascicoli:
     *  Nel caso di sessione giornaliera i doc da versare sono quelli che hanno come statoVersamento VERSARE o AGGIORNARE.
     *  Nel caso di sessione di forzatura i doc da versare sono solo quelli che hanno come statoVersamento FORZARE
     *  NB: Prende solo i doc dell'azienda a cui il job fa riferimento
     * @param versamentiDaProcessare la mappa dei versamenti da effettuare alla quale verranno aggiunti i doc
     * @param tipologiaVersamento la tipologia del versamento (deriva dalla sessione GIORNALIERO o FORZATURA)
     * @param queryFactory
     * @return la mappa dei versamenti da effettuare (è la stessa passata in input)
     */
    private Map<Integer, List<VersamentoDocInformation>> addDocsDaProcessareDaDocs(Map<Integer, List<VersamentoDocInformation>> versamentiDaProcessare, TipologiaVersamento tipologiaVersamento, JPAQueryFactory queryFactory) { 
        List<Integer> docsIdDaProcessare;
        List<StatoVersamento> statiVersamento ;
        // se la siamo in una sessione di forzatura, prenso solo i doc in stato FORZARE 
        if (tipologiaVersamento == TipologiaVersamento.FORZATURA) {
            statiVersamento = Arrays.asList(Versamento.StatoVersamento.FORZARE);
        } else { // altrimenti prendo tutti quelli da versare o aggiornare
            statiVersamento = Arrays.asList(
                Versamento.StatoVersamento.VERSARE, 
                Versamento.StatoVersamento.AGGIORNARE);
        }
        QDoc qDoc = QDoc.doc;
        docsIdDaProcessare = queryFactory
            .select(qDoc.id)
            .from(qDoc)
            .where(
                    qDoc.statoVersamento.isNotNull()
                .and(
                    qDoc.statoVersamento.in(statiVersamento))
                .and(
                    qDoc.idAzienda.id.eq(getWorkerData().getIdAzienda())
                )
            )
            .fetch();
//        }
//        else {
//            QDocDetail qDocDetail = QDocDetail.docDetail;
//            docsIdDaProcessare = queryFactory
//                .select(qDocDetail.id)
//                .from(qDocDetail)
//                .where(
//                    qDocDetail.statoUltimoVersamento.isNotNull().and(
//                    qDocDetail.statoUltimoVersamento.in(
//                        Arrays.asList(
//                                    Versamento.StatoVersamento.IN_CARICO.toString(), 
//                                    Versamento.StatoVersamento.IN_CARICO_CON_ERRORI.toString(),
//                                    Versamento.StatoVersamento.IN_CARICO_CON_ERRORI_RITENTABILI.toString()))
//                    ).and(
//                        qDocDetail.idAzienda.id.eq(getWorkerData().getIdAzienda())
//                    )
//                )
//                .fetch();
//        }
        
        for (Integer docIdDaProcessare : docsIdDaProcessare) {
            VersamentoDocInformation versamentoDocInformation = buildVersamentoDocInformation(docIdDaProcessare, null, tipologiaVersamento);
            List<VersamentoDocInformation> versamenti = versamentiDaProcessare.get(docIdDaProcessare);
            if (versamenti == null) {
                versamenti = new ArrayList<>();
                versamentiDaProcessare.put(docIdDaProcessare, versamenti);
            }
            versamenti.add(versamentoDocInformation);
        }
        return versamentiDaProcessare;
    }
    
    /**
     * 
     * @param idDocsDaVersare
     * @param tipologiaVersamento
     * @param queryFactory
     * @return
     * @throws VersatoreProcessingException 
     */
    private Map<Integer, List<VersamentoDocInformation>> addDocsDaVersare(
            List<Integer> idDocsDaVersare, 
            TipologiaVersamento tipologiaVersamento, 
            JPAQueryFactory queryFactory) throws MasterjobsWorkerException {
        List<Integer> docsIdDaProcessare;
        StatoVersamento statoVersamento = null;
        // se la siamo in una sessione di forzatura, prenso solo i doc in stato FORZARE 
        if (tipologiaVersamento == TipologiaVersamento.FORZATURA) {
            statoVersamento = Versamento.StatoVersamento.FORZARE;
        } else { // altrimenti prendo tutti quelli da ritentare
            statoVersamento = Versamento.StatoVersamento.ERRORE_RITENTABILE;
        }
        switch (tipologiaVersamento) {
            case FORZATURA:
                statoVersamento = Versamento.StatoVersamento.FORZARE;
                break;
            case RITENTA:
                statoVersamento = Versamento.StatoVersamento.ERRORE_RITENTABILE;
                break;
            default:
                throw new MasterjobsWorkerException("Tipologia versamento non prevista");
        }
        
        // Faccio la query anche se ho già gli idDoc per assicurarmi che corrispondano alla giusta tipologia e alla giusta azienda
        QDoc qDoc = QDoc.doc;
        docsIdDaProcessare = queryFactory
            .select(qDoc.id)
            .from(qDoc)
            .where(
                    qDoc.statoVersamento.eq(statoVersamento)
                .and(
                    qDoc.idAzienda.id.eq(getWorkerData().getIdAzienda())
                )
                .and(
                    qDoc.id.in(idDocsDaVersare)   
                )
            )
            .fetch();
        
        Map<Integer, List<VersamentoDocInformation>> versamentiDaProcessare = new HashMap();
        for (Integer docIdDaProcessare : docsIdDaProcessare) {
            VersamentoDocInformation versamentoDocInformation = buildVersamentoDocInformation(docIdDaProcessare, null, tipologiaVersamento);
            List<VersamentoDocInformation> versamenti = versamentiDaProcessare.get(docIdDaProcessare);
            if (versamenti == null) {
                versamenti = new ArrayList<>();
                versamentiDaProcessare.put(docIdDaProcessare, versamenti);
            }
            versamenti.add(versamentoDocInformation);
        }
        return versamentiDaProcessare;
    }
    
    /**
     * aggiunge alla mappa dei versamenti da effettuare i doc all'interno dei fascicoli da versare
     * Se siamo in una sessione giornaliera, verranno aggiunti tutti i doc all'interno dei fascicoli nello stato Versare, indipendentemente dal loro sato versamento.
     * Se siamo in una sessione di forzatura, verranno aggiunti solo i doc in stato FORZARE
     * I fascicoli presi in considerazione saranno tutti i fascicoli che hanno come statoVersamento VERSARE o AGGIORNARE
     * NB: Prende solo i doc dell'azienda a cui il fascicolo fa riferimento
     * @param versamentiDaProcessare la mappa dei versamenti da effettuare alla quale verranno aggiunti i doc
     * @param tipologiaVersamento la tipologia del versamento (GIORNALIERO o FORZATURA)
     * @param queryFactory
     * @return la mappa dei versamenti da effettuare (è la stessa passata in input)
     */
    private Map<Integer, List<VersamentoDocInformation>> addDocsDaProcessareDaArchivi(Map<Integer, List<VersamentoDocInformation>> versamentiDaProcessare, TipologiaVersamento tipologiaVersamento, JPAQueryFactory queryFactory) {        
        List<Integer> archiviIdDaProcessare;
        /* 
        estrae tutti i fascicoli da versare, coè quelli che hanno statoVersamento VERSARE o AGGIORNARE, 
        dell'azienda indicata nel job
        */
        QArchivio qArchivio = QArchivio.archivio;
        archiviIdDaProcessare = queryFactory
            .select(qArchivio.id)
            .from(qArchivio)
            .where(
                qArchivio.statoVersamento.isNotNull().and(
                qArchivio.statoVersamento.in(
                        Arrays.asList(
                                Versamento.StatoVersamento.VERSARE.toString(), 
                                Versamento.StatoVersamento.AGGIORNARE.toString()))
                ).and(
                    qArchivio.idAzienda.id.eq(getWorkerData().getIdAzienda())
                )
            )
            .fetch();
        
        for (Integer idArchivioDaVersare : archiviIdDaProcessare) {
            List<Integer> docsIdDaProcessare;
            QArchivioDoc qArchivioDoc = QArchivioDoc.archivioDoc;
            QDocDetail qDocDetail = QDocDetail.docDetail;
            
            /*
            Se sono in uno versamento giornaliero, estraggo i doc all'interno dei fascicoli trovati prima e ne inserisco il versamento 
            di tutti nella mappa, indipendentemente dallo stato del doc
            */  
            BooleanExpression filter = 
            qArchivioDoc.idArchivio.id.eq(idArchivioDaVersare)
                .and(
            qDocDetail.numeroRegistrazione.isNotNull());
            // se sono in una sessione FORZATURA, allora considero solo i doc nello stato FORZARE
            if (tipologiaVersamento == TipologiaVersamento.FORZATURA) {
                filter = filter.and(qArchivioDoc.idDoc.statoVersamento.eq(Versamento.StatoVersamento.FORZARE));
            }
            
            docsIdDaProcessare = queryFactory
                .select(qArchivioDoc.idDoc.id)
                .from(qArchivioDoc)
                .join(qDocDetail).on(qDocDetail.id.eq(qArchivioDoc.idDoc.id))
                .where(filter)
                .fetch();
            for (Integer docIdDaProcessare : docsIdDaProcessare) {
                VersamentoDocInformation versamentoDocInformation = buildVersamentoDocInformation(docIdDaProcessare, idArchivioDaVersare, tipologiaVersamento);
                List<VersamentoDocInformation> versamentiDoc = versamentiDaProcessare.get(docIdDaProcessare);
                if (versamentiDoc == null) {
                    versamentiDoc = new ArrayList<>();
                    versamentiDaProcessare.put(docIdDaProcessare, versamentiDoc);
                }
                versamentiDoc.add(versamentoDocInformation);
            }
        }
        
        return versamentiDaProcessare;
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
        versamentoDocInformation.setParams(getWorkerData().getParams());
        return versamentoDocInformation;
    }
}
