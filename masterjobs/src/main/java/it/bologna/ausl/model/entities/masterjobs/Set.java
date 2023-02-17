package it.bologna.ausl.model.entities.masterjobs;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import it.nextsw.common.annotations.GenerateProjections;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
 * @author gdm
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
public class Set implements Serializable {

    public static enum SetPriority {
        NORMAL,
        HIGH,
        HIGHEST
    }

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
    private String priority = SetPriority.NORMAL.toString();

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
    
    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "set", fetch = FetchType.LAZY)
    @JsonBackReference(value = "jobList")
    private List<Job> jobList;
    
    public Set() {
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
        if (priority != null) {
            return SetPriority.valueOf(priority);
        } else {
            return null;
        }
    }

    public void setPriority(SetPriority priority) {
        if (priority != null) {
            this.priority = priority.toString();
        } else {
            this.priority = null;
        }
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

    public List<Job> getJobList() {
        return jobList;
    }

    public void setJobList(List<Job> jobList) {
        this.jobList = jobList;
    }

    public ZonedDateTime getNextExecutableCheck() {
        return nextExecutableCheck;
    }

    public void setNextExecutableCheck(ZonedDateTime nextExecutableCheck) {
        this.nextExecutableCheck = nextExecutableCheck;
    }
}
