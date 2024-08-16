package it.bologna.ausl.internauta.utils.downloader.plugin;

import it.bologna.ausl.internauta.utils.downloader.configuration.RepositoryManager;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderDownloadException;
import java.io.InputStream;
import java.util.Map;

/**
 * Rappresenta un generico puglin per lo scaricamento dei file
 * 
 * @author gdm
 */
public abstract class DownloaderDownloadPlugin implements DownloaderPlugin {

    protected Map<String, Object> params;
   
    protected RepositoryManager repositoryManager;
    
    /**
     * Questo costruttore andrà esteso nella classe concreta passando i params letti dal token e il repositoryManager
     * @param params params letti dal token, nei quali ci sono i dettagli per lo scaricamento del file
     * @param repositoryManager l'oggetto RepositoryManager settato dall'applicazione che include il modulo di Downloader (attualmetne internauta)
     */
    protected DownloaderDownloadPlugin(Map<String, Object> params, RepositoryManager repositoryManager) {
        this.params = params;
        this.repositoryManager = repositoryManager;
    }

    /**
     * Torna lo stram del file indentificabile dai params passati in fase di costruzione dell'oggetto concreto
     * @return lo stram del file 
     * @throws DownloaderDownloadException 
     */
    public abstract InputStream getFile() throws DownloaderDownloadException;
    
    /**
     * Torna il nome del file indentificabile dai params passati in fase di costruzione dell'oggetto concreto
     * @return il nome del file
     * @throws DownloaderDownloadException 
     */
    public abstract String getFileName() throws DownloaderDownloadException;
}
