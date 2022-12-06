package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class VersatoreJobWorkerData  extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(VersatoreJobWorkerData.class);

    private String hostId;
    private Boolean forzatura;
    private Integer poolSize;
    private Integer idPersonaForzatura;
    
    public VersatoreJobWorkerData() {
    }
    
    public VersatoreJobWorkerData(String hostId, Boolean forzatura, Integer poolSize, Integer idPersonaForzatura) {
        this.hostId = hostId;
        this.forzatura = forzatura;
        this.poolSize = poolSize;
        this.idPersonaForzatura = idPersonaForzatura;
    }

    public Boolean getForzatura() {
        return forzatura;
    }

    public void setForzatura(Boolean forzatura) {
        this.forzatura = forzatura;
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

    public Integer getIdPersonaForzatura() {
        return idPersonaForzatura;
    }

    public void setIdPersonaForzatura(Integer idPersonaForzatura) {
        this.idPersonaForzatura = idPersonaForzatura;
    }
}
