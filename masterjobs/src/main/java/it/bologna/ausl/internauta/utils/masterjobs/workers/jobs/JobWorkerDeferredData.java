package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsParsingException;
import static it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData.getClassNameKey;
import java.util.Map;

/**
 *
 * @author gdm
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@class")
public abstract class JobWorkerDeferredData implements JobWorkerDataInterface {

    public abstract JobWorkerData toWorkerData();
    

}