package it.bologna.ausl.internauta.utils.firma.remota.controllers;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.utils.firma.remota.FirmaRemota;
import it.bologna.ausl.internauta.utils.firma.remota.FirmaRemotaFactory;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaHttpException;
import it.bologna.ausl.internauta.utils.firma.repositories.ConfigurationRepository;
import it.bologna.ausl.model.entities.firma.Configuration;
import it.bologna.ausl.model.entities.firma.QConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
 * Ogni servlet, come prima cosa, reperisce l'istanza corretta della classe di firma (implementazione di FirmaRemota) basandosi sull'hostId passato in input.
 * L'istanza viene reperita tramite la classe factory FirmaRemotaFactory
 * 
 * @author gdm
 */
@RestController
@RequestMapping(value = "${firma.remota.mapping.url}")
public class FirmaRemotaRestController implements ControllerHandledExceptions {

    @Autowired
    private FirmaRemotaFactory firmaRemotaFactory;
    
    @Autowired
    private ConfigurationRepository configurationRepository;

    @RequestMapping(value = "/test/{nome}/{cognome}", method = RequestMethod.GET)
    public String test(@PathVariable String nome, @PathVariable String cognome) {

//        System.out.println("ESITO: " + var);
        return "ciao " + nome + cognome;
    }

    /**
     * Servlet che esegue la pre-autenticazione.Da usare ad esempio con Aruba per la modalità firma con token ottenuto per sms o chiamata al telefono
     * @param firmaRemotaInformation l'oggetto può contenere solo la parte userInformation con le informazioni che identificano l'utente al quale mandare l'sms o la telefonata
     * @param hostId l'hostId della tabella Configurations che identifica l'installazione della firma remota da utilizzare
     * @throws FirmaRemotaHttpException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     * @deprecated legacy fino a quando usiamo le applicazioni inde, poi usare preAutentication
     */
    @Deprecated
    @RequestMapping(value = "/telefona", method = RequestMethod.POST)
    public void telefona(
                @RequestBody FirmaRemotaInformation firmaRemotaInformation,
                @RequestParam(required = true) String hostId) throws FirmaRemotaHttpException, FirmaRemotaConfigurationException {
        firmaRemotaFactory.getFirmaRemotaInstance(hostId).preAuthentication(firmaRemotaInformation.getUserInformation());
    }
    
    /**
     * Servlet che esegue la pre-autenticazione.Da usare ad esempio con Aruba per la modalità firma con token ottenuto per sms o chiamata al telefono
     * @param userInformation le informazioni che identificano l'utente al quale mandare l'sms o la telefonata
     * @param hostId l'hostId della tabella Configurations che identifica l'installazione della firma remota da utilizzare
     * @throws FirmaRemotaHttpException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     */
    @RequestMapping(value = "/preAutentication", method = RequestMethod.POST)
    public void preAutentication(
                @RequestBody UserInformation userInformation,
                @RequestParam(required = true) String hostId) throws FirmaRemotaHttpException, FirmaRemotaConfigurationException {
        firmaRemotaFactory.getFirmaRemotaInstance(hostId).preAuthentication(userInformation);
    }

