package it.bologna.ausl.model.entities.configurazione;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import it.nextsw.common.data.annotations.GenerateProjections;
import java.io.Serializable;
import java.time.ZonedDateTime;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author spritz
 */
@TypeDef(name = "string-array", typeClass = StringArrayType.class)
@Entity
@Table(name = "parametri_aziende", schema = "configurazione")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
@GenerateProjections({})
@DynamicUpdate
public class ParametroAziende implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "nome", columnDefinition = "text")
    private String nome;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "valore", columnDefinition = "text")
    private String valore;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "id_applicazioni", columnDefinition = "text[]")
    @Type(type = "string-array")
    private String[] idApplicazioni;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "id_aziende", columnDefinition = "integer[]")
    @Type(type = "string-array")
    private Integer[] idAziende;

    @Basic(optional = false)
    @NotNull
    @Column(name = "hide_from_api", columnDefinition = "boolean")
    private Boolean hideFromApi = true;
    
    @Version()
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private ZonedDateTime version;

    public ZonedDateTime getVersion() {
        return version;
    }

    public void setVersion(ZonedDateTime version) {
        this.version = version;
    }

    public ParametroAziende() {
    }

    public ParametroAziende(Integer id) {
        this.id = id;
    }

    public ParametroAziende(Integer id, String nome, String valore, String[] idApplicazioni, Integer[] idAziende) {
        this.id = id;
        this.nome = nome;
        this.valore = valore;
        this.idApplicazioni = idApplicazioni;
        this.idAziende = idAziende;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getValore() {
        return valore;
    }

    public void setValore(String valore) {
        this.valore = valore;
    }

    public String[] getIdApplicazioni() {
        return idApplicazioni;
    }

    public void setIdApplicazioni(String[] idApplicazioni) {
        this.idApplicazioni = idApplicazioni;
    }

    public Integer[] getIdAziende() {
        return idAziende;
    }

    public void setIdAziende(Integer[] idAziende) {
        this.idAziende = idAziende;
    }

    public Boolean getHideFromApi() {
        return hideFromApi;
    }

    public void setHideFromApi(Boolean hideFromApi) {
        this.hideFromApi = hideFromApi;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ParametroAziende)) {
            return false;
        }
        ParametroAziende other = (ParametroAziende) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getClass().getCanonicalName() + " [ id=" + id + " ]";
    }

}
