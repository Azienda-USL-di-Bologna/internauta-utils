package it.bologna.ausl.internauta.utils.downloader.authorization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Classe di configurazione che setta i cors da accettare sulle chiamate
 * @author gdm
 */
@Configuration
public class DownloaderRestConfiguration {

    @Value("${downloader.cors.allowed.origins}")
    private String allowedOriginsString;

    /**
     * Accetto solo richieste Options, Get, e Post e solo gli header application authorization e content-type
     * e solo richieste provenienti dagli indirizzi inserito nel parametro downloader.cors.allowed.origins inserito nell'application.properties dell'applicazione
     * @return 
     */
    @Bean
    public CorsFilter downloaderCorsFilter() {

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration configResources = new CorsConfiguration();
//        configResources.setAllowCredentials(false); // con la versione 2.4.2 di spring se settato a true vanno per forza settati anche gli allowed orgin a qualcosa diversa da *
        configResources.setAllowCredentials(true); // con la versione 2.4.2 di spring se settato a true vanno per forza settati anche gli allowed orgin a qualcosa diversa da *
        List<String> allowedOriginList = new ArrayList<>(Arrays.asList(allowedOriginsString.split(",")));
        configResources.setAllowedOrigins(allowedOriginList);

        configResources.addAllowedHeader("application");
        configResources.addAllowedHeader("authorization");
        configResources.addAllowedHeader("content-type");
        configResources.addAllowedMethod(HttpMethod.OPTIONS);
        configResources.addAllowedMethod(HttpMethod.GET);
        configResources.addAllowedMethod(HttpMethod.POST);


        source.registerCorsConfiguration("/downloader-api/**", configResources);
        return new CorsFilter(source);
    }
}
