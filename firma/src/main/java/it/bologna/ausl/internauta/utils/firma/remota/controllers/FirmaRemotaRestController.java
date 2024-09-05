package it.bologna.ausl.internauta.utils.firma.remota.controllers;

import com.querydsl.core.types.dsl.BooleanExpression;
import it.bologna.ausl.internauta.utils.firma.remota.FirmaRemota;
import it.bologna.ausl.internauta.utils.firma.remota.FirmaRemotaFactory;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaUserSign;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.medassignservice.MedasUserSign;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.exceptions.FirmaHttpException;
import it.bologna.ausl.internauta.utils.firma.repositories.ConfigurationRepository;
import it.bologna.ausl.model.entities.firma.Configuration;
import it.bologna.ausl.model.entities.firma.QConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaControllerHandledExceptions;

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
public class FirmaRemotaRestController implements FirmaRemotaControllerHandledExceptions {

    private static Logger logger = LoggerFactory.getLogger(FirmaRemotaRestController.class);
    
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
     * @throws FirmaHttpException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     * @deprecated legacy fino a quando usiamo le applicazioni inde, poi usare preAutentication
     */
    @Deprecated
    @RequestMapping(value = "/telefona", method = RequestMethod.POST)
    public void telefona(
                @RequestBody FirmaRemotaInformation firmaRemotaInformation,
                @RequestParam(required = true) String hostId) throws FirmaHttpException, FirmaRemotaConfigurationException {
        firmaRemotaFactory.getFirmaRemotaInstance(hostId).preAuthentication(firmaRemotaInformation.getUserInformation());
    }
    
    /**
     * Servlet che esegue la pre-autenticazione.Da usare ad esempio con Aruba per la modalità firma con token ottenuto per sms o chiamata al telefono
     * @param userInformation le informazioni che identificano l'utente al quale mandare l'sms o la telefonata
     * @param hostId l'hostId della tabella Configurations che identifica l'installazione della firma remota da utilizzare
     * @throws FirmaHttpException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     */
    @RequestMapping(value = "/preAutentication", method = RequestMethod.POST)
    public void preAutentication(
                @RequestBody UserInformation userInformation,
                @RequestParam(required = true) String hostId) throws FirmaHttpException, FirmaRemotaConfigurationException {
        firmaRemotaFactory.getFirmaRemotaInstance(hostId).preAuthentication(userInformation);
    }

