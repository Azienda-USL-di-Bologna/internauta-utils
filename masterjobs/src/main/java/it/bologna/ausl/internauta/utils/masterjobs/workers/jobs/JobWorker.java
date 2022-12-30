package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs;

import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    protected JobWorkerData workerData;
    protected JobWorkerDeferredData workerDeferredData;
    protected boolean deferred;
    
    /**
     * da chiamare, dopo aver istanziato il bean del worker, per creare un worker non deferred
     * @param workerData i dati per l'esecuzione del job
     */
    public void build(JobWorkerData workerData) {
        this.workerData = workerData;
        this.deferred = false;
    }
    
    /**
     * da chiamare, dopo aver istanziato il bean del worker, per creare un worker deferred
     * @param workerDeferredData i deffered data, che saranno usati per creare i data nel momento dell'esecuzione del job
     */
    public void buildDeferred(JobWorkerDeferredData workerDeferredData) {
        this.workerDeferredData = workerDeferredData;
        this.deferred = true;
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
        if (deferred) {
            this.workerData = this.workerDeferredData.toWorkerData();
        }
        return doRealWork();
    }

    /**
     * Se un job è deferred verranno tornati i DeferredData, altrimenti i WorkerData
     * @return 
     */
    public JobWorkerDataInterface getData() {
        if (deferred)
            return workerDeferredData;
        else
            return workerData;
    }
    
    /**
     * Torna i parametri del job.
     * @param <T>
     * @param workerDataClass la classe dei parametri del woker in cui castare
     * @return 
     */
    protected T getWorkerData() {
        return (T) workerData;
    }

    /**
     * Indica se un job è deferred
     * @return "true" se il job è deferred, "false" altrimenti
     */
    public boolean isDeferred() {
        return deferred;
    }
    
    /**
     * da implementare nei worker specifici, è l'effettiva esecuzione del lavoro
     * @return L'oggetto WorkerResult o una sua sottoclasse, rappresentante il risultato del job. Si può tornare anche null
     * @throws MasterjobsWorkerException nel caso di un errore nell'esecuzione del job
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
    protected abstract R doRealWork() throws MasterjobsWorkerException;
}
