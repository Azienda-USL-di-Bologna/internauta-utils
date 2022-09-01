package it.bologna.ausl.internauta.utils.firma.remota.infocertsignservice;

import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaFile;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.infocertsignservice.InfoCertContextEnum;
import it.bologna.ausl.internauta.utils.firma.data.remota.infocertsignservice.InfoCertPathEnum;
import it.bologna.ausl.internauta.utils.firma.data.remota.infocertsignservice.InfocertUserInformation;
import it.bologna.ausl.internauta.utils.firma.remota.FirmaRemota;
import it.bologna.ausl.internauta.utils.firma.remota.InternalCredentialManager;
import it.bologna.ausl.internauta.utils.firma.remota.configuration.ConfigParams;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaHttpException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.InvalidCredentialException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteServiceException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.WrongTokenException;
import it.bologna.ausl.internauta.utils.firma.remota.utils.FirmaRemotaDownloaderUtils;
import it.bologna.ausl.internauta.utils.firma.remota.utils.pdf.PdfSignFieldDescriptor;
import it.bologna.ausl.internauta.utils.firma.remota.utils.pdf.PdfUtils;
import it.bologna.ausl.internauta.utils.firma.utils.exceptions.EncryptionException;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.firma.Configuration;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import okhttp3.Call;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Classe che implementa i metodi necessari per la firma remota Infocert.
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class FirmaRemotaInfocert extends FirmaRemota {

    private static final Logger logger = LoggerFactory.getLogger(FirmaRemotaInfocert.class);
    private static final String INFOCERT_SIGN_SERVICE = "InfocertSignService";

    private final String signServiceEndPointUri;
    private String sslCertPath;
    private String sslCertPswd;

    public FirmaRemotaInfocert(ConfigParams configParams, FirmaRemotaDownloaderUtils firmaRemotaDownloaderUtils, Configuration configuration, InternalCredentialManager internalCredentialManager, String sslCertPath, String sslCertPswd) throws FirmaRemotaConfigurationException {
        super(configParams, firmaRemotaDownloaderUtils, configuration, internalCredentialManager);

        // leggo le informazioni di configurazione della firma remota e del credential proxy
        Map<String, Object> firmaRemotaConfiguration = configuration.getParams();
        Map<String, Object> infocertServiceConfiguration = (Map<String, Object>) firmaRemotaConfiguration.get(INFOCERT_SIGN_SERVICE);
        signServiceEndPointUri = infocertServiceConfiguration.get("InfocertSignServiceEndPointUri").toString();
        this.sslCertPath = sslCertPath;
        this.sslCertPswd = sslCertPswd;
    }

    /**
     * Implementazione firma remota Infocert. Prende i parametri in input e
     * costruisce il json per effettuare la chiamata http al server Infocert.
     *
     * @param firmaRemotaInformation L'oggetto contenente i files da firmare e i
     * parametri dell'utente.
     * @param codiceAzienda Il codice dell'azienda, utilizzato per effettuare
     * l'upload del file sul repository.
     * @return L'oggetto RifmaRemotaInformation con le informazioni aggiuntive
     * dei file firmati.
     * @throws FirmaRemotaHttpException Errore durante l'upload del file.
     */
    @Override
    public FirmaRemotaInformation firma(FirmaRemotaInformation firmaRemotaInformation, String codiceAzienda, HttpServletRequest request) throws FirmaRemotaHttpException {

        List<FirmaRemotaFile> filesDaFirmare = firmaRemotaInformation.getFiles();

        for (FirmaRemotaFile file : filesDaFirmare) {

            try {
                File tempDir = new File(System.getProperty("java.io.tmpdir"), "firma_remota");
                if (!tempDir.exists()) {
                    tempDir.mkdir();
                }
                String filename = "firma_remota_to_sing_tmp";
                File tmpFileToSign = File.createTempFile(filename, null, tempDir);

                InputStream in = new URL(file.getUrl()).openStream();
                logger.info(String.format("saving file %s on temp file %s...", file.getFileId(), tmpFileToSign.getAbsolutePath()));
                Files.copy(in, Paths.get(tmpFileToSign.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);

                InfocertUserInformation userInformation = (InfocertUserInformation) firmaRemotaInformation.getUserInformation();

                // creo la richiesta multipart mettendo il token nei query-params
                RequestBody dataBody = RequestBody.create(okhttp3.MultipartBody.FORM, tmpFileToSign);
                MultipartBody.Builder formData = new MultipartBody.Builder().setType(MultipartBody.FORM);

                if (userInformation.useSavedCredential() != null
                        && userInformation.useSavedCredential()
                        && configuration.getInternalCredentialsManager() != null) {
                    formData.addFormDataPart("pin", internalCredentialManager.getPlainPassword(userInformation.getUsername(), configuration.getHostId()));
                } else {
                    formData.addFormDataPart("pin", userInformation.getPassword());
                }

                String context;
                if (userInformation.getModalitaFirma().equals(InfocertUserInformation.ModalitaFirma.OTP)) {
                    formData.addFormDataPart("otp", userInformation.getToken());
                    context = InfoCertContextEnum.REMOTE.context;
                } else {
                    context = InfoCertContextEnum.AUTO.context;
                }
                String endPointFirmaURI;
                switch (file.getFormatoFirma()) {
                    case PDF:
                        endPointFirmaURI = InfoCertPathEnum.FIRMA_PADES.getPath(context, userInformation.getUsername());
                        // Check della SignAppearance, se voluta
                        logger.info("signAppearence: " + file.getSignAppearance());
                        if (file.getSignAppearance() != null) {
                            logger.info(String.format("creating signApparence for file %s...", file.getFileId()));
                            PdfSignFieldDescriptor pdfSignFieldDescriptor = PdfUtils.toPdfSignFieldDescriptor(new FileInputStream(tmpFileToSign), file.getSignAppearance());

                            formData.addFormDataPart("box_signature_page", Integer.toString(pdfSignFieldDescriptor.getPage()));
                            formData.addFormDataPart("box_signature_llx", Integer.toString(pdfSignFieldDescriptor.getLowerLeftX()));
                            formData.addFormDataPart("box_signature_lly", Integer.toString(pdfSignFieldDescriptor.getLowerLeftY()));
                            formData.addFormDataPart("box_signature_urx", Integer.toString(pdfSignFieldDescriptor.getUpperRightX()));
                            formData.addFormDataPart("box_signature_ury", Integer.toString(pdfSignFieldDescriptor.getUpperRightY()));
                            // formData.addFormDataPart("box_signature_reason", pdfSignFieldDescriptor.getSignName());         
                        }
                        break;

                    case P7M:
                        endPointFirmaURI = InfoCertPathEnum.FIRMA_CADES.getPath(context, userInformation.getUsername());
                        break;
                    default:
                        throw new FirmaRemotaHttpException(String.format("unexpected or invalid sign format %s", file.getFormatoFirma()));
                }

                logger.info(String.format("sending file %s for pdf sign...", file.getFileId()));

                MultipartBody multipartBody = formData.addFormDataPart("contentToSign-0", filename, dataBody)
                        .build();

                Request uploaderRequest = new Request.Builder().header("Content-Type", "multipart/form-data")
                        .url(signServiceEndPointUri + endPointFirmaURI)
                        .post(multipartBody)
                        .build();

                OkHttpClient httpClient;
                // eseguo la chiamata all'upload
                if (sslCertPath != null && sslCertPswd != null) {
                    httpClient = buildSSLHttpClient();
                } else {
                    OkHttpClient.Builder builder = new OkHttpClient.Builder();
                    httpClient = builder.connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build();
                }

                Call call = httpClient.newCall(uploaderRequest);
                okhttp3.Response response = call.execute();

                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        try {
                            InputStream signedFileIs = responseBody.byteStream();
                            // esegue l'upload (su mongo o usando l'uploader a seconda di file.getOutputType()) e setta il risultato sul campo adatto (file.setUuidFirmato() o file.setUrlFirmato())
                            super.upload(file, signedFileIs, codiceAzienda, request);
                            logger.info("File firmato");
                        } catch (MinIOWrapperException e) {
                            logger.error("error", e);
                        }
                    } else {
                        logger.warn("error", response);
                        throwCorrectException(responseBody.string());
                    }
                }
            } catch (IOException | EncryptionException ex) {
                logger.error("error", ex);
                throw new FirmaRemotaHttpException(ex.getMessage());
            }
        }

        return firmaRemotaInformation;
    }

    private OkHttpClient buildSSLHttpClient() {
        SSLContext sslContext = null;
        OkHttpClient httpClient = null;
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

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            httpClient = builder
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0])
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS).build();

        } catch (Throwable e) {
            logger.error("errore buildSSLContext", e);
        }
        return httpClient;

    }

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
        // Non so se ha senso mettere un controllo dell'alias utente o sul numero di volte che può fare richiedi
        try {
            Request request = new Request.Builder()
                    .url(signServiceEndPointUri + InfoCertPathEnum.RICHIESTA_OPT.getPath(InfoCertContextEnum.REMOTE.context, userInformation.getUsername()))
                    .build();

            OkHttpClient httpClient = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build();
            Call call = httpClient.newCall(request);
            call.execute();
        } catch (IOException ex) {
            logger.error("error", ex);
            throw new FirmaRemotaHttpException(ex.getMessage());
        }
    }

    @Override
    protected boolean externalExistingCredential(UserInformation userInformation, String hostId) throws FirmaRemotaHttpException, InvalidCredentialException, RemoteServiceException {
        return false;
    }

    @Override
    protected boolean externalSetCredential(UserInformation userInformation, String hostId) throws FirmaRemotaHttpException, InvalidCredentialException, RemoteServiceException {
        return false;
    }

    @Override
    protected boolean externalRemoveCredential(UserInformation userInformation, String hostId) throws FirmaRemotaHttpException, InvalidCredentialException, RemoteServiceException {
        return false;
    }

    /**
     * Questa funzione identifica uno stato di errore, lancia la corretta
     * eccezione in base al codice di errore parsato dal body della response in
     * xml passato in ingresso come stringa.
     *
     * @param xmlResponse Il body xml della response in formato stringa.
     * @throws InvalidCredentialException Credenziali non valide.
     * @throws WrongTokenException Token non valido.
     * @throws RemoteServiceException Errore generico.
     */
    public void throwCorrectException(String xmlResponse) throws InvalidCredentialException, WrongTokenException, RemoteServiceException {

        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xmlResponse)));
            String errorCode = doc.getElementsByTagName("proxysign-error-code").item(0).getTextContent();
            String errorDescription = doc.getElementsByTagName("proxysign-error-description").item(0).getTextContent();

            logger.info(String.format("Errore firma remota Infocert code: %s - description: %s", errorCode, errorDescription));
            String descriptionString = (errorDescription != null && !errorDescription.isEmpty()) ? ": " + errorDescription : "";
            if (errorCode != null) {
                switch (errorCode) {
                    case "PRS-0002":
                    case "PRS-0009":
                    case "PRS-0010":
                    case "PRS-0011":
                    case "PRS-0012":
                    case "PRS-0017":
                        throw new InvalidCredentialException("invalid credential" + descriptionString);
                    case "PRS-0003":
                    case "PRS-0008":
                    case "PRS-0015":
                        throw new WrongTokenException("invalid or blocked token" + descriptionString);
                    // TODO: inserire gli eventuali altri casi di errore
                    default:
                        throw new RemoteServiceException(String.format("remote server error. code: %s - description: %s", errorCode, errorDescription));
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            logger.error("error", ex);
        }
    }

}
