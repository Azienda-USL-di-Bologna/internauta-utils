package it.bologna.ausl.internauta.utils.masterjobs;

import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsQueueData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.masterjobs.configuration.MasterjobsApplicationConfig;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.workers.Worker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerDataInterface;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerDeferredData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 * 
 * classe utility che permetti di istanziare vari oggetti del masterjobs
 */
@Component
public class MasterjobsObjectsFactory {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsObjectsFactory.class);

    @Autowired
    private BeanFactory beanFactory;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private MasterjobsApplicationConfig masterjobsApplicationConfig;
    
    // mappa che ha come chiave il nome del Worker (il nome è quello che ritorna la getName e come valore la classe del worker
    @Autowired
    private Map<String, Class<? extends Worker>> workerMap;
    
    /**
     * construisce l'oggetto MasterjobsQueueData dalla stringa passata (che è quella letta da redis)
     * @param data la stringa dalla qualle costruire MasterjobsQueueData
     * @return l'oggetto MasterjobsQueueData
     * @throws JsonProcessingException 
     */
    public MasterjobsQueueData getMasterjobsQueueDataFromString(String data) throws JsonProcessingException {
        MasterjobsQueueData masterjobsQueueData = this.objectMapper.readValue(data, MasterjobsQueueData.class);
        masterjobsQueueData.setObjectMapper(objectMapper);
        return masterjobsQueueData;
    }

    /**
     * Crea un oggetto MasterjobsQueueData con i dati passati
     * @param jobsId lista dei jobId da inserire
     * @param setId il set a cui i job fanno riferimento
     * @param queue la coda in cui la QueueData andrà inserita
     * @return 
     */
    public MasterjobsQueueData buildMasterjobsQueueData(List<Long> jobsId, Long setId, String queue) {
        MasterjobsQueueData queueData = new MasterjobsQueueData(objectMapper);
        queueData.setJobs(jobsId);
        queueData.setSet(setId);
        queueData.setQueue(queue);
        return queueData;
    }

    /**
     * Costruisce il bean di un executor
     * @param <T>
     * @param classz la classe dell'executor da costruire
     * @return 
     */
    public <T extends MasterjobsJobsExecutionThread> T getJobsExecutionThreadObject(Class<T> classz) {
        T executionThreadObject = beanFactory.getBean(classz);
        executionThreadObject
            .activeThreadsSetName(masterjobsApplicationConfig.getActiveThreadsSetName())
            .commandsStreamName(masterjobsApplicationConfig.getCommandsStreamName())
            .inQueueNormal(masterjobsApplicationConfig.getInQueueNormal())
            .inQueueHigh(masterjobsApplicationConfig.getInQueueHigh())
            .inQueueHighest(masterjobsApplicationConfig.getInQueueHighest())
            .workQueue(masterjobsApplicationConfig.getWorkQueue())
            .errorQueue(masterjobsApplicationConfig.getErrorQueue())
            .waitQueue(masterjobsApplicationConfig.getWaitQueue())
            .sleepMillis(masterjobsApplicationConfig.getSleepMillis())
            .queueReadTimeoutMillis(masterjobsApplicationConfig.getQueueReadTimeoutMillis())
            .useDebuggingOptions(masterjobsApplicationConfig.isUseDebuggingOptions())
            .ip(masterjobsApplicationConfig.getMachineIp())
            .self(executionThreadObject);
        return executionThreadObject;
    }
    
    /**
     * Torna un JobWorker del nome passato, costruito con i dati passati
     * @param name il nome del JobWorker (quello che viene tornato dal metodo getName())
     * @param workerData i dati del job (JobWorkerDeferredData se il job è deferred JobWorkerData se non lo è)
     * @param deferred "true" se il job è deferred, "false" altrimenti
     * @return un JobWorker con del nome passato, costruito con i dati passati
     * @throws it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException
     */
    public JobWorker getJobWorker(String name, JobWorkerDataInterface workerData, boolean deferred) throws MasterjobsWorkerException {
        Class<? extends JobWorker> jobWorkerClass = (Class<? extends JobWorker>)workerMap.get(name);
        JobWorker worker = getJobWorker(jobWorkerClass, workerData, deferred);
        return worker;
    }
    
    /**
     * Torna un JobWorker della classe passata, costruito con i dati passati
     * @param <T>
     * @param jobWorkerClass la classe concreta del JobWorker desiderato
     * @param workerData i dati del job (JobWorkerDeferredData se il job è deferred JobWorkerData se non lo è)
     * @param deferred "true" se il job è deferred, "false" altrimenti
     * @return un JobWorker della classe passata, costruito con i dati passati
     * @throws it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException
     */
    public <T extends JobWorker> T getJobWorker(Class<T> jobWorkerClass, JobWorkerDataInterface workerData, boolean deferred) throws MasterjobsWorkerException {
        T worker = beanFactory.getBean(jobWorkerClass);
        if (deferred) {
            worker.buildDeferred((JobWorkerDeferredData) workerData);
        } else {
            worker.build((JobWorkerData) workerData);
        }
        MasterjobsJobsQueuer masterjobsJobsQueuer = beanFactory.getBean(MasterjobsJobsQueuer.class);
        worker.init(this, masterjobsJobsQueuer, masterjobsApplicationConfig.isUseDebuggingOptions(), masterjobsApplicationConfig.getMachineIp(), masterjobsApplicationConfig.getHttpPort());
        return worker;
    }
    
    /**
     * Torna un ServiceWorker del nome passato
     * @param name il nome del ServiceWorker (quello che viene tornato dal metodo getName())
     * @return un ServiceWorker del nome passato
     * @throws it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException
     */
    public ServiceWorker getServiceWorker(String name) throws MasterjobsWorkerException {
        Class<? extends Worker> serviceWorkerClass = workerMap.get(name);
        ServiceWorker serviceWorker = (ServiceWorker) beanFactory.getBean(serviceWorkerClass);
        MasterjobsJobsQueuer masterjobsJobsQueuer = beanFactory.getBean(MasterjobsJobsQueuer.class);
        serviceWorker.init(this, masterjobsJobsQueuer, masterjobsApplicationConfig.isUseDebuggingOptions(), masterjobsApplicationConfig.getMachineIp(), masterjobsApplicationConfig.getHttpPort());
        return serviceWorker;
    }
}
