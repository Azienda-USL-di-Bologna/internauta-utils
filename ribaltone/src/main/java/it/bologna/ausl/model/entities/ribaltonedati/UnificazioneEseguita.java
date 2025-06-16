/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.model.entities.ribaltonedati;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 *
 * @author Top
 */
public class UnificazioneEseguita implements Serializable, DatiRibaltoneInterface {

    private Integer idCasellaSorgente;
    private String nomeStrutturaSorgente;
    private String nomeAziendaSorgente;

    private Integer idCasellaDestinazione;
    private String nomeStrutturaDestinazione;
    private String nomeAziendaDestinazione;

    private ZonedDateTime dataAccensioneAttivazione;

    private StrutturaUnificata.TipoUnificazione tipoOperazione;

    private Integer idAzienda;

    public static UnificazioneEseguita buildUnificazioneEseguita(StrutturaUnificata su) {
        UnificazioneEseguita ue = new UnificazioneEseguita();
        ue.setIdCasellaSorgente(su.getIdStrutturaSorgente().getIdCasella());
        ue.setNomeStrutturaSorgente(su.getIdStrutturaSorgente().getNome());
        ue.setNomeAziendaSorgente(su.getIdStrutturaSorgente().getIdAzienda().getNome());

        ue.setIdCasellaDestinazione(su.getIdStrutturaDestinazione().getIdCasella());
        ue.setNomeStrutturaDestinazione(su.getIdStrutturaDestinazione().getNome());
        ue.setNomeAziendaDestinazione(su.getIdStrutturaDestinazione().getIdAzienda().getNome());

        ue.setDataAccensioneAttivazione(su.getDataAccensioneAttivazione());

        ue.setTipoOperazione(su.getTipoOperazione());

        ue.setIdAzienda(su.getIdStrutturaSorgente().getIdAzienda().getId());

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

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public TipologiaCsv getTipo() {
        return TipologiaCsv.UNIFICAZIONI;
    }

    @Override
    public String getClasse() {
        return DatiImportatiAnagrafica.class.getCanonicalName();
    }

    @Override
    public Integer getIdAzienda() {
        return idAzienda;
    }

    public void setIdAzienda(Integer idAzienda) {
        this.idAzienda = idAzienda;
    }

}
