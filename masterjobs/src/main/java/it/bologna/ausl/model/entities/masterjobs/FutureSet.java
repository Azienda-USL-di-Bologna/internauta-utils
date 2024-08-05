package it.bologna.ausl.model.entities.masterjobs;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import it.nextsw.common.data.annotations.GenerateProjections;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author gusgus
 */
@TypeDefs({
    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
@Entity
@Table(name = "sets", catalog = "internauta", schema = "masterjobs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
@GenerateProjections({})
@DynamicUpdate
public class FutureSet implements Serializable, SetInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    
    @Basic(optional = true)
    @Column(name = "object_id")
    private String objectId;
    
    @Basic(optional = true)
    @Column(name = "object_type")
    private String objectType;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "wait_object")
    private Boolean waitObject = true;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    private SetPriority priority = SetPriority.NORMAL;

    @Basic(optional = true)
    @Column(name = "app")
    private String app;
    
    @Basic(optional = true)
    @Column(name = "inserted_from")
    private String insertedFrom;
    
    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "set", fetch = FetchType.LAZY)
    @JsonBackReference(value = "jobList")
    private List<JobInterface> jobList;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "execution_ts")
    private ZonedDateTime executionTs;
    
    public FutureSet() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getInsertedFrom() {
        return insertedFrom;
    }

    public void setInsertedFrom(String insertedFrom) {
        this.insertedFrom = insertedFrom;
    }

    public List<JobInterface> getJobList() {
        return jobList;
    }

    public void setJobList(List<JobInterface> jobList) {
        this.jobList = jobList;
    }

    public ZonedDateTime getNextExecutableCheck() {
        return null;
    }

    public void setNextExecutableCheck(ZonedDateTime nextExecutableCheck) {
    }
    
    public ZonedDateTime getExecutionTs() {
        return executionTs;
    }

    public void setExecutionTs(ZonedDateTime executionTs) {
        this.executionTs = executionTs;
    }
}
