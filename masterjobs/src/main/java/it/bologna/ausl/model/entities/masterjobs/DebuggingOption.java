package it.bologna.ausl.model.entities.masterjobs;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import it.nextsw.common.annotations.GenerateProjections;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
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
@Table(name = "debugging_options", catalog = "internauta", schema = "masterjobs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
@GenerateProjections({})
@DynamicUpdate
public class DebuggingOption implements Serializable {

    public static enum Key {
        filterJobs, limitSetExecutionToInsertedIP
    }
    
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "key")
    private String key;
    
    @Basic(optional = true)
    @Type(type = "jsonb")
    @Column(name = "value", columnDefinition = "jsonb")
    private Object value;
    
    @Basic(optional = true)
    @Column(name = "note")
    private String note;
    
    public DebuggingOption() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public boolean equals(Object object) {
        // this method won't work in the case the id fields are not set
        if (!(object instanceof DebuggingOption)) {
            return false;
        }
        DebuggingOption other = (DebuggingOption) object;
        return !((this.key == null && other.key != null) || (this.key != null && !this.key.equals(other.key)));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.key);
        return hash;
    }

   @Override
    public String toString() {
        return getClass().getCanonicalName() + "[ id=" + key + " ]";
    }
}
