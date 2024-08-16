package it.bologna.ausl.internauta.utils.authorizationutils.exceptions;

/**
 *
 * @author gdm
 */
public class InternautaAuthorizationHttpException extends AuthorizationUtilsException {

    public InternautaAuthorizationHttpException() {
    }

    public InternautaAuthorizationHttpException(String string) {
        super(string);
    }

    public InternautaAuthorizationHttpException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

    public InternautaAuthorizationHttpException(Throwable thrwbl) {
        super(thrwbl);
    }
    
}
