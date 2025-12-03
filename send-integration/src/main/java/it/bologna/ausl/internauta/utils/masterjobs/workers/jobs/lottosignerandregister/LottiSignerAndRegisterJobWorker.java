package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.lottosignerandregister;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationScriptaWrapperConfiguration;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationScriptaWrapperManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class LottiSignerAndRegisterJobWorker extends JobWorker<LottiSignerAndRegisterJobWorkerData, JobWorkerResult> {
     private static final Logger log = LoggerFactory.getLogger(LottiSignerAndRegisterJobWorker.class);
    private final String name = LottiSignerAndRegisterJobWorker.class.getSimpleName();
    
    @Autowired
    private SendIntegrationScriptaWrapperConfiguration sendIntegrationScriptaWrapperConfiguration;
    
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in do doWork() di {}", getName());
        
        SendIntegrationScriptaWrapperManager scriptaWrapperManger = sendIntegrationScriptaWrapperConfiguration.getScriptaWrapperManger();
        scriptaWrapperManger.generaDocumentoPUProtocollato();
        
        log.info("job finito!");
        return null;
    }
}
