package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.controlloidoneita;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Dati del job di controllo di idoneità
 * 
 * @author gdm
 */
public class IdoneitaCheckerJobWorkerData  extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(IdoneitaCheckerJobWorkerData.class);

    private Integer idAzienda;
    private String hostId;
    private Boolean controllaArchivi = false;
    private Boolean controllaDocs = false;
    
    public IdoneitaCheckerJobWorkerData() {
    }

    /**
     * Costruisce i dati per il job di Versamento
     * @param idAzienda azienda per la quale effettuare i versamenti
     * @param hostId hostId della configurazione del versatore (tabella versatore.configurations)
     * @param controllaArchivi indica se il job deve controllare l'idoneità degli archivi
     * @param controllaDocs indica se il job deve controllare l'idoneità dei doc
     */
    public IdoneitaCheckerJobWorkerData(Integer idAzienda, String hostId, Boolean controllaArchivi, Boolean controllaDocs) {
        this.idAzienda = idAzienda;
        this.hostId = hostId;
        this.controllaArchivi = controllaArchivi;
        this.controllaDocs = controllaDocs;
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

    public Boolean getControllaArchivi() {
        return controllaArchivi;
    }

    public void setControllaArchivi(Boolean controllaArchivi) {
        this.controllaArchivi = controllaArchivi;
    }

    public Boolean getControllaDocs() {
        return controllaDocs;
    }

    public void setControllaDocs(Boolean controllaDocs) {
        this.controllaDocs = controllaDocs;
    }
}
