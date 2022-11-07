package it.bologna.ausl.model.entities.versatore;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sun.istack.NotNull;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.range.Range;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Doc;
import java.io.Serializable;
import java.time.ZonedDateTime;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@TypeDefs({
    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
@Entity
@Table(name = "versamenti", schema = "versatore")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
@DynamicUpdate
public class Versamento implements Serializable {
    
    public static enum StatoVersamento {
        VERSATO,
        DA_VERSARE,
        VERSAMENTO_PARZIALE,
        ERRORE;
    }

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "id")
    private String id;
    
    @JoinColumn(name = "id_doc", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JsonBackReference(value = "idDoc")
    private Doc idDoc;
    
    @Column(name = "stato")
    private StatoVersamento stato;
    
    @Column(name = "metadati_versati")
    private String metadatiVersati;
    
    @Column(name = "rapporto")
    private String rapporto;
    
    @Column(name = "codice_errore")
    private String codiceErrore;
    
    @Column(name = "descrizione_errore")
    private String descrizioneErrore;    
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "data_inserimento")
    @Basic(optional = false)
    @NotNull
    private ZonedDateTime dataInserimento = ZonedDateTime.now();
    
    @JoinColumn(name = "id_persona_forzatura", referencedColumnName = "id")
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JsonBackReference(value = "idPersonaForzatura")
    private Persona idPersonaForzatura;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "data_forzatura")
    private ZonedDateTime dataForzatura;
        
    @Version()
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private ZonedDateTime version;

    public Versamento() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Doc getIdDoc() {
        return idDoc;
    }

    public void setIdDoc(Doc idDoc) {
        this.idDoc = idDoc;
    }

    public StatoVersamento getStato() {
        return stato;
    }

    public void setStato(StatoVersamento stato) {
        this.stato = stato;
    }

    public String getMetadatiVersati() {
        return metadatiVersati;
    }

    public void setMetadatiVersati(String metadatiVersati) {
        this.metadatiVersati = metadatiVersati;
    }

    public String getRapporto() {
        return rapporto;
    }

    public void setRapporto(String rapporto) {
        this.rapporto = rapporto;
    }

    public String getCodiceErrore() {
        return codiceErrore;
    }

    public void setCodiceErrore(String codiceErrore) {
        this.codiceErrore = codiceErrore;
    }

    public String getDescrizioneErrore() {
        return descrizioneErrore;
    }

    public void setDescrizioneErrore(String descrizioneErrore) {
        this.descrizioneErrore = descrizioneErrore;
    }

    public ZonedDateTime getDataInserimento() {
        return dataInserimento;
    }

    public void setDataInserimento(ZonedDateTime dataInserimento) {
        this.dataInserimento = dataInserimento;
    }

    public Persona getIdPersonaForzatura() {
        return idPersonaForzatura;
    }

    public void setIdPersonaForzatura(Persona idPersonaForzatura) {
        this.idPersonaForzatura = idPersonaForzatura;
    }

    public ZonedDateTime getDataForzatura() {
        return dataForzatura;
    }

    public void setDataForzatura(ZonedDateTime dataForzatura) {
        this.dataForzatura = dataForzatura;
    }

    public ZonedDateTime getVersion() {
        return version;
    }

    public void setVersion(ZonedDateTime version) {
        this.version = version;
    }
    
    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Versamento)) {
            return false;
        }
        Versamento other = (Versamento) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return getClass().getCanonicalName()+ "[ id=" + id + " ]";
    }

}