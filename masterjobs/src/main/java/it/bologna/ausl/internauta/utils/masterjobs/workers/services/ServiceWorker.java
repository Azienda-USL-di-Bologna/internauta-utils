package it.bologna.ausl.internauta.utils.masterjobs.workers.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.vladmihalcea.hibernate.type.range.Range;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsObjectNotFoundException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.executors.services.MasterjobsServicesExecutionScheduler;
import it.bologna.ausl.internauta.utils.masterjobs.workers.Worker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.changeservicedetector.ChangeServiceDetectorWorker;
import it.bologna.ausl.model.entities.masterjobs.DebuggingOption;
import it.bologna.ausl.model.entities.masterjobs.QService;
import it.bologna.ausl.model.entities.masterjobs.Service;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import javax.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.StringUtils;


/**
 *
 * @author gdm
 */
public abstract class ServiceWorker extends Worker implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ServiceWorker.class);

    protected ScheduledFuture scheduledFuture;
    protected Service serviceEntity;
    protected MasterjobsServicesExecutionScheduler masterjobsServicesExecutionScheduler;

    public void setScheduledFuture(ScheduledFuture scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }
    
    public void setServiceEntity(Service serviceEntity) {
        this.serviceEntity = serviceEntity;
    }
    
    protected boolean isStopped() {    
        return scheduledFuture != null && (scheduledFuture.isCancelled() || scheduledFuture.isDone());
    }

    public void setMasterjobsServicesExecutionScheduler(MasterjobsServicesExecutionScheduler masterjobsServicesExecutionScheduler) {
        this.masterjobsServicesExecutionScheduler = masterjobsServicesExecutionScheduler;
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
    
    /**
     * Controlla se può eseguire il servizio.
     * Un servizio può essere eseguito solo se l'ip della macchina e la porta dell'applicazione è uguale all'ip e la porta
     * scritte sulla colonna execute_only_on del servizio (tabella masterjobs.services) oppure la colonna è vuota.
     * Se la colonna è vuota il vengono settati ip e porta dell'applicazione. Da ora in poi il servizio sarà eseguto solo da lei
     * @return true se il sevizio può essere eseguito false altrimenti.
     */
    protected boolean CanExecuteRun() {
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        
        QService qService = QService.service;
        /* 
        select for update per controllare ip e porta di chi può eseguire il servizio .
        colonna execute_only_on in tabella masterjobs.services.
        Il primo che arriva riesce a fare la select, gli altri aspettano fino a che il primo non esce dalla funzione attuale.
        */
        String executeOnlyOn = jPAQueryFactory.query().setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .select(qService.executeOnlyOn)
            .from(qService)
            .where(qService.name.eq(serviceEntity.getName()))
            .fetchOne();
        
        // se execute_only_on è null o vuoto, setto i miei ip e porta
        if (!StringUtils.hasText(executeOnlyOn)) {
            jPAQueryFactory
                .update(qService)
                .set(qService.executeOnlyOn, ip + ":" + port)
                .where(qService.name.eq(serviceEntity.getName()))
                .execute();
            executeOnlyOn = ip + ":" + port; // così facendo la funzione tornerà per forxa true
        }
        return executeOnlyOn != null && executeOnlyOn.equals(ip + ":" + port);
    }
    
    @Override
    public void run() {
        
        /*
        controllo se l'applicazione può eseguire questo servizio (se ip:porta settati sulla colonna execute_only_on sono
        uguali ai miei
        */
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Boolean canExecuteRun = transactionTemplate.execute(a -> CanExecuteRun());
        
        if (canExecuteRun) {
            log.info(String.format("starting %s...", getName()));
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
}
