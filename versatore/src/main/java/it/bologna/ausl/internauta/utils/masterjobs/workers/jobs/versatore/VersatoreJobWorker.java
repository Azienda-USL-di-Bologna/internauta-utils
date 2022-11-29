package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class VersatoreJobWorker extends JobWorker {
    private static final Logger log = LoggerFactory.getLogger(VersatoreJobWorker.class);
    
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in doWork()");
        return null;
    }
    
}
