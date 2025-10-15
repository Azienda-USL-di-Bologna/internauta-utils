package it.bologna.ausl.model.entities.ribaltonedati;

import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 *
 * @author Top
 */
public class UnificazioneDaGestire implements Serializable {

    private Integer idCasellaSorgente;
    private String nomeStrutturaSorgente;
    private String nomeAziendaSorgente;
    private Integer idAziendaSorgente;

    private Integer idCasellaDestinazione;
    private String nomeStrutturaDestinazione;
    private String nomeAziendaDestinazione;
    private Integer idAziendaDestinazione;

    private ZonedDateTime dataAccensioneAttivazione;

    private StrutturaUnificata.TipoUnificazione tipoOperazione;

    private Integer idUnificazione;

    public static UnificazioneDaGestire buildUnificazioneEseguita(StrutturaUnificata su) {
        UnificazioneDaGestire ue = new UnificazioneDaGestire();
        ue.setIdUnificazione(su.getId());
        ue.setIdCasellaSorgente(su.getIdStrutturaSorgente().getIdCasella());
        ue.setNomeStrutturaSorgente(su.getIdStrutturaSorgente().getNome());
        ue.setNomeAziendaSorgente(su.getIdStrutturaSorgente().getIdAzienda().getNome());
        ue.setIdAziendaSorgente(su.getIdStrutturaSorgente().getIdAzienda().getId());

        ue.setIdCasellaDestinazione(su.getIdStrutturaDestinazione().getIdCasella());
        ue.setNomeStrutturaDestinazione(su.getIdStrutturaDestinazione().getNome());
        ue.setNomeAziendaDestinazione(su.getIdStrutturaDestinazione().getIdAzienda().getNome());
        ue.setIdAziendaDestinazione(su.getIdStrutturaDestinazione().getIdAzienda().getId());
        ue.setDataAccensioneAttivazione(su.getDataAccensioneAttivazione());

        ue.setTipoOperazione(su.getTipoOperazione());

        return ue;
    }

    public Integer getIdCasellaSorgente() {
        return idCasellaSorgente;
    }

    public void setIdCasellaSorgente(Integer idCasellaSorgente) {
        this.idCasellaSorgente = idCasellaSorgente;
    }

    public String getNomeStrutturaSorgente() {
        return nomeStrutturaSorgente;
    }

    public void setNomeStrutturaSorgente(String nomeStrutturaSorgente) {
        this.nomeStrutturaSorgente = nomeStrutturaSorgente;
    }

    public String getNomeAziendaSorgente() {
        return nomeAziendaSorgente;
    }

    public void setNomeAziendaSorgente(String nomeAziendaSorgente) {
        this.nomeAziendaSorgente = nomeAziendaSorgente;
    }

    public Integer getIdCasellaDestinazione() {
        return idCasellaDestinazione;
    }

    public void setIdCasellaDestinazione(Integer idCasellaDestinazione) {
        this.idCasellaDestinazione = idCasellaDestinazione;
    }

    public String getNomeStrutturaDestinazione() {
        return nomeStrutturaDestinazione;
    }

    public void setNomeStrutturaDestinazione(String nomeStrutturaDestinazione) {
        this.nomeStrutturaDestinazione = nomeStrutturaDestinazione;
    }

    public String getNomeAziendaDestinazione() {
        return nomeAziendaDestinazione;
    }

    public void setNomeAziendaDestinazione(String nomeAziendaDestinazione) {
        this.nomeAziendaDestinazione = nomeAziendaDestinazione;
    }

    public ZonedDateTime getDataAccensioneAttivazione() {
        return dataAccensioneAttivazione;
    }

    public void setDataAccensioneAttivazione(ZonedDateTime dataAccensioneAttivazione) {
        this.dataAccensioneAttivazione = dataAccensioneAttivazione;
    }

    public StrutturaUnificata.TipoUnificazione getTipoOperazione() {
        return tipoOperazione;
    }

    public void setTipoOperazione(StrutturaUnificata.TipoUnificazione tipoOperazione) {
        this.tipoOperazione = tipoOperazione;
    }

    public Integer getIdUnificazione() {
        return idUnificazione;
    }

    public void setIdUnificazione(Integer idUnificazione) {
        this.idUnificazione = idUnificazione;
    }

    public String getClasse() {
        return UnificazioneDaGestire.class.getCanonicalName();
    }

    public Integer getIdAziendaSorgente() {
        return idAziendaSorgente;
    }

    public void setIdAziendaSorgente(Integer idAziendaSorgente) {
        this.idAziendaSorgente = idAziendaSorgente;
    }

    public Integer getIdAziendaDestinazione() {
        return idAziendaDestinazione;
    }

    public void setIdAziendaDestinazione(Integer idAziendaDestinazione) {
        this.idAziendaDestinazione = idAziendaDestinazione;
    }

}
