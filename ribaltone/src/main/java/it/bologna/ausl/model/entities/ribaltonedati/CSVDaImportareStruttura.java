package it.bologna.ausl.model.entities.ribaltonedati;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.nextsw.common.data.annotations.GenerateProjections;
import java.io.Serializable;
import java.time.ZonedDateTime;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.format.annotation.DateTimeFormat;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;

/**
 *
 * @author Top
 */
@Entity
@Table(name = "csv_da_importare_strutture", catalog = "internauta", schema = "ribaltone_dati")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@GenerateProjections({})
@DynamicUpdate
public class CSVDaImportareStruttura implements Serializable, DatiRibaltoneInterface {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_casella")
    private Integer idCasella;

    @Column(name = "id_padre")
    private Integer idPadre;

    @Size(max = 2147483647)
    @Column(name = "descrizione")
    private String descrizione;

    @Size(max = 2147483647)
    @Column(name = "tipo_legame")
    private String tipoLegame;

    @Column(name = "errore")
    private String errore;

    @Column(name = "id_azienda")
    private Integer idAzienda;

    @Column(name = "codice_azienda")
    private String codiceAzienda;

    @Column(name = "codice_ente")
    private String codiceEnte;

    @Column(name = "datain")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private ZonedDateTime datain;

    @Column(name = "datafi")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private ZonedDateTime datafi;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;

    @Version()
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private ZonedDateTime version;

    public CSVDaImportareStruttura() {
    }

    public Integer getIdCasella() {
        return idCasella;
    }

    public void setIdCasella(Integer idCasella) {
        this.idCasella = idCasella;
    }

    public Integer getIdPadre() {
        return idPadre;
    }

    public void setIdPadre(Integer idPadre) {
        this.idPadre = idPadre;
    }

    @Override
    public Integer getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Integer idAzienda) {
        this.idAzienda = idAzienda;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getTipoLegame() {
        return tipoLegame;
    }

    public void setTipoLegame(String tipoLegame) {
        this.tipoLegame = tipoLegame;
    }

    public String getErrore() {
        return errore;
    }

    public void setErrore(String errore) {
        this.errore = errore;
    }

    public String getCodiceAzienda() {
        return codiceAzienda;
    }

    public void setCodiceAzienda(String codiceAzienda) {
        this.codiceAzienda = codiceAzienda;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ZonedDateTime getVersion() {
        return version;
    }

    public void setVersion(ZonedDateTime version) {
        this.version = version;
    }

    public ZonedDateTime getDatain() {
        return datain;
    }

    public void setDatain(ZonedDateTime datain) {
        this.datain = datain;
    }

    public ZonedDateTime getDatafi() {
        return datafi;
    }

    public void setDatafi(ZonedDateTime datafi) {
        this.datafi = datafi;
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
        if (!(object instanceof CSVDaImportareStruttura)) {
            return false;
        }
        CSVDaImportareStruttura other = (CSVDaImportareStruttura) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "it.bologna.ausl.internauta.model.entities.DatiDaImportareStruttura[ id=" + id + " ]";
    }

    @Override
    public String getKey() {
        return idCasella.toString() + " " + descrizione;
    }

    @Override
    public TipologiaCsv getTipo() {
        return TipologiaCsv.STRUTTURE;
    }

    @Override
    public String getClasse() {
        return CSVDaImportareStruttura.class.getCanonicalName();
    }

    public String getCodiceEnte() {
        return codiceEnte;
    }

    public void setCodiceEnte(String codiceEnte) {
        this.codiceEnte = codiceEnte;
    }

    public DatiImportatiStruttura buildDatiImportatiStruttura() {
        DatiImportatiStruttura output = new DatiImportatiStruttura();

        output.setIdCasella(this.idCasella);
        output.setIdPadre(this.idPadre);
        output.setDescrizione(this.descrizione);
        output.setDatain(this.datain);
        output.setDatafi(this.datafi);
        output.setTipoLegame(this.tipoLegame);
        output.setCodiceEnte(this.codiceEnte);
        output.setCodiceAzienda(this.codiceAzienda);
        output.setIdAzienda(this.idAzienda);

        return output;
    }

    public DatiDaImportareStruttura buildDatiDaImportareStruttura() {
        DatiDaImportareStruttura output = new DatiDaImportareStruttura();

        output.setIdCasella(this.idCasella);
        output.setIdPadre(this.idPadre);
        output.setDescrizione(this.descrizione);
        output.setDatain(this.datain);
        output.setDatafi(this.datafi);
        output.setTipoLegame(this.tipoLegame);
        output.setCodiceEnte(this.codiceEnte);
        output.setCodiceAzienda(this.codiceAzienda);
        output.setIdAzienda(this.idAzienda);

        return output;
    }
}
