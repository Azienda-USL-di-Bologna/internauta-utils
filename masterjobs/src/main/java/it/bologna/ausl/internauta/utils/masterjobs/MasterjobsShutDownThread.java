package it.bologna.ausl.internauta.utils.masterjobs;

import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsJobsExecutionThread;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 * 
 * Classe che rappresenta un thread che si occupa di terminare tutti i threads di masterjobs allo spegnimento dell'applicazione
 */
@Component
public class MasterjobsShutDownThread extends Thread {
    private static Logger log = LoggerFactory.getLogger(MasterjobsShutDownThread.class);
    
    @Autowired
    @Qualifier("masterjobsScheduledThreadPoolExecutor")
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    
    @Autowired
    private MasterjobsThreadsManager masterjobsThreadsManager;
    
    @Override
    public void run() {
        log.info("Masterjobs shutdown initiated");
        boolean terminated = false;
        /* come prima cosa spegne il thread pool dei servizi e degli executor, questo fa in modo che quelli attivi finiscano il loro lavoro, 
        * ma non ne vengono schedulati altri
        */
        scheduledThreadPoolExecutor.shutdown();
        masterjobsThreadsManager.getExecutorService().shutdown();
        
        // poi manda un comando di stop a tutti gli executor in modo da fargli terminare, altrimenti non terminerebbero
        for (MasterjobsJobsExecutionThread masterjobsJobsExecutionThread : masterjobsThreadsManager.getMasterjobsJobsExecutionThreadsList()) {
            masterjobsJobsExecutionThread.executeCommand(MasterjobsJobsExecutionThread.STOP_COMMAND);
        }
        while (!terminated) {
            try {
                // attende la terminazione per 5 minuti, dopo di che la forza
                terminated = 
                        scheduledThreadPoolExecutor.awaitTermination(5, TimeUnit.MINUTES) && 
                        masterjobsThreadsManager.getExecutorService().awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException ex) {
                log.warn("Masterjobs shutdown thread interrupted while waiting for threads termination");
            }
        }
        if (terminated == true) {
            log.info("ALL masterjobs executor threads are gone");
        } else {
            log.error("timeout waiting for masterjobs thread exit!");
        }

    }
}
