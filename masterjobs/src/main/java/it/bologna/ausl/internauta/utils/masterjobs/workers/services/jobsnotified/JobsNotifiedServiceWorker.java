package it.bologna.ausl.internauta.utils.masterjobs.workers.services.jobsnotified;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsParsingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsRuntimeExceptionWrapper;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerDataInterface;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.model.entities.masterjobs.JobNotified;
import it.bologna.ausl.model.entities.masterjobs.QJobNotified;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import org.hibernate.Session;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class JobsNotifiedServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(JobsNotifiedServiceWorker.class);

    public static final String NEW_JOB_NOTIFIED_NOTIFY = "new_job_notified_notify";

    @Autowired
    private ObjectMapper objectMapper;
    
    private JPAQueryFactory queryFactory;
    private final QJobNotified qJobNotified = QJobNotified.jobNotified;
    
    @Override
    public void preWork() throws MasterjobsWorkerException {
        queryFactory = new JPAQueryFactory(entityManager);
        Session session = entityManager.unwrap(Session.class);

        // all'avvio schedulo il job per recuperare il pregresso
        transactionTemplate.executeWithoutResult(a -> {
            try {
                extractCreateAndQueueJobs();
            } catch (MasterjobsWorkerException ex) {
                throw new MasterjobsRuntimeExceptionWrapper(ex);
            }
        });
        
        session.doWork((Connection connection) -> {
            try {
                try (Statement listenStatement = connection.createStatement()) {
                    log.info(String.format("executing LISTEN on %s", NEW_JOB_NOTIFIED_NOTIFY));
                    listenStatement.execute(String.format("LISTEN %s", NEW_JOB_NOTIFIED_NOTIFY));
                    log.info("LISTEN completed");
                }
            } catch (Throwable ex) {
                String errorMessage = String.format("error executing LISTEN %s", NEW_JOB_NOTIFIED_NOTIFY);
                log.error(errorMessage, ex);
                throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
            }
        });
    }
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
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

                        // attendo una notifica per 10 secondi poi mi rimetto in attesa. In modo da fermarmi nel caso isStopped sia "true"
                        PGNotification notifications[] = pgc.getNotifications(10000);

                        if (notifications != null && notifications.length > 0) {
                            log.info(String.format("received notification: %s with paylod: %s", notifications[0].getName(), notifications[0].getParameter()));
                            log.info("Launching extractCreateAndQueueJobs()...");
                            transactionTemplate.executeWithoutResult(a -> {
                                try {
                                    extractCreateAndQueueJobs();
                                } catch (MasterjobsWorkerException ex) {
                                    throw new MasterjobsRuntimeExceptionWrapper(ex);
                                }
                            });
                        }
                    }
                }
            } catch (Throwable ex) {
                String errorMessage = String.format("error on managing %s notification", NEW_JOB_NOTIFIED_NOTIFY);
                log.error(errorMessage, ex);
                throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
            }
        });
        log.info(String.format("%s ended", getName()));
        return null;
    }
    
    private void extractCreateAndQueueJobs() throws MasterjobsWorkerException  {
        log.info("reading jobs to create...");
        boolean done = false;
        do {
            List<JobNotified> jobsNotified = queryFactory
                .select(qJobNotified)
                .from(qJobNotified)
                .orderBy(qJobNotified.id.asc())
                .limit(100)
                .fetch();
            if (jobsNotified != null && !jobsNotified.isEmpty()) {
                for (JobNotified jobNotified : jobsNotified) {
                    try {
                        createAndQueueJobs(jobNotified);
                        log.info(String.format("job %s, with jobs_notifies id: %s queued", jobNotified.getJobName(), jobNotified.getId()));
                    } catch (Exception ex) {
                        String errorMessage = String.format("error on create job %s, jobs_notifies id: %s", jobNotified.getJobName(), jobNotified.getId());
                        log.error(errorMessage, ex);
                        throw new MasterjobsWorkerException(errorMessage, ex);
                    }
                    try {
                        deleteJobNotifiedAndCommit(jobNotified.getId());
                        log.info(String.format("jobs_notifies with id: %s deleted ",jobNotified.getId()));
                    } catch (Exception ex) {
                        String errorMessage = String.format("error on delete jobs_notifies with id: %s", jobNotified.getId());
                        log.error(errorMessage, ex);
                        throw new MasterjobsWorkerException(errorMessage, ex);
                    }
                }
            } else {
                done = true;
            }
        } while (!done);
    }
    
    private void createAndQueueJobs(JobNotified jobNotified) throws MasterjobsParsingException, MasterjobsWorkerException, MasterjobsQueuingException {
        JobWorkerDataInterface jobData = JobWorkerDataInterface.parseFromJobData(objectMapper, jobNotified.getJobData());
        JobWorker jobWorker = masterjobsObjectsFactory.getJobWorker(jobNotified.getJobName(), jobData, jobNotified.getDeferred());
        masterjobsJobsQueuer.queue(
            jobWorker, 
            jobNotified.getObjectId(), 
            jobNotified.getObjectType(), 
            jobNotified.getApp(), 
            jobNotified.getWaitObject(), 
            jobNotified.getPriority(),
            jobNotified.getSkipIfAlreadyPresent());
    }
    
    private void deleteJobNotifiedAndCommit(Long jobNotifiedId) {
//        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult( a -> {
            queryFactory.delete(qJobNotified).where(qJobNotified.id.eq(jobNotifiedId)).execute();
        });
    }
}
