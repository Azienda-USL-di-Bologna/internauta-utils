package it.bologna.ausl.internauta.utils.downloader.authorization;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderSecurityException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class AuthorizationUtils {
    
    @Value("${downloader.mode:test}")
    private String downloaderMode;
    
    @Value("classpath:DOWNLOADER.crt")
    private Resource downloaderPublicCertProd;

    @Value("classpath:DOWNLOADER_TEST.crt")
    private Resource downloaderPublicCertTest;
    
    @Value("classpath:Internauta_Downloader_Encription_Private_Key.pem")
    private Resource tokenDecripterPrivateKey;
    
    @Value("classpath:Internauta_Downloader_Encription_Private_Key_Test.pem")
    private Resource tokenDecripterPrivateKeyTest;
    
    private tokenDecripterPrivateKeyTest;
    
    public void decodeToken() {
        switch (downloaderMode.toLowerCase()) {
            case "test":
                break;
            case "prod":
                break;
            default:
                throw new DownloaderSecurityException(String.format("downloader mode deve essere \"%s\" o \"%s\". Valore trovato \"%s\"", "test", "prod", downloaderMode));
        }
        File publicKeyFile = new File("public.key");
        byte[] publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath());
    }
    
    public void insertInContext(String token) {
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        
        Claims claims = Jwts.parser().
                setSigningKey(secretKey).
                parseClaimsJws(token).
                getBody();
//        logger.info("insertInContext fromInternet: " + fromInternet);
        TokenBasedAuthentication authentication;
        Applicazioni applicazione = Applicazioni.valueOf(idApplicazione);
        if (realUser != null) {
            authentication = new TokenBasedAuthentication(user, realUser, applicazione, fromInternet);
        } else {
            authentication = new TokenBasedAuthentication(user, applicazione, fromInternet);
        }
        authentication.setToken(token);
        authentication.setIdSessionLog(idSessionLog);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
