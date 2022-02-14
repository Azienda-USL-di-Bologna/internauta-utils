package it.bologna.ausl.internauta.utils.downloader.controllers;

import com.nimbusds.jwt.SignedJWT;
import it.bologna.ausl.internauta.utils.downloader.authorization.DownloaderAuthorizationUtils;
import it.bologna.ausl.internauta.utils.downloader.authorization.TokenBasedAuthentication;
import it.bologna.ausl.internauta.utils.downloader.configuration.DownloaderRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderDownloadException;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderParsingContextException;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderPluginException;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderUploadException;
import it.bologna.ausl.internauta.utils.downloader.plugin.DonwloaderPluginFactory;
import it.bologna.ausl.internauta.utils.downloader.plugin.DownloaderDownloadPlugin;
import it.bologna.ausl.internauta.utils.downloader.plugin.DownloaderUploadPlugin;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${downloader.mapping.url}")
public class DownloaderController {
    private static Logger logger = LoggerFactory.getLogger(DownloaderAuthorizationUtils.class);
    
    @Autowired
    private DownloaderRepositoryConfiguration downloaderRepositoryConfiguration;
    
    /**
     * Servlet chiamata per lo scaricamento di un file.
     * 
     * Anche il token è passato in input, non viene usato direttamente:
     *  La chiamata è intercettata da un filter (vedi classe it.bologna.ausl.internauta.utils.downloader.authorization.DownloaderRegistrationBean)
     *  Questo filter decritta e controlla il token e lo inserisce nel contesto.
     *  La servlet estrae il token dal contesto e reperisce il context-claim, che contiene le informazioni sul file da scaricare, grazie alle quali ne può reperire e tornare lo stream
     *
     * @param request
     * @param response
     * @param stringToken il token in stringa
     * @param forceDownload indica se forzare lo scaricamento del file settando l'apposito Content-Disposition
     * @throws DownloaderParsingContextException
     * @throws DownloaderDownloadException 
     */
    @RequestMapping(value = "/download", method = RequestMethod.GET)
    public void download(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = true, name = "token") String stringToken,
            @RequestParam(required = true, defaultValue = "false", name = "forceDownload") Boolean forceDownload) throws DownloaderParsingContextException, DownloaderDownloadException {

        DownloaderDownloadPlugin downloadPlugin;
        Map<String, Object> contextClaim;
        try {
            // reperisco il context-claim
            contextClaim = getContextClaimFromToken();
            
            // dal context-claim estraggo le informazioni per instanziare il corretto download plugin e lo reperisco
            downloadPlugin = getDownloadPluginInstance(contextClaim);
        } catch (DownloaderPluginException | ParseException ex) {
            String errorMessage = "errore nel parsing dei parametri per il download del file";
            logger.error(errorMessage, ex);
            throw new DownloaderParsingContextException(errorMessage, ex);
        }

        // reperisco il file e setto gli header, poi copio lo stream nella responde per far partire il download
        try (InputStream file = downloadPlugin.getFile()) {
            setDownloadResponseHeader(response, downloadPlugin, contextClaim, forceDownload);
            IOUtils.copyLarge(file, response.getOutputStream());
        } catch (Exception ex) {
            String errorMessage = "errore nel reperimento del file";
            logger.error(errorMessage, ex);
            throw new DownloaderDownloadException(errorMessage, ex);
        }
    }
    
    /**
     * Servlet chiamata per l'upload di un file.Anche il token è passato in input, non viene usato direttamente:
     * La chiamata è intercettata da un filter (vedi classe it.bologna.ausl.internauta.utils.downloader.authorization.DownloaderRegistrationBean)
     * Questo filter decritta e controlla il token e lo inserisce nel contesto.La servlet estrae il token dal contesto e reperisce il context-claim, che contiene le informazioni sul file da scaricare, grazie alle quali ne può reperire e tornare lo stream
     * 
     *
     * @param request
     * @param response
     * @param stringToken il token in stringa
     * @param file il file da caricare
     * @throws DownloaderParsingContextException 
     * @throws DownloaderUploadException 
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public void upload(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = true, name = "token") String stringToken, 
            @RequestParam("file") MultipartFile file
            ) throws DownloaderParsingContextException, DownloaderUploadException {
        DownloaderUploadPlugin uploadPlugin;
        Map<String, Object> contextClaim;
        try {
            // reperisco il context-claim
            contextClaim = getContextClaimFromToken();
            
            // dal context-claim estraggo le informazioni per istanziare il corretto upload plugin e lo reperisco
            uploadPlugin = getUploadPluginInstance(contextClaim);
            
            String path = null;
            if (contextClaim.containsKey("filePath")) {
                path = (String) contextClaim.get("filePath");
            }
            
            String fileName = null;
            if (contextClaim.containsKey("fileName")) {
                fileName = (String) contextClaim.get("fileName");
            }
            
            // carica il file sul repository
            Map<String, Object> uploadRes = uploadPlugin.putFile(file.getInputStream(), path, fileName);
        } catch (DownloaderPluginException | ParseException ex) {
            String errorMessage = "errore nel parsing dei parametri per il download del file";
            logger.error(errorMessage, ex);
            throw new DownloaderParsingContextException(errorMessage, ex);
        } catch (IOException ex) {
            String errorMessage = "errore nella lettura dello stream file";
            logger.error(errorMessage, ex);
            throw new DownloaderUploadException(errorMessage, ex);
        }
        
        //TODO: devo creare il token e l'url di upload
            
    }

    /**
     * Torna il downloadPlugin adatto per scaricare il file, basandosi sul campo source all'interno dei context-claim estratti dal token di autenticazione
     * @param contextClaim
     * @return l'istanza dell'downloaPlugin
     * @throws ParseException
     * @throws DownloaderPluginException 
     */
    private DownloaderDownloadPlugin getDownloadPluginInstance(Map<String, Object> contextClaim) throws ParseException, DownloaderPluginException {
        
        // per instanziare il corretto downloader leggo il campo source dal context-claim
        DonwloaderPluginFactory.TargetRepository source = DonwloaderPluginFactory.TargetRepository.valueOf((String) contextClaim.get("source"));
        
        // inoltre per istanziarlo ho bisogno anche dei parametri, inserito nel campo params
        Map<String, Object> params = (Map<String, Object>) contextClaim.get("params");
        
        // tramite la factory ottengo l'istanza corretta del downloader.plugin
        DownloaderDownloadPlugin downloadPluginInstance = DonwloaderPluginFactory.getDownloadPlugin(source, params, this.downloaderRepositoryConfiguration.getRepositoryManager());
        return downloadPluginInstance;
    }
    
    /**
     * Torna l'uploadPlugin adatto, basandosi sul campo target all'interno dei context-claim estratti dal token di autenticazione
     * @param contextClaim
     * @return l'istanza dell'uploadPlugin
     * @throws ParseException
     * @throws DownloaderPluginException 
     */
    private DownloaderUploadPlugin getUploadPluginInstance(Map<String, Object> contextClaim) throws ParseException, DownloaderPluginException {
        
        // per instanziare il corretto downloader leggo il campo source dal context-claim
        DonwloaderPluginFactory.TargetRepository target = DonwloaderPluginFactory.TargetRepository.valueOf((String) contextClaim.get("target"));
        
        // inoltre per istanziarlo ho bisogno anche dei parametri, inserito nel campo params
        Map<String, Object> params = (Map<String, Object>) contextClaim.get("params");
        
        // tramite la factory ottengo l'istanza corretta del downloader.plugin
        DownloaderUploadPlugin uploadPluginInstance = DonwloaderPluginFactory.getUploadPlugin(target, params, this.downloaderRepositoryConfiguration.getRepositoryManager());
        return uploadPluginInstance;
    }
    
    /**
     * Estrae i context-claim dal token di autenticazione, reperendolo dal contesto.
     * 
     * Nel contesto viene inserito intercettando la chiamata tramite un filter. 
     * vedi classe it.bologna.ausl.internauta.utils.downloader.authorization.DownloaderRegistrationBean
     * @return i context-claim etratti dal token di autenticazione
     * @throws ParseException 
     */
    private Map<String, Object> getContextClaimFromToken() throws ParseException {
        TokenBasedAuthentication authenticatedApp = (TokenBasedAuthentication) SecurityContextHolder.getContext().getAuthentication();
        SignedJWT token = authenticatedApp.getToken();
        Map<String, Object> context = (Map<String, Object>) token.getJWTClaimsSet().getClaim("context");
        return context;
    } 
    
    /**
     * Setta gli header per il download:
     *  - Content-disposition: 
     *      - se si passa forceDownload = false allora viene inserito inline;filename=... in questo modo si indica al client di visualizzare il file nel suo
     *          visualizzatore (se possibile);
     *      - se si passa forceDownload = true allora viene inserito attachment;filename=.... in questo modo si indica al client far partire il download 
     *      - nel filename viene inserito il nome indicato nel campo fileName del token. Se non viene passato, viene reperito direttamente dal repository
     *  - Content-Type: Se passato nel token attraverso il campo mimeType viene settato, altrimenti non viene inserito l'header
     * @param response
     * @param downloaderPlugin
     * @param contextClaim
     * @param forceDownload
     * @throws DownloaderDownloadException 
     */
    private void setDownloadResponseHeader(HttpServletResponse response, DownloaderDownloadPlugin downloaderPlugin, Map<String, Object> contextClaim, boolean forceDownload) throws DownloaderDownloadException {
        String fileName;
        if (contextClaim.containsKey("fileName") && StringUtils.hasText((String) contextClaim.get("fileName"))) {
            fileName = (String) contextClaim.get("fileName");
        } else {
            fileName = downloaderPlugin.getFileName();
        }
        if (contextClaim.containsKey("mimeType") && StringUtils.hasText((String) contextClaim.get("mimeType"))) {
            String mimeType = (String) contextClaim.get("mimeType");
            response.addHeader("Content-Type", mimeType);
        }
        if (forceDownload) {
            response.addHeader("Content-disposition", "attachment;filename=" + "\"" + fileName + "\"");
        }
        else {
            response.addHeader("Content-disposition", "inline;filename=" + "\"" + fileName + "\"");
        }
    }
}
