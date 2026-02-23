package it.bologna.ausl.internauta.utils.sendintegration.configuration;

import it.bologna.ausl.minio.manager.MinIOWrapper;

/**
 * Classe astratta che descrive i metodi che il Downloader userà per ottenere l'accesso ai repository contenenti i file da scaricare
 * 
 * La classe va implementata all'interno dell'applicazione nella quale il modulo è inserito (attualmente internauta) e poi settata tramite il mettodo
 *  setRepositoryManager della classe it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationRepositoryConfiguration
 * 
 * @author gdm
 */
public abstract class SendIntegrationRepositoryManager {
    
    public abstract MinIOWrapper getMinIOWrapper();
}
