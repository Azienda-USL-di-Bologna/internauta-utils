package it.bologna.ausl.internauta.utils.firma.remota;

import it.bologna.ausl.internauta.utils.firma.repositories.ConfigurationRepository;
import it.bologna.ausl.internauta.utils.firma.repositories.CredentialRepository;
import it.bologna.ausl.internauta.utils.firma.utils.AESEncryption;
import it.bologna.ausl.internauta.utils.firma.utils.exceptions.EncryptionException;
import it.bologna.ausl.model.entities.firma.Configuration;
import it.bologna.ausl.model.entities.firma.Credential;
import it.bologna.ausl.model.entities.firma.QCredential;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class InternalCredentialManager {
    
    @Autowired
    private CredentialRepository credentialRepository;
    
    @Autowired
    private ConfigurationRepository configurationRepository;
    
    @Autowired
    private AESEncryption aesEncryption;
    
    public Boolean setCredential(String username, String password, String hostId) throws EncryptionException {
        return setCredential(username, password, hostId, null);
    }
    
    public Boolean setCredential(String username, String password, String hostId, Map<String, Object> additionalData) throws EncryptionException {        
        Credential credential = getCredential(username, hostId, additionalData);
        
        if (credential != null) {
            try {
                credential.setPassword(aesEncryption.encrypt(password));
            } catch (Exception ex) {
                throw new EncryptionException("errore nella cifratura della password", ex);
            }
        } else {
            Optional<Configuration> configurationOp = configurationRepository.findById(hostId);
            if (configurationOp.isPresent()) {
                credential = new Credential();
                credential.setConfiguration(configurationOp.get());
                credential.setUsername(username);
                if (additionalData != null) {
                    credential.setAddtionalData(additionalData);
                }
                try {
                    credential.setPassword(aesEncryption.encrypt(password));
                } catch (Exception ex) {
                    throw new EncryptionException("errore nella cifratura della password", ex);
                }
            } else {
                return false;
            }
        }
        credentialRepository.save(credential);
        return true;
    }
    
    public Boolean existingCredential(String username, String hostId) {
        return existingCredential(username, hostId, null);
    }
    
    public Boolean existingCredential(String username, String hostId, Map<String, Object> additionalData) {
        return getCredential(username, hostId, additionalData) != null;
    }
    
    public Boolean removeCredential(String username, String hostId) {
        return removeCredential(username, hostId, null);
    }
    
    public Boolean removeCredential(String username, String hostId, Map<String, Object> additionalData) {
        Credential credential = getCredential(username, hostId, additionalData);
        if (credential != null) {
            credentialRepository.delete(credential);
            return true;
        } else {
            return false;
        }
    }
    
    public String getPlainPassword(String username, String hostId) throws EncryptionException {
        return getPlainPassword(username, hostId, null);
    }
    
    public String getPlainPassword(String username, String hostId, Map<String, Object> additionalData) throws EncryptionException {
        Credential res = getCredential(username, hostId, additionalData);
        if (res != null) {
            try {
                return aesEncryption.decrypt(res.getPassword());
            } catch (Exception ex) {
                throw new EncryptionException("errore nella decifratura della password", ex);
            }
        } else {
            return null;
        }
    }
    
    private Credential getCredential(String username, String hostId, Map<String, Object> additionalData) {
        Iterable<Credential> credentials = credentialRepository.findAll(
            QCredential.credential.username.eq(username).and(
                QCredential.credential.configuration.hostId.eq(hostId))
        );
        Credential res = null;
        if (additionalData != null && !additionalData.isEmpty()) {
            for (Credential credential : credentials) {
                if (credential.getAddtionalData().equals(additionalData)) {
                    res = credential;
                    break;
                }
            }
        } else {
            if (credentials.iterator().hasNext()) {
                res = credentials.iterator().next();
            } else {
                return null;
            }
        }
        
        return res;
    }
}
