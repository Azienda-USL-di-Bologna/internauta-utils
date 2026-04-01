package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.recuperorapportodiversamento;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.versatore.VersatoreFactory;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.RecuperoRapportoDiVersamento;
import it.bologna.ausl.model.entities.versatore.QRapportoDiVersamento;
import it.bologna.ausl.model.entities.versatore.QSessioneVersamento;
import it.bologna.ausl.model.entities.versatore.QVersamento;
import it.bologna.ausl.model.entities.versatore.RapportoDiVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;

/**
 *
 * @author boria
 *
 * Job che contatta il servizio di versamento per avere indietro il rapporto e salvarlo in tabelle
 * versatore.rapporti_di_versamento
 * Si usa quando la validazione e l'invio in conservazione avviene dopo la consegna di parte del Versatore al servizio di conservazione esterno a Babel,
 * e occorre dunque che venga recuperato contattando tale servizio.
 */
@MasterjobsWorker
public class RecuperoRapportoDiVersamentoJobWorker extends JobWorker<RecuperoRapportoDiVersamentoJobWorkerData, JobWorkerResult> {

    private static final Logger log = LoggerFactory.getLogger(RecuperoRapportoDiVersamentoJobWorker.class);

    @Autowired
    private VersatoreFactory versatoreFactory;

    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in doWork()");
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        // setto il transactionTemplate in modo che tutte le volte che lo si usa apra una transazione e la committi
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        QVersamento qVersamento = QVersamento.versamento;
        QRapportoDiVersamento qRapportoDiVersamento = QRapportoDiVersamento.rapportoDiVersamento;

        //prendo i versamenti di cui reucperare il rapporto di versamento:
        //sono quelli di cui la sessione è dell'azienda passata e che non hanno già rapporti di versamento o che ne hanno uno con daRitentare a true
        List<Versamento> versamentiDaProcessareList = transactionTemplate.execute(a -> {
            return queryFactory
                .selectFrom(qVersamento)
                .where(
                    qVersamento.idSessioneVersamento.idAzienda.id.eq(getWorkerData().getIdAzienda())
                        .and(qVersamento.stato.eq(Versamento.StatoVersamento.VERSATO.toString()))
                        .and(
                            JPAExpressions.selectOne()
                                .from(qRapportoDiVersamento)
                                .where(qRapportoDiVersamento.idVersamento.id.eq(qVersamento.id))
                                .notExists()
                                .or(
                                    JPAExpressions.selectOne()
                                        .from(qRapportoDiVersamento)
                                        .where(qRapportoDiVersamento.idVersamento.id.eq(qVersamento.id)
                                            .and(qRapportoDiVersamento.daRitentare.isTrue()))
                                        .exists()
                                )
                        )
                ).fetch();
        });

        log.info("Numero rapporti di versamento da recuperare: " + versamentiDaProcessareList.size());

        if (versamentiDaProcessareList != null && !versamentiDaProcessareList.isEmpty()) {
            List<RecuperoRapportoDiVersamentoThread> recuperoRapportoDiVersamentoThreadsList;
            try {
                recuperoRapportoDiVersamentoThreadsList = buildRecuperoRapportoDiVersamentoThreadsList(versamentiDaProcessareList);
            } catch (Throwable ex) {
                final String message = "errore nella creazione dei threads di recupero rapporto di versamento";
                log.error(message, ex);
                throw new MasterjobsWorkerException(message, ex);
            }
            List<RapportoDiVersamento> rapportoDiVersamentoResultList = executeAllRecuperoRapportoDiVersamentoThreads(recuperoRapportoDiVersamentoThreadsList);

            //levo dallo stato da ritentare i rapporti ritentati
            togliDaRitentare(queryFactory);
            //salvo i rapporti di versamento
            persistRapportiRecupertati(rapportoDiVersamentoResultList);

        }

