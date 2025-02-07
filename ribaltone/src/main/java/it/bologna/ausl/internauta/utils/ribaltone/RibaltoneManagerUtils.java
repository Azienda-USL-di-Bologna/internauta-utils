package it.bologna.ausl.internauta.utils.ribaltone;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportare;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.cache.OperationsCacheManager;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneCache;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReport;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReportManager;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationsManager;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.GruDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.GruSpecificData;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SourceDataManager;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Top
 */
public class RibaltoneManagerUtils {

    public static UserReportManager importDataAndGenerateUserReportWithCache(ObjectMapper objectMapper, EntityManager entityManager, String codiceAzienda, String idConfiguration) throws RibaltoneHttpException {
        RibaltoneDataConfiguration ribaltoneConf = getRibaltoneConf(entityManager, idConfiguration);
        RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper,ribaltoneConf.getCacheConfig());
        OperationsCacheManager operationsCacheManager = new OperationsCacheManager(ribaltoneCache, objectMapper);
        
        DatiDaImportare datiDaImportareValidated = validateSourceData(objectMapper, codiceAzienda, ribaltoneConf);
        OperationsManager operationsManager = new OperationsManager(datiDaImportareValidated, codiceAzienda);
        Operations operations = operationsManager.buildOperations();
        
        operationsCacheManager.dump(operations);
        //TODO: aggiungere il tempo di cache
        //TODO: fare il test fino al dump
        //TODO: fare il restore e testare
        
        return operations.generateUserReport(UserReport.UserReportType.HTML);
    }

    public static RibaltoneDataConfiguration getRibaltoneConf(EntityManager entityManager, String idConfiguration) throws RibaltoneHttpException {
        RibaltoneDataConfiguration ribaltoneConf = entityManager.find(RibaltoneDataConfiguration.class, idConfiguration);
        if (ribaltoneConf == null) {
            throw new RibaltoneHttpException("parametro ribaltoneConf non trovato Questo non puo accadere!");
        }
        return ribaltoneConf;
    }
    
    public static RibaltoneCache getRibaltoneCache(ObjectMapper objectMapper, HashMap<String, Object> cacheConfig) throws RibaltoneHttpException {
        RibaltoneCache ribaltoneCache = RibaltoneCache.build(cacheConfig, objectMapper);
        if (ribaltoneCache == null) {
            throw new RibaltoneHttpException("parametro ribaltoneConf non trovato Questo non puo accadere!");
        }
        return ribaltoneCache;
    }

    public static DatiDaImportare validateSourceData(ObjectMapper objectMapper, String codiceAzienda, RibaltoneDataConfiguration ribaltoneConf) throws RibaltoneHttpException {
        //recupero i dati da dove dice la conf
        DatiDaImportare sourceData = getSourceData(objectMapper, codiceAzienda, ribaltoneConf);
        DatiDaImportare datiDaImportareValidated = sourceData.validate();
        return datiDaImportareValidated;
    }

    private static DatiDaImportare getSourceData(ObjectMapper objectMapper, String codiceAzienda, RibaltoneDataConfiguration ribaltoneConf) throws RibaltoneHttpException {
        SourceDataManager sourceDataManager;
        switch (ribaltoneConf.getFonte()) {
            case "GRU":
                GruSpecificData gruSpecificData = objectMapper.convertValue(ribaltoneConf.getSpecifiche(), GruSpecificData.class);
                Map<String, String> queryRecuperoDati = objectMapper
                        .convertValue(ribaltoneConf.getSpecifiche().get("queryRecuperoDati"), new TypeReference<Map<String, String>>() {
                        });
                //capire cosa succede e perche sono costretto a fare questa cosa abberrante
                gruSpecificData.getQueryRecuperoDati().setQueryAnagrafiche(queryRecuperoDati.get("anagrafiche"));
                gruSpecificData.getQueryRecuperoDati().setQueryResponsabili(queryRecuperoDati.get("responsabili"));
                gruSpecificData.getQueryRecuperoDati().setQueryAppartenenti(queryRecuperoDati.get("appartenenti"));
                gruSpecificData.getQueryRecuperoDati().setQueryStrutture(queryRecuperoDati.get("strutture"));
                gruSpecificData.getQueryRecuperoDati().setQueryTrasformazioni(queryRecuperoDati.get("trasformazioni"));
                //fine aberrazione

                sourceDataManager = new GruDataManager(gruSpecificData, objectMapper, codiceAzienda);
                List<DatiDaImportareAppartenente> appartenenti = sourceDataManager.getAppartenenti();
                List<DatiDaImportareAnagrafica> anagrafiche = sourceDataManager.getAnagrafica();
                List<DatiDaImportareStruttura> strutture = sourceDataManager.getStrutture();
                List<DatiDaImportareTrasformazione> trasformazioni = sourceDataManager.getTrasformazioni();

                DatiDaImportare datiDaImportare = new DatiDaImportare(anagrafiche, strutture, appartenenti, trasformazioni, gruSpecificData.getProgressivo_ultima_trasformazione());
                return datiDaImportare;

            case "CSV":
                throw new RibaltoneHttpException("csv non ancora implementato");

            case "ASTRA":
                throw new RibaltoneHttpException("astra non ancora implementato");
            default:
                throw new AssertionError();
        }
    }

    public static void accendiSottoResponsabili(Operations operationAppartenenti) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public static void ribaltaAppartenenti(Operations appartenentiChekedData, Operations anagraficheCheckedData) {
        accendiPersoneNuove(appartenentiChekedData, anagraficheCheckedData);
        accendiUtentiNuovi(appartenentiChekedData, anagraficheCheckedData);
        accendiUtentiStruttura(appartenentiChekedData);
        accendiPermessiUtenti(appartenentiChekedData);
        spegniUtentiStruttura(appartenentiChekedData);
        spegniPermessiVeicolati(appartenentiChekedData);
        spegniUtentiSenzaAfferenza();
        spegniPermessiUtentiMorti();
        spegniPersoneSenzaUtenti();
        spegniPermessiPersoneMorte();
    }

    private static void accendiPersoneNuove(Operations appartenentiChekedData, Operations anagraficheCheckedData) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private static void accendiUtentiNuovi(Operations appartenentiChekedData, Operations anagraficheCheckedData) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private static void accendiUtentiStruttura(Operations appartenentiChekedData) {
        RibaltoneManagerUtils.accendiSottoResponsabili(appartenentiChekedData);
        accendiAltriUtenti();
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private static void spegniUtentiStruttura(Operations appartenentiChekedData) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private static void spegniPermessiVeicolati(Operations appartenentiChekedData) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private static void spegniUtentiSenzaAfferenza() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private static void spegniPermessiUtentiMorti() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private static void spegniPersoneSenzaUtenti() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private static void spegniPermessiPersoneMorte() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private static void accendiAltriUtenti() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private static void accendiPermessiUtenti(Operations appartenentiChekedData) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
