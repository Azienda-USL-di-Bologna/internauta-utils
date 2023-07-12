package it.bologna.ausl.internauta.utils.pdftoolkit.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.authorizationutils.DownloaderTokenCreator;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import it.bologna.ausl.internauta.utils.pdftoolkit.configuration.PdfToolkitConfiguration;
import it.bologna.ausl.internauta.utils.pdftoolkit.exceptions.PdfToolkitConfigurationException;
import it.bologna.ausl.internauta.utils.pdftoolkit.exceptions.PdfToolkitHttpException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import okhttp3.Call;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author Giuseppe Russo <g.russo@dilaxia.com>
 */
@Service
public class PdfToolkitDownloaderUtils {
     private static Logger logger = LoggerFactory.getLogger(PdfToolkitDownloaderUtils.class);
     
    @Value("${pdf-toolkit.mode:test}")
    private String pdfToolkitMode;

    // certificato con la chiave pubblica corrispondente alla chiave privata per la firma del token (prod)
    @Value("${pdf-toolkit.downloader.public-cert-babel-prod.location}")
    private String downloaderPublicCertBabelProdLocation;

    // certificato con la chiave pubblica corrispondente alla chiave privata per la firma del token (test)
    @Value("${pdf-toolkit.downloader.public-cert-babel-test.location}")
    private String downloaderPublicCertBabelTestLocation;
    
    // nell'inizializzazione viene settato con il certificato di test o di prod a seconda del mode
    private File downloaderPublicCertBabel;
    
    // chiave pubblica per la cifratura del token (prod)
    @Value("${pdf-toolkit.downloader.encrypt-token-public-key-prod.location}")
    private String downloaderEncryptionPublicKeyProdLocation;
    
    // chiave pubblica per la cifratura del token (test)
    @Value("${pdf-toolkit.downloader.encrypt-token-public-key-test.location}")
    private String downloaderEncryptionPublicKeyTestLocation;
    
    // nell'inizializzazione viene settato con la chiave di test o di prod a seconda del mode
    private File downloaderEncryptionPublicKey;
    
    
    // chiave privata per la firma del token
    @Value("${pdf-toolkit.downloader.sign-token-private-key-file.location}")
    private String signTokenPrivateKeyFileLocation;
    
    // alias della chiave all'interno del p12
    @Value("${pdf-toolkit.downloader.sign-token-private-key-file.key-alias}")
    private String signTokenPrivateKeyAlias;
    
    // password del p12 e della chiave (sono uguali)
    @Value("${pdf-toolkit.downloader.sign-token-private-key-file.password}")
    private String signTokenPrivateKeyPassword;
    
    // durata del token
    @Value("${pdf-toolkit.downloader.token-expire-seconds:60}")
    private Integer tokenExpireSeconds;
    
    @Autowired
    private PdfToolkitConfiguration pdfToolkitHttpClientConfiguration;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public static enum DonwloaderTarget {
        MinIO, Default
    }
    
    /**
     * in fase di avvio dell'applicazione setta la chiave e il certificato corretti a seconda che siamo in test o in prod (basandosi sul parametro firmaMode)
     * @throws PdfToolkitConfigurationException 
     */
    @PostConstruct
    public void initialize() throws PdfToolkitConfigurationException {
         switch (pdfToolkitMode.toLowerCase()) {
            case "test": // se sono in modalità di test prendo il certificato con la chiave pubblica di test e la chiave per cifrare il token di test
                this.downloaderPublicCertBabel = new File(this.downloaderPublicCertBabelTestLocation);
                this.downloaderEncryptionPublicKey = new File(this.downloaderEncryptionPublicKeyTestLocation);
                break;
            case "prod": // se sono in modalità di test prendo il certificato con la chiave pubblica di prod e la chiave per cifrare il token di prod
                this.downloaderPublicCertBabel = new File(this.downloaderPublicCertBabelProdLocation);
                this.downloaderEncryptionPublicKey = new File(this.downloaderEncryptionPublicKeyProdLocation);
                break;
            default:
                String errorMessage = String.format("PdfToolkit mode deve essere \"%s\" o \"%s\". Valore trovato \"%s\"", "test", "prod", pdfToolkitMode);
                logger.error(errorMessage);
                throw new PdfToolkitConfigurationException(errorMessage);
        }
    }
    
