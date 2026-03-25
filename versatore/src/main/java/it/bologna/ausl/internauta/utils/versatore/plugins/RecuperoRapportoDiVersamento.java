package it.bologna.ausl.internauta.utils.versatore.plugins;

import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.utils.VersatoreConfigParams;
import it.bologna.ausl.model.entities.versatore.RapportoDiVersamento;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

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

    /**
    contatta il servizio di conservazione per recuperare i rapporti di versamento
    @return
    @throws VersatoreProcessingException
     */
    public abstract RapportoDiVersamento recuperaRapportiDiVersamento() throws VersatoreProcessingException;

}
