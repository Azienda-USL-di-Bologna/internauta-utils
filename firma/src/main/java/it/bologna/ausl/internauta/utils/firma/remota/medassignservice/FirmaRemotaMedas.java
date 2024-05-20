package it.bologna.ausl.internauta.utils.firma.remota.medassignservice;

import com.sun.xml.ws.developer.JAXWSProperties;
import it.bologna.ausl.internauta.utils.firma.configuration.FirmaHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.firma.data.exceptions.SignParamsException;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaFile;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaUserSign;
import it.bologna.ausl.internauta.utils.firma.data.remota.SignAppearance;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.medassignservice.MedasUserInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.medassignservice.MedasUserSign;
import it.bologna.ausl.internauta.utils.firma.remota.FirmaRemota;
import it.bologna.ausl.internauta.utils.firma.remota.InternalCredentialManager;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaHttpException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.InvalidCredentialException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteFileNotFoundException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteServiceException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.WrongTokenException;
import it.bologna.ausl.internauta.utils.firma.remota.utils.FirmaRemotaDownloaderUtils;
import it.bologna.ausl.internauta.utils.firma.remota.utils.pdf.PdfSignFieldDescriptor;
import it.bologna.ausl.internauta.utils.firma.remota.utils.pdf.PdfUtils;
import it.bologna.ausl.internauta.utils.firma.utils.ConfigParams;
import it.bologna.ausl.model.entities.firma.Configuration;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.BindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
public class FirmaRemotaMedas extends FirmaRemota {
    
    private static Logger logger = LoggerFactory.getLogger(FirmaRemotaMedas.class);
    
    private  Map<String, String> syncSignServiceAuth;
    private  Map<String, String> utilsServiceAuth;
    private ScrybaSignServerSync syncSignService;
    private ScrybaSignServerUtils utilsService;
    
    public FirmaRemotaMedas(ConfigParams configParams, FirmaRemotaDownloaderUtils firmaRemotaDownloaderUtils, Configuration configuration, InternalCredentialManager internalCredentialManager, FirmaHttpClientConfiguration firmaHttpClientConfiguration, String dominioFirmaDefault) throws FirmaRemotaConfigurationException {
        super(configParams, firmaRemotaDownloaderUtils, configuration, internalCredentialManager, firmaHttpClientConfiguration);
        
        Map<String,Object> firmaRemotaConfiguration = configuration.getParams();
        Map<String, Object> medasServiceConfiguration=  (Map<String, Object>) firmaRemotaConfiguration.get("MedasSignService");
        List<String> syncSignServiceEndPointUriList = (List<String>) medasServiceConfiguration.get("SyncSignServiceEndPointUriList");
        List<String> utilsServiceEndPointUriList = (List<String>) medasServiceConfiguration.get("UtilsServiceEndPointUriList");
        this.syncSignServiceAuth = (Map<String, String>) medasServiceConfiguration.get("SyncSignServiceAuth");
        this.utilsServiceAuth = (Map<String, String>) medasServiceConfiguration.get("UtilsServiceAuth");

        this.syncSignService = new SyncSignService().getSyncSignServicePort();
        this.utilsService = new UtilsService().getUtilsServicePort();

        // i server potrebbero essere più di uno, il parametro infatti è una lista.
        // TODO: temporaneamente prendiamo il primo url della lista (poi dovremmo fare un qualche meccanismo di fail-over)
        
        // impostazione dei parametri di connessione al servizio di syncSign
        String syncSignServiceEndPointURL = syncSignServiceEndPointUriList.get(0);
        BindingProvider bpSyncSignService = (BindingProvider) this.syncSignService;
        bpSyncSignService.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, syncSignServiceEndPointURL);
        bpSyncSignService.getRequestContext().put(JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE, 8192);
        if (this.syncSignServiceAuth != null && !this.syncSignServiceAuth.isEmpty()) {
            if (StringUtils.hasText(this.syncSignServiceAuth.get("username")) && StringUtils.hasText(this.utilsServiceAuth.get("password"))) {
                bpSyncSignService.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, this.syncSignServiceAuth.get("username"));
                bpSyncSignService.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, this.syncSignServiceAuth.get("password"));
            }
        }
        
        // impostazione dei parametri di connessione al servizio di Utils
        String utilsServiceEndPointURL = utilsServiceEndPointUriList.get(0);
        BindingProvider bpUtilsService = (BindingProvider) this.utilsService;
        bpUtilsService.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, utilsServiceEndPointURL);
