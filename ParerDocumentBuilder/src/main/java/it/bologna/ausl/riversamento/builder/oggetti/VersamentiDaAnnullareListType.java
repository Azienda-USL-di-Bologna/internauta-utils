/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.riversamento.builder.oggetti;

import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 *
 * @author utente
 * Oggetto con i riferimenti delle ud da annullare
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VersamentiDaAnnullareListType", propOrder = {
    "versamentoDaAnnullare"
})
public class VersamentiDaAnnullareListType {
    @XmlElement(name = "VersamentoDaAnnullare", required = true)
    protected List<VersamentoDaAnnullareType> versamentoDaAnnullare;

    public List<VersamentoDaAnnullareType> getVersamentoDaAnnullare() {
        return versamentoDaAnnullare;
    }

    public void setVersamentoDaAnnullare(List<VersamentoDaAnnullareType> versamentoDaAnnullare) {
        this.versamentoDaAnnullare = versamentoDaAnnullare;
    }
}
