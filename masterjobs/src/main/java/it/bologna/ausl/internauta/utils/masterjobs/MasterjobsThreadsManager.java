package it.bologna.ausl.internauta.utils.masterjobs;

import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsHighPriorityJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsHighestPriorityJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsNormalPriorityJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsWaitQueueJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.executors.services.MasterjobsServicesExecutionScheduler;
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
    
    @Value("${masterjobs.manager.normal-priority-threads-number}")
    private int normalPriorityThreadsNumber;
    
    @Value("${masterjobs.manager.high-priority-threads-number}")
    private int highPriorityThreadsNumber;
    
    @Value("${masterjobs.manager.highest-priority-threads-number}")
    private int highestPriorityThreadsNumber;
    
    @Value("${masterjobs.manager.wait-queue-threads-number}")
    private Integer waitQueueThreadsNumber;
    
    @Autowired
    private BeanFactory beanFactory;
    
    @Autowired
    private MasterjobsServicesExecutionScheduler masterjobsServicesExecutionScheduler;
    
    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;

//    @Autowired
//    TransactionTemplate transactionTemplate;
    
    /**
     * lancia tutti i threads del Masterjobs
     */
    public void scheduleThreads(){
        
        // schedula gli ExecutionThreads per tutte le priorità e per la wait queue
        ExecutorService executor = Executors.newFixedThreadPool(
                normalPriorityThreadsNumber + 
                highPriorityThreadsNumber + 
                highestPriorityThreadsNumber +
                waitQueueThreadsNumber);
        scheduleExecutionThreads(normalPriorityThreadsNumber, executor, MasterjobsNormalPriorityJobsExecutionThread.class);
        scheduleExecutionThreads(highPriorityThreadsNumber, executor, MasterjobsHighPriorityJobsExecutionThread.class);
        scheduleExecutionThreads(highestPriorityThreadsNumber, executor, MasterjobsHighestPriorityJobsExecutionThread.class);
        scheduleExecutionThreads(waitQueueThreadsNumber, executor, MasterjobsWaitQueueJobsExecutionThread.class);
        
        // schedula i ServiceThreads attivi
        masterjobsServicesExecutionScheduler.scheduleServiceThreads();
    }
    
    private void scheduleExecutionThreads(int threadsNumber, ExecutorService executor, Class<? extends MasterjobsJobsExecutionThread> classz) {
        for (int i = 0; i < threadsNumber; i++) {
            MasterjobsJobsExecutionThread executionThreadObject = masterjobsObjectsFactory.getJobsExecutionThreadObject(classz);
            executor.execute(executionThreadObject);
        }
    }
}
