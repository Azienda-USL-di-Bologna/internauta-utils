package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica;

import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.IdoneitaChecker;
import it.bologna.ausl.model.entities.scripta.Doc;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author boria
 */
@Component
@Scope("prototype")
public class UnimaticaIdoneitaCheckerService extends IdoneitaChecker {

    private static final Logger log = LoggerFactory.getLogger(UnimaticaIdoneitaCheckerService.class);

    @Override
    public Boolean checkDocImpl(Integer id, Map<String, Object> params) throws VersatoreProcessingException {
        Boolean idoneo = false;
        log.debug("Sto calcolando l'idoneita del doc " + id.toString());
        Doc doc = entityManager.find(Doc.class, id);
        //voglio versare solo gli RGPICO
        if (doc.getTipologia().equals(Doc.TipologiaDoc.RGPICO)) {
            idoneo = true;
            log.info("Prendo da versare il documento id: " + id);
        }
        return idoneo;
    }

    @Override
    public Boolean checkArchivioImpl(Integer id, Map<String, Object> params) throws VersatoreProcessingException {
        log.info("Prendo da versare il fasciolo id: " + id);
        return true;
    }

}
