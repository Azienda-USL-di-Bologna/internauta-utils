package it.bologna.ausl.internauta.utils.sendintegration.authorization;

import java.io.IOException;
import java.security.cert.CertificateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In questa classe di configurazione indichiamo i path da proteggere tramite il controlo del token
 * 
 * @author gdm
 */
@Configuration
public class SendIntegrationRegistrationBean {

    @Value("${openapi.send-integration.active:false}")
    private Boolean sendIntegrationActive;
    
    // i path da proteggere sono inseriti all'interno del parametro openapi.send-integration.base-path dell'application.properties dell'applicazione
    @Value("${openapi.send-integration.start-nodes-protection}")
    private String sendIntegrationStartProtection;

    @Autowired
    private SendIntegrationAuthorizationUtils authorizationUtils;

    @Bean
    public FilterRegistrationBean sendIntegrationJwtFilter() throws CertificateException, IOException {

        final FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        if (sendIntegrationActive) {
            // intercetta le chiamate che arrivano da Send
            registrationBean.addUrlPatterns(sendIntegrationStartProtection.split(","));

            registrationBean.setFilter(new SendIntegrationJwtFilter(authorizationUtils));
        }
        return registrationBean;
    }
}