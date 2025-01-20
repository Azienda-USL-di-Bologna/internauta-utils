package it.bologna.ausl.internauta.utils.ribaltone.basedata.datachecker;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportareInterface;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import java.util.List;

/**
 *
 * @author Top
 */
public class ValidatorAnagrafiche extends AbstractValidator{

    public ValidatorAnagrafiche(List<DatiDaImportareAnagrafica> datiDaImportare) {
        super(datiDaImportare);
    }

    @Override
    public List<DatiDaImportareAnagrafica> validate() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    


    



    
    
}
