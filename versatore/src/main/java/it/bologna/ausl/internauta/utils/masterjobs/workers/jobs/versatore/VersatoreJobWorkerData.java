package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Dati del job Versatore
 * 
 * @author gdm
 */
public class VersatoreJobWorkerData  extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(VersatoreJobWorkerData.class);


//    private AzioneVersamento azioneVersamento;
    private Integer idAzienda;
    private String hostId;
    private Boolean forzatura = false;
    private Integer poolSize;
    private Integer idPersonaForzatura;
    private Map<String,Object> params;
    private String username;
    private String password;
    private String urlVersSync;
    
    public VersatoreJobWorkerData() {
    }

    /**
     * Costruisce i dati per il job di Versamento
     * @param idAzienda azienda per la quale effettuare i versamenti
     * @param hostId hostId della configurazione del versatore (tabella versatore.configurations)
     * @param forzatura indica se il job si sta eseguendo per una forzatura utente
     * @param poolSize dimensione del pool di threads (numero massimo di threads contemporanei di versamento)
     * @param idPersonaForzatura persona che effettua la forzatura
     * @param params parametri di versamento
     */
    public VersatoreJobWorkerData(Integer idAzienda, String hostId, Boolean forzatura, Integer poolSize, Integer idPersonaForzatura, Map<String,Object> params, String username, String password, String urlVersSync) {
        this.idAzienda = idAzienda;
        this.hostId = hostId;
        this.forzatura = forzatura;
        this.poolSize = poolSize;
        this.idPersonaForzatura = idPersonaForzatura;
        this.params = params;
        this.username = username;
        this.password = password;
        this.urlVersSync = urlVersSync;
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
    
    public Boolean getForzatura() {
        return forzatura;
    }

    public void setForzatura(Boolean forzatura) {
        this.forzatura = forzatura;
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

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrlVersSync() {
        return urlVersSync;
    }

    public void setUrlVersSync(String urlVersSync) {
        this.urlVersSync = urlVersSync;
    }
    
    
    
    
}
