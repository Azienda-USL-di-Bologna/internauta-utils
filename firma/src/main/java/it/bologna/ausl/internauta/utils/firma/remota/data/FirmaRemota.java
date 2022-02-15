package it.bologna.ausl.internauta.utils.firma.remota.data;

import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.FirmaRemotaException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.InvalidCredentialException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.RemoteFileNotFoundException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.RemoteServiceException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.WrongTokenException;



/**
 *
 * @author guido
 */
public abstract class FirmaRemota {

    public abstract FirmaRemotaInformation firma(FirmaRemotaInformation firmaRemotaInformation) throws FirmaRemotaException, RemoteFileNotFoundException, WrongTokenException, InvalidCredentialException, RemoteServiceException;

    public abstract void telefona(UserInformation userInformation) throws FirmaRemotaException, WrongTokenException, InvalidCredentialException, RemoteServiceException;

    public abstract boolean existingCredential(UserInformation userInformation) throws FirmaRemotaException, InvalidCredentialException, RemoteServiceException;

    public abstract boolean setCredential(UserInformation userInformation) throws FirmaRemotaException, InvalidCredentialException, RemoteServiceException;

    public abstract boolean removeCredential(UserInformation userInformation) throws FirmaRemotaException, InvalidCredentialException, RemoteServiceException;
}
