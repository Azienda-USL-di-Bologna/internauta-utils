package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.recuperorapportodiversamento;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.versatore.VersatoreFactory;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.RecuperoRapportoDiVersamento;
import it.bologna.ausl.model.entities.versatore.RapportoDiVersamento;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
        RecuperoRapportoDiVersamento recuperoRapportoDiVersamentoInstance;
        try {
            // reperisce la classe plugin per il recupero del rapporto di versamento
            recuperoRapportoDiVersamentoInstance = versatoreFactory.getRecuperoRapportoDiVersamentoInstance(getWorkerData().getHostId());
        } catch (VersatoreProcessingException ex) {
            String errorMessage = "errore nel reperire il plugin di versamento";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
        try {
            // per ogni versamento recupero il suo rapporto di versamento
            RapportoDiVersamento rapportoDiVersamento = recuperoRapportoDiVersamentoInstance.recuperaRapportiDiVersamento();
            log.info("rapporto: " + rapportoDiVersamento.getRapporto());
        } catch (Throwable ex) {
            String errorMessage = String.format("errore nel plugin di idoneitaCheck sull'archivio %s", 233712); //TODO mettere bersamenti
            log.error(errorMessage, ex);
            //TODO: compilare report checker
        }

        return null;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

}
