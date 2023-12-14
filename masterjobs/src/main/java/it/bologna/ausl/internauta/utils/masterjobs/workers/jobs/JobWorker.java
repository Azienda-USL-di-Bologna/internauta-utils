package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.repository.JobReporitory;
import it.bologna.ausl.internauta.utils.masterjobs.workers.Worker;
import it.bologna.ausl.model.entities.masterjobs.QJob;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author gdm
 * 
 * Questa è la classe astratta che descrive un Worker, cioè la classe che sa eseguire un job
 * Per ogni tipologia di job da eseguire andrà creata una specifica classe che estende questa e ne implementa i metodi astratti.
 * Un Worker può essere "deferred", con cui si indica che i dati per l'esecuzione del job non saranno passati nel momento in cui
 * si mette in coda per essere eseguito, ma saranno calcolati nel momento della sua effettiva esecuzione
 * 
 * Contiene anche i dati necessari all'esecuzione del job.
 * Questi possono essere di due tipi:
 *  JobWorkerData: sono i dati che il worker potrà usare per svolgere il job
 *  JobWorkerDeferredData: nel caso di worker deferred, sono dei dati a partire dai quali, 
 *  nel momento della sua esecuzione il worker creerà i JobWorkerData
 * 
 * NB: le classi concrete che implementeranno questa classe devono esse annotate come:
 * @param <T> classe che estende JobWorkerData e raprpesenta i dati del job
 * @param <R> classe che estende JobWorkerResult e raprpesenta i risultati del job
 * 
 * @Component
 * @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
 */
