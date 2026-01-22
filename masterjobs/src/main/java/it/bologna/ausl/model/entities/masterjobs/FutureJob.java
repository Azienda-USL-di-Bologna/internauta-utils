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
 * @author gusgus
 */
//@TypeDefs({
//    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
//})
@Entity
@Table(name = "future_jobs", catalog = "internauta", schema = "masterjobs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
@GenerateProjections({})
@DynamicUpdate
public class FutureJob implements Serializable, JobInterface {
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
    @JoinColumn(name = "future_set", referencedColumnName = "id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private FutureSet set;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "deferred")
    private Boolean deferred = false;
    
    @Basic(optional = true)
    @Column(name = "inserted_from")
    private String insertedFrom;
    
    @Basic(optional = true)
    @Column(name = "hash")
//    @Type("pg-uuid")
//    @Type(type="pg-uuid")
    private UUID hash;
    
    @Basic(optional = false)
    @Column(name = "executable_check_every_millis")
    @NotNull
    private Integer executableCheckEveryMillis = 100;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "insert_ts")
    @Basic(optional = false)
    @NotNull
    private ZonedDateTime insertTs = ZonedDateTime.now();
    
    public FutureJob() {
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

    public HashMap<String, Object> getData() {
        return data;
    }

    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }

    public SetInterface getSet() {
        return set;
    }

    public void setSet(SetInterface set) {
        this.set = (FutureSet) set;
    }

    public JobState getState() {
        return null;
    }

    public void setState(JobState state) {
    }

    public String getError() {
        return null;
    }

    public void setError(String error) {
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

    public HashMap<String, Object> getWorkData() {
        return null;
    }

    public void setWorkData(HashMap<String, Object> workData) {
    }

    public ZonedDateTime getLastExecutionTs() {
        return null;
    }

    public void setLastExecutionTs(ZonedDateTime lastExecutionTs) {
    }

    public ZonedDateTime getInsertTs() {
        return insertTs;
    }

    public void setInsertTs(ZonedDateTime insertTs) {
        this.insertTs = insertTs;
    }

}
