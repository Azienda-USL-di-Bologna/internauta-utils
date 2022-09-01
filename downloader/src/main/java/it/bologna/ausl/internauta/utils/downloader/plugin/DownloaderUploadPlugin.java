package it.bologna.ausl.internauta.utils.downloader.plugin;

import it.bologna.ausl.internauta.utils.downloader.configuration.RepositoryManager;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderUploadException;
import java.io.InputStream;
import java.util.Map;

/**
 * Rappresenta un generico puglin per l'upload dei file
 * 
 * @author gdm
 */
public abstract class DownloaderUploadPlugin implements DownloaderPlugin {

    protected Map<String, Object> params;
   
    protected RepositoryManager repositoryManager;
    
    /**
     * Questo costruttore andrà esteso nella classe concreta passando i params letti dal token e il repositoryManager
     * @param params params letti dal token, nei quali ci sono i dettagli per lo scaricamento del file
     * @param repositoryManager l'oggetto RepositoryManager settato dall'applicazione che include il modulo di Downloader (attualmetne internauta)
     */
    protected DownloaderUploadPlugin(Map<String, Object> params, RepositoryManager repositoryManager) {
        this.params = params;
        this.repositoryManager = repositoryManager;
    }

    /**
     * Carica il file sul repository target, indentificabile dai params passati in fase di costruzione dell'oggetto concreto
     * @param file lo stream del file da caricare
     * @param path il path in cui il file va inserito
     * @param fileName il nome del file
     * @return i params per la creazione del context per poterlo scaricare
     * @throws DownloaderUploadException 
     */
    public abstract Map<String, Object> putFile(InputStream file, String path, String fileName) throws DownloaderUploadException;
}
