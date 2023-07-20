package it.bologna.ausl.internauta.utils.pdftoolkit.configuration;

import org.springframework.stereotype.Component;

/**
 * Questa classe contiene l'istanza di HttpClientManager che rappresenta la configurazione del client OkHttp.
 * L'istanza deve essere settata dall'applicazione nella quale il modulo è inserito (attualmente internauta) tramite il metodo setHttpClientManager
 * 
 * @author gdm
 */
@Component
public class PdfToolkitConfiguration {
    
    private PdfToolkitConfigurationManager httpClientManager;

    public PdfToolkitConfigurationManager getHttpClientManager() {
        return httpClientManager;
    }

    public void setHttpClientManager(PdfToolkitConfigurationManager httpClientManager) {
        this.httpClientManager = httpClientManager;
    }
}
