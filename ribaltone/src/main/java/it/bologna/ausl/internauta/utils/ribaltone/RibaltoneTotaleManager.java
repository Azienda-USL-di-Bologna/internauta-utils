package it.bologna.ausl.internauta.utils.ribaltone;

import com.querydsl.jpa.impl.JPAQueryFactory;
import static it.bologna.ausl.internauta.utils.ribaltone.RibaltoneManagerUtils.unisciListeUnichePerCodiceFiscaleIdCasella;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportare;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.cache.OperationsCacheManager;
import it.bologna.ausl.internauta.utils.ribaltone.cache.RibaltoneCache;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationsManager;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneConfiguration;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.finalChecks.QueryChecks;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.CSVDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.CSVSpecificData;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.GruDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.GruSpecificData;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.FonteAggiuntaDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SourceDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReport;
import it.bologna.ausl.internauta.utils.ribaltone.utils.UsersNotifiesManager;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.data.ConfigRibaltoneView;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import it.bologna.ausl.model.entities.ribaltoneutils.RibaltoneDaLanciare;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author Top
 */
@Component
public class RibaltoneTotaleManager {

    private static final Logger log = LoggerFactory.getLogger(RibaltoneTotaleManager.class);

    @Autowired
    private RibaltoneConfiguration ribaltoneConfiguration;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepositoryFactory repositoryFactory;

    @Autowired
    private UsersNotifiesManager usersNotifiesManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @PostConstruct
    private void initTransactionTemplate() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void ribaltaWithOutUserReport(String codiceAzienda, ConfigRibaltoneView configRibaltoneView) throws RibaltoneHttpException, JacksonException {
        Operations buildedOperations = null;
        String errorDescription = null;
        try {
            RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(repositoryFactory.getEntityManager(), (String) configRibaltoneView.getFonteSelezionata());
//        SpecificData specificData = objectMapper.convertValue(ribaltoneConf.getSpecifiche(), SpecificData.class);
            DatiDaImportare validateSourceData = RibaltoneManagerUtils.getSourceDataAndValidateAndTransfer(ribaltoneConfiguration.getObjectMapper(), codiceAzienda, ribaltoneConf, repositoryFactory);
            OperationsManager operationsManager = new OperationsManager(validateSourceData, codiceAzienda, configRibaltoneView.getTolleranzaAppartenenti(), configRibaltoneView.getTolleranzaStrutture(), repositoryFactory);
            buildedOperations = operationsManager.buildOperations();
            operationsManager.isQuantitaDatiOk();
            log.info("disabilito i trigger");
            RibaltoneManagerUtils.disableTrigger(repositoryFactory);
            buildedOperations.execute(repositoryFactory, codiceAzienda);
            RibaltoneManagerUtils.updateProgressivoUltimaTrasformazione(configRibaltoneView.getFonteSelezionata(), codiceAzienda, repositoryFactory);
            fromSourceToDatiImportati(ribaltoneConf.getFonte(), repositoryFactory, codiceAzienda);
            log.info("faccio i check di conformita");
            QueryChecks.confomalsDataChecks(repositoryFactory, codiceAzienda);
            //devo abilitare dei trigger che avrebbbero rallentato troppo il ribaltone
            log.info("riabilito i trigger che ho spento");
            RibaltoneManagerUtils.enableTrigger(repositoryFactory);
        } catch (JacksonException | RibaltoneHttpException ex) {
            errorDescription = ex.getMessage();
            throw new RibaltoneHttpException(ex);
        } finally {
            usersNotifiesManager.generaAndInviaNotifiche(buildedOperations, codiceAzienda, configRibaltoneView.getIdPersoneDaNotificare(), configRibaltoneView.getMailDaNotificare(), errorDescription);
        }

    }

