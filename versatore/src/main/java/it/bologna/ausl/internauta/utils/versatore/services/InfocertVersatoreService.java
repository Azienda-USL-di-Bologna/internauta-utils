package it.bologna.ausl.internauta.utils.versatore.services;

import it.bologna.ausl.internauta.utils.versatore.VersamentoInformation;
import it.bologna.ausl.internauta.utils.versatore.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.utils.VersatoreConfigParams;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.versatore.Configuration;
import java.util.Map;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class InfocertVersatoreService extends VersatoreDocs {
    
    private static final Logger log = LoggerFactory.getLogger(InfocertVersatoreService.class);
    private static final String INFOCERT_VERSATORE_SERVICE = "InfocertVersatoreService";  
    
    private final String infocertVersatoreServiceEndPointUri;

    public InfocertVersatoreService(EntityManager entityManager, VersatoreConfigParams versatoreConfigParams, Configuration configuration) {
        super(entityManager, versatoreConfigParams, configuration);
        
        Map<String, Object> firmaRemotaConfiguration = configuration.getParams();
        Map<String, Object> infocertServiceConfiguration = (Map<String, Object>) firmaRemotaConfiguration.get(INFOCERT_VERSATORE_SERVICE);
        infocertVersatoreServiceEndPointUri = infocertServiceConfiguration.get("InfocertVersatoreServiceEndPointUri").toString();
        log.info("URI: %s", infocertVersatoreServiceEndPointUri);
    }

    @Override
    public VersamentoInformation versa(VersamentoInformation versamentoInformation) {
        
        Integer idDoc = versamentoInformation.getIdDoc();
        
        Doc doc = entityManager.find(Doc.class, idDoc);
        
        log.info(doc.getOggetto());
        
        return versamentoInformation;
    }
    
}
