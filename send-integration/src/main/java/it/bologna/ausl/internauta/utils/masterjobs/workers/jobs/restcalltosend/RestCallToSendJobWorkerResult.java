package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.restcalltosend;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.send_integration.model.LottoBase;

/**
 *
 * @author gdm
 */
public class RestCallToSendJobWorkerResult extends JobWorkerResult {
    private LottoBase lottoBase;

    public RestCallToSendJobWorkerResult(LottoBase lottoBase) {
        this.lottoBase = lottoBase;
    }

    public LottoBase getLottoBase() {
        return lottoBase;
    }

    public void setLottoBase(LottoBase lottoBase) {
        this.lottoBase = lottoBase;
    }
}
