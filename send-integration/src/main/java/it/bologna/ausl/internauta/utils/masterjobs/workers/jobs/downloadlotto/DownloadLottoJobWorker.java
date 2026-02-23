package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.downloadlotto;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.lottosignerandregister.LottiSignerAndRegisterJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.lottosignerandregister.LottiSignerAndRegisterJobWorkerData;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.restcalltosend.RestCallToSendJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.restcalltosend.RestCallToSendJobWorkerData;
import it.bologna.ausl.internauta.utils.sendintegration.model.Documento;
import it.bologna.ausl.internauta.utils.sendintegration.model.Errore;
import it.bologna.ausl.internauta.utils.sendintegration.model.Lotto;
import it.bologna.ausl.internauta.utils.sendintegration.model.LottoBaseConEventualiErrori;
import it.bologna.ausl.internauta.utils.sendintegration.SendIntegrationSFTPManager;
import it.bologna.ausl.internauta.utils.sendintegration.SendIntegrationUtils;
import it.bologna.ausl.internauta.utils.sendintegration.authorization.SendIntegrationAuthorizationUtils;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationRepositoryConfiguration;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.model.entities.masterjobs.SetInterface;
import it.bologna.ausl.model.entities.sendintegration.DocumentoLottoEntity;
import it.bologna.ausl.model.entities.sendintegration.SendIntegrationConfiguration;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class DownloadLottoJobWorker extends JobWorker<DownloadLottoJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(DownloadLottoJobWorker.class);
    private final String name = DownloadLottoJobWorker.class.getSimpleName();
    
    @Autowired
    private SendIntegrationRepositoryConfiguration repositoryConfiguration;
    
    @Autowired
    private SendIntegrationHttpClientConfiguration httpClientConfiguration;
    
    @Autowired
    private SendIntegrationAuthorizationUtils authorizationUtils;
    
    @Autowired
    private SendIntegrationSFTPManager sftpManager;
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in do doWork() di {}", getName());
        
        DownloadLottoJobWorkerData jobData = getWorkerData();
        Lotto lotto = jobData.getLotto();
        boolean internalError = false;
        List<Errore> errori = new ArrayList<>();
        List<DocumentoLottoEntity> documentoLottoEntityList = new ArrayList<>();
        
        // raccolgo i file temporanei creati per poterli cancellare alla fine (o ion caso di errore)
        List<File> tmpFiles = new ArrayList<>();
        
        // raccolgo i fileId caricati sul repository per poterli cancellare in caso di errore
        List<String> repoFileIds = new ArrayList<>();
        
        Integer idAzienda;
        String codiceRegione;
        String codiceAzienda;
        String basePath = null;
        try {
            MinIOWrapper minIOWrapper = repositoryConfiguration.getRepositoryManager().getMinIOWrapper();


            // leggo la configurazione dell'azienda a cui il lotto è associato, l'azienda è identificata dal paId del lotto
            SendIntegrationConfiguration lepidaAziendaConfiguration = 
                entityManager.find(SendIntegrationConfiguration.class, SendIntegrationConfiguration.Ids.lepidaAziendaConfiguration);
            Map<String, Object>  lepidaAziendaConfigurationMap = lepidaAziendaConfiguration.getValue();
            Map<String, Object> paConfiguration = (Map<String, Object>) lepidaAziendaConfigurationMap.get(lotto.getPaId());
            // controllo che l'integrazione con send sia attiva per l'azienda indicata
            boolean aziendaActive = (boolean) paConfiguration.get("active");
            if (aziendaActive) {
            
                List<Documento> documenti = lotto.getDocumenti();
                sftpManager.connect();
                if (sftpManager.existsPath(lotto.getInputBasePath())) {
                    // questa lista conterrà tutte le entità associate ai documenti del lotto, al termine del ciclo sarà schedulato un job che le gestirà

                    // leggo i parametri di configurazione dell'azienda
                    idAzienda = (Integer) paConfiguration.get("id_azienda");
                    codiceRegione = (String) paConfiguration.get("codice_regione");
                    codiceAzienda = (String) paConfiguration.get("codice_azienda");
                    basePath = (String) paConfiguration.get("base_path");

                    /*
                    ciclo su tutti i documenti del lotto:
                    uno per uno vengono scaricati dal server SFTP
                    vengono caricati sul nostro repository
                    viene creata l'entità DocumentoLottoEntity
                    al termine del ciclo le entità saranno salvata su db e verrà accodato il job che le gestirà
                    */
                    for (Documento documento : documenti) {
                        String filePath = String.format("%s/%s", lotto.getInputBasePath(), documento.getInputFileName());
                        if (sftpManager.existsPath(filePath)) {
                            try (InputStream file = sftpManager.retriveFile(filePath)) {
                                File tmpFile = File.createTempFile(String.format("lotto_%s_job%s_%s", lotto.getLottoId(), getJobId(), documento.getInputFileName()), PathUtils.getExtension(new File(documento.getInputFileName()).toPath()));
                                tmpFile.deleteOnExit();
                                tmpFiles.add(tmpFile);
                                try (OutputStream tmpFileOs = new FileOutputStream(tmpFile)) {
                                    IOUtils.copy(file, tmpFileOs);
                                } catch (IOException ex) {
                                    log.error(String.format("file non leggibile", ex));
                                    errori.add(new Errore().documentoId(documento.getDocumentoId()).code("FILE_NON_ LEGGIBILE").detail("File PDF non leggibile"));
                                    throw ex;
                                }
                                if (SendIntegrationUtils.isPdf(tmpFile)) {
                                    if (SendIntegrationUtils.getSha256Base64Encoded(tmpFile).equals(documento.getInputFileHash())) {

                                        // carica il file scaricato dal server SFTP sul repository
                                        Map<String, Object> metadata = new HashMap<>();
                                        metadata.put("paId", lotto.getPaId());
                                        metadata.put("lottoId", lotto.getLottoId());
                                        metadata.put("documentoId", documento.getDocumentoId());
                                        String repoPath = String.format("/send-integration/lotto_%s", lotto.getLottoId());
                                        MinIOWrapperFileInfo repoFileInfo = minIOWrapper.put(tmpFile, codiceAzienda, repoPath, documento.getInputFileName(), metadata, true);
                                        String repoFileId = repoFileInfo.getFileId();
                                        repoFileIds.add(repoFileId);
                                        // creo l'entità da salvare su db e la aggiungo alla lista
                                        documentoLottoEntityList.add(buildDocumentoLottoEntity(lotto, documento, repoFileId));
                                        
                                    } else {
                                        errori.add(new Errore().documentoId(documento.getDocumentoId()).code("HASH_ NON_VALIDO").detail("Valore del campo 'hash' non valido"));
                                    }
                                } else {
                                    errori.add(new Errore().documentoId(documento.getDocumentoId()).code("FORMATO_ FILE_ERRATO").detail("Formato del file PDF errato"));
                                }
                            }
                        } else {
                            errori.add(new Errore().documentoId(documento.getDocumentoId()).code("FILE_NON_ TROVATO").detail("File PDF non trovato"));
                        }
                    }
                } else {
                    errori.add(new Errore().code("PATH_NON_ TROVATO").detail("Path PDF non trovato"));
                }
            }  else {
                String error = String.format("L'integrazione con send è stata disabilitata per l'azienda con pdID %s", lotto.getPaId());
                log.error(error);
                throw new MasterjobsWorkerException(error);
            }
        } catch (Exception ex) {
            log.error("errore nel job", ex);
            
            deleteRepoFiles(repoFileIds); // elimina i file caricati sul repository
                        
            // se c'è un eccezione e non è stato inserito neanche un errore nella lista "errori", allora l'errore è un errore nostro e non dei dati che ci sono arrivati
            internalError = errori.isEmpty(); 
        } finally {
            // si disconnette dal server SFTP
            sftpManager.disconnect();
            
            // elimina i files temporanei creati
            deleteTmpFiles(tmpFiles);
        }
        
        if (!internalError) {
            try {
                entityManager.persist(documentoLottoEntityList);
                LottiSignerAndRegisterJobWorkerData lottiSignerAndRegisterJobWorkerData = new LottiSignerAndRegisterJobWorkerData(
                    lotto.getPaId(), lotto.getLottoId()
                );
                LottiSignerAndRegisterJobWorker lottiSignerAndRegisterJobWorker = masterjobsObjectsFactory
                    .getJobWorker(LottiSignerAndRegisterJobWorker.class,  lottiSignerAndRegisterJobWorkerData, false
                );
                
                RestCallToSendJobWorkerData  restCallToSendJobWorkerData = RestCallToSendJobWorkerData.buildElaboraLottoRicevuto(
                    getLottoBaseConEventualiErrori(lotto, errori, ZonedDateTime.now())
                );
                RestCallToSendJobWorker restCallToSendJobWorker = masterjobsObjectsFactory
                    .getJobWorker(RestCallToSendJobWorker.class,  restCallToSendJobWorkerData, false
                );
                
                masterjobsJobsQueuer.queueOnCommit(Arrays.asList(lottiSignerAndRegisterJobWorker, restCallToSendJobWorker), String.format("%s_%s", lotto.getPaId(), lotto.getLottoId()), "lotto", "send-integration", true, SetInterface.SetPriority.NORMAL, null);
                entityManager.flush();
            } catch (Exception ex) {
                String error = "Errore nella chiamata finale del job";
                throw new MasterjobsWorkerException(error);
            }
        } else {
            String error = String.format(
                "errore interno del job non attribuibile al fuitore, il job %s non è stato accodato, considero il job attuale in errore in modo che riproverà al prossimo giro",
                LottiSignerAndRegisterJobWorker.class.getSimpleName());
            log.error(error);
            throw new MasterjobsWorkerException(error);
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
    
    private LottoBaseConEventualiErrori getLottoBaseConEventualiErrori(Lotto lotto, List<Errore> errori, ZonedDateTime now) {
        LottoBaseConEventualiErrori lottoBaseConEventualiErrori = new LottoBaseConEventualiErrori()
            .paId(lotto.getPaId())
            .lottoId(lotto.getLottoId())
            .timestamp(now.toOffsetDateTime())
            .numeroDocumenti(lotto.getNumeroDocumenti())
            .numeroErrori(errori.size())
            .errori(errori);
         
        return lottoBaseConEventualiErrori;
    }
    
    /**
     * cancella tutti i file passati (da usare per cancellare i files temporaei creati)
     * @param files 
     */
    private void deleteTmpFiles(List<File> files){
        log.info("eliminazione dei files temporanei creati...");
        if (files != null && !files.isEmpty()) {
            for (File file : files) {
                file.delete();
            }
        }
        log.info("eliminazione dei files temporanei terminata");
    }
    
    /**
     * cancella tutti i files dal repository (spostandoli nel bucket trash) identificati dal fileId passato nella lista 
     * @param fileIds lista di fileId da eliminare
     */
    private void deleteRepoFiles(List<String> fileIds){
        log.info("eliminazione dei files caricati sul repository...");
        if (fileIds != null && !fileIds.isEmpty()) {
            MinIOWrapper minIOWrapper = repositoryConfiguration.getRepositoryManager().getMinIOWrapper();
            for (String fileId : fileIds) {
                try {
                    minIOWrapper.deleteByFileId(fileId);
                } catch (Exception ex) {
                    String error = String.format("errore nell'eliminazione del file con fileId %s del repository, lo salto e vado avanti", fileId);
                    log.error(error);
                }
            }
        }
        log.info("files caricati sul repository eliminati");
    }
}
