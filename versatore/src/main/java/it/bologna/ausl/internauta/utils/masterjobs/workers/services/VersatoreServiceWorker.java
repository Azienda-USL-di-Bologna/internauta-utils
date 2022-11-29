package it.bologna.ausl.internauta.utils.masterjobs.workers.services;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class VersatoreServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(VersatoreServiceWorker.class);

    private String name = VersatoreServiceWorker.class.getSimpleName();
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        log.info("sono il VersatoreServiceWorker e sto funzionando...");
        
        
        return null;
    }
}
