package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.elaboralotto;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gusgus
 */
@MasterjobsWorker
public class ElaboraLottoJobWorker extends JobWorker<ElaboraLottoJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(ElaboraLottoJobWorker.class);
    private final String name = ElaboraLottoJobWorker.class.getSimpleName();
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in do doWork() di {}", getName());
        
        log.info("job finito!");
        return null;
    }
    
}
