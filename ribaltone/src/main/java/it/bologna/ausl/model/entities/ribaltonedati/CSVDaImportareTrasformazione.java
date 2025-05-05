/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
@Table(name = "csv_da_importare_trasformazioni", catalog = "internauta", schema = "ribaltone_dati")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@GenerateProjections({})
@DynamicUpdate
public class CSVDaImportareTrasformazione implements Serializable, DatiRibaltoneInterface {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;

    @Column(name = "progressivo_riga")
    private Integer progressivoRiga;

    @Column(name = "id_casella_partenza")
    private Integer idCasellaPartenza;

    @Column(name = "id_casella_arrivo")
    private Integer idCasellaArrivo;

    @Column(name = "errore")
    private String errore;

    @Column(name = "data_trasformazione")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private ZonedDateTime dataTrasformazione;

    @Size(max = 2147483647)
    @Column(name = "motivo")
    private String motivo;

    @Column(name = "datain_partenza")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private ZonedDateTime datainPartenza;

    @Column(name = "dataora_oper")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private ZonedDateTime dataOraOper;

    @Column(name = "codice_azienda")
    private String codiceAzienda;

    @Column(name = "id_azienda")
    private Integer idAzienda;

    @Version()
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private ZonedDateTime version;

    public CSVDaImportareTrasformazione() {
    }

    public CSVDaImportareTrasformazione(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Integer idAzienda) {
        this.idAzienda = idAzienda;
    }

    public Integer getProgressivoRiga() {
        return progressivoRiga;
    }

    public void setProgressivoRiga(Integer progressivoRiga) {
        this.progressivoRiga = progressivoRiga;
    }

    public Integer getIdCasellaPartenza() {
        return idCasellaPartenza;
    }

    public void setIdCasellaPartenza(Integer idCasellaPartenza) {
        this.idCasellaPartenza = idCasellaPartenza;
    }

    public Integer getIdCasellaArrivo() {
        return idCasellaArrivo;
    }

    public void setIdCasellaArrivo(Integer idCasellaArrivo) {
        this.idCasellaArrivo = idCasellaArrivo;
    }

    public ZonedDateTime getDataTrasformazione() {
        return dataTrasformazione;
    }

    public void setDataTrasformazione(ZonedDateTime dataTrasformazione) {
        this.dataTrasformazione = dataTrasformazione;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public ZonedDateTime getDatainPartenza() {
        return datainPartenza;
    }

    public void setDatainPartenza(ZonedDateTime datainPartenza) {
        this.datainPartenza = datainPartenza;
    }

    public ZonedDateTime getDataOraOper() {
        return dataOraOper;
    }

    public void setDataOraOper(ZonedDateTime dataOraOper) {
        this.dataOraOper = dataOraOper;
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

    public String getErrore() {
        return errore;
    }

    public void setErrore(String errore) {
        this.errore = errore;
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
        if (!(object instanceof CSVDaImportareTrasformazione)) {
            return false;
        }
        CSVDaImportareTrasformazione other = (CSVDaImportareTrasformazione) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "it.bologna.ausl.internauta.model.entities.DatiDaImportareTrasformazione[progressivo_riga=" + progressivoRiga + ", id_casella_partenza=" + idCasellaPartenza + ", id_casella_arrivo=" + idCasellaArrivo + ", codice_azienda=" + codiceAzienda + ", id=" + id + " ]";
    }

    @Override
    public String getKey() {
        return progressivoRiga.toString();
    }

    @Override
    public TipologiaCsv getTipo() {
        return TipologiaCsv.TRASFORMAZIONI;
    }

    @Override
    public String getClasse() {
        return CSVDaImportareTrasformazione.class.getCanonicalName();
    }

}
