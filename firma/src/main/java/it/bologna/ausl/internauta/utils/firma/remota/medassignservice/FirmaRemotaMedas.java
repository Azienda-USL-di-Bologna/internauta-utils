package it.bologna.ausl.internauta.utils.firma.remota.medassignservice;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.BaseEncoding;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.BindingProvider;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
public class FirmaRemotaMedas extends FirmaRemota {
    
    private static Logger logger = LoggerFactory.getLogger(FirmaRemotaMedas.class);
    
    private final Map<String, String> syncSignServiceAuth;
    private final Map<String, String> utilsServiceAuth;
    private final Map<String, String> SFTPConnectionParams;
    private final ScrybaSignServerSync syncSignService;
    private final ScrybaSignServerUtils utilsService;
    private final List<Map<String, Object>> profiles;
    
    private final ThreadLocal<String> largeFileHash = new ThreadLocal<>();
    private final ThreadLocal<Triple<Session, Channel, ChannelSftp>> sftpConnection = new ThreadLocal<>();
    
    public FirmaRemotaMedas(ConfigParams configParams, FirmaRemotaDownloaderUtils firmaRemotaDownloaderUtils, Configuration configuration, InternalCredentialManager internalCredentialManager, FirmaHttpClientConfiguration firmaHttpClientConfiguration) throws FirmaRemotaConfigurationException {
        super(configParams, firmaRemotaDownloaderUtils, configuration, internalCredentialManager, firmaHttpClientConfiguration);
        
        // lista di file grossi da eliminare al termine della firma
//        this.largeFilesToDelete.set(new ArrayList<>()); // inizializzazione della lista di file grossi da eliminare al termine della firma

        Map<String,Object> firmaRemotaConfiguration = configuration.getParams();
        Map<String, Object> medasServiceConfiguration = (Map<String, Object>) firmaRemotaConfiguration.get("MedasSignService");
        List<String> syncSignServiceEndPointUriList = (List<String>) medasServiceConfiguration.get("SyncSignServiceEndPointUriList");
        List<String> utilsServiceEndPointUriList = (List<String>) medasServiceConfiguration.get("UtilsServiceEndPointUriList");
        this.syncSignServiceAuth = (Map<String, String>) medasServiceConfiguration.get("SyncSignServiceAuth");
        this.utilsServiceAuth = (Map<String, String>) medasServiceConfiguration.get("UtilsServiceAuth");
        this.SFTPConnectionParams = (Map<String, String>) medasServiceConfiguration.get("SFTPConnectionParams");
        this.profiles = (List<Map<String, Object>>) medasServiceConfiguration.get("Profiles");
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
    
    private ChannelSftp connectOnSFTP() throws JSchException {
        JSch jSch = new JSch();
        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
//        jSch.addIdentity(PRIVATE_KEY_FILE);
        System.out.println("Private Key Added.");
//        session = jSch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
        System.out.println("Session created.");

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        channel = session.openChannel("sftp");
        channelSftp.connect();
        sftpConnection.set(Triple.of(session, channel, channelSftp));
        return channelSftp;
    }
    
    private void disconnectFromSFTP() {
        Triple<Session, Channel, ChannelSftp> sftpConnectionTriple = sftpConnection.get();
        if (sftpConnectionTriple != null) {
            if (sftpConnectionTriple.getLeft() != null) {
                try {
                    sftpConnectionTriple.getLeft().disconnect();
                } catch (Exception ex) {
                    logger.error("erron on disconneting from SFTP session", ex);
                }
            }
            if (sftpConnectionTriple.getMiddle()!= null) {
                try {
                    sftpConnectionTriple.getMiddle().disconnect();
                } catch (Exception ex) {
                    logger.error("erron on disconneting from SFTP Channel", ex);
                }
            }
            if (sftpConnectionTriple.getRight()!= null) {
                try {
                    sftpConnectionTriple.getRight().disconnect();
                } catch (Exception ex) {
                    logger.error("erron on disconneting from SFTP ChannelSFTP", ex);
                }
            }
        }
    }
    
    private String manageFileOnSFTP(File file, String cf) throws RemoteServiceException  {
        ChannelSftp sftpChannel;
        try {
            sftpChannel = connectOnSFTP();
        } catch (Exception ex) {
            String errorMessage = "error on connecton to sftp";
            logger.error(errorMessage, ex);
            throw new RemoteServiceException(errorMessage, ex);
        }
        String sha256hex;
        try (FileInputStream fis = new FileInputStream(file)) {
            try(HashingInputStream his = new HashingInputStream(Hashing.sha256(), fis)) {
                byte[] buffer= new byte[8192];
                int count;
                while((count = his.read(buffer)) > 0){} // leggo i byte per caloclare l'hash
                sha256hex = his.hash().toString();
            }
        } catch (Exception ex) {
            String errorMessage = "error on getting hash";
            logger.error(errorMessage, ex);
            throw new RemoteServiceException(errorMessage, ex);
        }
        String toSignFolder = this.SFTPConnectionParams.get("toSingFolder");
        String randomFileName = UUID.randomUUID().toString();
        String sftpFilePathRandomName = String.format("%s/%s", toSignFolder, randomFileName);
        try {
            sftpChannel.put(file.getAbsolutePath(), sftpFilePathRandomName);
        } catch (Exception ex) {
            String errorMessage = "error on uploading file to sftp";
            logger.error(errorMessage, ex);
            removeFilePathFromSFTP(sftpFilePathRandomName);
            throw new RemoteServiceException(errorMessage, ex);
        }
        
        String sftpFileName =String.format("%s_%s", cf, sha256hex);
        String sftpFilePath = String.format("%s/%s", toSignFolder, sftpFileName);
        try {
            sftpChannel.rename(
                sftpFilePathRandomName, 
                sftpFilePath);
        } catch (Exception ex) {
            String errorMessage = "error on renaming file on sftp";
            logger.error(errorMessage, ex);
            removeFilePathFromSFTP(sftpFilePath);
            throw new RemoteServiceException(errorMessage, ex);
        }
        
        // aggiungere il file alla lista largeFilesToDelete
        this.largeFileHash.set(sftpFileName);
        return sha256hex;
    }
    
    private InputStream retreiveSignedFileFromSFTP(String sftpPath) throws RemoteServiceException {
        try {
            ChannelSftp sftpChannel = sftpConnection.get().getRight();
            String sftpSignedFolder = SFTPConnectionParams.get("signedFolder");
            return sftpChannel.get(String.format("%s/%s", sftpSignedFolder, sftpPath));
        } catch (Exception ex) {
            String errorMessage = String.format("error on retrieving file %s on sftp", sftpPath);
            logger.error(errorMessage, ex);
            throw new RemoteServiceException(errorMessage, ex);
        }
    }
    
    private void removeFilePathFromSFTP(String sftpPath) {
        try {
            logger.info(String.format("removing file %s from sftp...", sftpPath));
            ChannelSftp sftpChannel = sftpConnection.get().getRight();
            sftpChannel.rm(sftpPath);
        } catch (Exception ex) {
            String errorMessage = "error on deleting file to sftp";
            logger.error(errorMessage, ex);
        }
    }
    
    private void removeSignedFileFromSFTP(String filename) {
        String signedFolder = SFTPConnectionParams.get("signedFolder");
        removeFilePathFromSFTP(String.format("%s/%s", signedFolder, filename));
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

    private TypeGetUserInfo4Req getTypeGetUserInfo4Req(MedasUserInformation userInformation, String processId, List<String> docTypes) {
        TypeGetUserInfo4Req typeGetUserInfo4Req = new TypeGetUserInfo4Req();
        if (StringUtils.hasText(userInformation.getUsername())) {
            typeGetUserInfo4Req.setUsername(userInformation.getUsername());
        } else if (StringUtils.hasText(userInformation.getCodiceFiscale())) {
            typeGetUserInfo4Req.setSsn(userInformation.getCodiceFiscale());
        }
        if (StringUtils.hasText(processId)) {
            typeGetUserInfo4Req.setProcessId(processId);
        }
        if (docTypes != null && docTypes.isEmpty()) {
            TypeDocTypeList typeDocTypeList = new TypeDocTypeList();
            typeDocTypeList.getDocType().addAll(docTypes);
            typeGetUserInfo4Req.setDocTypeList(typeDocTypeList);
        }
        return typeGetUserInfo4Req;
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
    
    private TypeDespatchOtpReq getTypeDespatchOtpReq(MedasUserInformation userInformation, MedasUserSign medasUserSign) {
        TypeDespatchOtpReq typeDespatchOtpReq = new TypeDespatchOtpReq();
        typeDespatchOtpReq.setCertificateId(medasUserSign.getCertificateId());
        typeDespatchOtpReq.setUser(getTypeUser(userInformation));
        typeDespatchOtpReq.setCredentials(getTypeCredentials(userInformation));
        return typeDespatchOtpReq;
    }
    
    private TypeOpenSignSessionReq getTypeOpenSignSessionReq(MedasUserInformation userInformation, MedasUserSign medasUserSign) {
        TypeOpenSignSessionReq typeOpenSignSessionReq = new TypeOpenSignSessionReq();
        typeOpenSignSessionReq.setUser(getTypeUser(userInformation));
        typeOpenSignSessionReq.setCredentials(getTypeCredentials(userInformation));
        typeOpenSignSessionReq.setCertificateId(medasUserSign.getCertificateId());
        typeOpenSignSessionReq.setSignaturePowerId(medasUserSign.getSignPowerCode());
        return typeOpenSignSessionReq;
    }
    
    private TypeCloseSignSessionReq getTypeCloseSignSessionReq(String sessionId) {
        TypeCloseSignSessionReq typeCloseSignSessionReq = new TypeCloseSignSessionReq();
        typeCloseSignSessionReq.setSessionId(sessionId);
        return typeCloseSignSessionReq;
    }
    
    private TypeSignDocReq getTypeSignDocReq(MedasUserInformation userInformation, FirmaRemotaFile file, String sessionId) throws SignParamsException, IOException, RemoteFileNotFoundException, RemoteServiceException {
        TypeSignDocReq typeSignDocReq = new TypeSignDocReq();
        
        String processId = (String) profiles.get(0).get("processId");
        List<String> docTypes = (List<String>) profiles.get(0).get("docTypes");
        
//        MedasUserSign userSign = (MedasUserSign) userInformation.getUserSign();
        
        typeSignDocReq.setSessionId(sessionId);
        
        File tmpFileToSign = createTempFile(file);
        
        typeSignDocReq.setSignProperties(getTypeSignProperties(userInformation, file.getFormatoFirma(), file.getSignAppearance(), tmpFileToSign));
        
        TypeDocument typeDocument = new TypeDocument();
        typeDocument.setDocType(docTypes.get(0));
        
        String fileBase64;
        long fileSize = tmpFileToSign.length();
        boolean largeFile = fileSize > 10 * 1000000;
        logger.info(String.format("file size: %s bytes, largeFile: %s", fileSize, largeFile));
        if (!largeFile) {
            logger.info(String.format("file not large"));
            byte[] fileBytes = FileCopyUtils.copyToByteArray(tmpFileToSign);
            fileBase64 = BaseEncoding.base64().encode(fileBytes);
        } else {
            String hashFile = manageFileOnSFTP(tmpFileToSign, userInformation.getCodiceFiscale());
            typeDocument.setDocHash(hashFile);
            fileBase64 = "RklMRQ==";
        }
        typeDocument.setDocBin(fileBase64);
        
        typeSignDocReq.setDocument(typeDocument);
        
        return typeSignDocReq;
    }
    
    private TypeSignProperties getTypeSignProperties(MedasUserInformation userInformation, FirmaRemotaFile.FormatiFirma formatoFirma, SignAppearance signAppearance, File file) throws SignParamsException, FileNotFoundException, IOException {
        TypeSignProperties typeSignProperties = new TypeSignProperties();
        MedasUserSign userSign = (MedasUserSign) userInformation.getUserSign();
        
        typeSignProperties.setSignMode(getSignMode(formatoFirma));
        typeSignProperties.setParallel(true);
        typeSignProperties.setProcessId(userSign.getProcessId());
        
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
                typeArssPadesPropertiesApparence.setImage(""); // bisogna settare vuoto sennò dà errore
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
    
    private File createTempFile(FirmaRemotaFile file) throws IOException, RemoteFileNotFoundException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "firma_remota");
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }
        File tmpFileToSign = File.createTempFile("firma_remota_medas_to_sing_tmp", null, tempDir);

        // per prima cosa scarica il file da firmare nella cartella temporanea scaricandolo dall'url
        try {
            InputStream in = new URL(file.getUrl()).openStream();
            logger.info(String.format("saving file %s on temp file %s...", file.getFileId(), tmpFileToSign.getAbsolutePath()));
            Files.copy(in, Paths.get(tmpFileToSign.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
            logger.info("temfile saved");
        } catch (IOException e) {
            throw new RemoteFileNotFoundException("errore nel download del file da firmare probabilmente è scaduto il timeout", e);
        }
        
        return tmpFileToSign;
    }
    
    @Override
    public FirmaRemotaInformation firma(FirmaRemotaInformation firmaRemotaInformation, String codiceAzienda, HttpServletRequest request) throws FirmaRemotaHttpException, RemoteFileNotFoundException, WrongTokenException, InvalidCredentialException, RemoteServiceException {
        logger.info("in firma...");

        // prendo i file da firmare
        List<FirmaRemotaFile> files = firmaRemotaInformation.getFiles();
        
        if (files != null && !files.isEmpty()) {
            MedasUserInformation userInformation = (MedasUserInformation) firmaRemotaInformation.getUserInformation();
            OpenSignSessionReq openSignSessionReq;
            try {
                openSignSessionReq = new OpenSignSessionReq();        
                openSignSessionReq.setOpenSignSessionReq(getTypeOpenSignSessionReq(userInformation, (MedasUserSign) userInformation.getUserSign()));        
            } catch (Exception ex) {
                String errorMessage = "remote server error. Error opening session";
                logger.error(errorMessage, ex);
                throw new RemoteServiceException(errorMessage, ex);
            }
            OpenSignSessionResp openSignSessionResp = this.syncSignService.openSignSession(openSignSessionReq);
            throwCorrectException(openSignSessionResp.getOpenSignSessionResp().getMessage());
            String sessionId = openSignSessionResp.getOpenSignSessionResp().getSessionId();
            
            for (FirmaRemotaFile file : files) {
                TypeSignDocResp typeSignDocResp;
                try {
                    SignDocReq signDocReq = new SignDocReq();
                    signDocReq.setSignDocReq(getTypeSignDocReq(userInformation, file, sessionId));
                    SignDocResp signDocResp = this.syncSignService.signDoc(signDocReq);
                    typeSignDocResp = signDocResp.getSignDocResp();
                } catch (Exception ex) {
                    String errorMessage = String.format("remote server error. Error sign fileId %s", file.getFileId());
                    logger.error(errorMessage, ex);
                    throw new RemoteServiceException(errorMessage, ex);
                }
                throwCorrectException(typeSignDocResp.getMessage());
                InputStream signedFileIs;
                if (StringUtils.hasText(largeFileHash.get())) { // TODO: controllare anche nella response se si capisce se si è firmato un file grosso o meno
                    try {
                        signedFileIs = retreiveSignedFileFromSFTP(largeFileHash.get());
                    } catch (RemoteServiceException ex) {
                        removeSignedFileFromSFTP(largeFileHash.get());
                        largeFileHash.remove();
                        throw ex;
                    }
                } else {
                    signedFileIs = BaseEncoding.base64().decodingStream(new StringReader(typeSignDocResp.getSignedDocBin()));
                }
                try {
                    super.upload(file, signedFileIs, codiceAzienda, request);
                } catch (MinIOWrapperException ex) {
                    String errorMessage = String.format("remote server error. Error uploading fileId %s on repository", file.getFileId());
                    logger.error(errorMessage, ex);
                    throw new RemoteServiceException(errorMessage, ex);
                } finally {
                    if (StringUtils.hasText(largeFileHash.get())) {
                        removeSignedFileFromSFTP(largeFileHash.get());
                        largeFileHash.remove();
                    }
                }
            }
            CloseSignSessionReq closeSignSessionReq = new CloseSignSessionReq();
            closeSignSessionReq.setCloseSignSessionReq(getTypeCloseSignSessionReq(sessionId));
            this.syncSignService.closeSignSession(closeSignSessionReq);
            
            // probabilmente non serve
            if (StringUtils.hasText(largeFileHash.get())) {
                removeSignedFileFromSFTP(largeFileHash.get());
                largeFileHash.remove();
            }
        }
        return firmaRemotaInformation;
    }

    @Override
    public void preAuthentication(UserInformation userInformation) throws FirmaRemotaHttpException, WrongTokenException, InvalidCredentialException, RemoteServiceException {
        MedasUserInformation medasUserInformation = (MedasUserInformation) userInformation;
        TypeDespatchOtpResp typeDespatchOtpResp;
        try {
            DespatchOtpReq despatchOtpReq  = new DespatchOtpReq();
            despatchOtpReq.setDespatchOtpReq(getTypeDespatchOtpReq(medasUserInformation, (MedasUserSign) medasUserInformation.getUserSign()));
            DespatchOtpResp despatchOtpResp = utilsService.despatchOtp(despatchOtpReq);
            typeDespatchOtpResp = despatchOtpResp.getDespatchOtpResp();
        } catch (Exception ex) {
            String errorMessage = String.format("remote server error. Error despatching otp for user %s", medasUserInformation.getCodiceFiscale());
            logger.error(errorMessage, ex);
            throw new RemoteServiceException(errorMessage, ex);
        }
        throwCorrectException(typeDespatchOtpResp.getMessage());
    }

       /**
     * il salvataggio delle credenziali non è supportato da Medas, per cui torniamo sempre false
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
     * il salvataggio delle credenziali non è supportato da Medas, per cui torniamo sempre false
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
     * il salvataggio delle credenziali non è supportato da Medas, per cui torniamo sempre false
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

    /**
     * Rileva i poteri di firma e crea la lista di FirmaRemotaUserSign da mostrare all'utente perché selezioni quello voluto
     * @param userInformation
     * @return 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaHttpException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.InvalidCredentialException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteServiceException 
     */
    @Override
    public List<FirmaRemotaUserSign> getUserSigns(UserInformation userInformation) throws FirmaRemotaHttpException, InvalidCredentialException, RemoteServiceException {
        //TODO: va fatta chiamata multipla per gestare procId e DocType multipli
        TypeGetUserInfo4Resp typeGetUserInfo4Resp;
        List<FirmaRemotaUserSign> res = new ArrayList<>();
        MedasUserInformation medasUserInformation = (MedasUserInformation) userInformation;
        String processId = (String) profiles.get(0).get("processId");
        List<String> docTypes = (List<String>) profiles.get(0).get("docTypes");
        try {
            GetUserInfo4Req getUserInfo4Req = new GetUserInfo4Req();
            getUserInfo4Req.setGetUserInfo4Req(
                getTypeGetUserInfo4Req(
                    medasUserInformation, 
                    (String) profiles.get(0).get("processId"), 
                    (List<String>) profiles.get(0).get("docTypes")
                )
            );
            GetUserInfo4Resp getUserInfoResp = this.utilsService.getUserInfo4(getUserInfo4Req);
            typeGetUserInfo4Resp = getUserInfoResp.getGetUserInfo4Resp();
        } catch (Exception ex) {
            String errorMessage = String.format("remote server error. Error despatching otp for user %s", medasUserInformation.getCodiceFiscale());
            logger.error(errorMessage, ex);
            throw new RemoteServiceException(errorMessage, ex);
        }
        
        TypeMessageDesc resMessage = typeGetUserInfo4Resp.getMessage();
        if (resMessage != null) {
            if (resMessage.getCode().intValue() > 0) {
                String errorMessage = String.format("remote server error. code: %s - message: %s", resMessage.getCode().intValue(), resMessage.getDescription());
                logger.error(errorMessage);
                throw new RemoteServiceException(errorMessage);
            } else {
                TypeSigningUser4 signerUser = typeGetUserInfo4Resp.getSignerUser();
                List<TypeSignaturePower3> signaturePowerList = signerUser.getSignaturePowerList().getSignaturePower();
                for (TypeSignaturePower3 signaturePower : signaturePowerList) {
                    List<TypeCert4> certificateList = signaturePower.getCertificateList().getCertificate();
                    for (TypeCert4 certificate : certificateList) {
                        MedasUserSign userSign = new MedasUserSign();
                        userSign.setActive(signaturePower.isActive());
                        userSign.setCertificateId(certificate.getId());
                        userSign.setDescription(certificate.getDescription());
                        userSign.setProcessId(processId); // boh
                        userSign.setDocTypes(docTypes); // boh
                        userSign.setOtpType(toOTPTypeList(certificate.getOTPtypeList()));
                        userSign.setSignPowerCode(signaturePower.getCode());
                        userSign.setSignType(MedasUserSign.SignType.valueOf(signaturePower.getSignType()));
                        res.add(userSign);
                    }
                }
                
                // signerUser.getSignaturePowerList().getSignaturePower().get(0).getProfile().getProcessList().getProcess().get(0);
            }
        } else {
            String errorMessage = "il resultMessage è null, questo non dovrebbe succedere";
            throw new RemoteServiceException(errorMessage);
        }
        return res;
    }
    
    private List<MedasUserSign.OTPType> toOTPTypeList(TypeOTPtypeList typeOTPtypeList) {
        if (typeOTPtypeList != null && typeOTPtypeList.getOTPtype() != null && !typeOTPtypeList.getOTPtype().isEmpty()) {
            List<MedasUserSign.OTPType> res = typeOTPtypeList.getOTPtype().stream().map(s -> MedasUserSign.OTPType.valueOf(s)).collect(Collectors.toList());
            return res;
        } else {
            return null;
        }
    }
    
    /**
     * Questa funzione lancia la corretta eccezione se il resultMessage tornato da una chiamata al webservice (passato in input) identifica uno stato di errore.
     * Altrimenti non fa nulla.
     * 
     * @param resultMessage l'oggetto response tornato dalle chiamate al webservice
     * @throws InvalidCredentialException
     * @throws WrongTokenException
     * @throws RemoteServiceException 
     */
    public void throwCorrectException(TypeMessage resultMessage) throws InvalidCredentialException, WrongTokenException, RemoteServiceException {
        if (resultMessage != null ) {
            String description = String.format("remote server error. code: %s - message: %s", resultMessage.getCode().intValue(), resultMessage.getMessage());
            if (resultMessage.getCode().intValue() > 0) {
                logger.error(description);
                switch (resultMessage.getCode().intValue()) {
                    case   65:
                    case   81:
                    case  172:
                    case 1002:
                        throw new InvalidCredentialException("invalid credential" + resultMessage.getMessage());
                    case 1003:
                        throw new WrongTokenException("invalid credential" + resultMessage.getMessage());
                    default:
                        throw new RemoteServiceException(description);
                }
            } else {
                logger.info(description);
            }
        } else {
            throw new RemoteServiceException("il resultMessage è null, questo non dovrebbe succedere");
        }
    }
}
