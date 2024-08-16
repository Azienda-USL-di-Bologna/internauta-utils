package it.bologna.ausl.internauta.utils.versatore;

import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.versatore.Versamento;
import java.time.ZonedDateTime;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class VersamentoAllegatoInformation {
    
    private Integer idAllegato;
    
    private Allegato.DettagliAllegato.TipoDettaglioAllegato tipoDettaglioAllegato;
    
    private Boolean forzabile = false;
    
    private String metadatiVersati;
    
    private String rapporto;
    
    private String codiceErrore;
    
    private String descrizioneErrore;
    
    private ZonedDateTime dataVersamento;
    
    private Versamento.StatoVersamento statoVersamento;

    public VersamentoAllegatoInformation() {
    }

    public Integer getIdAllegato() {
        return idAllegato;
    }

    public void setIdAllegato(Integer idAllegato) {
        this.idAllegato = idAllegato;
    }

    public Allegato.DettagliAllegato.TipoDettaglioAllegato getTipoDettaglioAllegato() {
        return tipoDettaglioAllegato;
    }

    public void setTipoDettaglioAllegato(Allegato.DettagliAllegato.TipoDettaglioAllegato tipoDettaglioAllegato) {
        this.tipoDettaglioAllegato = tipoDettaglioAllegato;
    }

    public Boolean getForzabile() {
        return forzabile;
    }

    public void setForzabile(Boolean forzabile) {
        this.forzabile = forzabile;
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

    public void setDataVersamento(ZonedDateTime dataVersamento) {
        this.dataVersamento = dataVersamento;
    }

    public Versamento.StatoVersamento getStatoVersamento() {
        return statoVersamento;
    }

    public void setStatoVersamento(Versamento.StatoVersamento statoVersamento) {
        this.statoVersamento = statoVersamento;
    }
}
