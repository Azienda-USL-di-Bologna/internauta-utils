package it.bologna.ausl.internauta.utils.masterjobs.executors.services;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerInitializationException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.changeservicedetector.ChangeServiceDetectorWorker;
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

/**
 *
 * @author gdm
 * 
 * Classe che si occupa dello scheduling dei servizi
 */
public class MasterjobsServicesExecutionScheduler {   

    public MasterjobsServicesExecutionScheduler(EntityManager entityManager, MasterjobsObjectsFactory masterjobsObjectsFactory, ScheduledThreadPoolExecutor scheduledExecutorService) {
        this.entityManager = entityManager;
        this.masterjobsObjectsFactory = masterjobsObjectsFactory;
        this.scheduledExecutorService = scheduledExecutorService;
    }
    
    private final EntityManager entityManager;
    
    private final MasterjobsObjectsFactory masterjobsObjectsFactory;
 
    private final ScheduledThreadPoolExecutor scheduledExecutorService;
    
    private final Map<String, List<ScheduledFuture>> activeServiceMap = new HashMap<>();

    /**
     * Stoppa un servizio, se è presente nella mappa dei servizi in esecuzione e lo rimuove dalla mappa
     * @param serviceName il nome del servizio da stoppare
     */    
    public void stopService(String serviceName) {
        List<ScheduledFuture> scheduledServices = activeServiceMap.get(serviceName);
        if (scheduledServices != null && !scheduledServices.isEmpty()) {
            for (ScheduledFuture scheduledService : scheduledServices) {
                scheduledService.cancel(true);
            }
        }
        activeServiceMap.remove(serviceName);
    }
    
    public void scheduleServiceThreads() throws MasterjobsWorkerInitializationException {
        ZonedDateTime now = ZonedDateTime.now();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QService qService = QService.service;
        List<Service> activeServices = queryFactory
            .select(qService)
            .from(qService)
            .where(qService.active.eq(true))
            .fetch();
        for (Service activeService : activeServices) {
            List<ScheduledFuture> scheduledService = scheduleService(activeService, now);
        }
    }
    
