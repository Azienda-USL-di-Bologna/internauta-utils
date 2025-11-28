package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.elaboralotto;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteServiceException;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.send_integration.model.Documento;
import it.bologna.ausl.internauta.utils.send_integration.model.Errore;
import it.bologna.ausl.internauta.utils.send_integration.model.Lotto;
import it.bologna.ausl.internauta.utils.send_integration.model.LottoBaseConEventualiErrori;
import it.bologna.ausl.internauta.utils.sendintegration.SendIntegrationSFTPManager;
import it.bologna.ausl.internauta.utils.sendintegration.SendIntegrationUtils;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationRepositoryManager;
import it.bologna.ausl.internauta.utils.sendintegration.exceptions.SendIntegrationException;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gusgus
 */
@MasterjobsWorker
public class DownloadLottoJobWorker extends JobWorker<DownloadLottoJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(DownloadLottoJobWorker.class);
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
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in do doWork() di {}", getName());
        LottoBaseConEventualiErrori lottoBaseConEventualiErrori = new LottoBaseConEventualiErrori();
        
        try {
            MinIOWrapper minIOWrapper = repositoryConfiguration.getRepositoryManager().getMinIOWrapper();

            DownloadLottoJobWorkerData jobData = getWorkerData();
            Lotto lotto = jobData.getLotto();
            List<Documento> documenti = lotto.getDocumenti();
            List<Errore> errori = new ArrayList<>();
            sftpManager.connect();
            if (sftpManager.existsPath(lotto.getInputBasePath())) {
                if (documenti != null && !documenti.isEmpty()) {
                    for (Documento documento : documenti) {
                        boolean error = false;
                        String filePath = String.format("%s/%s", lotto.getInputBasePath(), documento.getInputFileName());
                        if (sftpManager.existsPath(filePath)) {
                            try (InputStream file = sftpManager.retriveFile(filePath)) {
                                File tmpFile = File.createTempFile(String.format("lotto_%s_job%s_%s", lotto.getLottoId(), getJobId(), documento.getInputFileName()), PathUtils.getExtension(new File(documento.getInputFileName()).toPath()));
                                try (OutputStream tmpFileOs = new FileOutputStream(tmpFile)) {
                                    IOUtils.copy(file, tmpFileOs);
                                } catch (IOException ex) {
                                    error = true;
                                    log.error(String.format("file non leggibile", ex));
                                    errori.add(new Errore(documento.getDocumentoId(), "FILE_NON_ LEGGIBILE").detail("File PDF non leggibile"));
                                }
                                if (!error) {
                                    if (SendIntegrationUtils.isPdf(tmpFile)) {
                                        if (SendIntegrationUtils.getSha256Base64Encoded(tmpFile).equals(documento.getInputFileHash())) {
                                            
                                            //TODO: proseguire
                                            minIOWrapper.put(tmpFile, filePath, name, filePath, metadata, deferred);
                                            
                                            
                                            
                                        } else {
                                            errori.add(new Errore(documento.getDocumentoId(), "HASH_ NON_VALIDO").detail("Valore del campo 'hash' non valido"));
                                        }
                                    } else {
                                        errori.add(new Errore(documento.getDocumentoId(), "FORMATO_ FILE_ERRATO").detail("Formato del file PDF errato"));
                                    }
                                }
                            }
                        } else {
                            errori.add(new Errore(documento.getDocumentoId(), "FILE_NON_ TROVATO").detail("File PDF non trovato"));
                        }
                    }
                }
            } else {
                errori.add(new Errore().code("PATH_NON_ TROVATO").detail("Path PDF non trovato"));
            }
        } catch (Exception ex) {
            log.error("errore nel job", ex);
            sftpManager.disconnect();
            // TODO gestione errore
        }
        
        log.info("job finito!");
        return null;
    }
    
    private void checkHash(File file, String hash) throws IOException, NoSuchAlgorithmException {
        if (!SendIntegrationUtils.getSha256Base64Encoded(file).equals(hash)) {
            throw new SendIntegrationException(file);
        }
    }
    
}
