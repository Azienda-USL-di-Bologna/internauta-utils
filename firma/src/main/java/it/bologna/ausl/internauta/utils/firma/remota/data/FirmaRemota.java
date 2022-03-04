package it.bologna.ausl.internauta.utils.firma.remota.data;

import it.bologna.ausl.internauta.utils.firma.remota.configuration.ConfigParams;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.FirmaRemotaException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.InvalidCredentialException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.RemoteFileNotFoundException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.RemoteServiceException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.WrongTokenException;
import it.bologna.ausl.internauta.utils.firma.remota.utils.FirmaRemotaDownloaderUtils;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import java.io.InputStream;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public abstract class FirmaRemota {
    private static Logger logger = LoggerFactory.getLogger(FirmaRemota.class);
    
    protected final ConfigParams configParams;
    protected final FirmaRemotaDownloaderUtils firmaRemotaDownloaderUtils;
    protected final String codiceAzienda;
    
    protected FirmaRemota(String codiceAzienda, ConfigParams configParams, FirmaRemotaDownloaderUtils firmaRemotaDownloaderUtils) {
        this.configParams = configParams;
        this.firmaRemotaDownloaderUtils = firmaRemotaDownloaderUtils;
        this.codiceAzienda = codiceAzienda;
    }
    
    /**
     * Questo metodo firma uno o più files all'interno di "files" in firmaRemotaInformation e ritorna lo stesso oggetto con il risultato per ogni file
     * @param firmaRemotaInformation contiene tutto il necessatio per firmare (i files da firmare e tutte le inforazioni che servono per firmarli)
     * @return lo stesso oggetto in input, ma che per ogni file all'interno del campo files ha scritto anche il risultato
     * @throws FirmaRemotaException
     * @throws RemoteFileNotFoundException
     * @throws WrongTokenException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    public abstract FirmaRemotaInformation firma(FirmaRemotaInformation firmaRemotaInformation) throws FirmaRemotaException, RemoteFileNotFoundException, WrongTokenException, InvalidCredentialException, RemoteServiceException;
    
    /**
     * Carica il file passato sul repository
     * A seconda di file.getOutputType() decide se caricarlo direttente su MinIO oppure facendo una chiamata all'uploader.Una volta caricato viene richiamato file.setUuidFirmato() o file.setUrlFirmato() a seconda di file.getOutputType()
     * 
     * @param file le informazioni del file
     * @param signedFileInputStream lo stream del file firmato
     * @throws MinIOWrapperException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.FirmaRemotaException 
     */
    public void upload(FirmaRemotaFile file, InputStream signedFileInputStream) throws MinIOWrapperException, FirmaRemotaException {
        MinIOWrapper minIOWrapper = this.configParams.getMinIOWrapper();
        
        logger.info(String.format("OutputType %s ...", file.getOutputType().toString()));
        
        // OutputType.UUID sarà rimosso dopo che non ci saranno più le APP legacy
        // con questa modalità di output è la firma stessa a caricare il file firmato sul repository (minIO).
        if (file.getOutputType() == FirmaRemotaFile.OutputType.UUID) {
            logger.info(String.format("putting file %s on temp repository...", file.getFileId()));
            MinIOWrapperFileInfo uploadedFileInfo = minIOWrapper.put(signedFileInputStream, this.codiceAzienda, "/temp", "signed_" + UUID.randomUUID(), null, false, UUID.randomUUID().toString(), this.codiceAzienda + "t");
            String signedUuid = uploadedFileInfo.getMongoUuid();
            logger.info(String.format("file %s written on temp repository", file.getFileId()));

            // setto l'uuid del file firmato caricato sul repository
            file.setUuidFirmato(signedUuid);
        } else { // in questa caso invio il file firmato al repository tramite la funzione "uploader" del modulo Downloader
            String signedFileName;
            String signedMimeType;
            if (file.getFormatoFirma() == FirmaRemotaFile.FormatiFirma.PDF) {
                signedFileName = file.getFileId() + ".pdf";
                signedMimeType = "application/pdf";
            } else {
                signedFileName = file.getFileId() + ".p7m";
                signedMimeType = "application/pkcs7-mime";
            }
            logger.info(String.format("uploading file %s to Uploader...", file.getFileId()));
            
            // invio il file all'uploader e ottengo l'url per il suo scaricamento
            String res = firmaRemotaDownloaderUtils.uploadToUploader(signedFileInputStream, signedFileName, signedMimeType, this.codiceAzienda, false);
            
            // una volta ottenuto l'url, lo setto
            file.setUrlFirmato(res);
        }
        logger.info(String.format("file %s completed", file.getFileId()));
    }
    
    /**
     * Questo metodo di occupa della pre-autenticazione, quando è necessario eseguire delle operazioni preliminari per ottenere il necessario per firmare.
     * Per esempio, il provider ARUBA ha una funzione che, previo passaggio di alcune informazioni invia un sms ho manda una chiamata al cellulare del firmatario con il codice OTP per firmare
     * @param userInformation le informazioni necessarie (che riaguardano l'utente) per eseguire le procedure di pre-autenticazione
     * @throws FirmaRemotaException
     * @throws WrongTokenException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    public abstract void preAuthentication(UserInformation userInformation) throws FirmaRemotaException, WrongTokenException, InvalidCredentialException, RemoteServiceException;

    /**
     * Questo metodo indica se sono presenti le credenziali dell'utente all'interno del sstema di memorizzazioni delle credenziali.
     * @param userInformation
     * @return true se sono presenti, false altrimenti
     * @throws FirmaRemotaException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    public abstract boolean existingCredential(UserInformation userInformation) throws FirmaRemotaException, InvalidCredentialException, RemoteServiceException;

    /**
     * Permette di memorizzare le credenziali di un utente per permetterli di non doverle inserire tutte le volte
     * @param userInformation
     * @return true se l'operazioni va a buon fine, false o eccezione altrimenti.
     * @throws FirmaRemotaException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    public abstract boolean setCredential(UserInformation userInformation) throws FirmaRemotaException, InvalidCredentialException, RemoteServiceException;

    /**
     * Elimina le eventuali credenziali salvate per l'utente passato
     * @param userInformation
     * @return true se le credenziali esistevano e sono state rimosse, false altrimenti
     * @throws FirmaRemotaException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    public abstract boolean removeCredential(UserInformation userInformation) throws FirmaRemotaException, InvalidCredentialException, RemoteServiceException;
}
