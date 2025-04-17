package it.bologna.ausl.internauta.utils.ribaltone.basedata;

import it.bologna.ausl.model.entities.ribaltonedati.CSVDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.CSVDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.CSVDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.CSVDaImportareTrasformazione;
import it.bologna.ausl.model.entities.tip.ImportazioneOggetto;

/**
 *
 * @author Top
 */
public interface DatiRibaltoneInterface {

    public static enum TipologiaCsv {
        ANAGRAFICA,
        APPARTENENTE,
        STRUTTURA,
        TRASFORMAZIONE
    }

    public static <T extends DatiRibaltoneInterface> T getImportazioneCSVRibaltoneImpl(TipologiaCsv tipologia) {
        T res = null;
        switch (tipologia) {
            case ANAGRAFICA:
                res = (T) new CSVDaImportareAnagrafica();
                break;
            case APPARTENENTE:
                res = (T) new CSVDaImportareAppartenente();
                break;
            case STRUTTURA:
                res = (T) new CSVDaImportareStruttura();
                break;
            case TRASFORMAZIONE:
                res = (T) new CSVDaImportareTrasformazione();
                break;
        }
        return res;
    }

    public String getKey();

    public TipologiaCsv getTipo();

    public String getClasse();

    public Integer getIdAzienda();

}
