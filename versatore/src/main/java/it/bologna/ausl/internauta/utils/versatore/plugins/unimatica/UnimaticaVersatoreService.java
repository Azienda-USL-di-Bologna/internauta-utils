package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica;

import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatorePluginException;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatorePluginExceptionRitentabile;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders.AllegatiBuilderUnimatica;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders.AllegatoUnimatica;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders.MetadatiBuilder;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author boria
 */
@Component
public class UnimaticaVersatoreService extends VersatoreDocs {

    private static final Logger log = LoggerFactory.getLogger(UnimaticaVersatoreService.class);
    private static final String UNIMATICA_VERSATORE_SERVICE = "UnimaticaVersatoreService";

    private String unimaticaServizioVersamentoURI;

    @Override
    public void init(VersatoreConfiguration versatoreConfiguration) {
        super.init(versatoreConfiguration);
        Map<String, Object> versatoreConfigurationMap = this.versatoreConfiguration.getParams();
        Map<String, Object> unimaticaServiceConfiguration = (Map<String, Object>) versatoreConfigurationMap.get(UNIMATICA_VERSATORE_SERVICE);
        unimaticaServizioVersamentoURI = unimaticaServiceConfiguration.get("UnimaticaServizioVersamentoURI").toString();
        log.info("Unimatica servizio versamento URI: {}", unimaticaServizioVersamentoURI);
    }

    @Override
    public VersamentoDocInformation versaImpl(VersamentoDocInformation versamentoDocInformation) throws VersatoreProcessingException {
        Map<String, Object> mappaResultAndAllegati = new HashMap<>();
        //Reperisoco i risultati del versamento
        mappaResultAndAllegati = versaDocumentoUnimatica(versamentoDocInformation);

        return versamentoDocInformation;
    }

    /**
     * Metodo che si occupa di fare la chiamata al Gateway di Unimatica per versare il documento
     * indicato in versamentoDocInformation. Restituisce una mappa contenete ((TODO
     * l'indice json e)) i metadati versati, la response proveniente da Unimatica e la lista dei
     * VersamentoAllegatoInformation dei file versati.
     *
     * @param versamentoDocInformation
     * @return
     */
    public Map<String, Object> versaDocumentoUnimatica(VersamentoDocInformation versamentoDocInformation) throws VersatoreProcessingException {
        log.info("Inizio con il versamento del doc: " + Integer.toString(versamentoDocInformation.getIdDoc()));
        //preparo i dati
        Integer idDoc = versamentoDocInformation.getIdDoc();
        Map<String, Object> risultatoEVersamentiAllegati = new HashMap<>();
        Map<String, Object> parametriVersamento = versamentoDocInformation.getParams();
        try {
            Doc doc = entityManager.find(Doc.class, idDoc);
            try {
                log.info("accedo ai dati degli allegati e li inserisco nell'XML");
                List<Allegato> allegati = doc.getAllegati();
                AllegatiBuilderUnimatica allegatiBuild = new AllegatiBuilderUnimatica(versatoreRepositoryConfiguration);
                Map<String, Object> mappaDatiAllegati = allegatiBuild.buildMappaAllegati(doc, allegati);
                AllegatoUnimatica documentoPrincipale = (AllegatoUnimatica) mappaDatiAllegati.get("documentoPrincipale");
                //creazione dell xml
                MetadatiBuilder metadatiBuilder = new MetadatiBuilder(parametriVersamento, documentoPrincipale);
                metadatiBuilder.build();
                String metadati = metadatiBuilder.toString();
                risultatoEVersamentiAllegati.put("metadati", metadati);
            } catch (VersatorePluginException e) {
                log.error("Errore:", e);
                //TODO fare una response per unimatica
                //response.setErrorMessage(e.getMessage());
                //response.setResponseCode(ERRORE_PLUG_IN);
            } catch (VersatorePluginExceptionRitentabile e) {
                log.error("Errore:", e);
                //TODO fare una response per unimatica
                //response.setErrorMessage(e.getMessage());
                //response.setResponseCode(ERRORE_PLUG_IN_RITENTABILE);
            }
        } catch (Exception e) {
            //TODO fare una response per unimatica
            //response.setErrorMessage("Causa errore: " + e.getCause() + ", messaggio: " + e.getMessage());
            //response.setResponseCode(ERRORE_PLUG_IN);
            log.error("Causa errore: " + e.getCause() + ", messaggio: " + e.getMessage(), e);
        }
        //chiamata autenticazione
        //creazione del json
        //stream dei file
        //creazione del multipart
        //invio
        //lettura esito
        return risultatoEVersamentiAllegati;
    }

}
