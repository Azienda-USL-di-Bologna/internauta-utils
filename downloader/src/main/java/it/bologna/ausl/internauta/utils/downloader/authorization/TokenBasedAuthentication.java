package it.bologna.ausl.internauta.utils.downloader.authorization;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class TokenBasedAuthentication extends AbstractAuthenticationToken {

    private String token;
    private AuthenticatedApplication application;

    public TokenBasedAuthentication(String token, AuthenticatedApplication application, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.token = token;
        this.application = application;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
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
