package it.bologna.ausl.internauta.utils.masterjobs.workers;

import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;

/**
 *
 * @author gdm
 * 
 */
public interface Worker {
    
    public WorkerResult doWork() throws MasterjobsWorkerException;

    /**
     * torna il nome del job/service che il worker sa eseguire.
     * Questo nome è quello con cui sarà scritto in tabella e sarà usato per individuarlo dalla tabella nel momento in cui deve
     * essere eseguito
     * @return il nome del job/service che il worker sa eseguire.
     */
    public abstract String getName();
}
