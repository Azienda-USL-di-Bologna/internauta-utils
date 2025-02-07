package it.bologna.ausl.internauta.utils.ribaltone.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import it.bologna.ausl.internauta.utils.ribaltone.RibaltoneManagerUtils;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SourceDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneConfiguration;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.GruDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.GruSpecificData;
import it.bologna.ausl.internauta.utils.ribaltone.repository.DatiDaImportareAppartenenteRepository;
import it.bologna.ausl.internauta.utils.ribaltone.repository.DatiDaImportareStrutturaRepository;
import it.bologna.ausl.internauta.utils.ribaltone.repository.DatiDaImportareAnagraficaRepository;
import it.bologna.ausl.internauta.utils.ribaltone.repository.DatiDaImportareTrasformazioneRepository;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportare;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Top
 */
@RestController
@RequestMapping(value = "${ribaltone.mapping.url.root}")
public class RibaltoneRestController implements ControllerHandledExceptions {

    @Autowired
    private RibaltoneConfiguration ribaltoneConfiguration;

    @Autowired
    private DatiDaImportareAppartenenteRepository datiDaImportareAppartenenteRepository;

    @Autowired
    private DatiDaImportareStrutturaRepository datiDaImportareStrutturaRepository;

    @Autowired
    private DatiDaImportareAnagraficaRepository datiDaImportareAnagraficaRepository;

    @Autowired
    private DatiDaImportareTrasformazioneRepository datiDaImportareTrasformazioneRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @RequestMapping(value = "/cleanSourceData", method = RequestMethod.GET)
    public DatiDaImportare cleanSourceData(
            @RequestParam(required = true) String codiceAzienda,
            @RequestParam(required = true) String idConfiguration) throws RibaltoneHttpException {
        RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(entityManager, idConfiguration);
        //momento in cui instanzio il plugin corretto
        SourceDataManager sourceDataManager;
        DatiDaImportare datiDaImportare = null;
        switch (ribaltoneConf.getFonte()) {
            case "GRU":
                GruSpecificData gruSpecificData = ribaltoneConfiguration.getObjectMapper().convertValue(ribaltoneConf.getSpecifiche(), GruSpecificData.class);
                Map<String, String> queryRecuperoDati = ribaltoneConfiguration.getObjectMapper()
                        .convertValue(ribaltoneConf.getSpecifiche().get("queryRecuperoDati"), new TypeReference<Map<String, String>>() {
                        });
                gruSpecificData.getQueryRecuperoDati().setQueryAnagrafiche(queryRecuperoDati.get("anagrafiche"));
                gruSpecificData.getQueryRecuperoDati().setQueryResponsabili(queryRecuperoDati.get("responsabili"));
                gruSpecificData.getQueryRecuperoDati().setQueryAppartenenti(queryRecuperoDati.get("appartenenti"));
                gruSpecificData.getQueryRecuperoDati().setQueryStrutture(queryRecuperoDati.get("strutture"));
                gruSpecificData.getQueryRecuperoDati().setQueryTrasformazioni(queryRecuperoDati.get("trasformazioni"));
                sourceDataManager = new GruDataManager(gruSpecificData, ribaltoneConfiguration.getObjectMapper(), codiceAzienda);
                List<DatiDaImportareAppartenente> appartenenti = sourceDataManager.getAppartenenti();
                List<DatiDaImportareAnagrafica> anagrafiche = sourceDataManager.getAnagrafica();
                List<DatiDaImportareStruttura> strutture = sourceDataManager.getStrutture();
                List<DatiDaImportareTrasformazione> trasformazioni = sourceDataManager.getTrasformazioni();

                datiDaImportare = new DatiDaImportare(anagrafiche, strutture, appartenenti, trasformazioni,gruSpecificData.getProgressivo_ultima_trasformazione());
                //return datiDaImportare;

            default:
//                throw new AssertionError();
        }
        if (datiDaImportare==null){return null;}
        DatiDaImportare validate = datiDaImportare.validate();
        
        datiDaImportareStrutturaRepository.deleteAll();
        datiDaImportareTrasformazioneRepository.deleteAll();
        datiDaImportareAppartenenteRepository.deleteAll();
        datiDaImportareAnagraficaRepository.deleteAll();
        
        datiDaImportareStrutturaRepository.saveAll(validate.getStruttureDaImportare());
        datiDaImportareTrasformazioneRepository.saveAll(validate.getTrasformazioniDaImportare());
        datiDaImportareAppartenenteRepository.saveAll(validate.getAppartenentiDaImportare());
        datiDaImportareAnagraficaRepository.saveAll(validate.getAnagraficheDaImportare());
        return validate;
    }

    @RequestMapping(value = "/checkSourceData", method = RequestMethod.GET)
    public Object checkSourceData(
            @RequestParam(required = true) String codiceAzienda,
            @RequestParam(required = true) String idConfiguration) throws RibaltoneHttpException {
//        //facci la classe di sourceData
//        Map<String, Object> sourceData = RibaltoneManagerUtils.getSourceData(codiceAzienda, idConfiguration);
//        RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(entityManager, idConfiguration);
////        cleanAndSaveSourceData(sourceData,ribaltoneConf);
//
//        ReportStrutture risultatoStr = ReportStrutture.check(ribaltoneConf.getCacheOperationToDo());
//        Map<String, String> risultatoApp = ReportAppartenenti.check(ribaltoneConf.getCacheOperationToDo());
//        Map<String, String> risultatoTra = ReportTrasformazioni.check(ribaltoneConf.getCacheOperationToDo());
//        writeOnRedis(report);
//        return generateReport(risultatoStr, risultatoApp, risultatoTra);
 throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @RequestMapping(value = "/ribalta", method = RequestMethod.GET)
    public void ribalta(
            @RequestParam(required = true) String codiceAzienda,
            @RequestParam(required = true) Boolean prendiDallaCache,
            @RequestParam(required = true) Boolean salvaNellaCache
            
    ) {
        
         
    }

    private void cleanAndSaveOnDBSourceData(Map<String, Object> sourceData) {
         throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
