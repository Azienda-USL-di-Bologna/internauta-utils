package it.bologna.ausl.model.entities.sendintegration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import it.bologna.ausl.internauta.utils.sendintegration.SendIntegrationConstants;
import java.io.Serializable;
import java.util.Map;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

/**
 *
 * @author gdm
 */
@Entity
@Table(name = "configuration", schema = "send_integration", catalog = "internauta")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
//@GenerateProjections({})
@DynamicUpdate
public class SendIntegrationConfiguration implements Serializable {
    public static enum Ids {
        lepidaSFTPConfiguration, lepidaAziendaConfiguration
    }
    
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "id")
    @Enumerated(EnumType.STRING)
    private Ids id;   

    @NotNull
    @Basic(optional = false)
    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Type(JsonBinaryType.class)
    @Column(name = "value", columnDefinition = "jsonb")
    private Map<String, Object> value;

    public SendIntegrationConfiguration() {
    }

    public Ids getId() {
        return id;
    }

    public void setId(Ids id) {
        this.id = id;
    }

    public Map<String, Object> getValue() {
        return value;
    }

    public void setValue(Map<String, Object> value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SendIntegrationConfiguration)) {
            return false;
        }
        SendIntegrationConfiguration other = (SendIntegrationConfiguration) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.id);
        hash = 17 * hash + Objects.hashCode(this.description);
        return hash;
    }

    @Override
    public String toString() {
        return getClass().getCanonicalName()+ "[ id=" + id + " ]";
    }

}
