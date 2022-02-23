package it.bologna.ausl.internauta.utils.downloader.configuration;

import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.mongowrapper.MongoWrapper;

/**
 * Classe astratta che descrive i metodi che il Downloader userà per ottenere l'accesso ai repository contenenti i file da scaricare
 * 
 * La classe va implementata all'interno dell'applicazione nella quale il modulo Downloader è inserito (attualmente internauta) e poi settata tramite il mettodo
 *  setRepositoryManager della classe it.bologna.ausl.internauta.utils.downloader.configuration.DownloaderRepositoryConfiguration
 * @author gdm
 */
public abstract class RepositoryManager {
    
    public abstract MinIOWrapper getMinIOWrapper();
    
    public abstract MongoWrapper getMongoWrapper(String codiceAzienda);
    
}
