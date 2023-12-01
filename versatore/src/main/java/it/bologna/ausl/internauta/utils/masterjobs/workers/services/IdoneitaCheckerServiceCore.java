package it.bologna.ausl.internauta.utils.masterjobs.workers.services;

import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.controlloidoneita.IdoneitaCheckerJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.controlloidoneita.IdoneitaCheckerJobWorkerData;
import it.bologna.ausl.model.entities.masterjobs.Set;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class IdoneitaCheckerServiceCore {
    private static final Logger log = LoggerFactory.getLogger(VersamentoServiceCore.class);
    
    private final MasterjobsJobsQueuer masterjobsJobsQueuer;
    private final MasterjobsObjectsFactory masterjobsObjectsFactory;

    public IdoneitaCheckerServiceCore(MasterjobsJobsQueuer masterjobsJobsQueuer, MasterjobsObjectsFactory masterjobsObjectsFactory) {
        this.masterjobsJobsQueuer = masterjobsJobsQueuer;
        this.masterjobsObjectsFactory = masterjobsObjectsFactory;
    }
    
    /**
     * accoda il job di controllo idoneità per ogni idAzienda chiave della mappa, con i parametri indicati nel valore della mappa
     * @param aziendeAttiveConParametri mappa che ha come chiave l'idAzienda e come valore i parametri del versatore 
     * (letti dalla tabella configurazione.parametri_azienda)
     * @param app applicazione a cui è legato il job da inserire
     */
    public void queueIdoneitaCheckerJobs(Map<Integer, Map<String, Object>> aziendeAttiveConParametri, String app) {
        // per tutte le aziende in cui il versatore è attivo, ne legge i paramentri accoda mestiere di versamento
        for (Integer idAzienda : aziendeAttiveConParametri.keySet()) {            
            Map<String, Object> versatoreConfigAziendaValue = aziendeAttiveConParametri.get(idAzienda);
            
            // hostId del servizio di versamento, indica il servizio che sarà utilizzato (sono definiti nella tabella versatore.configurations)
            String hostId = (String) versatoreConfigAziendaValue.get("hostId");
            
            // dai parametri leggo se il servizio di idoneità deve controllare gli archivi
            Boolean controllaIdoneitaArchivi = (Boolean) versatoreConfigAziendaValue.get("controllaIdoneitaArchivi");
            // dai parametri leggo anche se il servizio di idoneità deve controllare i doc
            Boolean controllaIdoneitaDocs = (Boolean) versatoreConfigAziendaValue.get("controllaIdoneitaDocs");
            
            //parametri per il versamento 
            Map<String,Object> params = (Map<String,Object>) versatoreConfigAziendaValue.get("params");
            
            // richiama il metodo sul core che si occupa dell'accodamento del job
            queueAziendaJob(idAzienda, hostId, controllaIdoneitaArchivi, controllaIdoneitaDocs, params, app);
        }
    }
    
    /**
     * Accoda il job di controllo idoneità
     * @param idAzienda azienda per la quale il job lavorerà
     * @param hostId hostId del servizio di versamento da usare
     * @param controllaIdoneitaArchivi passare true se si vuole che il servizio controlli l'idoneità degli archivi
     * @param controllaIdoneitaDocs passare true se si vuole che il servizio controlli l'idoneità dei docs
     */
    private void queueAziendaJob(Integer idAzienda, String hostId, Boolean controllaIdoneitaArchivi, Boolean controllaIdoneitaDocs, Map<String,Object> params, String app) {
        IdoneitaCheckerJobWorkerData idoneitaCheckerJobWorkerData = new IdoneitaCheckerJobWorkerData(idAzienda, hostId, controllaIdoneitaArchivi, controllaIdoneitaDocs, params);
        IdoneitaCheckerJobWorker jobWorker = null;
        try { // istanzia il woker
            jobWorker = masterjobsObjectsFactory.getJobWorker(IdoneitaCheckerJobWorker.class, idoneitaCheckerJobWorkerData, false);
        } catch (Exception ex) {
            String errorMessage = "errore nella creazione del job IdoneitaChecker";
            log.error(errorMessage, ex);
        }
        try {
            /* accoda il worker mettendo nell'id_oggetto l'id dell'azienda, in modo che se dovessero esserci 2 job sulla stessa azienda
             * non vengano eseguiti in parallelo.
             * Il caso di 2 job sulla stessa azienda potrebbe capitare se si riavvia internauta mentre sta eseguendo il job:
             * ci sarebbe il job che era in esecuzione, che riprenderebbe da capo e l'eventuale nuovo job aggiunto dal servizio.
             * NB: il job, dopo dopo che controlla l'idoneità setto lo stato versamento sull'oggetto e non riprende in considerazione
             * quelli con lo stato settatto, per cui anche se riprende da capo, l'idoneità non viene ricontrollata su quelli già controllati
            */
            masterjobsJobsQueuer.queue(jobWorker, "idoneita_" + idAzienda, "Versatore", app, true, Set.SetPriority.NORMAL, null);
        } catch (Exception ex) {
            String errorMessage = "errore nell'accodamento del job IdoneitaChecker";
            log.error(errorMessage, ex);
        }
    }
}
