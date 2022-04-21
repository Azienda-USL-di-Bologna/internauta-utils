package it.bologna.ausl.internauta.utils.firma.remota.arubasignservice;


import it.bologna.ausl.internauta.utils.firma.data.remota.arubasignservice.ArubaUserInformation;
import com.sun.xml.ws.developer.JAXWSProperties;
import it.bologna.ausl.internauta.utils.firma.remota.configuration.ConfigParams;

import it.bologna.ausl.internauta.utils.firma.remota.FirmaRemota;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaFile;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.SignAppearance;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.arubasignservice.ArubaUserInformation.ModalitaFirma;
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
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaException;

import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.InvalidCredentialException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteFileNotFoundException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteServiceException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.WrongTokenException;
import it.bologna.ausl.internauta.utils.firma.remota.utils.FirmaRemotaDownloaderUtils;
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
 * @author gdm
 * 
 * Implementazione per la firma con provide ARUBA.
 */
public class FirmaRemotaAruba extends FirmaRemota {

    private static Logger logger = LoggerFactory.getLogger(FirmaRemotaAruba.class);

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String P7M_CONTENT_TYPE = "application/pkcs7-mime";

    private Boolean credentialProxyActive = false;
    private final Map<String, Object> credentialProxyAdminInfo;
    private final ArubaSignService arubaSignService;
    private final CredentialProxyService credentialProxyService;
    private final String dominioFirmaDefault;

    public FirmaRemotaAruba(String codiceAzienda, ConfigParams configParams, FirmaRemotaDownloaderUtils firmaRemotaDownloaderUtils, String dominioFirmaDefault) throws FirmaRemotaConfigurationException {
        super(codiceAzienda, configParams, firmaRemotaDownloaderUtils);
        
        // leggo le informazioni di configurazione della firma remota e del credential proxy
        Map<String, Map<String, Object>> firmaRemotaConfiguration = configParams.getFirmaRemotaConfiguration(codiceAzienda);
        Map<String, Object> arubaServiceConfiguration = firmaRemotaConfiguration.get("ArubaSignService");
        List<String> signServiceEndPointUri = (List<String>) arubaServiceConfiguration.get("ArubaSignServiceEndPointUri");
        List<String> credentialProxyEndPointUriList = (List<String>) arubaServiceConfiguration.get("ArubaCredentialProxyEndPointUriList");
        this.credentialProxyAdminInfo = (Map<String, Object>) arubaServiceConfiguration.get("ArubaCredentialProxyAdminInfo");
        this.credentialProxyActive = (Boolean) credentialProxyAdminInfo.get("active");
        
        this.dominioFirmaDefault = dominioFirmaDefault;

        ArubaSignServiceService arubaSignServiceService = new ArubaSignServiceService();
        this.arubaSignService = arubaSignServiceService.getArubaSignServicePort();

        CredentialProxyServiceService credentialProxyServiceService = new CredentialProxyServiceService();
        this.credentialProxyService = credentialProxyServiceService.getCredentialProxyServicePort();

        // i server di ARUBA potrebbero essere più di uno, il parametro infatti è una lista.
        // TODO: temporaneamente prendiamo il primo url della lista (poi dovremmo fare un qualche meccanismo di fail-over)
        
        // impostazione dei parametri di connessione al servizio di firma remota
        String arubaSignServiceEndpointURL = signServiceEndPointUri.get(0);
        BindingProvider bpArubaSignService = (BindingProvider) this.arubaSignService;
        bpArubaSignService.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, arubaSignServiceEndpointURL);
        bpArubaSignService.getRequestContext().put(JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE, 8192);

        // impostazione dei parametri di connessione al servizio di credential proxy
        String arubaCredentialProxyEndpointURL = credentialProxyEndPointUriList.get(0);
        BindingProvider bpCredentialProxy = (BindingProvider) this.credentialProxyService;
        bpCredentialProxy.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, arubaCredentialProxyEndpointURL);
