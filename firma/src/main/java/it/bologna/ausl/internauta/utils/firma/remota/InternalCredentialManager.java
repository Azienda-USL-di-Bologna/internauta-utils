package it.bologna.ausl.internauta.utils.firma.remota;

import it.bologna.ausl.internauta.utils.firma.repositories.ConfigurationRepository;
import it.bologna.ausl.internauta.utils.firma.repositories.CredentialRepository;
import it.bologna.ausl.internauta.utils.firma.utils.AESEncryption;
import it.bologna.ausl.internauta.utils.firma.utils.exceptions.EncryptionException;
import it.bologna.ausl.model.entities.firma.Configuration;
import it.bologna.ausl.model.entities.firma.Credential;
import it.bologna.ausl.model.entities.firma.QCredential;
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
        Optional<Credential> credentialOp = credentialRepository.findOne(
                QCredential.credential.username.eq(username).and(
                QCredential.credential.configuration.hostId.eq(hostId)));
        Credential credential;
        if (credentialOp.isPresent()) {
            credential = credentialOp.get();
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
        Optional<Credential> credentialOp = credentialRepository.findOne(
                QCredential.credential.username.eq(username).and(
                QCredential.credential.configuration.hostId.eq(hostId)));
        return credentialOp.isPresent();
    }
    
    public Boolean removeCredential(String username, String hostId) {
        Optional<Credential> credentialOp = credentialRepository.findOne(
                QCredential.credential.username.eq(username).and(
                QCredential.credential.configuration.hostId.eq(hostId)));
        if (credentialOp.isPresent()) {
            credentialRepository.delete(credentialOp.get());
            return true;
        } else {
            return false;
        }
    }
    
    public String getPlainPassword(String username, String hostId) throws EncryptionException {
        Optional<Credential> credentialOp = credentialRepository.findOne(
                QCredential.credential.username.eq(username).and(
                QCredential.credential.configuration.hostId.eq(hostId)));
        if (credentialOp.isPresent()) {
            try {
                return aesEncryption.decrypt(credentialOp.get().getPassword());
            } catch (Exception ex) {
                throw new EncryptionException("errore nella decifratura della password", ex);
            }
        } else {
            return null;
        }
    }
}
