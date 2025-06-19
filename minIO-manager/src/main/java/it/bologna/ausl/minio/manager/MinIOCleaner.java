package it.bologna.ausl.minio.manager;

import it.bologna.ausl.minio.manager.exceptions.MinioCleanerException;
import java.time.ZonedDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class MinIOCleaner {
    private static final Logger log = LoggerFactory.getLogger(MinIOCleaner.class);

    private enum CleanType {
        BUCKET, DOWNLOAD, TRASH
    }
    
    private final MinIOWrapper minIOWrapper;

    public MinIOCleaner(MinIOWrapper minIOWrapper) {
        this.minIOWrapper = minIOWrapper;
    }
    
    public void cleanMinIOTrash(Integer intervalHour, String codiceAzienda) throws MinioCleanerException {
        removeFiles(CleanType.TRASH, null, codiceAzienda, intervalHour);
    }
    
    public void cleanMinIODownload(Integer intervalHour, String codiceAzienda) throws MinioCleanerException {
        if (!codiceAzienda.endsWith("t")) {
            throw new MinioCleanerException("è stato passato un codice azienda che non termina con t, mi fermo");
        }
        removeFiles(CleanType.DOWNLOAD, null, codiceAzienda, intervalHour);
    }
    
    public void cleanMinIOBucket(Integer intervalHour, String bucket) throws MinioCleanerException {
        removeFiles(CleanType.BUCKET, bucket, null, intervalHour);
    }
    
    private void removeFiles(CleanType cleanType, String bucket, String codiceAzienda, Integer intervalHour) throws MinioCleanerException {
        ZonedDateTime nowMinusInterval = ZonedDateTime.now().minusHours(intervalHour);
        log.info(String.format("inizio pulizia %s di minIO", cleanType));
        List<MinIOWrapperFileInfo> filesToRemove;
        try {
            log.info("eseguo la query...");

            do {
                switch (cleanType) {
                    case BUCKET -> filesToRemove = minIOWrapper.getFilesLessThanBucket(bucket, nowMinusInterval, true, 1000);
                    case DOWNLOAD -> filesToRemove = minIOWrapper.getFilesLessThan(codiceAzienda, nowMinusInterval, true, 1000);
                    case TRASH -> filesToRemove = minIOWrapper.getDeleted(codiceAzienda, nowMinusInterval, 1000);
                    default -> throw new MinioCleanerException(String.format("CleanType %s non valido", cleanType));
                }
                if (filesToRemove != null && !filesToRemove.isEmpty()) {
                    log.info("ho trovato dei files, inizio a cancellare...");
                    for (MinIOWrapperFileInfo file : filesToRemove) {
                        log.info(String.format("ereasing %s...", file.toString()));
                        minIOWrapper.removeByFileId(file.getFileId(), false);
                    }
                }
            } while (filesToRemove != null && !filesToRemove.isEmpty());
            log.info("non ho trovato più nulla da cancellare, esco");
        } catch (Exception ex) {
            String errorMessage = (String.format("errore nella pulizia %s", cleanType));
            log.error(errorMessage, ex);
            throw new MinioCleanerException(errorMessage, ex);
        }
    }
}
