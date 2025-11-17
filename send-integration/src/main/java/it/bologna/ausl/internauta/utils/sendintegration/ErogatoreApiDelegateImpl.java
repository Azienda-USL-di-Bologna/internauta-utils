package it.bologna.ausl.internauta.utils.sendintegration;

import it.bologna.ausl.internauta.service.send_integration.controller.ErogatoreApiDelegate;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsObjectsFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerInitializationException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.elaboralotto.ElaboraLottoJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.elaboralotto.ElaboraLottoJobWorkerData;
import it.bologna.ausl.internauta.utils.send_integration.model.Lotto;
import it.bologna.ausl.internauta.utils.send_integration.model.LottoBase;
import it.bologna.ausl.model.entities.masterjobs.Set;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired
    private MasterjobsJobsQueuer masterjobsJobsQueuer;

    @Autowired
    private MasterjobsObjectsFactory masterjobsObjectsFactory;

    @Override
    public ResponseEntity<LottoBase> elaboraLotto(Lotto lotto) {
        LOGGER.info("Richiesta di elaborazione del lotto ricevuta: paId={}, lottoId={}",
                lotto != null ? lotto.getPaId() : "n/d",
                lotto != null ? lotto.getLottoId() : "n/d");

        ElaboraLottoJobWorkerData jobdata = new ElaboraLottoJobWorkerData(
                lotto
        );

        ElaboraLottoJobWorker jobWorker;
        try {
            jobWorker = masterjobsObjectsFactory.getJobWorker(
                    ElaboraLottoJobWorker.class,
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
        } catch (MasterjobsWorkerInitializationException ex) {
            java.util.logging.Logger.getLogger(ErogatoreApiDelegateImpl.class.getName()).log(Level.SEVERE, null, ex);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (MasterjobsWorkerException ex) {
            java.util.logging.Logger.getLogger(ErogatoreApiDelegateImpl.class.getName()).log(Level.SEVERE, null, ex);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        LottoBase risposta = new LottoBase()
                .paId(lotto.getPaId()) // paId, si intende quella del lotto arrivato o quella dell'azienda AUSLBO? o di altra azienda?
                .lottoId(lotto.getLottoId())
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .numeroDocumenti(lotto.getNumeroDocumenti());
        
        LOGGER.info("Richiesta di elaborazione lotto {} accettata", lotto.getLottoId());
        return ResponseEntity.ok(risposta);
    }

}
