package it.bologna.ausl.internauta.utils.masterjobs.executors.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsUtils;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsBadDataException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsDataBaseException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsExecutionThreadsException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsInterruptException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsParsingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsReadQueueTimeout;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsRuntimeExceptionWrapper;
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
import it.bologna.ausl.model.entities.masterjobs.DebuggingOption;
import it.bologna.ausl.model.entities.masterjobs.QDebuggingOption;
import java.util.UUID;
import java.util.logging.Level;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;

/**
 *
 * @author gdm
 * 
 * Classe astratta che rappresenta un executor
 * Un executor è un thread in grado di eseguire i job
 * Gli executor esistenti e che estendono questa classe ad oggi sono: 
 *  - MasterjobsHighPriorityJobsExecutionThread
 *  - MasterjobsHighestPriorityJobsExecutionThread
 *  - MasterjobsNormalPriorityJobsExecutionThread
 *  - MasterjobsWaitQueueJobsExecutionThread
 */
public abstract class MasterjobsJobsExecutionThread implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsJobsExecutionThread.class);
    public static final String COMMAND_KEY = "command";
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
    protected boolean useDebuggingOptions;
    protected String ip;

    protected boolean stopped = false;
    protected boolean paused = false;
    
    private String name;
    private String lastStreamId = "0";
    private String currentCommand = null;
    
    
    /*
    * Metodi builder
    */
    
    public MasterjobsJobsExecutionThread self(MasterjobsJobsExecutionThread self) {
        this.self = self;
        return this;
    }
    
    public MasterjobsJobsExecutionThread activeThreadsSetName(String activeThreadsSetName) {
        this.activeThreadsSetName = activeThreadsSetName;
        return this;
    }
    
    public MasterjobsJobsExecutionThread commandsStreamName(String commandsStreamName) {
        this.commandsStreamName = commandsStreamName;
        return this;
    }
    
    public MasterjobsJobsExecutionThread inQueueNormal(String inQueueNormal) {
        this.inQueueNormal = inQueueNormal;
        return this;
    }
    
    public MasterjobsJobsExecutionThread inQueueHigh(String inQueueHigh) {
        this.inQueueHigh = inQueueHigh;
        return this;
    }
    
    public MasterjobsJobsExecutionThread inQueueHighest(String inQueueHighest) {
        this.inQueueHighest = inQueueHighest;
        return this;
    }

    public MasterjobsJobsExecutionThread workQueue(String workQueue) {
        this.workQueue = workQueue;
        return this;
    }
    
    public MasterjobsJobsExecutionThread errorQueue(String errorQueue) {
        this.errorQueue = errorQueue;
        return this;
    }

    public MasterjobsJobsExecutionThread waitQueue(String waitQueue) {
        this.waitQueue = waitQueue;
        return this;
    }

    public MasterjobsJobsExecutionThread outQueue(String outQueue) {
        this.outQueue = outQueue;
        return this;
    }

    public MasterjobsJobsExecutionThread sleepMillis(int sleepMillis) {
        this.sleepMillis = sleepMillis;
        return this;
    }

    public MasterjobsJobsExecutionThread queueReadTimeoutMillis(int queueReadTimeoutMillis) {
        this.queueReadTimeoutMillis = queueReadTimeoutMillis;
        return this;
    }
    
    public MasterjobsJobsExecutionThread useDebuggingOptions(boolean useDebuggingOptions) {
        this.useDebuggingOptions = useDebuggingOptions;
        return this;
    }
    
    public MasterjobsJobsExecutionThread ip(String ip) {
        this.ip = ip;
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
    
    /**
     * viene lanciato all'avvio del thread.
     * Inizializza il tutto e lancia runExecutor sulla classe concreta
     */
    @Override
    public void run() {
        
        while (!stopped && !paused) {
            insertInActiveThreadsSet();
            try {
                log.info(String.format("executor %s started", getUniqueName()));
                buildWorkQueue();
                checkCommand();
                
                // lancia runExecutor() sulla classe concreta
                this.runExecutor();
               
            } catch (MasterjobsInterruptException ex) { // se viene mandato un comando di stop o di pausa
                if (ex.getInterruptType() == MasterjobsInterruptException.InterruptType.PAUSE) { // se rilvea un comando pausa
                    // ri rimuove dai threads attivi
                    removeFromActiveThreadsSet();
                    
                    // rimane in pausa facendo uno sleep di 5 secondi fino a che non viene lanciato un resume
                    while (paused) {
                        try {
                            log.info(String.format("i'm paused, launch a %s command to resume me...", RESUME_COMMAND));
                            Thread.sleep(5000);
                            checkCommand();
                        } catch (InterruptedException | MasterjobsInterruptException subEx) {
                        }
                    }
                    // al resume esce da dal ciclo e ricomincia dall'inizio
                }
            } catch (Throwable ex) {
                log.error("fatal error", ex);
                // TODO: vedere cosa fare
            }
        }
        // arrivati qui il ciclo principale è finito, prima di terminare si rimuove dai thread attivi
        removeFromActiveThreadsSet();
        log.info(String.format("executor %s ended", getUniqueName()));
    }
    
    /**
     * deve essere implementato tornando il nome dell'executor (Es. HighPriorityExecutor)
     * viene usato per la creazione del nome univoco del thread
     * @return il nome dell'executor 
     */
    protected abstract String getExecutorName();
    
    /**
     * Costruisce un nome univoco che indetifica l'executor.
     * Viene inserita anche una parte random per assicurarsi che sia univoco anche nel caso ci siano più istanze dell'applicazione
     * @return 
     */
    protected String getUniqueName() {
        if (this.name == null) {
            this.name = getExecutorName() + "_" + UUID.randomUUID().toString() + "_" + Thread.currentThread().getName();
        }
        return this.name;
    }
    
    /**
     * Crea la workqueue dell'executor
     */
    protected void buildWorkQueue() {
        this.workQueue = this.workQueue.replace("[thread_name]", getUniqueName());
    }
    
    /**
     * imposta un comando da eseguire.
     * Non viene eseguito immediatamente, ma appena possibile
     * @param command il comando da eseguire
     * @throws MasterjobsInterruptException 
     */
    public void executeCommand(String command) throws MasterjobsInterruptException {
        this.currentCommand = command;
    }
    
    /**
     * verifica il comando da eseguire e lo esegue effettivamente
     * come prima cosa controlla se è stato inserito un comando sullo stream redis dei comandi
     * altrimenti esegue il comando settato con la executeCommand()
     * se nessun comando viene rilevato, la funzione non fa nulla
     * @throws MasterjobsInterruptException viene lanciata quando viene rilevato un comando di Interrupt (stop o pause)
     */
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
    
    /**
     * Gestisce la coda redis dei job da eseguire
     * @param priority la priorità che gestisce l'executor
     * @throws MasterjobsReadQueueTimeout lanciata quando non c'è nulla da eseguire nella coda dei job per un tempo definito nei parametri
     * @throws MasterjobsExecutionThreadsException se c'è un errore nella gestione dei job da eseguire
     * @throws MasterjobsInterruptException lanciata nel caso rileva un comando di stop o pause
     */
    protected void manageQueue(Set.SetPriority priority) throws MasterjobsReadQueueTimeout, MasterjobsExecutionThreadsException, MasterjobsInterruptException {
        try {
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transactionTemplate.executeWithoutResult(a -> {
                // come prima cosa verifica se ci sono comandi da eseguire e nel caso gli esegue
                try {
                    checkCommand();
                    // legge dalla coda della priorità passata e se ci sono, esegue i job
                    readFromQueueAndManageJobs(masterjobsUtils.getQueueBySetPriority(priority));
                } catch (Throwable ex) {
                    throw new MasterjobsRuntimeExceptionWrapper(ex);
                }
            });         
        } catch (MasterjobsRuntimeExceptionWrapper ex) {
            if (ex.getOriginalException().getClass().isAssignableFrom(MasterjobsBadDataException.class)) {
                String errorMessage = "error on selecting queue";
                log.error(errorMessage, ex);
                throw new MasterjobsExecutionThreadsException(errorMessage, ex);
            } else if (ex.getOriginalException().getClass().isAssignableFrom(MasterjobsReadQueueTimeout.class)) {
                throw (MasterjobsReadQueueTimeout) ex.getOriginalException();
            } else if (ex.getOriginalException().getClass().isAssignableFrom(MasterjobsInterruptException.class)) {
                throw (MasterjobsInterruptException) ex.getOriginalException();
            }
        }
    }
    
    /**
     * Legge dalla coda passata e esegue i job rilevati
     * @param queue
     * @throws MasterjobsReadQueueTimeout lanciata se non ci sono job da eseguire nella coda passata per unt empo definito nei parametri
     * @throws MasterjobsExecutionThreadsException se c'è un errore nella gestione dei job
     */
    public void readFromQueueAndManageJobs(String queue) throws MasterjobsReadQueueTimeout, MasterjobsExecutionThreadsException {
        // legge dalla coda indicata e sposta atomicamente i job nella coda di work
        String queueDataString = (String) redisTemplate.opsForList().move(
            queue, RedisListCommands.Direction.LEFT, 
            this.workQueue, RedisListCommands.Direction.RIGHT, 
            this.queueReadTimeoutMillis, TimeUnit.MILLISECONDS);
        
        if (queueDataString != null) { // se legge qualcosa dalla coda, allora lo gestisco
            manageQueueData(queueDataString);
        } else { // se va qui vuol dire che è scaduto il timeout perché non c'era nulla nella coda
            // lancio l'eccezione per indicare che non c'è nulla nelal coda
            throw new MasterjobsReadQueueTimeout(queue, this.queueReadTimeoutMillis);
        }
    }
    
    /**
     * Gestisce quanto letto dalla coda
     * @param queueDataString quello che è stato letto dalla coda
     * @throws MasterjobsExecutionThreadsException 
     */
    public void manageQueueData(String queueDataString) throws MasterjobsExecutionThreadsException {
        MasterjobsQueueData queueData = null;
        Set set = null;
        ObjectStatus objectStatus = null;
        try {
            // contruisce l'oggetto che rappresenta i job da eseguire e il set al quale appartengono
            queueData = masterjobsObjectsFactory.getMasterjobsQueueDataFromString(queueDataString);
            
            // carica i dati necessati all'esecuzione dei job
            set = self.getSet(queueData.getSet());
            if (set != null) {
                String objectId = set.getObjectId();
                String objectType = set.getObjectType();
                String app = set.getApp();
                ObjectStatus.ObjectState objectState;

                /* solo se il set deve attendere il completamento dei job di set precedenti, 
                * devo scrivere nella tabella ObjectStatus lo stato dell'oggetto a cui il set è attaccato
                * nel caso l'ogetto era già presente nella tabella, allora ne leggo lo stato
                */
                if (set.getWaitObject()) {
                    objectStatus = self.getAndUpdateObjectState(objectId, objectType, app);
                    objectState = objectStatus.getState();
                } else { // se il set non deve attendere altri set allora non scrivo nulla in tabella e considero l'oggetto libero (IDLE)
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
                        executeJobs(queueData, objectStatus, set);
                        break;
                    default:
                        String errorMessage = String.format("object state %s not excepted", objectState);
                        log.error(errorMessage);
                        throw new MasterjobsDataBaseException(errorMessage);
                }
            }
        } catch (Throwable t) {
            try {
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
                        self.setInError(null, objectStatus, null);
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
    
    /**
     * Funzione solo per debugging, ha senso solo se sono attive le debugging options.
     * Controlla se il job può essere eseguito o meno.
     * Se non sono attive le debugging options torna sempre "true".
     * Un job può essere eseguito solo se nel parametro filerJobs delle debugging options è prensente jobName con l'ip della macchina
     * @param jobName il job da controllare
     * @return "true" se il job con il jobName passato può essere eseguito, "false" altrimenti
     */
    protected boolean debuggingCanExecuteJob(String jobName) {
        boolean res = true;
        if (useDebuggingOptions) {
            JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
            QDebuggingOption qDebuggingOption = QDebuggingOption.debuggingOption;
            Object filterJobsObj = jPAQueryFactory
                    .select(qDebuggingOption.value)
                    .from(qDebuggingOption)
                    .where(qDebuggingOption.key.eq(DebuggingOption.Key.filterJobs.toString()))
                    .fetchOne();
            Map<String, Object> filterJobs = objectMapper.convertValue(filterJobsObj, new TypeReference<Map<String, Object>>(){});
            if (filterJobs != null && !filterJobs.isEmpty()) {
//                Object executeOthersObj = filterJobs.get("executeOthers");
//                Boolean executeOthers = objectMapper.convertValue(executeOthersObj, Boolean.class);
                //TODO: finire
                List<String> ipsToFilter = objectMapper.convertValue(filterJobs.get(jobName), new TypeReference<List<String>>(){});
                if (ipsToFilter != null && !ipsToFilter.isEmpty() && !ipsToFilter.stream().anyMatch(j -> j.equals(this.ip))) {
                    res = false;
                }
            }
        }
        return res;
    }
    
    /**
     * Funzione solo per debugging, ha senso solo se sono attive le debugging options.
     * Controlla se il set può essere eseguito dalla macchina.
     * Se non sono attive le debugging options torna sempre "true".
     * Il set può essere eseguito solo se esiste il parametro limitSetExecutionToInsertedIP ed è "true"
     * @param set il set da controllare
     * @return "true" se il set passato può essere eseguito, "false" altrimenti
     */
    protected boolean debuggingCanExecuteSet(Set set) {
        boolean res = true;
        if (useDebuggingOptions) {
            if (set.getInsertedFrom() != null) {
                QDebuggingOption qDebuggingOption = QDebuggingOption.debuggingOption;
                JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
                Object limitSetExecutionToInsertedIPObj = jPAQueryFactory
                    .select(qDebuggingOption.value)
                    .from(qDebuggingOption)
                    .where(qDebuggingOption.key.eq(DebuggingOption.Key.limitSetExecutionToInsertedIP.toString()))
                    .fetchOne();
                Boolean limitSetExecutionToInsertedIP = objectMapper.convertValue(limitSetExecutionToInsertedIPObj, Boolean.class);
                res = limitSetExecutionToInsertedIP && set.getInsertedFrom().equals(this.ip);
            }
        }
        return res;
    }
    
    /**
     * Esegue i job presenti all'interno di MasterjobsQueueData passato
     * @param queueData l'oggetto costruito da quanto letto dalla coda redis
     * @param objectStatus l'ObjectStatus a cui il set è associato (può essere anche null)
     * @param set il set a cui i job fanno riferimento
     * @throws MasterjobsParsingException
     * @throws MasterjobsExecutionThreadsException
     * @throws MasterjobsWorkerException 
     */
    protected void executeJobs(MasterjobsQueueData queueData, ObjectStatus objectStatus, Set set) throws MasterjobsParsingException, MasterjobsExecutionThreadsException, MasterjobsWorkerException {
        
        /* 
        per prima cosa controllo se posso eseguire i job
        I job possono essere eseguti se non devono attendere l'esecuzione di altri job, oppure
        se la funzione isExecutable() mi torna true. 
        Questa tornerà true solo se tutti i job dei set precendeti sono stati eseguiti
        */
        if (debuggingCanExecuteSet(set) && (!set.getWaitObject() || this.isExecutable(set))) {
            /* 
            tengo una lista dei job completati.
            Questa mi serve nel caso un job vada in errore, in modo da aggiornare sulla coda i job ancora non eseguiti
            */
            List<Long> jobsCompleted = new ArrayList();
            boolean stoppedJobsExecution = false;
            // se il set può essere eseguito allora ciclo su tutti i job e li eseguo uno ad uno
            for (Long jobId : queueData.getJobs()) {
                Job job = this.getJob(jobId);
                if (job != null && debuggingCanExecuteJob(job.getName())) {
                    try {
                        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                        transactionTemplate.executeWithoutResult(a -> {
                            this.updateJobState(jobId, Job.JobState.RUNNING, null);
                        });
                        // carico i dati del job
                        Map<String, Object> data = job.getData();
                        JobWorkerDataInterface workerData = JobWorkerDataInterface.parseFromJobData(objectMapper, data);
                        // istanzio il worker in grado eseguire il job passandogli i suoi dati
                        JobWorker worker = masterjobsObjectsFactory.getJobWorker(job.getName(), workerData, job.getDeferred());
                        // eseguo il job tramite il worker
                        JobWorkerResult res = worker.doWork();
                        /* se l'esecuzione è andata a buon fine cancello il job dal DB (se il job è l'ultimo verrà cancellato
                        * anche il set e l'ObjectStatus (se presente)
                        */
                        self.deleteJob(job);
                        // aggiungo il job alla lista dei completati
                        jobsCompleted.add(job.getId());
                    } catch (Throwable ex) {
                        /*
                        * se c'è un errore nell'esecuzione del job:
                        * setto in errore la riga in object_status (se esiste) e setto in errore anche il job
                        */
                        self.setInError(job, objectStatus, ex.getMessage());

                        /*
                        * una volta settato in errore sul DB, rimuovo i jobs completati dal queueData e
                        * lancio eccezione per far si che la funzione chiamante metta nella coda di errore
                        */
                        removeJobsCompletedFromQueueData(queueData, jobsCompleted);
                        throw ex;
                    }
                } else if (job == null) { // se il job non è in tabella, lo considero completato e vado avanti
                    jobsCompleted.add(jobId);
                } else { // se non posso eseguire il job a causa delle debugging option, ne interrompo l'esecuzione e setto che l'ho stoppata
                    stoppedJobsExecution = true;
                    break;
                }
            }
            // ho finito l'esecuzione dei jobs del set

            //se ho stoppato l'esecuzione, sposto nella waitQueue i rimanenti, in modo che saranno rimessi in coda
            if (stoppedJobsExecution) {
                // rimuovo i jobs completati dalla lista dei jobs
                removeJobsCompletedFromQueueData(queueData, jobsCompleted);
                try {
                    // sposto i jobs rimanenti nella waitQueue
                    redisTemplate.opsForList().rightPush(this.waitQueue, queueData.dump());
                } catch (Throwable ex) {
                    String errorMessage = "error moving jobs on wait queue after skip";
                    log.error(errorMessage, ex);
                    throw new MasterjobsExecutionThreadsException(errorMessage, ex);
                }
            }
            
            // elimino la workQueue
            redisTemplate.delete(this.workQueue);
        } else {
            // non posso eseguire il set perché devo aspettare l'esecuzione di un set precendente, sposto in wait queue
            redisTemplate.opsForList().move(
            this.workQueue, RedisListCommands.Direction.LEFT, 
            this.waitQueue, RedisListCommands.Direction.RIGHT);
        }
    }
    
    /**
     * rimuove i jobs completati dalla lista dei job in MasterjobsQueueData
     * @param masterjobsQueueData
     * @param jobsCompleted la lista dei job completati
     */
    private void removeJobsCompletedFromQueueData(MasterjobsQueueData masterjobsQueueData, List<Long> jobsCompleted) {
        List<Long> jobs = masterjobsQueueData.getJobs();
        jobs.removeAll(jobsCompleted);
    }
    
    /**
     * Legge e torna un Job dalla tabella dei job
     * @param jobId
     * @return 
     */
    public Job getJob(Long jobId) {
        Job job = entityManager.find(Job.class, jobId);
        return job;
    }
    
    /**
     * Legge e torna un Set dalla tabella dei set
     * @param setId
     * @return 
     */
    public Set getSet(Long setId) {
        Set set = entityManager.find(Set.class, setId);
        return set;
    }
    
    protected void updateJobState(Long jobId, Job.JobState state, String error) {
        QJob qJob = QJob.job;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        JPAUpdateClause updateClause = queryFactory.update(qJob).set(qJob.state, state.toString());
        if (error != null) {
            updateClause.set(qJob.error, error);
        }
        updateClause.where(qJob.id.eq(jobId)).execute();
    }
    
    /**
     * Cancella un job dalla tabella dei job.
     * se il job è l'ultimo del set, cancella anche il set dalla tabella dei set e l'oggetto dalla tabella ObjectStatus (se presente)
     * @param job il job da cancellare
     */
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
                BooleanExpression filter = getObjectFilter(
                        qObjectStatus.objectId, job.getSet().getObjectId(), 
                        qObjectStatus.objectType, job.getSet().getObjectType(), 
                        qObjectStatus.objectId, job.getSet().getApp());
                queryFactory.delete(qObjectStatus).where(filter).execute();
            }
        }
        
    }
    
    /**
     * setta in ERROR sul database sia il job, che l'ObjectStatus associato (se presente)
     * @param job
     * @param objectStatus 
     * @param jobError stringa di errore da inserire in tabella jobs
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
    public void setInError(Job job, ObjectStatus objectStatus, String jobError) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        if (job != null) {
            job.setState(Job.JobState.ERROR);
            this.updateJobState(job.getId(), job.getState(), jobError);
        }
        if (objectStatus != null) {
            objectStatus.setState(ObjectStatus.ObjectState.ERROR);
            QObjectStatus qObjectStatus = QObjectStatus.objectStatus;
            queryFactory
                .update(qObjectStatus)
                .set(qObjectStatus.state, objectStatus.getState().toString())
                .where(qObjectStatus.id.eq(objectStatus.getId()))
                .execute();
            }
    }
    
    /**
     * cerca (tramite una select for update) un ObjectStatus e se lo trova ed è in IDLE lo setta a PENDING
     * @param filter
     * @return un oggetto Optional con all'interno l'ObjectStatus trovato. Se non lo trova torna un Optional empty
     * @throws MasterjobsDataBaseException 
     */
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
    
    /**
     * costruisce un filtro per cercare un ObjectStatus o un Set con i parametri non null passati
     * @param objectIdPath il path dell'objectId (es. Qset.set.objectId)
     * @param objectIdValue il valore dell'objectId
     * @param objectTypePath il path dell'objectType (es. Qset.set.objectType)
     * @param objectTypeValue il valore dell'objectType, se è null non viene inserito nel filtro
     * @param appPath il path dell'app (es. Qset.set.app)
     * @param appValue il valore dell'app, se è null non viene inserito nel filtro
     * @return il filtro da inserire nella query
     */
    private BooleanExpression getObjectFilter(
            StringPath objectIdPath, String objectIdValue, 
            StringPath objectTypePath, String objectTypeValue, 
            StringPath appPath, String appValue) {
        BooleanExpression filter = objectIdPath.eq(objectIdValue);
        if (objectTypeValue != null) {  // objectType potrebbe non esserci
            filter = filter.and(objectTypePath.eq(objectTypeValue)); 
        }
        if (appValue != null) {  // app potrebbe non esserci
            filter = filter.and(appPath.eq(appValue)); 
        }
        return filter;
    }
    
    /**
     * legge lo stato dell'oggetto passato, se non esiste lo crea, se esiste ed è IDLE lo setta in PENDING
     * l'oggetto viene cercato solo per i parametri non null passati
     * NB: la funzione crea una nuova transazione e committa al termine
     * @param objectId
     * @param objectType
     * @param app
     * @return lo status corrente dell'oggetto
     * @throws MasterjobsDataBaseException 
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
    public ObjectStatus getAndUpdateObjectState(String objectId, String objectType, String app) throws MasterjobsDataBaseException {
        ObjectStatus objectStatus;
        QObjectStatus qObjectStatus = QObjectStatus.objectStatus;
        
        // crea il filtro per cercare l'oggetto in base ai paramettri non null passati
        BooleanExpression filter = getObjectFilter(
                qObjectStatus.objectId, objectId, 
                qObjectStatus.objectType, objectType, 
                qObjectStatus.app, app);
        
        /* in una nuova transazione, con commit al temine,
        * legge (tramite una select for update) l'oggetto e se è in IDLE lo setta in PENDING
        */
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
        if (objectStatusOp == null) { // non dovrebbe mai essere null, se non trova l'oggetto objectStatusOp viene settato a empty
            throw new MasterjobsDataBaseException("error managing objectStatus");
        }
        if (objectStatusOp.isPresent()) {
            objectStatus = objectStatusOp.get();
        } else { // se non c'è l'ggetto cercato allora viene inserito
            /* per inserirlo viene preso un lock, in modo che solo questo thread lo inserisca
            * il lock viene preso tramite la funzione pg_advisory_xact_lock di postgres su un numero ottenuto calcolando
            * l'hash del filtro utilizzato per cercare l'oggetto
            */
            String query = String.format("SELECT pg_advisory_xact_lock(%s)", filter.toString().hashCode());
            
            /* dopo aver preso il lock per prima cosa controllo se qualche altro thread (che aveva preso il lock prima di me)
            * ha già creato l'oggetto e nel caso lo ritorno.
            * se ancora non esiste lo inserisco
            */
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
    
    /**
     * inserisce un oggetto nella tabella ObjectStatus
     * @param objectId
     * @param objectType
     * @param app
     * @return 
     */
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
        if (set.getObjectId() != null) {
            QSet qSet = QSet.set;
            JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
            BooleanExpression filter = 
                    getObjectFilter(
                            qSet.objectId, set.getObjectId(),
                            qSet.objectType, set.getObjectType(), 
                            qSet.app, set.getApp())
                    .and(qSet.id.lt(set.getId()));

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
        } else {
            return true;
        }
    }
    
    
}
