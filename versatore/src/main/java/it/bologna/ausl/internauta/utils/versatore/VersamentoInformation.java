package it.bologna.ausl.internauta.utils.versatore;

import it.bologna.ausl.model.entities.versatore.SessioneVersamento;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class VersamentoInformation {
    
    private Integer idDoc;
    
    private Integer idArchivio;
    
    private SessioneVersamento.TipologiaVersamento tipologiaVersamento;
    
    private String risultato;

    public VersamentoInformation() {
    }

    public Integer getIdDoc() {
        return idDoc;
    }

    public void setIdDoc(Integer idDoc) {
        this.idDoc = idDoc;
    }

    public Integer getIdArchivio() {
        return idArchivio;
    }

    public void setIdArchivio(Integer idArchivio) {
        this.idArchivio = idArchivio;
    }

    public SessioneVersamento.TipologiaVersamento getTipologiaVersamento() {
        return tipologiaVersamento;
    }

    public void setTipologiaVersamento(SessioneVersamento.TipologiaVersamento tipologiaVersamento) {
        this.tipologiaVersamento = tipologiaVersamento;
    }  

    public String getRisultato() {
        return risultato;
    }

    public void setRisultato(String risultato) {
        this.risultato = risultato;
    }
}
