package it.bologna.ausl.internauta.utils.sendintegration;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import it.bologna.ausl.internauta.utils.sendintegration.exceptions.SendIntegrationException;
import it.bologna.ausl.model.entities.sendintegration.SendIntegrationConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.File;
import java.io.InputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
@Component
public class SendIntegrationSFTPManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendIntegrationSFTPManager.class);
    
    @Value("${openapi.send-integration.active:false}")
    private Boolean sendIntegrationActive;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private Map<String, Object> SFTPConnectionParams;
    private File sftpKeyFile;
    private final ThreadLocal<Pair<Session, ChannelSftp>> sftpConnection = new ThreadLocal<>();
    
    
    @PostConstruct
    public void init() throws SendIntegrationException {
        //TODO: devo mettere il false altrumenti non parte, poi si deve rimuovuore quando abbiamo il file per la connessione al server sftp
        if (false && sendIntegrationActive) {
            SendIntegrationConfiguration lepidaSFTPConfiguration = entityManager.find(SendIntegrationConfiguration.class, SendIntegrationConstants.Parameters.lepidaSFTPConfiguration.toString());
            this.SFTPConnectionParams = lepidaSFTPConfiguration.getValue();
            this.sftpKeyFile = new File((String) this.SFTPConnectionParams.get("keyPath"));
            if (!this.sftpKeyFile.exists()) {
                String error = String.format("il file della chiave per la connessione al servizio SFTP non esiste nel percorso indicato: %s", sftpKeyFile);
                throw new SendIntegrationException(error);
            }
        }
    }
    
    public ChannelSftp connect() throws JSchException {
    
        LOGGER.info("connecting to sftp...");
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
            LOGGER.info("Private Key Added.");
        }
        Session session = jSch.getSession(user, host, port);
        LOGGER.info("SFTP Session created.");

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();
        LOGGER.info("connected to SFTP channel.");
        sftpConnection.set(Pair.of(session, channelSftp));
        return channelSftp;
    }
    
    public void disconnect() {
        Pair<Session, ChannelSftp> sftpConnectionPair = sftpConnection.get();
        if (sftpConnectionPair != null) {
            if (sftpConnectionPair.getFirst()!= null) {
                try {
                    sftpConnectionPair.getFirst().disconnect();
                    LOGGER.info("disconnected from SFTP session.");
                } catch (Exception ex) {
                    LOGGER.error("erron on disconneting from SFTP session", ex);
                }
            }
            if (sftpConnectionPair.getSecond()!= null) {
                try {
                    sftpConnectionPair.getSecond().disconnect();
                    LOGGER.info("disconnected from SFTP channel.");
                } catch (Exception ex) {
                    LOGGER.error("erron on disconneting from SFTP Channel", ex);
                }
            }
        }
    }
    
    public boolean existsPath(String path) throws SftpException {
        ChannelSftp sftpChannel = sftpConnection.get().getSecond();
        try {
            sftpChannel.stat(path);
            return true;
        } catch (SftpException ex) {
            if (ex.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return false;
            } else {
                throw ex;
            }
        }
    }
    
    public InputStream retriveFile(String filePath) throws SftpException {
        ChannelSftp sftpChannel = sftpConnection.get().getSecond();
        return sftpChannel.get(filePath);
    }
}
