package it.bologna.ausl.internauta.utils.sendintegration.authorization;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 *
 * @author gdm
 */
public class SendIntegrationJwtFilter extends OncePerRequestFilter {

    private final SendIntegrationAuthorizationUtils authorizationUtils;
    
    public SendIntegrationJwtFilter(SendIntegrationAuthorizationUtils authorizationUtils) {
        this.authorizationUtils = authorizationUtils;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (!request.getMethod().equalsIgnoreCase("OPTIONS")) {
            ZonedDateTime now = ZonedDateTime.now();
            final String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                setResponseError(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Authorization header mancante o non valido");
                return;
            }

            // la parte dopo "Bearer "
            final String token = authHeader.substring(7);
            try  {
                authorizationUtils.verifyTokenAndSetContext(token, now);
            } catch (Exception ex) {
                logger.error("errore nel controllo del token", ex);
                setResponseError(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Accesso non autorizzato");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setResponseError(ServletRequest req, ServletResponse res, int status, String errorMessage) throws IOException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.setStatus(status);
        String headerOrigin = ((HttpServletRequest) req).getHeader("Origin");
        if (StringUtils.hasText(headerOrigin)) {
            response.setHeader("Access-Control-Allow-Origin", headerOrigin);
        }
        try (PrintWriter writer = res.getWriter()) {
            writer.print(errorMessage);
        }
    }

}
