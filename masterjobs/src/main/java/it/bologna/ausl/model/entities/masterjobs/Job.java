package it.bologna.ausl.model.entities.masterjobs;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import it.nextsw.common.data.annotations.GenerateProjections;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.UUID;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author gdm
 */
//@TypeDefs({
//    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
//})
@Entity
@Table(name = "jobs", catalog = "internauta", schema = "masterjobs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
@GenerateProjections({})
@DynamicUpdate
public class Job implements Serializable, JobInterface {

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
    @Type(JsonBinaryType.class)
    @Column(name = "data", columnDefinition = "jsonb")
    private HashMap<String, Object> data;
    
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
//    @Type(type="pg-uuid")
    private UUID hash;
    
    @Basic(optional = false)
    @Column(name = "executable_check_every_millis")
    @NotNull
    private Integer executableCheckEveryMillis = 100;
    
    @Basic(optional = true)
    @Type(JsonBinaryType.class)
    @Column(name = "work_data", columnDefinition = "jsonb")
    private HashMap<String, Object> workData;
        
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
    
//    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "job", fetch = FetchType.LAZY)
//    @JsonBackReference(value = "workingObjects")
//    private List<WorkingObject> workingObjects;
    
    public Job() {
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public HashMap<String, Object> getData() {
        return data;
    }

    @Override
    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }

    @Override
    public SetInterface getSet() {
        return set;
    }

    @Override
    public void setSet(SetInterface set) {
        this.set = (Set) set;
    }

    @Override
    public JobState getState() {
        return state;
    }

    @Override
    public void setState(JobState state) {
        this.state = state;
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public void setError(String error) {
        this.error = error;
    }
    @Override
    public String getInsertedFrom() {
        return insertedFrom;
    }

    @Override
    public void setInsertedFrom(String insertedFrom) {
        this.insertedFrom = insertedFrom;
    }

    @Override
    public UUID getHash() {
        return hash;
    }

    @Override
    public void setHash(UUID hash) {
        this.hash = hash;
    }
    
    @Override
    public Boolean getDeferred() {
        return deferred;
    }

    @Override
    public void setDeferred(Boolean deferred) {
        this.deferred = deferred;
    }

    @Override
    public Integer getExecutableCheckEveryMillis() {
        return executableCheckEveryMillis;
    }

    @Override
    public void setExecutableCheckEveryMillis(Integer executableCheckEveryMillis) {
        this.executableCheckEveryMillis = executableCheckEveryMillis;
    }

    @Override
    public HashMap<String, Object> getWorkData() {
        return workData;
    }

    @Override
    public void setWorkData(HashMap<String, Object> workData) {
        this.workData = workData;
    }

    @Override
    public ZonedDateTime getLastExecutionTs() {
        return lastExecutionTs;
    }

    @Override
    public void setLastExecutionTs(ZonedDateTime lastExecutionTs) {
        this.lastExecutionTs = lastExecutionTs;
    }

    @Override
    public ZonedDateTime getInsertTs() {
        return insertTs;
    }

    @Override
    public void setInsertTs(ZonedDateTime insertTs) {
        this.insertTs = insertTs;
    }
    
}
