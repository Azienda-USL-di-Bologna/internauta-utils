package it.bologna.ausl.internauta.utils.ribaltone.operation;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import java.io.Serializable;

/**
 *
 * @author Top
 */
public class OperationAnagrafica extends Operation<DatiRibaltoneInterface> implements Serializable{

    public OperationAnagrafica(Azione azione, DatiRibaltoneInterface entitaCoinvolta) {
        super(azione, entitaCoinvolta);
    }

    @Override
    public void esegui() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}
