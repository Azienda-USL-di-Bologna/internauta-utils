package it.bologna.ausl.internauta.utils.versatore.configuration;

import okhttp3.OkHttpClient;

/**
 * Classe astratta cpn li metodo per ottenere il client http
 * 
 * La classe va implementata all'interno dell'applicazione nella quale il modulo è inserito (attualmente internauta) e poi settata tramite il metodo
 * setHttpClientManager della classe it.bologna.ausl.internauta.utils.versatore.configuration.HttpClientConfiguration
 * 
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public abstract class VersatoreHttpClientManager {
    
    public abstract OkHttpClient getOkHttpClient();
    
}
