package it.bologna.ausl.internauta.utils.masterjobs.workers;

import it.bologna.ausl.internauta.utils.masterjobs.DebuggingOptionsManager;
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
    
    @Autowired
    protected DebuggingOptionsManager debuggingOptionsManager;
    
    protected MasterjobsObjectsFactory masterjobsObjectsFactory;
    protected MasterjobsJobsQueuer masterjobsJobsQueuer;
    
    protected boolean debuggingOptions = false;
    protected String ip;
    protected Integer port;
    
    public abstract WorkerResult doWork() throws MasterjobsWorkerException;

    /**
     * torna il nome del job/service che il worker sa eseguire.
     * Questo nome è quello con cui sarà scritto in tabella e sarà usato per individuarlo dalla tabella nel momento in cui deve
     * essere eseguito
     * @return il nome del job/service che il worker sa eseguire.
     */
    public abstract String getName();
    
    /**
     * eseguire l'inizializzazione, nello specifico pass i bean MasterjobsObjectsFactory e MasterjobsJobsQueuer.Eventualmente si può estendere per eseguire alche altra inizializzazione
     * @param masterjobsObjectsFactory
     * @param masterjobsJobsQueuer
     * @param debuggingOptions
     * @param ip
     * @param port
     * @throws it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException
     */
    public void init(MasterjobsObjectsFactory masterjobsObjectsFactory, MasterjobsJobsQueuer masterjobsJobsQueuer, boolean debuggingOptions, String ip, Integer port) throws MasterjobsWorkerException {
        this.masterjobsObjectsFactory = masterjobsObjectsFactory;
        this.masterjobsJobsQueuer = masterjobsJobsQueuer;
        this.debuggingOptions = debuggingOptions;
        this.ip = ip;
        this.port = port;
    }
}
