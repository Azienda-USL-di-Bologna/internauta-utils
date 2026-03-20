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

    private final String UNIMATICA = "unimatica";
    private final String DATA_REGISTRAZIONE = "dataRegistrazione";
    private final String DAYS = "days";
    private final String DIRECTION = "direction";
    private final String AFTER = "after";
    private final String BEFORE = "before";

    @Override
    protected void finalize() throws Throwable {
        super.finalize(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public Boolean checkDocImpl(Integer id, Map<String, Object> params) throws VersatoreProcessingException {
        Boolean idoneo = false;
        log.debug("Sto calcolando l'idoneita del doc " + id.toString());
        Doc doc = entityManager.find(Doc.class, id);
        switch (doc.getTipologia()) {
            //verso sempre gli RGPICO
            case RGPICO:
                idoneo = true;
                log.info("Prendo da versare il documento id: " + id);
                break;
            //verso solo i protocolli registrati da più di 10 giorni
            case PROTOCOLLO_IN_ENTRATA:
            case PROTOCOLLO_IN_USCITA:
                List<ArchivioDoc> archiviDocList = doc.getArchiviDocList()
                    .stream().filter(archivioListObj -> archivioListObj.getDataEliminazione() == null)
                    .collect(Collectors.toList());
                if (archiviDocList != null && !archiviDocList.isEmpty()) {
                    if (configParams.getIdoneitaEligibilityConditionsParams() != null) {
                        Map<String, Object> condizioniUnimatica = (Map<String, Object>) configParams.getIdoneitaEligibilityConditionsParams().get(UNIMATICA);
                        Map<String, Object> condizioniDataRegistrazione = (Map<String, Object>) condizioniUnimatica.get(DATA_REGISTRAZIONE);
                        Integer days = (Integer) condizioniDataRegistrazione.get(DAYS);
                        String direction = (String) condizioniDataRegistrazione.get(DIRECTION);
                        switch (direction) {
                            case AFTER:
                                if (doc.getDataRegistrazione().isAfter(ZonedDateTime.now().minusDays(days))) {
                                    idoneo = true;
                                    log.info("Prendo da versare il documento id: " + id);
                                }
                                break;
                            case BEFORE:
                                if (doc.getDataRegistrazione().isBefore(ZonedDateTime.now().minusDays(days))) {
                                    idoneo = true;
                                    log.info("Prendo da versare il documento id: " + id);
                                }
                                break;
                        }
                    } else {
                        idoneo = true;
                        log.info("Prendo da versare il documento id: " + id);
                    }
                }
                break;
        }
        //non verso pregressi
        if (doc.getPregresso()) {
            idoneo = false;
        }
        return idoneo;
    }

    @Override
    public Boolean checkArchivioImpl(Integer id, Map<String, Object> params) throws VersatoreProcessingException {
        return false;
    }

}
