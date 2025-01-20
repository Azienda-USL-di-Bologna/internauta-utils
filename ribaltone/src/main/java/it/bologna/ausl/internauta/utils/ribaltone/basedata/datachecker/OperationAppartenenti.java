package it.bologna.ausl.internauta.utils.ribaltone.basedata.datachecker;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.userreport.UserReport;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import java.util.List;

/**
 *
 * @author Top
 */
public class OperationAppartenenti extends Operation<DatiDaImportareAppartenente> {

    public OperationAppartenenti(Azione azione, DatiDaImportareAppartenente entitaCoinvolta) {
        super(azione, entitaCoinvolta);
    }

    @Override
    public void esegui() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    
    
}
