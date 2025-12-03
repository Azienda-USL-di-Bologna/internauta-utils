package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.restcalltosend;

import it.bologna.ausl.internauta.service.send_integration.api.FruitoreApi;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.send_integration.model.LottoBase;
import it.bologna.ausl.internauta.utils.sendintegration.authorization.SendIntegrationAuthorizationUtils;
import it.bologna.ausl.model.entities.sendintegration.SendIntegrationConfiguration;
import java.time.ZonedDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class RestCallToSendJobWorker extends JobWorker<RestCallToSendJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(RestCallToSendJobWorker.class);
    private final String name = RestCallToSendJobWorker.class.getSimpleName();
    
    @Autowired
    private FruitoreApi fruitoreApi;
    
    @Autowired
    private SendIntegrationAuthorizationUtils authorizationUtils;
    
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in do doWork() di {}", getName());
        
        JobWorkerResult res;
        
        RestCallToSendJobWorkerData jobData = getWorkerData();
        RestCallToSendJobWorkerData.RestCalls restCall = jobData.getRestCall();
        String paId = jobData.getPaId();
        
        SendIntegrationConfiguration lepidaAziendaConfiguration = 
            entityManager.find(SendIntegrationConfiguration.class, SendIntegrationConfiguration.Ids.lepidaAziendaConfiguration);
        Map<String, Object>  lepidaAziendaConfigurationMap = lepidaAziendaConfiguration.getValue();
        Map<String, Object> paConfiguration = (Map<String, Object>) lepidaAziendaConfigurationMap.get(paId);
        String basePath = (String) paConfiguration.get("base_path");
        
        ZonedDateTime now = ZonedDateTime.now();

        String token;
        try {
            token = authorizationUtils.generateTokenForFruitore(now);
        } catch (Exception ex) {
            String error = String.format("errore nella generazione del token", "elaboraLottoRicevuto");
            log.error(error);
            throw new MasterjobsWorkerException(error);
        }
        fruitoreApi.getApiClient().setBasePath(basePath).setBearerToken(token);
        ResponseEntity<LottoBase> resp;
        switch (restCall) {
            case ELABORA_LOTTO_RICEVUTO -> resp = fruitoreApi.elaboraLottoRicevutoWithHttpInfo(jobData.getLottoBaseConEventualiErrori());
            case LOTTO_ELABORATO -> resp = fruitoreApi.lottoElaboratoWithHttpInfo(jobData.getLottoElaborato());
            default -> {
                String error = String.format("restCall non prevista %s", restCall);
                log.error(error);
                throw new MasterjobsWorkerException(error);
            }
        }
        
        if (!resp.getStatusCode().is2xxSuccessful()) {
            String error = String.format("errore nella chiamata POST al %s: ha tornato %s", restCall.toString(), resp.getStatusCode().value());
            log.error(error);
            log.error(resp.toString());
            throw new MasterjobsWorkerException(error);
        }
        if (resp.hasBody()) {
            res = new RestCallToSendJobWorkerResult(resp.getBody());
        } else {
            String error = String.format("La risposta della chiamata POST al %s: non ha body", restCall.toString());
            log.error(error);
            throw new MasterjobsWorkerException(error);
        }
        
        log.info("job finito!");
        return res;
    }
}