    public Operations ribaltaWithUserReportAndCacheOperation(
            String codiceAzienda,
            ConfigRibaltoneView configRibaltoneView,
            UserReport.UserReportType typeUserReport
    ) throws RibaltoneHttpException {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return transactionTemplate.execute(action -> {
            RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(repositoryFactory.getEntityManager(), (String) configRibaltoneView.getFonteSelezionata());
            //        SpecificData specificData = objectMapper.convertValue(ribaltoneConf.getSpecifiche(), SpecificData.class);
            //        DatiDaImportare validatedSourceData = RibaltoneManagerUtils.getSourceDataAndValidateAndTransfer(objectMapper, codiceAzienda, ribaltoneConf, repositoryFactory);

            try {
//                DatiDaImportare sourceData = getSourceData(objectMapper, codiceAzienda, ribaltoneConf, repositoryFactory);
//                DatiDaImportare datiDaImportareValidated = sourceData.validate();
//                datiDaImportareValidated.transfer();

                DatiDaImportare datiDaImportareValidated = RibaltoneManagerUtils.getSourceDataAndValidateAndTransfer(ribaltoneConfiguration.getObjectMapper(), codiceAzienda, ribaltoneConf, repositoryFactory);
                OperationsManager operationsManager = new OperationsManager(
                        datiDaImportareValidated,
                        codiceAzienda,
                        configRibaltoneView.getTolleranzaAppartenenti(),
                        configRibaltoneView.getTolleranzaStrutture(),
                        repositoryFactory);

                Operations buildedOperations = operationsManager.buildOperations();
                operationsManager.isQuantitaDatiOk();
                RibaltoneCache ribaltoneCache = RibaltoneManagerUtils.getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), repositoryFactory.getEntityManager());
                OperationsCacheManager operationsCacheManager = new OperationsCacheManager(ribaltoneCache, objectMapper);
                operationsCacheManager.dump(buildedOperations);
                return buildedOperations;
            } catch (Exception ex) {
                throw new RibaltoneHttpException(ex);
            }
        });
    }

    public void ribaltaFromCachedOperation(String codiceAzienda, String idConfiguration, Utente utenteLanciatore, List<Integer> idPersoneDaNotificare, List<String> mails) throws RibaltoneHttpException, ClassNotFoundException, JacksonException {
        String descrizioneErrore = null;
        RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(repositoryFactory.getEntityManager(), idConfiguration);
        RibaltoneCache ribaltoneCache = RibaltoneManagerUtils.getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), repositoryFactory.getEntityManager());
        OperationsCacheManager operationsCacheManager = new OperationsCacheManager(ribaltoneCache, objectMapper);
        Operations buildedOperations = operationsCacheManager.restore();
        if (buildedOperations != null) {
            log.info("disabilito i trigger");
            RibaltoneManagerUtils.disableTrigger(repositoryFactory);
            buildedOperations.execute(repositoryFactory, codiceAzienda);
            try {
                RibaltoneManagerUtils.updateProgressivoUltimaTrasformazione(idConfiguration, codiceAzienda, repositoryFactory);
                fromSourceToDatiImportati(ribaltoneConf.getFonte(), repositoryFactory, codiceAzienda);
                log.info("faccio i check di conformita");
                QueryChecks.confomalsDataChecks(repositoryFactory, codiceAzienda);
                //devo abilitare dei trigger che avrebbbero rallentato troppo il ribaltone
                log.info("riabilito i trigger che ho spento");
                RibaltoneManagerUtils.enableTrigger(repositoryFactory);
            } catch (Exception ex) {
                descrizioneErrore = ex.getMessage();
                throw new RibaltoneHttpException(descrizioneErrore, ex);
            } finally {
                usersNotifiesManager.generaAndInviaNotifiche(buildedOperations, codiceAzienda, idPersoneDaNotificare, mails, descrizioneErrore);
            }

        }

        //return buildOperations;
    }

    public Integer lanciaRibaltTree(String codiceAzienda, String idFonteSelezionata, Utente utente, String note, Integer idRibaltTree, String from, Boolean soloLocale, String ex) throws RibaltoneHttpException {
        JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
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
                ribaltoneDaLanciare.setRibaltaInternauta(!soloLocale);
                ribaltoneDaLanciare.setNote(note);
                ribaltoneDaLanciare.setIdAzienda(idAzienda);
                ribaltoneDaLanciare.setFonteRibaltone(idFonteSelezionata);
                ribaltoneDaLanciare.setDataUltimaModifica(ZonedDateTime.now());
            }
            case "ribaltaPostUserReport" -> {
                ribaltoneDaLanciare = repositoryFactory.getEntityManager().find(RibaltoneDaLanciare.class, idRibaltTree);
                ribaltoneDaLanciare.setStato("DA_LANCIARE");
                ribaltoneDaLanciare.setDataUltimaModifica(ZonedDateTime.now());
            }
            case "ribaltaAndGetUserReport" -> {
                ribaltoneDaLanciare = new RibaltoneDaLanciare();
                ribaltoneDaLanciare.setStato("LANCIATO");
                ribaltoneDaLanciare.setDataUltimaModifica(ZonedDateTime.now());
                ribaltoneDaLanciare.setRibaltaArgo(Boolean.TRUE);
                ribaltoneDaLanciare.setCodiceAzienda(codiceAzienda);
                ribaltoneDaLanciare.setIdUtente(utente);
                ribaltoneDaLanciare.setRibaltaInternauta(!soloLocale);
                ribaltoneDaLanciare.setNote(note);
                ribaltoneDaLanciare.setIdAzienda(idAzienda);
                ribaltoneDaLanciare.setFonteRibaltone(idFonteSelezionata);
            }
            case "ribaltaDeleteCache" -> {
                ribaltoneDaLanciare = repositoryFactory.getEntityManager().find(RibaltoneDaLanciare.class, idRibaltTree);
                ribaltoneDaLanciare.setStato("ANNULLATO");
                ribaltoneDaLanciare.setDataUltimaModifica(ZonedDateTime.now());
                ribaltoneDaLanciare.setGestito(Boolean.TRUE);
            }
            case "errore" -> {
                if (idRibaltTree != null) {
                    ribaltoneDaLanciare = repositoryFactory.getEntityManager().find(RibaltoneDaLanciare.class, idRibaltTree);
                } else {
                    ribaltoneDaLanciare = new RibaltoneDaLanciare();
                    ribaltoneDaLanciare.setRibaltaArgo(Boolean.TRUE);
                    ribaltoneDaLanciare.setCodiceAzienda(codiceAzienda);
                    ribaltoneDaLanciare.setIdUtente(utente);
                    ribaltoneDaLanciare.setRibaltaInternauta(!soloLocale);
                    ribaltoneDaLanciare.setNote(note);
                    ribaltoneDaLanciare.setIdAzienda(idAzienda);
                    ribaltoneDaLanciare.setFonteRibaltone(idFonteSelezionata);
                    ribaltoneDaLanciare.setDataUltimaModifica(ZonedDateTime.now());
                }
                ribaltoneDaLanciare.setStato("ERRORE");
                ribaltoneDaLanciare.setDataUltimaModifica(ZonedDateTime.now());
                ribaltoneDaLanciare.setGestito(Boolean.FALSE);
                ribaltoneDaLanciare.setLog(ex);
            }
            default -> {
                throw new RibaltoneHttpException("errore nella creazione della riga di ribaltone da lanciare");
            }
        }
        repositoryFactory.getEntityManager().persist(ribaltoneDaLanciare);
        return ribaltoneDaLanciare.getId();
    }

    public void fromSourceToDatiImportati(String fonte, RepositoryFactory repositoryFactory, String codiceAzienda) {
        //Queste funzioni sui repository le ho scritte cosi per una questione di performance
        JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
        QDatiImportatiAnagrafica qDatiImportatiAnagrafica = QDatiImportatiAnagrafica.datiImportatiAnagrafica;
        queryFactory.delete(qDatiImportatiAnagrafica).where(qDatiImportatiAnagrafica.codiceAzienda.eq(codiceAzienda)).execute();
        //repositoryFactory.getDatiImportatiAnagraficaRepository().deleteAllByCodiceAzienda(codiceAzienda);
        QDatiImportatiAppartenente qDatiImportatiAppartenente = QDatiImportatiAppartenente.datiImportatiAppartenente;
        queryFactory.delete(qDatiImportatiAppartenente).where(qDatiImportatiAppartenente.codiceAzienda.eq(codiceAzienda)).execute();
        //repositoryFactory.getDatiImportatiAppartenenteRepository().deleteAllByCodiceAzienda(codiceAzienda);
        QDatiImportatiStruttura qDatiImportatiStruttura = QDatiImportatiStruttura.datiImportatiStruttura;
        queryFactory.delete(qDatiImportatiStruttura).where(qDatiImportatiStruttura.codiceAzienda.eq(codiceAzienda)).execute();
        //repositoryFactory.getDatiImportatiStrutturaRepository().deleteAllByCodiceAzienda(codiceAzienda);
        QDatiImportatiTrasformazione qDatiImportatiTrasformazione = QDatiImportatiTrasformazione.datiImportatiTrasformazione;
        queryFactory.delete(qDatiImportatiTrasformazione).where(qDatiImportatiTrasformazione.codiceAzienda.eq(codiceAzienda)).execute();
        repositoryFactory.getEntityManager().flush();
        //repositoryFactory.getDatiImportatiTrasformazioneRepository().deleteAllByCodiceAzienda(codiceAzienda);
        log.info("fonteeeeeeeeeeeeeee " + fonte);
        switch (fonte) {
            case "GRU" -> {
                repositoryFactory.getDatiImportatiAnagraficaRepository().fromDatiDaImportareToDatiImportati(codiceAzienda);
                repositoryFactory.getDatiImportatiAppartenenteRepository().fromDatiDaImportareToDatiImportati(codiceAzienda);
                repositoryFactory.getDatiImportatiStrutturaRepository().fromDatiDaImportareToDatiImportati(codiceAzienda);
                repositoryFactory.getDatiImportatiTrasformazioneRepository().fromDatiDaImportareToDatiImportati(codiceAzienda);
                repositoryFactory.getEntityManager().flush();
            }
            case "CSV" -> {
                repositoryFactory.getDatiImportatiAnagraficaRepository().fromCSVDaImportareToDatiImportati(codiceAzienda);
                repositoryFactory.getDatiImportatiAppartenenteRepository().fromCSVDaImportareToDatiImportati(codiceAzienda);
                repositoryFactory.getDatiImportatiStrutturaRepository().fromCSVDaImportareToDatiImportati(codiceAzienda);
                repositoryFactory.getDatiImportatiTrasformazioneRepository().fromCSVDaImportareToDatiImportati(codiceAzienda);
            }
            case "ASTRA" -> {
            }
            default ->
                throw new AssertionError();
        }
        repositoryFactory.getDatiImportatiAnagraficaRepository().fromFonteAggiuntaToDatiImportati(codiceAzienda);
        repositoryFactory.getDatiImportatiAppartenenteRepository().fromFonteAggiuntaToDatiImportati(codiceAzienda);
        repositoryFactory.getDatiImportatiStrutturaRepository().fromFonteAggiuntaToDatiImportati(codiceAzienda);
        repositoryFactory.getDatiImportatiTrasformazioneRepository().fromFonteAggiuntaToDatiImportati(codiceAzienda);
        repositoryFactory.getEntityManager().flush();
    }

    private DatiDaImportare getSourceData(ObjectMapper objectMapper, String codiceAzienda, RibaltoneDataConfiguration ribaltoneConf, RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        SourceDataManager sourceDataManager;
        List<DatiDaImportareAppartenente> appartenenti;
        List<DatiDaImportareAnagrafica> anagrafiche;
        List<DatiDaImportareStruttura> strutture;
        List<DatiDaImportareTrasformazione> trasformazioni;
        Integer progressivoUltimaTrasformazione;
        // NB: in JPQL si deve usare il nome dell'entità Java, in questo caso Azienda
        Azienda idAzienda = repositoryFactory.getEntityManager().createQuery("select a from Azienda a where codice = :codice", Azienda.class)
                .setParameter("codice", codiceAzienda)
                .getSingleResult();
        switch (ribaltoneConf.getFonte()) {
            case "GRU" -> {
                GruSpecificData gruSpecificData = objectMapper.convertValue(ribaltoneConf.getSpecifiche(), GruSpecificData.class);

                if (idAzienda == null) {
                    throw new RibaltoneHttpException("impossibile trovare l'azienda corrispondente");
                } else {
                    try (GruDataManager gruDataManager = new GruDataManager(gruSpecificData, objectMapper, codiceAzienda, idAzienda.getId())) {
                        sourceDataManager = gruDataManager;
                        appartenenti = sourceDataManager.getAppartenenti();
                        anagrafiche = sourceDataManager.getAnagrafica();
                        strutture = sourceDataManager.getStrutture();
                        trasformazioni = sourceDataManager.getTrasformazioni();
                    }
                    progressivoUltimaTrasformazione = gruSpecificData.getQueryRecuperoDati().getProgressivoUltimaTrasformazione();

                }
            }
            case "CSV" -> {
                CSVSpecificData csvSpecificData = objectMapper.convertValue(ribaltoneConf.getSpecifiche(), CSVSpecificData.class);
                if (idAzienda == null) {
                    throw new RibaltoneHttpException("impossibile trovare l'azienda corrispondente");
                } else {
                    sourceDataManager = new CSVDataManager(csvSpecificData, objectMapper, codiceAzienda, idAzienda.getId(), repositoryFactory.getEntityManager());
                    appartenenti = sourceDataManager.getAppartenenti();
                    anagrafiche = sourceDataManager.getAnagrafica();
                    strutture = sourceDataManager.getStrutture();
                    trasformazioni = sourceDataManager.getTrasformazioni();
                    progressivoUltimaTrasformazione = csvSpecificData.getQueryRecuperoDati().getProgressivoUltimaTrasformazione();
                }
            }

            case "ASTRA" ->
                throw new RibaltoneHttpException("astra non ancora implementato");
            default ->
                throw new AssertionError();
        }
        FonteAggiuntaDataManager fonteAggiuntaDataManager = new FonteAggiuntaDataManager(null, objectMapper, codiceAzienda, idAzienda.getId(), repositoryFactory.getEntityManager());
        List<DatiDaImportareAppartenente> fonteAggiuntaAppartenenti = fonteAggiuntaDataManager.getAppartenenti();
        List<DatiDaImportareAnagrafica> fonteAggiuntaAnagrafica = fonteAggiuntaDataManager.getAnagrafica();
        //List<DatiDaImportareStruttura> fonteAggiuntaStrutture = fonteAggiuntaDataManager.getStrutture();
        //List<DatiDaImportareTrasformazione> fonteAggiuntaTrasformazioni = fonteAggiuntaDataManager.getTrasformazioni();
        appartenenti = unisciListeUnichePerCodiceFiscaleIdCasella(appartenenti, fonteAggiuntaAppartenenti);
        anagrafiche = mergeAnagraficheListsOverrideOnCodiceFiscale(anagrafiche, fonteAggiuntaAnagrafica);
        //strutture = mergeDatiDaImportareStruttureListsOnConflicIdCasellaExpandIntervallo(strutture, fonteAggiuntaStrutture);
        DatiDaImportare datiDaImportare = new DatiDaImportare(anagrafiche, strutture, appartenenti, trasformazioni, progressivoUltimaTrasformazione, repositoryFactory, idAzienda.getId());
        return datiDaImportare;
    }

    private static List<DatiDaImportareAnagrafica> mergeAnagraficheListsOverrideOnCodiceFiscale(
            List<DatiDaImportareAnagrafica> lista1,
            List<DatiDaImportareAnagrafica> lista2) {

        Map<String, DatiDaImportareAnagrafica> mappaPerCodiceFiscale = new HashMap<>();

        // Prima lista: inserisce gli elementi nella mappa
        for (DatiDaImportareAnagrafica item : lista1) {
            if (item.getCodiceFiscale() != null) {
                mappaPerCodiceFiscale.put(item.getCodiceFiscale(), item);
            }
        }

        // Seconda lista: inserisce (sovrascrive) gli elementi nella mappa
        for (DatiDaImportareAnagrafica item : lista2) {
            if (item.getCodiceFiscale() != null) {
                mappaPerCodiceFiscale.put(item.getCodiceFiscale(), item); // sovrascrive se esiste
            }
        }

        // Ritorna una nuova lista con i valori uniti
        return new ArrayList<>(mappaPerCodiceFiscale.values());
    }

}
