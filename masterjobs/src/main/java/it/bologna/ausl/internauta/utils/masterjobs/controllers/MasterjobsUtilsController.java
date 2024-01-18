package it.bologna.ausl.internauta.utils.masterjobs.controllers;

import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsWorkingObject;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.MasterjobsJobsQueuer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
public class MasterjobsUtilsController {
    
    @Autowired
    private MasterjobsJobsQueuer masterjobsJobsQueuer;
    
    @RequestMapping(value = "getWorkingObjectsNumber", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = {Error.class})
    public ResponseEntity<Long> getWorkingObjectsNumber(@RequestParam(required = true) String workingObectjId, @RequestParam(required = true) String workingObectType) {
        Long workingObjectNumber = masterjobsJobsQueuer.getWorkingObjectNumber(new MasterjobsWorkingObject(workingObectjId, workingObectType));
        return ResponseEntity.ok(workingObjectNumber);
    }
    
}
