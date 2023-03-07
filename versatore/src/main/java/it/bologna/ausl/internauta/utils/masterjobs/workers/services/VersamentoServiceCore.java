package it.bologna.ausl.internauta.utils.masterjobs.workers.services;

import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore.VersatoreJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore.VersatoreJobWorkerData;
import it.bologna.ausl.model.entities.masterjobs.Set;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe core del servizio di versamento, che contiene la logica di accodamento dei job di versamento per le varie aziende
 * Da richiamare dal servizio su internauta
 * @author gdm
 */
public class VersamentoServiceCore {
    private static final Logger log = LoggerFactory.getLogger(VersamentoServiceCore.class);
    
    private final MasterjobsJobsQueuer masterjobsJobsQueuer;
    private final MasterjobsObjectsFactory masterjobsObjectsFactory;

    public VersamentoServiceCore(MasterjobsJobsQueuer masterjobsJobsQueuer, MasterjobsObjectsFactory masterjobsObjectsFactory) {
        this.masterjobsJobsQueuer = masterjobsJobsQueuer;
        this.masterjobsObjectsFactory = masterjobsObjectsFactory;
    }
    
    /**
     * accoda il job di versamento per ogni idAzienda chiave della mappa, con i parametri indicati nel valore della mappa
     * @param aziendeAttiveConParametri mappa che ha come chiave l'idAzienda e come valore i parametri del versatore 
     * (letti dalla tabella configurazione.parametri_azienda)
     * @param app applicazione a cui è legato il job da inserire
     */
    public void queueVersatoreJobs(Map<Integer, Map<String, Object>> aziendeAttiveConParametri, String app) {
        // per tutte le aziende in cui il versatore è attivo, ne legge i paramentri accoda mestiere di versamento
        for (Integer idAzienda : aziendeAttiveConParametri.keySet()) {            
            Map<String, Object> versatoreConfigAziendaValue = aziendeAttiveConParametri.get(idAzienda);
            
            // hostId del servizio di versamento, indica il servizio che sarà utilizzato (sono definiti nella tabella versatore.configurations)
            String hostId = (String) versatoreConfigAziendaValue.get("hostId");
            
            // numero massimo di thread paralleli che il job di versamento istanzierà per effettuare i versamenti
            Integer threadPoolSize = (Integer) versatoreConfigAziendaValue.get("threadPoolSize");
            
            //parametri per il versamento 
            Map<String,Object> params = (Map<String,Object>) versatoreConfigAziendaValue.get("params");
            
            
            
            // richiama il metodo sul core che si occupa dell'accodamento del job
            queueAziendaJob(idAzienda, hostId, false, null, threadPoolSize, app, params);
            
        }
    }
    
    /**
     * Accoda il job di versamento
     * @param idAzienda azienda per la quale il job lavorerà
     * @param hostId hostId del servizio di versamento da usare
     * @param forzatura passare "true" se si tratta di una forzatura, "false" se accodato da un servizio giornaliero
     * @param idPersonaForzatura la persona che effettua la forzatura, se non si tratta di una forzatura passare null
     * @param poolsize numero di threads contemporanei massimi del pool di versamento
     * @param app applicazione a cui legare il job
     * @param params parametri specifici per il versamento
     */
    private void queueAziendaJob(Integer idAzienda, String hostId, Boolean forzatura, Integer idPersonaForzatura, Integer poolsize, String app, Map<String,Object> params) {
        VersatoreJobWorkerData versatoreJobWorkerData = new VersatoreJobWorkerData(idAzienda, hostId, forzatura, poolsize, idPersonaForzatura, params);
        VersatoreJobWorker jobWorker = null;
        try { // istanzia il woker
            jobWorker = masterjobsObjectsFactory.getJobWorker(VersatoreJobWorker.class, versatoreJobWorkerData, false);
        } catch (Exception ex) {
            String errorMessage = "errore nella creazione del job Versatore";
            log.error(errorMessage, ex);
        }
        try {
            /* accoda il worker mettendo nell'id_oggetto l'id dell'azienda, in modo che se dovessero esserci 2 job sulla stessa azienda
             * non vengano eseguiti in parallelo.
             * Il caso di 2 job sulla stessa azienda potrebbe capitare se si riavvia internauta mentre sta eseguendo il job:
             * ci sarebbe il job che era in esecuzione, che riprenderebbe da capo e l'eventuale nuovo job aggiunto dal servizio.
             * NB: il job, dopo che versa un doc, non dovrebbe riversalo nuovamente, quindi anche se riprende da capo, non riversa 
             * i documenti già versati
            */
            masterjobsJobsQueuer.queue(jobWorker, "versatore_" + idAzienda, "Versatore", app, true, Set.SetPriority.NORMAL);
        } catch (Exception ex) {
            String errorMessage = "errore nell'accodamento del job Versatore";
            log.error(errorMessage, ex);
        }
    }
}
