package it.bologna.ausl.model.entities.firma;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.io.Serializable;
import java.util.Map;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 *
 * @author solidus83
 */
@TypeDefs({
    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
@Entity
@Table(name = "configurations", schema = "firma")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
//@GenerateProjections({"ribaltoneDaLanciareList"})
@DynamicUpdate
public class Configuration implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "host_id")
    private String hostId;
    
    @Basic(optional = false)
    @Column(name = "provider")
    private String provider;
    
    @Basic(optional = false)
    @Column(name = "internal_credentials_manager")
    private Boolean internalCredentialsManager = true;
    
    @Type(type = "jsonb")
    @Column(name = "params", columnDefinition = "jsonb")
    private Map<String, Object> params;
    
    @Basic(optional = false)
    @Column(name = "descrizione")
    private String descrizione;

    public Configuration() {
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Boolean getInternalCredentialsManager() {
        return internalCredentialsManager;
    }

    public void setInternalCredentialsManager(Boolean internalCredentialsManager) {
        this.internalCredentialsManager = internalCredentialsManager;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }
    
    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Configuration)) {
            return false;
        }
        Configuration other = (Configuration) object;
        return !((this.hostId == null && other.hostId != null) || (this.hostId != null && !this.hostId.equals(other.hostId)));
    }

    @Override
    public String toString() {
        return getClass().getCanonicalName()+ "[ id=" + hostId + " ]";
    }

}
