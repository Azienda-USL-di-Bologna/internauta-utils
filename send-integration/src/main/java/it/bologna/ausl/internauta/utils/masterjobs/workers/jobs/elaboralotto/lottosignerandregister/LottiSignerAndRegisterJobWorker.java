package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.elaboralotto.lottosignerandregister;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.downloadlotto.DownloadLottoJobWorker;
import it.bologna.ausl.internauta.utils.sendintegration.SendIntegrationSFTPManager;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationRepositoryConfiguration;
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
    private final String name = DownloadLottoJobWorker.class.getSimpleName();
    
    @Autowired
    private SendIntegrationRepositoryConfiguration repositoryConfiguration ;
    @Autowired
    private SendIntegrationHttpClientConfiguration httpClientConfiguration ;
    
    @Autowired
    private SendIntegrationSFTPManager sftpManager;
    
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in do doWork() di {}", getName());
        log.info("job finito!");
        return null;
    }
}
