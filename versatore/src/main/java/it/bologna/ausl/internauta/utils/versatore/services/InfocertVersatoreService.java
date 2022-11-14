package it.bologna.ausl.internauta.utils.versatore.services;

import it.bologna.ausl.internauta.utils.versatore.VersamentoInformation;
import it.bologna.ausl.internauta.utils.versatore.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.enums.AttributesEnum;
import it.bologna.ausl.internauta.utils.versatore.utils.VersatoreConfigParams;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.versatore.Configuration;
import it.bologna.ausl.utils.versatore.infocert.wsclient.DocumentAttribute;
import it.bologna.ausl.utils.versatore.infocert.wsclient.GenericDocument;
import it.bologna.ausl.utils.versatore.infocert.wsclient.GenericDocumentService;
import java.net.MalformedURLException; 
import java.net.URL; 
import java.util.ArrayList; 
import java.util.List; 
import java.util.Map;
import javax.activation.DataHandler; 
import javax.activation.FileDataSource; 
import javax.xml.ws.BindingProvider; 
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
        log.info(String.format("URI: %s", infocertVersatoreServiceEndPointUri));
    }

    @Override
    public VersamentoInformation versa(VersamentoInformation versamentoInformation) {

        Integer idDoc = versamentoInformation.getIdDoc();

        Doc doc = entityManager.find(Doc.class, idDoc);

        log.info(doc.getOggetto());

        log.info(AttributesEnum.RISERVATO.toString());

        GenericDocumentService iss;

        try {
            iss = new GenericDocumentService(new URL(infocertVersatoreServiceEndPointUri));

            GenericDocument is = iss.getGenericDocumentPort();
            BindingProvider bp = (BindingProvider) is;
            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                    infocertVersatoreServiceEndPointUri);
            List<DocumentAttribute> attributes = new ArrayList();

            DocumentAttribute attr1 = new DocumentAttribute();
            attr1.setName(AttributesEnum.NOME.toString());
            attr1.setValue("Mario");
            DocumentAttribute attr2 = new DocumentAttribute();
            attr2.setName(AttributesEnum.COGNOME.toString());
            attr2.setValue("Rossi");
            String docID = "0123456";

            attributes.add(attr1);
            attributes.add(attr2);
//            DataHandler data = new DataHandler(new FileDataSource(arg[1]));
            DataHandler data = null;
            is.submitDocument(docID, attributes, data);

        } catch (MalformedURLException e) {
            log.error(e.getMessage());
        }

        return versamentoInformation;
    }

}
