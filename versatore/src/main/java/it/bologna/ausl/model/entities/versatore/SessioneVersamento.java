package it.bologna.ausl.model.entities.versatore;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.range.Range;
import it.bologna.ausl.model.entities.scripta.Archivio;
import java.io.Serializable;
import java.time.ZonedDateTime;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
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
@Table(name = "sessioni_versamenti", schema = "versatore")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
@DynamicUpdate
public class SessioneVersamento implements Serializable {
    
    public static enum TipologiaVersamento {
        FORZATURA,
        INVIO_GIORNALIERO,
        CHIUSURA_FASCICOLO;    
    }

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "id")
    private String id;
    
    @Basic(optional = false)
    @Column(name = "tipologia")
    private TipologiaVersamento tipologia;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "time_interval", columnDefinition = "tstzrange")
    private Range<ZonedDateTime> timeInterval;
    
    @JoinColumn(name = "id_archivio", referencedColumnName = "id")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JsonBackReference(value = "idArchivio")
    private Archivio idArchivio;

    public SessioneVersamento() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TipologiaVersamento getTipologia() {
        return tipologia;
    }

    public void setTipologia(TipologiaVersamento tipologia) {
        this.tipologia = tipologia;
    }

    public Range<ZonedDateTime> getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(Range<ZonedDateTime> timeInterval) {
        this.timeInterval = timeInterval;
    }

    public Archivio getIdArchivio() {
        return idArchivio;
    }

    public void setIdArchivio(Archivio idArchivio) {
        this.idArchivio = idArchivio;
    }
    
    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SessioneVersamento)) {
            return false;
        }
        SessioneVersamento other = (SessioneVersamento) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return getClass().getCanonicalName()+ "[ id=" + id + " ]";
    }

}
