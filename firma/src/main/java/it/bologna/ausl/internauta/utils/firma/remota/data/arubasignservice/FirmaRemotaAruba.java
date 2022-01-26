package it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice;

import com.sun.xml.internal.ws.developer.JAXWSProperties;
import it.bologna.ausl.internauta.utils.firma.remota.configuration.ConfigParams;

import it.bologna.ausl.internauta.utils.firma.remota.data.FirmaRemota;
import it.bologna.ausl.internauta.utils.firma.remota.data.FirmaRemotaFile;
import it.bologna.ausl.internauta.utils.firma.remota.data.FirmaRemotaInformation;
import it.bologna.ausl.internauta.utils.firma.remota.data.SignAppearance;
import it.bologna.ausl.internauta.utils.firma.remota.data.UserInformation;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.ArubaUserInformation.ModalitaFirma;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.credentialproxy.CredentialProxyAdmin;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.credentialproxy.CredentialProxyService;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.credentialproxy.CredentialProxyServiceService;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.wsclient.ArubaSignService;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.wsclient.ArubaSignServiceService;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.wsclient.Auth;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.wsclient.CredentialsType;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.wsclient.PdfProfile;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.wsclient.PdfSignApparence;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.wsclient.SignRequestV2;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.wsclient.SignReturnV2;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.wsclient.TypeOfTransportNotImplemented_Exception;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.wsclient.TypeTransport;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.FirmaRemotaException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.InvalidCredentialException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.RemoteFileNotFoundException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.RemoteServiceException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.WrongTokenException;
import it.bologna.ausl.internauta.utils.firma.remota.utils.FirmaRemotaUtils;
import it.bologna.ausl.internauta.utils.firma.remota.utils.pdf.PdfSignFieldDescriptor;
import it.bologna.ausl.internauta.utils.firma.remota.utils.pdf.PdfUtils;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.xml.ws.BindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 *
 * @author guido
 */
public class FirmaRemotaAruba extends FirmaRemota {

    private static Logger logger = LoggerFactory.getLogger(FirmaRemotaAruba.class);

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String P7M_CONTENT_TYPE = "application/pkcs7-mime";

    private final ConfigParams configParams;
    Boolean credentialProxyActive = false;
    private final Map<String, Object> credentialProxyAdminInfo;
    private final FirmaRemotaUtils firmaRemotaUtils;
//    private final HttpClient httpClient;
    private final ArubaSignService arubaSignService;
    private final CredentialProxyService credentialProxyService;
    private final String dominioFirmaDefault;

    public FirmaRemotaAruba(ConfigParams configParams, FirmaRemotaUtils firmaRemotaUtils, String dominioFirmaDefault) {
        this.configParams = configParams;
        Map<String, Map<String, Object>> firmaRemotaConfiguration = this.configParams.getFirmaRemotaConfiguration();
        Map<String, Object> arubaServiceConfiguration = firmaRemotaConfiguration.get("ArubaSignService");
        List<String> signServiceEndPointUri = (List<String>) arubaServiceConfiguration.get("ArubaSignServiceEndPointUri");
        List<String> credentialProxyEndPointUriList = (List<String>) arubaServiceConfiguration.get("ArubaCredentialProxyEndPointUriList");
        this.credentialProxyAdminInfo = (Map<String, Object>) arubaServiceConfiguration.get("ArubaCredentialProxyAdminInfo");
        this.credentialProxyActive = (Boolean) credentialProxyAdminInfo.get("active");
        
        this.firmaRemotaUtils = firmaRemotaUtils;
//        this.firmaRemotaUtils = firmaRemotaUtils;
//        this.httpClient = httpClient;
        this.dominioFirmaDefault = dominioFirmaDefault;

        ArubaSignServiceService arubaSignServiceService = new ArubaSignServiceService();
        this.arubaSignService = arubaSignServiceService.getArubaSignServicePort();

        CredentialProxyServiceService credentialProxyServiceService = new CredentialProxyServiceService();
        this.credentialProxyService = credentialProxyServiceService.getCredentialProxyServicePort();

        // TODO: temporaneamente prendiamo il primo url della lista (poi dovremmo fare un qualche meccanismo di fail-over)
        String arubaSignServiceEndpointURL = signServiceEndPointUri.get(0);
        BindingProvider bpArubaSignService = (BindingProvider) this.arubaSignService;
        bpArubaSignService.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, arubaSignServiceEndpointURL);
        bpArubaSignService.getRequestContext().put(JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE, 8192);