    public List<ScheduledFuture> scheduleService(Service activeService, ZonedDateTime now) throws MasterjobsWorkerInitializationException {
        List<ScheduledFuture> scheduledService = new ArrayList<>();
        if (activeService.getEveryDayAt() != null) { // se è un servizio giornaliero
            long secondsToStart = 0;
            if (!activeService.getStartAt().isAfter(now)) { // se startAt è <= oggi
                
                /**
                 * Ho un servizio giornaliero attivo con startAt minore/uguale di oggi. 
                 * Quindi va capito se il servizio: 
                 * a) va lanciato subito (cioè oggi doveva già essere partito ma non lo ha ancora fatto).
                 * b) va lanciato oggi ma non subito (va lanciato all'ora di EveryDayAt)
                 * c) va lanciato domani (all'ora di EveryDayAt)
                 * 
                 * Caso a: Va lanciato subito se: 
                 *  - l'ora EveryDayAt è già stata raggiunta
                 *  - l'ultima ora in cui è terminato, se esiste, è minore di EveryDayAt, oppure se non è mai terminato
                 * 
                 * Caso b: Va lanciato oggi ma non subito se:
                 *  - l'ora EveryDayAt non è ancora stata raggiunta
                 * 
                 * Caso c: Va lanciato domani se:
                 *  - l'ora EveryDayAt è già stata raggiunta
                 *  - l'ultima volta in cui è terminato è maggiore/uguale di EveryDayAt
                 */
                
                // Questa variabile todayAtTheRightTime è la data di oggi all'ora di everyDayAt. E' la dataora su cui ragiono per capire tutto
                ZonedDateTime todayAtTheRightTime = now.with(activeService.getEveryDayAt());
                ZonedDateTime lastTermination = null;
                if (activeService.getTimeInterval() != null && activeService.getTimeInterval().hasUpperBound()) {
                    lastTermination = activeService.getTimeInterval().upper();
                }
                // Preparo i booleani informativi
                Boolean everyDayAtReached = !todayAtTheRightTime.isAfter(now); // EveryDayAt raggiunta, todayAtTheRightTime <= now()
                Boolean lastTerminationIsBeforeEveryDayAt = lastTermination == null ? true : lastTermination.isBefore(todayAtTheRightTime); // lastTermination <= EveryDayAt
                
                if (everyDayAtReached) {
                    if (lastTerminationIsBeforeEveryDayAt) {
                        // Caso a
                        ServiceWorker service = getServiceWorkerAndInit(activeService.getName(), activeService);
                        ScheduledFuture scheduleFuture = scheduledExecutorService.schedule(service, 0, TimeUnit.SECONDS);
                        service.setScheduledFuture(scheduleFuture);
                        scheduledService.add(scheduleFuture);
                        // Conto i secondi che intercorrono tra now e e domani all'orario di everyDayAt
                        secondsToStart = ChronoUnit.SECONDS.between(now, now.plusDays(1).with(activeService.getEveryDayAt()));
                    } else {
                        // Caso c
                        // Conto i secondi che intercorrono tra now e e domani all'orario di everyDayAt
                        secondsToStart = ChronoUnit.SECONDS.between(now, now.plusDays(1).with(activeService.getEveryDayAt()));
                    }                    
                } else {
                    // Caso b
                    // Conto i secondi che intercorrono tra now e now all'ora di everyDayAt
                    secondsToStart = ChronoUnit.SECONDS.between(now, todayAtTheRightTime);
                }

                
                // Commento codice vecchio
//                // se il servizio non è mai girato oppure non è mai terminato considero come prossima data di avvio del servizio la startAt
//                // altrimenti se è terminato, considero come prossima data di avvio la data di terminazione
//                ZonedDateTime startDate;
//                if (activeService.getTimeInterval() != null && activeService.getTimeInterval().hasUpperBound()) {
//                    startDate = activeService.getTimeInterval().upper();
//                } else {
//                    startDate = activeService.getStartAt();
//                }
//
//                // se il servizio non è mai girato oppure non è mai terminato
//                if (!isSameDate(startDate, now) || startDate.isAfter(now.with(activeService.getEveryDayAt()))
//                ) { // se la startAt è prima di oggi oppure è oggi e l'ora di avvio è già passata, il servizio deve partire subito
//                    ServiceWorker service = getServiceWorkerAndInit(activeService.getName(), activeService);
//                    ScheduledFuture scheduleFuture = scheduledExecutorService.schedule(service, 0, TimeUnit.SECONDS);
//                    service.setScheduledFuture(scheduleFuture);
//                    scheduledService.add(scheduleFuture);
//                    // il prossimo partire domani all'ora indicata, per cui aggiungo 24 ore
//                    secondsToStart = now.toLocalTime().until(startDate.toLocalTime(), ChronoUnit.SECONDS) + 60*60*24;
//                } else {
//                    secondsToStart = now.toLocalTime().until(activeService.getEveryDayAt(), ChronoUnit.SECONDS);
//                }
            } else { // se la data di avvio è nel futuro, calcolo i secondi che mancano ad arrivare alla data di avvio
                /**
                 * Conto i secondi che intercorrono tra now e il giorno in cui deve avvenire il primo lancio.
                 * Il giorno in cui deve partire il primo lancio potrebbe essere StartAt o il giorno successivo, 
                 * dipende dall'orario di StartAt rispetto all'orario di EveryDayAt
                 */
                
                // Se l'ora di avvio è dopo l'ora di avvio ogiornaliera, allora la data effettiva di avvio sarà il giorno dopo all'ora indicata
                if (activeService.getStartAt().toLocalTime().isAfter(activeService.getEveryDayAt())) {
                    
                    secondsToStart = ChronoUnit.SECONDS.between(now, activeService.getStartAt().plusDays(1).with(activeService.getEveryDayAt()));
                } else {
                    secondsToStart = ChronoUnit.SECONDS.between(now, activeService.getStartAt().with(activeService.getEveryDayAt()));
                }
                // Commento codice vecchio
                // prima calcolo i secondi che mancano fino alla data di avvio
//                secondsToStart = now.until(activeService.getStartAt(), ChronoUnit.SECONDS);
//
//                // poi se l'ora di avvio è dopo l'ora di avvi ogiornaliera, allora la data effettiva di avvio sarà il giorno dopo all'ora indicata
//                if (activeService.getStartAt().toLocalTime().isAfter(activeService.getEveryDayAt())) { 
//                    // per cui devo calcolare i secondi tra le due ore di avvio e sommare 24 ore
//                    secondsToStart += activeService.getEveryDayAt().until(activeService.getStartAt().toLocalTime(), ChronoUnit.SECONDS) + 60*60*24;
//                } else { // altrimenti calcolo i secondi tra l'ora di avvio indicata e l'ora di avvio giornaliera
//                    secondsToStart += activeService.getStartAt().toLocalTime().until(activeService.getEveryDayAt(), ChronoUnit.SECONDS);
//                }
            }
            // poi schedulo la prossima partenza
            ServiceWorker service = getServiceWorkerAndInit(activeService.getName(), activeService);
            ScheduledFuture scheduleFuture = scheduledExecutorService.scheduleAtFixedRate(service, secondsToStart, 60*60*24, TimeUnit.SECONDS);
            service.setScheduledFuture(scheduleFuture);
            scheduledService.add(scheduleFuture);

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
            ServiceWorker service = getServiceWorkerAndInit(activeService.getName(), activeService);
            ScheduledFuture scheduleFuture = scheduledExecutorService.scheduleAtFixedRate(service, secondsToStart, activeService.getEverySeconds(), TimeUnit.SECONDS);
            service.setScheduledFuture(scheduleFuture);
            scheduledService.add(scheduleFuture);
        } else { // è un servizio una tantum
            // se non è mai girato, oppure non è mai finito, oppure è un servizio che deve partire sempre all'avvio dell'applicazione
            if (    activeService.getTimeInterval() == null || 
                    !activeService.getTimeInterval().hasUpperBound() || 
                    (activeService.getScheduleOnStart() != null && activeService.getScheduleOnStart())) {
                long secondsToStart = 0;

                // se la data di partenza è nel futuro, calcolo i secondi mancanti al suo avvio, altrimenti i secondi al suo avvio sono 0
                if (activeService.getStartAt().isAfter(now)) {
                    secondsToStart = ChronoUnit.SECONDS.between(now, activeService.getStartAt());
                }
                ServiceWorker service = getServiceWorkerAndInit(activeService.getName(), activeService);
                ScheduledFuture scheduledFuture = scheduledExecutorService.schedule(service, secondsToStart, TimeUnit.SECONDS);
                service.setScheduledFuture(scheduledFuture);
                scheduledService.add(scheduledFuture);
            }
        }
        if (!scheduledService.isEmpty()) {
            activeServiceMap.put(activeService.getName(), scheduledService);
        }
        return scheduledService;
    }
    
