package it.bologna.ausl.internauta.utils.versatore.plugins;

import it.bologna.ausl.internauta.utils.versatore.exceptions.RecuperoRapportoDiVersamentoPluginException;
import it.bologna.ausl.internauta.utils.versatore.utils.VersatoreConfigParams;
import it.bologna.ausl.model.entities.versatore.RapportoDiVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * Classe astratta per la gestione del recupero del rapporto di versamento
 * I vari servizi che eseguono i versamenti dovranno implementare i metodi TODO per eseguire il
 * recupero
 * @author boria
 */
public abstract class RecuperoRapportoDiVersamento {

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected VersatoreConfigParams configParams;

    protected VersatoreConfiguration versatoreConfiguration;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    public void init(VersatoreConfiguration versatoreConfiguration) {
        this.versatoreConfiguration = versatoreConfiguration;
    }

    public RapportoDiVersamento recuperaRapportiDiVersamento(Versamento versamento, Map<String, Object> params) throws RecuperoRapportoDiVersamentoPluginException {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(a -> {
            try {
                return recuperaRapportiDiVersamentoImpl(versamento, params);
            } catch (RecuperoRapportoDiVersamentoPluginException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    /**
    metodo che contatta effettivamente il servizio di conservazione per recuperare i rapporti di versamento
     */
    public abstract RapportoDiVersamento recuperaRapportiDiVersamentoImpl(Versamento versamento, Map<String, Object> params) throws RecuperoRapportoDiVersamentoPluginException;

}
