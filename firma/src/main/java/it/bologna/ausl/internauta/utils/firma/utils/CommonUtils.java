package it.bologna.ausl.internauta.utils.firma.utils;

import javax.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
public class CommonUtils {
        public static String getHostname(HttpServletRequest request) {

        String res;
        String header = request.getHeader("X-Forwarded-Host");
        // TODO: non è detto che vada bene tornare sempre il primo elemento, bisognerebbe controllare che il Path dell'azienda matchi con uno qualsiasi degli elementi
        if (StringUtils.hasText(header)) {
            String[] headerToken = header.split(",");
            res = headerToken[0];
        } else {
            res = request.getServerName();
        }
        return res;
    }
}
