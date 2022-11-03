/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.masterjobs;

import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsBadDataException;
import it.bologna.ausl.internauta.utils.masterjobs.executors.jobs.MasterjobsWaitQueueJobsExecutionThread;
import it.bologna.ausl.model.entities.masterjobs.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class MasterjobsUtils {
    
    private static final Logger log = LoggerFactory.getLogger(MasterjobsUtils.class);
    
    @Value("${masterjobs.manager.jobs-executor.in-redis-queue-normal}")
    private String inQueueNormal;
    
    @Value("${masterjobs.manager.jobs-executor.in-redis-queue-high}")
    private String inQueueHigh;
    
    @Value("${masterjobs.manager.jobs-executor.in-redis-queue-highest}")
    private String inQueueHighest;
    
    /**
     * Torna il nome della coda relativa alla priorità passata
     * @param setPriority la priorità
     * @return il nome della coda relativa alla priorità passata
     * @throws MasterjobsBadDataException 
     */
    public String getQueueBySetPriority(Set.SetPriority setPriority) throws MasterjobsBadDataException {
        String destinationQueue;
        switch (setPriority) {
            case NORMAL:
                destinationQueue = this.inQueueNormal;
                break;
            case HIGH:
                destinationQueue = this.inQueueHigh;
                break;
            case HIGHEST:
                destinationQueue = this.inQueueHighest;
                break;
            default:
                String errorMessage = String.format("priority %s not excepted", setPriority);
                log.error(errorMessage);
                throw new MasterjobsBadDataException(errorMessage);
        }
        return destinationQueue;
    }
}
