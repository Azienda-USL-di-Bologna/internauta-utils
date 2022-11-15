package it.bologna.ausl.internauta.utils.masterjobs;

import it.bologna.ausl.internauta.utils.masterjobs.configuration.MasterjobsApplicationConfig;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsHighPriorityJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsHighestPriorityJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsNormalPriorityJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsWaitQueueJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.services.MasterjobsServicesExecutionScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 * 
 * Si occupa di far partire tutti i threads degli executor, per tutte le priorità e per la coda di wait
 */
@Component
public class MasterjobsThreadsManager {
    
    private static Logger log = LoggerFactory.getLogger(MasterjobsThreadsManager.class);    
    
    @Autowired
    private MasterjobsApplicationConfig masterjobsApplicationConfig;
    
    @Autowired
    private MasterjobsServicesExecutionScheduler masterjobsServicesExecutionScheduler;
    
    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;
    
    @Autowired
    @Qualifier(value = "redisMaterjobs")
    protected RedisTemplate redisTemplate;
    
    private ExecutorService executorService;
    
    private final List<MasterjobsJobsExecutionThread> masterjobsJobsExecutionThreadsList = new ArrayList<>();

    /**
     * lancia tutti i threads del Masterjobs
     * @throws it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException
     */
    public void scheduleThreads() throws MasterjobsWorkerException {
        
        moveWorkQueueinWaitQueue();
        
        // schedula gli ExecutionThreads per tutte le priorità e per la wait queue
        executorService = Executors.newFixedThreadPool(
                masterjobsApplicationConfig.getNormalPriorityThreadsNumber() + 
                masterjobsApplicationConfig.getHighPriorityThreadsNumber() + 
                masterjobsApplicationConfig.getHighestPriorityThreadsNumber() +
                masterjobsApplicationConfig.getWaitQueueThreadsNumber());
//        executorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
//        executorService.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        scheduleExecutionThreads(masterjobsApplicationConfig.getNormalPriorityThreadsNumber(), executorService, MasterjobsNormalPriorityJobsExecutionThread.class);
        scheduleExecutionThreads(masterjobsApplicationConfig.getHighPriorityThreadsNumber(), executorService, MasterjobsHighPriorityJobsExecutionThread.class);
        scheduleExecutionThreads(masterjobsApplicationConfig.getHighestPriorityThreadsNumber(), executorService, MasterjobsHighestPriorityJobsExecutionThread.class);
        scheduleExecutionThreads(masterjobsApplicationConfig.getWaitQueueThreadsNumber(), executorService, MasterjobsWaitQueueJobsExecutionThread.class);
        
        // schedula i ServiceThreads attivi
        masterjobsServicesExecutionScheduler.scheduleUpdateServiceDetector();
        masterjobsServicesExecutionScheduler.scheduleServiceThreads();
    }
    
    private void scheduleExecutionThreads(int threadsNumber, ExecutorService executor, Class<? extends MasterjobsJobsExecutionThread> classz) {
        for (int i = 0; i < threadsNumber; i++) {
            MasterjobsJobsExecutionThread executionThreadObject = masterjobsObjectsFactory.getJobsExecutionThreadObject(classz);
            executor.execute(executionThreadObject);
            masterjobsJobsExecutionThreadsList.add(executionThreadObject);
        }
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public List<MasterjobsJobsExecutionThread> getMasterjobsJobsExecutionThreadsList() {
        return masterjobsJobsExecutionThreadsList;
    }
    
    /**
     * Viene chiamata prima dell'avvio dei threads.
     * Sposta i job che erano rimasti in esecuzione nelle work queue nella coda di wait, 
     * in modo che vengano smistati nelle loro code di appartenenza
    */
    private void moveWorkQueueinWaitQueue() {
        // prendo tutte le code di work (viene eseguito il comando redis keys masterjobsWork_*)
        Set workQueues = redisTemplate.keys(masterjobsApplicationConfig.getWorkQueue().replace("[thread_name]", "*"));
        if (workQueues != null && ! workQueues.isEmpty()) {
            // per ogni coda di work trovata, sposto tutti gli elementi nella waitQueue
            for (Object workQueue : workQueues) {
                // quando nella coda di work non c'è più nulla la move torna null
                while ( redisTemplate.opsForList().move(
                        workQueue, RedisListCommands.Direction.LEFT, 
                        masterjobsApplicationConfig.getWaitQueue(), RedisListCommands.Direction.RIGHT) != null) {};
            }
        }
    }
}
