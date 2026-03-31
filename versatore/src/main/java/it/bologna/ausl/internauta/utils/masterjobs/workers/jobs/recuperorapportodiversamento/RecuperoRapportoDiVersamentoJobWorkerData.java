package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.recuperorapportodiversamento;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author boria
 */
public class RecuperoRapportoDiVersamentoJobWorkerData extends JobWorkerData {

    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(RecuperoRapportoDiVersamentoJobWorkerData.class);

    private Integer idAzienda;
    private String hostId;
    private Integer poolSize;
    private Map<String, Object> params;

    public RecuperoRapportoDiVersamentoJobWorkerData(Integer idAzienda, String hostId, Integer poolSize, Map<String, Object> params) {
        this.idAzienda = idAzienda;
        this.hostId = hostId;
        this.poolSize = poolSize;
        this.params = params;
    }

    public Integer getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Integer idAzienda) {
        this.idAzienda = idAzienda;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public Integer getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(Integer poolSize) {
        this.poolSize = poolSize;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

}
