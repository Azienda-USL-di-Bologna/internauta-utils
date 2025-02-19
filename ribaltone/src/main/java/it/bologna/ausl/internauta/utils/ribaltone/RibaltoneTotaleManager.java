package it.bologna.ausl.internauta.utils.ribaltone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import static it.bologna.ausl.internauta.utils.ribaltone.RibaltoneManagerUtils.getRibaltoneCache;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportare;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.cache.OperationsCacheManager;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneCache;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationsManager;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneConfiguration;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SpecificData;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReport;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
public class RibaltoneTotaleManager {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private static RibaltoneConfiguration ribaltoneConfiguration;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepositoryFactory repositoryFactory;

    public void ribaltaWithOutUserReport(String codiceAzienda, String idConfiguration) throws RibaltoneHttpException {
        RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(entityManager, idConfiguration);
        SpecificData specificData = objectMapper.convertValue(ribaltoneConf.getSpecifiche(), SpecificData.class);
        DatiDaImportare validateSourceData = RibaltoneManagerUtils.validateSourceData(ribaltoneConfiguration.getObjectMapper(), codiceAzienda, ribaltoneConf, repositoryFactory);
        OperationsManager operationsManager = new OperationsManager(validateSourceData, codiceAzienda, specificData.getTolleranzaAppartenenti(), specificData.getTolleranzaStrutture(), repositoryFactory);
        Operations buildOperations = operationsManager.buildOperations();
        operationsManager.isQuantitaDatiOk();
        buildOperations.execute();
    }

    private void ribaltaTutto(DatiDaImportare datiDaImportareValidated, String codiceAzienda, Integer tolleranzaAppartenenti, Integer tolleranzaStrutture) {
        OperationsManager operationsManager = new OperationsManager(datiDaImportareValidated, codiceAzienda, tolleranzaAppartenenti, tolleranzaStrutture, repositoryFactory);
        Operations operations = operationsManager.buildOperations();
    }

    private void ribaltaStrutture(Operations struttureCheckedData) {
        spegniStrutture(struttureCheckedData);
        accendiStrutture(struttureCheckedData);
    }

    private void ribaltaTrasformazioni(Operations struttureCheckedData, Operations trasformazioniCheckedData) {
        capisciTrasformazioniCapibili(struttureCheckedData, trasformazioniCheckedData);
        spostaStrutture(struttureCheckedData, trasformazioniCheckedData);
        spegniPermessiStruttureChiuse(struttureCheckedData);
    }

    private void spegniStrutture(Operations struttureCheckedData) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void accendiStrutture(Operations struttureCheckedData) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void capisciTrasformazioniCapibili(Operations struttureCheckedData, Operations trasformazioniCheckedData) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void spostaStrutture(Operations struttureCheckedData, Operations trasformazioniCheckedData) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void spegniPermessiStruttureChiuse(Operations struttureCheckedData) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void unificaStruttureDaUnificare() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public Object ribaltaWithUserReportAndCacheOperation(String codiceAzienda, String idConfiguration, UserReport.UserReportType typeUserReport) throws RibaltoneHttpException {
        RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(entityManager, idConfiguration);
        SpecificData specificData = objectMapper.convertValue(ribaltoneConf.getSpecifiche(), SpecificData.class);
        DatiDaImportare validateSourceData = RibaltoneManagerUtils.validateSourceData(objectMapper, codiceAzienda, ribaltoneConf, repositoryFactory);
        
        OperationsManager operationsManager = new OperationsManager(
                validateSourceData,
                codiceAzienda,
                specificData.getTolleranzaAppartenenti(),
                specificData.getTolleranzaStrutture(),
                repositoryFactory);
        
        Operations buildOperations = operationsManager.buildOperations();
        operationsManager.isQuantitaDatiOk();
        RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), entityManager);
        OperationsCacheManager operationsCacheManager = new OperationsCacheManager(ribaltoneCache, objectMapper);

        operationsCacheManager.dump(buildOperations);
//        UserReportManager userReportManager = buildOperations.generateUserReport(typeUserReport);
//        return userReportManager.get();
        return null;
    }

    public Operations ribaltaFromCachedOperation(String codiceAzienda, String idConfiguration) throws RibaltoneHttpException, ClassNotFoundException,JsonProcessingException {
        RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(entityManager, idConfiguration);
        SpecificData specificData = objectMapper.convertValue(ribaltoneConf.getSpecifiche(), SpecificData.class);
        RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), entityManager);
        OperationsCacheManager operationsCacheManager = new OperationsCacheManager(ribaltoneCache, objectMapper);
        Operations buildOperations = operationsCacheManager.restore();
        buildOperations.execute();
        return buildOperations;
    }

}
