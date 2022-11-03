package it.bologna.ausl.internauta.utils.masterjobs.executors.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsQueueData;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsUtils;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsBadDataException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsDataBaseException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsExecutionThreadsException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsInterruptException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsParsingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsReadQueueTimeout;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.model.entities.masterjobs.Job;
import it.bologna.ausl.model.entities.masterjobs.ObjectStatus;
import it.bologna.ausl.model.entities.masterjobs.QJob;
import it.bologna.ausl.model.entities.masterjobs.QObjectStatus;
import it.bologna.ausl.model.entities.masterjobs.QSet;
import it.bologna.ausl.model.entities.masterjobs.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerDataInterface;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;

/**
 *
 * @author gdm
 */
public abstract class MasterjobsJobsExecutionThread implements Runnable, MasterjobsJobsExecutionThreadBuilder {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsJobsExecutionThread.class);
    private static final String COMMAND_KEY = "command";
    public static final String STOP_COMMAND = "stop";
    public static final String PAUSE_COMMAND = "freeze";
    public static final String RESUME_COMMAND = "resume";
    
    @PersistenceContext
    protected EntityManager entityManager;
    
    @Autowired
    @Qualifier(value = "redisMaterjobs")
    protected RedisTemplate redisTemplate;
    
    @Autowired
    protected TransactionTemplate transactionTemplate;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected MasterjobsObjectsFactory masterjobsObjectsFactory;
    
    @Autowired
    protected MasterjobsUtils masterjobsUtils;
    
    protected MasterjobsJobsExecutionThread self;
    
    protected String activeThreadsSetName;
    protected String commandsStreamName;
    protected Integer commandsExpireSeconds;
    protected String inQueueNormal;
    protected String inQueueHigh;
    protected String inQueueHighest;
    protected String workQueue;
    protected String errorQueue;
    protected String waitQueue;
    protected String outQueue;
    protected int sleepMillis;
    protected int queueReadTimeoutMillis;

    protected boolean stopped = false;
    protected boolean paused = false;

    private String lastStreamId = "0";
    private String currentCommand = null;
    
    @Override
    public MasterjobsJobsExecutionThread self(MasterjobsJobsExecutionThread self) {
        this.self = self;
        return this;
    }
    
    @Override
    public MasterjobsJobsExecutionThread activeThreadsSetName(String activeThreadsSetName) {
        this.activeThreadsSetName = activeThreadsSetName;
        return this;
    }
    
    @Override
    public MasterjobsJobsExecutionThread commandsStreamName(String commandsStreamName) {
        this.commandsStreamName = commandsStreamName;
        return this;
    }
    
    @Override
    public MasterjobsJobsExecutionThread inQueueNormal(String inQueueNormal) {
        this.inQueueNormal = inQueueNormal;
        return this;
    }
    
    @Override
    public MasterjobsJobsExecutionThread inQueueHigh(String inQueueHigh) {
        this.inQueueHigh = inQueueHigh;
        return this;
    }
    
    @Override
    public MasterjobsJobsExecutionThread inQueueHighest(String inQueueHighest) {
        this.inQueueHighest = inQueueHighest;
        return this;
    }

    @Override
    public MasterjobsJobsExecutionThread workQueue(String workQueue) {
        this.workQueue = workQueue;
        return this;
    }
    
    @Override
    public MasterjobsJobsExecutionThread errorQueue(String errorQueue) {
        this.errorQueue = errorQueue;
        return this;
    }

    @Override
    public MasterjobsJobsExecutionThread waitQueue(String waitQueue) {
        this.waitQueue = waitQueue;
        return this;
    }

    @Override
    public MasterjobsJobsExecutionThread outQueue(String outQueue) {
        this.outQueue = outQueue;
        return this;
    }

    @Override
    public MasterjobsJobsExecutionThread sleepMillis(int sleepMillis) {
        this.sleepMillis = sleepMillis;
        return this;
    }

    @Override
    public MasterjobsJobsExecutionThread queueReadTimeoutMillis(int queueReadTimeoutMillis) {
        this.queueReadTimeoutMillis = queueReadTimeoutMillis;
        return this;
    }
    
    /**
     * da implementare tornando a quale coda è affine l'executor
     * i possibili valori sono: inQueueNormal, inQueueHigh, inQueueHighest, waitQueue
     * @return priorità alla quale è affine l'executor 
     */
    public abstract String getQueueAffinity();
    
    /**
     * da implementare con l'esecuzione dei job vera e propria
     * @throws MasterjobsInterruptException 
     */
    public abstract void runExecutor() throws MasterjobsInterruptException;
    
    /**
     * Viene chiamata all'avvio del thread.
     * Sposta il job che era rimasto in esecuzione nella coda alla quale il thread è affine
     */
    protected void moveWorkQueueinInQueue() {
        redisTemplate.opsForList().move(
            this.workQueue, RedisListCommands.Direction.LEFT, 
            getQueueAffinity(), RedisListCommands.Direction.RIGHT);
    }
    
    /**
     * inserisce il riferimento del thread nella mappa dei threads attivi
     */
    protected void insertInActiveThreadsSet() {
        redisTemplate.opsForHash().put(activeThreadsSetName, String.valueOf(Thread.currentThread().getId()), getUniqueName());
    }
    
    /**
     * inserisce il riferimento del thread nella mappa dei threads attivi
     */
    protected void clearInActiveThreadsSet() {
        redisTemplate.delete(activeThreadsSetName);
    }
    
    /**
     * rimuove il riferimento del thread nella mappa dei threads attivi
     */
    protected void removeFromActiveThreadsSet() {
        redisTemplate.opsForHash().delete(activeThreadsSetName, String.valueOf(Thread.currentThread().getId()));
        if (redisTemplate.opsForHash().size(activeThreadsSetName) == 0) {
            redisTemplate.delete(commandsStreamName);
        }
    }
    
    @Override
    public void run() {
        while (!stopped && !paused) {
//            boolean isInterrupted = Thread.currentThread().isInterrupted();
            insertInActiveThreadsSet();
            try {
                log.info(String.format("executor %s started", getUniqueName()));
                buildWorkQueue();
                moveWorkQueueinInQueue();
                checkCommand();
                transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                transactionTemplate.executeWithoutResult(a -> {
                    this.runExecutor();
                });
               
            } catch (MasterjobsInterruptException ex) {
                if (ex.getInterruptType() == MasterjobsInterruptException.InterruptType.PAUSE) {
                    removeFromActiveThreadsSet();
                    while (paused) {
                        try {
                            log.info(String.format("i'm paused, launch a %s command to resume me...", RESUME_COMMAND));
                            Thread.sleep(5000);
                            checkCommand();
                        } catch (InterruptedException | MasterjobsInterruptException subEx) {
                        }
                    }
                }
            } catch (Throwable ex) {
                log.error("fatal error", ex);
                // TODO: vedere cosa fare
            }
        }
        removeFromActiveThreadsSet();
        log.info(String.format("executor %s ended", getUniqueName()));
    }
    
    protected abstract String getExecutorName();
    
    protected String getUniqueName() {
        return getExecutorName() + "_" + Thread.currentThread().getName();
    }
    
    protected void buildWorkQueue() {
        this.workQueue = this.workQueue.replace("[thread_name]", getUniqueName());
    }
    
    public void executeCommand(String command) throws MasterjobsInterruptException {
        this.currentCommand = command;
    }
    
    protected void checkCommand() throws MasterjobsInterruptException {
        List<MapRecord> commandsList = (List<MapRecord>) redisTemplate.opsForStream().read(StreamReadOptions.empty().count(1), 
                StreamOffset.create(commandsStreamName, ReadOffset.from(lastStreamId)));
        if (commandsList != null && !commandsList.isEmpty()) {
            MapRecord commandRecord = commandsList.get(0);
            lastStreamId = commandRecord.getId().getValue();
            Map commandMap = (Map) commandRecord.getValue();
            executeCommand((String) commandMap.get(COMMAND_KEY));
        }

        if (currentCommand != null) {
            log.warn(String.format("command %s detected", currentCommand));
            log.warn(String.format("executing %s command...", currentCommand));
            try {
                switch (currentCommand) {
                    case STOP_COMMAND:
                        log.warn("i'm going to stop myself...");
                        stopped = true;
                        throw new MasterjobsInterruptException(MasterjobsInterruptException.InterruptType.STOP);
                    case PAUSE_COMMAND:
                        log.warn("i'm going to pause myself...");
                        paused = true;
                        throw new MasterjobsInterruptException(MasterjobsInterruptException.InterruptType.PAUSE);
                    case RESUME_COMMAND:
                        log.warn("i'm going to resume myself...");
                        paused = false;
                        break;
                    default:
                        log.warn(String.format("command %s not recognized, it will be ignored", currentCommand));
                }
            } finally {
                currentCommand = null;
            }
        }
    }
    
    public void manageQueue(Set.SetPriority priority) throws MasterjobsReadQueueTimeout, MasterjobsExecutionThreadsException, MasterjobsInterruptException {
        try {
            checkCommand();
            readFromQueueAndManageJobs(masterjobsUtils.getQueueBySetPriority(priority));
        } catch (MasterjobsBadDataException ex) {
            String errorMessage = "error on selecting queue";
            log.error(errorMessage, ex);
            throw new MasterjobsExecutionThreadsException(errorMessage, ex);
        }
    }
    
