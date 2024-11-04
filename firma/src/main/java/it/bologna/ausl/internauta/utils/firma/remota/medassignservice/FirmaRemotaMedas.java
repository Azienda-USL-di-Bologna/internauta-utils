package it.bologna.ausl.internauta.utils.firma.remota.medassignservice;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.BaseEncoding;
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
import it.bologna.ausl.internauta.utils.firma.exceptions.FirmaHttpException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.InvalidCredentialException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteFileNotFoundException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteServiceException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.WrongTokenException;
import it.bologna.ausl.internauta.utils.firma.remota.utils.FirmaRemotaDownloaderUtils;
import it.bologna.ausl.internauta.utils.firma.remota.utils.pdf.PdfSignFieldDescriptor;
import it.bologna.ausl.internauta.utils.firma.remota.utils.pdf.PdfUtils;
import it.bologna.ausl.internauta.utils.firma.utils.CommonUtils;
import it.bologna.ausl.internauta.utils.firma.utils.ConfigParams;
import it.bologna.ausl.internauta.utils.firma.exceptions.EncryptionException;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.firma.Configuration;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.ws.BindingProvider;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
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
    private final Map<String, Object> SFTPConnectionParams;
    private final ScrybaSignServerSync syncSignService;
    private final ScrybaSignServerUtils utilsService;
    private final List<Map<String, Object>> profiles;
    
    private File sftpKeyFile = null;
    private final ThreadLocal<File> tmpFileToSign = new ThreadLocal<>();
    private final ThreadLocal<String> largeFileHash = new ThreadLocal<>();
    private final ThreadLocal<Pair<Session, ChannelSftp>> sftpConnection = new ThreadLocal<>();
    
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
        this.SFTPConnectionParams = (Map<String, Object>) medasServiceConfiguration.get("SFTPConnectionParams");
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
                bpUtilsService.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, this.utilsServiceAuth.get("username"));
                bpUtilsService.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, this.utilsServiceAuth.get("password"));
            }
        }
        
        // se l'invio al server SFTP per file di grandi dimensioni è attivo e ha bisogno di una chiave, controllo che il file della chiave esista
        if (isSftpForLargeFileEnabled() && this.SFTPConnectionParams.containsKey("keyPath")) {
            String sftpKeyFilePath = (String) this.SFTPConnectionParams.get("keyPath");
            this.sftpKeyFile = new File(sftpKeyFilePath);
            if (!sftpKeyFile.exists()) {
                String errorMessage = String.format("la chieve per la connessione al server SFTP non è stata trovata al path: %s", sftpKeyFile.getAbsolutePath());
                logger.error(errorMessage);
                throw new FirmaRemotaConfigurationException(errorMessage);
            }
        }
    }
    
    /**
     * torna se nei parametri è abilitato il server sftp da utilizzare per la firma dei file di grandi dimensioni
     * @return 
     */
    private boolean isSftpForLargeFileEnabled() {
        return 
            this.SFTPConnectionParams != null &&
            !this.SFTPConnectionParams.isEmpty() &&
            this.SFTPConnectionParams.containsKey("enabled") &&
            (Boolean) this.SFTPConnectionParams.get("enabled");
    }
    
    private ChannelSftp connectOnSFTP() throws JSchException {
        logger.info("connecting to sftp...");
        JSch jSch = new JSch();        
        
        String host = (String) this.SFTPConnectionParams.get("host");
        Integer port = (Integer) this.SFTPConnectionParams.get("port");
        String user = (String) this.SFTPConnectionParams.get("user");
        String keyPassword = (String) this.SFTPConnectionParams.get("keyPassword");
        
        if (this.sftpKeyFile != null) {
            if (StringUtils.hasText(keyPassword)) {
                jSch.addIdentity(this.sftpKeyFile.getAbsolutePath(), keyPassword);
            } else {
                jSch.addIdentity(this.sftpKeyFile.getAbsolutePath());
            }
            logger.info("Private Key Added.");
        }
        Session session = jSch.getSession(user, host, port);
        logger.info("SFTP Session created.");

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();
        logger.info("connected to SFTP channel.");
        sftpConnection.set(Pair.of(session, channelSftp));
        return channelSftp;
    }
    
    private void disconnectFromSFTP() {
        Pair<Session, ChannelSftp> sftpConnectionTriple = sftpConnection.get();
        if (sftpConnectionTriple != null) {
            if (sftpConnectionTriple.getFirst()!= null) {
                try {
                    sftpConnectionTriple.getFirst().disconnect();
                    logger.info("disconnected from SFTP session.");
                } catch (Exception ex) {
                    logger.error("erron on disconneting from SFTP session", ex);
                }
            }
            if (sftpConnectionTriple.getSecond()!= null) {
                try {
                    sftpConnectionTriple.getSecond().disconnect();
                    logger.info("disconnected from SFTP channel.");
                } catch (Exception ex) {
                    logger.error("erron on disconneting from SFTP Channel", ex);
                }
            }
        }
    }
    
    /**
     * trasferisce il file sul server sftp secondo le regole di medas
     * @param file il file
     * @param cf il codice fiscale del firmatario
     * @return l'hash sha256 in hex del file
     * @throws RemoteServiceException 
     */
    private String manageFileOnSFTP(File file, String cf) throws RemoteServiceException  {
        ChannelSftp sftpChannel;
        try {
            sftpChannel = connectOnSFTP();
        } catch (Exception ex) {
            String errorMessage = "error on connecton to sftp";
            logger.error(errorMessage, ex);
            throw new RemoteServiceException(errorMessage, ex);
        }
        
        // calcolo l'hash sha256 in hex del file
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
        
        // carico il file sul server sftp con un nome random nella cartella transfer
        String toSignFolder = (String) this.SFTPConnectionParams.get("toSingFolder");
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
        
        // se tutto ok, rinomino il file secondo il formato voluto da medas: cf_sha256hex"
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
        
        // aggiungo il file appena creato alla lista di quelli da cancellare alla fine della firma o in caso di errore
        this.largeFileHash.set(sftpFileName);
        return sha256hex;
    }
    
    /**
     * dopo la chiamata alla firma, recupera il file dalla cartella dei file firmati transfer/signed"
     * viene inserito in questa cartella in automatico dal server dopo la firma
     * @param sftpPath il file con il nome cf_sha256hex 
     * @return lo stream del file firmato
     * @throws RemoteServiceException 
     */
    private InputStream retreiveSignedFileFromSFTP(String sftpPath) throws RemoteServiceException {
        try {
            ChannelSftp sftpChannel = sftpConnection.get().getSecond();
            String sftpSignedFolder = (String) SFTPConnectionParams.get("signedFolder");
            return sftpChannel.get(String.format("%s/%s", sftpSignedFolder, sftpPath));
        } catch (Exception ex) {
            String errorMessage = String.format("error on retrieving file %s on sftp", sftpPath);
            logger.error(errorMessage, ex);
            throw new RemoteServiceException(errorMessage, ex);
        }
    }
    
    /**
     * rimuove il filepath passato, dal server sftp
     * @param sftpPath 
     */
    private void removeFilePathFromSFTP(String sftpPath) {
        try {
            logger.info(String.format("removing file %s from sftp...", sftpPath));
            ChannelSftp sftpChannel = sftpConnection.get().getSecond();
            sftpChannel.rm(sftpPath);
        } catch (Exception ex) {
            String errorMessage = "error on deleting file to sftp";
            logger.error(errorMessage, ex);
        }
    }
    
    /**
     * rimvuome il file firmato passato, dal server sftp
     * @param filename 
     */
    private void removeSignedFileFromSFTP(String filename) {
        String signedFolder = (String) SFTPConnectionParams.get("signedFolder");
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
            typeGetUserInfo4Req.setSsn(userInformation.getUsername());
        } else if (StringUtils.hasText(userInformation.getCodiceFiscale())) {
            typeGetUserInfo4Req.setSsn(userInformation.getCodiceFiscale());
        }
        typeGetUserInfo4Req.setUsername("");  // bisogna settare vuoto perché così ci hanno chiesto di fare
        if (StringUtils.hasText(processId)) {
            typeGetUserInfo4Req.setProcessId(processId);
        }
         if (docTypes != null && !docTypes.isEmpty()) {
            TypeDocTypeList typeDocTypeList = new TypeDocTypeList();
            typeDocTypeList.getDocType().addAll(docTypes);
            typeGetUserInfo4Req.setDocTypeList(typeDocTypeList);
        }
        return typeGetUserInfo4Req;
    }
    
    /**
     * crea l'oggetto contenente le credenziali da passare alla chiamata
     * La password, se non è settata, ma è stato passato userInformation.useSavedCredential() a true, prova a recuparla dal credential manager
     * @param userInformation le informazione dell'utenza di firma
     * @param medasUserSign l'oggetto che indica il certificato e potere di firma selezionati per firmare
     * @return
     * @throws EncryptionException 
     */
    private TypeCredentials getTypeCredentials(MedasUserInformation userInformation, MedasUserSign medasUserSign) throws EncryptionException {
        TypeCredentials typeCredentials = new TypeCredentials();
        if (StringUtils.hasText(userInformation.getOtp())) {
            typeCredentials.setOtp(userInformation.getOtp());
        }
        
        if (StringUtils.hasText(userInformation.getPassword())) {
            typeCredentials.setPassword(userInformation.getPassword());
        } else  if (
            medasUserSign != null &&
            userInformation.useSavedCredential() != null &&  userInformation.useSavedCredential() && 
            configuration.getInternalCredentialsManager() != null  && configuration.getInternalCredentialsManager()) {
                /* 
                    devo recupare la password deal credential manager. Dato che lo stesso username può avere più firme, devo costruire l'oggetto additionalData
                    per indicare quale recuperare
                */
                HashMap<String, Object> additionalData = new HashMap<>();
                additionalData.put(MedasUserSign.CREDENTIAL_ADDITIONAL_DATA_KEY, medasUserSign.getId());
                String plainPassword = internalCredentialManager.getPlainPassword(userInformation.getUsername(), configuration.getHostId(), additionalData);
                if (StringUtils.hasText(plainPassword)) {
                    typeCredentials.setPassword(plainPassword);
                }
        }
        return typeCredentials;
    }
    
    /**
     * costruisce l'oggetto TypeUser da passare alle chiamate.
     * Usa sempre ssn per indentificare l'utente, ma viene letto dal campo username per far funzionare iol caso di cf diverso dall'effettivo cf dell'utente. Più
     * che altro server per i di test
     * @param userInformation
     * @return 
     */
    private TypeUser getTypeUser(MedasUserInformation userInformation) {
        TypeUser typeUser = new TypeUser();
        //typeUser.setUsername(userInformation.getUsername());
//        typeUser.setFirstName(userInformation.getNome());
//        typeUser.setLastName(userInformation.getCognome());
//        typeUser.setSsn(userInformation.getCodiceFiscale());
        typeUser.setSsn(userInformation.getUsername());
        return typeUser;
    }
    
    private TypeDespatchOtpReq getTypeDespatchOtpReq(MedasUserInformation userInformation, MedasUserSign medasUserSign) throws EncryptionException {
        TypeDespatchOtpReq typeDespatchOtpReq = new TypeDespatchOtpReq();
        typeDespatchOtpReq.setCertificateId(medasUserSign.getCertificateId());
        typeDespatchOtpReq.setUser(getTypeUser(userInformation));
        typeDespatchOtpReq.setCredentials(getTypeCredentials(userInformation, medasUserSign));
        return typeDespatchOtpReq;
    }
    
    private TypeOpenSignSessionReq getTypeOpenSignSessionReq(MedasUserInformation userInformation, MedasUserSign medasUserSign) throws EncryptionException {
        TypeOpenSignSessionReq typeOpenSignSessionReq = new TypeOpenSignSessionReq();
        typeOpenSignSessionReq.setUser(getTypeUser(userInformation));
        typeOpenSignSessionReq.setCredentials(getTypeCredentials(userInformation, medasUserSign));
        typeOpenSignSessionReq.setCertificateId(medasUserSign.getCertificateId());
        typeOpenSignSessionReq.setSignaturePowerId(medasUserSign.getSignPowerCode());
        return typeOpenSignSessionReq;
    }
    
    private TypeCloseSignSessionReq getTypeCloseSignSessionReq(String sessionId) {
        TypeCloseSignSessionReq typeCloseSignSessionReq = new TypeCloseSignSessionReq();
        typeCloseSignSessionReq.setAction("");  // bisogna settare vuoto perché così ci hanno chiesto da Medas
        typeCloseSignSessionReq.setSessionId(sessionId);
        return typeCloseSignSessionReq;
    }
    
    /**
     * crea l'oggetto TypeSignDocReq da passare per eseguire la firma. Presupopne che sia stata aperta già una sessione.
     * gestisce anche il caso del file di grandi dimensioni
     * @param userInformation le info dell'utenza
     * @param file il fire da firmare
     * @param session l'id della sessione aperta
     * @return
     * @throws SignParamsException
     * @throws IOException
     * @throws RemoteFileNotFoundException
     * @throws RemoteServiceException 
     */
    private TypeSignDocReq getTypeSignDocReq(MedasUserInformation userInformation, FirmaRemotaFile file, String sessionId) throws SignParamsException, IOException, RemoteFileNotFoundException, RemoteServiceException, FileNotFoundException, UnsupportedEncodingException, MimeTypeException {
        TypeSignDocReq typeSignDocReq = new TypeSignDocReq();
        
        String processId = (String) profiles.get(0).get("processId");
        List<String> docTypes = (List<String>) profiles.get(0).get("docTypes");
        
//        MedasUserSign userSign = (MedasUserSign) userInformation.getUserSign();
        
        typeSignDocReq.setSessionId(sessionId);
        
        File tmpFileToSign = createTempFile(file);
        
        typeSignDocReq.setSignProperties(getTypeSignProperties(userInformation, file.getFormatoFirma(), file.getSignAppearance(), tmpFileToSign));
        
        TypeDocument typeDocument = new TypeDocument();
        typeDocument.setDocType(docTypes.get(0));
        typeDocument.setSignStringCode("");
        
        String fileBase64;
        long fileSize = tmpFileToSign.length();
        boolean largeFile = fileSize > 10 * 1000000;
        // largeFile = true;
        logger.info(String.format("file size: %s bytes, largeFile: %s", fileSize, largeFile));
        if (largeFile && isSftpForLargeFileEnabled()) {
            logger.info(String.format("large file"));
            String hashFile = manageFileOnSFTP(tmpFileToSign, userInformation.getUsername());
            typeDocument.setDocHash(hashFile);
            fileBase64 = "RklMRQ==";
        } else {
            logger.info(String.format("file not large"));
            byte[] fileBytes = FileCopyUtils.copyToByteArray(tmpFileToSign);
            fileBase64 = BaseEncoding.base64().encode(fileBytes);
        }
        typeDocument.setDocBin(fileBase64);
        
        typeSignDocReq.setDocument(typeDocument);
        
        return typeSignDocReq;
    }
    
    /**
     * crea l'oggetto TypeSignProperties.
     * gestisce anche il caso di firma visibile
     * @param userInformation
     * @param formatoFirma
     * @param signAppearance
     * @param file
     * @return
     * @throws SignParamsException
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private TypeSignProperties getTypeSignProperties(MedasUserInformation userInformation, FirmaRemotaFile.FormatiFirma formatoFirma, SignAppearance signAppearance, File file) throws SignParamsException, FileNotFoundException, IOException, UnsupportedEncodingException, MimeTypeException {
        TypeSignProperties typeSignProperties = new TypeSignProperties();
        MedasUserSign userSign = (MedasUserSign) userInformation.getUserSign();
        
        typeSignProperties.setSignMode(getSignMode(formatoFirma));
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
                        String.format("%s %s", "#SIGNERSURNAME#", "#signerName#"),
                        null);
                }
                TypeArssPadesPropertiesApparence typeArssPadesPropertiesApparence = new TypeArssPadesPropertiesApparence();
                typeArssPadesPropertiesApparence.setImage(""); // bisogna settare vuoto sennò dà errore
                typeArssPadesPropertiesApparence.setText(pdfSignFieldDescriptor.getText());
                typeArssPadesProperties.setArssPadesPropertiesApparence(typeArssPadesPropertiesApparence);

                typePadesProperties.setPage(String.valueOf(pdfSignFieldDescriptor.getPage()));
                typePadesProperties.setLeftx(String.valueOf(pixelToMillimiter(pdfSignFieldDescriptor.getLowerLeftX())));
                typePadesProperties.setLefty(String.valueOf(pixelToMillimiter(pdfSignFieldDescriptor.getLowerLeftY())));
                typePadesProperties.setRightx(String.valueOf(pixelToMillimiter(pdfSignFieldDescriptor.getUpperRightX())));
                typePadesProperties.setRighty(String.valueOf(pixelToMillimiter(pdfSignFieldDescriptor.getUpperRightY())));
            } else {
                // per inserire una firma invisibile sevo settare a 1 tutte le proprietà di typePadesProperties
                typePadesProperties.setPage("1");
                typePadesProperties.setLeftx("1");
                typePadesProperties.setLefty("1");
                typePadesProperties.setRightx("1");
                typePadesProperties.setRighty("1");
            }
            
            typePadesProperties.setArssPadesProperties(typeArssPadesProperties);
            typeSignProperties.setPadesProperties(typePadesProperties);
        } else if (formatoFirma == FirmaRemotaFile.FormatiFirma.P7M) {
            if (CommonUtils.isP7m(file)) {
                typeSignProperties.setParallel(true);
            } else {
                typeSignProperties.setParallel(false);
            }
        }
        return typeSignProperties;
    }
    
    /**
     * trasforma i pixel in millimetri presupponendo una risoluzione di 72 dpi (la risoluzione del firmispizio)
     * @param pixel
     * @return 
     */
    private int pixelToMillimiter(int pixel) {
        float mm = pixel * 0.35f;
        return Math.round(mm);
    }
    
    private File createTempFile(FirmaRemotaFile file) throws IOException, RemoteFileNotFoundException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "firma_remota");
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }
        File tmpFile = File.createTempFile("firma_remota_medas_to_sing_tmp", null, tempDir);
        // per prima cosa scarica il file da firmare nella cartella temporanea scaricandolo dall'url
        try (InputStream in = new URL(file.getUrl()).openStream()) {
            logger.info(String.format("saving file %s on temp file %s...", file.getFileId(), tmpFile.getAbsolutePath()));
            Files.copy(in, Paths.get(tmpFile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
            this.tmpFileToSign.set(tmpFile);
            tmpFile.deleteOnExit();
            logger.info("temfile saved");
        } catch (IOException e) {
            throw new RemoteFileNotFoundException("errore nel download del file da firmare probabilmente è scaduto il timeout", e);
        }
        
        return tmpFile;
    }
    
    @Override
    public FirmaRemotaInformation firma(FirmaRemotaInformation firmaRemotaInformation, String codiceAzienda, HttpServletRequest request) throws FirmaHttpException, RemoteFileNotFoundException, WrongTokenException, InvalidCredentialException, RemoteServiceException {
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
            OpenSignSessionResp openSignSessionResp = null;
            try {
                openSignSessionResp = this.syncSignService.openSignSession(openSignSessionReq);
            } catch (Exception ex) {
                String errorMessage = "errore generico di connessione al server della firma";
                logger.error(errorMessage, ex);
                throw new RemoteServiceException(errorMessage, ex);
            }
            throwCorrectException(openSignSessionResp.getOpenSignSessionResp().getMessage());
            String sessionId = openSignSessionResp.getOpenSignSessionResp().getSessionId();
            
            for (FirmaRemotaFile file : files) {
                TypeSignDocResp typeSignDocResp;
                try { // questo try mi server per potermi disconnettere da sftp nel caso mi ci sono connesso
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
                    // se c'è un errore lancia l'eccezione corretta, altrimenti non fa nulla
                    throwCorrectException(typeSignDocResp.getMessage());
                    
                    InputStream signedFileIs = null;
                    try {
                        /*
                            se ho fimato un file grosso recupero lo stream dal server sftp
                            TODO: forse si potrebbe controllare anche nella guardando nella response, per vedere se si capisce se si è firmato un file grosso o meno
                        */
                        if (StringUtils.hasText(largeFileHash.get())) {
                            try {
                                signedFileIs = retreiveSignedFileFromSFTP(largeFileHash.get());
                            } catch (RemoteServiceException ex) {
                                // se c'è errore rimuovo il file dal server SFTP
                                removeSignedFileFromSFTP(largeFileHash.get());
                                largeFileHash.remove();
                                throw ex;
                            }
                        } else {
                            // altrimenti creo lo stream decodificando il base64 tornato dal server
                            signedFileIs = BaseEncoding.base64().decodingStream(new StringReader(typeSignDocResp.getSignedDocBin()));
                        }
                        try {
                            // carico il file sul repository
                            super.upload(file, signedFileIs, codiceAzienda, request);
                        } catch (MinIOWrapperException ex) {
                            String errorMessage = String.format("remote server error. Error uploading fileId %s on repository", file.getFileId());
                            logger.error(errorMessage, ex);
                            throw new RemoteServiceException(errorMessage, ex);
                        } finally {
                            // rimuovo il file dal server SFTP
                            if (StringUtils.hasText(largeFileHash.get())) {
                                removeSignedFileFromSFTP(largeFileHash.get());
                                largeFileHash.remove();
                            }
                        }
                    } finally {
                        IOUtils.closeQuietly(signedFileIs);
                    }
                } finally {
                    if (tmpFileToSign.get() != null && tmpFileToSign.get().exists()) {
                        tmpFileToSign.get().delete();
                    }
                    disconnectFromSFTP();
                }
            }
            CloseSignSessionReq closeSignSessionReq = new CloseSignSessionReq();
            closeSignSessionReq.setCloseSignSessionReq(getTypeCloseSignSessionReq(sessionId));
            this.syncSignService.closeSignSession(closeSignSessionReq);
            
            // probabilmente non serve, ma per sicurezza rimuovo il file dal server SFTP
            if (StringUtils.hasText(largeFileHash.get())) {
                removeSignedFileFromSFTP(largeFileHash.get());
                largeFileHash.remove();
            }
        }
        return firmaRemotaInformation;
    }

    @Override
    public void preAuthentication(UserInformation userInformation) throws FirmaHttpException, WrongTokenException, InvalidCredentialException, RemoteServiceException {
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
     * @throws FirmaHttpException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    @Override
    protected boolean externalExistingCredential(UserInformation userInformation, String hostId) throws FirmaHttpException, InvalidCredentialException, RemoteServiceException {
        return false;
    }
    
    /**
     * il salvataggio delle credenziali non è supportato da Medas, per cui torniamo sempre false
     * @param userInformation
     * @param hostId
     * @return
     * @throws FirmaHttpException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    @Override
    protected boolean externalSetCredential(UserInformation userInformation, String hostId) throws FirmaHttpException, InvalidCredentialException, RemoteServiceException {
        return false;
    }
    
    /**
     * il salvataggio delle credenziali non è supportato da Medas, per cui torniamo sempre false
     * @param userInformation
     * @param hostId
     * @return
     * @throws FirmaHttpException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    @Override
    protected boolean externalRemoveCredential(UserInformation userInformation, String hostId) throws FirmaHttpException, InvalidCredentialException, RemoteServiceException {
        return false;
    }

    /**
     * Rileva i poteri di firma e crea la lista di FirmaRemotaUserSign da mostrare all'utente perché selezioni quello voluto
     * @param userInformation
     * @return 
     * @throws it.bologna.ausl.internauta.utils.firma.exceptions.FirmaHttpException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.InvalidCredentialException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteServiceException 
     */
    @Override
    public List<FirmaRemotaUserSign> getUserSigns(UserInformation userInformation) throws FirmaHttpException, InvalidCredentialException, RemoteServiceException {
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
            String errorMessage = "errore generico di connessione al server per la firma";
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
                        String otpTypesDescription = String.join(",", certificate.getOTPtypeList().getOTPtype());
                        userSign.setDescription(String.format("%s - %s", certificate.getDescription(), otpTypesDescription));
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
        Collections.sort(res, (FirmaRemotaUserSign lhs, FirmaRemotaUserSign rhs) -> {
            if (lhs == null) {
                return -1;
            } else if (rhs == null) {
                return 1;
            }
            return lhs.getDescription().compareTo(rhs.getDescription());
        });
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
            if (resultMessage.getCode().intValue() > 0) {
                String message = resultMessage.getMessage();
                if (message == null) {
                    message = "Errore generico";
                }
                String description = String.format("remote server error. code: %s - message: %s", resultMessage.getCode().intValue(), message);
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
                
                String description = String.format("remote server response OK. code: %s - message: %s", resultMessage.getCode().intValue(), resultMessage.getMessage());
                logger.info(description);
            }
        } else {
            throw new RemoteServiceException("il resultMessage è null, questo non dovrebbe succedere");
        }
    }
}
