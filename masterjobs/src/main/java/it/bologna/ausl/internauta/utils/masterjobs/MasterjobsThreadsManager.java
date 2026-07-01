package it.bologna.ausl.internauta.utils.masterjobs;

import it.bologna.ausl.internauta.utils.masterjobs.configuration.MasterjobsApplicationConfig;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsHighPriorityJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsHighestPriorityJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsNormalPriorityJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsWaitQueueJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.services.MasterjobsServicesExecutionScheduler;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private MasterjobsObjectsFactory masterjobsObjectsFactory;
    
    @Autowired
    private MasterjobsJobsQueuer masterjobsJobsQueuer;

    @Autowired
    private MasterjobsWorkingThreadsRegistry workingThreadsRegistry;
    
    @Autowired
    @Qualifier(value = "redisMaterjobs")
    protected RedisTemplate redisTemplate;
    
    @PersistenceContext
    private EntityManager entityManager;
 
    @Autowired
    @Qualifier("masterjobsScheduledThreadPoolExecutor")
    private ScheduledThreadPoolExecutor scheduledExecutorService;
    
    private ExecutorService executorService;
    
    private final List<MasterjobsJobsExecutionThread> masterjobsJobsExecutionThreadsList = new ArrayList<>();

    /**
     * lancia tutti i threads esecutori dei jobs
     * @throws it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException
     */
    public void scheduleJobsExecutorThreads() throws MasterjobsWorkerException {
        
        // sposta le eventuali code di work rimaste appese nella wait queue, in modo che i job vengano ripresi in considerazione
        moveWorkQueueInWaitQueue();
        Long errorQueueSize = redisTemplate.opsForList().size(masterjobsApplicationConfig.getErrorQueue());
        if (errorQueueSize != null && errorQueueSize > 0) {
            log.info(String.format("found %s jobs in error, relaunch...", errorQueueSize));
            masterjobsJobsQueuer.relaunchJobsInError();
        }
        
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

        // avvio l'alive checker: mantiene vive (TTL) le chiavi dei set in esecuzione su questa istanza
        scheduleWorkingThreadsAliveChecker();
    }

    /**
     * Schedula l'alive checker: periodicamente rinfresca il TTL delle chiavi Redis dei set
     * attualmente in esecuzione su questa istanza, così i set vivi restano distinguibili dagli orfani.
     */
    private void scheduleWorkingThreadsAliveChecker() {
        int aliveCheckerMillis = masterjobsApplicationConfig.getAliveCheckerMillis();
        scheduledExecutorService.scheduleAtFixedRate(
            () -> {
                // il try/catch evita che un errore transitorio annulli il task periodico (scheduleAtFixedRate)
                try {
                    workingThreadsRegistry.aliveCheck();
                } catch (Exception ex) {
                    log.error("errore nell'alive checker dei working threads", ex);
                }
            },
            aliveCheckerMillis, aliveCheckerMillis, TimeUnit.MILLISECONDS);
    }
    
    /**
     * lancia i treads dei servizi
     * @throws MasterjobsWorkerException 
     */
    public void scheduleServiceExecutorThreads() throws MasterjobsWorkerException {
        // schedula i ServiceThreads attivi
        MasterjobsServicesExecutionScheduler masterjobsServicesExecutionScheduler = new MasterjobsServicesExecutionScheduler(entityManager, masterjobsObjectsFactory, scheduledExecutorService);
//        masterjobsServicesExecutionScheduler.scheduleUpdateServiceDetector();
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
    private void moveWorkQueueInWaitQueue() {
        // work queue dei thread vivi (job in corso su questa o altre istanze): non vanno recuperate, altrimenti si duplicherebbe il job
        Set<String> liveWorkQueues = new HashSet<>();
        for (String uniqueName : workingThreadsRegistry.getLiveThreadUniqueNames()) {
            liveWorkQueues.add(masterjobsApplicationConfig.getWorkQueue().replace("[thread_name]", uniqueName));
        }
        // prendo tutte le code di work (viene eseguito il comando redis keys masterjobsWork_*)
        Set workQueues = redisTemplate.keys(masterjobsApplicationConfig.getWorkQueue().replace("[thread_name]", "*"));
        if (workQueues != null && ! workQueues.isEmpty()) {
            // per ogni coda di work trovata, sposto tutti gli elementi nella waitQueue
            for (Object workQueue : workQueues) {
                // salto le work queue dei thread vivi: recupero (sposto in waitQueue) solo le orfane
                if (!liveWorkQueues.contains(workQueue.toString())) {
                    // quando nella coda di work non c'è più nulla la move torna null
                    while ( redisTemplate.opsForList().move(
                            workQueue, RedisListCommands.Direction.LEFT,
                            masterjobsApplicationConfig.getWaitQueue(), RedisListCommands.Direction.RIGHT) != null) {};
                }
            }
        }
    }
}
