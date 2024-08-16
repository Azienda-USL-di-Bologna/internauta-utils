package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.foo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerDataInterface;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerDeferredData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class FooWorkerDeferredData extends JobWorkerDeferredData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(FooWorkerDeferredData.class);
    
    @JsonIgnore
    private String name = "Foo";
    
    public FooWorkerDeferredData() {
    }
    
    @Override
    public JobWorkerData toWorkerData() {
        FooWorkerData wd = new FooWorkerData();
        wd.setName("erano i deferred data");
        wd.setParams2("params 1 deferred");
        return wd;
    }
}
