package it.bologna.ausl.model.entities.masterjobs.views;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.array.IntArrayType;
import com.vladmihalcea.hibernate.type.array.ListArrayType;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import it.bologna.ausl.model.entities.masterjobs.Job;
import it.bologna.ausl.model.entities.masterjobs.Set.SetPriority;
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
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author gdm
 */
@TypeDefs({
    @TypeDef(name = "list-array", typeClass = ListArrayType.class)
})
@Entity
@Table(name = "set_with_jobs_array", catalog = "internauta", schema = "masterjobs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
@GenerateProjections({})
@DynamicUpdate
public class SetWithJobIdsArray implements Serializable {

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
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "next_executable_check")
    @Basic(optional = true)
    private ZonedDateTime nextExecutableCheck;
    
    @Column(name = "jobs_ids", columnDefinition = "int8[]")
    @Type(type = "list-array")
    private List<Long> jobsIds;
    
    public SetWithJobIdsArray() {
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

    public ZonedDateTime getNextExecutableCheck() {
        return nextExecutableCheck;
    }

    public void setNextExecutableCheck(ZonedDateTime nextExecutableCheck) {
        this.nextExecutableCheck = nextExecutableCheck;
    }

    public List<Long> getJobsIds() {
        return jobsIds;
    }

    public void setJobsIds(List<Long> jobsIds) {
        this.jobsIds = jobsIds;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[ id=" + id + " ]";
    }
}
