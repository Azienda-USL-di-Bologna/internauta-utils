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
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.credentialproxy.CredentialProxyService;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaHttpException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.InvalidCredentialException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteFileNotFoundException;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import okhttp3.Call;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class FirmaRemotaInfocert extends FirmaRemota {

    private static Logger logger = LoggerFactory.getLogger(FirmaRemotaInfocert.class);
    private static String INFOCERT_SIGN_SERVICE = "InfocertSignService";
    
    private Boolean credentialProxyActive = false;
    private Map<String, Object> credentialProxyAdminInfo;
    private CredentialProxyService credentialProxyService;
    private String signServiceEndPointUri;
    
    public FirmaRemotaInfocert(ConfigParams configParams, FirmaRemotaDownloaderUtils firmaRemotaDownloaderUtils, Configuration configuration, InternalCredentialManager internalCredentialManager) throws FirmaRemotaConfigurationException {
        super(configParams, firmaRemotaDownloaderUtils, configuration, internalCredentialManager);
        
        // leggo le informazioni di configurazione della firma remota e del credential proxy
        Map<String, Object> firmaRemotaConfiguration = configuration.getParams();
        Map<String, Object> infocertServiceConfiguration = (Map<String, Object>) firmaRemotaConfiguration.get(INFOCERT_SIGN_SERVICE);
        signServiceEndPointUri = infocertServiceConfiguration.get("InfocertSignServiceEndPointUri").toString();
        credentialProxyAdminInfo = (Map<String, Object>) infocertServiceConfiguration.get("CredentialProxyAdminInfo");
    }

    @Override
    public FirmaRemotaInformation firma(FirmaRemotaInformation firmaRemotaInformation, String codiceAzienda) throws FirmaRemotaHttpException, RemoteFileNotFoundException, WrongTokenException, InvalidCredentialException, RemoteServiceException {
        
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
                MultipartBody.Builder formData = new MultipartBody.Builder();
                
                if (userInformation.useSavedCredential()) {
                    if (configuration.getInternalCredentialsManager()) {
                        formData.addFormDataPart("pin", internalCredentialManager.getPlainPassword(userInformation.getUsername(), configuration.getHostId()));
                    } else {
                        formData.addFormDataPart("pin", userInformation.getPassword());
                    }
                } else {
                    formData.addFormDataPart("pin", userInformation.getPassword());
                }
                String context = InfoCertContextEnum.AUTO.context;
                if (userInformation.getModalitaFirma().equals(InfocertUserInformation.ModalitaFirma.AUTOMATICA)) {
                    formData.addFormDataPart("otp", userInformation.getToken());
                    context = InfoCertContextEnum.REMOTE.context;
                }
                String endPointFirmaURI;
                switch (file.getFormatoFirma()) {
                    case PDF:      
                        endPointFirmaURI = InfoCertPathEnum.FIRMA_CADES.getPath(context, userInformation.getUsername());
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
                            formData.addFormDataPart("box_signature_reason", pdfSignFieldDescriptor.getSignName());         
                        }
                        break;

                    case P7M:
                        endPointFirmaURI = InfoCertPathEnum.FIRMA_PADES.getPath(context, userInformation.getUsername());
                        break;
                    default:
                        throw new FirmaRemotaHttpException(String.format("unexpected or invalid sign format %s", file.getFormatoFirma()));
                }
                
                logger.info(String.format("sending file %s for pdf sign...", file.getFileId()));
                
                MultipartBody multipartBody = formData.addPart(MultipartBody.Part.createFormData("file", filename, dataBody))
                        .build();
                
                Request request = new Request.Builder()
                        .url(signServiceEndPointUri + endPointFirmaURI)
                        .post(multipartBody)
                        .build();

                // eseguo la chiamata all'upload
                OkHttpClient.Builder builder = new OkHttpClient.Builder();
                OkHttpClient httpClient = builder.connectTimeout(15, TimeUnit.MINUTES).readTimeout(15, TimeUnit.MINUTES).writeTimeout(15, TimeUnit.MINUTES).build();
                Call call = httpClient.newCall(request);
                okhttp3.Response response = call.execute();

                if (response.isSuccessful()) {
                    try ( InputStream signedFileIs = response.body().byteStream()) {
                        // esegue l'upload (su mongo o usando l'uploader a seconda di file.getOutputType()) e setta il risultato sul campo adatto (file.setUuidFirmato() o file.setUrlFirmato())
                        super.upload(file, signedFileIs, codiceAzienda);
                        logger.info("File firmato");
                    } catch (IOException | MinIOWrapperException e) {
                        logger.error(e.getMessage());
                    }
                } else {
                    logger.error("Errore:" + response.message());
                }      
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            } catch (EncryptionException ex) {
                java.util.logging.Logger.getLogger(FirmaRemotaInfocert.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return firmaRemotaInformation;
    }

    @Override
    public void preAuthentication(UserInformation userInformation) throws FirmaRemotaHttpException, WrongTokenException, InvalidCredentialException, RemoteServiceException {
      
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

}
