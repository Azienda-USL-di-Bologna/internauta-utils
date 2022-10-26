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
public class MasterjobsNormalPriorityJobsExecutionThread extends MasterjobsJobsExecutionThread {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsNormalPriorityJobsExecutionThread.class);

    @Override
    public String getExecutorName() {
        return "NormalPriorityExecutor";
    }
    
    @Override
    public String getQueueAffinity() {
        return super.inQueueNormal;
    }
    
    @Override
    public void runExecutor() throws MasterjobsInterruptException {
        Set.SetPriority priority = Set.SetPriority.NORMAL;
        while (!stopped) {
            try {
                self.manageQueue(priority);
                
                priority = Set.SetPriority.HIGHEST;
                self.manageQueue(priority);
                
                priority = Set.SetPriority.HIGH;
                self.manageQueue(priority);
                
                priority = Set.SetPriority.NORMAL;
                Thread.sleep(super.sleepMillis);
            } catch (MasterjobsInterruptException ex) {
                throw ex;
            } catch (MasterjobsReadQueueTimeout ex) { // se non c'è nulla nella coda
                String queue = ex.getQueue();
                if (queue.equals(inQueueNormal)) {  // se non c'è nulla nella coda normal vado a guardare la higest
                    priority = Set.SetPriority.HIGHEST;
                } else if (queue.equals(inQueueHighest)) {  // se non c'è nulla nella coda highest vado a guardare la high
                    priority = Set.SetPriority.HIGH;
                } else { // se non c'è nulla nella coda high vado a guardare la normal
                    priority = Set.SetPriority.NORMAL;
                }
            } catch (Exception ex) {
                log.error("execution error, moving next...", ex);
            }
        }
        
    }
}
