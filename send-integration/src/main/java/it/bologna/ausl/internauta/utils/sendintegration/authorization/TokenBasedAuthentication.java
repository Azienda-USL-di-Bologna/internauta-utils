package it.bologna.ausl.internauta.utils.sendintegration.authorization;

import com.nimbusds.jwt.SignedJWT;
import it.bologna.ausl.internauta.model.entities.sendintegration.ApiKeyStoreEntry;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Questa classe viene iserita nel contesto, una volta che la chiamata è autenticata (il token è valido)
 * Tramite questa classe di può ottenere:
 *  - il token decodificato dal quale si possono poi estrarre i claims
 *  - l'applicazione chiamante
 * 
 * @author gdm
 */
public class TokenBasedAuthentication extends AbstractAuthenticationToken {

    private SignedJWT token;
    private final ApiKeyStoreEntry.Chiamante chiamante;

    public TokenBasedAuthentication(SignedJWT token, ApiKeyStoreEntry.Chiamante chiamante) {
        super(chiamante.getAuthorities());
        this.token = token;
        this.chiamante = chiamante;
    }

    public SignedJWT getToken() {
        return token;
    }

    public void setToken(SignedJWT token) {
        this.token = token;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public UserDetails getPrincipal() {
        return chiamante;
    }
}