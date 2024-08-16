package it.bologna.ausl.internauta.utils.authorizationutils;

import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 *
 * @author MicheleD'Onza
 */
public class InadTokenManager {
    
    public static PrivateKey getSignTokenPrivateKey(String keyPath, String keyName, String keyPassword) throws AuthorizationUtilsException, KeyStoreException, FileNotFoundException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidKeySpecException {
        
        byte[] keyBytes = Files.readAllBytes(Paths.get(keyPath));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey generatePrivate = kf.generatePrivate(spec);
        generatePrivate.toString();
//        KeyStore rsa = KeyStore.getInstance("RSA");
//        rsa.load(new FileInputStream(keyPath), keyPassword.toCharArray());
//        Key secretKey = rsa.getKey(keyName, keyPassword.toCharArray());
//        kf.generatePrivate(spec);
        return generatePrivate;
    }
    
    private static String getToken(){
    return null;
    }
    
}
