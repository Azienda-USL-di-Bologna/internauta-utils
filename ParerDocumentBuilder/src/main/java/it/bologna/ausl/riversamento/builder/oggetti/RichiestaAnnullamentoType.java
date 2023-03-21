/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.riversamento.builder.oggetti;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author utente
 * Oggetto che racchiude i documenti da annullare. 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RichiestaAnnullamentoType", propOrder = {
    "codice",
    "descrizione",
    "motivazione",
    "immediata",
    "forzaAnnullamento",
    "richiestaDaPreIngest"
})
public class RichiestaAnnullamentoType {
    @XmlElement(name = "Codice", required = true)
    protected String codice;
    @XmlElement(name = "Descrizione", required = true)
    protected String descrizione;
    @XmlElement(name = "Motivazione", required = true)
    protected String motivazione;
    @XmlElement(name = "Immediata", required = true)
    protected String immediata;
    @XmlElement(name = "ForzaAnnullamento", required = true)
    protected String forzaAnnullamento;
    @XmlElement(name = "RichiestaDaPreIngest", required = true)
    protected String richiestaDaPreIngest;

    public String getCodice() {
        return codice;
    }

    public void setCodice(String codice) {
        this.codice = codice;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getMotivazione() {
        return motivazione;
    }

    public void setMotivazione(String motivazione) {
        this.motivazione = motivazione;
    }

    public String getImmediata() {
        return immediata;
    }

    public void setImmediata(String immediata) {
        this.immediata = immediata;
    }

    public String getForzaAnnullamento() {
        return forzaAnnullamento;
    }

    public void setForzaAnnullamento(String forzaAnnullamento) {
        this.forzaAnnullamento = forzaAnnullamento;
    }

    public String getRichiestaDaPreIngest() {
        return richiestaDaPreIngest;
    }

    public void setRichiestaDaPreIngest(String richiestaDaPreIngest) {
        this.richiestaDaPreIngest = richiestaDaPreIngest;
    }
}
