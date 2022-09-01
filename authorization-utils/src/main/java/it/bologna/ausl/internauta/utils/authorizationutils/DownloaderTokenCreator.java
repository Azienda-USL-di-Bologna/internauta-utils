package it.bologna.ausl.internauta.utils.authorizationutils;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

/**
 *
 * @author gdm
 */
public class DownloaderTokenCreator {

    public DownloaderTokenCreator() {
    }
    
    /**
     * Torna la chiave privata per la firma del token
     * @param keyPath il path del file p12 contenente la chiave
     * @param keyName il nome della chiave all'interno del p12
     * @param keyPassword la password per aprire il file e la chiave
     * @return
     * @throws it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException
     */
    public PrivateKey getSignTokenPrivateKey(String keyPath, String keyName, String keyPassword) throws AuthorizationUtilsException {
        try {
            KeyStore p12 = KeyStore.getInstance("pkcs12");
            p12.load(new FileInputStream(keyPath), keyPassword.toCharArray());
//        p12.load(new FileInputStream("DOWNLOADER_TEST.p12"), "siamofreschi".toCharArray());
//        p12.load(new FileInputStream("DOWNLOADER_BABEL.p12"), "LIDa8flBxS3gFhwdHLxX".toCharArray());
            Key secretKey = p12.getKey(keyName, keyPassword.toCharArray());
//        Key secretKey = p12.getKey("DOWNLOADER BABEL", "LIDa8flBxS3gFhwdHLxX".toCharArray());
//        Key secretKey = p12.getKey("DOWNLOADER TEST", "siamofreschi".toCharArray());

            return (PrivateKey) secretKey;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException ex) {
            throw new AuthorizationUtilsException("errore nel parsin della chiave privata per la firam del token", ex);
        }
    }
    
    /**
     * Torna un X509Certificate dal file passato in input
     * @param certFile il file del certificato
     * @return
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public X509Certificate getX509Certificate(InputStream certFile) throws FileNotFoundException, IOException {
        // 
        X509Certificate cert = X509CertUtils.parse(new PemReader(new InputStreamReader(certFile)).readPemObject().getContent());
//        X509Certificate cert = X509CertUtils.parse(new PemReader(new FileReader("DOWNLOADER_TEST.crt")).readPemObject().getContent());
         
//        X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
//        RDN cn = x500name.getRDNs(BCStyle.CN)[0];
//
//        String valueToString = IETFUtils.valueToString(cn.getFirst().getValue());
//        System.out.println("common name: " + valueToString); 
        
        return cert;
    }
    
    /**
     * Estrae il common name dal certificato
     * @param cert il certificato dal quale etrarre il common-name
     * @return il common name estratto dal certificato passato in input
     * @throws CertificateEncodingException 
     */
    private String getCommonNameFromX509Certificate(X509Certificate cert) throws CertificateEncodingException {
        X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
        RDN cn = x500name.getRDNs(BCStyle.CN)[0];

        String cnString = IETFUtils.valueToString(cn.getFirst().getValue());
        return cnString;
    }
    
