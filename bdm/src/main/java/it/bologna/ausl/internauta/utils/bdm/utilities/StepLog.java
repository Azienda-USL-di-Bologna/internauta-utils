package it.bologna.ausl.internauta.utils.bdm.utilities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author gdm
 */
//@Embeddable
public class StepLog implements Serializable {
    private String stepId;
    private String stepType;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    protected ZonedDateTime executionDate;

    private Map<String, Object> logData;

    public StepLog() {
    }

    public StepLog(String stepId, String stepType, ZonedDateTime executionDate) {
        this.stepId = stepId;
        this.stepType = stepType;
        this.executionDate = executionDate;
        this.logData = null;
    }

    public StepLog(String stepId, String stepType, ZonedDateTime executionDate, Map<String, Object> logData) {
        this.stepId = stepId;
        this.stepType = stepType;
        this.executionDate = executionDate;
        this.logData = logData;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public ZonedDateTime getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(ZonedDateTime executionDate) {
        this.executionDate = executionDate;
    }

    public Map<String, Object> getLogData() {
        return logData;
    }

    public void setLogData(Map<String, Object> logData) {
        this.logData = logData;
    }

    @JsonIgnore
    public void putInLogData(String key, Object value) {
        if (logData == null)
            logData = new HashMap();

        logData.put(key, value);
    }

    @JsonIgnore
    public Object getFromLogData(String key) {
        if (logData != null)
            return logData.get(key);
        else
            return null;
    }
}
