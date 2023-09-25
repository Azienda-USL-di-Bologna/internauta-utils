package it.bologna.ausl.internauta.utils.masterjobs.workers.services.autorelaunchjobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.configuration.MasterjobsApplicationConfig;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.WorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.services.ServiceWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class AutoRelaunchJobsWorker extends ServiceWorker {
    private static Logger log = LoggerFactory.getLogger(AutoRelaunchJobsWorker.class);

    public static final String CHANGE_SERVICE_NOTIFY = "auto_relaunch_jobs";
    
    @Autowired
    @Qualifier(value = "redisMaterjobs")
    protected RedisTemplate redisTemplate;
    
    @Autowired
    private MasterjobsApplicationConfig masterjobsApplicationConfig;
    

    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public WorkerResult doWork() throws MasterjobsWorkerException {
        Long errorQueueSize = redisTemplate.opsForList().size(masterjobsApplicationConfig.getErrorQueue());
        if (errorQueueSize != null && errorQueueSize > 0) {
            log.info(String.format("found %s jobs in error, relaunch...", errorQueueSize));
            masterjobsJobsQueuer.relaunchJobsInError();
        } else {
            log.info("no jobs in error");
        }
        log.info(String.format("%s ended", getName()));
        return null;
    }
}
