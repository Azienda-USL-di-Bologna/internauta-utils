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
public class MasterjobsHighPriorityJobsExecutionThread extends MasterjobsJobsExecutionThread {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsHighPriorityJobsExecutionThread.class);

    @Override
    public String getExecutorName() {
        return "HighPriorityExecutor";
    }
    
    @Override
    public void runExecutor() throws MasterjobsInterruptException {
        Set.SetPriority priority = Set.SetPriority.HIGH;
        while (true) {
            try {
                self.manageQueue(priority);
                switch (priority) {
                    case HIGHEST:
                        priority = Set.SetPriority.HIGH;
                        break;
                    case NORMAL:
                    case HIGH:
                        priority = Set.SetPriority.HIGHEST;
                }
//                self.manageQueue(priority);
                //self.manageNormalQueue();
                Thread.sleep(super.sleepMillis);
            } catch (MasterjobsInterruptException ex) {
                throw ex;
            } catch (MasterjobsReadQueueTimeout ex) {
                String queue = ex.getQueue();
                if (queue.equals(inQueueHigh)) {
                    priority = Set.SetPriority.HIGHEST;
                } else if (queue.equals(inQueueHighest)) {
                    priority = Set.SetPriority.NORMAL;
                } else { // normal
                    priority = Set.SetPriority.HIGH;
                }
            } catch (Exception ex) {
                log.error("execution error, moving next...", ex);
            }
        }
        
    }

    
}