    /**
     * Schedula il thread che si accorge delle modifiche alla tabella dei servizi
     * @throws MasterjobsWorkerException 
     */
//    public void scheduleUpdateServiceDetector() throws MasterjobsWorkerException {
//        ChangeServiceDetectorWorker service = (ChangeServiceDetectorWorker) getServiceWorkerAndInit(ChangeServiceDetectorWorker.class.getSimpleName(), null);
//        ScheduledFuture scheduledFuture = scheduledExecutorService.schedule(service, 0, TimeUnit.SECONDS);
//        service.setScheduledFuture(scheduledFuture);
//        service.setMasterjobsServicesExecutionScheduler(this);
//    }
    
    private boolean isSameDate(ZonedDateTime zonedDateTime, ZonedDateTime now) {
        return zonedDateTime.toLocalDate().equals(now.toLocalDate());
    }
    
    private ServiceWorker getServiceWorkerAndInit(String name, Service serviceEntity) throws MasterjobsWorkerInitializationException {
        ServiceWorker serviceWorker = masterjobsObjectsFactory.getServiceWorker(name);
        serviceWorker.setServiceEntity(serviceEntity);
        serviceWorker.setMasterjobsServicesExecutionScheduler(this);
        // serviceWorker.init(masterjobsObjectsFactory, masterjobsJobsQueuer);
        return serviceWorker;
    }
    
}
