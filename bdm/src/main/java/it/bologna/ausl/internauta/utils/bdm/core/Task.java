package it.bologna.ausl.internauta.utils.bdm.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess.BdmStatus;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.ProcessWorkFlowException;
import it.bologna.ausl.internauta.utils.bdm.utilities.Dumpable;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author andrea
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = Dumpable.BDM_CLASS_TYPE)

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Task implements Dumpable {

    public static final String TASK_PARAMETER_KEY = "GenericTask";

    private String taskId = UUID.randomUUID().toString();
    private String taskType;
    protected BdmStatus status = BdmStatus.NOT_STARTED;
    protected Map<String, Object> params;
    protected Boolean auto = false;
    
    @JsonIgnore
    protected EntityManager entityManager;
    
    @JsonIgnore
    protected ObjectMapper objectMapper;
    
    @JsonIgnore
    protected Map<String, Object> processBag;
    
    // di questa funzione va fatto l'override nelle sottoclassi
    public static String getTaskParametersKey() {
        return TASK_PARAMETER_KEY;
    }

    public Boolean getAuto() {
        return auto;
    }

    public void setAuto(Boolean auto) {
        this.auto = auto;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    protected ZonedDateTime stepOnts;

    public ZonedDateTime getStepOnts() {
        return stepOnts;
    }

    public void setStepOnTimeStamp(ZonedDateTime stepOnts) {
        this.stepOnts = stepOnts;
    }

    public void reset() {
        status = BdmStatus.NOT_STARTED;
        params = null;
    }
    
    @JsonIgnore
    public void init(Map<String, Object> p) {
        this.params = p;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public abstract String getTaskType();

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public abstract String getTaskVersion();

//    public abstract void setTaskType(String taskType); {
//        this.taskType = taskType;
//    }
    abstract public Result execute(Map<String, Object> runningContext, Map<String, Object> context, Map<String, Object> params);

    /**
     * Lanciata quando il task diventa il passo corrente
     *
     * @param c contesto del processo
     * @param p parametri da passare al passo
     */
    //abstract public void stepIn(Context c, Map<String, Object> p);

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setStatus(BdmStatus status) {
        this.status = status;
    }

    public BdmStatus getStatus() {
        return status;
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

    @JsonIgnore
    public final void taskUndo(Map<String, Object> runningContext, Map<String, Object> context, Map<String, Object> parameters) {
        status = BdmStatus.NOT_STARTED;
        undo(runningContext, context, parameters);
    }

    public void undo(Map<String, Object> runningContext, Map<String, Object> context, Map<String, Object> parameters) {}
    
    public Map<String, Object> buildLogData(Map<String, Object> runningContext, Map<String, Object> context, Map<String, Object> params) throws ProcessWorkFlowException {
        return null;
    }
}
