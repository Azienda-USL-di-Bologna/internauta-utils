package it.bologna.ausl.internauta.utils.firma.jnj.controllers;

import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParams;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParamsComponent;
import it.bologna.ausl.internauta.utils.firma.remota.controllers.*;
import it.bologna.ausl.internauta.utils.firma.remota.FirmaRemota;
import it.bologna.ausl.internauta.utils.firma.remota.FirmaRemotaFactory;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaException;
import it.bologna.ausl.internauta.utils.firma.repositories.RequestParameterRepository;
import it.bologna.ausl.model.entities.firma.RequestParameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
@RequestMapping(value = "${firma.jnj.mapping.url}")
public class FirmaJnJRestController implements ControllerHandledExceptions {

    @Autowired
    private RequestParameterRepository requestParameterRepository;

    @RequestMapping(value = "/test/{nome}/{cognome}", method = RequestMethod.GET)
    public String test(@PathVariable String nome, @PathVariable String cognome) {

//        System.out.println("ESITO: " + var);
        return "ciao " + nome + cognome;
    }
    
    @RequestMapping(value = "/getParameters", method = RequestMethod.GET)
    public SignParams getParameters(@RequestParam(required = true) String token) {
        List<RequestParameter> findAll = requestParameterRepository.findAll();
        for (RequestParameter requestParameter : findAll) {
            System.out.println(requestParameter.getId() + " " + requestParameter.getData().toString());
        }
        SignParamsComponent.SignDocument signDocument = new SignParamsComponent.SignDocument();
        signDocument.setFile("http://aaaaa.pdf");
        signDocument.setId("file-1");
        signDocument.setMimeType("application/pdf");
        signDocument.setName("aaaa.pdf");
        signDocument.setSignType(SignParamsComponent.SignDocument.SignTypes.PADES);
        signDocument.setSource(SignParamsComponent.SignDocument.Sources.URI);
        signDocument.setType("AllegatoPicoNuovoPU");
        
        List<SignParamsComponent.SignDocument> signFileList = Arrays.asList(signDocument);
        
        SignParamsComponent.EndSign endSign = new SignParamsComponent.EndSign();
        endSign.setCallBackUrl("aaaaa");
        Map<String, Object> endSignParams = new HashMap();
        endSignParams.put("param1", "value1");
        endSign.setEndSignParams(endSignParams);
//        endSign.setSignResult(SignParamsComponent.EndSign.SignResult.ALL_SIGNED);
//        endSign.setSignedFileList(signFileList);
        SignParams s = new SignParams();
        s.setUserId("gdm");
        s.setEndSign(endSign);
        s.setServerUrl(token);
        return null;
    }

    
    
}
