package it.bologna.ausl.internauta.utils.versatore;

import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Classe che raccoglie i dati di un versamento. E' usata sia per indicare cosa versare al plugin di versamento,
 * sia il risultato una volta che il plugin ha versato.
 * 
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class VersamentoDocInformation {
    
    private Integer idDoc;
    private Integer idArchivio;
    private SessioneVersamento.TipologiaVersamento tipologiaVersamento;
    private boolean primoVersamento = true;
    private Integer idVersamentoPrecedente;
    private Versamento.StatoVersamento statoVersamentoPrecedente;
    private Boolean forzabile = false;
    private String rapporto;
    private String metadatiVersati;
    private String codiceErrore;
    private String descrizioneErrore;
    private ZonedDateTime dataVersamento;
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

    /**
     * indica il versamento è forzabile.
     * Tipicamente è settato dal plugin, dopo un errore.
     * @return se il versamento è forzabile
     */
    public Boolean getForzabile() {
        return forzabile;
    }

    /**
     * setta se il versamento è forzabile.
     * Tipicamente viene settato dal plugin, dopo un errore.
     * @param forzabile 
     */
    public void setForzabile(Boolean forzabile) {
        this.forzabile = forzabile;
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

    /**
     * indica a quale altro versamento già effettuato, questo versamento è collegato.
     * E' popolato solo se isPrimoVersamento è false.
     * Tipicamente il versamento precedente rappresenta un versamento provato, ma andato in errore o non ancora terminato.
     * @return  l'id del versamento al quale quello attuale è collegato 
     */
    public Integer getIdVersamentoPrecedente() {
        return idVersamentoPrecedente;
    }

    /**
     * setta il versamento già effettuato al quale collegare questo versamento.
     * Da settare solo se isPrimoVersamento è settato a false
     * Tipicamente il versamento precedente rappresenta un versamento provato, ma andato in errore o non ancora terminato.
     * @param idVersamentoPrecedente 
     */
    public void setIdVersamentoPrecedente(Integer idVersamentoPrecedente) {
        this.idVersamentoPrecedente = idVersamentoPrecedente;
    }

    public Versamento.StatoVersamento getStatoVersamentoPrecedente() {
        return statoVersamentoPrecedente;
    }

    public void setStatoVersamentoPrecedente(Versamento.StatoVersamento statoVersamentoPrecedente) {
        this.statoVersamentoPrecedente = statoVersamentoPrecedente;
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

    /**
     * indica se questo è il primo vesamento il doc con quell'archivio, oppure è un versamento collegato ad un altro.
     * Se è true, si può leggere il versamento correlato traminte la getIdVersamentoPrecedente().
     * @return true se è il primo versamento per il doc/archivio, false altrimenti
     */
    public boolean isPrimoVersamento() {
        return primoVersamento;
    }

    /**
     * setta se questo versamento è il primo vesamento il doc con quell'archivio. Di default è true.
     * Se settato a false, va settato anche il versamento precedente tramite la setIdVersamentoPrecedente().
     * @param primoVersamento true se questo è il primo versamento (default), false altrimenti.
     */
    public void setPrimoVersamento(boolean primoVersamento) {
        this.primoVersamento = primoVersamento;
    }
}
