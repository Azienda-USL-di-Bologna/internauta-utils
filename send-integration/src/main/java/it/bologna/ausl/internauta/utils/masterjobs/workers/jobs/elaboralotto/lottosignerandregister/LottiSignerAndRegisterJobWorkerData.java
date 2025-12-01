package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.elaboralotto.lottosignerandregister;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;

/**
 *
 * @author gdm
 */
public class LottiSignerAndRegisterJobWorkerData extends JobWorkerData {
    private String paId;
    private String lottoId;

    public LottiSignerAndRegisterJobWorkerData(String paId, String lottoId) {
        this.paId = paId;
        this.lottoId = lottoId;
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
}
