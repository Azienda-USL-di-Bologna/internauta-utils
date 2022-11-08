package it.bologna.ausl.internauta.utils.masterjobs;

import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsHighPriorityJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsHighestPriorityJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsNormalPriorityJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsWaitQueueJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.services.MasterjobsServicesExecutionScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class MasterjobsThreadsManager {
    
    private static Logger log = LoggerFactory.getLogger(MasterjobsThreadsManager.class);
    
    @Value("${masterjobs.manager.jobs-executor.normal-priority-threads-number}")
    private int normalPriorityThreadsNumber;
    
    @Value("${masterjobs.manager.jobs-executor.high-priority-threads-number}")
    private int highPriorityThreadsNumber;
    
    @Value("${masterjobs.manager.jobs-executor.highest-priority-threads-number}")
    private int highestPriorityThreadsNumber;
    
    @Value("${masterjobs.manager.jobs-executor.wait-queue-threads-number}")
    private Integer waitQueueThreadsNumber;
    
    @Autowired
    private MasterjobsServicesExecutionScheduler masterjobsServicesExecutionScheduler;
    
    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;
    
    private ExecutorService executorService;
    
    private final List<MasterjobsJobsExecutionThread> masterjobsJobsExecutionThreadsList = new ArrayList<>();

//    @Autowired
//    TransactionTemplate transactionTemplate;
    
    /**
     * lancia tutti i threads del Masterjobs
     * @throws it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException
     */
    public void scheduleThreads() throws MasterjobsWorkerException {
        
        // schedula gli ExecutionThreads per tutte le priorità e per la wait queue
        executorService = Executors.newFixedThreadPool(
                normalPriorityThreadsNumber + 
                highPriorityThreadsNumber + 
                highestPriorityThreadsNumber +
                waitQueueThreadsNumber);
//        executorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
//        executorService.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        scheduleExecutionThreads(normalPriorityThreadsNumber, executorService, MasterjobsNormalPriorityJobsExecutionThread.class);
        scheduleExecutionThreads(highPriorityThreadsNumber, executorService, MasterjobsHighPriorityJobsExecutionThread.class);
        scheduleExecutionThreads(highestPriorityThreadsNumber, executorService, MasterjobsHighestPriorityJobsExecutionThread.class);
        scheduleExecutionThreads(waitQueueThreadsNumber, executorService, MasterjobsWaitQueueJobsExecutionThread.class);
        
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
}
