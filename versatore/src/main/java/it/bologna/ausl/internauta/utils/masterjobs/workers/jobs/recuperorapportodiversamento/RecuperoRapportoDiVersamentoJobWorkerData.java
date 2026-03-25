package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.recuperorapportodiversamento;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author boria
 */
public class RecuperoRapportoDiVersamentoJobWorkerData extends JobWorkerData {

    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(RecuperoRapportoDiVersamentoJobWorkerData.class);

    private String hostId;

    public RecuperoRapportoDiVersamentoJobWorkerData(String hostId) {
        this.hostId = hostId;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

}
