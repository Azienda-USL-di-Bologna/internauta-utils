package it.bologna.ausl.internauta.utils.sendintegration.configuration;

import org.springframework.stereotype.Component;

/**
 * Questa classe contiene l'istanza di SendIntegrationRepositoryManager che rappresenta la configurazione del repository per accedere ai file da scaricare.
 * L'istanza repositoryManager deve essere settata dall'applicazione nella quale il modulo è inserito (attualmente internauta) tramite il metodo setRepositoryManager
 * 
 * @author gdm
 */
@Component
public class SendIntegrationRepositoryConfiguration {
    
    private SendIntegrationRepositoryManager repositoryManager;

    public SendIntegrationRepositoryManager getRepositoryManager() {
        return repositoryManager;
    }

    public void setRepositoryManager(SendIntegrationRepositoryManager repositoryManager) {
        this.repositoryManager = repositoryManager;
    }
}
