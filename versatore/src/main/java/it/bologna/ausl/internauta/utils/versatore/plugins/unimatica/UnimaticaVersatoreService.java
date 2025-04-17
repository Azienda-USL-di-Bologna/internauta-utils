package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica;

import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders.MetadatiBuilder;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import java.util.HashMap;
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
        //log.info("Inizio con il versamento del doc: " + Integer.toString(versamentoDocInformation.getIdDoc()));

        Map<String, Object> risultatoEVersamentiAllegati = new HashMap<>();
        //chiamata autenticazione
        //creazione del json
        //creazione dell xml
        MetadatiBuilder metadatiBuilder = new MetadatiBuilder();
        metadatiBuilder.build();
        String metadati = metadatiBuilder.toString();
        risultatoEVersamentiAllegati.put("metadati", metadati);
        //stream dei file
        //creazione del multipart
        //invio
        //lettura esito
        return risultatoEVersamentiAllegati;
    }

}