    public String uploadToUploader(InputStream file, String filename, String mimeType, Boolean forceDownload, String downloadUrl, String uploadUrl) throws PdfToolkitHttpException {
        return uploadToUploader(file, filename, mimeType, forceDownload, downloadUrl, uploadUrl, null);
    }
    
    /**
     * carica il file passato sul nostro repository servendosi della funzione upload del Downloader e ne torna l'url per poterlo scaricare attraverso il downloader
     * @param file il file da inviare
     * @param filename il nome che il file dovrà avere sul repository
     * @param mimeType il mimeType del file
     * @param forceDownload se "true" l'url tornato forzerà il download
     * @param downloadUrl l'url da usare per generare queello di download
     * @param uploadUrl l'url da usare per generare queello di upload
     * @param downloadTokenExpireSeconds il tempo in secondi per la scadenza del token per scaricare il download
     * @return l'url per poter scaricare il file attraverso la funzione download del Downloader
     * @throws PdfToolkitHttpException 
     */
    public String uploadToUploader(InputStream file, String filename, String mimeType, Boolean forceDownload, String downloadUrl, String uploadUrl, Integer downloadTokenExpireSeconds) throws PdfToolkitHttpException {
        String res;
        String token;
        
        // per prima cosa creo il token da inserire per la chiamata alla funzione upload del Downloader
        try {
            token = buildToken(getUploaderContext(filename, null));
        } catch (Exception ex) {
            String errorMessage = "errore nella creazione del token per l'upload";
            logger.error(errorMessage, ex);
            throw new PdfToolkitHttpException(errorMessage, ex);
        }

        File tmpFileToUpload = null;
        try {
            // creo un file temporaneo dallo stream passato. Lo cancello poi alla fine (nel finally)
            tmpFileToUpload = File.createTempFile(getClass().getSimpleName() + "to_uploader_", ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tmpFileToUpload)) {
                IOUtils.copy(file, fos);
            } catch (Exception ex) {
                String errorMessage = "errore nella creazione del file temporaneo per l'upload";
                logger.error(errorMessage, ex);
                throw new PdfToolkitHttpException(errorMessage, ex);
            }
            
            // creo la richiesta multipart mettendo il token nei query-params
            RequestBody dataBody = RequestBody.create(okhttp3.MultipartBody.FORM, tmpFileToUpload);
            MultipartBody multipartBody = new MultipartBody.Builder()
                .addPart(MultipartBody.Part.createFormData("file", filename, dataBody))
                .build();
            Request uploadRequest = new Request.Builder()
                .url(String.format("%s?token=%s", uploadUrl, token))
                .post(multipartBody)
                .build();

            // eseguo la chiamata all'upload
            OkHttpClient httpClient;
            if (pdfToolkitHttpClientConfiguration.getHttpClientManager().getOkHttpClient() != null) {
                httpClient = pdfToolkitHttpClientConfiguration.getHttpClientManager().getOkHttpClient();
            } else {
                 httpClient = getHttpClient();
            }
            Call call = httpClient.newCall(uploadRequest);
            Response response = call.execute();

            ResponseBody content = response.body();
            if (!response.isSuccessful()) {
                if (content != null) {
                    throw new PdfToolkitHttpException(String.format("errore nella chiamata all'URL: %s RESPONSE: %s", uploadUrl, content.string()));
                } else {
                    throw new PdfToolkitHttpException(String.format("errore nella chiamata all'URL: %s RESPONSE: null", uploadUrl));
                }
            } else { // tutto ok
                if (content != null) {
                    // se tutto ok creo l'url per il download e lo torno
                    Map<String, Object> downloadParams = objectMapper.readValue(content.byteStream(), new TypeReference<Map<String, Object>>(){});
                    res = buildDownloadUrl(filename, mimeType, downloadParams, forceDownload, downloadUrl, downloadTokenExpireSeconds);
                }
                else {
                    throw new PdfToolkitHttpException(String.format("l'upload non ha tornato risultato", uploadUrl));
                }
            }
            return res;
        }
        catch (Exception ex) {
            String errorMessage = "errore nella creazione del token per l'upload";
            logger.error(errorMessage, ex);
            throw new PdfToolkitHttpException(errorMessage, ex);
        } finally { // elimina sempre il file temporaneo creato e chiude lo stream del file passato in input
            IOUtils.closeQuietly(file);
            if (tmpFileToUpload != null && tmpFileToUpload.exists()) {
                tmpFileToUpload.delete();
            }
        }
    }
    
    private String buildToken(Map<String,Object> context) throws IOException, AuthorizationUtilsException, NoSuchAlgorithmException, InvalidKeySpecException {
        return buildToken(context, this.tokenExpireSeconds);
    }
    
    /**
     * Costruisce il token JWE per la chiamata al Downloader, servendosi della classe DownloaderTokenCreator fornita dal modulo authorization-utils
     * @param context il context da inserire nel token
     * @return
     * @throws IOException
     * @throws AuthorizationUtilsException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException 
     */
    private String buildToken(Map<String,Object> context, Integer tokenExpireSeconds) throws IOException, AuthorizationUtilsException, NoSuchAlgorithmException, InvalidKeySpecException {
        DownloaderTokenCreator downloaderTokenCreator = new DownloaderTokenCreator();
        PrivateKey signTokenPrivateKey = downloaderTokenCreator.getSignTokenPrivateKey(signTokenPrivateKeyFileLocation, signTokenPrivateKeyAlias, signTokenPrivateKeyPassword);
        RSAPublicKey encryptionPublicKey = downloaderTokenCreator.getEncryptionPublicKey(this.downloaderEncryptionPublicKey);
        return downloaderTokenCreator.getToken(context, this.downloaderPublicCertBabel, signTokenPrivateKey, encryptionPublicKey, tokenExpireSeconds, "reporter-internauta");
    }
    
    /**
     * Crea l'url per scaricare il file usando i params passati.
     * Da usare con i params tornati dalla chiamata all'upload
     * @param codiceAzienda
     * @param fileName
     * @param mimeType
     * @param downloadParams il risultato dell'uploader
     * @param forceDownload
     * @return
     * @throws PdfToolkitConfigurationException
     * @throws IOException
     * @throws AuthorizationUtilsException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException 
     */
    private String buildDownloadUrl(String fileName, String mimeType, Map<String, Object> downloadParams, Boolean forceDownload, String downloadUrl, Integer downloadTokenExpireSeconds) 
            throws PdfToolkitConfigurationException, IOException, AuthorizationUtilsException, NoSuchAlgorithmException, InvalidKeySpecException {
        Map<String, Object> downloaderContext = getDownloaderContext(downloadParams, fileName, mimeType);
        String token;
        if (downloadTokenExpireSeconds != null){
            token = buildToken(downloaderContext, downloadTokenExpireSeconds);
        }else {
            token = buildToken(downloaderContext);
        }
        return String.format("%s?token=%s&forceDownload=%s", downloadUrl, token, forceDownload);
    }
    
    /**
     * Costruisce l'HttpClient per fare le chiamate http con timeout impostato a 15 minuti.
     * @return 
     */
    private OkHttpClient getHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        OkHttpClient client = builder.connectTimeout(15, TimeUnit.MINUTES).readTimeout(15, TimeUnit.MINUTES).writeTimeout(15, TimeUnit.MINUTES).build();
        return client;
    }
    
    /**
     * Costruisce il context da inserire nel token per la chiamata al download
     * @param fileName il nome che si vuole far avere al file sul repository
     * @param metadata eventuali metadati da attribuire al file sul repository. Si può passare null
     * @return 
     */
    private Map<String, Object> getUploaderContext(String fileName, Map<String, Object> metadata) {        
        Map<String, Object> context = new HashMap();
        context.put("target", DonwloaderTarget.Default);
        context.put("fileName", fileName);
        if (metadata != null) {
            Map<String, Object> minIOParams = new HashMap();
            minIOParams.put("metadata", metadata);
            context.put("params", minIOParams);
        }
        return context;
    }  
    
    /**
     * Construisce il context da inserire nel token per il download
     * @param params i parametri per il plugin di scaricamento (sono quelli tornato dalla chiamata all'upload)
     * @param fileName il nome che avrà il file scaricato
     * @param mimeType il mimeType del file da scaricare
     * @return 
     */
    private Map<String, Object> getDownloaderContext(Map<String, Object> params, String fileName, String mimeType) {        
        Map<String, Object> context = new HashMap();
        context.put("params", params);
        context.put("fileName", fileName);
        context.put("mimeType", mimeType);
        context.put("source", DonwloaderTarget.Default);
        return context;
    }  
    
}
