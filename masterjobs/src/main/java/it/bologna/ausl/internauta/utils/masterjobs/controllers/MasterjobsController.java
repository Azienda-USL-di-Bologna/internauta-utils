package it.bologna.ausl.internauta.utils.masterjobs.controllers;

import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import it.bologna.ausl.model.entities.masterjobs.Service;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
@RequestMapping(value = "${masterjobs.manager.api.url}")
public class MasterjobsController {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private MasterjobsJobsQueuer masterjobsJobsQueuer;
    
//    @Autowired
//    private MasterjobsServicesExecutionScheduler masterjobsServicesExecutionScheduler;
//    
    @RequestMapping(value = "relauchJobsInError", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = {Error.class})
    public void relauchJobsInError() {
        masterjobsJobsQueuer.relaunchJobsInError();
    }
    
    @RequestMapping(value = "getService/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<Service> getService(@PathVariable(required = true) String name) {
        Service service = entityManager.find(Service.class, name);
        return ResponseEntity.ok(service);
    }
    
    @RequestMapping(value = "addService", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<Service> addService(@RequestBody Service service) throws MasterjobsWorkerException {
        entityManager.persist(service);
        return ResponseEntity.ok(service);
    }
    
//    @RequestMapping(value = "stopService/{name}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Transactional(rollbackFor = {Error.class})
//    public void stopService(@PathVariable(required = true) String name) throws MasterjobsWorkerException {
//        masterjobsServicesExecutionScheduler.stopService(name);
//    }
    
    @RequestMapping(value = "disableService/{name}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = {Error.class})
    public void disableService(@PathVariable(required= true) String name, @RequestParam(defaultValue = "true") Boolean removeFromDB) throws MasterjobsWorkerException {
        
        Service service = entityManager.find(Service.class, name);
        if (removeFromDB) {
            entityManager.remove(service);
        } else {
            if (service.getActive()) {
                service.setActive(false);
                entityManager.merge(service);
            }
        }    
    }
}
