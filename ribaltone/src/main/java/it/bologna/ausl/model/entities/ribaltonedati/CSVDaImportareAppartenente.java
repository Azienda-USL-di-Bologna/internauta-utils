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
@Table(name = "csv_da_importare_appartenenti", catalog = "internauta", schema = "ribaltone_dati")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@GenerateProjections({})
@DynamicUpdate
public class CSVDaImportareAppartenente implements Serializable, DatiRibaltoneInterface {

    private static final long serialVersionUID = 1L;

    @Column(name = "codice_matricola")
    private String codiceMatricola;

    @Size(max = 2147483647)
    @Column(name = "cognome")
    private String cognome;

    @Size(max = 2147483647)
    @Column(name = "nome")
    private String nome;

    @Size(max = 2147483647)
    @Column(name = "codice_fiscale")
    private String codiceFiscale;

    @Column(name = "id_casella")
    private Integer idCasella;

    @Size(max = 2147483647)
    @Column(name = "tipo_appartenenza")
    private String tipoAppartenenza;

    @Size(max = 2147483647)
    @Column(name = "username")
    private String username;

    @Column(name = "codice_azienda")
    private String codiceAzienda;

    @Column(name = "id_azienda")
    private Integer idAzienda;

    @Column(name = "responsabile")
    private Boolean responsabile;

    @Column(name = "errore")
    private String errore;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;

    @Version()
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private ZonedDateTime version;

    public CSVDaImportareAppartenente() {

    }

    public Integer getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Integer idAzienda) {
        this.idAzienda = idAzienda;
    }

    public String getErrore() {
        return errore;
    }

    public void setErrore(String errore) {
        this.errore = errore;
    }

    public String getCodiceMatricola() {
        return codiceMatricola;
    }

    public void setCodiceMatricola(String codiceMatricola) {
        this.codiceMatricola = codiceMatricola;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    public Integer getIdCasella() {
        return idCasella;
    }

    public void setIdCasella(Integer idCasella) {
        this.idCasella = idCasella;
    }

    public String getTipoAppartenenza() {
        return tipoAppartenenza;
    }

    public void setTipoAppartenenza(String tipoAppartenenza) {
        this.tipoAppartenenza = tipoAppartenenza;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getResponsabile() {
        return responsabile;
    }

    public void setResponsabile(Boolean responsabile) {
        this.responsabile = responsabile;
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

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CSVDaImportareAppartenente)) {
            return false;
        }
        CSVDaImportareAppartenente other = (CSVDaImportareAppartenente) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "it.bologna.ausl.internauta.model.entities.DatiDaImportareAppartenente[ id=" + id + " ]";
    }

    @Override
    public String getKey() {
        return codiceFiscale + "_" + idCasella + "_" + idAzienda;
    }

    @Override
    public TipologiaCsv getTipo() {
        return TipologiaCsv.APPARTENENTI;
    }

    @Override
    public String getClasse() {
        return CSVDaImportareAppartenente.class.getCanonicalName();
    }

}