    /**
     * Torna l'oggetto RSAPublicKey dal file in input. Da usare per la cifratura del token
     * @param publicKeyFile
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException 
     */
    public RSAPublicKey getEncryptionPublicKey(InputStream publicKeyFile) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        try (
            InputStreamReader keyReader = new InputStreamReader(publicKeyFile);
            PemReader pemReader = new PemReader(keyReader)) {
            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(content);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            return (RSAPublicKey) publicKey;
        }
    }

    
    /**
     * Crea il token di autorizzazione per il downloader.
     * @param context il context per il downloader/uploader da inserire nel token
     * @param publicCertFile il file del certificato contenente la chiave pubblica per il controllo della firma del token.
     * Serve per reperire il common-name e calcolare l'hash della chiave pubblica. Questi dati venno inseriti nel token.
     * @param singTokenPrivateKey chiave per la firma del token (corrispondente alla chiave pubblica del certificato passato nel parametro "publicCertPath").
     * Reperibile tramite il metodo getSignTokenPrivateKey() 
     * @param tokenEncryptionPublickey chiave pubblica per la cifratura del token, reperibile tramite il metodo getEncryptionPublicKey()
     * @param expirationSeconds durata del token in secondi
     * @param issuer issuer da inserire nel token
     * @return il token da passare come query-param nella chiamata al downloader
     * @throws it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException
     */
    public String getToken(
            Map<String, Object> context, 
            InputStream publicCertFile, 
            PrivateKey singTokenPrivateKey, 
            RSAPublicKey tokenEncryptionPublickey, 
            Integer expirationSeconds, 
            String issuer) throws AuthorizationUtilsException {
        X509Certificate publicCert;
        Key jwsPublicKey;
        String commonName;
        try {
            publicCert = getX509Certificate(publicCertFile);
            jwsPublicKey = publicCert.getPublicKey();
            commonName = getCommonNameFromX509Certificate(publicCert);
        } catch (IOException | CertificateEncodingException ex) {
            throw new AuthorizationUtilsException("errore nel parsing del certificato", ex);
        }
        String hashFromBytes;
        try {
            hashFromBytes = getHashFromBytes(jwsPublicKey.getEncoded(), "SHA-256");
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new AuthorizationUtilsException("errore nel calcolo dell'hash della chiave pubblica", ex);
        }
            
        // Creazione del token JWT
        LocalDateTime now = LocalDateTime.now();
        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(hashFromBytes).build(),
            new JWTClaimsSet.Builder()
                .subject(commonName)
                .issueTime(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
                .expirationTime(
                    Date.from(now.plusSeconds(expirationSeconds).atZone(ZoneId.systemDefault()).toInstant()))
                .issuer(issuer)
                .claim("context", context)
                .build());

        try {
            // firma del token JWT, per ottenere il token JWS
            signedJWT.sign(new RSASSASigner(singTokenPrivateKey));
        } catch (JOSEException ex) {
            throw new AuthorizationUtilsException("errore nella firma del token", ex);
        }

        /*
        * crezione del token JWE, contenente il JWS come payload.
        * creo l'oggetto, inserisco il JWS come payload e poi lo cifro
        */
        JWEObject jweObject = new JWEObject(
            new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                .contentType("JWT") // required to indicate nested JWT
                .build(),
            new Payload(signedJWT));

        try {
            // cifro con la chiave pubblica
            jweObject.encrypt(new RSAEncrypter(tokenEncryptionPublickey));
        } catch (JOSEException ex) {
            throw new AuthorizationUtilsException("errore nella cifratura del token", ex);
        }

        // creo la stringa del token JWE
        String jweStringEncrypted = jweObject.serialize();
        return jweStringEncrypted;
        
    }
    
    public static Map<String, Object> getUploaderContext() throws JOSEException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, Exception {        
        Map<String, Object> context = new HashMap();
        Map<String, Object> metadata = new HashMap();
        metadata.put("chiave1", 1);
        metadata.put("chiave2", "ciao");
        
        Map<String, Object> minIOParams = new HashMap();
        minIOParams.put("metadata", metadata);
//        minIOParams.put("bucket", "105");
//        minIOParams.put("codiceAzienda", "105");
//        minIOParams.put("overwrite", false);

//        context.put("mimeType", "application/pdf");
//        context.put("filePath", "/tmp/test");
//        context.put("fileName", "prova_gdm.jpeg");
//        context.put("target", Source.MinIO);
        context.put("params", minIOParams);
        context.put("target", "Default");
//        context.put("params", minIOParams);
        
        return context;
    }
    
    private String getHashFromBytes(byte[] file, String algorithm) throws IOException, NoSuchAlgorithmException {
 
        MessageDigest mdigest = MessageDigest.getInstance(algorithm);
        
        // read the data from file and update that data in the message digest
        mdigest.update(file);
 
        // store the bytes returned by the digest() method
        byte[] hashBytes = mdigest.digest();
 
        // this array of bytes has bytes in decimal format so we need to convert it into hexadecimal format
        // for this we create an object of StringBuilder since it allows us to update the string i.e. its mutable
        StringBuilder sb = new StringBuilder();
       
        Formatter fmt = new Formatter();
        // loop through the bytes array
        for (int i = 0; i < hashBytes.length; i++) {
           
            // the following line converts the decimal into hexadecimal format and appends that to the StringBuilder object
            //sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
            fmt.format("%02X", hashBytes[i]);
        }
 
        // finally we return the complete hash
        return fmt.toString();
    }

}
