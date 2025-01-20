package it.bologna.ausl.internauta.utils.ribaltone.basedata.datachecker;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportareInterface;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import java.util.List;

/**
 *
 * @author Top
 */
public class ValidatorTrasformazioni extends AbstractValidator{

    public ValidatorTrasformazioni(List<DatiDaImportareTrasformazione> datiDaImportare) {
        super(datiDaImportare);
    }    

    @Override
    public List<DatiDaImportareTrasformazione> validate() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}
