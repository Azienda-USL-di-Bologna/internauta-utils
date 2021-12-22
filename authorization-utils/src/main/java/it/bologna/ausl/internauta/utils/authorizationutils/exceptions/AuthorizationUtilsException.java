package it.bologna.ausl.internauta.utils.authorizationutils.exceptions;

/**
 *
 * @author gdm
 */
public class AuthorizationUtilsException extends Exception {

    public AuthorizationUtilsException() {
    }

    public AuthorizationUtilsException(String string) {
        super(string);
    }

    public AuthorizationUtilsException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

    public AuthorizationUtilsException(Throwable thrwbl) {
        super(thrwbl);
    }
}
