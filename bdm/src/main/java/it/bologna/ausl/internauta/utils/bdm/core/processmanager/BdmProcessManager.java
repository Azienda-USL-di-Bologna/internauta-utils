package it.bologna.ausl.internauta.utils.bdm.core.processmanager;

import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess;
import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess.BdmStatus;
import it.bologna.ausl.internauta.utils.bdm.core.Step;
import it.bologna.ausl.internauta.utils.bdm.core.Task;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.BdmExeption;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.IllegalStepStateException;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.ProcessWorkFlowException;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.StorageException;
import it.bologna.ausl.internauta.utils.bdm.utilities.Bag;
import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author gdm
 */
public class BdmProcessManager {

    private static final Logger log = LoggerFactory.getLogger(BdmProcessManager.class);

    public static final String ADDING_PROCESS_TYPE = "process_type";
    public static final String ADDING_PROCESS_PARAMS = "process_params";
    private final ProcessStorageManager psm;
    private final EntityManager entityManager;
    private Map<String, Object> processBag;

    public BdmProcessManager(ProcessStorageManager psm, EntityManager entityManager, Map<String, Object> processBag) {
        this.psm = psm;
        this.entityManager = entityManager;
        this.processBag = processBag;
    }

    public String addProcess(BdmProcess p) {
        try {
            psm.saveProcess(p);
            return p.getProcessId();
        } catch (StorageException ex) {
            String error = String.format("unable to add process %s", (p != null? p.getProcessId(): "null"));
            log.error(error, ex);
        }
        return null;
    }

    public BdmProcess addProcess(Bag parameters) throws BdmExeption {
        try {
            String processType = (String) parameters.get(ADDING_PROCESS_TYPE);
            Bag processParameters = (Bag) parameters.get(ADDING_PROCESS_PARAMS);

            BdmProcess p = (BdmProcess) Class.forName("it.bologna.ausl.internauta.utils.bdm.workflows.processes." + processType).newInstance();
            p.init(processParameters);
            addProcess(p);
            return p;
        } catch (Exception ex) {
            String error = String.format("unable to instantiate process %s", (parameters != null? ((String) parameters.get(ADDING_PROCESS_TYPE)): "null"));
            log.error(error, ex);
            throw new BdmExeption(error, ex);
        }
    }

    public BdmProcess getProcess(String id) {
        try {
            BdmProcess p = psm.loadProcess(id);
            p.setEntityManager(entityManager);
            p.setProcessBag(processBag);
            return p;
        } catch (StorageException ex) {
            String error = String.format("unable to get process %s", id);
            log.error(error, ex);
            return null;
        }
    }

    public Boolean abortProcess(String id) throws StorageException {
        Objects.requireNonNull(id);
        BdmProcess p;
        try {
            p = psm.loadProcess(id);
        } catch (StorageException ex) {
            String error = String.format("unable to load process %s", id);
            log.error(error, ex);
            return false;
        }
        p.setEntityManager(entityManager);
        p.setProcessBag(processBag);
        p.setStatus(BdmStatus.ABORTED);
        try {
            psm.saveProcess(p);
        } catch (StorageException ex) {
            String error = String.format("unable to abort process %s", id);
            log.error(error, ex);
            return false;
        }
        return true;
    }
    
    public Boolean deleteProcess(String id) {

        try {
            psm.deleteProcess(id);
        } catch (StorageException ex) {
            String error = String.format("unable to delete process %s", id);
            log.error(error, ex);
            return false;
        }
        return true;
    }
    
    public Boolean updateProcess(BdmProcess p) {

        try {
            psm.saveProcess(p);
        } catch (StorageException ex) {
            String error = String.format("unable to update process %s", (p != null? p.getProcessId(): "null"));
            log.error(error, ex);
            return false;
        }
        return true;
    }

    public BdmStatus stepOnProcess(String id, Bag parameters) throws IllegalStepStateException, ProcessWorkFlowException, StorageException {
        BdmProcess p;
        try {
            p = psm.loadProcess(id);
        } catch (StorageException ex) {
            String error = String.format("unable to step-on process %s", id);
            log.error(error, ex);
            return null;
        }
        p.setEntityManager(entityManager);
        p.setProcessBag(processBag);
        BdmStatus status = p.stepOn(parameters);
        psm.saveProcess(p);
        return status;
    }

