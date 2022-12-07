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
import it.bologna.ausl.model.entities.scripta.Doc;
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
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;

/**
 *
 * @author gdm
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
                        QSessioneVersamento.sessioneVersamento.stato.eq(StatoSessioneVersamento.RUNNING.toString())
                    )
                )
                .fetchOne();
            }
        );
    }
    
    private SessioneVersamento openNewSessioneVersamento(TipologiaVersamento tipologiaVersamento) {
        SessioneVersamento sessioneVersamento = transactionTemplate.execute(a -> {
            SessioneVersamento newSessioneVersamento = buildSessioneVersamento(tipologiaVersamento);
                entityManager.persist(newSessioneVersamento);
                return newSessioneVersamento;
            });
        return sessioneVersamento;
    }
    
    private void closeSessioneVersamento(StatoSessioneVersamento statoSessioneVersamento, SessioneVersamento sessioneVersamento) {
        sessioneVersamento.setStato(statoSessioneVersamento);
        sessioneVersamento.setTimeInterval(Range.open(sessioneVersamento.getTimeInterval().lower(), ZonedDateTime.now()));
        transactionTemplate.executeWithoutResult(a -> entityManager.persist(statoSessioneVersamento));
    }
    
    private Map<Integer, List<VersamentoDocInformation>> removeDocGiaVersati(Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare, Integer idSessioneVersamento, JPAQueryFactory queryFactory) {
        List<Tuple> versamentiEffettuati = transactionTemplate.execute(a -> {
            return queryFactory
                    .select(QVersamento.versamento.idDoc.id, QVersamento.versamento.idArchivio.id)
                    .from(QVersamento.versamento)
                    .where(QVersamento.versamento.idSessioneVersamento.id.eq(idSessioneVersamento))
                    .fetch();
        });
        if (versamentiEffettuati != null && !versamentiEffettuati.isEmpty()) {
            for (Tuple versamentoEffettuato : versamentiEffettuati) {
                Integer idDocVersamentoEffettuato = versamentoEffettuato.get(QVersamento.versamento.idDoc.id);
                Integer idArchivioVersamentoEffettuato = versamentoEffettuato.get(QVersamento.versamento.idArchivio.id);
                List<VersamentoDocInformation> versamentiDocDaEffettuare = versamentiDaEffettuare.get(idDocVersamentoEffettuato);
                if (versamentiDocDaEffettuare != null) {
                    List<VersamentoDocInformation> versamentiDocDaEffettuareFiltrati = 
                            versamentiDocDaEffettuare.stream().filter(versamentoDocDaEffettuare -> 
                                !versamentoDocDaEffettuare.getIdDoc().equals(idDocVersamentoEffettuato) ||
                                versamentoDocDaEffettuare.getIdArchivio() == null && idArchivioVersamentoEffettuato != null ||
                                versamentoDocDaEffettuare.getIdArchivio() != null && idArchivioVersamentoEffettuato == null || (
                                    versamentoDocDaEffettuare.getIdArchivio() != null && idArchivioVersamentoEffettuato != null && 
                                    !versamentoDocDaEffettuare.getIdArchivio().equals(idArchivioVersamentoEffettuato)
                                )    
                    ).collect(Collectors.toList());
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
    
    private StatoSessioneVersamento executeAllVersatoreDocThreads(List<VersatoreDocThread> versatoreDocThreads) throws MasterjobsWorkerException {
        Integer poolSize = getWorkerData().getPoolSize();
        ExecutorService executoreService = Executors.newFixedThreadPool(poolSize);
        
        List<VersamentoDocInformation> allResults = new ArrayList<>();
        try {
            List<Future<List<VersamentoDocInformation>>> threadsResult = executoreService.invokeAll(versatoreDocThreads);
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
            allResults.stream().map(v -> v.getIdArchivio());
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
    
    private List<VersatoreDocThread> buildVersatoreDocThreadsList(Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare, SessioneVersamento sessioneVersamento) {
        VersatoreDocs versatoreDocsInstance = versatoreFactory.getVersatoreDocsInstance(getWorkerData().getHostId());
        
        Persona personaForzatura = getPersonaForzatura();
        
        List<VersatoreDocThread> versatoreDocThreads = new ArrayList<>();
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
    
    private SessioneVersamento buildSessioneVersamento(TipologiaVersamento tipologiaVersamento) {
        Azienda azienda = entityManager.find(Azienda.class, getWorkerData().getIdAzienda());
        SessioneVersamento sessioneVersamento = new SessioneVersamento();
        sessioneVersamento.setStato(StatoSessioneVersamento.RUNNING);
        sessioneVersamento.setTipologia(tipologiaVersamento);
        sessioneVersamento.setIdAzienda(azienda);
        Range<ZonedDateTime> timeInterval = Range.openInfinite(startSessioneVersamento);
        sessioneVersamento.setTimeInterval(timeInterval);
        return sessioneVersamento;
    }
    
    private Map<Integer, List<VersamentoDocInformation>> addDocsDaRitentare(Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare, TipologiaVersamento tipologiaVersamento, JPAQueryFactory queryFactory) {
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
            Integer idVersamento = versamentoRitentabile.get(QVersamento.versamento.id);
            Integer idDoc = versamentoRitentabile.get(QVersamento.versamento.idDoc.id);
            Integer idArchivio = versamentoRitentabile.get(QVersamento.versamento.idArchivio.id);
            List<VersamentoDocInformation> versamenti = versamentiDaEffettuare.get(idDoc);
            boolean versamentoDaInserire = true;
            if (versamenti == null) {
                versamenti = new ArrayList<>();
                versamentiDaEffettuare.put(idDoc, versamenti);
            } else {
                for (VersamentoDocInformation v : versamenti) {
                    if ((idArchivio == null && v.getIdArchivio() == null) || 
                        (idArchivio != null && v.getIdArchivio() != null && v.getIdArchivio().equals(idArchivio))) {
                        v.setVersamentoPrecedente(idVersamento);
                        versamentoDaInserire = false;
                        break;
                    }
                }
            }
            
            if (versamentoDaInserire) {
                VersamentoDocInformation versamentoDocInformation = buildVersamentoDocInformation(idDoc, idArchivio, tipologiaVersamento);
                versamenti.add(versamentoDocInformation);
            }
        }
        
        return versamentiDaEffettuare;
    }
    
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
    
    private Map<Integer, List<VersamentoDocInformation>> addDocsDaVersareDaArchivi(Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare, TipologiaVersamento tipologiaVersamento, JPAQueryFactory queryFactory) {
        
        QArchivio qArchivio = QArchivio.archivio;
        BooleanExpression filterArchiviDaVersare = 
            qArchivio.statoVersamento.isNotNull().and(
            qArchivio.statoVersamento.in(
                    Arrays.asList(
                            Versamento.StatoVersamento.VERSARE.toString(), 
                            Versamento.StatoVersamento.AGGIORNARE.toString(), 
                            Versamento.StatoVersamento.ERRORE_RITENTABILE.toString()))
            ).and(
                qArchivio.idAzienda.id.eq(getWorkerData().getIdAzienda())
            );
        
        List<Integer> archiviIdDaVersare = queryFactory.select(qArchivio.id).from(qArchivio).where(filterArchiviDaVersare).fetch();
        for (Integer idArchivioDaVersare : archiviIdDaVersare) {
            QArchivioDoc qArchivioDoc = QArchivioDoc.archivioDoc;
            List<Integer> docsIdDaVersare = queryFactory
                .select(qArchivioDoc.idDoc.id)
                .from(qArchivioDoc)
                .where(qArchivioDoc.idArchivio.id.eq(idArchivioDaVersare)).fetch();
//            archiviDocs.put(idArchivioDaVersare, docsIdDaVersare.stream().collect(Collectors.toMap(Function.identity(), null)));
            for (Integer docIdDaVersare : docsIdDaVersare) {
                VersamentoDocInformation versamentoDocInformation = buildVersamentoDocInformation(docIdDaVersare, idArchivioDaVersare, tipologiaVersamento);
                List<VersamentoDocInformation> versamentiDoc = versamentiDaEffettuare.get(docIdDaVersare);
                if (versamentiDoc == null) {
                    versamentiDoc = new ArrayList<>();
                    versamentiDaEffettuare.put(docIdDaVersare, versamentiDoc);
                }
                versamentiDoc.add(versamentoDocInformation);
//versaDoc(versatoreDocsInstance, queryFactory, docIdDaVersare, idArchivioDaVersare, SessioneVersamento.TipologiaVersamento.GIORNALIERO_FASCICOLI);
;
            }
        }
        
        return versamentiDaEffettuare;
    }
    
    private VersamentoDocInformation buildVersamentoDocInformation(Integer idDoc, Integer idArchivio, TipologiaVersamento tipologiaVersamento) {
        VersamentoDocInformation versamentoDocInformation = new VersamentoDocInformation();
        versamentoDocInformation.setTipologiaVersamento(tipologiaVersamento);
        versamentoDocInformation.setIdDoc(idDoc);
        versamentoDocInformation.setIdArchivio(idArchivio);
        return versamentoDocInformation;
    }
}
