package it.bologna.ausl.internauta.utils.ribaltone.controllers;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.nextsdr.RibaltoneRestControllerEngineImpl;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import it.nextsw.common.controller.BaseCrudController;
import it.nextsw.common.controller.RestControllerEngine;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import jakarta.servlet.http.HttpServletRequest;
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
 * @author Tom
 */
@RestController
@RequestMapping(value = "${ribaltonedati.mapping.url.root}")
public class RibaltoneDatiBaseController extends BaseCrudController {

    @Autowired
    private RibaltoneRestControllerEngineImpl restControllerEngine;

    @Override
    public RestControllerEngine getRestControllerEngine() {
        return restControllerEngine;
    }

    @RequestMapping(value = {"ribaltonedataconfiguration", "ribaltonedataconfiguration/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ribaltonedataconfiguration(
        @QuerydslPredicate(root = RibaltoneDataConfiguration.class) Predicate predicate,
        Pageable pageable,
        @RequestParam(required = false) String projection,
        @PathVariable(required = false) String id,
        HttpServletRequest request,
        @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QRibaltoneDataConfiguration.ribaltoneDataConfiguration, RibaltoneDataConfiguration.class);
        return ResponseEntity.ok(resource);
    }

}
