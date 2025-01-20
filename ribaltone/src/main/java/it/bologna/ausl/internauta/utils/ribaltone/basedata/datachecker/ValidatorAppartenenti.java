package it.bologna.ausl.internauta.utils.ribaltone.basedata.datachecker;

import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import java.util.List;

/**
 *
 * @author Top
 */
public class ValidatorAppartenenti extends AbstractValidator{

    public ValidatorAppartenenti(List<DatiDaImportareAppartenente> datiDaImportare) {
        super(datiDaImportare);
    }

    @Override
    public List<DatiDaImportareAppartenente> validate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }



    


    



    
    
}
