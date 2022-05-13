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
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.tuple.Pair;
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
 * 
 * Questa classe effettua il controllo del token ricevuto nella chiamata. Lo Decripta e poi controlla la firma del token jws
 */
@Component
public class DownloaderAuthorizationUtils {
    
    private static Logger logger = LoggerFactory.getLogger(DownloaderAuthorizationUtils.class);
    
    @Value("${downloader.mode:test}")
    private String downloaderMode;
    
    @Value("${downloader.private-key-file.location}")
    private String privateKeyFileLocation;
    
//    @Value("classpath:downloader/Internauta_Downloader_Encription_Private_Key.pk8")
//    private Resource tokenDecripterPrivateKeyProd;
//    
//    @Value("classpath:downloader/Internauta_Downloader_Encription_Private_Key_Test.pk8")
//    private Resource tokenDecripterPrivateKeyTest;
    
    @Value("${downloader.public-cert-babel-prod}")
    private Resource downloaderPublicCertBabelProd;

    @Value("${downloader.public-cert-babel-test}")
    private Resource downloaderPublicCertBabelTest;

    @Value("${downloader.max-limit-token-seconds:30}")
    private Integer maxLimitTokenSeconds = 86400; 
    
    // la mappa contiene come chiave lo SHA-256 della chiave pubblica contenuta usata per firmare il token e come valore una coppia: common name del certificato - chiave pubblica estratta dal certificato
    public Map<String, Pair<String, PublicKey>> hashPublicKeyMap = new HashMap();
    
    @PostConstruct // questa annotazione fa si che questo metodo venga eseguito subito dopo che spring istanzia questa classe
    public void initialize() throws DownloaderSecurityException, FileNotFoundException, IOException, CertificateEncodingException {
        switch (downloaderMode.toLowerCase()) {
            case "test": // se sono in modalità di test prendo la chiave di test per decrittare il token e il certificato di test per controllare la firma
//                this.tokenDecripterPrivateKey = this.tokenDecripterPrivateKeyTest;
                // qui ci vanno tutti i certificati di test
                
                // certificato di test
//                X509Certificate publicCertBabelTest = getX509CertificateFromFile(new File("downloader/DOWNLOADER_TEST.crt"));
                X509Certificate publicCertBabelTest = getX509CertificateFromFile(this.downloaderPublicCertBabelTest.getInputStream());
                hashPublicKeyMap.put("FDB1F11965344A44DB32C4FE1D53C4A5104453BAEFB58F106BD6ABDD4736537B", 
                        Pair.of(getCommonNameFromX509Certificate(publicCertBabelTest), publicCertBabelTest.getPublicKey()));
                hashPublicKeyMap.put("546B45E5E5190F9909467052C63FAD59067DE9B6AEC45E7D8E4BDE742FF2F195", 
                        Pair.of(getCommonNameFromX509Certificate(publicCertBabelTest), publicCertBabelTest.getPublicKey()));
                break;
            case "prod": // se sono in modalità di prod, prendo al chiave di prod per decrittare il token e il certificato di prod per controllare la firma
//                this.tokenDecripterPrivateKey = this.tokenDecripterPrivateKeyProd;
                
                // qui ci vanno tutti i certificati di prod
                
                // certificato interno nostro per prod
                X509Certificate publicCertBabelProd = getX509CertificateFromFile(this.downloaderPublicCertBabelProd.getInputStream());
                this.hashPublicKeyMap.put("819EE5A635FA45FBCED93BEE6ED0B9721C02A9B933328806DDFF84CD5AA9DD42",
                        Pair.of(getCommonNameFromX509Certificate(publicCertBabelProd), publicCertBabelProd.getPublicKey()));
                break;
            default:
                String errorMessage = String.format("downloader mode deve essere \"%s\" o \"%s\". Valore trovato \"%s\"", "test", "prod", downloaderMode);
                logger.error(errorMessage);
                throw new DownloaderSecurityException(errorMessage);
        }
    }
    
    /**
     * Legge dal file corretto, la chiave privata per decrittare il token
     * @return l'oggetto PrivateKey che rappresenta la chiave privata per decrittare il token
     * @throws NoSuchAlgorithmException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException 
     */
    private PrivateKey getEncryptPrivateKey() throws NoSuchAlgorithmException, FileNotFoundException, IOException, InvalidKeyException, InvalidKeySpecException {
        KeyFactory factory = KeyFactory.getInstance("RSA");
        try (
//            FileReader keyReader = new FileReader(tokenDecripterPrivateKey.getFile());
            FileReader keyReader = new FileReader(new File(this.privateKeyFileLocation));
            PemReader pemReader = new PemReader(keyReader)) {

            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
            PrivateKey privateKey = factory.generatePrivate(privKeySpec);
            return privateKey;
        }
    }
    
