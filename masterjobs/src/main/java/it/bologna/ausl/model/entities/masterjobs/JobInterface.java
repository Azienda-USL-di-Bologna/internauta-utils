package it.bologna.ausl.model.entities.masterjobs;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.UUID;

/**
 *
 * @author gusgus
 */
public interface JobInterface {
    
    public static enum JobState {
        READY,
        RUNNING,
        ERROR,
        DONE
    }
    
     public Long getId();

    public void setId(Long id);

    public String getName();

    public void setName(String name);

    public HashMap<String, Object> getData();

    public void setData(HashMap<String, Object> data);

    public SetInterface getSet();

    public void setSet(SetInterface set);

    public JobState getState();

    public void setState(JobState state);

    public String getError();

    public void setError(String error);
    
    public String getInsertedFrom();

    public void setInsertedFrom(String insertedFrom);

    public UUID getHash();

    public void setHash(UUID hash);
    
    public Boolean getDeferred();

    public void setDeferred(Boolean deferred);

    public Integer getExecutableCheckEveryMillis();

    public void setExecutableCheckEveryMillis(Integer executableCheckEveryMillis);

    public HashMap<String, Object> getWorkData();

    public void setWorkData(HashMap<String, Object> workData);

    public ZonedDateTime getLastExecutionTs();

    public void setLastExecutionTs(ZonedDateTime lastExecutionTs);

    public ZonedDateTime getInsertTs();

    public void setInsertTs(ZonedDateTime insertTs);
    
}
