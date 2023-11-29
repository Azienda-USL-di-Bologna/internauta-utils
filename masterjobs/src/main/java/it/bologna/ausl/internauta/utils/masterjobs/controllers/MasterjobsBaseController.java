package it.bologna.ausl.internauta.utils.masterjobs.controllers;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.utils.masterjobs.configuration.nextsdr.MasterjobsRestControllerEngineImpl;
import it.bologna.ausl.model.entities.masterjobs.Job;
import it.bologna.ausl.model.entities.masterjobs.QJob;
import it.nextsw.common.controller.RestControllerEngine;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @author gusgus
 */
@RestController
@RequestMapping(value = "${masterjobs.mapping.url.root}")
public class MasterjobsBaseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MasterjobsBaseController.class);
    
    @Autowired
    private MasterjobsRestControllerEngineImpl restControllerEngine;
    
    public RestControllerEngine getRestControllerEngine() {
        return restControllerEngine;
    }
    
    @RequestMapping(value = {"job", "job/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> job(
            @QuerydslPredicate(root = Job.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "$additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QJob.job, Job.class);
        return ResponseEntity.ok(resource);
    }
}
