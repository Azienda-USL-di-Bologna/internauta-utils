package it.bologna.ausl.internauta.utils.firma.remota.namirialsignservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.bologna.ausl.internauta.utils.firma.configuration.FirmaHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaFile;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.SignAppearance;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.namirialsignservice.NamirialRestPathsEnum;
import it.bologna.ausl.internauta.utils.firma.data.remota.namirialsignservice.NamirialUserInformation;
import it.bologna.ausl.internauta.utils.firma.remota.FirmaRemota;
import it.bologna.ausl.internauta.utils.firma.remota.InternalCredentialManager;
import it.bologna.ausl.internauta.utils.firma.utils.ConfigParams;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaHttpException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.InvalidCredentialException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteServiceException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.TimeoutException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.WrongTokenException;
import it.bologna.ausl.internauta.utils.firma.remota.utils.FirmaRemotaDownloaderUtils;
import it.bologna.ausl.internauta.utils.firma.utils.exceptions.EncryptionException;
import it.bologna.ausl.model.entities.firma.Configuration;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Classe che implementa i metodi necessari per la firma remota Namirial.
 *
 * @author gdm
 */
public class FirmaRemotaNamirial extends FirmaRemota {

    private static final Logger logger = LoggerFactory.getLogger(FirmaRemotaNamirial.class);
    private static final String NAMIRIAL_SIGN_SERVICE = "NamirialSignService";

    private final String signServiceEndPointUri;
    private OkHttpClient okHttpClient;

    public FirmaRemotaNamirial(ConfigParams configParams, FirmaRemotaDownloaderUtils firmaRemotaDownloaderUtils, Configuration configuration, InternalCredentialManager internalCredentialManager, FirmaHttpClientConfiguration firmaHttpClientConfiguration, String sslCertPath, String sslCertPswd) throws FirmaRemotaConfigurationException {
        super(configParams, firmaRemotaDownloaderUtils, configuration, internalCredentialManager, firmaHttpClientConfiguration);
        buildOkHttpClientWithSSLContext(sslCertPath, sslCertPswd);
        
        // leggo le informazioni di configurazione della firma remota e del credential proxy
        Map<String, Object> firmaRemotaConfiguration = configuration.getParams();
        Map<String, Object> namirialServiceConfiguration = (Map<String, Object>) firmaRemotaConfiguration.get(NAMIRIAL_SIGN_SERVICE);
        signServiceEndPointUri = namirialServiceConfiguration.get("NamirialSignServiceEndPointUri").toString();
    }

