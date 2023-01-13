package it.bologna.ausl.internauta.utils.versatore.configuration;

import org.springframework.stereotype.Component;

/**
 * Questa classe contiene l'istanza di HttpClientManager che rappresenta la configurazione del client OkHttp.
 * L'istanza deve essere settata dall'applicazione nella quale il modulo è inserito (attualmente internauta)
 * tramite il metodo setHttpClientManager
 * 
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@Component
public class VersatoreHttpClientConfiguration {
    
    private VersatoreHttpClientManager httpClientManager;

    public VersatoreHttpClientManager getHttpClientManager() {
        return httpClientManager;
    }

    public void setHttpClientManager(VersatoreHttpClientManager httpClientManager) {
        this.httpClientManager = httpClientManager;
    }
}
