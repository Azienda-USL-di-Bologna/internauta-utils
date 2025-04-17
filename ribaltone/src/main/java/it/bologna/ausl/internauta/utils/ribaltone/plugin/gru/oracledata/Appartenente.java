package it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.oracledata;

import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import jakarta.persistence.Column;
import java.io.Serializable;
import java.time.ZoneId;
import java.util.Date;

/**
 *
 * @author Top
 */
public class Appartenente implements Serializable {

    @Column(name = "CODICE_ENTE")
    private String codiceEnte;

    @Column(name = "CODICE_MATRICOLA")
    private String codiceMatricola;

    @Column(name = "COGNOME")
    private String cognome;

    @Column(name = "NOME")
    private String nome;
    
    @Column(name = "CODICE_FISCALE")
    private String codiceFiscale;

    @Column(name = "ID_CASELLA")
    private Integer idCasella;

//    @Column(name = "DATAIN")
//    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    private Date datain;
//
//    @Column(name = "DATAFI")
//    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    private Date datafi;

    @Column(name = "TIPO_APPARTENENZA")
    private String tipoAppartenenza;

    @Column(name = "USERNAME")
    private String username;

//    @Column(name = "DATA_ASSUNZIONE")
//    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    private Date dataAssunzione;
//
//    @Column(name = "DATA_DIMISSIONE")
//    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    private Date dataDimissione;

    public Appartenente() {
    }

    public String getCodiceEnte() {
        return codiceEnte;
    }

    public void setCodiceEnte(String codiceEnte) {
        this.codiceEnte = codiceEnte;
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
//
//    public Date getDatain() {
//        return datain;
//    }
//
//    public void setDatain(Date datain) {
//        this.datain = datain;
//    }
//
//    public Date getDatafi() {
//        return datafi;
//    }
//
//    public void setDatafi(Date datafi) {
//        this.datafi = datafi;
//    }

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

//    public Date getDataAssunzione() {
//        return dataAssunzione;
//    }
//
//    public void setDataAssunzione(Date dataAssunzione) {
//        this.dataAssunzione = dataAssunzione;
//    }
//
//    public Date getDataDimissione() {
//        return dataDimissione;
//    }
//
//    public void setDataDimissione(Date dataDimissione) {
//        this.dataDimissione = dataDimissione;
//    }

    @Override
    public String toString() {
        return "it.bologna.ausl.internauta.model.entities.ribaltone.entita.plugin.gru.oracledata.Appartenente[ id=" + codiceFiscale + " ]";
    }
    
    /**
     * ritorna un appartenente in formato fonte intermedia 
     * setta sempre la responsabilita a false
     * @param codiceAzienda
     * @param idAzienda
     * @return DatiDaImportareAppartenente 
     */
    public DatiDaImportareAppartenente toFonteIntermedia(String codiceAzienda, Integer idAzienda){
        DatiDaImportareAppartenente fonteIntermediaAppartenente = new DatiDaImportareAppartenente();
        fonteIntermediaAppartenente.setCodiceEnte(codiceEnte);
        fonteIntermediaAppartenente.setCodiceFiscale(codiceFiscale);
        fonteIntermediaAppartenente.setCodiceMatricola(codiceMatricola);
        fonteIntermediaAppartenente.setNome(nome);
        fonteIntermediaAppartenente.setCognome(cognome);
        fonteIntermediaAppartenente.setIdAzienda(idAzienda);
//        fonteIntermediaAppartenente.setDataAssunzione(dataAssunzione.toInstant().atZone(ZoneId.systemDefault()));
//        fonteIntermediaAppartenente.setDataDimissione(dataDimissione.toInstant().atZone(ZoneId.systemDefault()));
//        fonteIntermediaAppartenente.setDatain(datain.toInstant().atZone(ZoneId.systemDefault()));
//        fonteIntermediaAppartenente.setDatafi(datafi.toInstant().atZone(ZoneId.systemDefault()));
        fonteIntermediaAppartenente.setIdCasella(idCasella);
        fonteIntermediaAppartenente.setTipoAppartenenza(tipoAppartenenza);
        fonteIntermediaAppartenente.setUsername(username);
        fonteIntermediaAppartenente.setCodiceAzienda(codiceAzienda);
        fonteIntermediaAppartenente.setResponsabile(Boolean.FALSE);
    return fonteIntermediaAppartenente;
    }

}
