package it.bologna.ausl.internauta.utils.bdm.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess.BdmStatus;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.ProcessWorkFlowException;
import it.bologna.ausl.internauta.utils.bdm.utilities.Bag;
import it.bologna.ausl.internauta.utils.bdm.utilities.Dumpable;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

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
    protected Bag params;
    protected Boolean auto = false;
    
    @JsonIgnore
    protected EntityManager entityManager;
    
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
    public void init(Bag p) {
        this.params = p;
    }

    public Bag getParams() {
        return params;
    }

    public void setParams(Bag params) {
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
    abstract public Result execute(Bag runningContext, Bag context, Bag params);

    /**
     * Lanciata quando il task diventa il passo corrente
     *
     * @param c contesto del processo
     * @param p parametri da passare al passo
     */
    //abstract public void stepIn(Context c, Bag p);

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

    public Map<String, Object> getProcessBag() {
        return processBag;
    }

    public void setProcessBag(Map<String, Object> processBag) {
        this.processBag = processBag;
    }

    @JsonIgnore
    public void undo(Bag runningContext, Bag context, Bag parameters) {
        status = BdmStatus.NOT_STARTED;
    }

}