public abstract class JobWorker<T extends JobWorkerData, R extends JobWorkerResult> extends Worker {
    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);

    protected JobWorkerData _workerData;
    protected JobWorkerDeferredData _workerDeferredData;
    protected boolean deferred;
    protected Integer executableCheckEveryMillis;
    protected ZonedDateTime jobExecutableCheckStart = null;
   
    private Map<String, Object> jobWorkData;
    private boolean jobWorkerDataChanged = false;
    
    private Long jobId;
    private ZonedDateTime jobInsertTs;
    private ZonedDateTime jobLastExecutionTs;
    
    @Autowired
    private JobReporitory jobRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * da chiamare, dopo aver istanziato il bean del worker, per creare un worker non deferred
     * @param workerData i dati per l'esecuzione del job
     */
    public void build(JobWorkerData workerData) {
        this._workerData = workerData;
        this.deferred = false;
    }
    
    /**
     * da chiamare, dopo aver istanziato il bean del worker, per creare un worker deferred
     * @param workerDeferredData i deffered data, che saranno usati per creare i data nel momento dell'esecuzione del job
     */
    public void buildDeferred(JobWorkerDeferredData workerDeferredData) {
        this._workerDeferredData = workerDeferredData;
        this.deferred = true;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }
    
    public TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }
    
    /**
     * da richiamare per far eseguire il job.
     * Se il worker è deferred, verranno prima calcolati i WorkerData richiamando il metodo astratto toWorkerData
     * sui deferred data. Questo metodo è implementato dalle classi WorkerDeferredData concrete
     * @return il risultato della chiamata al metodo doRealWork
     * @throws MasterjobsWorkerException 
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
    @Override
    public R doWork() throws MasterjobsWorkerException {
        log.info(String.format("executing job %s with jobId: %s ", getName(), getJobId()));
        if (deferred) {
            this._workerData = this._workerDeferredData.toWorkerData();
        }
        R res = doRealWork();
        log.info(String.format("job %s with jobId: %s ended", getName(), getJobId()));
        return res;
    }

    /**
     * Se un job è deferred verranno tornati i DeferredData, altrimenti i WorkerData
     * @return 
     */
    public JobWorkerDataInterface getData() {
        if (deferred)
            return _workerDeferredData;
        else
            return _workerData;
    }
    
    /**
     * Torna i parametri del job.
     * @return 
     */
    protected T getWorkerData() {
        return (T) _workerData;
    }

    /**
     * Indica se un job è deferred
     * @return "true" se il job è deferred, "false" altrimenti
     */
    public boolean isDeferred() {
        return deferred;
    }
    
    public boolean executableCheck() {
        if (this.jobExecutableCheckStart == null) {
            jobExecutableCheckStart = ZonedDateTime.now();
        }
        return true;
    }
    
    public final boolean _isExecutable() {
        boolean isExecutable = isExecutable();
        if (getJobId() != null && isJobWorkerDataChanged()) {
            writeJobWorkerData();
            setJobWorkerDataChanged(false);
        }
        return isExecutable;
    }
    
    private void writeJobWorkerData() {
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(getEntityManager());
        QJob qJob = QJob.job;
        getTransactionTemplate().setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        getTransactionTemplate().executeWithoutResult(a -> {
           Object value = getJobWorkData();
           if (getJobWorkData() != null) {
               jPAQueryFactory.update(qJob).set(qJob.workData, this.getJobWorkData()).where(qJob.id.eq(getJobId())).execute();
           } else {
               jPAQueryFactory.update(qJob).setNull(qJob.workData).where(qJob.id.eq(getJobId())).execute();
           }
        });
    }
    
    public boolean isExecutable() {
        return true;
    }

    public Long getJobId() {
        return this.jobId;
    }
    
    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Integer getExecutableCheckEveryMillis() {
        return executableCheckEveryMillis;
    }
    
    public void setExecutableCheckEveryMillis(Integer executableCheckEveryMillis) {
        this.executableCheckEveryMillis = executableCheckEveryMillis;
    }

    public Map<String, Object> getJobWorkData() {
        return jobWorkData;
    }

    public boolean isJobWorkerDataChanged() {
        return jobWorkerDataChanged;
    }

    public void setJobWorkerDataChanged(boolean jobWorkerDataChanged) {
        this.jobWorkerDataChanged = jobWorkerDataChanged;
    }
    
    public void setJobWorkData(Map<String, Object> jobWorkData) {
        setJobWorkerDataChanged(true);
        this.jobWorkData = jobWorkData;
    }

    public ZonedDateTime getJobInsertTs() {
        return jobInsertTs;
    }

    public void setJobInsertTs(ZonedDateTime jobInsertTs) {
        this.jobInsertTs = jobInsertTs;
    }

    public ZonedDateTime getJobLastExecutionTs() {
        return jobLastExecutionTs;
    }

    public void setJobLastExecutionTs(ZonedDateTime jobLastExecutionTs) {
        this.jobLastExecutionTs = jobLastExecutionTs;
    }
    
    /**
     * da implementare nei worker specifici, è l'effettiva esecuzione del lavoro
     * @return L'oggetto WorkerResult o una sua sottoclasse, rappresentante il risultato del job. Si può tornare anche null
     * @throws MasterjobsWorkerException nel caso di un errore nell'esecuzione del job
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
    protected abstract R doRealWork() throws MasterjobsWorkerException;
    
    public UUID calcolaMD5() throws JsonProcessingException, NoSuchAlgorithmException{
        //String md5 = jobRepository.calcolaMD5(this.getName(), objectMapper.writeValueAsString(this.getData()),this.isDeferred());
        log.info("inizio calcolo applicativo md5");
        String md5 = getMD5(this);
        log.info("fine calcolo applicativo md5");
        return UUID.fromString(md5.replaceFirst( 
            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5" 
        ));
    }
    
    /**
     * Calcola l'hash md5 del job che sarà inserito in tabella con questo worker
     * @param worker
     * @return
     * @throws NoSuchAlgorithmException
     * @throws JsonProcessingException 
     */
    private String getMD5(JobWorker worker) throws NoSuchAlgorithmException, JsonProcessingException {
        String strData = 
                worker.getName() + 
                (worker.getData() != null? worker.getData().toJsonString(objectMapper): "") +
                worker.isDeferred();
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.reset();
        m.update(strData.getBytes());
        byte[] digest = m.digest();
        BigInteger bigInt = new BigInteger(1,digest);
        String hashtext = bigInt.toString(16);
        // Now we need to zero pad it if you actually want the full 32 chars.
        while(hashtext.length() < 32 ){
          hashtext = "0"+hashtext;
        }
        return hashtext;
    }
}
