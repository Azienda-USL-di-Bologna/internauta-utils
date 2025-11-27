package it.bologna.ausl.internauta.utils.sendintegration;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import it.bologna.ausl.internauta.utils.sendintegration.exceptions.SendIntegrationException;
import it.bologna.ausl.model.entities.sendintegration.Parameter;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.File;
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
public class SendSFTPManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendSFTPManager.class);
    
    @Value("${openapi.send-integration.active:false}")
    private Boolean sendIntegrationActive;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private Map<String, Object> SFTPConnectionParams;
    private File sftpKeyFile;
    private final ThreadLocal<Pair<Session, ChannelSftp>> sftpConnection = new ThreadLocal<>();
    
    
    @PostConstruct
    public void init() throws SendIntegrationException {
        if (sendIntegrationActive) {
            Parameter lepidaSFTPConfiguration = entityManager.find(Parameter.class, SendIntegrationConstants.Parameters.lepidaSFTPConfiguration.toString());
            this.SFTPConnectionParams = lepidaSFTPConfiguration.getValue();
            this.sftpKeyFile = (File) this.SFTPConnectionParams.get("keyPath");
            if (!this.sftpKeyFile.exists()) {
                String error = String.format("il file della chiave per la connessione al servizio SFTP non esiste nel percorso indicato: %s", sftpKeyFile);
                throw new SendIntegrationException(error);
            }
        }
    }
    
    public ChannelSftp connectOnSFTP() throws JSchException {
    
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
    
    public void disconnectFromSFTP() {
        Pair<Session, ChannelSftp> sftpConnectionTriple = sftpConnection.get();
        if (sftpConnectionTriple != null) {
            if (sftpConnectionTriple.getFirst()!= null) {
                try {
                    sftpConnectionTriple.getFirst().disconnect();
                    LOGGER.info("disconnected from SFTP session.");
                } catch (Exception ex) {
                    LOGGER.error("erron on disconneting from SFTP session", ex);
                }
            }
            if (sftpConnectionTriple.getSecond()!= null) {
                try {
                    sftpConnectionTriple.getSecond().disconnect();
                    LOGGER.info("disconnected from SFTP channel.");
                } catch (Exception ex) {
                    LOGGER.error("erron on disconneting from SFTP Channel", ex);
                }
            }
        }
    }    
}
