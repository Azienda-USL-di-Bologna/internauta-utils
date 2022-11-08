package it.bologna.ausl.internauta.utils.masterjobs.executors.services;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.changeservicedetector.ChangeServiceDetector;
import it.bologna.ausl.model.entities.masterjobs.QService;
import it.bologna.ausl.model.entities.masterjobs.Service;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class MasterjobsServicesExecutionScheduler {    
        
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;
    
    @Autowired
    private MasterjobsJobsQueuer masterjobsJobsQueuer;
 
    @Autowired
    @Qualifier("masterjobsScheduledThreadPoolExecutor")
    private ScheduledThreadPoolExecutor scheduledExecutorService;
    
    private final Map<String, List<ScheduledFuture>> activeServiceMap = new HashMap<>();
    
    public void stopService(String serviceName) {
        List<ScheduledFuture> scheduledServices = activeServiceMap.get(serviceName);
        if (scheduledServices != null && !scheduledServices.isEmpty()) {
            for (ScheduledFuture scheduledService : scheduledServices) {
                scheduledService.cancel(true);
            }
        }
    }
    
    public void stopAndDisableService(String serviceName, boolean removeFromDB) {
        stopService(serviceName);
        Service service = entityManager.find(Service.class, serviceName);
        if (service.getActive()) {
            service.setActive(false);
        }
        if (removeFromDB) {
            entityManager.remove(service);
        } else {
            entityManager.merge(service);
        }
    }
    
    public void scheduleServiceThreads() throws MasterjobsWorkerException {
        ZonedDateTime now = ZonedDateTime.now();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QService qService = QService.service;
        List<Service> activeServices = queryFactory
            .select(qService)
            .from(qService)
            .where(qService.active.eq(true))
            .fetch();
        for (Service activeService : activeServices) {
            List<ScheduledFuture> scheduledService = new ArrayList<>();
            if (activeService.getEveryDayAt() != null) { // se è un servizio giornaliero
                long secondsToStart = 0;
                if (!activeService.getStartAt().isAfter(now)) { // se startAt è <= oggi
                    
                    // se il servizio non è mai girato oppure non è mai terminato considero come prossima data di avvio del servizio la startAt
                    // altrimenti se è terminato, considero come prossima data di avvio la data di terminazione
                    ZonedDateTime startDate;
                    if (activeService.getTimeInterval() != null && activeService.getTimeInterval().hasUpperBound()) {
                        startDate = activeService.getTimeInterval().upper();
                    } else {
                        startDate = activeService.getStartAt();
                    }
                    
                    // se il servizio non è mai girato oppure non è mai terminato
                    if (!isToday(startDate, now) || !startDate.isBefore(now.with(activeService.getEveryDayAt()))
                    ) { // se la startAt è prima di oggi oppure è oggi e l'ora di avvio è già passata, il servizio deve partire subito
                        scheduledService.add(scheduledExecutorService.schedule(getServiceWorkerAndInit(activeService.getName()), 0, TimeUnit.SECONDS));
                        // il prossimo partire domani all'ora indicata, per cui aggiungo 24 ore
                        secondsToStart = now.toLocalTime().until(startDate.toLocalTime(), ChronoUnit.SECONDS) + 60*60*24;
                    } else {
                        secondsToStart = now.toLocalTime().until(activeService.getEveryDayAt(), ChronoUnit.SECONDS);
                    }
                } else { // se la data di avvio è nel futuro, calcolo i secondi che mancano ad arrivare alla data di avvio
                    // prima calcolo i secondi che mancano fino alla data di avvio
                    secondsToStart = now.until(activeService.getStartAt(), ChronoUnit.SECONDS);
                    
                    // poi se l'ora di avvio è dopo l'ora di avvi ogiornaliera, allora la data effettiva di avvio sarà il giorno dopo all'ora indicata
                    if (activeService.getStartAt().toLocalTime().isAfter(activeService.getEveryDayAt())) { 
                        // per cui devo calcolare i secondi tra le due ore di avvio e sommare 24 ore
                        secondsToStart += activeService.getEveryDayAt().until(activeService.getStartAt().toLocalTime(), ChronoUnit.SECONDS) + 60*60*24;
                    } else { // altrimenti calcolo i secondi tra l'ora di avvio indicata e l'ora di avvio giornaliera
                        secondsToStart += activeService.getStartAt().toLocalTime().until(activeService.getEveryDayAt(), ChronoUnit.SECONDS);
                    }
                }
                // poi schedulo la prossima partenza
                scheduledService.add(scheduledExecutorService.scheduleAtFixedRate(getServiceWorkerAndInit(activeService.getName()), secondsToStart, 60*60*24, TimeUnit.SECONDS));
                
            } else if (activeService.getEverySeconds() != null) { // se è un servizio periodico (ogni N secondi)
                long secondsToStart = 0;
                if (!activeService.getStartAt().isAfter(now)) { // se startAt è <= oggi
                    
                    // la data effettiva di partenza sarà:
                    // se c'è quella di fine dell'esecuzione sarò quella
                    // altrimenti sarà quella indicata come startAt
                    ZonedDateTime startDate;
                    if (activeService.getTimeInterval() != null && activeService.getTimeInterval().hasUpperBound()) {
                        startDate = activeService.getTimeInterval().upper();
                    } else {
                        startDate = activeService.getStartAt();
                    }
                    
                    // alla data di avvio aggiungo i secondi 
                    ZonedDateTime actualStartDate = startDate.plus(activeService.getEverySeconds(), ChronoUnit.SECONDS);
                    if (!actualStartDate.isBefore(now)) {
                        secondsToStart = ChronoUnit.SECONDS.between(actualStartDate, now);
                    }
                } else { // se la data di avvio è nel futuro il thread sarà schedulato per partire in quella data
                    secondsToStart = ChronoUnit.SECONDS.between(now, activeService.getStartAt());
                }
                scheduledService.add(scheduledExecutorService.scheduleAtFixedRate(getServiceWorkerAndInit(activeService.getName()), secondsToStart, activeService.getEverySeconds(), TimeUnit.SECONDS));
            } else { // è un servizio una tantum
                // // se non è mai girato, oppure non è mai finito
                if (activeService.getTimeInterval() == null || !activeService.getTimeInterval().hasUpperBound()) {
                    long secondsToStart = 0;
                    
                    // se la data di partenza è nel futuro, calcolo i secondi mancanti al suo avvio, altrimenti i secondi al suo avvio sono 0
                    if (activeService.getStartAt().isAfter(now)) {
                        secondsToStart = ChronoUnit.SECONDS.between(activeService.getStartAt(), now);
                    }
                    scheduledService.add(scheduledExecutorService.schedule(getServiceWorkerAndInit(activeService.getName()), secondsToStart, TimeUnit.SECONDS));
                }
            }
            if (!scheduledService.isEmpty()) {
                activeServiceMap.put(activeService.getName(), scheduledService);
            }
        }
    }
    
    /**
     * Schedula il thread che si accorge delle modifiche alla tabella dei servizi
     * @throws MasterjobsWorkerException 
     */
    public void scheduleUpdateServiceDetector() throws MasterjobsWorkerException {
        scheduledExecutorService.scheduleAtFixedRate(getServiceWorkerAndInit(ChangeServiceDetector.class.getSimpleName()), 1l, 2l, TimeUnit.SECONDS);
    }
    
    private boolean isToday(ZonedDateTime zonedDateTime, ZonedDateTime now) {
        return zonedDateTime.toLocalDate().equals(now.toLocalDate());
    }
    
    private ServiceWorker getServiceWorkerAndInit(String name) throws MasterjobsWorkerException {
        ServiceWorker serviceWorker = masterjobsObjectsFactory.getServiceWorker(name);
        serviceWorker.init(masterjobsObjectsFactory, masterjobsJobsQueuer);
        return serviceWorker;
    }
    
}
