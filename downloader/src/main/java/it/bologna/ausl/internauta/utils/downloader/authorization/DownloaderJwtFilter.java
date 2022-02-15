package it.bologna.ausl.internauta.utils.downloader.authorization;

/**
 * Questa classe di occupa di intercettare tutte le chiamate, leggere il token dai query params, controllarlo e, se valido, inserirlo nel contesto
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

    /**
     * Intercetta tramite un filter la chiamata, controlla il token e, se valido, lo inserisce nel contesto.
     * Se il token non è valido, la chiamata viene interrotta tornando errore.
     * @param req
     * @param res
     * @param chain
     * @throws IOException
     * @throws ServletException 
     */
    @Override
    public void doFilter(final ServletRequest req,
            final ServletResponse res,
            final FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;

        // le chiamate di tipo Options non vengono controllate
        if (!request.getMethod().equalsIgnoreCase("OPTIONS")) {

            // legge il token dai query params della request
            final String authToken = request.getParameter("token");

            if (!StringUtils.hasText(authToken)) {
                setResponseError(req, res, HttpServletResponse.SC_UNAUTHORIZED, "Missing authorization token.");
                return;
            }

            try {
                // chiama la funzione authorizationUtils.insertInContext, che controlla il token e se valido lo inserisce nel contesto
                SignedJWT decodedToken = authorizationUtils.insertInContext(authToken);
                
                // inoltre il token decodificato viene inserito come attributo "decodedToken" nella request, nel caso dovesse servire
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
        
        // se la chiamata aveva l'header Origin, lo inserisco anche nella response
        String headerOrigin = ((HttpServletRequest) req).getHeader("Origin");
        if (StringUtils.hasText(headerOrigin)) {
            response.setHeader("Access-Control-Allow-Origin", headerOrigin);
        }
        
        // setto il Content-Type a text/plain;Charset=utf-8 visto che l'errore sarà solo testo semplice
        response.setHeader("Content-Type", "text/plain;Charset=utf-8");
        try (PrintWriter writer = res.getWriter()) {
            writer.print(errorMessage);
        }
    }
}
