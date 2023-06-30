package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 *
 * @author gdm
 * chiunque deve estendere questa classe deve anche creare un costruttore vuoto
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class")
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class JobWorkerData implements JobWorkerDataInterface {
    
}
