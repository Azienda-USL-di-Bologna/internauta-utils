package it.bologna.ausl.internauta.utils.masterjobs.workers.services.changeservicedetector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsRuntimeExceptionWrapper;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.executors.services.MasterjobsServicesExecutionScheduler;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.model.entities.masterjobs.Service;
import java.sql.Connection;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.util.Map;
import org.hibernate.Session;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class ChangeServiceDetectorWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(ChangeServiceDetectorWorker.class);

    public static final String CHANGE_SERVICE_NOTIFY = "change_service_notify";

    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    private MasterjobsServicesExecutionScheduler masterjobsServicesExecutionScheduler;

    public void setMasterjobsServicesExecutionScheduler(MasterjobsServicesExecutionScheduler masterjobsServicesExecutionScheduler) {
        this.masterjobsServicesExecutionScheduler = masterjobsServicesExecutionScheduler;
    }
    
    @Override
    public void preWork() throws MasterjobsWorkerException {
        try {
            Session session = entityManager.unwrap(Session.class);
            session.doWork((Connection connection) -> {
                try {
                    try (Statement listenStatement = connection.createStatement()) {
                        log.info(String.format("executing LISTEN on %s", CHANGE_SERVICE_NOTIFY));
                        listenStatement.execute(String.format("LISTEN %s", CHANGE_SERVICE_NOTIFY));
                        log.info("LISTEN completed");
                    }
                } catch (Throwable ex) {
                    String errorMessage = String.format("error executing LISTEN %s", CHANGE_SERVICE_NOTIFY);
                    log.error(errorMessage, ex);
                    throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
                }
            });
        } catch (Exception ex) {
            log.error(String.format("error executing LISTEN %s", CHANGE_SERVICE_NOTIFY, ex));
            throw new MasterjobsRuntimeExceptionWrapper(ex);
        }
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        log.info(String.format("starting %s...", getName()));
        
        Session session = entityManager.unwrap(Session.class);
        session.doWork((Connection connection) -> {
            try {
                PGConnection pgc;
                while (!isStopped()) {
                    if (connection.isWrapperFor(PGConnection.class)) {
                        pgc = (PGConnection) connection.unwrap(PGConnection.class);
                        
                        // attendo una notifica per 10 secondi poi termino. Il service viene poi rischedulato ogni 30 secondi
                        PGNotification notifications[] = pgc.getNotifications(10000);
                        
                        if (notifications != null && notifications.length > 0) {
                            log.info(String.format("received notification: %s with paylod: %s", notifications[0].getName(), notifications[0].getParameter()));
                            for (PGNotification notification : notifications) {
                                log.info(String.format("notify: %s payload: %s", notification.getName(), notification.getParameter()));
                                manageChangeService(notification);
                            }
                        }
                    }
                }
            } catch (Throwable ex) {
                String errorMessage = String.format("error on managing %s notification", CHANGE_SERVICE_NOTIFY);
                log.error(errorMessage, ex);
                throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
            }
        });
        log.info(String.format("%s ended", getName()));
        return null;
    }
    
    private void manageChangeService(PGNotification notification) {
        log.info("managing notification...");
        Map<String, String> notifyPayload = null;
        try {
            notifyPayload = objectMapper.readValue(notification.getParameter(), new TypeReference<Map<String, String>>(){});
        } catch (Throwable t) {
            log.error("error parsing notify json, skipping...", t);
        }
        String action = null;
        try {
            String serviceName = notifyPayload.get("name");
            action = notifyPayload.get("action");
            switch (action) {
                case "start":
                    masterjobsServicesExecutionScheduler.stopService(serviceName);
                    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    Service service = transactionTemplate.execute(a -> {
                        return entityManager.find(Service.class, serviceName);
                    });
                    masterjobsServicesExecutionScheduler.scheduleService(service, ZonedDateTime.now());
                    break;
                case "stop":
                    masterjobsServicesExecutionScheduler.stopService(serviceName);
                    break;
                default:
                    throw new AssertionError(String.format("action %s not recognized", action));
            }
        } catch (Throwable t) {
            String serviceName = notifyPayload.get("name");
            log.error(String.format("error managing action %s, on service %s skipping...", action, serviceName), t);
        }
        
    }
}