    /**
  * Servlet per la firma dei file
     * @param firmaRemotaInformation le informazioni che identificano i file da firmare
     * @param hostId l'hostId della tabella Configurations che identifica l'installazione della firma remota da utilizzare
     * @param codiceAzienda codice dell'azienda per la quale si vuole agire (es. 102,105,106,ecc.)
     * @return
     * @throws FirmaRemotaHttpException
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException
     * @deprecated legacy fino a quando usiamo le applicazioni inde, poi usare firmaRemota
     */
    @Deprecated
    @RequestMapping(value = "/firma", method = RequestMethod.POST)
    public FirmaRemotaInformation firma(
                @RequestBody FirmaRemotaInformation firmaRemotaInformation,
                @RequestParam(required = true) String hostId,
                @RequestParam(required = true) String codiceAzienda) throws FirmaRemotaHttpException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(hostId);
        FirmaRemotaInformation res = firmaRemotaInstance.firma(firmaRemotaInformation, codiceAzienda);
        return res;
    }
    
    /**
     * Servlet per la firma dei file
     * @param firmaRemotaInformation le informazioni che identificano i file da firmare
     * @param hostId l'hostId della tabella Configurations che identifica l'installazione della firma remota da utilizzare
     * @param codiceAzienda codice dell'azienda per la quale si vuole agire (es. 102,105,106,ecc.)
     * @return
     * @throws FirmaRemotaHttpException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     */
    @RequestMapping(value = "/firmaRemota", method = RequestMethod.POST)
    public FirmaRemotaInformation firmaRemota(
                @RequestBody FirmaRemotaInformation firmaRemotaInformation, 
                @RequestParam(required = true) String hostId,
                @RequestParam(required = true) String codiceAzienda) throws FirmaRemotaHttpException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(hostId);
        FirmaRemotaInformation res = firmaRemotaInstance.firma(firmaRemotaInformation, codiceAzienda);
        return res;
    }

    /**
     * controlla se esistono le credenziali dell'utente passato sul sistema di memorizzazione credenziali
     * @param userInformation contiene le informazioni per identificare l'utenza
     * @param hostId l'hostId della tabella Configurations che identifica l'installazione della firma remota da utilizzare
     * @return
     * @throws FirmaRemotaHttpException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     */
    @RequestMapping(value = "/existingCredential", method = RequestMethod.POST)
    public Boolean existingCredential(
                @RequestBody UserInformation userInformation, 
                @RequestParam(required = true) String hostId) throws FirmaRemotaHttpException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(hostId);
        return firmaRemotaInstance.existingCredential(userInformation, hostId);
    }

    /**
     * setta le credenziali per l'utente passato sul sistema di memorizzazione credenziali
     * @param userInformation contiene le informazioni per identificare l'utenza
     * @param hostId l'hostId della tabella Configurations che identifica l'installazione della firma remota da utilizzare
     * @return true se le credenziali sono state settate, false altrimenti
     * @throws FirmaRemotaHttpException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     */
    @RequestMapping(value = "/setCredential", method = RequestMethod.POST)
    public Boolean setCredential(
                @RequestBody UserInformation userInformation, 
                @RequestParam(required = true) String hostId) throws FirmaRemotaHttpException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(hostId);
        return firmaRemotaInstance.setCredential(userInformation, hostId);
    }

    /**
     * rimuove le credenziali per l'utente passato sul sistema di memorizzazione credenziali
     * @param userInformation contiene le informazioni per identificare l'utenza
     * @param hostId l'hostId della tabella Configurations che identifica l'installazione della firma remota da utilizzare
     * @return true se le credenziali sono state rimosse, false altrimenti
     * @throws FirmaRemotaHttpException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     */
    @RequestMapping(value = "/removeCredential", method = RequestMethod.POST)
    public Boolean removeCredential(
                @RequestBody UserInformation userInformation, 
                @RequestParam(required = true) String hostId) throws FirmaRemotaHttpException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(hostId);
        return firmaRemotaInstance.removeCredential(userInformation, hostId);
    }
    
    /**
     * Restituisce le descrizioni di tutti i providers oppure dei soli hostIds richiesti.
     * @param hostIds La lista degli hostId.
     * @return La descrizione dei providers.
     */
    @RequestMapping(value = "/getProvidersInfo", method = RequestMethod.GET)
    public List<Map<String, String>> getProvidersInfo(@RequestParam(required = false) List<String> hostIds) {
        List<Configuration> configurationList;
        if (hostIds == null || hostIds.isEmpty()) {
            configurationList = configurationRepository.findAll();
        } else {
            configurationList = new ArrayList<>();
            BooleanExpression filter = QConfiguration.configuration.hostId.in(hostIds);
            configurationRepository.findAll(filter).iterator().forEachRemaining(configurationList::add);
        }
        
        List<Map<String, String>> providersInfo = new ArrayList();
        configurationList.stream().forEach(c -> {
            Map<String, String> row = new HashMap();
            row.put("hostId", c.getHostId());
            row.put("provider", c.getProvider());
            row.put("descrizione", c.getDescrizione());
            providersInfo.add(row);
        });
        return providersInfo;
    }
    
}
