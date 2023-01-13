package it.bologna.ausl.internauta.utils.masterjobs.workers.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.vladmihalcea.hibernate.type.range.Range;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsObjectNotFoundException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.Worker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.changeservicedetector.ChangeServiceDetectorWorker;
import it.bologna.ausl.model.entities.masterjobs.DebuggingOption;
import it.bologna.ausl.model.entities.masterjobs.QService;
import it.bologna.ausl.model.entities.masterjobs.Service;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
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

    public void setScheduledFuture(ScheduledFuture scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }
    
    public void setServiceEntity(Service serviceEntity) {
        this.serviceEntity = serviceEntity;
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
    
    protected boolean debuggingCanExecuteRun() {
        boolean res = true;
        if (super.debuggingOptions) {
            try {
                if (getClass().isAssignableFrom(ChangeServiceDetectorWorker.class)) {
                    List<String> limitedIPs = debuggingOptionsManager.getDebuggingParam(DebuggingOption.Key.limitChangeServiceDetectorToIP, new TypeReference<List<String>>(){});
                    if(limitedIPs != null && !limitedIPs.isEmpty() && !limitedIPs.contains(ip)) {
                        res = false;
                    }
                } else if (serviceEntity != null && StringUtils.hasText(serviceEntity.getExecuteOnlyOn())) {
                    Boolean limitServiceExecutionToIP = debuggingOptionsManager.getDebuggingParam(DebuggingOption.Key.limitServiceExecutionToIP, Boolean.class);
                    if (limitServiceExecutionToIP) {
                        res = limitServiceExecutionToIP && serviceEntity.getExecuteOnlyOn().equals(ip);
                    }
                }
            } catch (Exception ex) {
                log.error("error reading debugging option", ex);
            }
        }
        return res;
    }
    
    @Override
    public void run() {
        if (debuggingCanExecuteRun()) {
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
