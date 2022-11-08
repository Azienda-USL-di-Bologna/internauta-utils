package it.bologna.ausl.internauta.utils.masterjobs.workers.services.changeservicedetector;

import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsRuntimeExceptionWrapper;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Level;
import org.hibernate.Session;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class ChangeServiceDetector extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(ChangeServiceDetector.class);

    public static final String CHANGE_SERVICE_NOTIFY = "change_service_notify";
    
    private Session session;
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void init(MasterjobsObjectsFactory masterjobsObjectsFactory, MasterjobsJobsQueuer masterjobsJobsQueuer) throws MasterjobsWorkerException {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(a -> {
            try {
                super.init(masterjobsObjectsFactory, masterjobsJobsQueuer);
                
                session = entityManager.unwrap(Session.class);
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
            } catch (MasterjobsWorkerException ex) {
                throw new MasterjobsRuntimeExceptionWrapper(ex);
            }
        });
    }
    
    
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        log.info(String.format("starting %s...", getName()));
        session.doWork((Connection connection) -> {
            try {
                PGConnection pgc;
                if (connection.isWrapperFor(PGConnection.class)) {
                    pgc = (PGConnection) connection.unwrap(PGConnection.class);
                    
                    // attendo una notifica per 10 secondi poi termino. Il service viene poi rischedulato ogni 30 secondi
                    PGNotification notifications[] = pgc.getNotifications(10000);

                    if (notifications != null && notifications.length > 0) {
                        log.info(String.format("received notification %s. Launching scheduleManageCambiAssociazioniJob...", CHANGE_SERVICE_NOTIFY));
                        for (PGNotification notification : notifications) {
                            log.info(String.format("name: %s parameters: %s", notification.getName(), notification.getParameter()));
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
}
