package it.bologna.ausl.internauta.utils.versatore;

import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import java.time.ZonedDateTime;
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
    private String metadatiVersati;
    private String codiceErrore;
    private String descrizioneErrore;
    private ZonedDateTime dataVersamento;
    private Integer versamentoPrecedente;
    private List<VersamentoAllegatoInformation> versamentiAllegatiInformations;
    private Versamento.StatoVersamento statoVersamento;

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

    public String getRapporto() {
        return rapporto;
    }

    public void setRapporto(String rapporto) {
        this.rapporto = rapporto;
    }

    public String getMetadatiVersati() {
        return metadatiVersati;
    }

    public void setMetadatiVersati(String metadatiVersati) {
        this.metadatiVersati = metadatiVersati;
    }

    public String getCodiceErrore() {
        return codiceErrore;
    }

    public void setCodiceErrore(String codiceErrore) {
        this.codiceErrore = codiceErrore;
    }

    public String getDescrizioneErrore() {
        return descrizioneErrore;
    }

    public void setDescrizioneErrore(String descrizioneErrore) {
        this.descrizioneErrore = descrizioneErrore;
    }

    public ZonedDateTime getDataVersamento() {
        return dataVersamento;
    }

    public Integer getVersamentoPrecedente() {
        return versamentoPrecedente;
    }

    public void setVersamentoPrecedente(Integer versamentoPrecedente) {
        this.versamentoPrecedente = versamentoPrecedente;
    }

    public void setDataVersamento(ZonedDateTime dataVersamento) {
        this.dataVersamento = dataVersamento;
    }

    public List<VersamentoAllegatoInformation> getVersamentiAllegatiInformations() {
        return versamentiAllegatiInformations;
    }

    public void setVersamentiAllegatiInformations(List<VersamentoAllegatoInformation> versamentiAllegatiInformations) {
        this.versamentiAllegatiInformations = versamentiAllegatiInformations;
    }

    public Versamento.StatoVersamento getStatoVersamento() {
        return statoVersamento;
    }

    public void setStatoVersamento(Versamento.StatoVersamento statoVersamento) {
        this.statoVersamento = statoVersamento;
    }
}
