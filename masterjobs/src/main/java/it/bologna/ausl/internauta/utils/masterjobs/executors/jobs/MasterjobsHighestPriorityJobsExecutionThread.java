package it.bologna.ausl.internauta.utils.masterjobs.executors.jobs;

import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsInterruptException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsReadQueueTimeout;
import it.bologna.ausl.model.entities.masterjobs.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MasterjobsHighestPriorityJobsExecutionThread extends MasterjobsJobsExecutionThread {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsHighestPriorityJobsExecutionThread.class);
    
    @Override
    public String getExecutorName() {
        return "HighestPriorityExecutor";
    }
    
@Override
    public String getQueueAffinity() {
        return super.inQueueHighest;
    }
    
    @Override
    public void runExecutor() throws MasterjobsInterruptException {
        Set.SetPriority priority = Set.SetPriority.HIGHEST;
        while (true) {
            try {
                self.manageQueue(priority);
                priority = Set.SetPriority.HIGHEST;
                Thread.sleep(super.sleepMillis);
            } catch (MasterjobsInterruptException ex) {
                throw ex;
            } catch (MasterjobsReadQueueTimeout ex) {
                String queue = ex.getQueue();
                if (queue.equals(inQueueHighest)) {
                    priority = Set.SetPriority.HIGH;
                } else if (queue.equals(inQueueHigh)) {
                    priority = Set.SetPriority.NORMAL;
                } else { // normal
                    priority = Set.SetPriority.HIGHEST;
                }
            } catch (Exception ex) {
                log.error("execution error, moving next...", ex);
            }
        }
    }
    
}