        // TODO: temporaneamente prendiamo il primo url della lista (poi dovremmo fare un qualche meccanismo di fail-over)
        String arubaCredentialProxyEndpointURL = credentialProxyEndPointUriList.get(0);
        BindingProvider bpCredentialProxy = (BindingProvider) this.credentialProxyService;
        bpCredentialProxy.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, arubaCredentialProxyEndpointURL);
//        bpCredentialProxy.getRequestContext().put(JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE, 8192);
    }

    @Override
    public FirmaRemotaInformation firma(FirmaRemotaInformation firmaRemotaInformation) throws FirmaRemotaException {

        logger.info("in firma...");
//        MongoWrapper mongoDownload = configParams.getMongoWrapperDownload();
        MinIOWrapper minIOWrapper = this.configParams.getMinIOWrapper();

        List<FirmaRemotaFile> files = firmaRemotaInformation.getFiles();

        Auth identity = getIdentity((ArubaUserInformation) firmaRemotaInformation.getUserInformation());

        String sessionId = null;
        try {
            logger.info("opening session...");
            sessionId = arubaSignService.opensession(identity);
            logger.info("sessionId: " + sessionId);
            if (sessionId == null) {
                throw new RemoteServiceException("sign session opened is null");
            } else {
                if (sessionId.startsWith("KO")) {
                    logger.error(String.format("errore nella firma: %s ", sessionId));
                    String errorCode = sessionId.split("-")[1];
                    throwCorrectException("KO", errorCode, "error opening session");
                }
            }
        } catch (FirmaRemotaException ex) {
            if (sessionId != null) {
                arubaSignService.closesession(identity, sessionId);
            }
            throw ex;
        } catch (Exception ex) {
            logger.error("error opening session", ex);
            if (sessionId != null) {
                logger.info("closing session...");
                arubaSignService.closesession(identity, sessionId);
            }
            throw new FirmaRemotaException("error opening session", ex);
        }

        try {
//            ArssReturn verifyOtp = service.verifyOtp(identity);
//            System.out.println(verifyOtp.getReturnCode());
//            System.out.println(verifyOtp.getDescription());
            for (FirmaRemotaFile file : files) {
//              HttpResponse response = firmaRemotaUtils.getFile(httpClient, file.getUrl());
//              InputStream is = response.getEntity().getContent();
                logger.info(String.format("signing file %s ...", file.getFileId()));
                try (InputStream signedFileIs = sendSignRequest(sessionId, identity, file)) {
                    logger.info(String.format("OutputType %s ...", file.getOutputType().toString()));
                    if (file.getOutputType() == FirmaRemotaFile.OutputType.UUID) {
                        logger.info(String.format("putting file %s on temp repository...", file.getFileId()));
                        MinIOWrapperFileInfo uploadedFileInfo = minIOWrapper.put(signedFileIs, file.getCodiceAzienda(), "/temp", "signed_" + UUID.randomUUID(), null, false, UUID.randomUUID().toString(), file.getCodiceAzienda() + "t");
                        String signedUuid = uploadedFileInfo.getMongoUuid();
//                        String signedUuid = mongoDownload.put(signedFileIs, "signed_" + UUID.randomUUID(), "/temp", false);
                        logger.info(String.format("file %s written on temp repository", file.getFileId()));
                        file.setUuidFirmato(signedUuid);
                    } else {
                        String signedFileName;
                        String signedMimeType;
                        if (file.getFormatoFirma() == FirmaRemotaFile.FormatiFirma.PDF) {
                            signedFileName = file.getFileId() + ".pdf";
                            signedMimeType = PDF_CONTENT_TYPE;
                        } else {
                            signedFileName = file.getFileId() + ".p7m";
                            signedMimeType = P7M_CONTENT_TYPE;
                        }
                        logger.info(String.format("uploading file %s to Uploader...", file.getFileId()));
                        String res = firmaRemotaUtils.uploadToUploader(
                                configParams.getUploaderFileServletUrl(), signedFileIs, signedFileName, signedMimeType);
                        file.setUrlFirmato(res);
                    }
                    logger.info(String.format("file %s completed", file.getFileId()));
                }
                logger.info("all file signed");

                //IOUtils.write(bytes, new FileOutputStream("c:/temp/" + file.getFileId() + "_test.gdm.pdf"));
            }
        } catch (FirmaRemotaException ex) {
            logger.error("errore nella firma remota dei file: ", ex);
            throw ex;
        } catch (Exception ex) {
            logger.error("errore: ", ex);
            throw new FirmaRemotaException(ex);
        } finally {
            arubaSignService.closesession(identity, sessionId);
        }
        return firmaRemotaInformation;
    }

