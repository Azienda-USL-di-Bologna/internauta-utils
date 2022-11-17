package it.bologna.ausl.internauta.utils.versatore.configuration;

import it.bologna.ausl.minio.manager.MinIOWrapper;

/**
 * Classe astratta con li metodo per ottenere MinIOWrapper
 * 
 * La classe va implementata all'interno dell'applicazione nella quale il modulo è inserito (attualmente internauta) e poi settata tramite il metodo
 * setMinIOWrapper della classe it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreMinIOWrapperConfiguration
 * 
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public abstract class VersatoreRepositoryManager {
    
    public abstract MinIOWrapper getMinIOWrapper();
    
}
