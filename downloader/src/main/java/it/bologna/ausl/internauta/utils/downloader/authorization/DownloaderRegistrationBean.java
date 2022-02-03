package it.bologna.ausl.internauta.utils.downloader.authorization;

import java.io.IOException;
import java.security.cert.CertificateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author gdm
 */
@Configuration
public class DownloaderRegistrationBean {

    @Value("${downloader.security.start-nodes-protection}")
    private String startNodesProtection;

    @Autowired
    private DownloaderAuthorizationUtils authorizationUtils;

    @Bean
    public FilterRegistrationBean downloaderJwtFilter() throws CertificateException, IOException {

        final FilterRegistrationBean registrationBean = new FilterRegistrationBean();

        registrationBean.setFilter(new DownloaderJwtFilter(authorizationUtils));

        // intercetta tutte le chiamate che iniziano per...
        registrationBean.addUrlPatterns(startNodesProtection.split(","));

        return registrationBean;
    }
}
