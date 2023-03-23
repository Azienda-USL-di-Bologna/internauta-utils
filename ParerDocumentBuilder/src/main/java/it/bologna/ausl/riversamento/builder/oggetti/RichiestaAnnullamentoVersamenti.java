/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.riversamento.builder.oggetti;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author utente
 * Oggetto che contiene tutta la richiesta di annullamento di documenti
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="RichiestaAnnullamentoVersamenti")
@XmlType(name = "RichiestaAnnullamentoVersamenti", propOrder = {
    "versioneXmlRichiesta",
    "versatore",
    "richiesta",
    "versamentiDaAnnullare"
})
public class RichiestaAnnullamentoVersamenti {
    @XmlElement(name = "VersioneXmlRichiesta", required = true)
    protected String versioneXmlRichiesta;
    @XmlElement(name = "Versatore", required = true)
    protected VersatoreType versatore;
    @XmlElement(name = "Richiesta", required = true)
    protected RichiestaAnnullamentoType richiesta;
    @XmlElement(name = "VersamentiDaAnnullare", required = true)
    protected VersamentiDaAnnullareListType versamentiDaAnnullare;

    public String getVersioneXmlRichiesta() {
        return versioneXmlRichiesta;
    }

    public void setVersioneXmlRichiesta(String versioneXmlRichiesta) {
        this.versioneXmlRichiesta = versioneXmlRichiesta;
    }

    public VersatoreType getVersatore() {
        return versatore;
    }

    public void setVersatore(VersatoreType versatore) {
        this.versatore = versatore;
    }

    public RichiestaAnnullamentoType getRichiesta() {
        return richiesta;
    }

    public void setRichiesta(RichiestaAnnullamentoType richiesta) {
        this.richiesta = richiesta;
    }

    public VersamentiDaAnnullareListType getVersamentiDaAnnullare() {
        return versamentiDaAnnullare;
    }

    public void setVersamentiDaAnnullare(VersamentiDaAnnullareListType versamentiDaAnnullare) {
        this.versamentiDaAnnullare = versamentiDaAnnullare;
    }
    
}
