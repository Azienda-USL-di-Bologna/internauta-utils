package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.elaboralotto;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.internauta.utils.send_integration.model.Lotto;

/**
 *
 * @author gsugus
 */
public class ElaboraLottoJobWorkerData extends JobWorkerData{
    private Lotto lotto;

    public ElaboraLottoJobWorkerData(Lotto lotto) {
        this.lotto = lotto;
    }

    public Lotto getLotto() {
        return lotto;
    }

    public void setLotto(Lotto lotto) {
        this.lotto = lotto;
    }
    
}
