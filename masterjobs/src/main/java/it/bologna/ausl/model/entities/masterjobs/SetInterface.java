package it.bologna.ausl.model.entities.masterjobs;

import java.time.ZonedDateTime;
import java.util.List;

/**
 *
 * @author gusgus
 */
public interface SetInterface {
    
    public static enum SetPriority {
        NORMAL,
        HIGH,
        HIGHEST
    }
        
    public Long getId();

    public void setId(Long id);

    public String getObjectId();

    public void setObjectId(String objectId);

    public String getObjectType();

    public void setObjectType(String objectType);

    public Boolean getWaitObject();

    public void setWaitObject(Boolean waitObject);

    public Set.SetPriority getPriority();

    public void setPriority(Set.SetPriority priority);

    public String getApp();

    public void setApp(String app);

    public String getInsertedFrom();

    public void setInsertedFrom(String insertedFrom);

    public List<? extends JobInterface> getJobList();

    public void setJobList(List<? extends JobInterface> jobList);

    public ZonedDateTime getNextExecutableCheck();

    public void setNextExecutableCheck(ZonedDateTime nextExecutableCheck);
    
    public ZonedDateTime getExecutionTs();
    
    public void setExecutionTs(ZonedDateTime executionTs);
}
