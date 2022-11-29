package it.bologna.ausl.internauta.utils.versatore;

import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import java.util.List;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class VersamentoDocInformation {
    
    private Integer idDoc;
    private Integer idArchivio;
    private SessioneVersamento.TipologiaVersamento tipologiaVersamento;
    private String rapporto;
    private List<VersamentoAllegatoInformation> veramentiAllegatiInformations;

    public VersamentoDocInformation() {
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
        return rapporto;
    }

    public void setRisultato(String risultato) {
        this.rapporto = risultato;
    }

    public List<VersamentoAllegatoInformation> getVeramentiAllegatiInformations() {
        return veramentiAllegatiInformations;
    }

    public void setVeramentiAllegatiInformations(List<VersamentoAllegatoInformation> veramentiAllegatiInformations) {
        this.veramentiAllegatiInformations = veramentiAllegatiInformations;
    }
}
