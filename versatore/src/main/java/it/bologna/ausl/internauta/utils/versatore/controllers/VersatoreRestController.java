package it.bologna.ausl.internauta.utils.versatore.controllers;

import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.VersatoreFactory;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.exceptions.http.ControllerHandledExceptions;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@RestController
@RequestMapping(value = "${versatore.mapping.url}")
public class VersatoreRestController implements ControllerHandledExceptions {
    
    @Autowired
    private VersatoreFactory versatoreFactory;
    
    @RequestMapping(value = "/test/{variable}", method = RequestMethod.GET)
    public String test(@PathVariable String variable) {

        return "It works!\n" + variable;
    }
    
    @RequestMapping(value = "/controllaStatoVersamento", method = RequestMethod.GET)
    public VersamentoDocInformation controllaStatoVersamento(
            @RequestParam(required = true) Integer idDoc,
            @RequestParam(required = true) String rapporto,
            @RequestParam(required = true) String hostId) throws VersatoreProcessingException {
        VersatoreDocs versatoreDocsInstance = versatoreFactory.getVersatoreDocsInstance(hostId);
        VersamentoDocInformation versamentoDocInformation = new VersamentoDocInformation();
        versamentoDocInformation.setIdDoc(idDoc);
        versamentoDocInformation.setRapporto(rapporto);
        
        VersamentoDocInformation res = versatoreDocsInstance.versa(versamentoDocInformation);
        return res;
    }
    
    @RequestMapping(value = "/versaDocumento", method = RequestMethod.POST)
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
    public VersamentoDocInformation versa(
                @RequestBody VersamentoDocInformation versamentoInformation, 
                @RequestParam(required = true) String hostId,
                HttpServletRequest request) throws VersatoreProcessingException {
        VersatoreDocs versatoreDocsInstance = versatoreFactory.getVersatoreDocsInstance(hostId);
        VersamentoDocInformation res = versatoreDocsInstance.versa(versamentoInformation);
        
        return res;
    } 
    
}
