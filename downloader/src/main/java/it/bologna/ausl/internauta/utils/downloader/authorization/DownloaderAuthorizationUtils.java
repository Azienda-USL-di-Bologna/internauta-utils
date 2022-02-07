package it.bologna.ausl.internauta.utils.downloader.authorization;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderSecurityException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javafx.util.Pair;
import javax.annotation.PostConstruct;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class DownloaderAuthorizationUtils {
    
    private static Logger logger = LoggerFactory.getLogger(DownloaderAuthorizationUtils.class);
    
    @Value("${downloader.mode:test}")
    private String downloaderMode;
    
    
    @Value("classpath:downloader/Internauta_Downloader_Encription_Private_Key.pk8")
    private Resource tokenDecripterPrivateKeyProd;
    
    @Value("classpath:downloader/Internauta_Downloader_Encription_Private_Key_Test.pk8")
    private Resource tokenDecripterPrivateKeyTest;
    
    @Value("classpath:downloader/DOWNLOADER_BABEL.crt")
    private Resource downloaderPublicCertBabelProd;

    @Value("classpath:downloader/DOWNLOADER_TEST.crt")
    private Resource downloaderPublicCertBabelTest;
    
    
    // la mappa contiene come chiave lo SHA-256 della chiave pubblica contenuta usata per firmare il token e come valore una coppia: common name del certificato - chiave pubblica estratta dal certificato
    public Map<String, Pair<String, PublicKey>> hashPublicKeyMap = new HashMap();
    
    private Resource tokenDecripterPrivateKey;
    
    @PostConstruct
    public void initialize() throws DownloaderSecurityException, FileNotFoundException, IOException, CertificateEncodingException {
        switch (downloaderMode.toLowerCase()) {
            case "test":
                this.tokenDecripterPrivateKey = this.tokenDecripterPrivateKeyTest;
                // qui ci vanno tutti i certificati di test
                
                // certificato di test
                X509Certificate publicCertBabelTest = getX509CertificateFromFile(this.downloaderPublicCertBabelTest.getFile());
                hashPublicKeyMap.put("FDB1F11965344A44DB32C4FE1D53C4A5104453BAEFB58F106BD6ABDD4736537B", 
                        new Pair(getCommonNameFromX509Certificate(publicCertBabelTest), publicCertBabelTest.getPublicKey()));
                break;
            case "prod":
                this.tokenDecripterPrivateKey = this.tokenDecripterPrivateKeyProd;
                
                // qui ci vanno tutti i certificati di prod
                
                // certificato interno nostro per prod
                X509Certificate publicCertBabelProd = getX509CertificateFromFile(this.downloaderPublicCertBabelProd.getFile());
                this.hashPublicKeyMap.put("819EE5A635FA45FBCED93BEE6ED0B9721C02A9B933328806DDFF84CD5AA9DD42",
                        new Pair(getCommonNameFromX509Certificate(publicCertBabelProd), publicCertBabelProd.getPublicKey()));
                break;
            default:
                String errorMessage = String.format("downloader mode deve essere \"%s\" o \"%s\". Valore trovato \"%s\"", "test", "prod", downloaderMode);
                logger.error(errorMessage);
                throw new DownloaderSecurityException(errorMessage);
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
    
    private X509Certificate getX509CertificateFromFile(File certFile) throws FileNotFoundException, IOException {
        try (
            FileReader keyReader = new FileReader(certFile);
            PemReader pemReader = new PemReader(keyReader)) {
            
            X509Certificate cert = X509CertUtils.parse(pemReader.readPemObject().getContent());
            return cert;
        }
    }

    private String getCommonNameFromX509Certificate(X509Certificate cert) throws CertificateEncodingException {
        X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
        RDN cn = x500name.getRDNs(BCStyle.CN)[0];

        String cnString = IETFUtils.valueToString(cn.getFirst().getValue());
        return cnString;
    }
    
    private SignedJWT checkAndDecodeToken(String encryptedToken) throws DownloaderSecurityException, ParseException, NoSuchAlgorithmException, IOException, FileNotFoundException, InvalidKeyException, InvalidKeySpecException, JOSEException {
        switch (downloaderMode.toLowerCase()) {
            case "test":
                break;
            case "prod":
                break;
            default:
                String errorMessage = String.format("downloader mode deve essere \"%s\" o \"%s\". Valore trovato \"%s\"", "test", "prod", downloaderMode);
                logger.error(errorMessage);
                throw new DownloaderSecurityException(errorMessage);
        }
        
         // Parse the JWE string
        JWEObject jweObject = JWEObject.parse(encryptedToken);

        // Decrypt with private key
        jweObject.decrypt(new RSADecrypter(getEncryptPrivateKey()));

        // Extract payload
        SignedJWT signedJWTDecrypted = jweObject.getPayload().toSignedJWT();

        String keyID = signedJWTDecrypted.getHeader().getKeyID();
        String cn = this.hashPublicKeyMap.get(keyID).getKey();
        PublicKey pk = this.hashPublicKeyMap.get(keyID).getValue();
        
        // Check the signature
        if (!signedJWTDecrypted.verify(new RSASSAVerifier((RSAPublicKey) pk))) {
            String errorMessage = "la firma del token non è valida";
            logger.error(errorMessage);
            throw new DownloaderSecurityException(errorMessage);
        }
        String subject = signedJWTDecrypted.getJWTClaimsSet().getSubject();
        if (!cn.equalsIgnoreCase(subject)) {
            String errorMessage = "il subject del token non corrisponde al common name del certificato";
            logger.error(String.format(errorMessage + " subject: %s - common name: %s", subject, cn));
            throw new DownloaderSecurityException(errorMessage);
        }
        Date expirationTime = signedJWTDecrypted.getJWTClaimsSet().getExpirationTime();
        if (expirationTime == null || new Date().after(expirationTime)) {
            String errorMessage = "il token è scaduto";
            logger.error(errorMessage);
            throw new DownloaderSecurityException(errorMessage);
        }
        return signedJWTDecrypted;
    }
    
    public SignedJWT insertInContext(String token) throws  DownloaderSecurityException{
        SignedJWT decodedToken;
        try {
            decodedToken = checkAndDecodeToken(token);
        } catch (DownloaderSecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            String errorMessage = "errore nel controllo del token";
            logger.error(errorMessage, ex);
            throw new DownloaderSecurityException(errorMessage, ex);
        }
        String subject;
        try {
            subject = decodedToken.getJWTClaimsSet().getSubject();
        } catch (ParseException ex) {
            String errorMessage = "errore nell'estrazione del subject dal token";
            logger.error(errorMessage, ex);
            throw new DownloaderSecurityException(errorMessage, ex);
        }
        
        AuthenticatedApplication authenticatedApplication = new AuthenticatedApplication(subject);
        
        TokenBasedAuthentication authentication = new TokenBasedAuthentication(decodedToken, authenticatedApplication);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        return decodedToken;
    }
}
