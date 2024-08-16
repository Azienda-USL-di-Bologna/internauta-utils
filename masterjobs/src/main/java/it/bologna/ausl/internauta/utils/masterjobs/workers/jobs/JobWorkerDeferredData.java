package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

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