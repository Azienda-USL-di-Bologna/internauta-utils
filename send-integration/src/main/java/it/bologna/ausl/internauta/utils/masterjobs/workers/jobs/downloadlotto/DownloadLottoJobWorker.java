package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.downloadlotto;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteServiceException;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerInitializationException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.elaboralotto.lottosignerandregister.LottiSignerAndRegisterJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.elaboralotto.lottosignerandregister.LottiSignerAndRegisterJobWorkerData;
import it.bologna.ausl.internauta.utils.send_integration.model.Documento;
import it.bologna.ausl.internauta.utils.send_integration.model.Errore;
import it.bologna.ausl.internauta.utils.send_integration.model.Lotto;
import it.bologna.ausl.internauta.utils.send_integration.model.LottoBaseConEventualiErrori;
import it.bologna.ausl.internauta.utils.sendintegration.SendIntegrationConstants;
import it.bologna.ausl.internauta.utils.sendintegration.SendIntegrationSFTPManager;
import it.bologna.ausl.internauta.utils.sendintegration.SendIntegrationUtils;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationRepositoryManager;
import it.bologna.ausl.internauta.utils.sendintegration.exceptions.RuntimeExceptionContainer;
import it.bologna.ausl.internauta.utils.sendintegration.exceptions.SendIntegrationException;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.masterjobs.SetInterface;
import it.bologna.ausl.model.entities.sendintegration.DocumentoLottoEntity;
import it.bologna.ausl.model.entities.sendintegration.SendIntegrationConfiguration;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;

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
        
        boolean queueError = false;
        try {
            MinIOWrapper minIOWrapper = repositoryConfiguration.getRepositoryManager().getMinIOWrapper();

            DownloadLottoJobWorkerData jobData = getWorkerData();
            Lotto lotto = jobData.getLotto();
            List<Documento> documenti = lotto.getDocumenti();
            List<Errore> errori = new ArrayList<>();
            sftpManager.connect();
            if (sftpManager.existsPath(lotto.getInputBasePath())) {
                if (documenti != null && !documenti.isEmpty()) {
                    SendIntegrationConfiguration lepidaAziendaConfiguration = 
                        entityManager.find(SendIntegrationConfiguration.class, SendIntegrationConstants.Parameters.lepidaAziendaConfiguration);
                    Map<String, Object>  lepidaAziendaConfigurationMap = lepidaAziendaConfiguration.getValue();
                    Map<String, Object> paConfiguration = (Map<String, Object>) lepidaAziendaConfigurationMap.get(lotto.getPaId());
                    boolean aziendaActive = (boolean) paConfiguration.get("active");
                    if (aziendaActive) {
                        Integer idAzienda = (Integer) paConfiguration.get("id_azienda");
                        String codiceRegione = (String) paConfiguration.get("codice_regione");
                        String codiceAzienda = (String) paConfiguration.get("codice_azienda");
                        for (Documento documento : documenti) {
                            boolean inError = false;
                            String filePath = String.format("%s/%s", lotto.getInputBasePath(), documento.getInputFileName());
                            if (sftpManager.existsPath(filePath)) {
                                try (InputStream file = sftpManager.retriveFile(filePath)) {
                                    File tmpFile = File.createTempFile(String.format("lotto_%s_job%s_%s", lotto.getLottoId(), getJobId(), documento.getInputFileName()), PathUtils.getExtension(new File(documento.getInputFileName()).toPath()));
                                    try (OutputStream tmpFileOs = new FileOutputStream(tmpFile)) {
                                        IOUtils.copy(file, tmpFileOs);
                                    } catch (IOException ex) {
                                        inError = true;
                                        log.error(String.format("file non leggibile", ex));
                                        errori.add(new Errore(documento.getDocumentoId(), "FILE_NON_ LEGGIBILE").detail("File PDF non leggibile"));
                                    }
                                    if (!inError) {
                                        if (SendIntegrationUtils.isPdf(tmpFile)) {
                                            if (SendIntegrationUtils.getSha256Base64Encoded(tmpFile).equals(documento.getInputFileHash())) {

                                                //TODO: proseguire
                                                Map<String, String> metadata = new HashMap<>();
                                                metadata.put("paId", lotto.getPaId());
                                                metadata.put("lottoId", lotto.getLottoId());
                                                metadata.put("documentoId", documento.getDocumentoId());
                                                String repoFileId = null;
                                                //minIOWrapper.put(tmpFile, filePath, name, filePath, metadata, deferred);

                                                try {
                                                    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                                                    DocumentoLottoEntity docLottoEntity = transactionTemplate.execute(a -> {
                                                        try {
                                                            DocumentoLottoEntity innerDocLottoEntity = buildDocumentoLottoEntity(lotto, documento, repoFileId);
                                                            entityManager.persist(innerDocLottoEntity);
                                                            LottiSignerAndRegisterJobWorkerData lottiSignerAndRegisterJobWorkerData = 
                                                                    new LottiSignerAndRegisterJobWorkerData(lotto.getPaId(), lotto.getLottoId());
                                                            LottiSignerAndRegisterJobWorker jobWorker = masterjobsObjectsFactory.getJobWorker(
                                                                    LottiSignerAndRegisterJobWorker.class, 
                                                                    lottiSignerAndRegisterJobWorkerData, false
                                                            );
                                                            masterjobsJobsQueuer.queueOnCommit(Arrays.asList(jobWorker), filePath, filePath, ip, deferred, SetInterface.SetPriority.NORMAL, ip);
                                                            return innerDocLottoEntity;
                                                        } catch (Exception ex) {
                                                            throw new RuntimeExceptionContainer(ex);
                                                        }
                                                    });
                                                } catch (Throwable ex) {
                                                    queueError = true;
                                                    String error = String.format(
                                                        "errore nell'accodamento dal job %s per il lottoId %s con paId %s",
                                                        LottiSignerAndRegisterJobWorker.class.getSimpleName(), 
                                                        lotto.getLottoId(),
                                                        lotto.getPaId());
                                                    log.error(error);
                                                }
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
                    } else {
                        String error = String.format("L'integrazione con send è stata disabilitata per l'azienda con pdID %s", lotto.getPaId());
                        log.error(error);
                        throw new MasterjobsWorkerException(error);
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
        
        if (queueError) {
            String error = String.format(
                "non tutti i job %s sono stati accodati, considero il job attuale in errore in modo che riproverà l'accodamento al prossimo giro",
                LottiSignerAndRegisterJobWorker.class.getSimpleName());
            log.error(error);
            throw new MasterjobsWorkerException("non tutti i job accoadti");
        }
        log.info("job finito!");
        return null;
    }

    private DocumentoLottoEntity buildDocumentoLottoEntity(Lotto lotto, Documento doc, String repoFileId) {
        DocumentoLottoEntity docLotto = new DocumentoLottoEntity(
            lotto.getPaId(), lotto.getLottoId(), lotto.getTimestamp().toZonedDateTime(), lotto.getNumeroDocumenti(), lotto.getFirmatario(), 
            lotto.getInputBasePath(), lotto.getOutputBasePath(), doc.getDocumentoId(), doc.getInputFileName(), doc.getInputFileHash(), repoFileId
        );
        return docLotto;
    }
}
