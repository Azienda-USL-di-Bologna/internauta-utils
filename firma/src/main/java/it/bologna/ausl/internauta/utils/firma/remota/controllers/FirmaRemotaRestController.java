package it.bologna.ausl.internauta.utils.firma.remota.controllers;

import it.bologna.ausl.internauta.utils.firma.remota.FirmaRemota;
import it.bologna.ausl.internauta.utils.firma.remota.FirmaRemotaFactory;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller che implementa le API per la firma remota
 * 
 * Ogni servlet, come prima cosa, reperisce l'istanza corretta della classe di firma (implementazione di FirmaRemota) basandosi sul provider passato in input.
 * L'istanza viene reperita tramite la classe factory FirmaRemotaFactory
 * 
 * @author gdm
 */
@RestController
@RequestMapping(value = "${firma.remota.mapping.url}")
public class FirmaRemotaRestController implements ControllerHandledExceptions {

    @Autowired
    private FirmaRemotaFactory firmaRemotaFactory;

    @RequestMapping(value = "/test/{nome}/{cognome}", method = RequestMethod.GET)
    public String test(@PathVariable String nome, @PathVariable String cognome) {

//        System.out.println("ESITO: " + var);
        return "ciao " + nome + cognome;
    }

    /**
     * Servlet che esegue la pre-autenticazione.Da usare ad esempio con Aruba per la modalità firma con token ottenuto per sms o chiamata al telefono
     * @param firmaRemotaInformation l'oggetto può contenere solo la parte userInformation con le informazioni che identificano l'utente al quale mandare l'sms o la telefonata
     * @param codiceAzienda codcice dell'azienda per la quale si vuole agire (es. 102,105,106,ecc.)
     * @throws FirmaRemotaException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     * @deprecated legacy fino a quando usiamo le applicazioni inde, poi usare preAutentication
     */
    @Deprecated
    @RequestMapping(value = "/telefona", method = RequestMethod.POST)
    public void telefona(
                @RequestBody FirmaRemotaInformation firmaRemotaInformation,
                @RequestParam(required = true) String codiceAzienda) throws FirmaRemotaException, FirmaRemotaConfigurationException {
        firmaRemotaFactory.getFirmaRemotaInstance(firmaRemotaInformation.getProvider(), codiceAzienda).preAuthentication(firmaRemotaInformation.getUserInformation());
    }
    
    /**
     * Servlet che esegue la pre-autenticazione.Da usare ad esempio con Aruba per la modalità firma con token ottenuto per sms o chiamata al telefono
     * @param userInformation le informazioni che identificano l'utente al quale mandare l'sms o la telefonata
     * @param codiceAzienda codcice dell'azienda per la quale si vuole agire (es. 102,105,106,ecc.)
     * @param provider
     * @throws FirmaRemotaException 
     */
    @RequestMapping(value = "/preAutentication", method = RequestMethod.POST)
    public void preAutentication(
                @RequestBody UserInformation userInformation,
                @RequestParam(required = true) String codiceAzienda,
                @RequestParam(required = true) FirmaRemotaInformation.FirmaRemotaProviders provider) throws FirmaRemotaException, FirmaRemotaConfigurationException {
        firmaRemotaFactory.getFirmaRemotaInstance(provider, codiceAzienda).preAuthentication(userInformation);
    }

    /**
  * Servlet per la firma dei file
     * @param firmaRemotaInformation le informazioni che identificano i file da firmare
     * @param codiceAzienda codcice dell'azienda per la quale si vuole agire (es. 102,105,106,ecc.)
     * @return
     * @throws FirmaRemotaException
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException
     * @deprecated legacy fino a quando usiamo le applicazioni inde, poi usare firmaRemota
     */
    @Deprecated
    @RequestMapping(value = "/firma", method = RequestMethod.POST)
    public FirmaRemotaInformation firma(
                @RequestBody FirmaRemotaInformation firmaRemotaInformation,
                @RequestParam(required = true) String codiceAzienda) throws FirmaRemotaException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(firmaRemotaInformation.getProvider(), codiceAzienda);
        FirmaRemotaInformation res = firmaRemotaInstance.firma(firmaRemotaInformation);
        return res;
    }
    
    /**
     * Servlet per la firma dei file
     * @param firmaRemotaInformation le informazioni che identificano i file da firmare
     * @param provider
     * @param codiceAzienda codcice dell'azienda per la quale si vuole agire (es. 102,105,106,ecc.)
     * @return
     * @throws FirmaRemotaException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     */
    @RequestMapping(value = "/firmaRemota", method = RequestMethod.POST)
    public FirmaRemotaInformation firmaRemota(
                @RequestBody FirmaRemotaInformation firmaRemotaInformation, 
                @RequestParam(required = true) FirmaRemotaInformation.FirmaRemotaProviders provider,
                @RequestParam(required = true) String codiceAzienda) throws FirmaRemotaException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(provider, codiceAzienda);
        FirmaRemotaInformation res = firmaRemotaInstance.firma(firmaRemotaInformation);
        return res;
    }

    /**
     * controlla se esistono le credenziali dell'utente passato sul sistema di memorizzazione credenziali
     * @param userInformation contiene le informazioni per identificare l'utenza
     * @param provider
     * @param codiceAzienda codcice dell'azienda per la quale si vuole agire (es. 102,105,106,ecc.)
     * @return
     * @throws FirmaRemotaException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     */
    @RequestMapping(value = "/existingCredential", method = RequestMethod.POST)
    public Boolean existingCredential(
                @RequestBody UserInformation userInformation, 
                @RequestParam(required = true) FirmaRemotaInformation.FirmaRemotaProviders provider,
                @RequestParam(required = true) String codiceAzienda) throws FirmaRemotaException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(provider, codiceAzienda);
        return firmaRemotaInstance.existingCredential(userInformation);
    }

    /**
     * setta le credenziali per l'utente passato sul sistema di memorizzazione credenziali
     * @param userInformation contiene le informazioni per identificare l'utenza
     * @param provider
     * @param codiceAzienda
     * @return
     * @throws FirmaRemotaException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     */
    @RequestMapping(value = "/setCredential", method = RequestMethod.POST)
    public Boolean setCredential(
                @RequestBody UserInformation userInformation, 
                @RequestParam(required = true) FirmaRemotaInformation.FirmaRemotaProviders provider,
                @RequestParam(required = true) String codiceAzienda) throws FirmaRemotaException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(provider, codiceAzienda);
        return firmaRemotaInstance.setCredential(userInformation);
    }

    /**
     * rimuove le credenziali per l'utente passato sul sistema di memorizzazione credenziali
     * @param userInformation contiene le informazioni per identificare l'utenza
     * @param provider
     * @param codiceAzienda codcice dell'azienda per la quale si vuole agire (es. 102,105,106,ecc.)
     * @return
     * @throws FirmaRemotaException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     */
    @RequestMapping(value = "/removeCredential", method = RequestMethod.POST)
    public Boolean removeCredential(
                @RequestBody UserInformation userInformation, 
                @RequestParam(required = true) FirmaRemotaInformation.FirmaRemotaProviders provider,
                @RequestParam(required = true) String codiceAzienda) throws FirmaRemotaException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(provider, codiceAzienda);
        return firmaRemotaInstance.removeCredential(userInformation);
    }
    
}
