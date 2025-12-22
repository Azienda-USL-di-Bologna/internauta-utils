package it.bologna.ausl.internauta.utils.sendintegration.configuration;

import org.springframework.stereotype.Component;

/**
 * Questa classe contiene l'istanza di SendIntegrationHttpClientManager che rappresenta la configurazione del repository per accedere ai file da scaricare.
 * L'istanza httpClientManager deve essere settata dall'applicazione nella quale il modulo è inserito (attualmente internauta) tramite il metodo setHttpClientManager
 * 
 * @author gdm
 */
@Component
public class SendIntegrationHttpClientConfiguration {
    
    private SendIntegrationHttpClientManager httpClientManager;

    public SendIntegrationHttpClientManager getHttpClientManager() {
        return httpClientManager;
    }

    public void setHttpClientManager(SendIntegrationHttpClientManager httpClientManager) {
        this.httpClientManager = httpClientManager;
    }
}
