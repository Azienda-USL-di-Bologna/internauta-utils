package it.bologna.ausl.internauta.utils.sendintegration.authorization;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.authorizationutils.AuthorizationUtilityFunctions;
import it.bologna.ausl.model.entities.sendintegration.ApiKeyStoreEntry;
import it.bologna.ausl.model.entities.sendintegration.QApiKeyStoreEntry;
import it.bologna.ausl.internauta.utils.sendintegration.authorization.exceptions.NotValidJwtException;
import it.bologna.ausl.internauta.utils.sendintegration.exceptions.RuntimeExceptionContainer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ApiKeyStoreEntry getApiKeyEntryForErogatore(UUID apiKey, ZonedDateTime now) throws NotValidJwtException {
        QApiKeyStoreEntry qApiKeyStore = QApiKeyStoreEntry.apiKeyStoreEntry;
        
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        ApiKeyStoreEntry apiKeyStoreEntry = queryFactory
            .select(qApiKeyStore)
            .from(qApiKeyStore)
            .where(
                qApiKeyStore.apiKey.eq(apiKey).and(
                    qApiKeyStore.tipoChiamante.eq(ApiKeyStoreEntry.TipoChiamante.FRUITORE).and(
                        qApiKeyStore.chiamante.eq(ApiKeyStoreEntry.Chiamante.Lepida).and(
                            qApiKeyStore.dataInizio.before(now).and(
                                (qApiKeyStore.dataFine.isNull().or(qApiKeyStore.dataFine.after(now)))
                            )
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
    
     @Transactional(rollbackFor = Throwable.class)
    public ApiKeyStoreEntry getApiKeyEntryForFruitore(ZonedDateTime now) throws NotValidJwtException {
        QApiKeyStoreEntry qApiKeyStore = QApiKeyStoreEntry.apiKeyStoreEntry;
        
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        ApiKeyStoreEntry apiKeyStoreEntry = queryFactory
            .select(qApiKeyStore)
            .from(qApiKeyStore)
            .where(
                qApiKeyStore.tipoChiamante.eq(ApiKeyStoreEntry.TipoChiamante.EROGATORE).and(
                    qApiKeyStore.chiamante.eq(ApiKeyStoreEntry.Chiamante.Babel).and(
                        qApiKeyStore.dataInizio.before(now).and(
                            (qApiKeyStore.dataFine.isNull().or(qApiKeyStore.dataFine.after(now)))
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
    
    public String generateTokenForFruitore(ZonedDateTime now) throws NotValidJwtException {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        ApiKeyStoreEntry apiKeyStoreEntry = null;
        try {
            apiKeyStoreEntry = transactionTemplate.execute(a -> {
                try {
                    return getApiKeyEntryForFruitore(now);
                } catch (NotValidJwtException ex) {
                    throw new RuntimeExceptionContainer(ex);
                }
            });
        } catch (RuntimeExceptionContainer ex) {
            if (ex.getException() instanceof NotValidJwtException notValidJwtException) {
                throw notValidJwtException;
            }
        } catch (Throwable ex) {
            String error = "errore nella lettura dell'ApiKeyEntry";
            logger.error(error, ex);
            throw new NotValidJwtException(error, ex);
        }
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(apiKeyStoreEntry.getApiKey().toString())
                .issueTime(Date.from(now.toInstant()))
                .build();
            return AuthorizationUtilityFunctions.generateJWT(apiKeyStoreEntry.getApiSecret().toString(), claims);
        } catch (Exception ex) {
             String error = "errore nella lettura dell'ApiKeyEntry";
            logger.error(error, ex);
            throw new NotValidJwtException(error, ex);
        }
    }
    
    public void verifyTokenAndSetContext(String token, ZonedDateTime now) throws NotValidJwtException {
        SignedJWT signedToken;
        JWTClaimsSet claims;
        try {
            signedToken = SignedJWT.parse(token);
            claims = signedToken.getJWTClaimsSet();
        } catch (ParseException ex) {
            String error = "errore nel parsing del token";
            logger.error(error, ex);
            throw new NotValidJwtException(error, ex);
        }
        
//        Map<String, Object> payloadMap = payload.toJSONObject();
        String apyKey = (String) claims.getIssuer();
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        ApiKeyStoreEntry apiKeyStoreEntry = null;
        try {
            apiKeyStoreEntry = transactionTemplate.execute(a -> {
                try {
                    return getApiKeyEntryForErogatore(UUID.fromString(apyKey), now);
                } catch (NotValidJwtException ex) {
                    throw new RuntimeExceptionContainer(ex);
                }
            });
        } catch (RuntimeExceptionContainer ex) {
            if (ex.getException() instanceof NotValidJwtException notValidJwtException) {
                throw notValidJwtException;
            }
        } catch (Throwable ex) {
            String error = "errore nella lettura dell'ApiKeyEntry";
            logger.error(error, ex);
            throw new NotValidJwtException(error, ex);
        }
        try {
            String apiSecret = apiKeyStoreEntry.getApiSecret().toString();
            JWSVerifier verifier = new MACVerifier(apiSecret);
            if (!signedToken.verify(verifier)) {
                String error = "il token non è valido";
                logger.error(error);
                throw new NotValidJwtException(error);
            }
        } catch (JOSEException ex) {
            String error = "errore nel parsing del token";
            logger.error(error, ex);
            throw new NotValidJwtException(error, ex);
        }
        
        ZonedDateTime issuedTime = ZonedDateTime.ofInstant(claims.getIssueTime().toInstant(), ZoneId.systemDefault());
        if (now.isAfter(issuedTime.plusSeconds(apiKeyStoreEntry.getSecondiValidita()))) {
            String error = "il token è scaduto";
            logger.error(error);
            throw new NotValidJwtException(error);
        }
        
        TokenBasedAuthentication authentication = new TokenBasedAuthentication(signedToken, apiKeyStoreEntry.getChiamante());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
