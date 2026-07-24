package it.bologna.ausl.internauta.utils.bdm.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess.BdmStatus;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.BdmRuntimeExceptionContainer;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.IllegalStepStateException;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.ProcessWorkFlowException;
import it.bologna.ausl.internauta.utils.bdm.utilities.Dumpable;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author andrea
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Step implements Dumpable {

    public static enum StepLogic {

        SEQ, // tutti i passi verranno eseguiti sequenzialmente
        ANY, // tutti i passi possono essere eseguiti contemporaneamente, ma per procedere basta che ne sia completato uno
        ALL // tutti i passi posso essere eseguiti contemporaneamente, ma per procedere devono essere tutti compleatati
    }

    private String stepId = UUID.randomUUID().toString();
    private List<Task> taskList;
    private List<Task> enterTaskList;
    private List<Task> exitTaskList;
    private List<Result> taskResults;
    private StepLogic stepLogic = StepLogic.SEQ;
    private String stepType = "UNKNOWN";
    private String description = "No Description";
    private List<String> forwardStepList;
    private List<String> backwardStepList;
    private Boolean enterDone = false;
    private Boolean exitDone = false;
    private List<StepLogic> allowedStepLogic;

    //private Map<String, Task> taskMap;
    private int currentTaskIndex = 0;

    private BdmStatus stepStatus = BdmStatus.NOT_STARTED;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    protected ZonedDateTime stepOnTimeStamp;

    @JsonIgnore
    protected EntityManager entityManager;
    
    @JsonIgnore
    protected ObjectMapper objectMapper;
    
    @JsonIgnore
    protected Map<String, Object> processBag;

    public Step() {
    }

    public Step(String stepType, String description, StepLogic stepLogic, List<StepLogic> allowedStepLogic) {

        this.stepLogic = stepLogic;
        this.taskList = new ArrayList<>();
        this.taskResults = new ArrayList<>();
        this.forwardStepList = new ArrayList<>();
        this.backwardStepList = new ArrayList<>();
        this.description = description;
        this.stepType = stepType;
        this.allowedStepLogic = allowedStepLogic;
    }
    
//    public void resetExcept(List<Class> taskClassesToExclude) {
//        enterDone = false;
//        exitDone = false;
//        stepStatus = BdmStatus.NOT_STARTED;
//        currentTaskIndex = 0;
//        stepOnTimeStamp = null;
//        
//        // resetto tutti i task tranne quelli da escludere
//        if (taskList != null && !taskList.isEmpty()) {
//            taskList.stream().forEach((Task t) -> {
//                boolean exclude = false;
//                if (taskClassesToExclude != null) {
//                    for (Class<Task> taskToExclude: taskClassesToExclude) {
//                        if (taskToExclude.isAssignableFrom(t.getClass())) {
//                            exclude = true;
//                            break;
//                        }
//                    }
//                }
//                if (!exclude)
//                    t.reset();
//            });
//        }
//        
//        // resetto tutti i task nella lista dei task in entrata tranne quelli da escludere
//        if (enterTaskList != null && !enterTaskList.isEmpty()) {
//            enterTaskList.stream().forEach(t -> {
//                boolean exclude = false;
//                if (taskClassesToExclude != null) {
//                    for (Class<Task> taskToExclude: taskClassesToExclude) {
//                        if (taskToExclude.isAssignableFrom(t.getClass())) {
//                            exclude = true;
//                            break;
//                        }
//                    }
//                }
//                if (!exclude)
//                    t.reset();
//            });
//        }
//
//        // resetto tutti i task nella lista dei task in uscita tranne quelli da escludere
//        if (exitTaskList != null && !exitTaskList.isEmpty()) {
//            exitTaskList.stream().forEach(t -> {
//                boolean exclude = false;
//                if (taskClassesToExclude != null) {
//                    for (Class<Task> taskToExclude: taskClassesToExclude) {
//                        if (taskToExclude.isAssignableFrom(t.getClass())) {
//                            exclude = true;
//                            break;
//                        }
//                    }
//                }
//                if (!exclude)
//                    t.reset();       
//            });
//        }
//        
//        // resetto tutti i task nella lista dei task in uscita tranne quelli da escludere
//        if (taskResults != null && !taskResults.isEmpty()) {
//            taskResults.stream().forEach(t -> {
//                boolean exclude = false;
//                if (taskClassesToExclude != null) {
//                    for (Class<Task> taskToExclude: taskClassesToExclude) {
//                        if (taskToExclude.isAssignableFrom(t.getClass())) {
//                            exclude = true;
//                            break;
//                        }
//                    }
//                }
//                if (!exclude)
//                    t = null;      
//            });
//        }
//    }
//
//    public void reset() {
//        resetExcept(null);
//    }

    public void addOnEnterTask(Task task) {
        if (enterTaskList == null)
            enterTaskList = new ArrayList<>();
        enterTaskList.add(task);
    }

    public void addOnExitTask(Task task) {
        if (exitTaskList == null)
            exitTaskList = new ArrayList<>();
        exitTaskList.add(task);
    }

    public void addBackwardStep(String stepId) {
        if (backwardStepList == null)
            backwardStepList = new ArrayList<>();
        backwardStepList.add(stepId);
    }

    public void addForwardStep(String stepId){
        if (forwardStepList == null)
            forwardStepList = new ArrayList<>();
        forwardStepList.add(stepId);
    }

    public void reset() {
        enterDone = false;
        exitDone = false;
        stepStatus = BdmStatus.NOT_STARTED;
        currentTaskIndex = 0;
        stepOnTimeStamp = null;
        taskList.forEach(t -> t.setStatus(BdmStatus.NOT_STARTED));
        for (int i = 0; i < taskResults.size(); i++) {
            taskResults.set(i, null);
        }
    }
    
    public void undo(Map<String, Object> runningContext, Map<String, Object> context, Map<String, Object> params) throws ProcessWorkFlowException {


        runningContext.put(BdmProcess.CURRENT_STEP, this);
        try {
            if (exitTaskList != null) {
                exitTaskList.stream().filter((t) -> (t.getStatus() == BdmStatus.FINISHED || t.getStatus() == BdmStatus.RUNNING)).
                    collect(Collectors.toCollection(LinkedList<Task>::new)).descendingIterator().
                    forEachRemaining((t) -> {
                        t.setEntityManager(entityManager);
                        t.setObjectMapper(objectMapper);
                        t.setProcessBag(processBag);
                        t.taskUndo(runningContext, context, params);
                        if (t.getStatus() == BdmStatus.ERROR) {
                            stepStatus = BdmStatus.ERROR;
                            throw new BdmRuntimeExceptionContainer(new ProcessWorkFlowException("error executing task: " + t.toString()));
                        }
                });
            }

            if (taskList != null) {
                taskList.stream().filter((t) -> (t.getStatus() == BdmStatus.FINISHED || t.getStatus() == BdmStatus.RUNNING || t.getStatus() == BdmStatus.ERROR)).
                    collect(Collectors.toCollection(LinkedList<Task>::new)).descendingIterator().
                    forEachRemaining((t) -> {
                        t.setEntityManager(entityManager);
                        t.setObjectMapper(objectMapper);
                        t.setProcessBag(processBag);
                        t.taskUndo(runningContext, context, params);
                        if (t.getStatus() == BdmStatus.ERROR) {
                            stepStatus = BdmStatus.ERROR;
                            throw new BdmRuntimeExceptionContainer(new ProcessWorkFlowException("error executing task: " + t.toString()));
                        }
                });
            }

            if (enterTaskList != null) {
                enterTaskList.stream().filter((t) -> (t.getStatus() == BdmStatus.FINISHED || t.getStatus() == BdmStatus.RUNNING)).
                    collect(Collectors.toCollection(LinkedList<Task>::new)).descendingIterator().
                    forEachRemaining((t) -> {
                        t.setEntityManager(entityManager);
                        t.setObjectMapper(objectMapper);
                        t.setProcessBag(processBag);
                        t.taskUndo(runningContext, context, params);
                        if (t.getStatus() == BdmStatus.ERROR) {
                            stepStatus = BdmStatus.ERROR;
                            throw new BdmRuntimeExceptionContainer(new ProcessWorkFlowException("error executing task: " + t.toString()));
                        }
                });
            }
            reset();
        } catch (BdmRuntimeExceptionContainer t) {
            if (t.getException() instanceof ProcessWorkFlowException ex)
                throw ex;
            else
                throw new ProcessWorkFlowException("error executing task", t);
        }
    }

    public void executeOnEnterTasks(Map<String, Object> runningContext, Map<String, Object> context, Map<String, Object> params) throws ProcessWorkFlowException {
        if (enterDone) {
            return;
        }
        if (enterTaskList != null) {
            for (Task t : enterTaskList) {
                t.setEntityManager(entityManager);
                t.setObjectMapper(objectMapper);
                t.setProcessBag(processBag);
                if (!t.getAuto()) {
                    throw new ProcessWorkFlowException("Only automatic tasks are allowed on enter");
                }
                t.execute(runningContext, context, params);
                if (t.getStatus() == BdmStatus.ERROR) {
                    throw new ProcessWorkFlowException(String.format("task %s return error", t.getTaskType()));
                }
            }
        }
        enterDone = true;
    }
    
    public void executeOnExitTasks(Map<String, Object> runningContext, Map<String, Object> context, Map<String, Object> params) throws ProcessWorkFlowException {
        if (exitDone) {
            return;
        }
        if (exitTaskList != null) {
            for (Task t : exitTaskList) {
                t.setEntityManager(entityManager);
                t.setObjectMapper(objectMapper);
                t.setProcessBag(processBag);
                if (!t.getAuto()) {
                    throw new ProcessWorkFlowException("Only automatic tasks are allowed on enter");
                }
                t.execute(runningContext, context, params);
            }
        }
        exitDone = true;
    }

    @JsonIgnore
    Task getCurrentTask() {
        if (taskList != null) {
            Task currentTask = taskList.get(currentTaskIndex);
            currentTask.setEntityManager(entityManager);
            currentTask.setObjectMapper(objectMapper);
            currentTask.setProcessBag(processBag);
            return currentTask;
            
        }
        return null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Task> getEnterTaskList() {
        return enterTaskList;
    }

    public void setEnterTaskList(List<Task> enterTaskList) {
        this.enterTaskList = enterTaskList;
    }

    public Boolean getEnterDone() {
        return enterDone;
    }

    public void setEnterDone(Boolean enterDone) {
        this.enterDone = enterDone;
    }

    public List<Task> getExitTaskList() {
        return exitTaskList;
    }

    public void setExitTaskList(List<Task> exitTaskList) {
        this.exitTaskList = exitTaskList;
    }

    public Boolean getExitDone() {
        return exitDone;
    }

    public void setExitDone(Boolean exitDone) {
        this.exitDone = exitDone;
    }

    public ZonedDateTime getStepOnTimeStamp() {
        return stepOnTimeStamp;
    }

    public void setStepOnTimeStamp(ZonedDateTime stepOnts) {
        this.stepOnTimeStamp = stepOnts;
    }

    public List<String> getForwardStepList() {
        return forwardStepList;
    }

    public void setForwardStepList(List<String> forwardStepList) {
        this.forwardStepList = forwardStepList;
    }

    public List<String> getBackwardStepList() {
        return backwardStepList;
    }

    public void setBackwardStepList(List<String> backwardStepList) {
        this.backwardStepList = backwardStepList;
    }

    public <T extends Task> List<T> getTaskList(Class taskClass) {
        List<Task> filteredTaskList = taskList.stream().filter(t -> taskClass.isAssignableFrom(t.getClass())).collect(Collectors.toList());
        
        return (List<T>) filteredTaskList;
    }
    
    public List<Task> getTaskList() {
        return taskList;
    }

    public void setTaskList(List<Task> taskList) {
        this.taskList = taskList;
    }

    public List<Result> getTaskResults() {
        return taskResults;
    }

    public void setTaskResults(List<Result> taskResults) {
        this.taskResults = taskResults;
    }

    public StepLogic getStepLogic() {
        return stepLogic;
    }

    public void setStepLogic(StepLogic stepLogic) {
        this.stepLogic = stepLogic;
    }

    public int getCurrentTaskIndex() {
        return currentTaskIndex;
    }

    public void setCurrentTaskIndex(int currentTaskIndex) {
        this.currentTaskIndex = currentTaskIndex;
    }

    public BdmStatus getStepStatus() {
        return stepStatus;
    }

    public void setStepStatus(BdmStatus stepStatus) {
        this.stepStatus = stepStatus;
    }

    public void addTask(Task task) {
        task.setEntityManager(entityManager);
        task.setObjectMapper(objectMapper);
        task.setProcessBag(processBag);
        taskList.add(task);
        taskResults.add(null);
//        taskMap.put(task.getId(), task);
    }

    public BdmStatus stepOn(Map<String, Object> runningContext, Map<String, Object> context, Map<String, Object> params, boolean onlyAuto) throws IllegalStepStateException, ProcessWorkFlowException {
        if (stepStatus == BdmStatus.ERROR || stepStatus == BdmStatus.ABORTED || stepStatus == BdmStatus.FINISHED) {
            throw new IllegalStepStateException("cannot stepon with StepStatus: " + stepStatus.toString());
        } 
        else if (taskList == null || taskList.isEmpty()) {
//        if (taskList == null || taskList.isEmpty()) {
            stepStatus = BdmStatus.ERROR;
            throw new IllegalStepStateException("cannot stepon with no Tasks");
        }
        else {
            Result res;
            stepOnTimeStamp = ZonedDateTime.now();
            
            // se lo step sta partendo ora ne inserisco i dati necessari nella lista di StepLog del processo
//            if (stepStatus == BdmStatus.NOT_STARTED) {
//                BdmProcess currentProcess = (BdmProcess) runningContext.get(BdmProcess.CURRENT_PROCESS);
//                List<StepLog> stepsLog = currentProcess.getStepsLog();
////                StepLog currentStepLog = stepsLog.get(stepsLog.size() - 1);
//                stepsLog.add(new StepLog(getStepId(), getStepType(), stepOnTimeStamp));
//            }
            
            stepStatus = BdmStatus.RUNNING;
            

//            while (taskResults.size() < taskList.size()) {
//                taskResults.add(null);
//            }
            switch (stepLogic) {
                case SEQ:
                    Task currentTask = taskList.get(currentTaskIndex);
                    currentTask.setEntityManager(entityManager);
                    currentTask.setObjectMapper(objectMapper);
                    currentTask.setProcessBag(processBag);
                    currentTask.setStepOnTimeStamp(stepOnTimeStamp);
                    res = currentTask.execute(runningContext, context, params);

                    taskResults.set(currentTaskIndex, res);
                    if (res.getStatus() == BdmStatus.FINISHED) {

                        //Eseguo tutti i task automatici
                        currentTaskIndex++;
                        while (currentTaskIndex < taskList.size() && taskList.get(currentTaskIndex).getAuto()) {
                            currentTask = taskList.get(currentTaskIndex);
                            currentTask.setEntityManager(entityManager);
                            currentTask.setObjectMapper(objectMapper);
                            currentTask.setProcessBag(processBag);
                            currentTask.setStepOnTimeStamp(stepOnTimeStamp);
                            res = currentTask.execute(runningContext, context, params);
                            if (res.getStatus() != BdmStatus.FINISHED) {
                                throw new ProcessWorkFlowException("Automatic task didn't finish!");
                            }
                            taskResults.set(currentTaskIndex, res);
                            currentTaskIndex++;
                        }
                        if (currentTaskIndex >= taskList.size()) {
                            stepStatus = BdmStatus.FINISHED;
                        }
                    } else if (res.getStatus() == BdmStatus.ERROR) {
                        throw new ProcessWorkFlowException("task error");
                    }
                    break;
                case ANY:
                    boolean finishedOne = false;

                    // per ogni task, se è automatico lo eseguo sempre, 
                    // se non è automatico lo eseguo solo se non ne è proceduto già uno
                    for (int i = 0; i < taskList.size(); i++) {
                        Task t = taskList.get(i);
                        if (!onlyAuto || t.getAuto()) {
                            t.setEntityManager(entityManager);
                            t.setObjectMapper(objectMapper);
                            t.setProcessBag(processBag);
                            if (t.getStatus() == BdmStatus.NOT_STARTED || t.getStatus() == BdmStatus.RUNNING) {
                                if (t.getAuto() || !finishedOne) {
                                    res = t.execute(runningContext, context, params);
                                    taskResults.set(i, res);
                                    if (res.getStatus() == BdmStatus.ERROR) {
                                        throw new ProcessWorkFlowException("task error");
                                    } else if (!t.getAuto() && res.getStatus() == BdmStatus.FINISHED) {
                                        finishedOne = true;
                                    }
                                }   
                            }
                        }
                    }
                    if (finishedOne || getNotFinishedTasks().isEmpty()) {
                        stepStatus = BdmStatus.FINISHED;
                    }
//                    else {
//                        stepStatus = BdmStatus.ERROR;
//                        throw new ProcessWorkFlowException("no task stepped on");
//                    }
                    break;
                case ALL:
                    int finishedTasks = 0;
//                    int listSize = taskList.size();
                    for (int i = 0; i < taskList.size(); i++) {
                        Task t = taskList.get(i);
                        if (!onlyAuto || t.getAuto()) {
                            t.setEntityManager(entityManager);
                            t.setObjectMapper(objectMapper);
                            t.setProcessBag(processBag);
                            switch (t.getStatus()) {
                                case NOT_STARTED, RUNNING, ERROR -> {
                                    res = t.execute(runningContext, context, params);
                                    taskResults.set(i, res);
                                    if (res.getStatus() == BdmStatus.ERROR) {
                                        stepStatus = BdmStatus.ERROR;
                                        throw new ProcessWorkFlowException("error executing task: " + t.toString());
                                    }
                                }
                            }
                            if (t.getStatus() == BdmStatus.FINISHED) {
                                finishedTasks++;
                            }
                        }
                    }
                    if (finishedTasks == taskList.size()) {
                        stepStatus = BdmStatus.FINISHED;
                    }
            }
        }
        return stepStatus;
    }

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public List<StepLogic> getAllowedStepLogic() {
        return allowedStepLogic;
    }

    public void setAllowedStepLogic(List<StepLogic> allowedStepLogic) {
        this.allowedStepLogic = allowedStepLogic;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getProcessBag() {
        return processBag;
    }

    public void setProcessBag(Map<String, Object> processBag) {
        this.processBag = processBag;
    }

    public List<Task> getNotExecutedAutoTask() {
        List notExecutedAutoTask = new ArrayList();
        for (Task task: taskList) {
            if (task.getStatus() == BdmStatus.NOT_STARTED && task.getAuto())
                notExecutedAutoTask.add(task);
        }
        return notExecutedAutoTask;
    }

    public List<Task> getNotFinishedTasks() {
        List notFinishedTask = null;
        notFinishedTask = taskList.stream().filter((task) -> (
                task.getStatus() != BdmStatus.FINISHED && 
                task.getStatus() != BdmStatus.ERROR && 
                task.getStatus() != BdmStatus.ABORTED)).collect(Collectors.toList());
        return notFinishedTask;
    }
}
