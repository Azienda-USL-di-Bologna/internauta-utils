package it.bologna.ausl.model.entities.masterjobs;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import it.bologna.ausl.internauta.utils.masterjobs.MasterjobsWorkingObject;
import it.bologna.ausl.model.entities.masterjobs.Set.SetPriority;
import it.nextsw.common.data.annotations.GenerateProjections;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 *
 * @author gdm
 */
@TypeDefs({
    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
@Entity
@Table(name = "jobs_notified", catalog = "internauta", schema = "masterjobs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
@GenerateProjections({})
@DynamicUpdate
public class JobNotified implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    
    @Basic(optional = false)
    @Column(name = "job_name")
    @NotNull
    private String jobName;
    
    @Basic(optional = true)
    @Type(type = "jsonb")
    @Column(name = "job_data", columnDefinition = "jsonb")
    private Map<String, Object> jobData;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "deferred")
    private Boolean deferred = false;
    
    @Basic(optional = true)
    @Column(name = "object_id")
    private String objectId;
    
    @Basic(optional = true)
    @Column(name = "object_type")
    private String objectType;
    
    @Basic(optional = true)
    @Column(name = "app")
    private String app;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "wait_object")
    private Boolean waitObject = true;
    
    @Basic(optional = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private SetPriority priority = SetPriority.NORMAL;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "skip_if_already_present")
    private Boolean skipIfAlreadyPresent = false;
    
    @Basic(optional = true)
    @Column(name = "inserted_from")
    private String insertedFrom;
    
    @Basic(optional = true)
    @Type(type = "jsonb")
    @Column(name = "working_objects", columnDefinition = "jsonb")
    private List<MasterjobsWorkingObject> workingObjects;
    
    public JobNotified() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Map<String, Object> getJobData() {
        return jobData;
    }

    public void setJobData(Map<String, Object> jobData) {
        this.jobData = jobData;
    }

    public Boolean getDeferred() {
        return deferred;
    }

    public void setDeferred(Boolean deferred) {
        this.deferred = deferred;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public Boolean getWaitObject() {
        return waitObject;
    }

    public void setWaitObject(Boolean waitObject) {
        this.waitObject = waitObject;
    }

    public SetPriority getPriority() {
        return priority;
    }

    public void setPriority(SetPriority priority) {
        this.priority = priority;
    }

    public Boolean getSkipIfAlreadyPresent() {
        return skipIfAlreadyPresent;
    }

    public void setSkipIfAlreadyPresent(Boolean skipIfAlreadyPresent) {
        this.skipIfAlreadyPresent = skipIfAlreadyPresent;
    }

    public String getInsertedFrom() {
        return insertedFrom;
    }

    public void setInsertedFrom(String insertedFrom) {
        this.insertedFrom = insertedFrom;
    }

    public List<MasterjobsWorkingObject> getWorkingObjects() {
        return workingObjects;
    }

    public void setWorkingObjects(List<MasterjobsWorkingObject> workingObjects) {
        this.workingObjects = workingObjects;
    }

    @Override
    public boolean equals(Object object) {
        // this method won't work in the case the id fields are not set
        if (!(object instanceof JobNotified)) {
            return false;
        }
        JobNotified other = (JobNotified) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.id);
        return hash;
    }

   @Override
    public String toString() {
        return getClass().getCanonicalName() + "[ id=" + id + " ]";
    }
}
