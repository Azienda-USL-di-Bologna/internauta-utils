package it.bologna.ausl.internauta.utils.versatore;

import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class VersamentoAllegatoInformation {
    
    private Allegato allegato;
    
    private Allegato.DettaglioAllegato dettaglio;
    
    private String metadatiVersati;
    
    private String rapporto;

    public VersamentoAllegatoInformation() {
    }

    public Allegato getAllegato() {
        return allegato;
    }

    public void setAllegato(Allegato allegato) {
        this.allegato = allegato;
    }
    
    public Allegato.DettaglioAllegato getDettaglio() {
        return dettaglio;
    }

    public void setDettaglio(Allegato.DettaglioAllegato dettaglio) {
        this.dettaglio = dettaglio;
    }
    
    public String getMetadatiVersati() {
        return metadatiVersati;
    }

    public void setMetadatiVersati(String metadatiVersati) {
        this.metadatiVersati = metadatiVersati;
    }

    public String getRapporto() {
        return rapporto;
    }

    public void setRapporto(String rapporto) {
        this.rapporto = rapporto;
    }
}
