package it.bologna.ausl.internauta.utils.masterjobs.workers.services;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.vladmihalcea.hibernate.type.range.Range;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.Worker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.model.entities.masterjobs.QService;
import java.time.ZonedDateTime;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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

//    private Service service;
    
    @PersistenceContext
    protected EntityManager entityManager;
    
    @Autowired
    protected TransactionTemplate transactionTemplate;
    
    protected MasterjobsObjectsFactory masterjobsObjectsFactory;
    protected MasterjobsJobsQueuer masterjobsJobsQueuer;
    
    public void init(
            MasterjobsObjectsFactory masterjobsObjectsFactory,
            MasterjobsJobsQueuer masterjobsJobsQueuer) throws MasterjobsWorkerException {
        
        this.masterjobsObjectsFactory = masterjobsObjectsFactory;
        this.masterjobsJobsQueuer = masterjobsJobsQueuer;
    }
    
    @Override
    public void run() {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(t -> startTimeInterval());
        
        transactionTemplate.execute(t -> {
            try {
                return doWork();
            } catch (Throwable ex) {
                String errorMessage = String.format("error on executing service %s", getName());
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
