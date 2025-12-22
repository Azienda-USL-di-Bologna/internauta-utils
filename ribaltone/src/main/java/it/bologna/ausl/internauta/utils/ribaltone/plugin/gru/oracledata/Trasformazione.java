/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.oracledata;

import com.fasterxml.jackson.annotation.JsonFormat;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import java.io.Serializable;
import java.time.ZonedDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author Top
 */
public class Trasformazione implements Serializable {

    private Integer progressivoRiga;

    private Integer idCasellaPartenza;

    private Integer idCasellaArrivo;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private ZonedDateTime dataTrasformazione;

    private String motivo;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private ZonedDateTime datainPartenza;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    private ZonedDateTime dataoraOper;

    private String codiceEnte;
        
    public Trasformazione() {
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

    public ZonedDateTime getDataoraOper() {
        return dataoraOper;
    }

    public void setDataoraOper(ZonedDateTime dataoraOper) {
        this.dataoraOper = dataoraOper;
    }

    public String getCodiceEnte() {
        return codiceEnte;
    }

    public void setCodiceEnte(String codiceEnte) {
        this.codiceEnte = codiceEnte;
    }

    @Override
    public String toString() {
        return "it.bologna.ausl.internauta.model.entities.ribaltone.entita.plugin.gru.oracledata.Trasformazioni[progressivo_riga=" + progressivoRiga + ", id_casella_partenza=" + idCasellaPartenza + ", id_casella_arrivo=" + idCasellaArrivo + ", codice_ente=" + codiceEnte + "]";
    }
    
    public DatiDaImportareTrasformazione toFonteIntermedia(String codiceAzienda, Integer idAzienda){
        DatiDaImportareTrasformazione fonteIntermediaTrasformazione = new DatiDaImportareTrasformazione();
        fonteIntermediaTrasformazione.setCodiceEnte(codiceEnte);
        fonteIntermediaTrasformazione.setDataTrasformazione(dataTrasformazione);
        fonteIntermediaTrasformazione.setDatainPartenza(datainPartenza);
        fonteIntermediaTrasformazione.setDataoraOper(dataoraOper);
        fonteIntermediaTrasformazione.setIdCasellaArrivo(idCasellaArrivo);
        fonteIntermediaTrasformazione.setIdCasellaPartenza(idCasellaPartenza);
        fonteIntermediaTrasformazione.setMotivo(motivo);
        fonteIntermediaTrasformazione.setIdAzienda(idAzienda);
        fonteIntermediaTrasformazione.setProgressivoRiga(progressivoRiga);
        fonteIntermediaTrasformazione.setCodiceAzienda(codiceAzienda);
    return fonteIntermediaTrasformazione;
    }

}
