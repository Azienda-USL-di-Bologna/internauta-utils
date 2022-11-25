package it.bologna.ausl.internauta.utils.versatore.configuration;

import org.springframework.stereotype.Component;

/**
 * Questa classe contiene l'istanza di VersatoreRepositoryManager.
 * L'istanza deve essere settata dall'applicazione nella quale il modulo è inserito (attualmente internauta)
 * tramite il metodo setVersatoreRepositoryManager
 * 
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@Component
public class VersatoreRepositoryConfiguration {
    
    private VersatoreRepositoryManager versatoreRepositoryManager;

    public VersatoreRepositoryManager getVersatoreRepositoryManager() {
        return versatoreRepositoryManager;
    }

    public void setVersatoreRepositoryManager(VersatoreRepositoryManager versatoreRepositoryManager) {
        this.versatoreRepositoryManager = versatoreRepositoryManager;
    }
}
