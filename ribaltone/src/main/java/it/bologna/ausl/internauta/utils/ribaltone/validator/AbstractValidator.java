package it.bologna.ausl.internauta.utils.ribaltone.validator;

import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import java.util.ArrayList;
import java.util.List;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;

/**
 *  classe che si occupa di effettuare i controlli di coerenza
 * @author Top
 */
public abstract class AbstractValidator {

    protected List<? extends DatiRibaltoneInterface> datiDaImportare = new ArrayList<>();
    protected List<? extends DatiRibaltoneInterface> datiInvalidi = new ArrayList<>();


    protected AbstractValidator(List<? extends DatiRibaltoneInterface> datiDaImportare) {
        this.datiDaImportare = datiDaImportare;
    }

    public List<? extends DatiRibaltoneInterface> getDatiInvalidi() {
        return datiInvalidi;
    }

    public void setDatiInvalidi(List<? extends DatiRibaltoneInterface> datiInvalidi) {
        this.datiInvalidi = datiInvalidi;
    }

    public List<? extends DatiRibaltoneInterface> getDatiDaImportare() {
        return datiDaImportare;
    }

    public void setDatiDaImportare(List<? extends DatiRibaltoneInterface> datiDaImportare) {
        this.datiDaImportare = datiDaImportare;
    }    

    public abstract <T extends DatiRibaltoneInterface> List<T> validate(RepositoryFactory repositoryFactory) throws RibaltoneHttpException;

}
