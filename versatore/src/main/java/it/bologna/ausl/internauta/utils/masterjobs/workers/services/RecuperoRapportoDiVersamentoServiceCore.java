package it.bologna.ausl.internauta.utils.masterjobs.workers.services;

import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.recuperorapportodiversamento.RecuperoRapportoDiVersamentoJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.recuperorapportodiversamento.RecuperoRapportoDiVersamentoJobWorkerData;
import it.bologna.ausl.model.entities.masterjobs.Set;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author boria
 */
public class RecuperoRapportoDiVersamentoServiceCore {

    private static final Logger log = LoggerFactory.getLogger(RecuperoRapportoDiVersamentoServiceCore.class);

    private final MasterjobsJobsQueuer masterjobsJobsQueuer;
    private final MasterjobsObjectsFactory masterjobsObjectsFactory;

    public RecuperoRapportoDiVersamentoServiceCore(MasterjobsJobsQueuer masterjobsJobsQueuer, MasterjobsObjectsFactory masterjobsObjectsFactory) {
        this.masterjobsJobsQueuer = masterjobsJobsQueuer;
        this.masterjobsObjectsFactory = masterjobsObjectsFactory;
    }

    /**
     * accoda il job di recupero rapporto di versamento per ogni idAzienda chiave della mappa, con i parametri indicati nel valore della mappa
     * @param aziendeAttiveConParametri mappa che ha come chiave l'idAzienda e come valore i parametri del versatore
     * (letti dalla tabella configurazione.parametri_azienda)
     * @param app applicazione a cui è legato il job da inserire
     */
    public void queueRecuperoRapportoDiVersamentoJobs(Map<Integer, Map<String, Object>> aziendeAttiveConParametri, String app) {
        // per tutte le aziende in cui il versatore è attivo, ne legge i paramentri accoda mestiere di recupero rapporto di versamento
        for (Integer idAzienda : aziendeAttiveConParametri.keySet()) {
            Map<String, Object> versatoreConfigAziendaValue = aziendeAttiveConParametri.get(idAzienda);

            // dai parametri leggo se il servizio di recupero del rapporto di versamento deve essere esguito.
            // se il parametro non c'è lo interpreto come false
            boolean eseguiRecuperoRapportoDiVersamento = Boolean.TRUE.equals(
                versatoreConfigAziendaValue.get("eseguiRecuperoRapportoDiVersamento")
            );

            if (eseguiRecuperoRapportoDiVersamento) {
                // hostId del servizio di versamento, indica il servizio che sarà utilizzato (sono definiti nella tabella versatore.configurations)
                String hostId = (String) versatoreConfigAziendaValue.get("hostId");

                // numero massimo di thread paralleli che il job di versamento istanzierà per effettuare i versamenti
                Integer threadPoolSize = (Integer) versatoreConfigAziendaValue.get("threadPoolSize");

                //parametri per il versamento
                Map<String, Object> params = (Map<String, Object>) versatoreConfigAziendaValue.get("params");

                // richiama il metodo sul core che si occupa dell'accodamento del job
                queueAziendaJob(idAzienda, hostId, threadPoolSize, app, params);
            }

        }
    }

    /**
     * Accoda il job di recupero rapporto di versamento
     * @param idAzienda azienda per la quale il job lavorerà
     * @param hostId hostId del servizio di versamento da usare
     */
    private void queueAziendaJob(Integer idAzienda, String hostId, Integer poolsize, String app, Map<String, Object> params) {
        RecuperoRapportoDiVersamentoJobWorkerData recuperoRapportoDiVersamentoJobWorkerData = new RecuperoRapportoDiVersamentoJobWorkerData(idAzienda, hostId, poolsize, params); //TODO mettere i valori
        RecuperoRapportoDiVersamentoJobWorker jobWorker = null;
        try { // istanzia il worker
            jobWorker = masterjobsObjectsFactory.getJobWorker(RecuperoRapportoDiVersamentoJobWorker.class, recuperoRapportoDiVersamentoJobWorkerData, false);
        } catch (Exception ex) {
            String errorMessage = "errore nella creazione del job RecuperoRapportoDiVersamento";
            log.error(errorMessage, ex);
        }
        try {
            /* accoda il worker mettendo nell'id_oggetto l'id dell'azienda, in modo che se dovessero esserci 2 job sulla stessa azienda
             * non vengano eseguiti in parallelo.
             * Il caso di 2 job sulla stessa azienda potrebbe capitare se si riavvia internauta mentre sta eseguendo il job:
             * ci sarebbe il job che era in esecuzione, che riprenderebbe da capo e l'eventuale nuovo job aggiunto dal servizio.
             * NB: se il job riviene eseguito per lo stesso versamento aggiungerà semplicemente una riga con il rapporto aggiornato
             */
            masterjobsJobsQueuer.queue(jobWorker, "recupero_rapporto_di_versamento_" + idAzienda, "RecuperoRapportoDiVersamento", app, true, Set.SetPriority.NORMAL, null);
        } catch (Exception ex) {
            String errorMessage = "errore nella creazione del job RecuperoRapportoDiVersamento";
            log.error(errorMessage, ex);
        }
    }

}