    /**
     * Legge dal file il certificato per il controllo della firma del token
     * @param certFile
     * @return
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private X509Certificate getX509CertificateFromFile(InputStream certFile) throws FileNotFoundException, IOException {
        try (
            InputStreamReader keyReader = new InputStreamReader(certFile);
            PemReader pemReader = new PemReader(keyReader)) {
            
            X509Certificate cert = X509CertUtils.parse(pemReader.readPemObject().getContent());
            return cert;
        }
    }

    /**
     * Estrae il common name dal certificato
     * @param cert
     * @return
     * @throws CertificateEncodingException 
     */
    private String getCommonNameFromX509Certificate(X509Certificate cert) throws CertificateEncodingException {
        X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
        RDN cn = x500name.getRDNs(BCStyle.CN)[0];

        String cnString = IETFUtils.valueToString(cn.getFirst().getValue());
        return cnString;
    }
    
    /**
     * Decripta, controlla la firma del token.
     * Inoltre controlla che il subject del token sia uguale common name del certificato dal quale ho estratto la chiave pubblica usata per controllare la firma
     * @param encryptedToken
     * @return il token in formato SignedJWT
     * @throws DownloaderSecurityException
     * @throws ParseException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws FileNotFoundException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     * @throws JOSEException 
     */
    private SignedJWT checkAndDecodeToken(String encryptedToken) throws DownloaderSecurityException, ParseException, NoSuchAlgorithmException, IOException, FileNotFoundException, InvalidKeyException, InvalidKeySpecException, JOSEException {
        
        // Parso la stringa che rappresenta il token criptato (jwe)
        JWEObject jweObject = JWEObject.parse(encryptedToken);

        // decritto il token usando la mia chiave privata
        jweObject.decrypt(new RSADecrypter(getEncryptPrivateKey()));

        // Estraggo il payload che contiene il token firmato (jws)
        SignedJWT signedJWTDecrypted = jweObject.getPayload().toSignedJWT();

        // nel keyID dell'header c'è l'hash (SHA-256) della chiave pubblica corrispondente alla chiava privata usata per firmare il token
        // lo estraggo e lo uso per trovare la chiave pubblica nella mia mappa "hashPublicKeyMap"
        String keyID = signedJWTDecrypted.getHeader().getKeyID();
        String cn = this.hashPublicKeyMap.get(keyID).getKey();
        PublicKey pk = this.hashPublicKeyMap.get(keyID).getValue();
        
        // Controllo la firma con la chiave pubblica estratta sopra
        if (!signedJWTDecrypted.verify(new RSASSAVerifier((RSAPublicKey) pk))) {
            String errorMessage = "la firma del token non è valida";
            logger.error(errorMessage);
            throw new DownloaderSecurityException(errorMessage);
        }
        
        // controllo che il subject del token sia uguale common name del certificato dal quale ho estratto la chiave pubblica
        String subject = signedJWTDecrypted.getJWTClaimsSet().getSubject();
        if (!cn.equalsIgnoreCase(subject)) {
            String errorMessage = "il subject del token non corrisponde al common name del certificato";
            logger.error(String.format(errorMessage + " subject: %s - common name: %s", subject, cn));
            throw new DownloaderSecurityException(errorMessage);
        }
        
        // controllo che il token non sia scaduto oppure che la data di scadenza sia troppo elevata
        Date expirationTime = signedJWTDecrypted.getJWTClaimsSet().getExpirationTime();
        Date maxLimitTokenTime = Date.from(LocalDateTime.now().plusSeconds(this.maxLimitTokenSeconds).atZone(ZoneId.systemDefault()).toInstant());
        if (expirationTime == null || new Date().after(expirationTime)) {
            String errorMessage = "il token è scaduto";
            logger.error(errorMessage);
            throw new DownloaderSecurityException(errorMessage);
        } else if (expirationTime.after(maxLimitTokenTime)) {
            String errorMessage = String.format("la scadenza del token è troppo lunga, il limite massimo è %s secondi", maxLimitTokenSeconds);
            logger.error(errorMessage);
            throw new DownloaderSecurityException(errorMessage);
        }
        return signedJWTDecrypted;
    }
    
    /**
     * Inserisce il token, dovo averlo parsato e controllato, nel contesto, dentro l'oggetto TokenBasedAuthentication
     * @param token
     * @return Il token parsato e controllato, dopo averlo inserito nel contesto
     * @throws DownloaderSecurityException 
     */
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
