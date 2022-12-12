package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento.AzioneVersamento;
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


    private AzioneVersamento azioneVersamento;
    private Integer idAzienda;
    private String hostId;
    private Boolean forzatura = false;
    private Integer poolSize;
    private Integer idPersonaForzatura;
    
    public VersatoreJobWorkerData() {
    }

    /**
     * Costruisce i dati per il job di Versamento
     * @param azioneVersamento indica se il job farà il versamento, oppure il controllo dello stato dei docs IN_CARICO/IN_CARICO_CON_ERRORI
     * @param idAzienda azienda per la quale effettuare i versamenti
     * @param hostId hostId della configurazione del versatore (tabella versatore.configurations)
     * @param forzatura indica se il job si sta eseguendo per una forzatura utente
     * @param poolSize dimensione del pool di threads (numero massimo di threads contemporanei di versamento)
     * @param idPersonaForzatura persona che effettua la forzatura
     */
    public VersatoreJobWorkerData(AzioneVersamento azioneVersamento, Integer idAzienda, String hostId, Boolean forzatura, Integer poolSize, Integer idPersonaForzatura) {
        this.azioneVersamento = azioneVersamento;
        this.idAzienda = idAzienda;
        this.hostId = hostId;
        this.forzatura = forzatura;
        this.poolSize = poolSize;
        this.idPersonaForzatura = idPersonaForzatura;
    }

    public AzioneVersamento getAzioneVersamento() {
        return azioneVersamento;
    }

    public void setAzioneVersamento(AzioneVersamento azioneVersamento) {
        this.azioneVersamento = azioneVersamento;
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
}
