package it.bologna.ausl.internauta.utils.firma.remota.controllers;

import it.bologna.ausl.internauta.utils.firma.remota.data.FirmaRemota;
import it.bologna.ausl.internauta.utils.firma.remota.data.FirmaRemotaFactory;
import it.bologna.ausl.internauta.utils.firma.remota.data.FirmaRemotaInformation;
import it.bologna.ausl.internauta.utils.firma.remota.data.UserInformation;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.FirmaRemotaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
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
     * 
     * @param firmaRemotaInformation
     * @throws FirmaRemotaException 
     * @deprecated legacy fino a quando usiamo le applicazioni inde, poi usare preAutentication
     */
    @Deprecated
    @RequestMapping(value = "/telefona", method = RequestMethod.POST)
    public void telefona(
                @RequestBody FirmaRemotaInformation firmaRemotaInformation) throws FirmaRemotaException {
        firmaRemotaFactory.getFirmaRemotaInstance(firmaRemotaInformation.getProvider()).telefona(firmaRemotaInformation.getUserInformation());
    }
    
    @RequestMapping(value = "/preAutentication", method = RequestMethod.POST)
    public void preAutentication(
                @RequestBody UserInformation userInformation, 
                @RequestParam(required = true) FirmaRemotaInformation.FirmaRemotaProviders provider) throws FirmaRemotaException {
        firmaRemotaFactory.getFirmaRemotaInstance(provider).telefona(userInformation);
    }

    /**
     * 
     * @param firmaRemotaInformation
     * @return
     * @throws FirmaRemotaException
     * @deprecated legacy fino a quando usiamo le applicazioni inde, poi usare preAutentication l'altra firma
     */
    @Deprecated
    @RequestMapping(value = "/firma", method = RequestMethod.POST)
    public FirmaRemotaInformation firma(
                @RequestBody FirmaRemotaInformation firmaRemotaInformation) throws FirmaRemotaException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(firmaRemotaInformation.getProvider());
        FirmaRemotaInformation res = firmaRemotaInstance.firma(firmaRemotaInformation);
        return res;
    }
    
    @RequestMapping(value = "/firma", method = RequestMethod.POST)
    public FirmaRemotaInformation firma(
                @RequestBody FirmaRemotaInformation firmaRemotaInformation, 
                @RequestParam(required = true) FirmaRemotaInformation.FirmaRemotaProviders provider) throws FirmaRemotaException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(firmaRemotaInformation.getProvider());
        FirmaRemotaInformation res = firmaRemotaInstance.firma(firmaRemotaInformation);
        return res;
    }

    @RequestMapping(value = "/existingCredential", method = RequestMethod.POST)
    public Boolean existingCredential(
                @RequestBody UserInformation userInformation, 
                @RequestParam(required = true) FirmaRemotaInformation.FirmaRemotaProviders provider) throws FirmaRemotaException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(provider);
        return firmaRemotaInstance.existingCredential(userInformation);
    }

    @RequestMapping(value = "/setCredential", method = RequestMethod.POST)
    public Boolean setCredential(
                @RequestBody UserInformation userInformation, 
                @RequestParam(required = true) FirmaRemotaInformation.FirmaRemotaProviders provider) throws FirmaRemotaException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(provider);
        return firmaRemotaInstance.setCredential(userInformation);
    }

    @RequestMapping(value = "/removeCredential", method = RequestMethod.POST)
    public Boolean removeCredential(
                @RequestBody UserInformation userInformation, 
                @RequestParam(required = true) FirmaRemotaInformation.FirmaRemotaProviders provider) throws FirmaRemotaException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(provider);
        return firmaRemotaInstance.removeCredential(userInformation);
    }
    
}