    /**
  * Servlet per la firma dei file
     * @param firmaRemotaInformation le informazioni che identificano i file da firmare
     * @param hostId l'hostId della tabella Configurations che identifica l'installazione della firma remota da utilizzare
     * @param codiceAzienda codice dell'azienda per la quale si vuole agire (es. 102,105,106,ecc.)
     * @param request
     * @return
     * @throws FirmaHttpException
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException
     * @deprecated legacy fino a quando usiamo le applicazioni inde, poi usare firmaRemota
     */
    @Deprecated
    @RequestMapping(value = "/firma", method = RequestMethod.POST)
    public FirmaRemotaInformation firma(
                @RequestBody FirmaRemotaInformation firmaRemotaInformation,
                @RequestParam(required = true) String hostId,
                @RequestParam(required = true) String codiceAzienda,
                HttpServletRequest request) throws FirmaHttpException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(hostId);
        FirmaRemotaInformation res = firmaRemotaInstance.firma(firmaRemotaInformation, codiceAzienda, request);
        return res;
    }
    
    /**
     * Servlet per la firma dei file
     * @param firmaRemotaInformation le informazioni che identificano i file da firmare
     * @param hostId l'hostId della tabella Configurations che identifica l'installazione della firma remota da utilizzare
     * @param codiceAzienda codice dell'azienda per la quale si vuole agire (es. 102,105,106,ecc.)
     * @param request
     * @return
     * @throws FirmaHttpException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     */
    @RequestMapping(value = "/firmaRemota", method = RequestMethod.POST)
    public FirmaRemotaInformation firmaRemota(
                @RequestBody FirmaRemotaInformation firmaRemotaInformation, 
                @RequestParam(required = true) String hostId,
                @RequestParam(required = true) String codiceAzienda,
                HttpServletRequest request) throws FirmaHttpException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(hostId);
        try {
            FirmaRemotaInformation res = firmaRemotaInstance.firma(firmaRemotaInformation, codiceAzienda, request);
            return res;
        } catch (Exception ex) {
            logger.error("errore nella firma", ex);
            throw ex;
        }
    }
    
    @RequestMapping(value = "/firmaRemotaMultpart", consumes = "multipart/form-data", method = RequestMethod.POST)
    public FirmaRemotaInformation firmaRemotaMultipart(
                @RequestPart("files") MultipartFile[] files,
                @RequestPart("firmaRemotaInformation") FirmaRemotaInformation firmaRemotaInformation,
                @RequestParam(required = true) String hostId,
                HttpServletRequest request) throws FirmaHttpException, FirmaRemotaConfigurationException {
       //MultipartHttpServletRequest
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(hostId);
        FirmaRemotaInformation res = firmaRemotaInstance.firmaMultipart(files, firmaRemotaInformation, hostId, request);
        return res;
    }

    /**
     * controlla se esistono le credenziali dell'utente passato sul sistema di memorizzazione credenziali
     * @param userInformation contiene le informazioni per identificare l'utenza
     * @param hostId l'hostId della tabella Configurations che identifica l'installazione della firma remota da utilizzare
     * @param additionalData
     * @return
     * @throws FirmaHttpException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     */
    @RequestMapping(value = "/existingCredential", method = RequestMethod.POST)
    public Boolean existingCredential(
                @RequestBody UserInformation userInformation, 
                @RequestParam(required = true) String hostId,
                @RequestParam(required = false) Map<String, Object> additionalData) throws FirmaHttpException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(hostId);
        return firmaRemotaInstance.existingCredential(userInformation, hostId, additionalData);
    }

    /**
     * setta le credenziali per l'utente passato sul sistema di memorizzazione credenziali
     * @param userInformation contiene le informazioni per identificare l'utenza
     * @param hostId l'hostId della tabella Configurations che identifica l'installazione della firma remota da utilizzare
     * @param additionalData
     * @return true se le credenziali sono state settate, false altrimenti
     * @throws FirmaHttpException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     */
    @RequestMapping(value = "/setCredential", method = RequestMethod.POST)
    public Boolean setCredential(
                @RequestBody UserInformation userInformation, 
                @RequestParam(required = true) String hostId,
                @RequestParam(required = false) Map<String, Object> additionalData) throws FirmaHttpException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(hostId);
        return firmaRemotaInstance.setCredential(userInformation, hostId, additionalData);
    }

    /**
     * rimuove le credenziali per l'utente passato sul sistema di memorizzazione credenziali
     * @param userInformation contiene le informazioni per identificare l'utenza
     * @param hostId l'hostId della tabella Configurations che identifica l'installazione della firma remota da utilizzare
     * @param additionalData
     * @return true se le credenziali sono state rimosse, false altrimenti
     * @throws FirmaHttpException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     */
    @RequestMapping(value = "/removeCredential", method = RequestMethod.POST)
    public Boolean removeCredential(
                @RequestBody UserInformation userInformation, 
                @RequestParam(required = true) String hostId,
                @RequestParam(required = false) Map<String, Object> additionalData) throws FirmaHttpException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(hostId);
        return firmaRemotaInstance.removeCredential(userInformation, hostId, additionalData);
    }
    
    /**
     * Torna i poteri di firma dell'utente passato. Per poteri di firma si intende le possibili firma che l'utente possiede sull'hostId passato
     * @param userInformation contiene le informazioni per identificare l'utenza
     * @param hostId l'hostId della tabella Configurations che identifica l'installazione della firma remota da utilizzare
     * @return i poteri di firma dell'utente
     * @throws FirmaHttpException
     * @throws FirmaRemotaConfigurationException 
     */
    @RequestMapping(value = "/getUserSigns", method = RequestMethod.POST)
    public List<FirmaRemotaUserSign> getUserSigns(
                @RequestBody UserInformation userInformation, 
                @RequestParam(required = true) String hostId) throws FirmaHttpException, FirmaRemotaConfigurationException {
        
//        List<FirmaRemotaUserSign> res = new ArrayList<>();
//        MedasUserSign uno = new MedasUserSign();
//        uno.setActive(true);
//        uno.setCertificateId("cert 1");
//        uno.setDescription("prima firma");
//        uno.setProcessId("proc 1");
//        uno.setSignPowerCode("power 1");
//        uno.setSignType(MedasUserSign.SignType.FD);
//        uno.setOtpType(Arrays.asList(MedasUserSign.OTPType.ARUBACALL));
//        res.add(uno);
//        MedasUserSign due = new MedasUserSign();
//        due.setActive(true);
//        due.setCertificateId("cert 2");
//        due.setDescription("seconda firma");
//        due.setProcessId("proc 2");
//        due.setSignPowerCode("power 2");
//        due.setSignType(MedasUserSign.SignType.FD);
//        due.setOtpType(Arrays.asList(MedasUserSign.OTPType.C100, MedasUserSign.OTPType.APP));
//        res.add(due);
//        MedasUserSign tre = new MedasUserSign();
//        tre.setActive(true);
//        tre.setCertificateId("cert 3");
//        tre.setDescription("terza firma");
//        tre.setProcessId("proc 3");
//        tre.setSignPowerCode("power 3");
//        tre.setSignType(MedasUserSign.SignType.FDA);
//        tre.setOtpType(Arrays.asList(MedasUserSign.OTPType.C100, MedasUserSign.OTPType.APP));
//        res.add(tre);
//        MedasUserSign quattro = new MedasUserSign();
//        quattro.setActive(true);
//        quattro.setCertificateId("cert 4");
//        quattro.setDescription("quarta firma");
//        quattro.setProcessId("proc 4");
//        quattro.setSignPowerCode("power 4");
//        quattro.setSignType(MedasUserSign.SignType.FD);
//        quattro.setOtpType(Arrays.asList(MedasUserSign.OTPType.SMS));
//        res.add(quattro);
//        return res;
        
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(hostId);
        return firmaRemotaInstance.getUserSigns(userInformation);
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
            row.put("provider", c.getProvider().getId());
            row.put("descrizione", c.getDescrizione());
            providersInfo.add(row);
        });
        return providersInfo;
    }
    
}