//    private CredentialsType getCredentialsType(ArubaUserInformation.ModalitaFirma modalitaFirma) {
//        CredentialsType credentialsType = null;
//        switch (modalitaFirma) {
//            case ARUBACALL:
//                credentialsType = CredentialsType.ARUBACALL;
//                break;
//        }
//        return credentialsType;
//    }
    @Override
    public void telefona(UserInformation userInformation) throws FirmaRemotaException {
        Auth identity = getIdentity((ArubaUserInformation) userInformation);
        it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.wsclient.ArssReturn credential = arubaSignService.sendCredential(identity, CredentialsType.ARUBACALL);
        //System.out.println(String.format("sendCredential %s %s %s", credential.getStatus(), credential.getDescription(), credential.getReturnCode()));
        if (credential == null) {
            throw new RemoteServiceException("remote call error, result is null");
        } else {
            throwCorrectException(credential.getStatus(), credential.getReturnCode(), credential.getDescription());
        }
    }

    private boolean isArssReturnSuccessful(it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.credentialproxy.ArssReturn resp) {
        return resp.getStatus().equals("OK") && resp.getReturnCode().equals("0000");
    }

    @Override
    public boolean existingCredential(UserInformation userInformation) throws RemoteServiceException, InvalidCredentialException, WrongTokenException {
        
        boolean credenzialiPresenti = false;
        ArubaUserInformation arubaUserInformation = (ArubaUserInformation) userInformation;
        it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.credentialproxy.ArssReturn resp = null;
        try {
            resp = this.credentialProxyService.existingCredential(
                    getCredentialProxyAdmin(),
                    arubaUserInformation.getUsername(),
                    arubaUserInformation.getDominioFirma());
        } catch (Throwable ex) {
            logger.error("Errore durante la chiamata: " + ex.getMessage(), ex);
            throw ex;
        }

        if (resp == null) {
            throw new RemoteServiceException("remote call error, result is null");
        } else if (isArssReturnSuccessful(resp)) {
            logger.info("Credential existence successfully verified");
            credenzialiPresenti = true;
        } else if (resp.getReturnCode().equals("0001")) {
            logger.warn("Attenzione: le credenziali richieste non esistono");
        } else {
            throwCorrectException(resp.getStatus(), resp.getReturnCode(), resp.getDescription());
        }

        return credenzialiPresenti;
    }

    @Override
    public boolean setCredential(UserInformation userInformation) throws FirmaRemotaException, InvalidCredentialException, RemoteServiceException {
        boolean insertionSuccessful = false;
        ArubaUserInformation arubaUserInformation = (ArubaUserInformation) userInformation;
        it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.credentialproxy.ArssReturn resp = this.credentialProxyService.setCredential(
                getCredentialProxyAdmin(),
                arubaUserInformation.getUsername(),
                arubaUserInformation.getDominioFirma(),
                arubaUserInformation.getPassword());
        if (resp == null) {
            throw new RemoteServiceException("remote call error, result is null");
        } else if (isArssReturnSuccessful(resp)) {
            logger.info("Credential insertion completed");
            logger.info("Verifico presenza credenziali inserite...");
            insertionSuccessful = existingCredential(userInformation);
            if (insertionSuccessful) {
                logger.info("Credential insertion successfully verified");
            } else {
                throw new RemoteServiceException("Error: insertion successful but retrieval failed");
            }
        } else {
            throwCorrectException(resp.getStatus(), resp.getReturnCode(), resp.getDescription());
        }
        return insertionSuccessful;
    }

    @Override
    public boolean removeCredential(UserInformation userInformation) throws FirmaRemotaException, InvalidCredentialException, RemoteServiceException {
        if (this.credentialProxyActive) {
            boolean isCredentialRemoved = false;
            ArubaUserInformation arubaUserInformation = (ArubaUserInformation) userInformation;
            it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.credentialproxy.ArssReturn resp = this.credentialProxyService.removeCredential(
                    getCredentialProxyAdmin(),
                    arubaUserInformation.getUsername(),
                    arubaUserInformation.getDominioFirma());
            if (resp == null) {
                throw new RemoteServiceException("remote call error, result is null");
            } else if (isArssReturnSuccessful(resp)) {
                logger.info("Credential removal accomplished");
                logger.info("Verifico la rimozione delle credenziali...");
                isCredentialRemoved = !existingCredential(userInformation);   // mi deve tornare false
                if (isCredentialRemoved) {
                    logger.info("Credential removal successfully verified");
                }
            } else if (resp.getReturnCode().equals("0001")) {
                logger.warn("Attenzione: le credenziali richieste non esistono");
            } else {
                throwCorrectException(resp.getStatus(), resp.getReturnCode(), resp.getDescription());
            }
            return isCredentialRemoved;
        } else {
            throw new RemoteServiceException("servizio di credentialProxy non attivo");
        }
    }

    public CredentialProxyAdmin getCredentialProxyAdmin() throws RemoteServiceException {
        if (this.credentialProxyActive) {
            String credentialProxyAdminUser = (String) credentialProxyAdminInfo.get("user");
            String credentialProxyAdminPassword = (String) credentialProxyAdminInfo.get("password");

            CredentialProxyAdmin credentialProxyAdmin = new CredentialProxyAdmin();
            credentialProxyAdmin.setAdminUser(credentialProxyAdminUser);
            credentialProxyAdmin.setAdminPwd(credentialProxyAdminPassword);
            return credentialProxyAdmin;
        }  else {
            throw new RemoteServiceException("servizio di credentialProxy non attivo");
        }
        
    }

    private Auth getIdentity(ArubaUserInformation userInformation) {
        Auth identity = new Auth();
        identity.setTypeHSM("COSIGN");
        logger.info("dominio: " + userInformation.getDominioFirma());
        if (!StringUtils.isEmpty(userInformation.getDominioFirma())) {
            identity.setTypeOtpAuth(userInformation.getDominioFirma());
        } else {
            identity.setTypeOtpAuth(dominioFirmaDefault);
        }
        identity.setUser(userInformation.getUsername());
        identity.setUserPWD(userInformation.getPassword());
        if (userInformation.getModalitaFirma() == ModalitaFirma.ARUBACALL) {
            identity.setExtAuthtype(CredentialsType.ARUBACALL);
        }
        identity.setOtpPwd(userInformation.getToken());

        return identity;
    }

    private PdfSignApparence getPdfSignApparence(File file, SignAppearance signAppearance) throws IOException {
        PdfSignFieldDescriptor pdfSignFieldDescriptor = PdfUtils.toPdfSignFieldDescriptor(new FileInputStream(file), signAppearance);

        PdfSignApparence pdfApparence = new PdfSignApparence();
        pdfApparence.setPage(pdfSignFieldDescriptor.getPage());
        pdfApparence.setLocation(pdfSignFieldDescriptor.getLocation());
        pdfApparence.setLeftx(pdfSignFieldDescriptor.getLowerLeftX());
        pdfApparence.setLefty(pdfSignFieldDescriptor.getLowerLeftY());
        pdfApparence.setRightx(pdfSignFieldDescriptor.getUpperRightX());
        pdfApparence.setRighty(pdfSignFieldDescriptor.getUpperRightY());

        return pdfApparence;
    }

    private SignRequestV2 getSignRequestV2(String sessionId, Auth identity, File file) {
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setSessionId(sessionId);
        signRequestV2.setIdentity(identity);
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);
        signRequestV2.setCertID("AS0");
        signRequestV2.setProfile(null);

        DataSource fds = new FileDataSource(file);
        DataHandler handler = new DataHandler(fds);
        signRequestV2.setStream(handler);

        return signRequestV2;
    }

    private InputStream sendSignRequest(String sessionId, Auth identity, FirmaRemotaFile file) throws MalformedURLException, IOException, FirmaRemotaException, TypeOfTransportNotImplemented_Exception, RemoteFileNotFoundException, RemoteServiceException, InvalidCredentialException, WrongTokenException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "firma_remota");
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }
        File tmpFileToSign = File.createTempFile("firma_remota_to_sing_tmp", null, tempDir);
