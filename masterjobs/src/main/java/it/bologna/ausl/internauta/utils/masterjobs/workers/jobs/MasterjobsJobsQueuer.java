package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsQueueData;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsUtils;
import it.bologna.ausl.internauta.utils.masterjobs.configuration.MasterjobsApplicationConfig;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsBadDataException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsRuntimeExceptionWrapper;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsJobsExecutionThread;
import it.bologna.ausl.internauta.utils.masterjobs.repository.JobReporitory;
import it.bologna.ausl.model.entities.masterjobs.Job;
import it.bologna.ausl.model.entities.masterjobs.JobNotified;
import it.bologna.ausl.model.entities.masterjobs.ObjectStatus;
import it.bologna.ausl.model.entities.masterjobs.QJob;
import it.bologna.ausl.model.entities.masterjobs.QObjectStatus;
import it.bologna.ausl.model.entities.masterjobs.QSet;
import it.bologna.ausl.model.entities.masterjobs.Set;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 * 
 * Classe che permette di accodare dei job per l'esecuzione dal parte del Masterjobs
 */
@Component
public class MasterjobsJobsQueuer {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsJobsQueuer.class);
    
    @Autowired
    private JobReporitory jobReporitory;

    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    @Qualifier(value = "redisMaterjobs")
    private RedisTemplate redisTemplate;
    
