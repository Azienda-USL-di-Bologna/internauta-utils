/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.oracledata;

import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import java.io.Serializable;

/**
 *
 * @author Top
 */
public class Anagrafica implements Serializable {

    private String codiceEnte;

    private String codiceMatricola;

    private String cognome;

    private String nome;

    private String codiceFiscale;

    private String email;

    public Anagrafica() {
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "it.bologna.ausl.internauta.model.entities.ribaltone.entita.plugin.gru.oracledata.Anagrafica[ codiceFiscale=" + codiceFiscale + " ]";
    }

    public DatiDaImportareAnagrafica toFonteIntermedia(String codiceAzienda, Integer idAzienda){
        DatiDaImportareAnagrafica fonteIntermediaAnagrafica = new DatiDaImportareAnagrafica();
        fonteIntermediaAnagrafica.setCodiceEnte(codiceEnte);
        fonteIntermediaAnagrafica.setCodiceFiscale(codiceFiscale);
        fonteIntermediaAnagrafica.setCodiceMatricola(codiceMatricola);
        fonteIntermediaAnagrafica.setCognome(cognome);
        fonteIntermediaAnagrafica.setNome(nome);
        fonteIntermediaAnagrafica.setEmail(email);
        fonteIntermediaAnagrafica.setIdAzienda(idAzienda);
        fonteIntermediaAnagrafica.setCodiceAzienda(codiceAzienda);
        return fonteIntermediaAnagrafica;
    }
}