//        bpCredentialProxy.getRequestContext().put(JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE, 8192);
    }

    /**
     * Implementazione ARUBA della firma.
     * @param firmaRemotaInformation
     * @return
     * @throws FirmaRemotaException 
     */
    @Override
    public FirmaRemotaInformation firma(FirmaRemotaInformation firmaRemotaInformation) throws FirmaRemotaException {
        logger.info("in firma...");

        // prendo i file da firmare
        List<FirmaRemotaFile> files = firmaRemotaInformation.getFiles();

        // creo l'oggetto Auth necessario al WEB-SERVICE di aruba per identificare l'utente di firma
        Auth identity = getIdentity((ArubaUserInformation) firmaRemotaInformation.getUserInformation());

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
            throw new FirmaRemotaException("error opening session", ex);
        }

        try {
            // ciclo i file da firmare e li firmo
            for (FirmaRemotaFile file : files) {
                logger.info(String.format("signing file %s ...", file.getFileId()));
                try (   // firma del file
                    InputStream signedFileIs = sendSignRequest(sessionId, identity, file)) {
                    logger.info(String.format("uploading fileId %s...", file.getFileId()));

                    // esegue l'upload (su mongo o usando l'uploader a seconda di file.getOutputType()) e setta il risultato sul campo adatto (file.setUuidFirmato() o file.setUrlFirmato())
                    super.upload(file, signedFileIs);
                }
                logger.info(String.format("file %s completed", file.getFileId()));
            }
            logger.info("all file signed");
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

    /**
     * Questo medoto fa partire la telefonata o l'sms dai quali reperire il codice OTP identificato dal parametro userInformation
     * @param userInformation
     * @throws FirmaRemotaException 
     */
    @Override
    public void preAuthentication(UserInformation userInformation) throws FirmaRemotaException {
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

    /**
     * Controlla se esistono le credenziali sul CredentialProxy per l'utente passato
     * @param userInformation
     * @return "true" se esistono le credenziali sul CredentialProxy per l'utente passato, false altrimenti
     * @throws RemoteServiceException
     * @throws InvalidCredentialException
     * @throws WrongTokenException 
     */
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

    /**
     * Setta le credenziali dell'utente passato sul CredentialProxy
     * @param userInformation
     * @return "true" se le credenziali sono state settate correttamente, "false" altrimenti
     * @throws FirmaRemotaException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
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
            String[] statusSplitted = resp.getStatus().split("-", 2);
            throwCorrectException(statusSplitted[0].trim(), statusSplitted[1].trim(), resp.getDescription());
        }
        return insertionSuccessful;
    }

    /**
     * Rimuove le credenziali dell'utente passato dal CredentialProxy
     * @param userInformation
     * @return "true" se le credenziali sono state rimosse correttamente, "false" altrimenti
     * @throws FirmaRemotaException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
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

    /**
     * Crea l'oggetto CredentialProxyAdmin, che identifica l'amministratore del servizio di CredentialProxy. L'oggetto è necessario per l'utilizzo del servizio.
     * I dati sono reperiti dai parametri aziendali.
     * @return
     * @throws RemoteServiceException 
     */
    private CredentialProxyAdmin getCredentialProxyAdmin() throws RemoteServiceException {
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

    /**
     * Crea l'oggetto AUTH a partire dall'implementazione di UserInformation
     * @param userInformation
     * @return 
     */
    private Auth getIdentity(ArubaUserInformation userInformation) {
        Auth identity = new Auth();
        identity.setTypeHSM("COSIGN");
        
        // impostazione del dominio. Il dominio è un parametro di configurazione dell'utente ed è fornito da ARUBA in fase di attivazioen dell'utenza
        logger.info("dominio: " + userInformation.getDominioFirma());     
        if (StringUtils.hasText(userInformation.getDominioFirma())) {
            identity.setTypeOtpAuth(userInformation.getDominioFirma());
        } else {
            identity.setTypeOtpAuth(dominioFirmaDefault);
        }
        identity.setUser(userInformation.getUsername());
        identity.setUserPWD(userInformation.getPassword());
        
        // questo serve nel caso in cui si vuole effettuare la pre-autenthication
        if (userInformation.getModalitaFirma() == ModalitaFirma.ARUBACALL) {
            identity.setExtAuthtype(CredentialsType.ARUBACALL);
        }
        identity.setOtpPwd(userInformation.getToken());

        return identity;
    }

    /**
     * Crea l'oggetto che descrive l'aspetto del campo firma visibile sui pdf
     * @param file
     * @param signAppearance
     * @return
     * @throws IOException 
     */
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

    /**
     * Genera l'oggetto contente i parametri per la firma, necessario al WEB-SERVICE ARUBA
     * @param sessionId
     * @param identity
     * @param file
     * @return 
     */
    private SignRequestV2 getSignRequestV2(String sessionId, Auth identity, File file) {
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setSessionId(sessionId);
        signRequestV2.setIdentity(identity);
        signRequestV2.setTransport(TypeTransport.STREAM); // settando Stream è possibile passare il file in InputStream
        signRequestV2.setRequiredmark(false);
        signRequestV2.setCertID("AS0");
        signRequestV2.setProfile(null);

        // settaggio del file da firmare in modalità stream
        // devo necessariamente parire dal file fisico per creare lo stream, altrimenti non funziona (il perché non lo so, ma dipende dall'implementazione lato ARUBA)
        DataSource fds = new FileDataSource(file);
        DataHandler handler = new DataHandler(fds);
        signRequestV2.setStream(handler);

        return signRequestV2;
    }

    /**
     * esegue la chiamata per la firma al WEB-SERVICE di ARUBA
     * @param sessionId
     * @param identity
     * @param file
     * @return
     * @throws MalformedURLException
     * @throws IOException
     * @throws FirmaRemotaException
     * @throws TypeOfTransportNotImplemented_Exception
     * @throws RemoteFileNotFoundException
     * @throws RemoteServiceException
     * @throws InvalidCredentialException
     * @throws WrongTokenException 
     */
    private InputStream sendSignRequest(String sessionId, Auth identity, FirmaRemotaFile file) throws MalformedURLException, IOException, FirmaRemotaException, TypeOfTransportNotImplemented_Exception, RemoteFileNotFoundException, RemoteServiceException, InvalidCredentialException, WrongTokenException {
        
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "firma_remota");
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }
        File tmpFileToSign = File.createTempFile("firma_remota_to_sing_tmp", null, tempDir);
        
        try {
            // per prima cosa scarica il file da firmare nella cartella temporanea scaricandolo dall'url
            try {
                InputStream in = new URL(file.getUrl()).openStream();
                logger.info(String.format("saving file %s on temp file %s...", file.getFileId(), tmpFileToSign.getAbsolutePath()));
                Files.copy(in, Paths.get(tmpFileToSign.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
                logger.info("temfile saved");
            } catch (IOException e) {
                throw new RemoteFileNotFoundException("errore nel download del file da firmare probabilmente è scaduto il timeout", e);
            }
            // genero l'oggetto dei parametri per eseguire la chiamata di firma
            logger.info(String.format("creating SignRequestV2 dor file %s", file.getFileId()));
            SignRequestV2 signRequestV2 = getSignRequestV2(sessionId, identity, tmpFileToSign);

            SignReturnV2 signReturn = null;
            switch (file.getFormatoFirma()) {
                case PDF:
                    PdfSignApparence signAppearance = null;
                    logger.info("signAppearence: " + file.getSignAppearance());
                    
                    // se firmo in pdf creo la SignAppearance, se voluta
                    if (file.getSignAppearance() != null) {
                        logger.info(String.format("creating signApparence for file %s...", file.getFileId()));
                        signAppearance = getPdfSignApparence(tmpFileToSign, file.getSignAppearance());
                        logger.info("signApperence created");
                    }
                    logger.info(String.format("sending file %s for pdf sign...", file.getFileId()));
                    
                    // firma il file in pdf
                    signReturn = arubaSignService.pdfsignatureV2(signRequestV2, signAppearance, PdfProfile.PADESBES, null, null);
                    logger.info(String.format("file %s signed", file.getFileId()));
                    break;
                case P7M:
                    /* se voglio firmare in p7m ci sono 2 modalità possibili: 
                     * p7m-mime, che crea un file p7m contentente sia il file, che la firma)
                     * p7m-detached cre crea un file p7m con solo la firma.
                     * A seconda della scelta chiamo la funzione adatta
                    */
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
                // richiamo la funzione che in base allo status della firma lancia le eventuali eccezioni specifiche.
                // Se la firma è andata a buon fine, la funzione non fa nulla
                throwCorrectException(signReturn.getStatus(), signReturn.getReturnCode(), signReturn.getDescription());
            }

            // reperisco lo stream del file firamto e lo ritorno
            InputStream signedFileIs = signReturn.getStream().getInputStream();
            return signedFileIs;
        } finally {
            // una volta firmato, elimino il file non firmato
            if (tmpFileToSign.exists()) {
                tmpFileToSign.delete();
            }
        }

    }

    /**
     * Questa funzione, se lo status della firma (passato in input) identifica uno stato di errore, lancia la corretta eccezione in base al code (sempre passato in input)
     * Se lo status non è di errore, allora non fa nulla.
     * 
     * @param status status della firma tornato da ARUBA
     * @param code codice dell'operazione tornato da ARUBA
     * @param description descrizione dello status dell'operazione (tornato da ARUBA)
     * @throws InvalidCredentialException
     * @throws WrongTokenException
     * @throws RemoteServiceException 
     */
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
