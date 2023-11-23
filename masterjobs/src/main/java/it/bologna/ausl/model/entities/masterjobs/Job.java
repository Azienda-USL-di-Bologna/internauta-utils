package it.bologna.ausl.model.entities.masterjobs;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import it.nextsw.common.data.annotations.GenerateProjections;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
    
    @Basic(optional = false)
    @NotNull
    @JoinColumn(name = "set", referencedColumnName = "id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private Set set;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private JobState state;
        
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
    
    @Basic(optional = true)
    @Type(type = "jsonb")
    @Column(name = "work_data", columnDefinition = "jsonb")
    private Map<String, Object> workData;
        
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "insert_ts")
    @Basic(optional = false)
    @NotNull
    private ZonedDateTime insertTs = ZonedDateTime.now();
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "last_execution_ts")
    @Basic(optional = true)
    private ZonedDateTime lastExecutionTs;
  
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
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
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

    public Map<String, Object> getWorkData() {
        return workData;
    }

    public void setWorkData(Map<String, Object> workData) {
        this.workData = workData;
    }

    public ZonedDateTime getLastExecutionTs() {
        return lastExecutionTs;
    }

    public void setLastExecutionTs(ZonedDateTime lastExecutionTs) {
        this.lastExecutionTs = lastExecutionTs;
    }

    public ZonedDateTime getInsertTs() {
        return insertTs;
    }

    public void setInsertTs(ZonedDateTime insertTs) {
        this.insertTs = insertTs;
    }
    
    
}
