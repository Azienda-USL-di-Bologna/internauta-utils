package it.bologna.ausl.internauta.utils.firma.remota.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.authorizationutils.DownloaderTokenCreator;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import it.bologna.ausl.internauta.utils.firma.remota.configuration.ConfigParams;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaHttpException;
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
import org.springframework.core.io.Resource;

import org.springframework.stereotype.Component;

/**
 * Fornisce gli strumenti per l'upload dei file tramite la funzione upload del Downloader
 * @author gdm
 */
@Component
public class FirmaRemotaDownloaderUtils {
    private static Logger logger = LoggerFactory.getLogger(FirmaRemotaDownloaderUtils.class);
    
    @Value("${firma.mode:test}")
    private String firmaMode;
    
    // certificato con la chiave pubblica corrispondente alla chiave privata per la firma del token (prod)
    @Value("${firma.downloader.public-cert-babel-prod}")
    private Resource downloaderPublicCertBabelProd;

    // certificato con la chiave pubblica corrispondente alla chiave privata per la firma del token (test)
    @Value("${firma.downloader.public-cert-babel-test}")
    private Resource downloaderPublicCertBabelTest;
    
    // nell'inizializzazione viene settato con il certificato di test o di prod a seconda del mode
    private Resource downloaderPublicCertBabel;
    
    // chiave pubblica per la cifratura del token (prod)
    @Value("${firma.downloader.encrypt-token-public-key-prod}")
    private Resource downloaderEncryptionPublicKeyProd;
    
    // chiave pubblica per la cifratura del token (test)
    @Value("${firma.downloader.encrypt-token-public-key-test}")
    private Resource downloaderEncryptionPublicKeyTest;
    
    // nell'inizializzazione viene settato con la chiave di test o di prod a seconda del mode
    private Resource downloaderEncryptionPublicKey;
    
    
    // chiave privata per la firma del token
    @Value("${firma.downloader.sign-token-private-key-file.location}")
    private String signTokenPrivateKeyFileLocation;
    
    // alias della chiave all'interno del p12
    @Value("${firma.downloader.sign-token-private-key-file.key-alias}")
    private String signTokenPrivateKeyAlias;
    
    // password del p12 e della chiave (sono uguali)
    @Value("${firma.downloader.sign-token-private-key-file.password}")
    private String signTokenPrivateKeyPassword;
    
    // durata del token
    @Value("${firma.downloader.token-expire-seconds:60}")
    private Integer tokenExpireSeconds;
    
    @Autowired
    private ConfigParams configParams;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public static enum DonwloaderTarget {
        MinIO, Default
    }
    
    /**
     * in fase di avvio dell'applicazione setta la chiave e il certificato corretti a seconda che siamo in test o in prod (basandosi sul parametro firmaMode)
     * @throws FirmaRemotaConfigurationException 
     */
    @PostConstruct
    public void initialize() throws FirmaRemotaConfigurationException {
         switch (firmaMode.toLowerCase()) {
            case "test": // se sono in modalità di test prendo il certificato con la chiave pubblica di test e la chiave per cifrare il token di test
                this.downloaderPublicCertBabel = this.downloaderPublicCertBabelTest;
                this.downloaderEncryptionPublicKey = this.downloaderEncryptionPublicKeyTest;
                break;
            case "prod": // se sono in modalità di test prendo il certificato con la chiave pubblica di prod e la chiave per cifrare il token di prod
                this.downloaderPublicCertBabel = this.downloaderPublicCertBabelProd;
                this.downloaderEncryptionPublicKey = this.downloaderEncryptionPublicKeyProd;
                break;
            default:
                String errorMessage = String.format("firma mode deve essere \"%s\" o \"%s\". Valore trovato \"%s\"", "test", "prod", firmaMode);
                logger.error(errorMessage);
                throw new FirmaRemotaConfigurationException(errorMessage);
        }
    }
    
