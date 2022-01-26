package it.bologna.ausl.internauta.utils.downloader.authorization;

/**
 *
 * @author gdm
 */
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
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

public class JwtFilter extends GenericFilterBean {

    private final String secretKey;
    private final AuthorizationUtils authorizationUtils;

    public JwtFilter(String secretKey, AuthorizationUtils authorizationUtils) {
        super();
        this.secretKey = secretKey;
        this.authorizationUtils = authorizationUtils;
    }

    @Override
    public void doFilter(final ServletRequest req,
            final ServletResponse res,
            final FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;

        if (!request.getMethod().equalsIgnoreCase("OPTIONS")) {

            final String authHeader = request.getHeader("Authorization");
            final String applicazione = request.getHeader("Application");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                setResponseError(req, res, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header.");
                return;
            }

            // la parte dopo "Bearer "
            final String token = authHeader.substring(7);

            try {
                Claims claims = authorizationUtils.setInSecurityContext(token, secretKey, applicazione);
                request.setAttribute("claims", claims);
            } catch (ClassNotFoundException | BlackBoxPermissionException ex) {
                throw new ServletException("Invalid token", ex);
            } catch (ExpiredJwtException ex) {
                logger.warn("token scaduto", ex);
                setResponseError(req, res, HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
                return;
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
        try (PrintWriter writer = res.getWriter()) {
            writer.print(errorMessage);
        }
    }
}
