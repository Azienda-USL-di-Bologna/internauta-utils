package it.bologna.ausl.internauta.utils.ribaltone.operation;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import jakarta.persistence.EntityManager;
import java.io.Serializable;

/**
 *
 * @author Top
 */
public class OperationUnificazione extends Operation<DatiRibaltoneInterface> implements Serializable {

    public OperationUnificazione(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager) {
        super(azione, entitaCoinvolta, entityManager);
    }

    @Override
    public void esegui(Object workToDo, RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        switch (getAzione()) {
            case CAMBIO_PADRE:

                break;
            case CONFLUENZA:

                break;
            case RINOMINA:

                break;
            case CHIUSURA:

                break;
            default:
                throw new AssertionError();
        }
    }

    public void menageContatto(RepositoryFactory repositoryFactory) {

    }

}
