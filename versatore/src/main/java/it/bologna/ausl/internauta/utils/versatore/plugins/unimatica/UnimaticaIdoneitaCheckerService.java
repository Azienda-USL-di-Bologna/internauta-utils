package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica;

import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.IdoneitaChecker;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
        //verso sempre gli RGPICO
        switch (doc.getTipologia()) {
            //verso sempre gli RGPICO
            case RGPICO:
                idoneo = true;
                log.info("Prendo da versare il documento id: " + id);
                break;
            //verso solo i protocolli registrati da più di 10 giorni
            case PROTOCOLLO_IN_ENTRATA:
            case PROTOCOLLO_IN_USCITA:
                //TODO mettere before, vedere come
                List<ArchivioDoc> archiviDocList = doc.getArchiviDocList()
                    .stream().filter(archivioListObj -> archivioListObj.getDataEliminazione() == null)
                    .collect(Collectors.toList());
                if (doc.getDataRegistrazione().isAfter(ZonedDateTime.now().minusDays(10)) && archiviDocList != null && !archiviDocList.isEmpty()) {
                    idoneo = true;
                    log.info("Prendo da versare il documento id: " + id);
                }
                break;
        }
        return idoneo;
    }

    @Override
    public Boolean checkArchivioImpl(Integer id, Map<String, Object> params) throws VersatoreProcessingException {
        //TODO
        //log.info("Prendo da versare il fasciolo id: " + id);
        //return true;
        return false;
    }

}
