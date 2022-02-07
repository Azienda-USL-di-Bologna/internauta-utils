package it.bologna.ausl.internauta.utils.downloader.controllers;

import com.nimbusds.jwt.SignedJWT;
import it.bologna.ausl.internauta.utils.downloader.authorization.TokenBasedAuthentication;
import it.bologna.ausl.internauta.utils.downloader.plugin.DonwloaderPluginFactory;
import it.bologna.ausl.internauta.utils.downloader.plugin.DownloaderPlugin;
import java.text.ParseException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
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
        @RequestMapping(value = "/download", method = RequestMethod.GET)
        public void download(HttpServletRequest request,
                @RequestParam(required = true, name = "token") String stringToken) throws ParseException {
            TokenBasedAuthentication authenticatedApp = (TokenBasedAuthentication) SecurityContextHolder.getContext().getAuthentication();
            SignedJWT token = authenticatedApp.getToken();
            Map<String, Object> claim = (Map<String, Object>) token.getJWTClaimsSet().getClaim("context");
            DonwloaderPluginFactory.getDownloaderPlugin(DonwloaderPluginFactory.Source.Mongo);
            System.out.println("download");
    }
}
