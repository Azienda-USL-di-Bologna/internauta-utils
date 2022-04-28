package it.bologna.ausl.internauta.utils.firma.remota.infocertsignservice;

import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaFile;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.infocertsignservice.InfoCertContextEnum;
import it.bologna.ausl.internauta.utils.firma.data.remota.infocertsignservice.InfoCertPathEnum;
import it.bologna.ausl.internauta.utils.firma.data.remota.infocertsignservice.InfocertUserInformation;
import it.bologna.ausl.internauta.utils.firma.remota.FirmaRemota;
import it.bologna.ausl.internauta.utils.firma.remota.configuration.ConfigParams;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.credentialproxy.CredentialProxyService;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.InvalidCredentialException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteFileNotFoundException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteServiceException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.WrongTokenException;
import it.bologna.ausl.internauta.utils.firma.remota.utils.FirmaRemotaDownloaderUtils;
import it.bologna.ausl.internauta.utils.firma.remota.utils.pdf.PdfSignFieldDescriptor;
import it.bologna.ausl.internauta.utils.firma.remota.utils.pdf.PdfUtils;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
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
    
    public FirmaRemotaInfocert(String codiceAzienda, ConfigParams configParams, FirmaRemotaDownloaderUtils firmaRemotaDownloaderUtils) throws FirmaRemotaConfigurationException {
        super(codiceAzienda, configParams, firmaRemotaDownloaderUtils);
        
        // leggo le informazioni di configurazione della firma remota e del credential proxy
        Map<String, Map<String, Object>> firmaRemotaConfiguration = configParams.getFirmaRemotaConfiguration(codiceAzienda);
        Map<String, Object> infocertServiceConfiguration = firmaRemotaConfiguration.get(INFOCERT_SIGN_SERVICE);
        signServiceEndPointUri = infocertServiceConfiguration.get("SignServiceEndPointUri").toString();
        credentialProxyAdminInfo = (Map<String, Object>) infocertServiceConfiguration.get("CredentialProxyAdminInfo");
    }

    @Override
    public FirmaRemotaInformation firma(FirmaRemotaInformation firmaRemotaInformation) throws FirmaRemotaException, RemoteFileNotFoundException, WrongTokenException, InvalidCredentialException, RemoteServiceException {
        
        List<FirmaRemotaFile> filesDaFirmare = firmaRemotaInformation.getFiles();
        
        for (FirmaRemotaFile file : filesDaFirmare) { 
            
            try {
                File tempDir = new File(System.getProperty("java.io.tmpdir"), "firma_remota");
                if (!tempDir.exists()) {
                    tempDir.mkdir();
                }
                File tmpFileToSign = File.createTempFile("firma_remota_to_sing_tmp", null, tempDir);
                
                InputStream in = new URL(file.getUrl()).openStream();
                logger.info(String.format("saving file %s on temp file %s...", file.getFileId(), tmpFileToSign.getAbsolutePath()));
                Files.copy(in, Paths.get(tmpFileToSign.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
                
                InfocertUserInformation userInformation = (InfocertUserInformation) firmaRemotaInformation.getUserInformation();
                Client client = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();

                FormDataMultiPart form = new FormDataMultiPart();
                form.field("pin", userInformation.getPin());
                String context = InfoCertContextEnum.AUTO.context;
                if (userInformation.getModalitaFirma().equals(InfocertUserInformation.ModalitaFirma.AUTOMATICA)) {
                    form.field("otp", userInformation.getToken());
                    context = InfoCertContextEnum.REMOTE.context;
                }
                String endPointFirmaURI;
                switch (file.getFormatoFirma()) {
                    case PDF:      
                        endPointFirmaURI = InfoCertPathEnum.FIRMA_CADES.getPath(context, userInformation.getAlias());
                        // Check della SignAppearance, se voluta
                        logger.info("signAppearence: " + file.getSignAppearance());
                        if (file.getSignAppearance() != null) {
                            logger.info(String.format("creating signApparence for file %s...", file.getFileId()));
                            PdfSignFieldDescriptor pdfSignFieldDescriptor = PdfUtils.toPdfSignFieldDescriptor(new FileInputStream(tmpFileToSign), file.getSignAppearance());
                            
                            form.field("box_signature_page", Integer.toString(pdfSignFieldDescriptor.getPage()));
                            form.field("box_signature_llx", Integer.toString(pdfSignFieldDescriptor.getLowerLeftX()));
                            form.field("box_signature_lly", Integer.toString(pdfSignFieldDescriptor.getLowerLeftY()));
                            form.field("box_signature_urx", Integer.toString(pdfSignFieldDescriptor.getUpperRightX()));
                            form.field("box_signature_ury", Integer.toString(pdfSignFieldDescriptor.getUpperRightY()));
                            form.field("box_signature_reason", pdfSignFieldDescriptor.getSignName());      
                        }
                        break;

                    case P7M:
                        endPointFirmaURI = InfoCertPathEnum.FIRMA_PADES.getPath(context, userInformation.getAlias());
                        break;
                    default:
                        throw new FirmaRemotaException(String.format("unexpected or invalid sign format %s", file.getFormatoFirma()));
                }
                
                logger.info(String.format("sending file %s for pdf sign...", file.getFileId()));
                form.bodyPart(new FileDataBodyPart("contentToSign-0", tmpFileToSign));
                Response response = client.target(signServiceEndPointUri).path(endPointFirmaURI).
                        request(MediaType.MULTIPART_FORM_DATA).post(Entity.entity(form, form.getMediaType()));
                if (response.getStatus() == 200) {
                    try ( InputStream signedFileIs = response.readEntity(InputStream.class)) {
                        // esegue l'upload (su mongo o usando l'uploader a seconda di file.getOutputType()) e setta il risultato sul campo adatto (file.setUuidFirmato() o file.setUrlFirmato())
                        super.upload(file, signedFileIs);
                        logger.info("File firmato");
                    } catch (IOException | MinIOWrapperException e) {
                        logger.error(e.getMessage());
                    }
                } else {
                    logger.error("Errore:" + response.readEntity(String.class));
                }      
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        }

        return firmaRemotaInformation;
    }

    @Override
    public void preAuthentication(UserInformation userInformation) throws FirmaRemotaException, WrongTokenException, InvalidCredentialException, RemoteServiceException {
    }

    @Override
    public boolean existingCredential(UserInformation userInformation) throws FirmaRemotaException, InvalidCredentialException, RemoteServiceException {
        return false;
    }

    @Override
    public boolean setCredential(UserInformation userInformation) throws FirmaRemotaException, InvalidCredentialException, RemoteServiceException {
        return false;
    }

    @Override
    public boolean removeCredential(UserInformation userInformation) throws FirmaRemotaException, InvalidCredentialException, RemoteServiceException {
        return false;
    }       
}
