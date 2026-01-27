package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.restcalltosend;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.internauta.utils.sendintegration.model.LottoBaseConEventualiErrori;
import it.bologna.ausl.internauta.utils.sendintegration.model.LottoElaborato;

/**
 *
 * @author gdm
 */
public class RestCallToSendJobWorkerData extends JobWorkerData {
    public static enum RestCalls {ELABORA_LOTTO_RICEVUTO, LOTTO_ELABORATO}
    
    private final String paId;
    private final RestCalls restCall;
    private final LottoBaseConEventualiErrori lottoBaseConEventualiErrori;
    private final LottoElaborato lottoElaborato;

    public static RestCallToSendJobWorkerData buildElaboraLottoRicevuto(LottoBaseConEventualiErrori lottoBaseConEventualiErrori) {
        return new RestCallToSendJobWorkerData(RestCalls.ELABORA_LOTTO_RICEVUTO, lottoBaseConEventualiErrori);
    }
    
    public static RestCallToSendJobWorkerData buildLottoElaborato(LottoElaborato lottoElaborato) {
        return new RestCallToSendJobWorkerData(RestCalls.LOTTO_ELABORATO, lottoElaborato);
    }
    
    private RestCallToSendJobWorkerData(RestCalls restCall, LottoBaseConEventualiErrori lottoBaseConEventualiErrori) {
        this.restCall = restCall;
        this.lottoBaseConEventualiErrori = lottoBaseConEventualiErrori;
        this.paId = lottoBaseConEventualiErrori.getPaId();
        this.lottoElaborato = null;
    }

    private RestCallToSendJobWorkerData(RestCalls restCall, LottoElaborato lottoElaborato) {
        this.restCall = restCall;
        this.lottoElaborato = lottoElaborato;
        this.paId = lottoElaborato.getPaId();
        this.lottoBaseConEventualiErrori = null;
    }

    public LottoBaseConEventualiErrori getLottoBaseConEventualiErrori() {
        return lottoBaseConEventualiErrori;
    }

    public LottoElaborato getLottoElaborato() {
        return lottoElaborato;
    }

    public RestCalls getRestCall() {
        return restCall;
    }

    public String getPaId() {
        return paId;
    }
}
