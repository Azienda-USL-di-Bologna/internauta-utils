package it.bologna.ausl.internauta.utils.ribaltone.operation;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReport;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import java.util.List;

/**
 *
 * @author Top
 */
public class OperationAppartenente extends Operation<DatiDaImportareAppartenente> {

    public OperationAppartenente(Azione azione, DatiDaImportareAppartenente entitaCoinvolta) {
        super(azione, entitaCoinvolta);
    }

    @Override
    public void esegui() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    
    
}