    /**
     * Implementazione firma remota Namirial.
     *
     * @param firmaRemotaInformation L'oggetto contenente i files da firmare e le credenziali utente.
     * @param codiceAzienda Il codice dell'azienda, utilizzato per effettuare l'upload del file sul repository.
     * @return L'oggetto firmaRemotaInformation con le informazioni aggiuntive dei file firmati.
     * @throws FirmaRemotaHttpException
     */
    @Override
    public FirmaRemotaInformation firma(FirmaRemotaInformation firmaRemotaInformation, String codiceAzienda, HttpServletRequest request) throws FirmaRemotaHttpException {

        /* 
        come prima cosa reperiamo le credenziali. Queste cambiano in base al fatto che si firmi con OTP o con firma AUTOMATICA.
        Per la firma OTP la procedura è diversa a seconda se si vuole firmare un solo file o più di uno. In quanto per la firma di un solo file è sufficiente
        passare l'OTP all'interno delle credenziali, mentre per la firma di più file è necessario aprire una sessione, facendo una chiamata ad un url apposito passando
        le credenziali con l'OTP.
        Per semplicità è stato scelto di usare sempre la procedura della firma di più file, anche se se ne vuole firmare solo uno.
        */
        NamirialUserInformation userInformation = (NamirialUserInformation) firmaRemotaInformation.getUserInformation();
        String sessionKey = null;
        // come scritto sopra, nel caso di firma OTP, facciamo la chiamata per l'apertura della sessione
        if (userInformation.getModalitaFirma() == NamirialUserInformation.ModalitaFirma.OTP) {
            // esegue l'apertura della sessione e torna una sessionKey, da passare in ogni chiamata di firma.
            sessionKey = openSession(userInformation);
        }
        
        try {
            // genera il json delle credenziali da passare in tutte le chiamate di firma, se c'è la sessionKey la include nel json
            String credentialJson;
            try {
                credentialJson = getCredentialJson(userInformation, sessionKey);
            } catch (Throwable ex) {
                String errorMessage = "errore nella creazione del Json delle credenziali";
                logger.error(errorMessage, ex);
                throw new FirmaRemotaHttpException(errorMessage, ex);
            }

            List<FirmaRemotaFile> filesDaFirmare = firmaRemotaInformation.getFiles();

            // clicla su tutti i file da firmare
            for (FirmaRemotaFile file : filesDaFirmare) {
                File tmpFileToSign = null;
                try {
                    /* 
                    Se non esiste, crea una cartella in cui inserire i file temporanei. L'unico file temporaneo sarà il file da firmare, che viene scaricato per poi essere passato
                    alla firma. Alla fine questo file viene eliminato
                    */
                    File tempDir = new File(System.getProperty("java.io.tmpdir"), "firma_remota");
                    if (!tempDir.exists()) {
                        tempDir.mkdir();
                    }
                    String filename = "namirial_firma_remota_to_sing_tmp";
                    try {
                        // crea il file da firmare scaricamendolo dall'utr passato nei parametri di firma.
                        tmpFileToSign = File.createTempFile(filename, null, tempDir);
                        try (InputStream in = new URL(file.getUrl()).openStream();) {
                            logger.info(String.format("saving file %s on temp file %s...", file.getFileId(), tmpFileToSign.getAbsolutePath()));
                            Files.copy(in, Paths.get(tmpFileToSign.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (Throwable ex) {
                        String errorMessage = "errore nella creazione del file temporaneo da firmare";
                        logger.error(errorMessage, ex);
                        throw new FirmaRemotaHttpException(errorMessage, ex);
                    }

                    // se il file è più grande di 78MB (il limite è 80, ma ci teniamo leggermente più bassi per sicurezza) devo fare una chiamata ad un API diversa
                    long fileSize = tmpFileToSign.length();
                    boolean largeFile = fileSize > 78 * 1000000;
                    logger.info(String.format("file size: %s Bytes, largeFile: %s", fileSize, largeFile));
//                    largeFile = true;
                    // creo la richiesta multipart per la firma
                    MultipartBody.Builder formData = new MultipartBody.Builder().setType(MultipartBody.FORM);

                    // aggiunge la part con le credenziali
                    formData.addFormDataPart("credentials", credentialJson);

                    NamirialRestPathsEnum firmaPath;
                    String filePartName;
                    switch (file.getFormatoFirma()) {
                        case PDF: // firma PADES
                            logger.info(String.format("creating padesPreferences for file %s...", file.getFileId()));
                            logger.info("signAppearence: " + file.getSignAppearance());
                            String padesPreferences;
                            try {
                                // crea il json con la configurazione della firma PADES e le aggiunge alla multipart
                                padesPreferences = configParams.getObjectMapper().writeValueAsString(getPadesPreference(file.getSignAppearance()));
                            } catch (Throwable ex) {
                                String errorMessage = "errore nella creazione del Json delle padesPreferences ";
                                logger.error(errorMessage, ex);
                                throw new FirmaRemotaHttpException(errorMessage, ex);
                            }
                            if (!largeFile) {
                                firmaPath = NamirialRestPathsEnum.PADES_SIGN;
                                filePartName = "buffer";
                                formData.addFormDataPart("padesPreferences", padesPreferences);
                            } else {
                                firmaPath = NamirialRestPathsEnum.PADES_SIGN_LARGE;
                                filePartName = "file";
                                formData.addFormDataPart("preferences", padesPreferences);
                            }
                            break;
                        case P7M: // firma CADES
                            logger.info(String.format("creating cadesPreferences for file %s...", file.getFileId()));
                            String cadesPreferences;
                            try {
                                // crea il json con la configurazione della firma CADES e le aggiunge alla multipart
                                cadesPreferences = configParams.getObjectMapper().writeValueAsString(getCadesPreference());
                            }  catch (Throwable ex) {
                                String errorMessage = "errore nella creazione del Json delle cadesPreferences ";
                                logger.error(errorMessage, ex);
                                throw new FirmaRemotaHttpException(errorMessage, ex);
                            }
                            if (!largeFile) {
                                firmaPath = NamirialRestPathsEnum.CADES_SIGN;
                                filePartName = "buffer";
                                formData.addFormDataPart("cadesPreferences", cadesPreferences);
                            } else {
                                firmaPath = NamirialRestPathsEnum.CADES_SIGN_LARGE;
                                filePartName = "file";
                                formData.addFormDataPart("preferences", cadesPreferences);
                            }
                            break;
                        default:
                            throw new FirmaRemotaHttpException(String.format("unexpected or invalid sign format %s", file.getFormatoFirma()));
                    }

                    // aggiunge il file al multipart
                    logger.info(String.format("sending file %s for pdf sign...", file.getFileId()));
                    RequestBody dataBody = RequestBody.create(MediaType.parse("application/octet-stream"), tmpFileToSign);
                    MultipartBody multipartBody = formData.addFormDataPart(filePartName, filename, dataBody).build();
                    String signUrl = signServiceEndPointUri + firmaPath.getPath();
                    Request httpRequest = new Request.Builder().header("Content-Type", "multipart/form-data")
                        .url(signUrl)
                        .post(multipartBody)
                        .build();

                    Call call = okHttpClient.newCall(httpRequest);
                    okhttp3.Response response;
                    try {
                        // effettua la chiamata
                        response = call.execute();
                    } catch (Throwable ex) {
                        String errorMessage = String.format("errore nella richiesta http per la firma all'url: %s", signUrl);
                        logger.error(errorMessage, ex);
                        throw new FirmaRemotaHttpException(errorMessage, ex);
                    }

                    try (ResponseBody responseBody = response.body()) {
                        // se tutto ok carica il file su minIO
                        if (response.isSuccessful()) {
                            try {
                                InputStream signedFileIs = responseBody.byteStream();
                                // esegue l'upload (su mongo o usando l'uploader a seconda di file.getOutputType()) e setta il risultato sul campo adatto (file.setUuidFirmato() o file.setUrlFirmato())
                                logger.info("File firmato, upload su minIO...");
                                super.upload(file, signedFileIs, codiceAzienda, request);
                                logger.info("File caricato su minIO");
                            } catch (Throwable ex) {
                                String errorMessage = "errore nell'upload del file su minIO";
                                logger.error(errorMessage, ex);
                                throw new FirmaRemotaHttpException(errorMessage, ex);
                            }
                        } else { // se c'è un errore lancia l'eccezione corretta, relativa all'errore ricevuto
                            String errorMessage = "Namirial ha tornato un errore, rilancio l'eccezione appropriata...";
                            logger.error(errorMessage);
                            throwCorrectException(response);
                        }
                    }
                } finally { // elimina il file temporaneo, se è stato creato
                    if (tmpFileToSign != null) {
                        if (!tmpFileToSign.delete()) {
                            tmpFileToSign.deleteOnExit();
                        }
                    }
                }
            }
        } finally { // se è stata aperta una sessione, la chiude
            if (sessionKey != null) {
                closeSession(userInformation, sessionKey);
            }
        }
        
        

        return firmaRemotaInformation;
    }

    /**
     * Costruisce okHttpClient, partendo dalla proprietà impostante in internauta, inserendo la chiave nel keystore di java.
     * Il formato della chiave deve essere p12
     *
     * @param sslCertPath il path del file p12 con la chiave
     * @param sslCertPswd  la password del p12
     */
    private void buildOkHttpClientWithSSLContext(String sslCertPath, String sslCertPswd) {
        SSLContext sslContext;
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            FileInputStream clientCertificateContent = new FileInputStream(sslCertPath);
            keyStore.load(clientCertificateContent, sslCertPswd.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, sslCertPswd.toCharArray());

            KeyStore trustedStore = loadJavaKeyStore();

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustedStore);

            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, new SecureRandom());
            
            this.okHttpClient = firmaHttpClientConfiguration.getHttpClientManager().getOkHttpClient().newBuilder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0])
                .build();
        } catch (Throwable ex) {
            logger.error("errore buildOkHttpClientWithSSLContext", ex);
        }

    }

    /**
     * Carica il keystore di java
     * @return
     * @throws FileNotFoundException
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException 
     */
    private KeyStore loadJavaKeyStore() throws FileNotFoundException, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        String relativeCacertsPath = "/lib/security/cacerts".replace("/", File.separator);
        String filename = System.getProperty("java.home") + relativeCacertsPath;
        FileInputStream is = new FileInputStream(filename);

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        String password = "changeit";
        keystore.load(is, password.toCharArray());

        return keystore;
    }

    /**
     * Implementazione della chiamata http per richiedere il token OTP.
     *
     * @param userInformation Le informazioni dell'utente.
     */
    @Override
    public void preAuthentication(UserInformation userInformation) throws FirmaRemotaHttpException {
        logger.info("Richiesta codice OTP per l'utente: " + userInformation.getUsername());
        try {
            String url = signServiceEndPointUri + NamirialRestPathsEnum.GET_SMS_OTP.getPath();
            Map<String, Map<String, Object>> credentialReqMap = new HashMap<>();
            credentialReqMap.put("credentials", getCredentialMap((NamirialUserInformation) userInformation, null));
            String credentialJson = this.configParams.getObjectMapper().writeValueAsString(credentialReqMap);
            RequestBody reqBody = RequestBody.create(MediaType.parse("application/json"),credentialJson);
            Request request = new Request.Builder()
                .url(url)
                .method("POST", reqBody)
                .addHeader("Content-Type", "application/json")
                .build();
            Call call = okHttpClient.newCall(request);
            try (Response resp = call.execute();) {
            } catch (Throwable ex) {
                throw ex;
            }
        } catch (Throwable ex) {
            logger.error("errore nella richiesta dell'invio dell'otp per SMS", ex);
            throw new FirmaRemotaHttpException(ex.getMessage());
        }
    }

    /**
     * Questa funzione identifica uno stato di errore, lancia la corretta
     * eccezione in base al codice di errore indicato nell'header "errorCode" della risposta (come da documentazione Namiral)
     *
     * @param response la responde della richiesta fatto con okHttp
     * @throws InvalidCredentialException Credenziali non valide.
     * @throws WrongTokenException Token non valido.
     * @throws FirmaRemotaHttpException Altro tipo di errore.
     * @throws TimeoutException è scaduto il tempo massimo per la firma
     */
    public void throwCorrectException(Response response) throws InvalidCredentialException, WrongTokenException, TimeoutException, FirmaRemotaHttpException {
        String errorCode = response.header("errorCode");
        String errorMessage = response.header("errorMsg");

        logger.error(String.format("Errore firma remota Namirial code: %s - description: %s", errorCode, errorMessage));
        if (errorCode != null) {
            switch (errorCode) {
                case "4":
                case "65":
                    throw new InvalidCredentialException(errorMessage);
                case "44":
                case "73":
                case "1001":
                case "1007":
                case "1009":
                case "1016":
                    throw new WrongTokenException(errorMessage);
                case "97":
                    throw new TimeoutException(errorMessage);
                // TODO: inserire gli eventuali altri casi di errore
                default:
                    throw new FirmaRemotaHttpException(String.format("remote server error. code: %s - description: %s", errorCode, errorMessage));
            }
        } else {
            throw new FirmaRemotaHttpException(String.format("remote server error. httpCode: %s - httpMessage: %s", response.code(), response.message()));
        }
    }

    /**
     * Apre una sessione per la firma OTP
     * @param userInformation le userInformation che devono contenere 
     *  - username, 
     *  - password(se non memorizzata nell'internalCredenzialManager)
     *  - Otp
     * @return
     * @throws FirmaRemotaHttpException
     * @throws InvalidCredentialException
     * @throws TimeoutException
     * @throws WrongTokenException 
     */
    private String openSession(NamirialUserInformation userInformation) throws InvalidCredentialException, TimeoutException, WrongTokenException, FirmaRemotaHttpException {
        String credentialJson;
        try {
            //credentialJson = getCredentialJson(userInformation, null);
            Map<String, Map<String, Object>> credentialReqMap = new HashMap<>();
            credentialReqMap.put("credentials", getCredentialMap(userInformation, null));
            credentialJson = this.configParams.getObjectMapper().writeValueAsString(credentialReqMap);
        } catch (Throwable ex) {
            throw new FirmaRemotaHttpException("errore nella creazione del json delle credenziali per la richiesta http openSession", ex);
        }
        
        
        
        RequestBody reqBody = RequestBody.create(MediaType.parse("application/json"),credentialJson);

        String url = signServiceEndPointUri + NamirialRestPathsEnum.OPEN_SESSION.getPath();
        Request request = new Request.Builder()
            .url(url)
            .method("POST", reqBody)
            .addHeader("Content-Type", "application/json")
            .build();
        
        try (Response response = okHttpClient.newCall(request).execute();){
            if (!response.isSuccessful()) {
                throwCorrectException(response);
            }
            try (ResponseBody respBody = response.body()){
                return respBody.string();
            } catch (IOException ex) {
                 throw new FirmaRemotaHttpException("errore nella lettura della sessionKey", ex);
            }
        } catch (Throwable ex) {
            throw new FirmaRemotaHttpException("errore nella richiesta http openSession", ex);
        }
        
    }
    
    /**
     * chiude la sessione aperta con openSession
     * @param userInformation le userInformation che devono contenere lo username
     * @param sessionKey la chiave della sessioen da chiudere (tornata dalla openSession)
     * @throws FirmaRemotaHttpException 
     */
    private void closeSession(NamirialUserInformation userInformation, String sessionKey) throws FirmaRemotaHttpException {
        String credentialJson;
        try {
            Map<String, Map<String, Object>> credentialReqMap = new HashMap<>();
            credentialReqMap.put("credentials", getCredentialMap(userInformation, sessionKey));
            credentialJson = this.configParams.getObjectMapper().writeValueAsString(credentialReqMap);
        } catch (Throwable ex) {
            throw new FirmaRemotaHttpException("errore nella creazione del json delle credenziali per la richiesta http closeSession", ex);
        }
        RequestBody reqBody = RequestBody.create(MediaType.parse("application/json"),credentialJson);

        String url = signServiceEndPointUri + NamirialRestPathsEnum.CLOSE_SESSION.getPath();
        Request request = new Request.Builder()
            .url(url)
            .method("POST", reqBody)
            .addHeader("Content-Type", "application/json")
            .build();
        
        try (Response response = okHttpClient.newCall(request).execute();){
            if (!response.isSuccessful()) {
                throwCorrectException(response);
            }
        } catch (Throwable ex) {
            throw new FirmaRemotaHttpException("errore nella richiesta http closeSession", ex);
        }
    }
    
    /**
     * Genera una mappa che rappresenta il parametro credentials della richesta post
     * @param userInformation le userInformation contenenti
     *  - username, 
     *  - password(se non memorizzata nell'internalCredenzialManager)
     *  - Otp, solo nel caso di firma OTP o nel caso il risultato serva per aprire una sessione di firma con il metodo OpenSession
     * @return la stringa json da passare come parametro credentials alla richesta post
     * @throws JsonProcessingException
     * @throws EncryptionException 
     */
    private Map<String, Object> getCredentialMap(NamirialUserInformation userInformation, String sessionKey) throws EncryptionException  {
        String password;
        if (userInformation.useSavedCredential() != null  && userInformation.useSavedCredential() && configuration.getInternalCredentialsManager() != null) {
            password = internalCredentialManager.getPlainPassword(userInformation.getUsername(), configuration.getHostId());
        } else {
            password = userInformation.getPassword();
        }
        Map<String, Object> credential = new HashMap();
        credential.put("username", userInformation.getUsername());
        credential.put("password", password);
        if (userInformation.getOtp()!= null) {
            credential.put("idOtp", -1);
            credential.put("otp", userInformation.getOtp());
        }
        if (sessionKey != null) {
            credential.put("sessionKey", sessionKey);
        }
        return credential;
    }
    
    /**
     * Genera la stringa json da passare come parametro credentials alla richesta post
     * @param userInformation le userInformation contenenti
     *  - username, 
     *  - password(se non memorizzata nell'internalCredenzialManager)
     *  - Otp, solo nel caso di firma OTP o nel caso il risultato serva per aprire una sessione di firma con il metodo OpenSession
     * @param sessionKey ottenibile tramite il metodo OpenSession. Da passare se si vuole generare il json per una firma OTP
     * @return la stringa json da passare come parametro credentials alla richesta post
     * @throws JsonProcessingException
     * @throws EncryptionException 
     */
    private String getCredentialJson(NamirialUserInformation userInformation, String sessionKey) throws EncryptionException, JsonProcessingException {
        Map<String, Object> credentialMap = getCredentialMap(userInformation, sessionKey);
        return configParams.getObjectMapper().writeValueAsString(credentialMap);
    }
    
    /**
     * Genera il json da passare nel parametro "padesPreferences" per la firma PADES
     * @param signAppearance l'oggetto che descrive le caratteristiche del campo firma visibile, se la firma deve essere invisibile, passare null
     * @return una mappa che rappresenta il json delle "padesPreferences"
     */
    private Map<String, Object> getPadesPreference(SignAppearance signAppearance) {
        Map<String, Object> padesPreference = getCadesPreference();
        Map<String, Object> signerImage = new HashMap<>();
        if (signAppearance != null) {
            String[] signAppearanceArray = signAppearance.getSignPosition().split(";");
            int page;
            if (signAppearanceArray[0].equalsIgnoreCase("n")) {
                page = -1;
            } else {
                page = Integer.parseInt(signAppearanceArray[0]);
            }
            padesPreference.put("page", page);
            signerImage.put("textVisible", true);
            signerImage.put("width", signAppearanceArray[1]);
            signerImage.put("height", signAppearanceArray[2]);
            signerImage.put("x", signAppearanceArray[3]);
            signerImage.put("y", signAppearanceArray[4]);
        } else { 
            /* 
            se signAppearance è null vuol dire che si vuole una firma invisibile, per cui viene settata la proprietà "textVisible" a false in modo che Namirial inserisca
            la firma invisibile
            */
            signerImage.put("textVisible", false);
        }
        padesPreference.put("signerImage", signerImage);
        return padesPreference;
    }
    
    /**
     * Genera il json da passare nel parametro "cadesPreferences" per le firme CADES
     * @return una mappa che rappresenta il json delle "cadesPreferences"
     */
    private Map<String, Object> getCadesPreference() {
        Map<String, Object> cadesPreference = new HashMap<>();
        cadesPreference.put("level", "B");
        cadesPreference.put("withTimestamp", false);
        return cadesPreference;
    }

    /**
     * il salvataggio delle credenziali non è supportato da Namirial, per cui torniamo sempre false
     * @param userInformation
     * @param hostId
     * @return
     * @throws FirmaRemotaHttpException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    @Override
    protected boolean externalExistingCredential(UserInformation userInformation, String hostId) throws FirmaRemotaHttpException, InvalidCredentialException, RemoteServiceException {
        return false;
    }
    /**
     * il salvataggio delle credenziali non è supportato da Namirial, per cui torniamo sempre false
     * @param userInformation
     * @param hostId
     * @return
     * @throws FirmaRemotaHttpException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    @Override
    protected boolean externalSetCredential(UserInformation userInformation, String hostId) throws FirmaRemotaHttpException, InvalidCredentialException, RemoteServiceException {
        return false;
    }
    /**
     * il salvataggio delle credenziali non è supportato da Namirial, per cui torniamo sempre false
     * @param userInformation
     * @param hostId
     * @return
     * @throws FirmaRemotaHttpException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    @Override
    protected boolean externalRemoveCredential(UserInformation userInformation, String hostId) throws FirmaRemotaHttpException, InvalidCredentialException, RemoteServiceException {
        return false;
    }
}
