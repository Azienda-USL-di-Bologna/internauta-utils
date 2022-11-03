package it.bologna.ausl.internauta.utils.masterjobs;

import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsThreadsManager;
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
        scheduledThreadPoolExecutor.shutdown();
        masterjobsThreadsManager.getExecutorService().shutdown();
        for (MasterjobsJobsExecutionThread masterjobsJobsExecutionThread : masterjobsThreadsManager.getMasterjobsJobsExecutionThreadsList()) {
            masterjobsJobsExecutionThread.executeCommand(MasterjobsJobsExecutionThread.STOP_COMMAND);
        }
        //DataSource dataSource = DbConnectionFactory.getDataSource();
        //if (dataSource != null) {}
        while (!terminated) {
            try {
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
