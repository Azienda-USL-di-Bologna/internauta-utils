package it.bologna.ausl.internauta.utils.ribaltone.basedata;

import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.utils.RibaltoneUtils;
import it.bologna.ausl.internauta.utils.ribaltone.validator.ValidatorAnagrafiche;
import it.bologna.ausl.internauta.utils.ribaltone.validator.ValidatorAppartenenti;
import it.bologna.ausl.internauta.utils.ribaltone.validator.ValidatorStrutture;
import it.bologna.ausl.internauta.utils.ribaltone.validator.ValidatorTrasformazioni;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiTrasformazione;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Top
 */
public class DatiImportati {

    private List<DatiImportatiAnagrafica> anagraficheImportati;
    private List<DatiImportatiStruttura> struttureImportati;
    private List<DatiImportatiAppartenente> appartenentiImportati;
    private List<DatiImportatiTrasformazione> trasformazioniImportati;
    protected Map<String, Integer> indexStrutture;
    protected Map<String, Integer> indexTrasformazioni;
    protected Map<String, Integer> indexAppartenenti;
    protected Map<String, Integer> indexAnagrafiche;

    public DatiImportati(List<DatiImportatiAnagrafica> anagraficheImportati, List<DatiImportatiStruttura> struttureImportati, List<DatiImportatiAppartenente> appartenentiImportati, List<DatiImportatiTrasformazione> trasformazioniImportati) {
        this.anagraficheImportati = anagraficheImportati;
        this.struttureImportati = struttureImportati;
        this.appartenentiImportati = appartenentiImportati;
        this.trasformazioniImportati = trasformazioniImportati;
        this.indexStrutture = RibaltoneUtils.generateIndex2(this.struttureImportati, DatiImportatiStruttura::getKey);
        this.indexAnagrafiche = RibaltoneUtils.generateIndex2(this.anagraficheImportati, DatiImportatiAnagrafica::getKey);
        this.indexTrasformazioni = RibaltoneUtils.generateIndex2(this.trasformazioniImportati, DatiImportatiTrasformazione::getKey);
        this.indexAppartenenti = RibaltoneUtils.generateIndex2(this.appartenentiImportati, DatiImportatiAppartenente::getKey);

    }

    public List<DatiImportatiAnagrafica> getAnagraficheImportati() {
        return anagraficheImportati;
    }

    public void setAnagraficheImportati(List<DatiImportatiAnagrafica> anagraficheImportati) {
        this.anagraficheImportati = anagraficheImportati;
    }

    public List<DatiImportatiStruttura> getStruttureImportati() {
        return struttureImportati;
    }

    public void setStruttureImportati(List<DatiImportatiStruttura> struttureImportati) {
        this.struttureImportati = struttureImportati;
    }

    public List<DatiImportatiAppartenente> getAppartenentiImportati() {
        return appartenentiImportati;
    }

    public void setAppartenentiImportati(List<DatiImportatiAppartenente> appartenentiImportati) {
        this.appartenentiImportati = appartenentiImportati;
    }

    public List<DatiImportatiTrasformazione> getTrasformazioniImportati() {
        return trasformazioniImportati;
    }

    public void setTrasformazioniImportati(List<DatiImportatiTrasformazione> trasformazioniImportati) {
        this.trasformazioniImportati = trasformazioniImportati;
    }

//    public UserReport getUserReport() {
//        DatiImportatiValidated datiImportatiChecked = new DatiImportatiValidated(anagraficheImportati, appartenentiImportati, struttureImportati, trasformazioniImportati);
//        return datiImportatiChecked.getUserReport();
//    }
}