//    private PlatformTransactionManager transactionManager;
    
    @Autowired 
    private TransactionTemplate transactionTemplate;
    
    @Autowired
    private MasterjobsUtils masterjobsUtils;
    
    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;
    
    
    @Autowired
    private MasterjobsApplicationConfig masterjobsApplicationConfig;
    
    /**
     * Accoda i jobs solo dopo che la transazione in corso committa,
     * Se non sono presenti transazioni, i jobs vengono accodati subito
     * @param workers
     * @param objectId
     * @param objectType
     * @param app
     * @param waitForObject
     * @param priority
     * @param ip
     * @return i jobs che che sono stati creati
     */
    public MasterjobsQueueData queueOnCommit(List<JobWorker> workers, String objectId, String objectType, String app, Boolean waitForObject, Set.SetPriority priority, String ip) {
        MasterjobsQueueData queueData = transactionTemplate.execute(action -> {
            try {
                MasterjobsQueueData _queueData = this.insertInDatabase(workers, objectId, objectType, app, waitForObject, priority, ip);
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
                    @Override
                    public void afterCommit() {
                        TransactionSynchronization.super.afterCommit();
                        try {
                            insertInQueue(_queueData);
                        } catch (Exception ex) {
                            String errorMessage = String.format("error queuing job with object id %s and object type %s ", objectId, objectType);
                            log.error(errorMessage, ex);
                            throw new MasterjobsRuntimeExceptionWrapper(new MasterjobsQueuingException(errorMessage, ex));
                        }
                    }
                });
                return _queueData;
            } catch (MasterjobsBadDataException ex) {
                String errorMessage = String.format("error queuing job with object id %s and object type %s ", objectId, objectType);
                log.error(errorMessage, ex);
                throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
            }
        });
        return queueData;
    }
    
    /**
     * accoda dei jobs
     * @param workers l'elenco dei workers(normali o deferred), con i loro dati (WorkerData o WorkerDeferredData)
     * @param objectId l'id dell'oggetto al quale i jobs fanno riferimento. Può essere null
     * @param objectType il tipo dell'oggetto al quale i jobs fanno riferimento. Può essere null
     * @param app l'id dell'app al quale i jobs fanno riferimento. Può essere null
     * @param waitForObject se true, vuol dire che questi jobs dovranno aspettare l'effettiva esecuzione 
     * degli altri job per lo stesso oggetto. (l'oggetto viene indentificato per objectId, objectType (se presente), app(se presente)
     * se non viene passato almeno objectId, questo parametro non ha effetto
     * @param priority la priortà con il quale il job deve essere eseguito
     * @param notQueue indica di non accodare in redis i jobs e non committare dopo l'inserimento nel DB, serve per poter e far si che li possa inserire il chiamante dopo il commit
     * @return i jobs che che sono stati creati, possono servire nel caso si passi notQueue = true, per poterli inserire in redis tramite la funzione insertInQueue
     * @throws MasterjobsQueuingException nel caso ci sia un errore nell'inserimento in coda
     */
    public MasterjobsQueueData queue(List<JobWorker> workers, String objectId, String objectType, String app, Boolean waitForObject, Set.SetPriority priority, Boolean notQueue, String ip) throws MasterjobsQueuingException {
        if (!notQueue)
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        MasterjobsQueueData queueData = transactionTemplate.execute(action -> {
            try {
                return this.insertInDatabase(workers, objectId, objectType, app, waitForObject, priority, ip);
            } catch (MasterjobsBadDataException ex) {
                String errorMessage = String.format("error queuing job with object id %s and object type %s ", objectId, objectType);
                log.error(errorMessage, ex);
                throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
            }
        });
        if (!notQueue) {
            try {
                insertInQueue(queueData);
            } catch (Exception ex) {
                String errorMessage = String.format("error queuing job with object id %s and object type %s ", objectId, objectType);
                log.error(errorMessage, ex);
                throw new MasterjobsQueuingException(errorMessage, ex);
            }
        }
        return queueData;
    }
    public MasterjobsQueueData queue(JobWorker worker, String objectId, String objectType, String app, Boolean waitForObject, Set.SetPriority priority, String ip) throws MasterjobsQueuingException {
        return queue(worker, objectId, objectType, app, waitForObject, priority, false , false, ip);
    }
    
    public MasterjobsQueueData queue(JobWorker worker, String objectId, String objectType, String app, Boolean waitForObject, Set.SetPriority priority, Boolean skipIfAlreadyPresent, String ip) throws MasterjobsQueuingException {
        if (!skipIfAlreadyPresent || !isAlreadyPresent(worker)){
            return queue(Arrays.asList(worker), objectId, objectType, app, waitForObject, priority, false, ip);
        }
        return null;
    }
    
    public MasterjobsQueueData queue(JobWorker worker, String objectId, String objectType, String app, Boolean waitForObject, Set.SetPriority priority, Boolean skipIfAlreadyPresent, Boolean notQueue, String ip) throws MasterjobsQueuingException {
        if (!skipIfAlreadyPresent || !isAlreadyPresent(worker)){
            return queue(Arrays.asList(worker), objectId, objectType, app, waitForObject, priority, notQueue, ip);
        }
        return null;
    }
    
    /**
     * Aggiunge il worker passato nella tabella dei jobs notified senza aprire nessun altra transazione.Così facendo l'aggiunta vera e propria verrà effettuata al 
     * commit del chiamante.E' utile nel caso si voglia far partire il job dopo il commit e non subito.
     * @param worker
     * @param objectId
     * @param objectType
     * @param app
     * @param waitForObject
     * @param priority
     * @param skipIfAlreadyPresent
     * @throws it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException 
     */
    public void queueInJobsNotified(JobWorker worker, String objectId, String objectType, String app, Boolean waitForObject, Set.SetPriority priority, Boolean skipIfAlreadyPresent) throws MasterjobsQueuingException {
        queueInJobsNotified(Arrays.asList(worker), objectId, objectType, app, waitForObject, priority, skipIfAlreadyPresent);
    }
    
    /**
     * Aggiunge i workers passati nella tabella dei jobs notified senza aprire nessun altra transazione.Così facendo l'aggiunta vera e propria verrà effettuata al 
     * commit del chiamante.E' utile nel caso si voglia far partire il job dopo il commit e non subito.
     * @param workers
     * @param objectId
     * @param objectType
     * @param app
     * @param waitForObject
     * @param priority
     * @param skipIfAlreadyPresent
     * @throws it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException 
     */
    public void queueInJobsNotified(List<JobWorker> workers, String objectId, String objectType, String app, Boolean waitForObject, Set.SetPriority priority, Boolean skipIfAlreadyPresent) throws MasterjobsQueuingException {
        try {
            for (JobWorker worker : workers) {
                JobNotified jn = new JobNotified();
                jn.setObjectId(objectId);
                jn.setObjectType(objectType);
                jn.setJobName(worker.getName());
                jn.setJobData(worker.getData().toJobData(objectMapper));
                if (waitForObject != null)
                    jn.setWaitObject(waitForObject);
                jn.setApp(app);
                jn.setPriority(priority);
                jn.setSkipIfAlreadyPresent(skipIfAlreadyPresent);
                jn.setInsertedFrom(masterjobsApplicationConfig.getMachineIp());
                entityManager.persist(jn);
            }
        } catch (Exception ex) {
            throw new MasterjobsQueuingException("errore nell'inserimento dei workers nei jobs notified", ex);
        }
    }
    
    /**
     * crea un set di jobs e lo inserisce nella coda redis di esecuzione
     * @param queueData
     * @throws JsonProcessingException
     */
    public void insertInQueue(MasterjobsQueueData queueData) throws JsonProcessingException {
        redisTemplate.opsForList().rightPush(queueData.getQueue(), queueData.dump());
    }
    
    /**
     * inserisce i jobs e un set che li raggruppa nel database
     * @param workers
     * @param objectId
     * @param objectType
     * @param app
     * @param waitForObject
     * @param priority
     * @param ip
     * @return 
     * @throws it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsBadDataException 
     */
    public MasterjobsQueueData insertInDatabase(List<JobWorker> workers, String objectId, String objectType, String app, Boolean waitForObject, Set.SetPriority priority, String ip) throws MasterjobsBadDataException {
        
        Set set = new Set();
        if (StringUtils.hasText(ip)) {
            set.setInsertedFrom(ip);
        } else {
            set.setInsertedFrom(masterjobsApplicationConfig.getMachineIp());
        }
        if (objectId != null)
            set.setObjectId(objectId);
        if (objectType != null)
            set.setObjectType(objectType);
        if (app != null) {
            set.setApp(app);
        }
        if (waitForObject != null)
            set.setWaitObject(waitForObject);
        if (priority != null)
            set.setPriority(priority);
        
        log.info("persisting set...");
        entityManager.persist(set);
        log.info(String.format("created set %s", set.getId()));
             
        List<Job> jobs = new ArrayList<>(); 
        for (JobWorker worker : workers) {
            Job job = new Job();
            if (worker.getExecutableCheckEveryMillis() != null) {
                job.setExecutableCheckEveryMillis(worker.getExecutableCheckEveryMillis());
            }
            if (StringUtils.hasText(ip)) {
                job.setInsertedFrom(ip);
            } else {
                job.setInsertedFrom(masterjobsApplicationConfig.getMachineIp());
            }
            job.setDeferred(worker.isDeferred());
            JobWorkerDataInterface workerData = worker.getData();
            if (workerData != null)
                job.setData(workerData.toJobData(objectMapper));
            job.setName(worker.getName());
            job.setState(Job.JobState.READY);
            try {
                job.setHash(worker.calcolaMD5());
            } catch (Exception ex) {
                String error = "errore nel calcolo dell'hash md5 del job";
                log.error(error, ex);
                throw new MasterjobsBadDataException(error, ex);
            }
            job.setSet(set);
            log.info(String.format("persisting job %s", job.getName()));
            entityManager.persist(job);
            log.info(String.format("created job %s with id %s",job.getName(), job.getId()));
            jobs.add(job);
        }
        
        List<Long> jobsId = new ArrayList<>();
        for (Job job : jobs) {
            jobsId.add(job.getId());
        }
        
        String queue = masterjobsUtils.getQueueBySetPriority(priority);
        MasterjobsQueueData queueData = masterjobsObjectsFactory.buildMasterjobsQueueData(jobsId, set.getId(), queue);
        return queueData;
    }
    
    /**
     * Accoda tutti insieme i jobs passati, in un unica transazione
     * la transazione viene aperta nel metodo
     * @param descriptors la lista dei worker con relativi dati, da accodare
     * @param ip
     * @throws MasterjobsQueuingException 
     */
    public void queueMultiJobs(List<MultiJobQueueDescriptor> descriptors, String ip) throws MasterjobsQueuingException {
        List<MasterjobsQueueData> toQueue = new ArrayList();
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(action -> {
            for (MultiJobQueueDescriptor descriptor : descriptors) {
                try {
                    List<JobWorker> workers = descriptor.getWorkers();
                    if (descriptor.getSkipIfAlreadyPresent()) {
                        workers = new ArrayList<>();
                        log.info("checking isAlreadyPresent...");
                        for (JobWorker worker : descriptor.getWorkers()) {
                            if (!isAlreadyPresent(worker)) {
                                workers.add(worker);
                            }
                        }
                        log.info("done checking isAlreadyPresent");
                    }
                    log.info(String.format("inserting %s workers with objectId: %s objectType: %s app: %s waitForObject: %s priority: %s ip: %s...", workers.size(), descriptor.getObjectId(), descriptor.getObjectType(), descriptor.getApp(), descriptor.getWaitForObject(), descriptor.getPriority(), ip));
                    MasterjobsQueueData queueData = insertInDatabase(workers, descriptor.getObjectId(), descriptor.getObjectType(), descriptor.getApp(), descriptor.getWaitForObject(), descriptor.getPriority(), ip);
                    log.info("done insering workers, queueing...");
                    toQueue.add(queueData);
                    log.info("done queueing");
                } catch (MasterjobsBadDataException ex) {
                    String errorMessage = String.format("error queuing job with object id %s and object type %s ", descriptor.getObjectId(), descriptor.getObjectType());
                    log.error(errorMessage, ex);
                    throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
                }
            }
        });
        for (MasterjobsQueueData queueData : toQueue) {
            try {
                insertInQueue(queueData);
            } catch (Exception ex) {
                String errorMessage = String.format("error queuing job %s ", queueData);
                log.error(errorMessage, ex);
                throw new MasterjobsQueuingException(errorMessage, ex);
            }
        }
    }
    
    /**
     * Cancella tutte le code relative ai jobs
     */
    private void deleteAllJobsQueue() {        
        // prendo tutte le code di work (viene eseguito il comando redis keys masterjobsWork_*)
        java.util.Set workQueues = redisTemplate.keys(masterjobsApplicationConfig.getWorkQueue().replace("[thread_name]", "*"));
        if (workQueues != null && ! workQueues.isEmpty()) {
            for (Object workQueue : workQueues) {
                redisTemplate.delete(workQueue);
            }
        }
        redisTemplate.delete(masterjobsApplicationConfig.getWaitQueue());
        redisTemplate.delete(masterjobsApplicationConfig.getErrorQueue());
        redisTemplate.delete(masterjobsApplicationConfig.getInQueueNormal());
        redisTemplate.delete(masterjobsApplicationConfig.getInQueueHigh());
        redisTemplate.delete(masterjobsApplicationConfig.getInQueueHighest());
        
    }
    
    public void regenerateQueue() throws MasterjobsRuntimeExceptionWrapper {
        log.info("inizio riginerazione code...");
        
        log.info("metto in pausa tutti i threads");
        pauseThreads();
        
        // cancello tutte le code relative ai jobs, in quanto rigenererò tutto da capo a partire dai jobs nel database
        log.info("rimuovo tutte le code relative ai jobs...");
        deleteAllJobsQueue();
        
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(a -> {
            QSet qSet = QSet.set;
            QJob qJob = QJob.job;
            
            // setto tutto nello stato iniziale
            log.info("resetto tutti i jobs e gli object_status su DB...");
            resetJobsState(false);
            
            // prendo tutti i set e per ognuno, rigenero il json dei jobs e lo inserisco nella coda di esecuzione
            log.info("estraggo tutti i set dal DB...");
            JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
            List<Set> setToRegenerate = queryFactory
                .select(qSet)
                .from(qSet)
                .fetch();
            for (Set set : setToRegenerate) {
                log.info(String.format("processo il set %s, ne estraggo i job...", set.getId()));
                List<Long> jobsofSet = queryFactory.select(qJob.id)
                    .from(qJob)
                    .where(qJob.set.id.eq(set.getId()))
                    .fetch();
                
                // calcolo la coda di esecuzione in cui inserirlo in base a quanto indicato sul set
                String queue;
                try {
                    log.info("calcolo la coda in base alla priorità");
                    queue = masterjobsUtils.getQueueBySetPriority(set.getPriority());
                    log.info(String.format("coda estratta %s", queue));
                } catch (MasterjobsBadDataException ex) {
                    String errorMessage = String.format("errore nella rigenerazione del set %s", set.getId());
                    log.error(errorMessage, ex);
                    throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
                }
                
                // constuisco il json dei jobs del set
                log.info("constuisco il json dei jobs del set...");
                MasterjobsQueueData queueData = masterjobsObjectsFactory.buildMasterjobsQueueData(jobsofSet, set.getId(), queue);
                try {
                    
                    // inserisco il json nella coda di esecuzione
                    log.info("inserisco il json nella coda di esecuzione...");
                    insertInQueue(queueData);
                } catch (JsonProcessingException ex) {
                    String errorMessage = String.format("errore nell'inserimo del json del set %s", set.getId());
                    log.error(errorMessage, ex);
                    try {
                        if (queueData != null)
                            log.error(String.format("il josn è il seguente: %s", queueData.dump()));
                        else
                            log.error("queueData è null");
                    } catch (JsonProcessingException subEx) {
                        log.error("non sono riuscito a stampare il json", ex);
                    }
                    throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
                }
            }
        });
        
        log.info("riattivo tutti i threads");
        resumeThreads();
    }
    
    /**
     * Setta nello stato iniziale gli objectStatus e i jobs
     * Lo stato iniziale è IDLE per gli objectStatus e READY per i jobs
     * @param onlyInError se true resetta solo quelli in errore, se false, tutti
     */
    private void resetJobsState(boolean onlyInError) {
        QJob qJob = QJob.job;
        QObjectStatus qObjectStatus = QObjectStatus.objectStatus;

        // prima setto gli objectStatus nello stato IDLE usando la select for update
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
       
        JPAQuery<ObjectStatus> querySelect = queryFactory.query().setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .select(qObjectStatus)
            .from(qObjectStatus);
        if (onlyInError)
            querySelect = querySelect.where(qObjectStatus.state.eq(ObjectStatus.ObjectState.ERROR.toString()));
//        List<ObjectStatus> objectStatusList = querySelect.fetch();
        queryFactory
            .update(qObjectStatus)
            .set(qObjectStatus.state, ObjectStatus.ObjectState.IDLE.toString())
            .where(qObjectStatus.in(querySelect))
            .execute();
        
        // poi setto anche i job nello stato READY
        JPAUpdateClause querySet = queryFactory
            .update(qJob)
            .set(qJob.state, Job.JobState.READY);
        if (onlyInError)
            querySet = querySet.where(qJob.state.eq(Job.JobState.ERROR));
        querySet.execute();
    }
    
    /**
     * Rilancia i job in errore nelle loro code di appartenenza, per far si che ne venga ritentata l'esecuzione
     */
    public void relaunchJobsInError() {
//        log.info("pausing all threads");
//        pauseThreads();
        
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(a -> {
            /* 
            prima setto gli objectStatus in error nello stato IDLE usando la select for update e poi
            anche i job in error nello stato READY
            */
            resetJobsState(true);
        }); // committo
        
        /* 
        * Una volta che lato DB è tutto committato, allora sposto nella waitQueue tutto quello che c'è nella errorQueue.
        * Questo farà si che i threads che si occupano della waitQueue, smistino i job nelle loro code di appartenenza
        */
        while (redisTemplate.opsForList().move(
            masterjobsApplicationConfig.getErrorQueue(), RedisListCommands.Direction.LEFT, 
            masterjobsApplicationConfig.getWaitQueue(), RedisListCommands.Direction.RIGHT) != null) {}

//        log.info("resuming all threads");
//        resumeThreads();
    }
    
    private void pauseThreads() {
        Map commands = new HashMap();
        commands.put(MasterjobsJobsExecutionThread.COMMAND_KEY, MasterjobsJobsExecutionThread.PAUSE_COMMAND);
        redisTemplate.opsForStream().add(masterjobsApplicationConfig.getCommandsStreamName(), commands);
        while (redisTemplate.opsForHash().size(masterjobsApplicationConfig.getActiveThreadsSetName()) > 0) {
            log.info("waiting for all threads to pause");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                log.error("interrupt exception", ex);
            }
        }
    }
    
    private void resumeThreads() {
        Map commands = new HashMap();
        commands.put(MasterjobsJobsExecutionThread.COMMAND_KEY, MasterjobsJobsExecutionThread.RESUME_COMMAND);
        redisTemplate.opsForStream().add(masterjobsApplicationConfig.getCommandsStreamName(), commands);
    }
    
    public void stopThreads() {
        Map commands = new HashMap();
        commands.put(MasterjobsJobsExecutionThread.COMMAND_KEY, MasterjobsJobsExecutionThread.STOP_COMMAND);
        redisTemplate.opsForStream().add(masterjobsApplicationConfig.getCommandsStreamName(), commands);
    }

    private Boolean isAlreadyPresent(JobWorker worker) {
        UUID md5;
        try {
            log.info("inizio funzione calcolaMD5");
            md5 = worker.calcolaMD5();
            log.info("fine funzione calcolaMD5");
            QJob qJob = QJob.job;
            JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
            Integer one = queryFactory
                .selectOne()
                .from(qJob)
                .where(qJob.hash.eq(md5)
                    .and(
                        qJob.state.eq(Job.JobState.READY)
                        .or(qJob.state.eq(Job.JobState.ERROR))
                    )
                )
                .fetchOne();
            return one != null;
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            log.error("",ex);
            return false;
        }
    }
}