    /**
     * carica il file passato sul nostro repository servendosi della funzione upload del Downloader e ne torna l'url per poterlo scaricare attraverso il downloader
     * @param file il file da inviare
     * @param filename il nome che il file dovrà avere sul repository
     * @param mimeType il mimeType del file
     * @param codiceAzienda il codice azienda alla quale il file appartiene
     * @param forceDownload se "true" l'url tornato forzerà il download
     * @return l'url per poter scaricare il file attraverso la funzione download del Downloader
     * @throws FirmaRemotaHttpException 
     */
    public String uploadToUploader(InputStream file, String filename, String mimeType, String codiceAzienda, Boolean forceDownload) throws FirmaRemotaHttpException {
        String res;
        String token;
        
        // per prima cosa creo il token da inserire per la chiamata alla funzione upload del Downloader
        try {
            token = buildToken(getUploaderContext(filename, null));
        } catch (Exception ex) {
            String errorMessage = "errore nella creazione del token per l'upload";
            logger.error(errorMessage, ex);
            throw new FirmaRemotaHttpException(errorMessage, ex);
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
                throw new FirmaRemotaHttpException(errorMessage, ex);
            }
            
            // reperisco l'url del downloader dell'azienda a cui il file fa riferimento
            String uploadUrl = this.configParams.getDownloaderParams(codiceAzienda).get("uploadUrl");
            
            // creo la richiesta multipart mettendo il token nei query-params
            RequestBody dataBody = RequestBody.create(okhttp3.MultipartBody.FORM, tmpFileToUpload);
            MultipartBody multipartBody = new MultipartBody.Builder()
                .addPart(MultipartBody.Part.createFormData("file", filename, dataBody))
                .build();
            Request request = new Request.Builder()
                .url(String.format("%s?token=%s", uploadUrl, token))
                .post(multipartBody)
                .build();

            // eseguo la chiamata all'upload
            OkHttpClient httpClient = getHttpClient();
            Call call = httpClient.newCall(request);
            Response response = call.execute();

            ResponseBody content = response.body();
            if (!response.isSuccessful()) {
                if (content != null) {
                    throw new FirmaRemotaHttpException(String.format("errore nella chiamata all'URL: %s RESPONSE: %s", uploadUrl, content.string()));
                } else {
                    throw new FirmaRemotaHttpException(String.format("errore nella chiamata all'URL: %s RESPONSE: null", uploadUrl));
                }
            } else { // tutto ok
                if (content != null) {
                    // se tutto ok creo l'url per il download e lo torno
                    Map<String, Object> downloadParams = objectMapper.readValue(content.byteStream(), new TypeReference<Map<String, Object>>(){});
                    res = buildDownloadUrl(codiceAzienda, filename, mimeType, downloadParams, forceDownload);
                }
                else {
                    throw new FirmaRemotaHttpException(String.format("l'upload non ha tornato risultato", uploadUrl));
                }
            }
            return res;
        }
        catch (Exception ex) {
            String errorMessage = "errore nella creazione del token per l'upload";
            logger.error(errorMessage, ex);
            throw new FirmaRemotaHttpException(errorMessage, ex);
        } finally { // elimina sempre il file temporaneo creato e chiude lo stream del file passato in input
            IOUtils.closeQuietly(file);
            if (tmpFileToUpload != null && tmpFileToUpload.exists()) {
                tmpFileToUpload.delete();
            }
        }
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
    private String buildToken(Map<String,Object> context) throws IOException, AuthorizationUtilsException, NoSuchAlgorithmException, InvalidKeySpecException {
        DownloaderTokenCreator downloaderTokenCreator = new DownloaderTokenCreator();
        PrivateKey signTokenPrivateKey = downloaderTokenCreator.getSignTokenPrivateKey(signTokenPrivateKeyFileLocation, signTokenPrivateKeyAlias, signTokenPrivateKeyPassword);
        RSAPublicKey encryptionPublicKey = downloaderTokenCreator.getEncryptionPublicKey(this.downloaderEncryptionPublicKey.getFile());
        return downloaderTokenCreator.getToken(context, this.downloaderPublicCertBabel.getFile(), signTokenPrivateKey, encryptionPublicKey, this.tokenExpireSeconds, "firma-internauta");
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
     * @throws FirmaRemotaConfigurationException
     * @throws IOException
     * @throws AuthorizationUtilsException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException 
     */
    private String buildDownloadUrl(String codiceAzienda, String fileName, String mimeType, Map<String, Object> downloadParams, Boolean forceDownload) throws FirmaRemotaConfigurationException, IOException, AuthorizationUtilsException, NoSuchAlgorithmException, InvalidKeySpecException {
        String downloadUrl = this.configParams.getDownloaderParams(codiceAzienda).get("downloadUrl");
        Map<String, Object> downloaderContext = getDownloaderContext(downloadParams, fileName, mimeType);
        String token = buildToken(downloaderContext);
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
