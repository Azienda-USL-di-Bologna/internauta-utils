package it.bologna.ausl.internauta.utils.ribaltone.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.ribaltone.RibaltoneManagerUtils;
import static it.bologna.ausl.internauta.utils.ribaltone.RibaltoneManagerUtils.getRibaltoneCache;
import it.bologna.ausl.internauta.utils.ribaltone.RibaltoneTotaleManager;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneCache;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.CsvImportManager;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReport;
import it.bologna.ausl.model.entities.configurazione.data.ConfigRibaltoneView;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.apache.tomcat.util.http.fileupload.IOUtils;

/**
 *
 * @author Top
 */
@RestController
@RequestMapping(value = "${ribaltone.mapping.url.root}")
public class RibaltoneRestController implements ControllerHandledExceptions {

    @Autowired
    private RibaltoneTotaleManager ribaltoneTotaleManager;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

//    @RequestMapping(value = "/cleanSourceData", method = RequestMethod.GET)
//    public DatiDaImportare cleanSourceData(
//            @RequestParam(required = true) String codiceAzienda,
//            @RequestParam(required = true) String idConfiguration) throws RibaltoneHttpException {
//        RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(entityManager, idConfiguration);
//        //momento in cui instanzio il plugin corretto
//        SourceDataManager sourceDataManager;
//        DatiDaImportare datiDaImportare = null;
//        if (datiDaImportare==null){return null;}
//        DatiDaImportare validate = datiDaImportare.validate();
//
//        datiDaImportareStrutturaRepository.deleteAll();
//        datiDaImportareTrasformazioneRepository.deleteAll();
//        datiDaImportareAppartenenteRepository.deleteAll();
//        datiDaImportareAnagraficaRepository.deleteAll();
//
//        datiDaImportareStrutturaRepository.saveAll(validate.getStruttureDaImportare());
//        datiDaImportareTrasformazioneRepository.saveAll(validate.getTrasformazioniDaImportare());
//        datiDaImportareAppartenenteRepository.saveAll(validate.getAppartenentiDaImportare());
//        datiDaImportareAnagraficaRepository.saveAll(validate.getAnagraficheDaImportare());
//        return validate;
//    }
//    @RequestMapping(value = "/checkSourceData", method = RequestMethod.GET)
//    public Object checkSourceData(
//            @RequestParam(required = true) String codiceAzienda,
//            @RequestParam(required = true) String idConfiguration) throws RibaltoneHttpException {
    ////        //facci la classe di sourceData
////        Map<String, Object> sourceData = RibaltoneManagerUtils.getSourceData(codiceAzienda, idConfiguration);
////        RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(entityManager, idConfiguration);
//////        cleanAndSaveSourceData(sourceData,ribaltoneConf);
////
////        ReportStrutture risultatoStr = ReportStrutture.check(ribaltoneConf.getCacheOperationToDo());
////        Map<String, String> risultatoApp = ReportAppartenenti.check(ribaltoneConf.getCacheOperationToDo());
////        Map<String, String> risultatoTra = ReportTrasformazioni.check(ribaltoneConf.getCacheOperationToDo());
////        writeOnRedis(report);
////        return generateReport(risultatoStr, risultatoApp, risultatoTra);
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
//    }

    /**
     *
     * @param codiceAzienda
     * @param idConfig
     * @throws RibaltoneHttpException
     */
    @RequestMapping(value = "/ribalta", method = RequestMethod.POST)
    public void ribalta(
        @RequestParam(required = true) String codiceAzienda,
        @RequestParam(required = true) ConfigRibaltoneView idConfig
    ) throws RibaltoneHttpException {
        ribaltoneTotaleManager.ribaltaWithOutUserReport(codiceAzienda, idConfig);

    }

    @RequestMapping(value = "/importaCSV", method = RequestMethod.POST)
    public void importaCSV(
        @RequestParam(required = true) String codiceAzienda,
        @RequestParam(required = true, name = "csv") MultipartFile csv,
        @RequestParam(required = true, name = "tipologia") TipologiaCsv tipologia,
        @RequestParam(required = true, name = "separatore") String separatore
    ) throws RibaltoneHttpException {
        File csvFile = null;
        try {
            // creo il csv come file temporaneo e lo cancello al termine
            csvFile = File.createTempFile("uploadCSVribaltone_", ".csv");
            csvFile.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(csvFile);) {
                try (InputStream csvIs = csv.getInputStream()) {
                    IOUtils.copy(csvIs, fos);
                }
            }
            CsvImportManager csvImportManager = new CsvImportManager(objectMapper, entityManager);
            csvImportManager.csvImportAndValidate(separatore, csvFile, tipologia, codiceAzienda);

        } catch (Exception ex) {
            throw new RibaltoneHttpException("errore nell'importazione", ex);
        } finally {
            if (csvFile != null) {
                csvFile.delete();
            }
        }
    }

    /**
     *
     * @param codiceAzienda
     * @param idConfig
     * @param typeUserReport
     * @return
     * @throws RibaltoneHttpException
     */
    @RequestMapping(value = "/ribaltaAndGetUserReport", method = RequestMethod.POST)
    public Object ribaltaAndGetUserReport(
        @RequestParam(required = true) String codiceAzienda,
        @RequestParam(required = true) ConfigRibaltoneView idConfig,
        @RequestParam(required = true) UserReport.UserReportType typeUserReport
    ) throws RibaltoneHttpException {
        return new ResponseEntity(ribaltoneTotaleManager.ribaltaWithUserReportAndCacheOperation(codiceAzienda, idConfig, typeUserReport), HttpStatus.OK);

    }

    /**
     *
     * @param codiceAzienda
     * @param idConfig
     * @throws RibaltoneHttpException
     * @throws java.lang.ClassNotFoundException
     */
    @RequestMapping(value = "/ribaltaPostUserReport", method = RequestMethod.POST)
    public Object ribaltaPostUserReport(
        @RequestParam(required = true) String codiceAzienda,
        @RequestParam(required = true) String idConfig
    ) throws RibaltoneHttpException, ClassNotFoundException, JsonProcessingException {
        return new ResponseEntity(ribaltoneTotaleManager.ribaltaFromCachedOperation(codiceAzienda, idConfig), HttpStatus.OK);
    }

    @RequestMapping(value = "/ribaltaDeleteCache", method = RequestMethod.POST)
    public void ribaltaDeleteCache(
        @RequestParam(required = true) String codiceAzienda,
        @RequestParam(required = true) String idConfiguration
    ) throws RibaltoneHttpException {
        RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(entityManager, idConfiguration);
        RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), entityManager);
        ribaltoneCache.cleanCache();
    }
}
