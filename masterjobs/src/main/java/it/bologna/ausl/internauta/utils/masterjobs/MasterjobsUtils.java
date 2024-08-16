package it.bologna.ausl.internauta.utils.masterjobs;

import it.bologna.ausl.internauta.utils.masterjobs.configuration.MasterjobsApplicationConfig;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsBadDataException;
import it.bologna.ausl.model.entities.masterjobs.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class MasterjobsUtils {
    
    private static final Logger log = LoggerFactory.getLogger(MasterjobsUtils.class);
    
    @Autowired
    private MasterjobsApplicationConfig masterjobsApplicationConfig;
    
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
                destinationQueue = masterjobsApplicationConfig.getInQueueNormal();
                break;
            case HIGH:
                destinationQueue = masterjobsApplicationConfig.getInQueueHigh();
                break;
            case HIGHEST:
                destinationQueue = masterjobsApplicationConfig.getInQueueHighest();
                break;
            default:
                String errorMessage = String.format("priority %s not excepted", setPriority);
                log.error(errorMessage);
                throw new MasterjobsBadDataException(errorMessage);
        }
        return destinationQueue;
    }
}
