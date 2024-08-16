package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.foo;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsObjectNotFoundException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.model.entities.masterjobs.DebuggingOption;
import java.time.ZonedDateTime;
import java.util.Map;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class FooWorker extends JobWorker {
    private static final Logger log = LoggerFactory.getLogger(FooWorker.class);
    private String name = "Foo";

    @Autowired
    private EntityManager em;
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in doWork()");
//        ObjectStatus o3 = new ObjectStatus();
//        FooWorkerData data = (FooWorkerData) getData();
//        o3.setObjectId(data.getParams1().toString());
//        o3.setState(ObjectStatus.ObjectState.IDLE);
//        em.persist(o3);
        if (false) {
            throw new MasterjobsWorkerException("prova errore");
        }
        return null;
    }

    @Override
    public boolean isExecutable() {
        try {
            //log.info("isExecutable: " + ZonedDateTime.now());
            Map jobWorkData = getJobWorkData();
            //log.info(jobWorkData == null? null: jobWorkData.toString());
            String debuggingParam = debuggingOptionsManager.getDebuggingParam(DebuggingOption.Key.test, String.class);
            if (debuggingParam.equalsIgnoreCase("write") && jobWorkData != null) {
                jobWorkData.put("key3", "tre");
                jobWorkData.put("key4", 4);
                jobWorkData.put("key5", false);
                setJobWorkData(jobWorkData);
            } else if (debuggingParam.equalsIgnoreCase("null") && jobWorkData != null) {
                setJobWorkData(null);
            }
            return debuggingParam.equalsIgnoreCase("gdm");
        } catch (Exception ex) {
            log.error("errore", ex);
            return false;
        }
    }
}
