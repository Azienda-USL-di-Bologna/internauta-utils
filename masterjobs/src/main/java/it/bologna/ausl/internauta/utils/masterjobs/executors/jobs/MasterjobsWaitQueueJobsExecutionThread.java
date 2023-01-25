package it.bologna.ausl.internauta.utils.masterjobs.executors.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsExecutionThreadsException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsInterruptException;
import it.bologna.ausl.model.entities.masterjobs.Set;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MasterjobsWaitQueueJobsExecutionThread extends MasterjobsJobsExecutionThread {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsWaitQueueJobsExecutionThread.class);

    @Override
    public String getExecutorName() {
        return "WaitQueueManager";
    }

    @Override
    public String getQueueAffinity() {
        return super.waitQueue;
    }
    
    @Override
    public void runExecutor() throws MasterjobsInterruptException {
        while (true) {
            try {
                checkCommand();
                manageWaitQueue();
                Thread.sleep(super.sleepMillis);
            } catch (MasterjobsInterruptException ex) {
                throw ex;
            } catch (Exception ex) {
                log.error("execution error, moving next...", ex);
            }
        }
    }

    private void manageWaitQueue() throws MasterjobsExecutionThreadsException {
        String queueDataString = (String) redisTemplate.opsForList().move(
            this.waitQueue, RedisListCommands.Direction.LEFT, 
            this.workQueue, RedisListCommands.Direction.RIGHT, 
            this.queueReadTimeoutMillis, TimeUnit.MILLISECONDS);
        if (queueDataString != null) {
//            log.info(String.format("readed: %s", queueDataString));
            MasterjobsQueueData queueData;
            try {
                queueData = masterjobsObjectsFactory.getMasterjobsQueueDataFromString(queueDataString);
            } catch (JsonProcessingException ex) {
                String errorMessage = String.format("json parse error from string %s", queueDataString);
                log.error(errorMessage);
                throw new MasterjobsExecutionThreadsException(errorMessage, ex);
            }
            Set set = super.getSet(queueData.getSet());
            if (set != null) {
                ZonedDateTime now = ZonedDateTime.now();
                // controllo se il set può essere eseguito
                if (
                    // se ho settato una data di controllo eseguibilità, allora il controllo lo farò solo se la data è giunta
                    (set.getNextExecutableCheck() == null || !now.isBefore(set.getNextExecutableCheck())) && 
                    // se la data è giunta, allora controllo l'eseguibilità sequenziale del set
                    (!set.getWaitObject() || super.isSetSequentiallyExecutable(set))
                    ) {
                    // se può essere eseguito, lo sposto nella sua coda originaria, davanti agli altri job
                    String destinationQueue = queueData.getQueue();
                    redisTemplate.opsForList().move(
                    this.workQueue, RedisListCommands.Direction.LEFT, 
                    destinationQueue, RedisListCommands.Direction.LEFT);
                } else {
                    
                    if (set.getNextExecutableCheck() != null) {
                        log.info(String.format("set %s not yet executable, now is %s, it will be executable at %s", set.getId(), now.toString(), set.getNextExecutableCheck().toString()));
                    }
                    
                    // se non può essere eseguito, lo riaccodo in fondo alla wait queue, in modo da ricontrollarlo dopo
                    redisTemplate.opsForList().move(
                    this.workQueue, RedisListCommands.Direction.LEFT, 
                    this.waitQueue, RedisListCommands.Direction.RIGHT);
                }
            } else {
                redisTemplate.delete(this.workQueue);
            }
        } else {
            // scaduto il timeout di lettura dalla coda redis, vuol dire che non c'è nulla da fare, mi riaccodo...
            //log.info(String.format("timeout of wait queue %s expired...", this.queueReadTimeoutMillis));
        }
    }
    
    private void aaa(Set set) {
        
    }
}
