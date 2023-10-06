package it.bologna.ausl.internauta.utils.versatore.plugins.sdico;

import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.IdoneitaChecker;
import it.bologna.ausl.internauta.utils.versatore.plugins.parer.ParerIdoneitaCheckerService;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Andrea
 */
@Component
public class SdicoIdoneitaCheckerService extends IdoneitaChecker{
    
    private static final Logger log = LoggerFactory.getLogger(ParerIdoneitaCheckerService.class);

    @Override
    public Boolean checkDocImpl(Integer id, Map<String, Object> params) throws VersatoreProcessingException {
        Boolean idoneo = false;
        log.warn("Sto calcolando l'idoneita del doc " + id.toString());
        Doc doc = entityManager.find(Doc.class, id);
        //voglio versare solo gli RGPICO TODO tutti quelli che non son stati versati? Il parametro lo aggiungo alla configurazione?
        if (doc.getTipologia().equals(DocDetailInterface.TipologiaDoc.RGPICO))
        {
            idoneo = true;
            log.info("Prendo da versare il documento id: " + id);
        }
        return idoneo;
    }

    @Override
    public Boolean checkArchivioImpl(Integer id, Map<String, Object> params) throws VersatoreProcessingException {
        // momentaneamente per i test ritorno sempre true
        log.info("Prendo da versare il fasciolo id: " + id);
        return true;
    }
    
}
