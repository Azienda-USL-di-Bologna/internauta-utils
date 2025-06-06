/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.model.entities.ribaltonedati;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "csv_da_importare_anagrafiche", catalog = "internauta", schema = "ribaltone_dati")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@GenerateProjections({})
@DynamicUpdate
public class CSVDaImportareAnagrafica implements Serializable, DatiRibaltoneInterface {

    private static final long serialVersionUID = 1L;

    @Column(name = "errore")
    private String errore;

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

    @Size(max = 2147483647)
    @Column(name = "email")
    private String email;

    @Size(max = 2147483647)
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "codice_azienda")
    private String codiceAzienda;

    @Column(name = "codice_ente")
    private String codiceEnte;

    @Column(name = "id_azienda")
    private Integer idAzienda;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;

    @Version()
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private ZonedDateTime version;

    public CSVDaImportareAnagrafica() {
    }

    public CSVDaImportareAnagrafica(Integer id) {
        this.id = id;
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

    public void setIdAzienda(Integer idAzienda) {
        this.idAzienda = idAzienda;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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
        if (!(object instanceof CSVDaImportareAnagrafica)) {
            return false;
        }
        CSVDaImportareAnagrafica other = (CSVDaImportareAnagrafica) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "it.bologna.ausl.model.entities.DatiDaImportareAnagrafica[ id=" + id + " ]";
    }

    @JsonIgnore
    @Override
    public String getKey() {
        return this.codiceFiscale;
    }

    @Override
    public TipologiaCsv getTipo() {
        return TipologiaCsv.ANAGRAFICHE;
    }

    @Override
    public String getClasse() {
        return CSVDaImportareAnagrafica.class.getCanonicalName();
    }

    @Override
    public Integer getIdAzienda() {
        return this.idAzienda;
    }

    public String getCodiceEnte() {
        return codiceEnte;
    }

    public void setCodiceEnte(String codiceEnte) {
        this.codiceEnte = codiceEnte;
    }

    public DatiImportatiAnagrafica buildDatiImportatiAnagrafica() {
        DatiImportatiAnagrafica anagraficaImportata = new DatiImportatiAnagrafica();
        anagraficaImportata.setCodiceEnte(this.codiceEnte);
        anagraficaImportata.setCodiceMatricola(this.codiceMatricola);
        anagraficaImportata.setCognome(this.cognome);
        anagraficaImportata.setNome(this.nome);
        anagraficaImportata.setCodiceFiscale(this.codiceFiscale);
        anagraficaImportata.setEmail(this.email);
        anagraficaImportata.setIdAzienda(this.idAzienda);
        anagraficaImportata.setCodiceAzienda(this.codiceAzienda);
        return anagraficaImportata;
    }
}
