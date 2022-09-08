package it.bologna.ausl.internauta.utils.firma.configuration;

import org.springframework.stereotype.Component;

/**
 * Questa classe contiene l'istanza di HttpClientManager che rappresenta la configurazione del client OkHttp.
 * L'istanza deve essere settata dall'applicazione nella quale il modulo è inserito (attualmente internauta)
 *  tramite il metodo setRepositoryManager
 * 
 * @author gdm
 */
@Component
public class FirmaHttpClientConfiguration {
    
    private FirmaHttpClientManager httpClientManager;

    public FirmaHttpClientManager getHttpClientManager() {
        return httpClientManager;
    }

    public void setHttpClientManager(FirmaHttpClientManager httpClientManager) {
        this.httpClientManager = httpClientManager;
    }
}
