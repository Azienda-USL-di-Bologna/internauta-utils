package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsParsingException;
import static it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData.getClassNameKey;
import java.util.Map;

/**
 *
 * @author gdm
 */
public interface JobWorkerDataInterface {    
    public static <T extends JobWorkerDataInterface> T parseFromJobData(ObjectMapper objectMapper, Map<String, Object> jobData) throws MasterjobsParsingException {
        if (jobData != null) {
            String className = (String) jobData.get(getClassNameKey());
            Class<T> workerDataClass;
            try {
                workerDataClass = (Class<T>) Class.forName(className);
            } catch (ClassNotFoundException ex) {
                throw new MasterjobsParsingException(String.format("unable to find class %s", className), ex);
            }
            return objectMapper.convertValue(jobData, workerDataClass);
        } else {
            return null;
        }
    }
    
    public default Map<String, Object> toJobData(ObjectMapper objectMapper) {
        return objectMapper.convertValue(this, new TypeReference<Map<String, Object>>(){});
    } 
}
