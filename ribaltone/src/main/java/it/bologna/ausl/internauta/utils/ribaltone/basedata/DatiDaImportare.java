package it.bologna.ausl.internauta.utils.ribaltone.basedata;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.datachecker.ValidatorAnagrafiche;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.datachecker.ValidatorAppartenenti;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.datachecker.ValidatorStrutture;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.datachecker.ValidatorTrasformazioni;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import java.util.List;

/**
 *
 * @author Top
 */
public class DatiDaImportare {

    private List<DatiDaImportareAnagrafica> anagraficheDaImportare;
    private List<DatiDaImportareStruttura> struttureDaImportare;
    private List<DatiDaImportareAppartenente> appartenentiDaImportare;
    private List<DatiDaImportareTrasformazione> trasformazioniDaImportare;

    public DatiDaImportare(List<DatiDaImportareAnagrafica> anagraficheDaImportare, List<DatiDaImportareStruttura> struttureDaImportare, List<DatiDaImportareAppartenente> appartenentiDaImportare, List<DatiDaImportareTrasformazione> trasformazioniDaImportare) {
        this.anagraficheDaImportare = anagraficheDaImportare;
        this.struttureDaImportare = struttureDaImportare;
        this.appartenentiDaImportare = appartenentiDaImportare;
        this.trasformazioniDaImportare = trasformazioniDaImportare;
    }

    public List<DatiDaImportareAnagrafica> getAnagraficheDaImportare() {
        return anagraficheDaImportare;
    }

    public void setAnagraficheDaImportare(List<DatiDaImportareAnagrafica> anagraficheDaImportare) {
        this.anagraficheDaImportare = anagraficheDaImportare;
    }

    public List<DatiDaImportareStruttura> getStruttureDaImportare() {
        return struttureDaImportare;
    }

    public void setStruttureDaImportare(List<DatiDaImportareStruttura> struttureDaImportare) {
        this.struttureDaImportare = struttureDaImportare;
    }

    public List<DatiDaImportareAppartenente> getAppartenentiDaImportare() {
        return appartenentiDaImportare;
    }

    public void setAppartenentiDaImportare(List<DatiDaImportareAppartenente> appartenentiDaImportare) {
        this.appartenentiDaImportare = appartenentiDaImportare;
    }

    public List<DatiDaImportareTrasformazione> getTrasformazioniDaImportare() {
        return trasformazioniDaImportare;
    }

    public void setTrasformazioniDaImportare(List<DatiDaImportareTrasformazione> trasformazioniDaImportare) {
        this.trasformazioniDaImportare = trasformazioniDaImportare;
    }

    public DatiDaImportare validate() {
        List<DatiDaImportareStruttura> struttureValideDaImportare = (new ValidatorStrutture(struttureDaImportare)).validate();
        List<DatiDaImportareAppartenente> appartenentiValidiDaImportare = (new ValidatorAppartenenti(appartenentiDaImportare)).validate();
        List<DatiDaImportareAnagrafica> anagraficheValideDaImportare = (new ValidatorAnagrafiche(anagraficheDaImportare)).validate();
        List<DatiDaImportareTrasformazione> trasformazioniValideDaImportare = (new ValidatorTrasformazioni(trasformazioniDaImportare)).validate();
        DatiDaImportare datiDaImportareValidati = new DatiDaImportare(anagraficheValideDaImportare, struttureValideDaImportare, appartenentiValidiDaImportare, trasformazioniValideDaImportare);
        return datiDaImportareValidati;
    }

//    public UserReport getUserReport() {
//        DatiDaImportareValidated datiDaImportareChecked = new DatiDaImportareValidated(anagraficheDaImportare, appartenentiDaImportare, struttureDaImportare, trasformazioniDaImportare);
//        return datiDaImportareChecked.getUserReport();
//    }

}
