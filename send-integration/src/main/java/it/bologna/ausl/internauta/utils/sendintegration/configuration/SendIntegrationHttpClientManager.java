package it.bologna.ausl.internauta.utils.sendintegration.configuration;

import okhttp3.OkHttpClient;

/**
 * Classe astratta con li metodo per ottenere il client http
 * 
 * La classe va implementata all'interno dell'applicazione nella quale il modulo è inserito (attualmente internauta) e poi settata tramite il metodo
 * setHttpClientManager della classe it.bologna.ausl.internauta.utils.firma.configuration.SendIntegrationHttpClientConfiguration
 * @author gdm
 */
public abstract class SendIntegrationHttpClientManager {
    
    public abstract OkHttpClient getOkHttpClient();
    
}