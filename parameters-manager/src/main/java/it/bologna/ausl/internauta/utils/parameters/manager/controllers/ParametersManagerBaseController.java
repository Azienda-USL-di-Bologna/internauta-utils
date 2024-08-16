package it.bologna.ausl.internauta.utils.parameters.manager.controllers;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.utils.parameters.manager.configuration.nextsdr.ParamtersManagerRestControllerEngineImpl;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.configurazione.QParametroAziende;
import it.nextsw.common.controller.BaseCrudController;
import it.nextsw.common.controller.RestControllerEngine;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${parameters-manager.mapping.url.root}")
public class ParametersManagerBaseController  {
    
    @Autowired
    private ParamtersManagerRestControllerEngineImpl restControllerEngine;

//    @Override
    public RestControllerEngine getRestControllerEngine() {
        return restControllerEngine;
    }
    
    @RequestMapping(value = {"parametroaziende", "parametroaziende/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> parametroaziende(
            @QuerydslPredicate(root = ParametroAziende.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QParametroAziende.parametroAziende, ParametroAziende.class);
        return ResponseEntity.ok(resource);
    }
}
