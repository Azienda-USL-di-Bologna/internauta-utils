package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.lottosignerandregister;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.restcalltosend.RestCallToSendJobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.restcalltosend.RestCallToSendJobWorkerData;
import it.bologna.ausl.internauta.utils.sendintegration.SendIntegrationSFTPManager;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationScriptaWrapperConfiguration;
import it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationScriptaWrapperManager;
import it.bologna.ausl.internauta.utils.sendintegration.exceptions.RuntimeExceptionContainer;
import it.bologna.ausl.internauta.utils.sendintegration.exceptions.SendIntegrationException;
import it.bologna.ausl.internauta.utils.sendintegration.model.DocumentoElaborato;
import it.bologna.ausl.internauta.utils.sendintegration.model.InfoRegistrazioneLotto;
import it.bologna.ausl.internauta.utils.sendintegration.model.LottoElaborato;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.masterjobs.SetInterface;
import it.bologna.ausl.model.entities.sendintegration.DocumentoLottoEntity;
import it.bologna.ausl.model.entities.sendintegration.QDocumentoLottoEntity;
import it.bologna.ausl.model.entities.sendintegration.SendIntegrationConfiguration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;

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
    
    @Autowired
    private SendIntegrationSFTPManager sftpManager;
    
    @Autowired
    private SendIntegrationRepositoryConfiguration repositoryConfiguration;
    
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in do doWork() di {}", getName());
       
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        try {
            transactionTemplate.executeWithoutResult(a -> {
                LottiSignerAndRegisterJobWorkerData workerData = getWorkerData();
                // leggo la configurazione dell'azienda a cui il lotto è associato, l'azienda è identificata dal paId del lotto
                SendIntegrationConfiguration lepidaAziendaConfiguration = 
                        entityManager.find(SendIntegrationConfiguration.class, SendIntegrationConfiguration.Ids.lepidaAziendaConfiguration);
                    Map<String, Object>  lepidaAziendaConfigurationMap = lepidaAziendaConfiguration.getValue();
                    Map<String, Object> paConfiguration = (Map<String, Object>) lepidaAziendaConfigurationMap.get(workerData.getPaId());
                // controllo che l'integrazione con send sia attiva per l'azienda indicata
                boolean aziendaActive = (boolean) paConfiguration.get("active");
                if (aziendaActive) {
                } else {
                    String error = String.format("L'integrazione con send è stata disabilitata per l'azienda con pdID %s", workerData.getPaId());
                    log.error(error);
                    throw new RuntimeExceptionContainer(new MasterjobsWorkerException(error));
                }
                SendIntegrationScriptaWrapperManager scriptaWrapperManger = sendIntegrationScriptaWrapperConfiguration.getScriptaWrapperManger();
                try {
                    InfoRegistrazioneLotto infoRegistrazioneLotto = scriptaWrapperManger.generaDocumentoPUProtocollato(
                        workerData.getPaId(), workerData.getLottoId(), workerData.getFirmatario(), workerData.getOutputBasePath(), paConfiguration);
                    LottoElaborato lottoElaborato = getLottoElaborato(
                        workerData.getLottoId(), workerData.getPaId(), 
                        workerData.getFirmatario(), workerData.getNumeroDocumenti(),
                        workerData.getTimestamp(), workerData.getOutputBasePath(), infoRegistrazioneLotto
                    );
                    RestCallToSendJobWorkerData restCallToSendJobWorkerData = RestCallToSendJobWorkerData.buildLottoElaborato(
                        lottoElaborato
                    );
                    RestCallToSendJobWorker restCallToSendJobWorker = masterjobsObjectsFactory
                        .getJobWorker(RestCallToSendJobWorker.class,  restCallToSendJobWorkerData, false
                    );
                    
                    if (true) {
                        masterjobsJobsQueuer.queueOnCommit(
                            Arrays.asList(restCallToSendJobWorker),
                            String.format("%s_%s", lottoElaborato.getPaId(), lottoElaborato.getLottoId()),
                            "lotto", "send-integration", true, SetInterface.SetPriority.NORMAL, null);
                    } else {
                        restCallToSendJobWorker.doWork(false); // Eseguo sincrono per le prove 
                    }
                } catch (Exception ex) {
                    String error = String.format("errore nella generazione del protocollo per il lotto %s", workerData.getLottoId());
                    log.error(error, ex);
                    throw new RuntimeExceptionContainer(new MasterjobsWorkerException(error, ex));
                }
            });
        } catch (Throwable ex) {
            if (ex instanceof RuntimeExceptionContainer re)
                throw new MasterjobsWorkerException(re.getException());
            else 
                throw new MasterjobsWorkerException(ex);
        }
        log.info("job finito!");
        return null;
    }
    
    private LottoElaborato getLottoElaborato(String lottoId, String paId, String firmatario, Integer numeroDocumenti, String timestamp, String outputBasePath, InfoRegistrazioneLotto infoRegistrazioneLotto) throws SftpException, JSchException, MasterjobsWorkerException {
        QDocumentoLottoEntity qDocumentoLottoEntity = QDocumentoLottoEntity.documentoLottoEntity;
        OffsetDateTime timestampOdt = OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        
        LottoElaborato lottoElaborato = 
            new LottoElaborato()
            .paId(paId)
            .lottoId(lottoId)
            .firmatario(firmatario)
            .timestamp(timestampOdt)
            .outputBasePath(outputBasePath)
            .numeroDocumenti(numeroDocumenti)
            .numeroErrori(infoRegistrazioneLotto.getNumeroErrori())
            .dataProtocollo(infoRegistrazioneLotto.getDataRegistrazione().toLocalDate())
            .numeroElaborati(infoRegistrazioneLotto.getNumeroDocumentiRegistrati())
            .numeroProtocollo(infoRegistrazioneLotto.getNumeroRegistrazione());
            
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        Stream<DocumentoLottoEntity> documentiLottoStream = queryFactory
                .select(qDocumentoLottoEntity)
                .from(qDocumentoLottoEntity)
                .where(qDocumentoLottoEntity.lottoId.eq(lottoId).and(qDocumentoLottoEntity.paId.eq(paId)))
                .stream();
        
        sftpManager.connect();
        try {
            if (!sftpManager.existsPath(outputBasePath)) {
                sftpManager.createDirectory(outputBasePath);
            }
            MinIOWrapper minIOWrapper = repositoryConfiguration.getRepositoryManager().getMinIOWrapper();
//        AtomicInteger index = new AtomicInteger(0);

            documentiLottoStream.forEach(documentoLotto -> {
                DocumentoElaborato documentoElaborato = 
                    new DocumentoElaborato()
                    .documentoId(documentoLotto.getDocumentoId())
                    .outputFileName(documentoLotto.getOutputFileName())
                    .outputFileHash(documentoLotto.getOutputFileSha256());

                String filePath = String.format("%s/%s", outputBasePath, documentoElaborato.getOutputFileName());
                try {
                    sftpManager.uploadFile(minIOWrapper.getByFileId(documentoLotto.getOutputFileRepoId()), filePath);
                } catch (Exception ex) {
                    String error = String.format("errore nell'upload del file della riga con id %s sul server sftp", documentoLotto.getId());
                    log.error(error, ex);
                    throw new RuntimeExceptionContainer(new SendIntegrationException(error, ex));
                }

                lottoElaborato.addDocumentiElaboratiItem(documentoElaborato);
            });
        } catch (RuntimeExceptionContainer ex) {
            String error = "errore nel caricamento dei file firmatori sul sftp";
            log.error(error, ex);
            throw new MasterjobsWorkerException(error, ex.getException());
        } finally {
            // si disconnette dal server SFTP
            sftpManager.disconnect();
        }
        return lottoElaborato;
    }
}
