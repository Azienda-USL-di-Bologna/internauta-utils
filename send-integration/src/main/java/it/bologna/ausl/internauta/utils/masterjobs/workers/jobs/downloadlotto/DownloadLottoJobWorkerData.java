package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.downloadlotto;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.internauta.utils.sendintegration.model.Lotto;

/**
 *
 * @author gsugus
 */
public class DownloadLottoJobWorkerData extends JobWorkerData{
    private Lotto lotto;

    public DownloadLottoJobWorkerData(Lotto lotto) {
        this.lotto = lotto;
    }

    public Lotto getLotto() {
        return lotto;
    }

    public void setLotto(Lotto lotto) {
        this.lotto = lotto;
    }
    
}
