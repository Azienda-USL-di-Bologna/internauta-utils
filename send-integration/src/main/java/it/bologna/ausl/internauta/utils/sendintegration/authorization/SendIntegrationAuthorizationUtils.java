package it.bologna.ausl.internauta.utils.sendintegration.authorization;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.model.entities.sendintegration.ApiKeyStoreEntry;
import it.bologna.ausl.internauta.model.entities.sendintegration.QApiKeyStoreEntry;
import it.bologna.ausl.internauta.utils.sendintegration.authorization.exceptions.NotValidJwtException;
import it.bologna.ausl.internauta.utils.sendintegration.exceptions.RuntimeExceptionContainer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.security.interfaces.RSAKey;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.logging.Level;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author gdm
 */
@Component
public class SendIntegrationAuthorizationUtils {
    private static Logger logger = LoggerFactory.getLogger(SendIntegrationAuthorizationUtils.class);
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    @Transactional(rollbackFor = Throwable.class)
    public ApiKeyStoreEntry getApiKeyEntry(String apiKey, ZonedDateTime now) throws NotValidJwtException {
        QApiKeyStoreEntry qApiKeyStore = QApiKeyStoreEntry.apiKeyStoreEntry;
        
        BooleanExpression attivoOra = Expressions.booleanTemplate(
            "{0} @> {1}",
            qApiKeyStore.intervallo,
//            Expressions.constant("@>"),
            now
        );
        
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        ApiKeyStoreEntry apiKeyStoreEntry = queryFactory
            .select(qApiKeyStore)
            .from(qApiKeyStore)
            .where(
                qApiKeyStore.apiKey.eq(apiKey).and(
                    qApiKeyStore.tipoChiamante.eq(ApiKeyStoreEntry.TipoChiamante.FRUITORE).and(
                        qApiKeyStore.chiamante.eq(ApiKeyStoreEntry.Chiamante.Lepida).and(
                            attivoOra
                        )
                    )
                )
            )
            .fetchOne();
        if (apiKeyStoreEntry == null ) {
            throw new NotValidJwtException("nessun ApiKeyEntry attivo trovato");
        }
        return apiKeyStoreEntry;
    }
    
    public void verifyTokenAndSetContext(String token, ZonedDateTime now) throws NotValidJwtException {
        JWSObject signedToken;
        try {
            signedToken = JWSObject.parse(token);
        } catch (ParseException ex) {
            throw new NotValidJwtException("errore nel parsing del token", ex);
        }
        Payload payload = signedToken.getPayload();
        Map<String, Object> payloadMap = payload.toJSONObject();
        String apyKey = (String) payloadMap.get("iss");
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        ApiKeyStoreEntry apiKeyStoreEntry = null;
        try {
            apiKeyStoreEntry = transactionTemplate.execute(a -> {
                try {
                    return getApiKeyEntry(apyKey, now);
                } catch (NotValidJwtException ex) {
                    throw new RuntimeExceptionContainer(ex);
                }
            });
        } catch (RuntimeExceptionContainer ex) {
            if (ex.getException() instanceof NotValidJwtException notValidJwtException) {
                throw notValidJwtException;
            }
        } catch (Throwable ex) {
            throw new NotValidJwtException("errore nella lettura dell'ApiKeyEntry ");
        }
        try {
            String apiSecret = apiKeyStoreEntry.getApiSecret();
            JWSVerifier verifier = new MACVerifier(apiSecret);
            if (!signedToken.verify(verifier)) {
                
            }
        } catch (JOSEException ex) {
            
        }
//        RSAKey rsaJWK = new RSAKey.Builder(pub)
//        .privateKey(priv)
//        .keyUse(KeyUse.SIGNATURE)
//        .algorithm(JWSAlgorithm.PS256)
//        .keyID("KeyID")
//        .build();
//        
//        byte[] bytes = Decoders.BASE64.decode(secretKey);
//        SecretKey key = Keys.hmacShaKeyFor(bytes);
//        new RSASSAVerifier(rsak);
//        if (!signedToken.verify(new RSASSAVerifier((RSAPublicKey) cert.getPublicKey()))) {
//            
//        }
//            // Controllo la firma con la chiave pubblica estratta sopra
//            if (!signedToken.verify(new RSASSAVerifier((RSAPublicKey) cert.getPublicKey()))) {
//                String errorMessage = "la firma del token non è valida";
//                logger.error(errorMessage);
//                throw new AuthorizationUtilsException(errorMessage);
//            }
//            
//            // lo estraggo e lo uso per trovare la chiave pubblica nella mia mappa "hashPublicKeyMap"
//            String cn = AuthorizationUtilityFunctions.getCommonNameFromX509Certificate(cert);
//        
//            JWTClaimsSet jWTClaimsSet = JWTClaimsSet.parse(signedToken.getPayload().toJSONObject());
        return;
    }
}