//    @Transactional
    public void readFromQueueAndManageJobs(String queue) throws MasterjobsReadQueueTimeout, MasterjobsExecutionThreadsException {
        String queueDataString = (String) redisTemplate.opsForList().move(
            queue, RedisListCommands.Direction.LEFT, 
            this.workQueue, RedisListCommands.Direction.RIGHT, 
            this.queueReadTimeoutMillis, TimeUnit.MILLISECONDS);
        //log.info(String.format("readed: %s", queueDataString));
        if (queueDataString != null) {
            manageQueueData(queueDataString);
        } else {
            throw new MasterjobsReadQueueTimeout(queue, this.queueReadTimeoutMillis);
        }
    }
    
//    @Transactional
    public void manageQueueData(String queueDataString) throws MasterjobsExecutionThreadsException {
        MasterjobsQueueData queueData = null;
        Set set = null;
        ObjectStatus objectStatus = null;
        try {
            queueData = masterjobsObjectsFactory.getMasterjobsQueueDataFromString(queueDataString);
            
            set = self.getSet(queueData.getSet());
            String objectId = set.getObjectId();
            String objectType = set.getObjectType();
            String app = set.getApp();
            ObjectStatus.ObjectState objectState;
            if (set.getWaitObject()) {
                objectStatus = self.getAndUpdateObjectState(objectId, objectType, app);
                objectState = objectStatus.getState();
            } else {
                objectState = ObjectStatus.ObjectState.IDLE;
            }
            switch (objectState) {
                case ERROR:
                    redisTemplate.opsForList().move(
                    this.workQueue, RedisListCommands.Direction.LEFT, 
                    this.errorQueue, RedisListCommands.Direction.RIGHT);
                    break;
                case IDLE:
                case PENDING:
                    executejobs(queueData, objectStatus, set);
                    break;
                default:
                    String errorMessage = String.format("object state %s not excepted", objectState);
                    log.error(errorMessage);
                    throw new MasterjobsDataBaseException(errorMessage);
            }
        } catch (Throwable t) {
            try {
                log.error("error managing queueData", t);
                if (!(t.getClass().isAssignableFrom(MasterjobsWorkerException.class))) {
                    /*
                    * se c'è un errore nell'esecuzione del job:
                    * se il set ha il wait, vuol dire che avrò la riga in object_status, per cui la setto in errore,
                    * poi setto in errore anche il job
                    */
                    if (objectStatus != null) {
                        objectStatus.setState(ObjectStatus.ObjectState.ERROR);
                    }
                    
                    if (set != null && set.getWaitObject()) {
                        self.setInError(null, objectStatus);
                    }
                }
                redisTemplate.opsForList().rightPush(this.errorQueue, queueData.dump());
                redisTemplate.delete(this.workQueue);
            } catch (Throwable subThr) {
                if (subThr.getClass().isAssignableFrom(JsonProcessingException.class)) {
                    log.error("error in dumping QueueData in json, moving all set in error queue", subThr);
                } else {
                    log.error("error in managin error, moving all set in error queue", subThr);
                }
                redisTemplate.opsForList().move(
                this.workQueue, RedisListCommands.Direction.LEFT, 
                this.errorQueue, RedisListCommands.Direction.RIGHT);
            }
            if (t.getClass().isAssignableFrom(JsonProcessingException.class)) {
                String errorMessage = String.format("json parse error from string %s", queueDataString);
                log.error(errorMessage, t);
            }
            throw new MasterjobsExecutionThreadsException(t);
        }
    }
    
    protected void executejobs(MasterjobsQueueData queueData, ObjectStatus objectStatus, Set set) throws MasterjobsParsingException, MasterjobsExecutionThreadsException, MasterjobsWorkerException {
        if (!set.getWaitObject() || this.isExecutable(set)) {
            List<Long> jobsCompleted = new ArrayList();
            for (Long jobId : queueData.getJobs()) {
                Job job = this.getJob(jobId);
                if (job != null) {
                    try {
                        Map<String, Object> data = job.getData();
                        JobWorkerDataInterface workerData = JobWorkerDataInterface.parseFromJobData(objectMapper, data);
                        JobWorker worker = masterjobsObjectsFactory.getJobWorker(job.getName(), workerData, job.getDeferred());
                        JobWorkerResult res = worker.doWork();
                        self.deleteJob(job);
                        jobsCompleted.add(job.getId());
                    } catch (Throwable ex) {
                        /*
                        * se c'è un errore nell'esecuzione del job:
                        * se il set ha il wait, vuol dire che avrò la riga in object_status, per cui la setto in errore,
                        * poi setto in errore anche il job
                        */
                        if (set.getWaitObject()) {
                            self.setInError(job, objectStatus);
                        }

                        /*
                        * una volta settato in errore sul DB, rimuovo i jobs completati dal queueData e
                        * lancio eccezione per far si che la funzione chiamante metta nella coda di errore
                        */
                        removeJobsCompletedFromQueueData(queueData, jobsCompleted);
                        throw ex;
                    }
                } else { // se il job non è in tabella, lo considero completato e vado avanti
                    jobsCompleted.add(jobId);
                }
            }
            // ho finito l'esecuzione dei jobs del set

            // elimino la workQueue
            redisTemplate.delete(this.workQueue);
        } else {
            // non posso eseguire il set perché devo aspettare l'esecuzione di un set precendente, sposto in wait queue
            redisTemplate.opsForList().move(
            this.workQueue, RedisListCommands.Direction.LEFT, 
            this.waitQueue, RedisListCommands.Direction.RIGHT);
        }
    }
    
    private void removeJobsCompletedFromQueueData(MasterjobsQueueData masterjobsQueueData, List<Long> jobsCompleted) {
        List<Long> jobs = masterjobsQueueData.getJobs();
        jobs.removeAll(jobsCompleted);
    }
    
