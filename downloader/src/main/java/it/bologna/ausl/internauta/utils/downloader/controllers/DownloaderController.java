package it.bologna.ausl.internauta.utils.downloader.controllers;

import com.nimbusds.jwt.SignedJWT;
import it.bologna.ausl.internauta.utils.downloader.authorization.DownloaderAuthorizationUtils;
import it.bologna.ausl.internauta.utils.downloader.authorization.TokenBasedAuthentication;
import it.bologna.ausl.internauta.utils.downloader.configuration.DownloaderRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderDownloadException;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderParsingContextException;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderPluginException;
import it.bologna.ausl.internauta.utils.downloader.plugin.DonwloaderPluginFactory;
import it.bologna.ausl.internauta.utils.downloader.plugin.DownloaderPlugin;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Map;
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

        DownloaderPlugin downloaderPlugin;
        Map<String, Object> contextClaim;
        try {
            // reperisco il context-claim
            contextClaim = getContextClaimFromToken();
            
            // dal context-claim estraffo le informazioni per instanziare il corretto downloader plugin e lo reperisco
            downloaderPlugin = getDownloaderInstance(contextClaim);
        } catch (DownloaderPluginException | ParseException ex) {
            String errorMessage = "errore nel parsing dei parametri per il download del file";
            logger.error(errorMessage, ex);
            throw new DownloaderParsingContextException(errorMessage, ex);
        }

        // reperisco il file e setto gli header, poi copio lo stream nella responde per far partire il download
        try (InputStream file = downloaderPlugin.getFile()) {
            setDownloadResponseHeader(response, downloaderPlugin, contextClaim, forceDownload);
            IOUtils.copyLarge(file, response.getOutputStream());
        } catch (Exception ex) {
            String errorMessage = "errore nel reperimento del file";
            logger.error(errorMessage, ex);
            throw new DownloaderDownloadException(errorMessage, ex);
        }
    }

    /**
     * Torna il downloaderPlugin adatto per scaricare il file, basandosi sul campo source all'interno dei context-claim estratti dal token di autenticazione
     * @param contextClaim
     * @return
     * @throws ParseException
     * @throws DownloaderPluginException 
     */
    private DownloaderPlugin getDownloaderInstance(Map<String, Object> contextClaim) throws ParseException, DownloaderPluginException {
        
        // pèer instanziare il corretto downloader leggo il campo source dal context-claim
        DonwloaderPluginFactory.Source source = DonwloaderPluginFactory.Source.valueOf((String) contextClaim.get("source"));
        
        // inoltre per istanziarlo ho bisogno anche dei parametri, inserito nel campo params
        Map<String, Object> params = (Map<String, Object>) contextClaim.get("params");
        
        // tramite la factory ottengo l'istanza corretta del downloader.plugin
        DownloaderPlugin downloaderInstance = DonwloaderPluginFactory.getDownloaderPlugin(source, params, this.downloaderRepositoryConfiguration.getRepositoryManager());
        return downloaderInstance;
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
    private void setDownloadResponseHeader(HttpServletResponse response, DownloaderPlugin downloaderPlugin, Map<String, Object> contextClaim, boolean forceDownload) throws DownloaderDownloadException {
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
