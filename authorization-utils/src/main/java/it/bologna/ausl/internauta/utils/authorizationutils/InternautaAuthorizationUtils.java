package it.bologna.ausl.internauta.utils.authorizationutils;

import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.InternautaAuthorizationHttpException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 *
 * @author gdm
 */
@Service
public class InternautaAuthorizationUtils {
    private final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.RS256;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${test-mode:true}")
    private Boolean testMode;

    @Value("classpath:BABEL_TEST.p12")
    private Resource babelP12Test;

    @Value("${babel-p12-test-alias:alias}")
    private String babelP12TestAlias;

    @Value("${babel-p12-test-password:password}")
    private String babelP12TestPassword;

    @Value("classpath:BABEL_PROD.p12")
    private Resource babelP12Prod;

    @Value("${babel-p12-prod-alias:alias}")
    private String babelP12ProdAlias;

    @Value("${babel-p12-prod-password:password}")
    private String babelP12ProdPassword;

    @Value("${internauta-base-url:localhost}")
    private String internautaBaseUrl; // http://localhost:10005/internauta-api

    @Value("${internauta-api-path:internauta-api}") // /endpoint/login
    private String internautaApiPath;
    
    @Value("${internauta-login-path:internauta-login}") // /endpoint/login
    private String internautaLoginPath;
    
    private String generatePreToken(String subject, String codiceAzienda, String codiceRegioneAzienda, String mode) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        KeyStore p12 = KeyStore.getInstance("pkcs12");
        Resource p12Path;
        String p12Alias;
        String p12Psw;
        if (!testMode) {
            p12Path = babelP12Prod;
            p12Alias = babelP12ProdAlias;
            p12Psw = babelP12ProdPassword;
        } else {
            p12Path = babelP12Test;
            p12Alias = babelP12TestAlias;
            p12Psw = babelP12TestPassword;
        }

        p12.load(p12Path.getInputStream(), p12Psw.toCharArray());
        Key secretKey = p12.getKey(p12Alias, p12Psw.toCharArray());

        Claims claims = Jwts.claims();
        claims.setSubject(subject);
//        claims.put("REAL_USER", realUser);
        claims.put("COMPANY", codiceAzienda);
        claims.put("codiceRegioneAzienda", codiceRegioneAzienda);
        claims.put("mode", mode);
        claims.put("FROM_INTERNET", false);

        claims.put("sub", subject);
        claims.put("iss", "internauta-bridge");
//        claims.put("context", context);

        String token = Jwts.builder()
                .setClaims(claims)
                //                .setHeaderParam("x5c", x5c)
                .signWith(SIGNATURE_ALGORITHM, secretKey)
                .compact();

        return token;
    }

    public String getTokenInternauta(String subject, String codiceAzienda, String codiceRegioneAzienda, String mode, String applicazione, boolean useCaching) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, InternautaAuthorizationHttpException {
        // TODO: cacheing del token
        String preToken = generatePreToken(subject, codiceAzienda, codiceRegioneAzienda, mode);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        OkHttpClient client = builder.connectTimeout(2, TimeUnit.MINUTES).readTimeout(2, TimeUnit.MINUTES).writeTimeout(2, TimeUnit.MINUTES).build();
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), objectMapper.writeValueAsString(new InternautaEndpointObject(preToken, applicazione)));
//        String a = request.getScheme() + "://" + UtilityFunctions.getHostname(request) + ":" + request.getServerPort();

//        String internautaLoginCompleteUrl = this.internautaLoginUrl.replace("[base_url]", preToken);
        Request httpRequest = new Request.Builder()
                .url(this.internautaBaseUrl + internautaApiPath + internautaLoginPath)
                .post(body)
                .build();

        Call call = client.newCall(httpRequest);
        Response response = call.execute();
        String res = response.body().string();
        if (!response.isSuccessful()) {
            throw new InternautaAuthorizationHttpException("errore nella generazione del token per la chiamata a internauta: " + res);
//            throw new ServletException("errore nella generazione del token per la chiamata a internauta: " + res);
        }
        Map<String, Object> resMap = objectMapper.readValue(res, new TypeReference<Map<String, Object>>() {
        });
        if (!resMap.containsKey("token")) {
            throw new InternautaAuthorizationHttpException("la chiamata a internauta per la generazione del token non ha restituito un risultato valido");
//            throw new ServletException("la chiamata a internauta per la generazione del token non ha restituito un risultato valido");
        }
        return (String) resMap.get("token");
    }

    public static class InternautaEndpointObject {

        public String jws;
        public String applicazione;

        public InternautaEndpointObject(String jws, String applicazione) {
            this.jws = jws;
            this.applicazione = applicazione;
        }
    }
}