//    @Transactional
    public Job getJob(Long jobId) {
        Job job = entityManager.find(Job.class, jobId);
        return job;
    }
    
//    @Transactional
    public Set getSet(Long setId) {
        Set set = entityManager.find(Set.class, setId);
        return set;
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
    public void deleteJob(Job job) {
        QJob qJob = QJob.job;
        QSet qSet = QSet.set;
        QObjectStatus qObjectStatus = QObjectStatus.objectStatus;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager); 
       
        Long setId = job.getSet().getId();
        
        // cancello il job dal database
        queryFactory.delete(qJob).where(qJob.id.eq(job.getId())).execute();
        
        // conto i jobs rimasti del set
        Long jobSetCount = queryFactory
            .select(qJob.count())
            .from(qJob)
            .where(qJob.set.id.eq(setId))
            .fetchOne();
        
        // se non ci sono più job attaccati al set, elimino sia il set che l'object_status
        if (jobSetCount == 0) {
            // eliminazione set
            queryFactory.delete(qSet).where(qSet.id.eq(job.getSet().getId())).execute();
            
            /* se il set è attaccato a un oggetto lo elimino
            * se il set non è attaccato a un oggetto allora objectId sarà null
            */
            if (job.getSet().getObjectId() != null) {
                // eliminazione object_status
                BooleanExpression filter = qObjectStatus.objectId.eq(job.getSet().getObjectId());
                if (job.getSet().getObjectType() != null)  // objectType potrebbe non esserci
                    filter = filter.and(qObjectStatus.objectType.eq(job.getSet().getObjectType()));
                if (job.getSet().getApp() != null)  // app potrebbe non esserci
                    filter = filter.and(qObjectStatus.app.eq(job.getSet().getApp()));
                queryFactory.delete(qObjectStatus).where(filter).execute();
            }
        }
        
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
    public void setInError(Job job, ObjectStatus objectStatus) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        if (job != null) {
            QJob qJob = QJob.job;
            job.setState(Job.JobState.ERROR);
            objectStatus.setState(ObjectStatus.ObjectState.ERROR);
            queryFactory
                .update(qJob)
                .set(qJob.state, job.getState().toString())
                .where(qJob.id.eq(job.getId()))
                .execute();
        }
        if (objectStatus != null) {
            QObjectStatus qObjectStatus = QObjectStatus.objectStatus;
            queryFactory
                .update(qObjectStatus)
                .set(qObjectStatus.state, objectStatus.getState().toString())
                .where(qObjectStatus.id.eq(objectStatus.getId()))
                .execute();
            }
    }
    
    private Optional<ObjectStatus> getObjectStatusAndSetIdleIfFound(BooleanExpression filter) throws MasterjobsDataBaseException {
        Optional<ObjectStatus> res = null;
        QObjectStatus qObjectStatus = QObjectStatus.objectStatus;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        List<ObjectStatus> objectStatusList = queryFactory.query().setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .select(qObjectStatus)
            .from(qObjectStatus)
            .where(filter)
            .fetch();
        if (objectStatusList.size() > 1) {
            String errorMessage = String.format("found %s object_status, only one excepted", objectStatusList.size());
            log.error(errorMessage);
            throw new MasterjobsDataBaseException(errorMessage);
        } else if (objectStatusList.isEmpty()) {
            res = Optional.empty();
        }
        else {
            ObjectStatus objectStatus = objectStatusList.get(0);
            if (objectStatus.getState() == ObjectStatus.ObjectState.IDLE) {
                long execute = queryFactory
                    .update(qObjectStatus)
                    .set(qObjectStatus.state, ObjectStatus.ObjectState.PENDING.toString())
                    .where(filter)
                    .execute();
//                log.info(String.valueOf(execute));
            }
            res = Optional.of(objectStatus);
        }
        return res;
    }
    
    private BooleanExpression getObjectStatusFilter(String objectId, String objectType, String app) {
        QObjectStatus qObjectStatus = QObjectStatus.objectStatus; 
        BooleanExpression filter = qObjectStatus.objectId.eq(objectId).and(qObjectStatus.objectType.eq(objectType));
        if (app != null) {
            filter = filter.and(qObjectStatus.app.eq(app));
        }
        return filter;
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
    public ObjectStatus getAndUpdateObjectState(String objectId, String objectType, String app) throws MasterjobsDataBaseException {
        ObjectStatus objectStatus;
        BooleanExpression filter = getObjectStatusFilter(objectId, objectType, app);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Optional<ObjectStatus> objectStatusOp = transactionTemplate.execute(action -> {
            Optional<ObjectStatus> res;
            try {
                res = getObjectStatusAndSetIdleIfFound(filter);
            } catch (MasterjobsDataBaseException ex) {
                res = null;
            }
            return res;
        });
        if (objectStatusOp == null) {
            throw new MasterjobsDataBaseException("error managing objectStatus");
        }
        if (objectStatusOp.isPresent()) {
            objectStatus = objectStatusOp.get();
        } else {
            String query = String.format("SELECT pg_advisory_xact_lock(%s)", filter.toString().hashCode());
            
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            objectStatus = transactionTemplate.execute(action -> {
                entityManager.createNativeQuery(query).getSingleResult();
                log.info("filter: " + filter.toString() + "hash: " + filter.toString().hashCode());
                Optional<ObjectStatus> res;
                try {
                    res = getObjectStatusAndSetIdleIfFound(filter);
                } catch (MasterjobsDataBaseException ex) {
                    return null;
                }
                if (res.isPresent()) {
                    return res.get();
                } else {
                    return insertObjectStatus(objectId, objectType, app);
                }
            });
            if (objectStatusOp == null) {
                throw new MasterjobsDataBaseException("error managing objectStatus");
            }
        }
        return objectStatus;
    }
    
    private ObjectStatus insertObjectStatus(String objectId, String objectType, String app) {
        ObjectStatus objectStatus = new ObjectStatus();
        objectStatus.setObjectId(objectId);
        objectStatus.setObjectType(objectType);
        objectStatus.setState(ObjectStatus.ObjectState.PENDING);
        if (app != null) {
            objectStatus.setApp(app);
        }
       
        try {
            entityManager.persist(objectStatus);
        } catch (PersistenceException persistenceException) {
            if (persistenceException.getCause() != null && persistenceException.getCause().getClass().isAssignableFrom(ConstraintViolationException.class)) {
                ConstraintViolationException constraintViolationException = (ConstraintViolationException) persistenceException.getCause();
                if (!constraintViolationException.getSQLState().equals("23505")) {
                    throw persistenceException;
                }
            } else {
                throw persistenceException; 
            }
        }
        return objectStatus;
    }
    
    /**
     * Controlla che il set sia eseguibile: cioè se non ci sono altri set per lo stesso oggetto, ma con id minore
     * @param set il set da controllare
     * @return "true" se il set è eseguibile, "false" altrimenti
     */
    public boolean isExecutable(Set set) {
        QSet qSet = QSet.set;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        BooleanExpression filter = 
            qSet.objectId.eq(set.getObjectId()).and(
            qSet.id.lt(set.getId()));
        if (set.getObjectType() != null) { // objectType potrebbe non esserci
            filter =  filter.and(qSet.objectType.eq(set.getObjectType()));
        }
        if (set.getApp() != null) { // app potrebbe non esserci
            filter = filter.and(qSet.app.eq(set.getApp()));
        }
        
        /* 
        * conta i set per lo stesso oggetto con id più piccolo del set in esame:
        * se ce ne sono, vuol dire che non posso ancora eseguire questo set
        */
        Long setCount = queryFactory
            .select(qSet.count())
            .from(qSet)
            .where(filter)
            .fetchOne();
        
        return setCount == 0;
    }
    
    
}