    public String stepToStep(String processId, String stepId, Bag parameters) throws IllegalStepStateException, ProcessWorkFlowException, StorageException {
        BdmProcess p;
        try {
            p = psm.loadProcess(processId);
        } catch (StorageException ex) {
            String error = String.format("unable to step-to process %s", processId);
            log.error(error, ex);
            return null;
        }
        p.setEntityManager(entityManager);
        p.setProcessBag(processBag);
        BdmStatus status = p.stepTo(stepId, parameters);
        psm.saveProcess(p);
        return status.toString();

    }

    public String addTask(String taskType, Bag taskParameters, String processId, String stepId) throws StorageException, BdmExeption {
        Objects.requireNonNull(taskType);
        Objects.requireNonNull(processId);
        Objects.requireNonNull(stepId);

        BdmProcess p = psm.loadProcess(processId);
        p.setEntityManager(entityManager);
        p.setProcessBag(processBag);
        Step s = p.getStep(stepId);
        try {
            Task t = (Task) Class.forName("it.bologna.ausl.internauta.utils.bdm.workflows.tasks." + taskType).newInstance();
            t.init(taskParameters);
            s.addTask(t);
            psm.saveProcess(p);
            return t.getTaskId();
        } catch (Exception ex) {
            String error = String.format("unable instatiate task on process %s", processId);
            log.error(error, ex);
            throw new BdmExeption(error, ex);
        }
    }
    
    public void setContext(String processId, Bag context) throws StorageException, BdmExeption {
        Objects.requireNonNull(processId);

        BdmProcess p = psm.loadProcess(processId);
        p.setEntityManager(entityManager);
        p.setProcessBag(processBag);
        p.setContext(context);
        psm.saveProcess(p);
    }
    
    public void addInContext(String processId, Bag values) throws StorageException {
        Objects.requireNonNull(processId);
        Objects.requireNonNull(values);

        BdmProcess p = psm.loadProcess(processId);
        p.setEntityManager(entityManager);
        p.setProcessBag(processBag);
        Bag currentContext = p.getContext();
        
        Map<String, Object> parameters = values.getParameters();
        Set<String> keys = parameters.keySet();
        for (String key: keys) {
            currentContext.put(key, values.get(key));
        }
        p.setContext(currentContext);
        psm.saveProcess(p);
    }
    
    public void setStepLogic(String processId, String stepId, Step.StepLogic stepLogic) throws StorageException {
        Objects.requireNonNull(processId);
        Objects.requireNonNull(stepId);
        Objects.requireNonNull(stepLogic);

        BdmProcess p = psm.loadProcess(processId);
        p.setEntityManager(entityManager);
        p.setProcessBag(processBag);
        Step step = p.getStep(stepId);
        step.setStepLogic(stepLogic);
        psm.saveProcess(p);
    }

    public void removeTask(String taskId, String processId, String stepId) throws StorageException, BdmExeption {
        Objects.nonNull(taskId);
        Objects.nonNull(processId);
        Objects.nonNull(stepId);

        BdmProcess p = psm.loadProcess(processId);
        p.setEntityManager(entityManager);
        p.setProcessBag(processBag);
        Step s = null;
        if (stepId != null) {
            s = p.getStep(stepId);
        }
        List<Step> stepList;
        if (s != null) {
            stepList = Arrays.asList(s);

        } else {
            stepList = p.getStepList();
        }
        boolean found = false;
        for (Step stmp : stepList) {
            for (Task t : stmp.getTaskList()) {
                if (t.getTaskId().equals(taskId)) {
                    stmp.getTaskList().remove(t);
                    found = true;
                    break;
                }
                if (found) {
                    break;
                }
            }

        }
        psm.saveProcess(p);
    }

    public String addStep(String processId, String stepDescription, Step.StepLogic stepLogic, String stepType, List<Step.StepLogic> allowedStepLogic) throws StorageException, BdmExeption {
        Objects.nonNull(stepType);
        Objects.nonNull(processId);
        Objects.nonNull(stepLogic);
        Objects.nonNull(stepType);
        Objects.nonNull(stepDescription);
        BdmProcess p = psm.loadProcess(processId);
        p.setEntityManager(entityManager);
        p.setProcessBag(processBag);
        Step s = new Step(stepType, stepDescription, stepLogic, allowedStepLogic);
        p.getStepList().add(s);
        psm.saveProcess(p);
        return s.getStepId();
    }

    public void removeStep(String stepId, String processId) throws StorageException {
        Objects.nonNull(stepId);
        Objects.nonNull(processId);

        BdmProcess p = psm.loadProcess(processId);
        p.setEntityManager(entityManager);
        p.setProcessBag(processBag);
        p.getStepList().remove(p.getStep(stepId));
        psm.saveProcess(p);
    }

    public List<String> getProcessIdList() throws StorageException {
        return psm.getProcessList();

    }

}
