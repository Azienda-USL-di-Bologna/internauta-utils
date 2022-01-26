package it.bologna.ausl.internauta.utils.downloader.authorization;

import java.io.IOException;
import java.security.cert.CertificateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 *
 * @author gdm
 */
@Configuration
public class RegistrationBean {

    @Value("${internauta.security.start-nodes-protection}")
    private String startNodesProtection;
    
    @Value("${internauta.security.passtoken-path}")
    private String passTokenPath;
    
    @Value("${security.logout.path}")
    private String logoutPath;
    
    @Value("${security.refresh-session.path}")
    private String refreshSessionPath;

    @Value("${jwt.secret:secret}")
    private String secretKey;

    @Autowired
    private AuthorizationUtils authorizationUtils;

    @Bean
    public FilterRegistrationBean jwtFilter() throws CertificateException, IOException {

        final FilterRegistrationBean registrationBean = new FilterRegistrationBean();

        registrationBean.setFilter(new JwtFilter(secretKey, authorizationUtils));

        // intercetta tutte le chiamate che iniziano per...
        registrationBean.addUrlPatterns(startNodesProtection.split(","));

        return registrationBean;
    }
}
