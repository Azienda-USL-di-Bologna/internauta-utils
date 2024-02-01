package it.bologna.ausl.internauta.utils.masterjobs.workers.services.jobsnotified;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsWorkingObject;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsParsingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsRuntimeExceptionWrapper;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsQueueData;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.TransactionDefinition;

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
    
    @Value("${masterjobs.manager.services-executor.jobs-notified-polling:false}")
    private Boolean usePolling;
    
//    @Value("${masterjobs.manager.services-executor.polling-seconds:20}")
//    private Boolean pollingSeconds;
    
    private JPAQueryFactory queryFactory;
    private final QJobNotified qJobNotified = QJobNotified.jobNotified;
    
    @Override
    public void preWork() throws MasterjobsWorkerException {
        queryFactory = new JPAQueryFactory(entityManager);

        /*
        se sono in modalità notify prima di mettermi in listen per le notify, accodo i comandi presenti in tabella.
        questo mi permette di accodare i comandi che sono stati inseriti mentre non ero in listen
        */
        if (serviceEntity.getWaitNotifyMillis() != null) {            

            Session session = entityManager.unwrap(Session.class);
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
            transactionTemplate.executeWithoutResult(a -> {
                try {
                    extractCreateAndQueueJobs();
                } catch (MasterjobsWorkerException ex) {
                    throw new MasterjobsRuntimeExceptionWrapper(ex);
                }
            });
        }
    }
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        Integer waitNotifyMillis = serviceEntity.getWaitNotifyMillis();
        if (waitNotifyMillis != null) {
            /*
            Se sono in modalità notify mi metto in attesa di notify per wait_notify_millis millisecondi.
            se wait_notify_millis è 0, allora vuol dire che voglio che questo servizio non termini mai restando sempre in listen
            in ogni caso faccio una getNotifications ogni 10 secondi per far si che se è stato fermato il masterjobs (isStopped == true) 
            il servizio riesca a terminare.
            */
            log.info(String.format("starting %s with notify...", getName()));
            Session session = entityManager.unwrap(Session.class);
            session.doWork((Connection connection) -> {
                try {
                    boolean stopLoop = false;
                    int notifyMillis;
                    while (!stopLoop && !isStopped()) {
                        if (waitNotifyMillis == 0) {
                            notifyMillis = 10000;
                            stopLoop = false;
                        } else {
                            notifyMillis = waitNotifyMillis;
                            stopLoop = true;
                        }
                        if (connection.isWrapperFor(PGConnection.class)) {
                            PGConnection pgc = (PGConnection) connection.unwrap(PGConnection.class);

                            // attendo una notifica per waitNotifyMillis poi termino e sarò rilanciato dal pool secondo le specifiche del servizio
                            PGNotification notifications[] = pgc.getNotifications(notifyMillis);

                            if (notifications != null && notifications.length > 0) {
                                log.info(String.format("received notification: %s with paylod: %s", notifications[0].getName(), notifications[0].getParameter()));
                                transactionTemplate.executeWithoutResult(a -> {
                                    try {
                                        log.info("Launching extractCreateAndQueueJobs()...");
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
        } else {
            log.info(String.format("starting %s with polling...", getName()));
            transactionTemplate.executeWithoutResult(a -> {
                try {
                    extractCreateAndQueueJobs();
                } catch (MasterjobsWorkerException ex) {
                    throw new MasterjobsRuntimeExceptionWrapper(ex);
                }
            });
        }
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
                        // per ogni job letto, lo inserisco nella tabella dei jobs, lo elimino dalla tabella dei jobs_notified, committo e lo accodo in redis
                        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                        MasterjobsQueueData masterjobsQueueData = transactionTemplate.execute( a -> {
                            MasterjobsQueueData res;
                            try {
                                res = createMasterjobsQueueData(jobNotified);
                                log.info(String.format("job %s, with jobs_notifies id: %s queued", jobNotified.getJobName(), jobNotified.getId()));
                            } catch (Exception ex) {
                                String errorMessage = String.format("error on create job %s, jobs_notifies id: %s", jobNotified.getJobName(), jobNotified.getId());
                                log.error(errorMessage, ex);
                                throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
                            }
                            try {
                                deleteJobNotified(jobNotified.getId());
                                log.info(String.format("jobs_notifies with id: %s deleted ",jobNotified.getId()));
                            } catch (Exception ex) {
                                String errorMessage = String.format("error on delete jobs_notifies with id: %s", jobNotified.getId());
                                log.error(errorMessage, ex);
                                throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
                            }
                            return res;
                        });
                        if (masterjobsQueueData != null) {
                            masterjobsJobsQueuer.insertInQueue(masterjobsQueueData);
                        }
                    } catch (Exception ex) {
                        String errorMessage = "error on manage jobs_notified";
                        log.error(errorMessage, ex);
                        throw new MasterjobsWorkerException(errorMessage, ex);
                    }
                }
            } else {
                done = true;
            }
        } while (!done);
    }
    
    private MasterjobsQueueData createMasterjobsQueueData(JobNotified jobNotified) throws MasterjobsParsingException, MasterjobsWorkerException, MasterjobsQueuingException {
        JobWorkerDataInterface jobData = JobWorkerDataInterface.parseFromJobData(objectMapper, jobNotified.getJobData());
        List<MasterjobsWorkingObject> workingObjects = jobNotified.getWorkingObjects();
        JobWorker jobWorker = masterjobsObjectsFactory.getJobWorker(jobNotified.getJobName(), jobData, jobNotified.getDeferred(), workingObjects);
        return masterjobsJobsQueuer.queue(
            jobWorker, 
            jobNotified.getObjectId(), 
            jobNotified.getObjectType(), 
            jobNotified.getApp(), 
            jobNotified.getWaitObject(), 
            jobNotified.getPriority(),
            jobNotified.getSkipIfAlreadyPresent(),
            true,
            jobNotified.getInsertedFrom());
    }
    
    private void deleteJobNotified(Long jobNotifiedId) {
//        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//        transactionTemplate.executeWithoutResult( a -> {
            queryFactory.delete(qJobNotified).where(qJobNotified.id.eq(jobNotifiedId)).execute();
//        });
    }
}
