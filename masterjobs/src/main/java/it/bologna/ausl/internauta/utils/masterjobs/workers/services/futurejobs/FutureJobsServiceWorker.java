package it.bologna.ausl.internauta.utils.masterjobs.workers.services.futurejobs;

import tools.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsParsingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsQueuingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsRuntimeExceptionWrapper;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsQueueData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerDataInterface;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MultiJobQueueDescriptor;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import it.bologna.ausl.model.entities.masterjobs.FutureJob;
import it.bologna.ausl.model.entities.masterjobs.FutureSet;
import it.bologna.ausl.model.entities.masterjobs.QFutureJob;
import it.bologna.ausl.model.entities.masterjobs.QFutureSet;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;

/**
 *
 * @author gusgus
 */
@MasterjobsWorker
public class FutureJobsServiceWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(FutureJobsServiceWorker.class);

    @Autowired
    private ObjectMapper objectMapper;
    private JPAQueryFactory queryFactory;
    private final QFutureSet qFutureSet = QFutureSet.futureSet;
    private final QFutureJob qFutureJob = QFutureJob.futureJob;
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public void preWork() throws MasterjobsWorkerException {
        queryFactory = new JPAQueryFactory(entityManager);
    }
    
    @Override
    public WorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info(String.format("starting %s with polling...", getName()));
        extractCreateAndQueueJobs();
        log.info(String.format("%s ended", getName()));
        return null;
    }
    
    private void extractCreateAndQueueJobs() throws MasterjobsWorkerException  {
        log.info("reading sets to create...");
        boolean done = false;
        do {
            List<FutureSet> futureSets = queryFactory
                .select(qFutureSet)
                .from(qFutureSet)
                .where(qFutureSet.executionTs.before(ZonedDateTime.now()))
                .orderBy(qFutureSet.id.asc())
                .limit(100)
                .fetch();
            if (futureSets != null && !futureSets.isEmpty()) {
                for (FutureSet futureSet : futureSets) {
                    try {
                        List<FutureJob> futureJobs = queryFactory
                            .select(qFutureJob)
                            .from(qFutureJob)
                            .where(qFutureJob.set.id.eq(futureSet.getId()))
                            .orderBy(qFutureJob.id.asc())
                            .fetch();
                        
                        // per ogni set, con i suoi job, lo inserisco nella tabella dei sets e jobs, lo elimino dalle tabelle dei future, committo e lo accodo in redis
                        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                        MasterjobsQueueData masterjobsQueueData = transactionTemplate.execute( a -> {
                            MasterjobsQueueData res;
                            
                            try {
                                res = createMasterjobsQueueData(futureSet, futureJobs);
                                log.info(String.format("future_set with id: %s queued", futureSet.getId()));
                            } catch (Exception ex) {
                                String errorMessage = String.format("error on create set future_set with id: %s", futureSet.getId());
                                log.error(errorMessage, ex);
                                throw new MasterjobsRuntimeExceptionWrapper(errorMessage, ex);
                            }
                            try {
                                deleteFutureSet(futureSet.getId());
                                log.info(String.format(" future_set with id: %s, deleted ",futureSet.getId()));
                            } catch (Exception ex) {
                                String errorMessage = String.format("error on delete future_set with id: %s", futureSet.getId());
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
    
    private MasterjobsQueueData createMasterjobsQueueData(FutureSet futureSet, List<FutureJob> futureJobs) throws MasterjobsParsingException, MasterjobsWorkerException, MasterjobsQueuingException {
        List<JobWorker> jobWorkers = new ArrayList();
        for (FutureJob futureJob : futureJobs) {
            JobWorkerDataInterface jobData = JobWorkerDataInterface.parseFromJobData(objectMapper, futureJob.getData());
            JobWorker jobWorker = masterjobsObjectsFactory.getJobWorker(futureJob.getName(), jobData, futureJob.getDeferred());
            jobWorkers.add(jobWorker);
        }
         MultiJobQueueDescriptor multiJobQueueDescriptor = MultiJobQueueDescriptor
                .newBuilder()
                .workers(jobWorkers)
                .objectId( futureSet.getObjectId())
                .objectType( futureSet.getObjectType())
                .app(futureSet.getApp())
                .waitForObject(futureSet.getWaitObject())
                .priority(futureSet.getPriority())
                .insertedFrom(futureSet.getInsertedFrom())
                .future(false)
                .executionTs(futureSet.getExecutionTs())
                .uuid(futureSet.getUuid())
                .build();
        return masterjobsJobsQueuer.queue(multiJobQueueDescriptor, true);
    }
    
    private void deleteFutureSet(Long futureSetId) {
        queryFactory.delete(qFutureJob).where(qFutureJob.set.id.eq(futureSetId)).execute();
        queryFactory.delete(qFutureSet).where(qFutureSet.id.eq(futureSetId)).execute();
    }
}
