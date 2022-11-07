package it.bologna.ausl.internauta.utils.versatore.controllers;

import it.bologna.ausl.internauta.utils.versatore.exceptions.http.ControllerHandledExceptions;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@RestController
@RequestMapping(value = "${versatore.mapping.url}")
public class VersatoreRestController implements ControllerHandledExceptions {
    
     @RequestMapping(value = "/test/{variable}", method = RequestMethod.GET)
    public String test(@PathVariable String variable) {

        return "It works!\n" + variable;
    }
    
}
