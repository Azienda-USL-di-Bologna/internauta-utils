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
    private SessioneVersamento.TipologiaVersamento tipologiaVersamento;
    
    public VersatoreJobWorkerData() {
    }
    
    public VersatoreJobWorkerData(String hostId, SessioneVersamento.TipologiaVersamento tipologiaVersamento) {
        this.hostId = hostId;
        this.tipologiaVersamento = tipologiaVersamento;
    }

    public SessioneVersamento.TipologiaVersamento getTipologiaVersamento() {
        return tipologiaVersamento;
    }

    public void setTipologiaVersamento(SessioneVersamento.TipologiaVersamento tipologiaVersamento) {
        this.tipologiaVersamento = tipologiaVersamento;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }
}
