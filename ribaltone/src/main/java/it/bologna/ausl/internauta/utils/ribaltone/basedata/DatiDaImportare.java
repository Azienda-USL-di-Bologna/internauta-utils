package it.bologna.ausl.internauta.utils.ribaltone.basedata;

import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.utils.RibaltoneUtils;
import it.bologna.ausl.internauta.utils.ribaltone.validator.ValidatorAnagrafiche;
import it.bologna.ausl.internauta.utils.ribaltone.validator.ValidatorAppartenenti;
import it.bologna.ausl.internauta.utils.ribaltone.validator.ValidatorStrutture;
import it.bologna.ausl.internauta.utils.ribaltone.validator.ValidatorTrasformazioni;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Top
 */
public class DatiDaImportare {

    private List<DatiDaImportareAnagrafica> anagraficheDaImportare;
    private List<DatiDaImportareStruttura> struttureDaImportare;
    private List<DatiDaImportareAppartenente> appartenentiDaImportare;
    private List<DatiDaImportareTrasformazione> trasformazioniDaImportare;
    protected Map<String, Integer> indexStrutture;
    protected Map<String, Integer> indexTrasformazioni;
    protected Map<String, Integer> indexAppartenenti;
    protected Map<String, Integer> indexAnagrafiche;
    private Integer progressivoTrasformazione;
    private RepositoryFactory repositoryFactory;

    public DatiDaImportare(List<DatiDaImportareAnagrafica> anagraficheDaImportare, List<DatiDaImportareStruttura> struttureDaImportare, List<DatiDaImportareAppartenente> appartenentiDaImportare, List<DatiDaImportareTrasformazione> trasformazioniDaImportare, Integer progressivoTrasformazione, RepositoryFactory repositoryFactory) {
        this.anagraficheDaImportare = anagraficheDaImportare;
        this.struttureDaImportare = struttureDaImportare;
        this.appartenentiDaImportare = appartenentiDaImportare;
        this.trasformazioniDaImportare = trasformazioniDaImportare;
        this.progressivoTrasformazione = progressivoTrasformazione;
        //qui metto getidcasella perche altrimenti il controllo sui padri morti che prende in considerazione id padre mi verrebbe piu complicato
        this.indexStrutture = RibaltoneUtils.generateIndex(this.struttureDaImportare, DatiDaImportareStruttura::getIdCasella);
        this.indexAnagrafiche = RibaltoneUtils.generateIndex(this.anagraficheDaImportare, DatiDaImportareAnagrafica::getKey);
        this.indexTrasformazioni = RibaltoneUtils.generateIndex(this.trasformazioniDaImportare, DatiDaImportareTrasformazione::getKey);
        this.indexAppartenenti = RibaltoneUtils.generateIndex(this.appartenentiDaImportare, DatiDaImportareAppartenente::getKey);
        this.repositoryFactory = repositoryFactory;
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

    public DatiDaImportare validate() throws RibaltoneHttpException {
        ValidatorStrutture validatorStrutture = new ValidatorStrutture(struttureDaImportare, indexStrutture);
        DatiDaImportare datiDaImportareValidati = null;
        
        List<DatiDaImportareStruttura> struttureValideDaImportare = validatorStrutture.validate(repositoryFactory);
        if (!validatorStrutture.getDatiInvalidi().isEmpty()){
            //posso non controllare altro questi sono errori che bloccano il ribaltone
        }else {
            indexStrutture = RibaltoneUtils.generateIndex(struttureValideDaImportare,DatiDaImportareStruttura::getKey);
            ValidatorTrasformazioni validatorTrasformazioni = new ValidatorTrasformazioni(trasformazioniDaImportare,indexStrutture, indexTrasformazioni, progressivoTrasformazione);
            //mi serve l'index delle strurrue per il controllo sulle trasformazioni
            List<DatiDaImportareTrasformazione> trasformazioniValideDaImportare = validatorTrasformazioni.validate(repositoryFactory);
            progressivoTrasformazione = validatorTrasformazioni.getProgressivoTrasformazione();
            if (!validatorTrasformazioni.getDatiInvalidi().isEmpty()){
                //posso non controllare altro perche ci sono problemi con le trasformazioni fornite
            }else {
                ValidatorAppartenenti validatorAppartenenti = new ValidatorAppartenenti(appartenentiDaImportare, indexStrutture, indexAppartenenti);
                List<DatiDaImportareAppartenente> appartenentiValidiDaImportare = validatorAppartenenti.validate(repositoryFactory);
                
                List<DatiDaImportareAnagrafica> anagraficheValideDaImportare = (new ValidatorAnagrafiche(anagraficheDaImportare)).validate(repositoryFactory);
                datiDaImportareValidati = new DatiDaImportare(anagraficheValideDaImportare, struttureValideDaImportare, appartenentiValidiDaImportare, trasformazioniValideDaImportare, progressivoTrasformazione, repositoryFactory);
            
            }
            
            
        }
        return datiDaImportareValidati;
    }
    
    

//    public UserReport getUserReport() {
//        DatiDaImportareValidated datiDaImportareChecked = new DatiDaImportareValidated(anagraficheDaImportare, appartenentiDaImportare, struttureDaImportare, trasformazioniDaImportare);
//        return datiDaImportareChecked.getUserReport();
//    }

}
