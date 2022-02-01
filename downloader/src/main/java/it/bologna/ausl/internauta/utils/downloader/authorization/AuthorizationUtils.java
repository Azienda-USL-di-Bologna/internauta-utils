package it.bologna.ausl.internauta.utils.downloader.authorization;

import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderSecurityException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.util.Formatter;
import javax.annotation.PostConstruct;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
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
    
    @Value("classpath:DOWNLOADER_BABEL.crt")
    private Resource downloaderPublicCertProd;

    @Value("classpath:DOWNLOADER_TEST.crt")
    private Resource downloaderPublicCertTest;
    
    @Value("classpath:Internauta_Downloader_Encription_Private_Key.pk8")
    private Resource tokenDecripterPrivateKeyProd;
    
    @Value("classpath:Internauta_Downloader_Encription_Private_Key_Test.pk8")
    private Resource tokenDecripterPrivateKeyTest;
    
    private Resource tokenDecripterPrivateKey;
    private Resource downloaderPublicCert;
    
    @PostConstruct
    public void initialize() throws DownloaderSecurityException {
        switch (downloaderMode.toLowerCase()) {
            case "test":
                this.tokenDecripterPrivateKey = this.tokenDecripterPrivateKeyTest;
                this.downloaderPublicCert = this.downloaderPublicCertTest;
                break;
            case "prod":
                this.tokenDecripterPrivateKey = this.tokenDecripterPrivateKeyProd;
                this.downloaderPublicCert = this.downloaderPublicCertProd;
                break;
            default:
                throw new DownloaderSecurityException(String.format("downloader mode deve essere \"%s\" o \"%s\". Valore trovato \"%s\"", "test", "prod", downloaderMode));
        }
    }
    
    private PrivateKey getEncryptPrivateKey() throws NoSuchAlgorithmException, FileNotFoundException, IOException, InvalidKeyException, InvalidKeySpecException {
        KeyFactory factory = KeyFactory.getInstance("RSA");
        try (
            FileReader keyReader = new FileReader(tokenDecripterPrivateKey.getFile());
            PemReader pemReader = new PemReader(keyReader)) {

            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
            PrivateKey privateKey = factory.generatePrivate(privKeySpec);
            return privateKey;
        }
    }
    
    public PublicKey getJwsPublicKey() throws Exception {
        try (
            FileReader keyReader = new FileReader(downloaderPublicCert.getFile());
            PemReader pemReader = new PemReader(keyReader)) {
            
            X509Certificate cert = X509CertUtils.parse(pemReader.readPemObject().getContent());
            return cert.getPublicKey();
        }
    }

    
    public SignedJWT decodeToken(String encryptedToken) throws DownloaderSecurityException, ParseException, Exception {
        switch (downloaderMode.toLowerCase()) {
            case "test":
                break;
            case "prod":
                break;
            default:
                throw new DownloaderSecurityException(String.format("downloader mode deve essere \"%s\" o \"%s\". Valore trovato \"%s\"", "test", "prod", downloaderMode));
        }
        
         // Parse the JWE string
        JWEObject jweObject = JWEObject.parse(encryptedToken);

        // Decrypt with private key
        jweObject.decrypt(new RSADecrypter(getEncryptPrivateKey()));

        // Extract payload
        SignedJWT signedJWTDecrypted = jweObject.getPayload().toSignedJWT();

        // Check the signature
        if (!signedJWTDecrypted.verify(new RSASSAVerifier((RSAPublicKey) getJwsPublicKey()))) {
            throw new DownloaderSecurityException("la firma del token non è valida");
        }
        return signedJWTDecrypted;
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
