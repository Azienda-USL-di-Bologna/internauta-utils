package it.bologna.ausl.internauta.utils.ribaltone.operation;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import jakarta.persistence.EntityManager;
import java.io.Serializable;

/**
 *
 * @author Top
 */
public class OperationAnagrafica extends Operation<DatiRibaltoneInterface> implements Serializable{

    public OperationAnagrafica(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager) {
        super(azione, entitaCoinvolta, entityManager);
    }

    @Override
    public void esegui(Object workToDo) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}
