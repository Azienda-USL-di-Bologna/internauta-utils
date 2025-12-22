package it.bologna.ausl.internauta.utils.downloader.configuration;

import org.springframework.stereotype.Component;

/**
 * Questa classe contiene l'istanza di RepositoryManager che rappresenta la configurazione del repository per accedere ai file da scaricare.
 * L'istanza repositoryManager deve essere settata dall'applicazione nella quale il modulo Downloader è inserito (attualmente internauta)
 *  tramite il metodo setRepositoryManager
 * 
 * @author gdm
 */
@Component
public class DownloaderRepositoryConfiguration {
    
    private DownloaderRepositoryManager repositoryManager;

    public DownloaderRepositoryManager getRepositoryManager() {
        return repositoryManager;
    }

    public void setRepositoryManager(DownloaderRepositoryManager repositoryManager) {
        this.repositoryManager = repositoryManager;
    }
}
