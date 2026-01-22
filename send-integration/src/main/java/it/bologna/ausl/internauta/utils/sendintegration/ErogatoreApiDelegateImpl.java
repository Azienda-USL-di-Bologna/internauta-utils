package it.bologna.ausl.internauta.utils.sendintegration;

import it.bologna.ausl.internauta.service.sendintegration.controller.ErogatoreApiDelegate;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.downloadlotto.DownloadLottoJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.downloadlotto.DownloadLottoJobWorkerData;
import it.bologna.ausl.internauta.utils.sendintegration.model.Lotto;
import it.bologna.ausl.internauta.utils.sendintegration.model.LottoBase;
import it.bologna.ausl.internauta.utils.sendintegration.exceptions.SendResponseStatusException;
import it.bologna.ausl.model.entities.masterjobs.Set;
import it.bologna.ausl.model.entities.sendintegration.SendIntegrationConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class ErogatoreApiDelegateImpl implements ErogatoreApiDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErogatoreApiDelegateImpl.class);
    
    @Value("${openapi.send-integration.active:false}")
    private Boolean sendIntegrationActive;
    
    @Autowired
    private MasterjobsJobsQueuer masterjobsJobsQueuer;

    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;

    @PersistenceContext
    private EntityManager entityManager;
    
    private final HttpServletRequest request;

    public ErogatoreApiDelegateImpl(HttpServletRequest request) {
        this.request = request;
    }
    
    @Override
    public ResponseEntity<LottoBase> elaboraLotto(Lotto lotto) {
        if (sendIntegrationActive) {
            SendIntegrationConfiguration lepidaAziendaConfiguration = entityManager.find(SendIntegrationConfiguration.class, SendIntegrationConfiguration.Ids.lepidaAziendaConfiguration);
            LOGGER.info("Richiesta di elaborazione del lotto ricevuta: paId={}, lottoId={}",
                lotto != null ? lotto.getPaId() : "n/d",
                lotto != null ? lotto.getLottoId() : "n/d");
            
            Map<String, Object>  lepidaAziendaConfigurationMap = lepidaAziendaConfiguration.getValue();
            Map<String, Object> paConfiguration = (Map<String, Object>) lepidaAziendaConfigurationMap.get(lotto.getPaId());
            boolean aziendaActive = (boolean) paConfiguration.get("active");
            if (aziendaActive) {
                DownloadLottoJobWorkerData jobdata = new DownloadLottoJobWorkerData(
                    lotto
                );
                DownloadLottoJobWorker jobWorker;
                try {
                    jobWorker = masterjobsObjectsFactory.getJobWorker(
                        DownloadLottoJobWorker.class,
                        jobdata,
                        false
                    );

                    jobWorker.doWork(); // Eseguo sincrono per le prove TODO: Rimuovere e metere queue in job notified
                    if (false) {
                        masterjobsJobsQueuer.queueOnCommit(
                            Arrays.asList(jobWorker),
                            null, // ObjectID 
                            null,
                            null,
                            false, // waitForObject
                            Set.SetPriority.NORMAL,
                            null
                        );
                    }
                } catch (Exception ex) {
                    //return ResponseEntity.internalServerError().body("Errore interno");
                    LOGGER.error("errore nell'accodamento del job", ex);
                    throw new SendResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno", ex);
                }
                LottoBase risposta = new LottoBase()
                    .paId(lotto.getPaId()) // paId, si intende quella del lotto arrivato o quella dell'azienda AUSLBO? o di altra azienda?
                    .lottoId(lotto.getLottoId())
                    .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                    .numeroDocumenti(lotto.getNumeroDocumenti());

                LOGGER.info("Richiesta di elaborazione lotto {} accettata", lotto.getLottoId());
                return ResponseEntity.ok(risposta);
            } else {
                String error = String.format("L'integrazione con send è disabilitata per l'azienda con pdID %s", lotto.getPaId());
                LOGGER.warn(error);
                throw new SendResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, error);
            }
        } else {
            LOGGER.warn("L'integrazione con send è disabilitata nell'application.properties, sulla proprietà: openapi.send-integration.active");
            throw new SendResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "L'integrazione con send non è attiva");
        }
    }

}
