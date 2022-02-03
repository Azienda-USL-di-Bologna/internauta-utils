package it.bologna.ausl.internauta.utils.downloader.authorization;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

public class TokenBasedAuthentication extends AbstractAuthenticationToken {

    private SignedJWT token;
    private AuthenticatedApplication application;

    public TokenBasedAuthentication(SignedJWT token, AuthenticatedApplication application) {
        super(application.getAuthorities());
        this.token = token;
        this.application = application;
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
        return application;
    }
}
