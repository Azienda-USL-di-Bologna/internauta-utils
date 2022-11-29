package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class VersatoreJobWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(VersatoreJobWorkerData.class);

    public VersatoreJobWorkerData() {
    }
}
