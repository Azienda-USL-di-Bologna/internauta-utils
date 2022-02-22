package it.bologna.ausl.internauta.utils.firma.remota.data;

import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.FirmaRemotaException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.InvalidCredentialException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.RemoteFileNotFoundException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.RemoteServiceException;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.WrongTokenException;



/**
 *
 * @author gdm
 */
public abstract class FirmaRemota {

    /**
     * Questo metodo firma uno o più files all'interno di "files" in firmaRemotaInformation e ritorna lo stesso oggetto con il risultato per ogni file
     * @param firmaRemotaInformation contiene tutto il necessatio per firmare (i files da firmare e tutte le inforazioni che servono per firmarli)
     * @return lo stesso oggetto in input, ma che per ogni file all'interno del campo files ha scritto anche il risultato
     * @throws FirmaRemotaException
     * @throws RemoteFileNotFoundException
     * @throws WrongTokenException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    public abstract FirmaRemotaInformation firma(FirmaRemotaInformation firmaRemotaInformation) throws FirmaRemotaException, RemoteFileNotFoundException, WrongTokenException, InvalidCredentialException, RemoteServiceException;

    /**
     * Questo metodo di occupa della pre-autenticazione, quando è necessario eseguire delle operazioni preliminari per ottenere il necessario per firmare.
     * Per esempio, il provider ARUBA ha una funzione che, previo passaggio di alcune informazioni invia un sms ho manda una chiamata al cellulare del firmatario con il codice OTP per firmare
     * @param userInformation le informazioni necessarie (che riaguardano l'utente) per eseguire le procedure di pre-autenticazione
     * @throws FirmaRemotaException
     * @throws WrongTokenException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    public abstract void preAuthentication(UserInformation userInformation) throws FirmaRemotaException, WrongTokenException, InvalidCredentialException, RemoteServiceException;

    /**
     * Questo metodo indica se sono presenti le credenziali dell'utente all'interno del sstema di memorizzazioni delle credenziali.
     * @param userInformation
     * @return true se sono presenti, false altrimenti
     * @throws FirmaRemotaException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    public abstract boolean existingCredential(UserInformation userInformation) throws FirmaRemotaException, InvalidCredentialException, RemoteServiceException;

    /**
     * Permette di memorizzare le credenziali di un utente per permetterli di non doverle inserire tutte le volte
     * @param userInformation
     * @return true se l'operazioni va a buon fine, false o eccezione altrimenti.
     * @throws FirmaRemotaException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    public abstract boolean setCredential(UserInformation userInformation) throws FirmaRemotaException, InvalidCredentialException, RemoteServiceException;

    /**
     * Elimina le eventuali credenziali salvate per l'utente passato
     * @param userInformation
     * @return true se le credenziali esistevano e sono state rimosse, false altrimenti
     * @throws FirmaRemotaException
     * @throws InvalidCredentialException
     * @throws RemoteServiceException 
     */
    public abstract boolean removeCredential(UserInformation userInformation) throws FirmaRemotaException, InvalidCredentialException, RemoteServiceException;
}
