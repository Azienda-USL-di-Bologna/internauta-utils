package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import java.util.List;
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
    //private Boolean forzatura = false;
    private SessioneVersamento.TipologiaVersamento tipologia;
    private Integer poolSize;
    private Integer idPersonaForzatura;
    private Map<String,Object> params;
    private List<Integer> idDocsDaVersare; // In caso di forzatura o errore ritentabile, questi sono i doc da versare
    
    public VersatoreJobWorkerData() {
    }

    /**
     * Costruisce i dati per il job di Versamento
     * @param idAzienda azienda per la quale effettuare i versamenti
     * @param hostId hostId della configurazione del versatore (tabella versatore.configurations)
     * @param poolSize dimensione del pool di threads (numero massimo di threads contemporanei di versamento)
     * @param params parametri di versamento
     * @param tipologia
     */
    public VersatoreJobWorkerData(Integer idAzienda, String hostId, Integer poolSize, Map<String,Object> params, SessioneVersamento.TipologiaVersamento tipologia) {
        this.idAzienda = idAzienda;
        this.hostId = hostId;
        this.poolSize = poolSize;
        this.params = params;
        this.tipologia = tipologia;
    }

    /**
     * Costruisce i dati per il job di Versamento
     * @param idAzienda azienda per la quale effettuare i versamenti
     * @param hostId hostId della configurazione del versatore (tabella versatore.configurations)
     * @param poolSize dimensione del pool di threads (numero massimo di threads contemporanei di versamento)
     * @param idPersonaForzatura persona che effettua la forzatura
     * @param params parametri di versamento
     * @param tipologia
     * @param idDocsDaVersare
     */
    public VersatoreJobWorkerData(Integer idAzienda, String hostId, SessioneVersamento.TipologiaVersamento tipologia, Integer poolSize, Integer idPersonaForzatura, Map<String, Object> params, List<Integer> idDocsDaVersare) {
        this.idAzienda = idAzienda;
        this.hostId = hostId;
        this.tipologia = tipologia;
        this.poolSize = poolSize;
        this.idPersonaForzatura = idPersonaForzatura;
        this.params = params;
        this.idDocsDaVersare = idDocsDaVersare;
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

    public SessioneVersamento.TipologiaVersamento getTipologia() {
        return tipologia;
    }

    public void setTipologia(SessioneVersamento.TipologiaVersamento tipologia) {
        this.tipologia = tipologia;
    }

    public List<Integer> getIdDocsDaVersare() {
        return idDocsDaVersare;
    }

    public void setIdDocsDaVersare(List<Integer> idDocsDaVersare) {
        this.idDocsDaVersare = idDocsDaVersare;
    }

    
    
    
    
    
}
