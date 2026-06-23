package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.lottosignerandregister;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import java.time.OffsetDateTime;

/**
 *
 * @author gdm
 */
public class LottiSignerAndRegisterJobWorkerData extends JobWorkerData {
    private String paId;
    private String lottoId;
    private String firmatario;
    private Integer numeroDocumenti;
    private String timestamp;
    private String outputBasePath;

    public LottiSignerAndRegisterJobWorkerData(String paId, String lottoId,  String firmatario, Integer numeroDocumenti, String timestamp, String outputBasePath) {
        this.paId = paId;
        this.lottoId = lottoId;
        this.firmatario = firmatario;
        this.numeroDocumenti = numeroDocumenti;
        this.timestamp = timestamp;
        this.outputBasePath = outputBasePath;
    }

    public String getPaId() {
        return paId;
    }

    public void setPaId(String paId) {
        this.paId = paId;
    }

    public String getLottoId() {
        return lottoId;
    }

    public void setLottoId(String lottoId) {
        this.lottoId = lottoId;
    }

    public String getFirmatario() {
        return firmatario;
    }

    public void setFirmatario(String firmatario) {
        this.firmatario = firmatario;
    }

    public Integer getNumeroDocumenti() {
        return numeroDocumenti;
    }

    public void setNumeroDocumenti(Integer numeroDocumenti) {
        this.numeroDocumenti = numeroDocumenti;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getOutputBasePath() {
        return outputBasePath;
    }

    public void setOutputBasePath(String outputBasePath) {
        this.outputBasePath = outputBasePath;
    }
}
