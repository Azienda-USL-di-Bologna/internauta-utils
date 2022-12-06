package it.bologna.ausl.internauta.utils.masterjobs.workers.services;

import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore.VersatoreJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore.VersatoreJobWorkerData;
import it.bologna.ausl.model.entities.masterjobs.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class VersatoreServiceCore {
    private static final Logger log = LoggerFactory.getLogger(VersatoreServiceCore.class);
    
    private final MasterjobsJobsQueuer masterjobsJobsQueuer;
    private final MasterjobsObjectsFactory masterjobsObjectsFactory;

    public VersatoreServiceCore(MasterjobsJobsQueuer masterjobsJobsQueuer, MasterjobsObjectsFactory masterjobsObjectsFactory) {
        this.masterjobsJobsQueuer = masterjobsJobsQueuer;
        this.masterjobsObjectsFactory = masterjobsObjectsFactory;
    }
    
    public void queueVersatoreJob(String hostId, Boolean forzatura, Integer idPersonaForzatura, Integer poolsize, String app) {
        
        VersatoreJobWorkerData versatoreJobWorkerData = new VersatoreJobWorkerData(hostId, forzatura, poolsize, idPersonaForzatura);
        VersatoreJobWorker jobWorker = null;
        try {
            jobWorker = masterjobsObjectsFactory.getJobWorker(VersatoreJobWorker.class, versatoreJobWorkerData, false);
        } catch (Exception ex) {
            String errorMessage = "errore nella creazione del job Versatore";
            log.error(errorMessage, ex);
        }
        try {
            masterjobsJobsQueuer.queue(jobWorker, "versatore", "Versatore", app, false, Set.SetPriority.NORMAL);
        } catch (Exception ex) {
            String errorMessage = "errore nell'accodamento del job Versatore";
            log.error(errorMessage, ex);
        }
    }
}
