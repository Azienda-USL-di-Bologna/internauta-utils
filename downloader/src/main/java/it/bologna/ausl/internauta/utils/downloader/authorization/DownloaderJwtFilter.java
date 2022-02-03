package it.bologna.ausl.internauta.utils.downloader.authorization;

/**
 *
 * @author gdm
 */
import com.nimbusds.jwt.SignedJWT;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderSecurityException;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

public class DownloaderJwtFilter extends GenericFilterBean {
    
    private final DownloaderAuthorizationUtils authorizationUtils;

    public DownloaderJwtFilter(DownloaderAuthorizationUtils authorizationUtils) {
        super();
        this.authorizationUtils = authorizationUtils;
    }

    @Override
    public void doFilter(final ServletRequest req,
            final ServletResponse res,
            final FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;

        if (!request.getMethod().equalsIgnoreCase("OPTIONS")) {

//            final String authHeader = request.getHeader("Authorization");
            final String authToken = request.getParameter("token");
//            final String applicazione = request.getHeader("Application");

//            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//                setResponseError(req, res, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header.");
//                return;
//            }
            if (!StringUtils.hasText(authToken)) {
                setResponseError(req, res, HttpServletResponse.SC_UNAUTHORIZED, "Missing authorization token.");
                return;
            }

            // la parte dopo "Bearer "
//            final String token = authHeader.substring(7);

            try {
                SignedJWT decodedToken = authorizationUtils.insertInContext(authToken);
                request.setAttribute("decodedToken", decodedToken);
            } catch (DownloaderSecurityException ex) {
                String errorMessage = "problemi di sicurezza nel controllo del token";
                logger.error(errorMessage, ex);
                setResponseError(req, res, HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
                return;
            } catch (Exception ex) {
                String errorMessage = "errore nel controllo del token";
                logger.error(errorMessage, ex);
                throw new ServletException(errorMessage, ex);
            }
        }

        chain.doFilter(req, res);
    }
    


    private void setResponseError(ServletRequest req, ServletResponse res, int status, String errorMessage) throws IOException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.setStatus(status);
        String headerOrigin = ((HttpServletRequest) req).getHeader("Origin");
        if (StringUtils.hasText(headerOrigin)) {
            response.setHeader("Access-Control-Allow-Origin", headerOrigin);
        }
        response.setHeader("Content-Type", "text/plain;Charset=utf-8");
        try (PrintWriter writer = res.getWriter()) {
            writer.print(errorMessage);
        }
    }
}