        return null;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
    Costruisce i threads capaci di effettuare i recuperi di rapporto di versamento
    @param versamentiDaRecuperare lista contenente i versamente di cui recuperare i rapporti di versamento
    @return la lista dei threads costruiti
    @throws VersatoreProcessingException
     */
    private List<RecuperoRapportoDiVersamentoThread> buildRecuperoRapportoDiVersamentoThreadsList(List<Versamento> versamentiDaRecuperare) throws VersatoreProcessingException {
        RecuperoRapportoDiVersamento recuperoRapportoDiVersamentoInstance = versatoreFactory.getRecuperoRapportoDiVersamentoInstance(getWorkerData().getHostId());

        List<RecuperoRapportoDiVersamentoThread> recuperoRapportoDiVersamentoThreadsList = new ArrayList<>();

        //cicla la lista dei versamenti di cui recuperare il rapporto di versamento e per ognuno costruisce un thread.
        for (Versamento versamento : versamentiDaRecuperare) {
            RecuperoRapportoDiVersamentoThread recuperoRapportoDiVersamentoThread = new RecuperoRapportoDiVersamentoThread(versamento, recuperoRapportoDiVersamentoInstance, getWorkerData().getParams());
            recuperoRapportoDiVersamentoThreadsList.add(recuperoRapportoDiVersamentoThread);
        }
        return recuperoRapportoDiVersamentoThreadsList;
    }

    /**
     * Lancia i threads per il recupero dei rapporti di versamento e attende che tutti abbiamo finito
     * NB: il massimo di threads contemporanei è indicato del parametro "poolSize" del job
    @param recuperoRapportoDiVersamentoThreads
    @return
    @throws MasterjobsWorkerException
     */
    private List<RapportoDiVersamento> executeAllRecuperoRapportoDiVersamentoThreads(List<RecuperoRapportoDiVersamentoThread> recuperoRapportoDiVersamentoThreads) throws MasterjobsWorkerException {
        Integer poolSize = getWorkerData().getPoolSize();
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);

        List<RapportoDiVersamento> rapportiDiVersamentoList = new ArrayList<>();
        try {
            // fa partire tutti i threads (massimo poolSize in contemporanea) e attende il risultato
            List<Future<RapportoDiVersamento>> threadsResult = executorService.invokeAll(recuperoRapportoDiVersamentoThreads);

            // tutti i threads hanno finito, ciclo tutti i risultati
            for (Future<RapportoDiVersamento> threadResult : threadsResult) {
                RapportoDiVersamento rapportoDiVersamento = threadResult.get();
                //aggiungo il rapporto di versamento
                rapportiDiVersamentoList.add(rapportoDiVersamento);
            }
            return rapportiDiVersamentoList;
        } catch (InterruptedException ex) {
            String errorMessage = "ricevuto un InterruptedException errore nell'attesa del completamento dei thread di recupero rapporto di versamento";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        } catch (Throwable ex) {
            String errorMessage = "errore nell'attesa del completamento dei thread di recupero rapporto di versamento";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
    }

    /**
    Persistenza dei rapporti recuperati
    @param rapportiDiVersamentoList
     */
    private void persistRapportiRecupertati(List<RapportoDiVersamento> rapportiDiVersamentoList) {
        transactionTemplate.executeWithoutResult(a -> {
            for (RapportoDiVersamento rapportoDiVersamento : rapportiDiVersamentoList) {
                entityManager.persist(rapportoDiVersamento);
            }
        });
    }

    /**
    Setto a false tutti i daRitentare per l'azienda che sta eseguendo il job
    @param queryFactory
     */
    private void togliDaRitentare(JPAQueryFactory queryFactory) {
        transactionTemplate.executeWithoutResult(a -> {
            QRapportoDiVersamento rdv = QRapportoDiVersamento.rapportoDiVersamento;
            QVersamento v = QVersamento.versamento;
            QSessioneVersamento sv = QSessioneVersamento.sessioneVersamento;

            // Subquery per ottenere gli id dei rapporti da aggiornare
            List<Integer> ids = queryFactory
                .select(rdv.id)
                .from(rdv)
                .join(rdv.idVersamento, v)
                .join(v.idSessioneVersamento, sv)
                .where(rdv.daRitentare.eq(Boolean.TRUE)
                    .and(sv.idAzienda.id.eq(getWorkerData().getIdAzienda())))
                .fetch();

            if (!ids.isEmpty()) {
                queryFactory
                    .update(rdv)
                    .set(rdv.daRitentare, Boolean.FALSE)
                    .where(rdv.id.in(ids))
                    .execute();
            }
        });
    }

}
