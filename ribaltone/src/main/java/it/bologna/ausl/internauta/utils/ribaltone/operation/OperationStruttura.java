package it.bologna.ausl.internauta.utils.ribaltone.operation;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;


/**
 *
 * @author Top
 */
public class OperationStruttura extends Operation<DatiRibaltoneInterface>{

    public OperationStruttura(Azione azione, DatiRibaltoneInterface entitaCoinvolta) {
        super(azione, entitaCoinvolta);
    }

    @Override
    public void esegui() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    
    
}
