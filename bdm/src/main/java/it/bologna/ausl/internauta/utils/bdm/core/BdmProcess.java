package it.bologna.ausl.internauta.utils.bdm.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.IllegalStepStateException;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.ProcessWorkFlowException;
import it.bologna.ausl.internauta.utils.bdm.utilities.Dumpable;
import it.bologna.ausl.internauta.utils.bdm.utilities.StepLog;
import it.bologna.ausl.internauta.utils.bdm.workflows.processes.SampleProcess;
import it.bologna.ausl.internauta.utils.bdm.workflows.tasks.SampleTask;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = Dumpable.BDM_CLASS_TYPE)
//@JsonSubTypes({  
//    @JsonSubTypes.Type(value = FirmaProcess.class, name = "firmaProcess"),  
//   
//})  
@JsonSubTypes({
    @JsonSubTypes.Type(value = SampleProcess.class, name = "it.bologna.ausl.internauta.utils.bdm.workflows.processes.SampleProcess")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public  class BdmProcess implements Dumpable, Serializable {

    public static enum ProcessDirection{BACKWARD, FORWARD};

    public static enum BdmStatus {
        NOT_STARTED,
        RUNNING,
        FINISHED,
        ABORTED,
        ERROR
    }
    
    public static final String CURRENT_PROCESS = "__CURRENT_PROCESS__";
    public static final String CURRENT_STEP = "__CURRENT_STEP__";
    public static final String TRANSACTION_ID_FIELD = "transactionId"; // nome della proprietà che identifica il transactionId
    
    // campo che identifica la transazione corrente sul processo, ogni volta che viene salvato, questo campo viene controllato per evitare problemi di concorrenza
    private Integer transactionId = 0;
    private String processVersion;
    private String processType;
    private String processId = UUID.randomUUID().toString();
    protected Map<String, Object> context;
    
    /*
    contiene quello che gli si passa in fase di creazione della classe bdmProcessManager
    idealmente contiene istanze di oggetti Autowired di cui si può aver bisogno all'interno dei task
    */
    Map<String, Object> processBag;
    
//    @JsonIgnore
    protected Map<String, Object> runningContext = new HashMap();
    private List<Step> stepList = new ArrayList<>();
    private List<String> executedStepList = new ArrayList<>();
    // protected String processVersion = null;
    protected BdmStatus status = BdmStatus.NOT_STARTED;
    private int currentStepIndex = 0;
    
    @JsonIgnore
    protected EntityManager entityManager;
    
    private List<StepLog> stepsLog = new ArrayList<>();

    public void init(Map<String, Object> parameters) {
        setContext(parameters);
        Step s = new Step("SampleStep", "Sample Process", Step.StepLogic.SEQ, Arrays.asList(Step.StepLogic.SEQ, Step.StepLogic.ALL));
        addStep(s);
        Task t = new SampleTask();
        s.addTask(t);
        addStep(s);
    }
    
     public String getProcessVersion() {
        return "0.1";
    }

    public String getProcessType() {
        return this.getClass().toString();
    }

    
//    public abstract void init(Map<String, Object> parameters);
//
//    public abstract String getProcessType();
//    
//    public abstract String getProcessVersion();

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    protected ZonedDateTime stepOnts;

    public ZonedDateTime getStepOnts() {
        return stepOnts;
    }

    public void setStepOnts(ZonedDateTime stepOnts) {
        this.stepOnts = stepOnts;
    }

    public void setProcessVersion(String processVersion) {
        this.processVersion = processVersion;
    }

    public List<String> getExecutedStepList() {
        return executedStepList;
    }

    public void setExecutedStepList(List<String> executedStepList) {
        this.executedStepList = executedStepList;
    }

    public void setStepList(List<Step> stepList) {
        this.stepList = stepList;
    }

    public List<Step> getStepList() {
        return stepList;
    }

    public BdmStatus getStatus() {
        return status;
    }

    public void setStatus(BdmStatus status) {
        this.status = status;
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public void setCurrentStepIndex(int currentStepIndex) {
        this.currentStepIndex = currentStepIndex;
    }

    public String getProcessId() {
        return processId;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    @JsonIgnore
    public EntityManager getEntityManager() {
        return entityManager;
    }

    @JsonIgnore
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @JsonIgnore
    public Map<String, Object> getProcessBag() {
        return processBag;
    }

    @JsonIgnore
    public void setProcessBag(Map<String, Object> processBag) {
        this.processBag = processBag;
    }

    public void setContext(Map<String, Object> c) {
        context = c;
    }

    public void addStep(Step s) {
        stepList.add(s);
    }

    public List<StepLog> getStepsLog() {
        return stepsLog;
    }

    public void setStepsLog(List<StepLog> stepsLog) {
        this.stepsLog = stepsLog;
    }

    public void setProcessType(String type) {
        processType = type;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    @JsonIgnore
    public Map<String, Object> getRunningContext() {
        return runningContext;
    }

    @JsonIgnore
    public void setRunningContext(Map<String, Object> runningContext) {
        this.runningContext = runningContext;
    }
    
    @JsonIgnore
    public Step getStep(String stepId) {
        return stepList.stream().filter(step-> step.getStepId().equals(stepId)).findFirst().get();
    }

    @JsonIgnore
    public Step getStepByType(String stepType) {
        return stepList.stream().filter(step-> step.getStepType().equals(stepType)).findFirst().get();
    }

    @JsonIgnore
    public Step getNextStep(String stepId) {
        for (int i=0; i<stepList.size(); i++) {
            if (stepList.get(i).getStepId().equals(stepId)) {
                if (i < stepList.size() - 1)
                    return stepList.get(i + 1);
            }
        }
        return null;
    }

    @JsonIgnore
    public BdmStatus stepOn(Map<String, Object> params) throws IllegalStepStateException, ProcessWorkFlowException {
        runningContext.put(CURRENT_PROCESS, this);
        status = BdmStatus.RUNNING;
        stepOnts = ZonedDateTime.now();
        Step step = stepList.get(currentStepIndex);
        step.setEntityManager(entityManager);
        step.setProcessBag(processBag);
        runningContext.put(CURRENT_STEP, step);

        // inserisco i dati necessari nella lista di StepLog
        stepsLog.add(new StepLog(step.getStepId(), step.getStepType(), ZonedDateTime.now()));

        // se lo step sta partendo ora eseguo i task in entrata
        if (step.getStepStatus() == BdmStatus.NOT_STARTED) {
            step.executeOnEnterTasks(runningContext, context, params);
        }
        
        BdmStatus stepStatus = step.stepOn(runningContext, context, params);
        switch (stepStatus) {
            case ABORTED:
            case FINISHED:

                // lo step è finito, eseguo gli eventuali task on exit dello step corrente
                step.executeOnExitTasks(runningContext, context, params);
                executedStepList.add(step.getStepId());
                currentStepIndex++;

                boolean stepChanged = true;
                while (currentStepIndex < stepList.size()) {
                    if (stepChanged) {
                        step = stepList.get(currentStepIndex);
                        step.setEntityManager(entityManager);
                        step.setProcessBag(processBag);
                        runningContext.put(CURRENT_STEP, step);
//                        step.reset();
                        stepsLog.add(new StepLog(step.getStepId(), step.getStepType(), ZonedDateTime.now()));
                        step.executeOnEnterTasks(runningContext, context, params);
                        stepChanged = false;
                    }
                    if ((step.getStepLogic() == Step.StepLogic.SEQ && step.getTaskList().get(step.getCurrentTaskIndex()).getAuto())
                            || (step.getStepLogic() != Step.StepLogic.SEQ && !step.getNotExecutedAutoTask().isEmpty())) {
                        stepStatus = step.stepOn(runningContext, context, params);
                        if (stepStatus == BdmStatus.FINISHED) {
                            // lo step è finito, eseguo gli eventuali task on exit dello step corrente
                            step.executeOnExitTasks(runningContext, context, params);
                            executedStepList.add(step.getStepId());
                            currentStepIndex++;
                            stepChanged = true;
//                                step = stepList.get(currentStepIndex);
//                                runningContext.put(CURRENT_STEP, step);
                        }
                    }
                    else {
                        break;
                    }
                }

//                // eseguo i task on enter del prossimo step
//                if (currentStepIndex < stepList.size()) {
//                    step = stepList.get(currentStepIndex);
//                    runningContext.put(CURRENT_STEP, step);
//                    step.executeOnEnterTasks(runningContext, context, params);
////                    while (currentStepIndex < stepList.size() && step.getStepLogic() == Step.StepLogic.SEQ) {
//                    while (currentStepIndex < stepList.size()) {
//                        if ((step.getStepLogic() == Step.StepLogic.SEQ && step.getTaskList().get(step.getCurrentTaskIndex()).getAuto()) ||
//                            (step.getStepLogic() != Step.StepLogic.SEQ && !step.getNotExecutedAutoTask().isEmpty()) 
//                           ) {
//                            stepStatus = step.stepOn(runningContext, context, params);
//                            if (stepStatus == BdmStatus.FINISHED) {
//                                // lo step è finito, eseguo gli eventuali task on exit dello step corrente
//                                step.executeOnExitTasks(runningContext, context, params);
//                                executedStepList.add(step.getStepId());
//                                currentStepIndex++;
//
//                                if (currentStepIndex < stepList.size()) {
//                                    step = stepList.get(currentStepIndex);
//                                    runningContext.put(CURRENT_STEP, step);
//                                    step.executeOnEnterTasks(runningContext, context, params);
//                                }
//                            }
//                        }
//                        else {
//                            break;
//                        }
//                    }
//                }
                if (currentStepIndex >= stepList.size()) {
                    status = BdmStatus.FINISHED;
                }
                break;
            case RUNNING:
                break;
            case ERROR:
            default:
                status = BdmStatus.ERROR;
                throw new ProcessWorkFlowException("Error in task");
        }

        return status;
    }

    @JsonIgnore
    public BdmStatus stepTo(String stepId, Map<String, Object> params) throws IllegalStepStateException, ProcessWorkFlowException {
        //annulliamo lo step attuale
        Step currentStep = stepList.get(currentStepIndex);
        currentStep.setEntityManager(entityManager);
        currentStep.setProcessBag(processBag);
        runningContext.put(CURRENT_PROCESS, this);
        runningContext.put(CURRENT_STEP, currentStep);
        currentStep.undo(runningContext, context, params);
        //Troviamo dove andare
        Step nextStep = null;
        Integer nextStepIndex = null;
        for (Step s : stepList) {
            if (s.getStepId().equalsIgnoreCase(stepId)) {
                nextStep = s;
                nextStepIndex = stepList.indexOf(s);
                break;
            }

        }
        if (nextStep == null || nextStepIndex == null) {
            throw new ProcessWorkFlowException("Unable to find next step :" + stepId);
        }
        
        nextStep.setEntityManager(entityManager);
        nextStep.setProcessBag(processBag);

        //controllo che non sia uno stepTo allo step corrente
        if (currentStep.getStepId().equals(nextStep.getStepId())) {
            return stepOn(params);
        }

        //controllo che il nextStep sia previsto tra i possibili
        if (currentStep.getForwardStepList().indexOf(nextStep.getStepId()) == -1 && currentStep.getBackwardStepList().indexOf(nextStep.getStepId()) == -1) {
            throw new ProcessWorkFlowException("Step from :" + currentStep.getStepType() + " to : " + nextStep.getStepType() + " is not allowed");
        }
        //cerco in quelli fatti per undoare....
        int n = executedStepList.lastIndexOf(nextStep.getStepId());
        if (n != -1) {
            List<String> stepIdToUndo = executedStepList.subList(n, executedStepList.size());
            //esegui undo per tutti i task nella lista di quelli eseguiti
//            stepList.stream().filter((t) -> (stepIdToUndo.indexOf(t.getStepId()) != -1)).forEach((t) -> {
//                t.undo(context, params);
//            });
            
            // filtro nella lista degli step quelli da undoare ed eseguo l'undo scorrendolinell'ordine inverso
            stepList.stream().filter((s) -> (stepIdToUndo.indexOf(s.getStepId()) != -1)).
                    collect(Collectors.toCollection(LinkedList<Step>::new)).
                    descendingIterator().
                        forEachRemaining((s) -> {
//                            runningContext.put(CURRENT_STEP, s);
                            s.undo(runningContext, context, params);
            });

            executedStepList.removeAll(stepIdToUndo);
        }
        currentStepIndex = nextStepIndex;
        
        Step step = stepList.get(currentStepIndex);
        step.setEntityManager(entityManager);
        step.setProcessBag(processBag);
        runningContext.put(CURRENT_STEP, step);
//        step.reset();
//        step.executeOnEnterTasks(runningContext, context, params);
        
        return stepOn(params);
    }


    @JsonIgnore
    public Step getCurrentStep() {
        if (stepList != null && currentStepIndex < stepList.size()) {
            return stepList.get(currentStepIndex);
        }
        return null;
    }

    @JsonIgnore
    public Task getCurrentTask() {
        if (stepList != null) {
            return stepList.get(currentStepIndex).getCurrentTask();
        }
        return null;
    }

    @JsonIgnore
    public List<String> getForwardSteps() {
        if (getCurrentStep() != null) {
            return getCurrentStep().getForwardStepList();
        }
        return null;
    }

    @JsonIgnore
    public List<String> getBackwardSteps() {
        if (getCurrentStep() != null) {
            return getCurrentStep().getBackwardStepList();
        }
        return null;
    }
    
    @JsonIgnore
    public boolean isLastStep(String stepId) throws ProcessWorkFlowException {
        for (int i=0; i<stepList.size(); i++) {
            if (stepList.get(i).getStepId().equals(stepId)) {
                return i == stepList.size() -1;
            }
        }
        throw new ProcessWorkFlowException("step not found");
    }
    
     @JsonIgnore
    public boolean isLastStep(Step step) throws ProcessWorkFlowException {
        return isLastStep(step.getStepId());
    }
//    
//    @JsonIgnore
//    public Step getStepById(String stepId){
//        for (int i=0; i<stepList.size(); i++) {
//            if (stepList.get(i).getStepId().equals(stepId)) {
//                return stepList.get(i);
//            }
//        }
//        return null;
//    }
}
