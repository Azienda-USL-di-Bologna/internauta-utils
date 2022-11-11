package it.bologna.ausl.internauta.utils.masterjobs.workers.services;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.vladmihalcea.hibernate.type.range.Range;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsRuntimeExceptionWrapper;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.Worker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import static it.bologna.ausl.internauta.utils.masterjobs.workers.services.changeservicedetector.ChangeServiceDetectorWorker.CHANGE_SERVICE_NOTIFY;
import it.bologna.ausl.model.entities.masterjobs.QService;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;


/**
 *
 * @author gdm
 */
public abstract class ServiceWorker implements Runnable, Worker {
    private static final Logger log = LoggerFactory.getLogger(ServiceWorker.class);

    @PersistenceContext
    protected EntityManager entityManager;
    
    @Autowired
    protected TransactionTemplate transactionTemplate;

    protected ScheduledFuture scheduledFuture;
    
    protected MasterjobsObjectsFactory masterjobsObjectsFactory;
    protected MasterjobsJobsQueuer masterjobsJobsQueuer;
    
    public void init(
            MasterjobsObjectsFactory masterjobsObjectsFactory,
            MasterjobsJobsQueuer masterjobsJobsQueuer) throws MasterjobsWorkerException {
        
        this.masterjobsObjectsFactory = masterjobsObjectsFactory;
        this.masterjobsJobsQueuer = masterjobsJobsQueuer;
    }

    public void setScheduledFuture(ScheduledFuture scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }
    
    protected boolean isStopped() {    
        return scheduledFuture != null && (scheduledFuture.isCancelled() || scheduledFuture.isDone());
    }
    
    /**
     * Da fare l'override nel caso si voglia eseguire un'operazione prima della doWork()
     * la funzione è in transazione e sarà committata
     * @throws it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException
     */
    public void preWork() throws MasterjobsWorkerException {}
    
    /**
     * Da fare l'override nel caso si voglia eseguire un'operazione dopo la doWork()
     * la funzione è in transazione e sarà committata
     * @throws it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException
     */
    public void postWork() throws MasterjobsWorkerException {}
    
    @Override
    public void run() {
        transactionTemplate.executeWithoutResult(t -> startTimeInterval());
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(a -> {
            try {
                preWork();
            } catch (Throwable ex) {
                String errorMessage = String.format("error on executing preWork of service %s", getName());
                log.error(errorMessage, ex);
                throw new RuntimeException(errorMessage, ex);
            }
        });
        
        transactionTemplate.execute(t -> {
            try {
                return doWork();
            } catch (Throwable ex) {
                String errorMessage = String.format("error on executing doWork of service %s", getName());
                log.error(errorMessage, ex);
                throw new RuntimeException(errorMessage, ex);
            }
        });
        
        transactionTemplate.executeWithoutResult(a -> {
            try {
                postWork();
            } catch (Throwable ex) {
                String errorMessage = String.format("error on executing postWork of service %s", getName());
                log.error(errorMessage, ex);
                throw new RuntimeException(errorMessage, ex);
            }
        });
        
        transactionTemplate.executeWithoutResult(t -> stopTimeInterval());     
    }

    private void startTimeInterval(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QService qService = QService.service;
        queryFactory
            .update(qService)
            .set(qService.timeInterval, Range.closedInfinite(ZonedDateTime.now()))
            .where(qService.name.eq(getName()))
            .execute();
    }
    
    private void stopTimeInterval(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QService qService = QService.service;
        Range<ZonedDateTime> timeInterval = queryFactory
            .select(qService.timeInterval)
            .from(qService)
            .where(qService.name.eq(getName()))
            .fetchOne();
        if (timeInterval != null) {
            Range<ZonedDateTime> newTimeInterval = Range.open(timeInterval.lower(), ZonedDateTime.now());
            queryFactory
                .update(qService)
                .set(qService.timeInterval, newTimeInterval)
                .where(qService.name.eq(getName()))
                .execute();
        }
    }
    
//    public Service getService() {
//        return service;
//    }
//
//    public void setService(Service service) {
//        this.service = service;
//    }
    
}
