package it.bologna.ausl.model.entities.bdm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess;
import it.bologna.ausl.internauta.utils.bdm.workflows.processes.SampleProcess;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

/**
 *
 * @author gdm
 */
@Entity
@Table(name = "processes", schema = "bdm")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
//@GenerateProjections({"ribaltoneDaLanciareList"})
@DynamicUpdate
public class Process {
    
    @Id
    @Column(name = "id")
    @NotNull
    @Basic(optional = false)
    private String id = UUID.randomUUID().toString();
    
    @Column(name = "json_process", columnDefinition = "jsonb")
    @Basic(optional = true)
//    @Type(JsonBinaryType.class)
//    @Embedded
//    @JdbcTypeCode(SqlTypes.JSON)
    @Type(JsonType.class)
//    private TestJson jsonProcess;
    private BdmProcess jsonProcess;
    
    @Column(name = "status")
    @Basic(optional = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    private BdmProcess.BdmStatus status;

    public Process() {
    }

    public Process(BdmProcess jsonProcess, BdmProcess.BdmStatus status) {
        this.jsonProcess = jsonProcess;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BdmProcess getJsonProcess() {
        return jsonProcess;
    }

    public void setJsonProcess(BdmProcess jsonProcess) {
        this.jsonProcess = jsonProcess;
    }

    public BdmProcess.BdmStatus getStatus() {
        return status;
    }

    public void setStatus(BdmProcess.BdmStatus status) {
        this.status = status;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.id);
        return hash;
    }
    
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Process)) {
            return false;
        }
        Process other = (Process) object;
        return !((this.getId() == null && other.getId() != null) || (this.getId() != null && !this.getId().equals(other.getId())));
    }

    @Override
    public String toString() {
        return getClass().getCanonicalName()+ "[ id=" + getId() + " ]";
    }
}
