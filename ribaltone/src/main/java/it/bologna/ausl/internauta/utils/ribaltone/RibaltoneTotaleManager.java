package it.bologna.ausl.internauta.utils.ribaltone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.data.ConfigRibaltoneView;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import it.bologna.ausl.model.entities.ribaltoneutils.RibaltoneDaLanciare;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
public class RibaltoneTotaleManager {

    private static final Logger log = LoggerFactory.getLogger(RibaltoneTotaleManager.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private RibaltoneConfiguration ribaltoneConfiguration;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepositoryFactory repositoryFactory;

    public void ribaltaWithOutUserReport(String codiceAzienda, ConfigRibaltoneView configRibaltoneView) throws RibaltoneHttpException {
        RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(entityManager, (String) configRibaltoneView.getFonteSelezionata());
        SpecificData specificData = objectMapper.convertValue(ribaltoneConf.getSpecifiche(), SpecificData.class);
        DatiDaImportare validateSourceData = RibaltoneManagerUtils.getAndValidateSourceData(ribaltoneConfiguration.getObjectMapper(), codiceAzienda, ribaltoneConf, repositoryFactory);
        OperationsManager operationsManager = new OperationsManager(validateSourceData, codiceAzienda, configRibaltoneView.getTolleranzaAppartenenti(), configRibaltoneView.getTolleranzaStrutture(), repositoryFactory);
        Operations buildOperations = operationsManager.buildOperations();
        operationsManager.isQuantitaDatiOk();
        buildOperations.execute(repositoryFactory, codiceAzienda);
        fromSouceToDatiImportati(validateSourceData, repositoryFactory);
    }

//    private void ribaltaTutto(DatiDaImportare datiDaImportareValidated, String codiceAzienda, Integer tolleranzaAppartenenti, Integer tolleranzaStrutture) {
//        OperationsManager operationsManager = new OperationsManager(datiDaImportareValidated, codiceAzienda, tolleranzaAppartenenti, tolleranzaStrutture, repositoryFactory);
//        Operations operations = operationsManager.buildOperations();
//    }
//
//    private void ribaltaStrutture(Operations struttureCheckedData) {
//        spegniStrutture(struttureCheckedData);
//        accendiStrutture(struttureCheckedData);
//    }
//
//    private void ribaltaTrasformazioni(Operations struttureCheckedData, Operations trasformazioniCheckedData) {
//        capisciTrasformazioniCapibili(struttureCheckedData, trasformazioniCheckedData);
//        spostaStrutture(struttureCheckedData, trasformazioniCheckedData);
//        spegniPermessiStruttureChiuse(struttureCheckedData);
//    }
//
//    private void spegniStrutture(Operations struttureCheckedData) {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
//    }
//
//    private void accendiStrutture(Operations struttureCheckedData) {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
//    }
//
//    private void capisciTrasformazioniCapibili(Operations struttureCheckedData, Operations trasformazioniCheckedData) {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
//    }
//
//    private void spostaStrutture(Operations struttureCheckedData, Operations trasformazioniCheckedData) {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
//    }
//
//    private void spegniPermessiStruttureChiuse(Operations struttureCheckedData) {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
//    }
    public Object ribaltaWithUserReportAndCacheOperation(
        String codiceAzienda,
        ConfigRibaltoneView configRibaltoneView,
        UserReport.UserReportType typeUserReport
    ) throws RibaltoneHttpException {
        RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(entityManager, (String) configRibaltoneView.getFonteSelezionata());
//        SpecificData specificData = objectMapper.convertValue(ribaltoneConf.getSpecifiche(), SpecificData.class);
        DatiDaImportare validatedSourceData = RibaltoneManagerUtils.getAndValidateSourceData(objectMapper, codiceAzienda, ribaltoneConf, repositoryFactory);

        OperationsManager operationsManager = new OperationsManager(
            validatedSourceData,
            codiceAzienda,
            configRibaltoneView.getTolleranzaAppartenenti(),
            configRibaltoneView.getTolleranzaStrutture(),
            repositoryFactory);

        Operations buildOperations = operationsManager.buildOperations();
        operationsManager.isQuantitaDatiOk();
        RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), entityManager);
        OperationsCacheManager operationsCacheManager = new OperationsCacheManager(ribaltoneCache, objectMapper);
        operationsCacheManager.dump(buildOperations);
//        UserReportManager userReportManager = buildOperations.generateUserReport(typeUserReport);
//        return userReportManager.get();
        return buildOperations;
    }

    public Operations ribaltaFromCachedOperation(String codiceAzienda, String idConfiguration) throws RibaltoneHttpException, ClassNotFoundException, JsonProcessingException {
        RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(entityManager, idConfiguration);
//        SpecificData specificData = objectMapper.convertValue(ribaltoneConf.getSpecifiche(), SpecificData.class);
        RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), entityManager);
        OperationsCacheManager operationsCacheManager = new OperationsCacheManager(ribaltoneCache, objectMapper);
        Operations buildOperations = operationsCacheManager.restore();
        buildOperations.execute(repositoryFactory, codiceAzienda);
        DatiDaImportare validatedSourceData = RibaltoneManagerUtils.getAndValidateSourceData(objectMapper, codiceAzienda, ribaltoneConf, repositoryFactory);
        fromSouceToDatiImportati(validatedSourceData, repositoryFactory);
        return buildOperations;
    }

    public Integer lanciaRibaltTree(String codiceAzienda, String idFonteSelezionata, Utente utente, String note, Integer idRibaltTree, String from) throws RibaltoneHttpException {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QAzienda qAzienda = QAzienda.azienda;
        Azienda idAzienda = queryFactory.select(qAzienda).from(qAzienda).where(qAzienda.codice.eq(codiceAzienda)).fetchOne();
        RibaltoneDaLanciare ribaltoneDaLanciare = null;
        switch (from) {
            case "ribalta" -> {
                ribaltoneDaLanciare = new RibaltoneDaLanciare();
                ribaltoneDaLanciare.setStato("DA_LANCIARE");
                ribaltoneDaLanciare.setRibaltaArgo(Boolean.TRUE);
                ribaltoneDaLanciare.setCodiceAzienda(codiceAzienda);
                ribaltoneDaLanciare.setIdUtente(utente);
                ribaltoneDaLanciare.setRibaltaInternauta(Boolean.TRUE);
                ribaltoneDaLanciare.setNote(note);
                ribaltoneDaLanciare.setIdAzienda(idAzienda);
                ribaltoneDaLanciare.setFonteRibaltone(idFonteSelezionata);
            }
            case "ribaltaPostUserReport" -> {
                ribaltoneDaLanciare = entityManager.find(RibaltoneDaLanciare.class, idRibaltTree);
                ribaltoneDaLanciare.setStato("DA_LANCIARE");
            }
            case "ribaltaAndGetUserReport" -> {
                ribaltoneDaLanciare = new RibaltoneDaLanciare();
                ribaltoneDaLanciare.setStato("LANCIATO");
                ribaltoneDaLanciare.setRibaltaArgo(Boolean.TRUE);
                ribaltoneDaLanciare.setCodiceAzienda(codiceAzienda);
                ribaltoneDaLanciare.setIdUtente(utente);
                ribaltoneDaLanciare.setRibaltaInternauta(Boolean.TRUE);
                ribaltoneDaLanciare.setNote(note);
                ribaltoneDaLanciare.setIdAzienda(idAzienda);
                ribaltoneDaLanciare.setFonteRibaltone(idFonteSelezionata);
            }
            case "ribaltaDeleteCache" -> {
                ribaltoneDaLanciare = entityManager.find(RibaltoneDaLanciare.class, idRibaltTree);
                ribaltoneDaLanciare.setStato("ANNULLATO");
            }
            default -> {
                throw new RibaltoneHttpException("errore nella creazione della riga di ribaltone da lanciare");
            }
        }

        entityManager.persist(ribaltoneDaLanciare);
        return ribaltoneDaLanciare.getId();
    }

    public void fromSouceToDatiImportati(DatiDaImportare validateSourceData, RepositoryFactory repositoryFactory) {
        //salvo le anagrafiche
        repositoryFactory.getDatiImportatiAnagraficaRepository().deleteAll();
        for (DatiDaImportareAnagrafica anagraficadaimportare : validateSourceData.getAnagraficheDaImportare()) {
            repositoryFactory.getEntityManager().persist(anagraficadaimportare.buildDatiImportatiAnagrafica());
        }
        repositoryFactory.getDatiImportatiAppartenenteRepository().deleteAll();
        for (DatiDaImportareAppartenente appartenente : validateSourceData.getAppartenentiDaImportare()) {
            repositoryFactory.getEntityManager().persist(appartenente.buildDatiImportatiAppartenente());
        }
        repositoryFactory.getDatiImportatiStrutturaRepository().deleteAll();
        for (DatiDaImportareStruttura struttura : validateSourceData.getStruttureDaImportare()) {
            repositoryFactory.getEntityManager().persist(struttura.buildDatiImportatiStruttura());
        }
        repositoryFactory.getDatiImportatiTrasformazioneRepository().deleteAll();
        for (DatiDaImportareTrasformazione trasformazione : validateSourceData.getTrasformazioniDaImportare()) {
            repositoryFactory.getEntityManager().persist(trasformazione.buildDatiImportatiTrasformazione());
        }

    }

}
