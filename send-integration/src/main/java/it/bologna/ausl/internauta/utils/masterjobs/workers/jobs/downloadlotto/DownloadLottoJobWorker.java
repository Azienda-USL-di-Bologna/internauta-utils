package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.downloadlotto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.send_integration.api.FruitoreApi;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.elaboralotto.lottosignerandregister.LottiSignerAndRegisterJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.elaboralotto.lottosignerandregister.LottiSignerAndRegisterJobWorkerData;
import it.bologna.ausl.internauta.utils.send_integration.model.Documento;
import it.bologna.ausl.internauta.utils.send_integration.model.Errore;
import it.bologna.ausl.internauta.utils.send_integration.model.Lotto;
import it.bologna.ausl.internauta.utils.send_integration.model.LottoBase;
import it.bologna.ausl.internauta.utils.send_integration.model.LottoBaseConEventualiErrori;
import it.bologna.ausl.internauta.utils.sendintegration.SendIntegrationConstants;
import it.bologna.ausl.internauta.utils.sendintegration.SendIntegrationSFTPManager;
import it.bologna.ausl.internauta.utils.sendintegration.SendIntegrationUtils;
import it.bologna.ausl.internauta.utils.sendintegration.authorization.SendIntegrationAuthorizationUtils;
import it.bologna.ausl.internauta.utils.sendintegration.authorization.exceptions.NotValidJwtException;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.sendintegration.exceptions.SendIntegrationException;
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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

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
    private FruitoreApi fruitoreApi;
    
    @Autowired
    private SendIntegrationAuthorizationUtils authorizationUtils;
    
    @Autowired
    private SendIntegrationSFTPManager sftpManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
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
                LottiSignerAndRegisterJobWorkerData lottiSignerAndRegisterJobWorkerData = new LottiSignerAndRegisterJobWorkerData(lotto.getPaId(), lotto.getLottoId());
                LottiSignerAndRegisterJobWorker jobWorker = masterjobsObjectsFactory.getJobWorker(LottiSignerAndRegisterJobWorker.class,  lottiSignerAndRegisterJobWorkerData, false);
                masterjobsJobsQueuer.queueOnCommit(Arrays.asList(jobWorker), String.format("%s_%s", lotto.getPaId(), lotto.getLottoId()), "lotto", "send-integration", true, SetInterface.SetPriority.NORMAL, null);
                entityManager.flush();
                
                ZonedDateTime now = ZonedDateTime.now();
                sendElaboraLottoRicevutoRequest(basePath, lotto, errori, now);
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
    
    @Deprecated // usa sendElaboraLottoRicevutoRequest
    private LottoBase sendElaboraLottoRicevutoRequestOkHttp(String url, Lotto lotto, List<Errore> errori, ZonedDateTime now) throws JsonProcessingException, IOException, SendIntegrationException {
        LottoBaseConEventualiErrori lottoBaseConEventualiErrori = new LottoBaseConEventualiErrori()
            .paId(lotto.getPaId())
            .lottoId(lotto.getLottoId())
            .timestamp(now.toOffsetDateTime())
            .numeroDocumenti(lotto.getNumeroDocumenti())
            .numeroErrori(errori.size())
            .errori(errori);
        OkHttpClient okHttpClient = httpClientConfiguration.getHttpClientManager().getOkHttpClient();
        
        RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(lottoBaseConEventualiErrori),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder().url(url).post(body).build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String error = String.format("Errore nella chiamata POST all'url %s: ha tornato %s" + url, response.code());
                log.error(error);
                if (response.body() != null) {
                    try {
                        log.error("response body:");
                        log.error(response.body().string());
                    } catch (Exception ex) {
                    }
                }
                throw new SendIntegrationException(error);
            }
            LottoBase resp = objectMapper.readValue(response.body().string(), LottoBase.class);
            return resp;
        }
        
    }
    
    private LottoBase sendElaboraLottoRicevutoRequest(String basePath, Lotto lotto, List<Errore> errori, ZonedDateTime now) throws NotValidJwtException, SendIntegrationException {
        LottoBaseConEventualiErrori lottoBaseConEventualiErrori = new LottoBaseConEventualiErrori()
            .paId(lotto.getPaId())
            .lottoId(lotto.getLottoId())
            .timestamp(now.toOffsetDateTime())
            .numeroDocumenti(lotto.getNumeroDocumenti())
            .numeroErrori(errori.size())
            .errori(errori);
        String token = authorizationUtils.generateTokenForFruitore(now);
        fruitoreApi.getApiClient().setBasePath(basePath).setBearerToken(token);
        ResponseEntity<LottoBase> resp = fruitoreApi.elaboraLottoRicevutoWithHttpInfo(lottoBaseConEventualiErrori);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            String error = String.format("Errore nella chiamata POST al %s: ha tornato %s", "elaboraLottoRicevuto", resp.getStatusCode().value());
            log.error(error);
            log.error(resp.toString());
            throw new SendIntegrationException(error);
        }
        if (resp.hasBody()) {
            return resp.getBody();
        } else {
            String error = String.format("La risposta della chiamata POST al %s: non ha body", "elaboraLottoRicevuto");
            log.error(error);
            throw new SendIntegrationException(error);
        }
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
