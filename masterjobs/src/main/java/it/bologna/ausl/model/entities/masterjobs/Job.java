package it.bologna.ausl.model.entities.masterjobs;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import it.nextsw.common.data.annotations.GenerateProjections;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
@Table(name = "jobs", catalog = "internauta", schema = "masterjobs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
@GenerateProjections({})
@DynamicUpdate
public class Job implements Serializable {

    public static enum JobState {
        READY,
        RUNNING,
        ERROR,
        DONE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "name")
    private String name;
    
    @Basic(optional = true)
    @Type(type = "jsonb")
    @Column(name = "data", columnDefinition = "jsonb")
    private Map<String, Object> data;
//    private WorkerData data;
    
    @Basic(optional = false)
    @NotNull
    @JoinColumn(name = "set", referencedColumnName = "id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private Set set;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "state")
    private String state;
        
    @Basic(optional = false)
    @NotNull
    @Column(name = "deferred")
    private Boolean deferred = false;
    
    @Basic(optional = true)
    @Column(name = "error")
    private String error;
    
    @Basic(optional = true)
    @Column(name = "inserted_from")
    private String insertedFrom;
    
    @Basic(optional = true)
    @Column(name = "hash")
    @Type(type="pg-uuid")
    private UUID hash;
    
    @Basic(optional = false)
    @Column(name = "executable_check_every_millis")
    @NotNull
    private Integer executableCheckEveryMillis = 100;
    
    public Job() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Set getSet() {
        return set;
    }

    public void setSet(Set set) {
        this.set = set;
    }

    public JobState getState() {
        if (state != null) {
            return JobState.valueOf(state);
        } else {
            return null;
        }
    }

    public void setState(JobState state) {
        if (state != null) {
            this.state = state.toString();
        } else {
            this.state = null;
        }
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
    public String getInsertedFrom() {
        return insertedFrom;
    }

    public void setInsertedFrom(String insertedFrom) {
        this.insertedFrom = insertedFrom;
    }

    public UUID getHash() {
        return hash;
    }

    public void setHash(UUID hash) {
        this.hash = hash;
    }
    
    public Boolean getDeferred() {
        return deferred;
    }

    public void setDeferred(Boolean deferred) {
        this.deferred = deferred;
    }

    public Integer getExecutableCheckEveryMillis() {
        return executableCheckEveryMillis;
    }

    public void setExecutableCheckEveryMillis(Integer executableCheckEveryMillis) {
        this.executableCheckEveryMillis = executableCheckEveryMillis;
    }
}
