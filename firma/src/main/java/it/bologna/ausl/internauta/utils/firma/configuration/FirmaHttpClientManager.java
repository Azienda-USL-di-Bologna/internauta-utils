package it.bologna.ausl.internauta.utils.firma.configuration;

import okhttp3.OkHttpClient;

/**
 * Classe astratta cpn li metodo per ottenere il client http
 * 
 * La classe va implementata all'interno dell'applicazione nella quale il modulo è inserito (attualmente internauta) e poi settata tramite il metodo
 * setHttpClientManager della classe it.bologna.ausl.internauta.utils.firma.configuration.HttpClientConfiguration
 * @author gdm
 */
public abstract class FirmaHttpClientManager {
    
    public abstract OkHttpClient getOkHttpClient();
    
}
