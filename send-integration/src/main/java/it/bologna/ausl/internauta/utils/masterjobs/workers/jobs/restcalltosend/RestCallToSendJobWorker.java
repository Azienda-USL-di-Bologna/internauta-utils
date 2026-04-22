package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.restcalltosend;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.service.sendintegration.api.FruitoreApi;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.sendintegration.model.LottoBase;
import it.bologna.ausl.internauta.utils.sendintegration.authorization.SendIntegrationAuthorizationUtils;
import it.bologna.ausl.internauta.utils.sendintegration.invoker.ApiException;
import it.bologna.ausl.model.entities.sendintegration.SendIntegrationConfiguration;
import java.time.ZonedDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import it.bologna.ausl.internauta.utils.sendintegration.invoker.ApiResponse;
import it.bologna.ausl.model.entities.sendintegration.DocumentoLottoEntity;
import it.bologna.ausl.model.entities.sendintegration.QDocumentoLottoEntity;
import java.util.List;
import java.util.stream.Stream;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class RestCallToSendJobWorker extends JobWorker<RestCallToSendJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(RestCallToSendJobWorker.class);
    private final String name = RestCallToSendJobWorker.class.getSimpleName();
    
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
        FruitoreApi fruitoreApi = new FruitoreApi();
        fruitoreApi.getApiClient().setBasePath(basePath).setBearerToken(token);
        ApiResponse<LottoBase> resp;
        DocumentoLottoEntity.DocumentiLottoStatus statusDaImpostare;
        switch (restCall) {
            case ELABORA_LOTTO_RICEVUTO -> {
                try {
                    DocumentoLottoEntity.DocumentiLottoStatus statusDaVerificare = DocumentoLottoEntity.DocumentiLottoStatus.SCARICATO_DA_COMUNICARE;
                    statusDaImpostare = DocumentoLottoEntity.DocumentiLottoStatus.SCARICATO_COMUNICATO;
                    if (isAllDocumentiLottoEntitiesInStatus(jobData.getLottoElaborato().getPaId(), jobData.getLottoElaborato().getLottoId(), statusDaVerificare)) {
                        resp = fruitoreApi.elaboraLottoRicevutoWithHttpInfo(jobData.getLottoBaseConEventualiErrori());
                    } else {
                        String error = String.format(
                            "impossibile eseguire la chiamata POST al %s perché lo status di tutti i documenti del lotto non è %s", 
                            restCall.toString(), statusDaImpostare);
                        log.error(error);
                        throw new MasterjobsWorkerException(error);
                    }
                } catch (ApiException ex) {
                    String error = String.format("eccezione nella chiamata POST al %s", restCall.toString());
                    log.error(error, ex);
                    throw new MasterjobsWorkerException(error, ex);
                }
            }
            case LOTTO_ELABORATO -> {
                try {
                    DocumentoLottoEntity.DocumentiLottoStatus statusDaVerificare = DocumentoLottoEntity.DocumentiLottoStatus.ELABORATO_DA_COMUNICARE;
                    statusDaImpostare = DocumentoLottoEntity.DocumentiLottoStatus.ELABORATO_COMUNICATO;
                    if (isAllDocumentiLottoEntitiesInStatus(jobData.getLottoElaborato().getPaId(), jobData.getLottoElaborato().getLottoId(), statusDaVerificare)) {
                        resp = fruitoreApi.lottoElaboratoWithHttpInfo(jobData.getLottoElaborato());
                    } else {
                        String error = String.format(
                            "impossibile eseguire la chiamata POST al %s perché lo status di tutti i documenti del lotto non è %s", 
                            restCall.toString(), statusDaImpostare);
                        log.error(error);
                        throw new MasterjobsWorkerException(error);
                    }
                    
                } catch (ApiException ex) {
                    String error = String.format("eccezione nella chiamata POST al %s", restCall.toString());
                    log.error(error, ex);
                    throw new MasterjobsWorkerException(error, ex);
                }
            }
            default -> {
                String error = String.format("restCall non prevista %s", restCall);
                log.error(error);
                throw new MasterjobsWorkerException(error);
            }
        }
        
        if (resp.getStatusCode() >= 200 && resp.getStatusCode() <= 299) {
            if (resp.getData() != null) {
                try {
                    updateDocumentiLotto(resp.getData().getPaId(), resp.getData().getLottoId(), statusDaImpostare);
                } catch (Throwable ex) {
                    String error = String.format("errore nell'update dello status a %s dei documento lotto", statusDaImpostare);
                    log.error(error, ex);
                    throw new MasterjobsWorkerException(error, ex);
                }
                res = new RestCallToSendJobWorkerResult(resp.getData());
            } else {
                String error = String.format("La risposta della chiamata POST al %s: non ha body", restCall.toString());
                log.error(error);
                throw new MasterjobsWorkerException(error);
            }
        } else {
            String error = String.format("errore nella chiamata POST al %s: ha tornato %s", restCall.toString(), resp.getStatusCode());
            log.error(error);
            log.error(resp.toString());
            throw new MasterjobsWorkerException(error);
        }
        
        log.info("job finito!");
        return res;
    }
    
    private Boolean isAllDocumentiLottoEntitiesInStatus(String paId, String lottoId, DocumentoLottoEntity.DocumentiLottoStatus status) {
        QDocumentoLottoEntity qDocumentoLottoEntity = QDocumentoLottoEntity.documentoLottoEntity;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        Stream<DocumentoLottoEntity.DocumentiLottoStatus> documentiLotto = queryFactory
            .select(qDocumentoLottoEntity.status)
            .from(qDocumentoLottoEntity)
            .where(
                qDocumentoLottoEntity.paId.eq(paId).and(
                qDocumentoLottoEntity.lottoId.eq(lottoId))
            )
            .stream();
        return documentiLotto.allMatch(d -> d == status);
    }
    
    private void updateDocumentiLotto(String paId, String lottoId, DocumentoLottoEntity.DocumentiLottoStatus status) {
        QDocumentoLottoEntity qDocumentoLottoEntity = QDocumentoLottoEntity.documentoLottoEntity;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        queryFactory
            .update(qDocumentoLottoEntity)
            .set(qDocumentoLottoEntity.status, status)
            .where(
                qDocumentoLottoEntity.paId.eq(paId).and(
                qDocumentoLottoEntity.lottoId.eq(lottoId))
            )
            .execute();
    }
}