//        bpUtilsService.getRequestContext().put(JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE, 8192);
        if (this.utilsServiceAuth != null && !this.utilsServiceAuth.isEmpty()) {
            if (StringUtils.hasText(this.utilsServiceAuth.get("username")) && StringUtils.hasText(this.utilsServiceAuth.get("password"))) {
                bpSyncSignService.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, this.utilsServiceAuth.get("username"));
                bpSyncSignService.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, this.utilsServiceAuth.get("password"));
            }
        }
    }
    
    private String getSignMode(FirmaRemotaFile.FormatiFirma formatoFirma) throws SignParamsException {
        String res;
        switch (formatoFirma) {
            case P7M:
                res = "CADES";
                break;
            case PDF:
                res = "PADES";
                break;
            default:
                throw new SignParamsException(String.format("firma in formato %s non implementata", formatoFirma));
        }
        return res;
    }

    private GetUserInfo4Resp getTypeGetUserInfo4Req(MedasUserInformation userInformation) {
        TypeGetUserInfo4Req typeGetUserInfo4Req = new TypeGetUserInfo4Req();
        if (StringUtils.hasText(userInformation.getUsername())) {
            typeGetUserInfo4Req.setUsername(userInformation.getUsername());
        } else if (StringUtils.hasText(userInformation.getCodiceFiscale())) {
            typeGetUserInfo4Req.setSsn(userInformation.getCodiceFiscale());
        }
        GetUserInfo4Req getUserInfo4Req = new GetUserInfo4Req();
        getUserInfo4Req.setGetUserInfo4Req(typeGetUserInfo4Req);
        GetUserInfo4Resp getUserInfoResp = this.utilsService.getUserInfo4(getUserInfo4Req);
        return getUserInfoResp;
    }
    
    private TypeCredentials getTypeCredentials(MedasUserInformation userInformation) {
        TypeCredentials typeCredentials = new TypeCredentials();
        if (StringUtils.hasText(userInformation.getOtp())) {
            typeCredentials.setOtp(userInformation.getOtp());
        }
        if (StringUtils.hasText(userInformation.getPassword())) {
            typeCredentials.setPassword(userInformation.getPassword());
        }
        
        return typeCredentials;
    }
    
    private TypeUser getTypeUser(MedasUserInformation userInformation) {
        TypeUser typeUser = new TypeUser();
        typeUser.setUsername(userInformation.getUsername());
        typeUser.setFirstName(userInformation.getNome());
        typeUser.setLastName(userInformation.getCognome());
        typeUser.setSsn(userInformation.getCodiceFiscale());
//        typeUser.setBirthDate(); // TODO: da chiedere, spero non sia obbligatoria
        return typeUser;
    }
    
    private TypeOpenSignSessionReq getTypeOpenSignSessionReq(MedasUserInformation userInformation, MedasUserSign medasUserSign) {
        TypeOpenSignSessionReq typeOpenSignSessionReq = new TypeOpenSignSessionReq();
        typeOpenSignSessionReq.setUser(getTypeUser(userInformation));
        typeOpenSignSessionReq.setCredentials(getTypeCredentials(userInformation));
        typeOpenSignSessionReq.setCertificateId(medasUserSign.getCertificateId());
        typeOpenSignSessionReq.setSignaturePowerId(medasUserSign.getSignPowerCode());
        return typeOpenSignSessionReq;
    }
    
    private void getTypeSignDocReq(MedasUserInformation userInformation, FirmaRemotaFile file, String sessionId) {
        TypeSignDocReq typeSignDocReq = new TypeSignDocReq();
        typeSignDocReq.setSessionId(sessionId);
        
        TypeDocument typeDocument = new TypeDocument();
        // typeDocument.setDocType(); // TODO: è obbligatorio? non so cosa metterci
        
        typeSignDocReq.setSignProperties(getTypeSignProperties(userInformation, file.getFormatoFirma(), file.getSignAppearance(), ));
        typeSignDocReq.setDocument(value);
    }
    
    private TypeSignProperties getTypeSignProperties(MedasUserInformation userInformation, FirmaRemotaFile.FormatiFirma formatoFirma, SignAppearance signAppearance, File file) throws SignParamsException, FileNotFoundException, IOException {
        TypeSignProperties typeSignProperties = new TypeSignProperties();
        typeSignProperties.setSignMode(getSignMode(formatoFirma));
        typeSignProperties.setParallel(true);
        //typeSignProperties.setProcessId(value); TODO: non so cosa metterci
        
        if (formatoFirma == FirmaRemotaFile.FormatiFirma.PDF) {
            TypeArssPadesProperties typeArssPadesProperties = new TypeArssPadesProperties();
            typeArssPadesProperties.setPdfProfile("PADESBES");
            TypePadesProperties typePadesProperties = new TypePadesProperties();

            // se la firma è visibile
            if (signAppearance != null) {
                PdfSignFieldDescriptor pdfSignFieldDescriptor;
                try (FileInputStream fis = new FileInputStream(file)) {
                    pdfSignFieldDescriptor = PdfUtils.toPdfSignFieldDescriptor(
                        fis,
                        signAppearance,
                        String.format("%s %s", userInformation.getCognome(), userInformation.getNome()),
                        null);
                }
                TypeArssPadesPropertiesApparence typeArssPadesPropertiesApparence = new TypeArssPadesPropertiesApparence();
                typeArssPadesPropertiesApparence.setText(pdfSignFieldDescriptor.getText());
                typeArssPadesProperties.setArssPadesPropertiesApparence(typeArssPadesPropertiesApparence);

                typePadesProperties.setPage(String.valueOf(pdfSignFieldDescriptor.getPage()));
                typePadesProperties.setLeftx(String.valueOf(pdfSignFieldDescriptor.getLowerLeftX()));
                typePadesProperties.setLefty(String.valueOf(pdfSignFieldDescriptor.getLowerLeftY()));
                typePadesProperties.setRightx(String.valueOf(pdfSignFieldDescriptor.getUpperRightX()));
                typePadesProperties.setRighty(String.valueOf(pdfSignFieldDescriptor.getUpperRightY()));
            }
            typePadesProperties.setArssPadesProperties(typeArssPadesProperties);
            typeSignProperties.setPadesProperties(typePadesProperties);
        }
        return typeSignProperties;
    }
    
    @Override
    public FirmaRemotaInformation firma(FirmaRemotaInformation firmaRemotaInformation, String codiceAzienda, HttpServletRequest request) throws FirmaRemotaHttpException, RemoteFileNotFoundException, WrongTokenException, InvalidCredentialException, RemoteServiceException {
        logger.info("in firma...");

        // prendo i file da firmare
        List<FirmaRemotaFile> files = firmaRemotaInformation.getFiles();
        
        if (files != null && !files.isEmpty()) {
            MedasUserInformation userInformation = (MedasUserInformation) firmaRemotaInformation.getUserInformation();

            OpenSignSessionReq openSignSessionReq = new OpenSignSessionReq();        
            openSignSessionReq.setOpenSignSessionReq(getTypeOpenSignSessionReq(userInformation, (MedasUserSign) firmaRemotaInformation.getUserSign()));        
            OpenSignSessionResp openSignSessionResp = this.syncSignService.openSignSession(openSignSessionReq);

            for (FirmaRemotaFile file : files) {
                SignDocReq signDocReq = new SignDocReq();

                signDocReq.setSignDocReq(value);

            }


            this.syncSignService.signDoc(signDocRequest);
        }
        
        // creo l'oggetto Auth necessario al WEB-SERVICE di aruba per identificare l'utente di firma
        Auth identity;
        try {
            identity = getIdentity((ArubaUserInformation) firmaRemotaInformation.getUserInformation());
        } catch (EncryptionException ex) {
            throw new FirmaRemotaHttpException("errore nel reperire le credenziali", ex);
        }

        String sessionId = null;
        try {
            // apertura sessione
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
        } catch (Exception ex) {
            logger.error("error opening session", ex);
            if (sessionId != null) { // se c'è un errore chiudo la sessione, se è aperta
                logger.info("closing session...");
                arubaSignService.closesession(identity, sessionId);
            }
            throw ex;
        }

        try {
            // ciclo i file da firmare e li firmo
            for (FirmaRemotaFile file : files) {
                logger.info(String.format("signing file %s ...", file.getFileId()));
                try (   // firma del file
                    InputStream signedFileIs = sendSignRequest(sessionId, identity, file)) {
                    logger.info(String.format("uploading fileId %s...", file.getFileId()));

                    // esegue l'upload (su mongo o usando l'uploader a seconda di file.getOutputType()) e setta il risultato sul campo adatto (file.setUuidFirmato() o file.setUrlFirmato())
                    super.upload(file, signedFileIs, codiceAzienda, request);
                }
                logger.info(String.format("file %s completed", file.getFileId()));
            }
            logger.info("all file signed");
        } catch (FirmaRemotaHttpException ex) {
            logger.error("errore nella firma remota dei file: ", ex);
            throw ex;
        } catch (Exception ex) {
            logger.error("errore: ", ex);
            throw new FirmaRemotaHttpException(ex);
        } finally {
            arubaSignService.closesession(identity, sessionId);
        }
        return firmaRemotaInformation;
    }

    @Override
    public void preAuthentication(UserInformation userInformation) throws FirmaRemotaHttpException, WrongTokenException, InvalidCredentialException, RemoteServiceException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    protected boolean externalExistingCredential(UserInformation userInformation, String hostId) throws FirmaRemotaHttpException, InvalidCredentialException, RemoteServiceException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    protected boolean externalSetCredential(UserInformation userInformation, String hostId) throws FirmaRemotaHttpException, InvalidCredentialException, RemoteServiceException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    protected boolean externalRemoveCredential(UserInformation userInformation, String hostId) throws FirmaRemotaHttpException, InvalidCredentialException, RemoteServiceException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public List<FirmaRemotaUserSign> getUserSigns(UserInformation userInformation) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}
