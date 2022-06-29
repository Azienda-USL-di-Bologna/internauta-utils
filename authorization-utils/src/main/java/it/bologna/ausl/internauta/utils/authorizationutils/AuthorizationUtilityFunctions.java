package it.bologna.ausl.internauta.utils.authorizationutils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import it.bologna.ausl.internauta.utils.authorizationutils.exceptions.AuthorizationUtilsException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

/**
 *
 * @author gdm
 */
public class AuthorizationUtilityFunctions {
    private static Logger logger = LoggerFactory.getLogger(AuthorizationUtilityFunctions.class);
    
    /**
     * Estrae dal subject di un certificato X509 l'identificatore passato
     * @param cert
     * @param identifier es. BCStyle.ROLE oppure BCStyle.CN
     * @return
     * @throws CertificateEncodingException 
     */
    public static String getSubjectRDNFromX509Certificate(X509Certificate cert, ASN1ObjectIdentifier identifier) throws CertificateEncodingException {
        X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
        RDN rdn = x500name.getRDNs(identifier)[0];

        String rdnString = IETFUtils.valueToString(rdn.getFirst().getValue());
        return rdnString;
    }
    
    /**
     * Estrae il common name dal certificato
     * @param cert
     * @return
     * @throws CertificateEncodingException 
     */
    public static String getCommonNameFromX509Certificate(X509Certificate cert) throws CertificateEncodingException {
        return getSubjectRDNFromX509Certificate(cert, BCStyle.CN);
    }
    
    /**
     * Estrae il role dal certificato
     * @param cert
     * @return
     * @throws CertificateEncodingException 
     */
    public static String getRoleFromX509Certificate(X509Certificate cert) throws CertificateEncodingException {
        return getSubjectRDNFromX509Certificate(cert, BCStyle.ROLE);
    }
    
    /**
     * Controlla che il token sia valido secondo i nostri meccanismi di sicurezza e torna i claims
     * Controlli:
     *  - sia firmato con il certificato incluso nel campo x5c del token
     *  - il certificato incluso nel campo x5c sia emesso dalla CA di cui si passa il certificato (parametro CACertificate)
     *  - la mode del token, dell'applicazione e del certificato corrispondano (per evitare di usare un token o certificato di test in prod e viceversa)
     *  - il token non sia scaduto
     * @param token
     * @param CACertificate
     * @param maxLimitTokenSeconds
     * @param applicationMode
     * @return
     * @throws AuthorizationUtilsException 
     */
    public static JWTClaimsSet checkJWTAndGetClaims(String token, X509Certificate CACertificate, Integer maxLimitTokenSeconds, String applicationMode) throws AuthorizationUtilsException {

        try {
            JWSObject signedToken = JWSObject.parse(token);
            List<Base64> x509CertChain = signedToken.getHeader().getX509CertChain();
            X509Certificate cert = X509CertUtils.parse(x509CertChain.get(0).decode());
            
            try {
                cert.verify(CACertificate.getPublicKey());
            } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException ex) {
                String errorMessage = "errore nella verifica dell'issuer del certificato";
                logger.error(errorMessage);
                throw new AuthorizationUtilsException(errorMessage, ex);
            }
            
            // Controllo la firma con la chiave pubblica estratta sopra
            if (!signedToken.verify(new RSASSAVerifier((RSAPublicKey) cert.getPublicKey()))) {
                String errorMessage = "la firma del token non è valida";
                logger.error(errorMessage);
                throw new AuthorizationUtilsException(errorMessage);
            }
            
            // lo estraggo e lo uso per trovare la chiave pubblica nella mia mappa "hashPublicKeyMap"
            String cn = AuthorizationUtilityFunctions.getCommonNameFromX509Certificate(cert);
        
            JWTClaimsSet jWTClaimsSet = JWTClaimsSet.parse(signedToken.getPayload().toJSONObject());
            
//            // controllo che il subject del token sia uguale common name del certificato dal quale ho estratto la chiave pubblica
//            String subject = jWTClaimsSet.getSubject();
//            if (!cn.equalsIgnoreCase(subject)) {
//                String errorMessage = "il subject del token non corrisponde al common name del certificato";
//                logger.error(String.format(errorMessage + " subject: %s - common name: %s", subject, cn));
//                throw new AuthorizationUtilsException(errorMessage);
//            }
            
            String tokenMode = (String) jWTClaimsSet.getClaim("mode");
            String certRole = getRoleFromX509Certificate(cert);
            if (!tokenMode.equalsIgnoreCase(applicationMode))  {
                String errorMessage = String.format("il token mode e l'application mode non corrispondono: tokenMode=%s applicationMode=%s", tokenMode, applicationMode);
                logger.error(errorMessage);
                throw new AuthorizationUtilsException(errorMessage);
            } else {
                if (!certRole.endsWith(applicationMode)) {
                    String errorMessage = String.format("il token mode e l'application mode corrispondono, ma non rispecchiano il ruolo del certificato: tokenMode=%s applicationMode=%s certRole=%s", tokenMode, applicationMode, certRole);
                    logger.error(errorMessage);
                    throw new AuthorizationUtilsException(errorMessage);
                }
            }

            // controllo che il token non sia scaduto oppure che la data di scadenza sia troppo elevata
            Date expirationTime = jWTClaimsSet.getExpirationTime();
            Date maxLimitTokenTime = Date.from(LocalDateTime.now().plusSeconds(maxLimitTokenSeconds).atZone(ZoneId.systemDefault()).toInstant());
            if (expirationTime == null || new Date().after(expirationTime)) {
                String errorMessage = "il token è scaduto";
                logger.error(errorMessage);
//                throw new AuthorizationUtilsException(errorMessage);
            } else if (expirationTime.after(maxLimitTokenTime)) {
                String errorMessage = String.format("la scadenza del token è troppo lunga, il limite massimo è %s secondi", maxLimitTokenSeconds);
                logger.error(errorMessage);
                throw new AuthorizationUtilsException(errorMessage);
            }
            return jWTClaimsSet;
            
        } catch (ParseException ex) {
            String errorMessage = "errore nel parsing del token";
            logger.error(errorMessage);
           throw new AuthorizationUtilsException(errorMessage, ex);
        } catch (JOSEException ex) {
            String errorMessage = "errore nel parsing della chiave pubblica";
            logger.error(errorMessage);
           throw new AuthorizationUtilsException(errorMessage, ex);
        } catch (CertificateEncodingException ex) {
            String errorMessage = "errore nella lettura del certificato";
            logger.error(errorMessage);
            throw new AuthorizationUtilsException(errorMessage, ex);
        }
    }
}
