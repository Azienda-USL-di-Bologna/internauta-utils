package it.bologna.ausl.internauta.utils.masterjobs.workers;

import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author gdm
 * 
 */
public abstract class Worker {  
    
    @PersistenceContext
    protected EntityManager entityManager;
    
    @Autowired
    protected TransactionTemplate transactionTemplate;    
    
    protected MasterjobsObjectsFactory masterjobsObjectsFactory;
    protected MasterjobsJobsQueuer masterjobsJobsQueuer;
    
    public abstract WorkerResult doWork() throws MasterjobsWorkerException;

    /**
     * torna il nome del job/service che il worker sa eseguire.
     * Questo nome è quello con cui sarà scritto in tabella e sarà usato per individuarlo dalla tabella nel momento in cui deve
     * essere eseguito
     * @return il nome del job/service che il worker sa eseguire.
     */
    public abstract String getName();
    
    /**
     * eseguire l'inizializzazione, nello specifico pass i bean MasterjobsObjectsFactory e MasterjobsJobsQueuer.
     * Eventualmente si può estendere per eseguire alche altra inizializzazione
     * @param masterjobsObjectsFactory
     * @param masterjobsJobsQueuer
     * @throws it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException
     */
    public void init(MasterjobsObjectsFactory masterjobsObjectsFactory, MasterjobsJobsQueuer masterjobsJobsQueuer) throws MasterjobsWorkerException {
        this.masterjobsObjectsFactory = masterjobsObjectsFactory;
        this.masterjobsJobsQueuer = masterjobsJobsQueuer;
    }
}
