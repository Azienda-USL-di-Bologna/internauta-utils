package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsParsingException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author gdm
 */
public interface JobWorkerDataInterface {

    public final String CLASS_NAME_KEY = "@class";

    public static <T extends JobWorkerDataInterface> T parseFromJobData(ObjectMapper objectMapper, Map<String, Object> jobData) throws MasterjobsParsingException {
        if (jobData != null) {
            String className = (String) jobData.get(CLASS_NAME_KEY);
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

    public default HashMap<String, Object> toJobData(ObjectMapper objectMapper) {
        return objectMapper.convertValue(this, new TypeReference<HashMap<String, Object>>() {
        });
    }

//    /**
//     *
//     * @param <T>
//     * @param jobData
//     * @return true or false if the passed statement is equals
//     */
//    public <T extends JobWorkerDataInterface> Boolean equals(T jobData);
}