//        File tempFileSigned = File.createTempFile("firma_remota_signed_tmp", null, tempDir);
        try {
            try {
                InputStream in = new URL(file.getUrl()).openStream();
                logger.info(String.format("saving file %s on temp file %s...", file.getFileId(), tmpFileToSign.getAbsolutePath()));
                Files.copy(in, Paths.get(tmpFileToSign.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
                logger.info("temfile saved");
            } catch (IOException e) {
                throw new RemoteFileNotFoundException("errore nel download del file da firmare probabilmente è scaduto il timeout", e);
            }
            logger.info(String.format("creating SignRequestV2 dor file %s", file.getFileId()));
            SignRequestV2 signRequestV2 = getSignRequestV2(sessionId, identity, tmpFileToSign);

            SignReturnV2 signReturn = null;
            switch (file.getFormatoFirma()) {
                case PDF:
                    PdfSignApparence signAppearance = null;
                    logger.info("signAppearence: " + file.getSignAppearance());
                    if (file.getSignAppearance() != null) {
                        logger.info(String.format("creating signApparence for file %s...", file.getFileId()));
                        signAppearance = getPdfSignApparence(tmpFileToSign, file.getSignAppearance());
                        logger.info("signApperence created");
                    }
                    logger.info(String.format("sending file %s for pdf sign...", file.getFileId()));
                    signReturn = arubaSignService.pdfsignatureV2(signRequestV2, signAppearance, PdfProfile.PADESBES, null, null);
                    logger.info(String.format("file %s signed", file.getFileId()));
                    break;
                case P7M:
                    if (file.getMimeType().equalsIgnoreCase(FirmaRemotaFile.P7M_MIMETYPE)) {
                        logger.info(String.format("sending file %s for p7m-mime sign...", file.getFileId()));
                        signReturn = arubaSignService.addpkcs7Sign(signRequestV2, false);
                        logger.info(String.format("file %s signed", file.getFileId()));
                    } else {
                        logger.info(String.format("sending file %s for p7m-detached sign...", file.getFileId()));
                        signReturn = arubaSignService.pkcs7SignV2(signRequestV2, false, false);
                        logger.info(String.format("file %s signed", file.getFileId()));
                    }
                    break;
                default:
                    throw new FirmaRemotaException(String.format("unexpected or invalid sign format %s", file.getFormatoFirma()));
            }

            if (signReturn == null) {
                throw new RemoteServiceException("remote sign error, result is null");
            } else {
                throwCorrectException(signReturn.getStatus(), signReturn.getReturnCode(), signReturn.getDescription());
            }

            InputStream signedFileIs = signReturn.getStream().getInputStream();

            return signedFileIs;
        } finally {
            if (tmpFileToSign.exists()) {
                tmpFileToSign.delete();
            }
        }

    }

    public void throwCorrectException(String status, String code, String description) throws InvalidCredentialException, WrongTokenException, RemoteServiceException {
        String descriptionString = (description != null && !description.isEmpty()) ? ": " + description : "";
        if ("KO".equalsIgnoreCase(status)) {
            switch (code) {
                case "0003":
                    throw new InvalidCredentialException("invalid credential" + descriptionString);
                case "0004":
                    throw new WrongTokenException("invalid or blocked token" + descriptionString);
                // TODO: inserire gli eventuali altri casi di errore
                default:
                    throw new RemoteServiceException(String.format("remote server error. code: %s - description: %s", code, description));
            }
        }
    }
}
