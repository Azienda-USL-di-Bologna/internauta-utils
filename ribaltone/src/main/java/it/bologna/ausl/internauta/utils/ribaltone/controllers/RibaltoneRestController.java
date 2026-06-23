package it.bologna.ausl.internauta.utils.ribaltone.controllers;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.internauta.utils.ribaltone.RibaltoneManagerUtils;
import static it.bologna.ausl.internauta.utils.ribaltone.RibaltoneManagerUtils.getRibaltoneCache;
import it.bologna.ausl.internauta.utils.ribaltone.RibaltoneTotaleManager;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.cache.RibaltoneCache;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.CsvImportManager;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReport;
import it.bologna.ausl.internauta.utils.ribaltone.utils.RibaltoneUtils;
import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QAfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.configurazione.data.ConfigRibaltoneView;
import it.bologna.ausl.model.entities.ribaltonedati.QCSVDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.QCSVDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.QCSVDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.QCSVDaImportareTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.annotation.RequestBody;
import it.bologna.ausl.internauta.utils.authorizationutils.session.AuthenticatedSessionData;
import it.bologna.ausl.internauta.utils.authorizationutils.session.AuthenticatedSessionDataBuilder;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv.ANAGRAFICHE;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv.APPARTENENTI;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv.STRUTTURE;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv.TRASFORMAZIONI;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.cache.utils.CacheUtils;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneConfiguration;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationsUtils;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.bologna.ausl.model.entities.baborg.QRuolo;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import static it.bologna.ausl.model.entities.baborg.StrutturaUnificata.TipoUnificazione.FUSIONE;
import static it.bologna.ausl.model.entities.baborg.StrutturaUnificata.TipoUnificazione.REPLICA;
import it.bologna.ausl.model.entities.ribaltonedati.ImportazioniOrganigramma;
import it.bologna.ausl.model.entities.ribaltonedati.QImportazioniOrganigramma;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.StaleObjectStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 *
 * @author Top
 */
@RestController
@RequestMapping(value = "${ribaltone.mapping.url.root}")
public class RibaltoneRestController implements ControllerHandledExceptions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RibaltoneRestController.class);

    @Autowired
    private RibaltoneTotaleManager ribaltoneTotaleManager;

    @Autowired
    private PermissionManager permissionManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @Autowired
    private RepositoryFactory repositoryFactory;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @PostConstruct
    private void initTransactionTemplate() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Autowired
    private RibaltoneConfiguration ribaltoneConfiguration;

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
     * @param configRibaltoneView
     * @throws RibaltoneHttpException
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     */
    @RequestMapping(value = "/ribalta", method = RequestMethod.POST)
    public void ribalta(
            @RequestParam(required = true) String codiceAzienda,
            @RequestBody(required = true) ConfigRibaltoneView configRibaltoneView
    ) throws RibaltoneHttpException, JacksonException {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        if (!CacheUtils.isRibaltoneInCorsoFromCache(configRibaltoneView.getFonteSelezionata(), repositoryFactory, objectMapper, transactionTemplate)) {
            AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
            try {
                transactionTemplate.executeWithoutResult(action -> {
                    Utente realUser = authenticatedUserProperties.getRealUser() != null ? authenticatedUserProperties.getRealUser() : authenticatedUserProperties.getUser();
                    realUser = repositoryFactory.getEntityManager().find(Utente.class, realUser.getId());
                    try {
                        CacheUtils.setRibaltoneCacheInCorso(configRibaltoneView.getFonteSelezionata(), realUser, repositoryFactory, objectMapper, transactionTemplate);
                        ribaltoneTotaleManager.ribaltaWithOutUserReport(codiceAzienda, configRibaltoneView);
                        ribaltoneTotaleManager.lanciaRibaltTree(codiceAzienda, configRibaltoneView.getFonteSelezionata(), realUser, configRibaltoneView.getNote(), null, "ribalta", false, null);
                    } catch (Exception ex) {
                        if (ex instanceof RibaltoneHttpException ribaltoneHttpException) {
                            throw ribaltoneHttpException;
                        } else {
                            throw new RibaltoneHttpException(ex);
                        }
                    }
                });
            } catch (RibaltoneHttpException ex) {
                transactionTemplate.executeWithoutResult(action -> {
                    Utente realUser = authenticatedUserProperties.getRealUser() != null ? authenticatedUserProperties.getRealUser() : authenticatedUserProperties.getUser();
                    realUser = repositoryFactory.getEntityManager().find(Utente.class, realUser.getId());
                    ribaltoneTotaleManager.lanciaRibaltTree(codiceAzienda, configRibaltoneView.getFonteSelezionata(), realUser, configRibaltoneView.getNote(), null, "errore", false, null);
                });
//                    try {
                CacheUtils.setRibaltoneCacheFinito(configRibaltoneView.getFonteSelezionata(), repositoryFactory, objectMapper, transactionTemplate);
//                    } catch (RibaltoneHttpException | JsonProcessingException e) {
                throw new RibaltoneHttpException(ex);
//                    }
//                });
            }
        } else {
            throw new RibaltoneHttpException("non puoi lanicare il ribaltone perche è gia in corso");
        }
    }

    @RequestMapping(value = "downloadCSVUploaded", method = RequestMethod.GET)
    public void downloadCSVUploaded(
            @RequestParam("id") Integer id,
            HttpServletResponse response,
            HttpServletRequest request) throws MinIOWrapperException {
        ImportazioniOrganigramma importazioniOrganigramma = repositoryFactory.getEntityManager().find(ImportazioniOrganigramma.class, id);
        if (importazioniOrganigramma != null) {
            InputStream fileIS = ribaltoneConfiguration.getMinIOWrapper().getByFileId(importazioniOrganigramma.getPath_csv_error());
            try {
                StreamUtils.copy(fileIS, response.getOutputStream());
            } catch (IOException ex) {
                LOGGER.error("errore nel copiare il file sull'ouput stream");
                throw new RibaltoneHttpException("errore nel copiare il file sull'ouput stream", ex);
            }
        }

    }

//    @Transactional(rollbackOn = Throwable.class)
    @RequestMapping(value = "/importaCSV", method = RequestMethod.POST)
    public ResponseEntity<String> importaCSV(
            @RequestParam(required = true, name = "codiceAzienda") String codiceAzienda,
            @RequestBody(required = true) MultipartFile csv,
            @RequestParam(required = true, name = "tipologia") TipologiaCsv tipologia,
            @RequestParam(required = true, name = "separatore") String separatore,
            @RequestParam(required = true, name = "idSelectedConfiguration") String idSelectedConfiguration
    ) throws RibaltoneHttpException, JacksonException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente utente = authenticatedUserProperties.getRealUser() != null
                ? authenticatedUserProperties.getRealUser()
                : authenticatedUserProperties.getUser();

        // Verifica se c'è già un'importazione in corso
        if (CacheUtils.isImportazioneCSVInCorsoFromCache(idSelectedConfiguration, repositoryFactory, objectMapper, transactionTemplate)) {
            RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(
                    repositoryFactory.getEntityManager(), idSelectedConfiguration);
            RibaltoneCache ribaltoneCache = getRibaltoneCache(
                    objectMapper, ribaltoneConf.getCacheConfig(), repositoryFactory.getEntityManager());
            Utente user = repositoryFactory.getEntityManager().find(Utente.class, ribaltoneCache.getIdUserImportingCSV());
            return ResponseEntity.status(HttpStatus.IM_USED).body("Importazione già in corso da parte dell'utente: " + user.getIdPersona().getDescrizione());
        }

        Integer idImportazioneOrganigramma = null;

        try {
            // Segna l'importazione come in corso
            CacheUtils.setImportazioneCSVInCorso(idSelectedConfiguration, utente,
                    repositoryFactory, objectMapper, transactionTemplate);

            // Salva il file temporaneamente - ORA È FINAL
            final File csvFile = File.createTempFile("uploadCSVribaltone_" + csv.getOriginalFilename() + "_", "");
            csvFile.deleteOnExit();

            try (InputStream csvIs = csv.getInputStream(); FileOutputStream fos = new FileOutputStream(csvFile)) {
                IOUtils.copy(csvIs, fos);
            }

            // Carica il file su MinIO
            String path = "/ribaltone/" + codiceAzienda + "/";
            MinIOWrapperFileInfo minIoFileInfo;

            try {
                // Salva il fileId del CSV su MinIO
                minIoFileInfo = ribaltoneConfiguration.getMinIOWrapper().put(csvFile, codiceAzienda, path, csv.getOriginalFilename(), null, false);

                // VERIFICA IMPORTANTE: controlla che il file sia stato caricato correttamente
                if (minIoFileInfo == null || minIoFileInfo.getFileId() == null) {
                    throw new RibaltoneHttpException("Caricamento su MinIO fallito: fileId nullo");
                }

                LOGGER.info("File CSV caricato su MinIO con ID: {}", minIoFileInfo.getFileId());

            } catch (MinIOWrapperException | FileNotFoundException ex) {
                LOGGER.error("Errore nel caricamento del file CSV su MinIO", ex);
                throw new RibaltoneHttpException("Errore caricamento su MinIO: " + ex.getMessage(), ex);
            }

            // Crea le variabili final per l'uso nelle lambda
            final String fileId = minIoFileInfo.getFileId();
            final Integer idUtente = utente.getId();

            // Crea il record di importazione in una nuova transazione
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            idImportazioneOrganigramma = transactionTemplate.execute(action -> {
                JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
                QUtente qUtente = QUtente.utente;
                QAzienda qAzienda = QAzienda.azienda;

                Utente ut = jPAQueryFactory.select(qUtente)
                        .from(qUtente)
                        .where(qUtente.id.eq(idUtente))
                        .fetchOne();

                Azienda azienda = jPAQueryFactory.select(qAzienda)
                        .from(qAzienda)
                        .where(qAzienda.codice.eq(codiceAzienda))
                        .fetchOne();

                if (ut == null) {
                    throw new IllegalStateException("Utente non trovato con ID: " + idUtente);
                }

                if (azienda == null) {
                    throw new IllegalStateException("Azienda non trovata con codice: " + codiceAzienda);
                }

                ImportazioniOrganigramma entity = new ImportazioniOrganigramma();
                entity.setDataInserimentoRiga(ZonedDateTime.now());
                entity.setIdAzienda(azienda);
                entity.setIdPersona(ut.getIdPersona());
                entity.setIdUtente(ut);
                entity.setNomeFile(csv.getOriginalFilename());
                entity.setTipo(tipologia.toString());
                entity.setEsito("IN CORSO");
                entity.setPath_csv_error(fileId);

                LOGGER.info("Salvando ImportazioniOrganigramma con path_csv_error: {}", fileId);

                repositoryFactory.getEntityManager().persist(entity);
                repositoryFactory.getEntityManager().flush();

                LOGGER.info("ImportazioniOrganigramma salvata con ID: {}", entity.getId());

                return entity.getId();
            });

            if (idImportazioneOrganigramma == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Errore nella creazione del record di importazione");
            }

            // Esegui l'importazione vera e propria
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            boolean esito = transactionTemplate.execute(action -> {
                try {
                    JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());

                    // Cancella i dati precedenti
                    switch (tipologia) {
                        case APPARTENENTI ->
                            jPAQueryFactory
                                    .delete(QCSVDaImportareAppartenente.cSVDaImportareAppartenente)
                                    .where(QCSVDaImportareAppartenente.cSVDaImportareAppartenente.codiceAzienda.eq(codiceAzienda))
                                    .execute();
                        case STRUTTURE ->
                            jPAQueryFactory
                                    .delete(QCSVDaImportareStruttura.cSVDaImportareStruttura)
                                    .where(QCSVDaImportareStruttura.cSVDaImportareStruttura.codiceAzienda.eq(codiceAzienda))
                                    .execute();
                        case ANAGRAFICHE ->
                            jPAQueryFactory
                                    .delete(QCSVDaImportareAnagrafica.cSVDaImportareAnagrafica)
                                    .where(QCSVDaImportareAnagrafica.cSVDaImportareAnagrafica.codiceAzienda.eq(codiceAzienda))
                                    .execute();
                        case TRASFORMAZIONI ->
                            jPAQueryFactory
                                    .delete(QCSVDaImportareTrasformazione.cSVDaImportareTrasformazione)
                                    .where(QCSVDaImportareTrasformazione.cSVDaImportareTrasformazione.codiceAzienda.eq(codiceAzienda))
                                    .execute();
                        default ->
                            throw new IllegalArgumentException("Tipologia non supportata: " + tipologia);
                    }

                    // Importa il CSV - csvFile è accessibile perché è final
                    CsvImportManager csvImportManager = new CsvImportManager(
                            objectMapper, repositoryFactory.getEntityManager(), conversionService);
                    csvImportManager.csvImportAndValidate(separatore, csvFile, tipologia, codiceAzienda);

                    return true;
                } catch (RibaltoneHttpException | IOException ex) {
                    LOGGER.error("Errore nella gestione del caricamento CSV", ex);
                    return false;
                }
            });

            // Aggiorna lo stato finale
            setImportazioneOrganigrammaFinito(idImportazioneOrganigramma, esito ? "OK" : "ERRORE");
            CacheUtils.setImportazioneCSVFinito(idSelectedConfiguration, repositoryFactory, objectMapper, transactionTemplate);
            return ResponseEntity.status(esito ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(esito ? "OK" : "ERRORE");

        } catch (Exception ex) {
            LOGGER.error("Errore durante l'importazione CSV", ex);
            CacheUtils.setImportazioneCSVFinito(idSelectedConfiguration, repositoryFactory, objectMapper, transactionTemplate);
            if (idImportazioneOrganigramma != null) {
                setImportazioneOrganigrammaFinito(idImportazioneOrganigramma, "ERRORE");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore: " + ex.getMessage());
        } finally {
            // Pulisci lo stato della cache
            CacheUtils.setImportazioneCSVFinito(idSelectedConfiguration,
                    repositoryFactory, objectMapper, transactionTemplate);

            // Il cleanup del file temporaneo non può essere fatto qui
            // perché csvFile è dichiarato nel blocco try
            // La chiamata csvFile.deleteOnExit() lo gestisce automaticamente
        }
    }

    /**
     *
     * @param configRibaltoneView
     * @param codiceAzienda
     * @param typeUserReport
     * @return
     * @throws RibaltoneHttpException
     */
//    @Transactional(rollbackOn = Throwable.class)
    @RequestMapping(value = "/ribaltaAndGetUserReport", method = RequestMethod.POST)
    public Object ribaltaAndGetUserReport(
            @RequestBody ConfigRibaltoneView configRibaltoneView,
            @RequestParam(required = true) String codiceAzienda,
            @RequestParam(required = true) UserReport.UserReportType typeUserReport
    ) throws RibaltoneHttpException {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        LOGGER.info("Inizio fase 1 del ribaltone per il codiceAzienda " + codiceAzienda);
        ResponseEntity response = transactionTemplate.execute(action -> {
            String fonteSelezionata = configRibaltoneView.getFonteSelezionata();
            EntityManager em = repositoryFactory.getEntityManager();
            if (hoPermessoPerLanciareRibaltone()) {
                if (!(CacheUtils.isRibaltoneInCorsoFromCache(fonteSelezionata, repositoryFactory, objectMapper, transactionTemplate) || CacheUtils.isImportazioneCSVInCorsoFromCache(fonteSelezionata, repositoryFactory, objectMapper, transactionTemplate))) {
                    AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
                    Utente realUser = authenticatedUserProperties.getRealUser() != null ? authenticatedUserProperties.getRealUser() : authenticatedUserProperties.getUser();
                    realUser = repositoryFactory.getEntityManager().find(Utente.class, realUser.getId());
                    RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(em, fonteSelezionata);
                    RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), repositoryFactory.getEntityManager());
                    try {
                        CacheUtils.setImportazioneCSVInCorso(fonteSelezionata, realUser, repositoryFactory, objectMapper, transactionTemplate);
                        CacheUtils.setRibaltoneCacheInCorso(fonteSelezionata, realUser, repositoryFactory, objectMapper, transactionTemplate);
                        Map<String, Object> infoRibaltone = new HashMap<>();
                        Operations restore = ribaltoneCache.restore();
                        if (restore != null) {
                            infoRibaltone.put("operations", restore);
                        } else {
                            infoRibaltone.put("operations", ribaltoneTotaleManager.ribaltaWithUserReportAndCacheOperation(codiceAzienda, configRibaltoneView, typeUserReport));
                        }
                        Integer lanciaRibaltTree = ribaltoneTotaleManager.lanciaRibaltTree(codiceAzienda, configRibaltoneView.getFonteSelezionata(), realUser, codiceAzienda, null, "ribaltaAndGetUserReport", false, null);
                        infoRibaltone.put("idRibaltTree", lanciaRibaltTree);
                        return new ResponseEntity(infoRibaltone, HttpStatus.OK);
                    } catch (RibaltoneHttpException | ClassNotFoundException | JacksonException | StaleObjectStateException ex) {
                        try {
                            CacheUtils.setImportazioneCSVFinito(fonteSelezionata, repositoryFactory, objectMapper, transactionTemplate);
                            CacheUtils.setRibaltoneCacheFinito(configRibaltoneView.getFonteSelezionata(), repositoryFactory, objectMapper, transactionTemplate);
                        } catch (JacksonException e) {
                            LOGGER.error("non ho settato il ribaltone finito", e);
                        }
                        LOGGER.info("setto la connessione come rollbackonly", ex);
                        action.setRollbackOnly();
                        ribaltoneCache.setImportingCSV(Boolean.FALSE, realUser);
                        return new ResponseEntity(ex, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                } else {
                    RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(repositoryFactory.getEntityManager(), configRibaltoneView.getFonteSelezionata());
                    RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), repositoryFactory.getEntityManager());
                    try {
                        Utente user = repositoryFactory.getEntityManager().find(Utente.class, ribaltoneCache.getIdUserExecuting());
                        return new ResponseEntity(user, HttpStatus.IM_USED);
                    } catch (JacksonException e) {
                        return new ResponseEntity(HttpStatus.IM_USED);
                    }
                }
            } else {
                return new ResponseEntity("non puoi lanicare il ribaltone perche non ne hai il permesso", HttpStatus.UNAUTHORIZED);
            }
        });
        return response;
    }

    /**
     *
     * @param codiceAzienda
     * @param idSelectedConfiguration
     * @param idRibaltTree
     * @return
     * @throws RibaltoneHttpException
     */
    @RequestMapping(value = "/ribaltaPostUserReport", method = RequestMethod.POST)
    public Object ribaltaPostUserReport(
            @RequestParam(required = true) String codiceAzienda,
            @RequestParam(required = true) String idSelectedConfiguration,
            @RequestParam(required = true) Integer idRibaltTree,
            @RequestParam(required = false) String mailDaNotificare,
            @RequestParam(required = false) String idPersoneDaNotificare
    ) throws RibaltoneHttpException, JacksonException {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        if (hoPermessoPerLanciareRibaltone()) {
            AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
            try {
                ResponseEntity response = transactionTemplate.execute(action -> {
                    Utente realUser = authenticatedUserProperties.getRealUser() != null ? authenticatedUserProperties.getRealUser() : authenticatedUserProperties.getUser();
                    realUser = repositoryFactory.getEntityManager().find(Utente.class, realUser.getId());
                    try {
                        List<String> mailDaNotificareList = null;
                        if (StringUtils.hasText(mailDaNotificare)) {
                            mailDaNotificareList = Arrays.asList(mailDaNotificare.split(","));

                        }
                        List<Integer> idPersoneDaNotificareList = null;
                        if (StringUtils.hasText(idPersoneDaNotificare)) {
                            idPersoneDaNotificareList = Arrays.stream(idPersoneDaNotificare.split(","))
                                .map(Integer::parseInt)
                                .toList();
                        }
                        LOGGER.info("inizio a ribaltare davvero con questo codice azienda " + codiceAzienda + "con questa configurazione " + idSelectedConfiguration);
                        ribaltoneTotaleManager.ribaltaFromCachedOperation(codiceAzienda, idSelectedConfiguration, realUser, idPersoneDaNotificareList, mailDaNotificareList);
                        ribaltoneTotaleManager.lanciaRibaltTree(codiceAzienda, idSelectedConfiguration, realUser, null, idRibaltTree, "ribaltaPostUserReport", false, null);
                        RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(repositoryFactory.getEntityManager(), idSelectedConfiguration);
                        RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), repositoryFactory.getEntityManager());
                        ribaltoneCache.cleanDataCache();
                        CacheUtils.setRibaltoneCacheFinito(idSelectedConfiguration, repositoryFactory, objectMapper, transactionTemplate);
                        return new ResponseEntity("tutto ok", HttpStatus.OK);
                    } catch (Exception ex) {
                        LOGGER.error("errore nel ribaltone ", ex);

                        CacheUtils.setRibaltoneCacheFinito(idSelectedConfiguration, repositoryFactory, objectMapper, transactionTemplate);
                        throw new RibaltoneHttpException(ex);
//                    return new ResponseEntity("tutto errore nel ribaltone", HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                });
                return response;
            } catch (RibaltoneHttpException ex) {
                transactionTemplate.executeWithoutResult(action -> {
                    Utente realUser = authenticatedUserProperties.getRealUser() != null ? authenticatedUserProperties.getRealUser() : authenticatedUserProperties.getUser();
                    ribaltoneTotaleManager.lanciaRibaltTree(codiceAzienda, idSelectedConfiguration, realUser, null, idRibaltTree, "errore", false, ExceptionUtils.getStackTrace(ex));

                });
                CacheUtils.setRibaltoneCacheFinito(idSelectedConfiguration, repositoryFactory, objectMapper, transactionTemplate);
//
//                CacheUtils.setRibaltoneCacheFinito(idSelectedConfiguration, repositoryFactory, objectMapper, transactionTemplate);
//                throw ex;
            }
            return new ResponseEntity("tutto errore nel ribaltone", HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            return new ResponseEntity("non puoi lanicare il ribaltone perche non ne hai il permesso", HttpStatus.UNAUTHORIZED);
        }
    }

    @Transactional(rollbackOn = Throwable.class)
    @RequestMapping(value = "/ribaltaDeleteCache", method = RequestMethod.POST)
    public void ribaltaDeleteCache(
            @RequestParam(required = true) String codiceAzienda,
            @RequestParam(required = true) String idSelectedConfiguration,
            @RequestParam(required = true) Integer idRibaltTree
    ) throws RibaltoneHttpException, JacksonException {
        RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(repositoryFactory.getEntityManager(), idSelectedConfiguration);
        RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), repositoryFactory.getEntityManager());
        ribaltoneCache.cleanDataCache();
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente realUser = authenticatedUserProperties.getRealUser() != null ? authenticatedUserProperties.getRealUser() : authenticatedUserProperties.getUser();
        realUser = repositoryFactory.getEntityManager().find(Utente.class, realUser.getId());
        CacheUtils.setRibaltoneCacheFinito(idSelectedConfiguration, repositoryFactory, objectMapper, transactionTemplate);
        ribaltoneTotaleManager.lanciaRibaltTree(codiceAzienda, idSelectedConfiguration, realUser, codiceAzienda, idRibaltTree, "ribaltaDeleteCache", false, null);
    }

    @Transactional(rollbackOn = Throwable.class)
    @RequestMapping(value = "/strutturaunificata/unifica", method = RequestMethod.POST)
    public void unifica(
            @RequestBody(required = true) Integer[] idUnificazioni
    ) throws RibaltoneHttpException {
        if (hoPermessoPerLanciareRibaltone()) {
            JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
            QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
            QAfferenzaStruttura qAfferenzaStruttura = QAfferenzaStruttura.afferenzaStruttura;
            AfferenzaStruttura idAfferenzaStruttura = queryFactory
                    .select(qAfferenzaStruttura).from(qAfferenzaStruttura).where(qAfferenzaStruttura.codice.equalsIgnoreCase(AfferenzaStruttura.CodiciAfferenzaStruttura.UNIFICATA.toString())).fetchOne();
            for (Integer idUnificazione : idUnificazioni) {

                StrutturaUnificata unificazione = queryFactory
                        .select(qStrutturaUnificata)
                        .from(qStrutturaUnificata)
                        .where(
                                qStrutturaUnificata.id.eq(idUnificazione)
                        ).fetchOne();

                if (unificazione == null) {
                    throw new RibaltoneHttpException("unificazione non attivabile perche non esiste");
                }
                unificazione.setDataAccensioneAttivazione(ZonedDateTime.now());
                unificazione.setDataAttivazione(ZonedDateTime.now());

                StrutturaUnificata.TipoUnificazione tipoUnificazione = unificazione.getTipoOperazione();
                Utente utente;
                Struttura sorgente = unificazione.getIdStrutturaSorgente();
                Struttura destinazione = unificazione.getIdStrutturaDestinazione();
                switch (tipoUnificazione) {

                    case FUSIONE -> {
                        List<UtenteStruttura> sorgenteUtenteStrutturaList = sorgente.getUtenteStrutturaList().stream().filter(us -> us.getAttivo()).toList();
                        List<UtenteStruttura> destinazioneUtenteStrutturaList = destinazione.getUtenteStrutturaList().stream().filter(us -> us.getAttivo()).toList();
                        List<UtenteStruttura> usDaAggiungereADestinazione = RibaltoneUtils.differenza(sorgenteUtenteStrutturaList, destinazioneUtenteStrutturaList);
                        List<UtenteStruttura> usDaAggiungereASorgente = RibaltoneUtils.differenza(destinazioneUtenteStrutturaList, sorgenteUtenteStrutturaList);

                        for (UtenteStruttura daAggiungereASorgente : usDaAggiungereASorgente) {
                            Optional<Utente> userOpt = daAggiungereASorgente.getIdUtente().getIdPersona().getUtenteList().stream().filter(u -> u.getIdAzienda().getId().equals(sorgente.getIdAzienda().getId())).findFirst();
                            if (userOpt.isPresent()) {
                                utente = userOpt.get();
                                utente.setAttivo(Boolean.TRUE);
                            } else {
                                utente = new Utente();
                                utente.setAttivo(Boolean.TRUE);
                                utente.setIdAzienda(sorgente.getIdAzienda());
                                utente.setIdPersona(daAggiungereASorgente.getIdUtente().getIdPersona());
                                utente.setUsername(daAggiungereASorgente.getIdUtente().getIdPersona().getUtenteList().get(0).getUsername());
                            }
                            UtenteStruttura utenteStrutturaNew = new UtenteStruttura();
                            utenteStrutturaNew.setAttivo(Boolean.TRUE);
                            utenteStrutturaNew.setAttivoDal(ZonedDateTime.now());
                            utenteStrutturaNew.setAttributi(daAggiungereASorgente.getAttributi());
                            utenteStrutturaNew.setIdStruttura(sorgente);
                            utenteStrutturaNew.setIdAfferenzaStruttura(idAfferenzaStruttura);
                            utenteStrutturaNew.setIdAziendaDerivazioneUnificazione(destinazione.getIdAzienda());
                            boolean responsabile = daAggiungereASorgente.getResponsabile() == null ? false : daAggiungereASorgente.getResponsabile();
                            utenteStrutturaNew.setResponsabile(responsabile);
                            utenteStrutturaNew.setRuoliUtenteStruttura(daAggiungereASorgente.getRuoliUtenteStruttura());
                            utenteStrutturaNew.setIdUtente(utente);
                            LOGGER.info(utente.getIdPersona().getDescrizione());
                            LOGGER.info(utente.getIdAzienda().getId().toString());
                            repositoryFactory.getEntityManager().persist(utenteStrutturaNew);

                            try {
                                permissionManager.copyActiveFlowPermissionsFromSubjectObjectToSubjectObject(
                                        daAggiungereASorgente.getIdUtente(),
                                        daAggiungereASorgente.getIdStruttura(), utenteStrutturaNew.getIdUtente(),
                                        utenteStrutturaNew.getIdStruttura());

                            } catch (BlackBoxPermissionException ex) {
                                LOGGER.error("errore nella copia dei permessi da unificare", ex);
                                throw new RibaltoneHttpException("errore nella copia dei permessi da unificare", ex);
                            }
                        }

                        for (UtenteStruttura daAggiungereADestinazione : usDaAggiungereADestinazione) {
                            Optional<Utente> userOpt = daAggiungereADestinazione.getIdUtente().getIdPersona().getUtenteList().stream().filter(u -> u.getIdAzienda().getId().equals(destinazione.getIdAzienda().getId())).findFirst();
                            if (userOpt.isPresent()) {
                                utente = userOpt.get();
                                utente.setAttivo(Boolean.TRUE);
                            } else {
                                utente = new Utente();
                                utente.setAttivo(Boolean.TRUE);
                                utente.setIdAzienda(destinazione.getIdAzienda());
                                utente.setIdPersona(daAggiungereADestinazione.getIdUtente().getIdPersona());
                                utente.setUsername(daAggiungereADestinazione.getIdUtente().getIdPersona().getUtenteList().get(0).getUsername());

                            }
                            boolean responsabile = daAggiungereADestinazione.getResponsabile() == null ? false : daAggiungereADestinazione.getResponsabile();
                            UtenteStruttura utenteStrutturaNew = new UtenteStruttura();
                            utenteStrutturaNew.setAttivo(Boolean.TRUE);
                            utenteStrutturaNew.setAttivoDal(ZonedDateTime.now());
                            utenteStrutturaNew.setAttributi(daAggiungereADestinazione.getAttributi());
                            utenteStrutturaNew.setIdStruttura(destinazione);
                            utenteStrutturaNew.setIdAfferenzaStruttura(idAfferenzaStruttura);
                            utenteStrutturaNew.setResponsabile(responsabile);
                            utenteStrutturaNew.setIdAziendaDerivazioneUnificazione(sorgente.getIdAzienda());
                            utenteStrutturaNew.setRuoliUtenteStruttura(daAggiungereADestinazione.getRuoliUtenteStruttura());
                            utenteStrutturaNew.setIdUtente(utente);
                            LOGGER.info(utente.getIdPersona().getDescrizione());
                            LOGGER.info(utente.getIdAzienda().getId().toString());
                            LOGGER.info(destinazione.getIdAzienda().getId().toString());
                            repositoryFactory.getEntityManager().persist(utenteStrutturaNew);
                            try {
                                permissionManager.copyActiveFlowPermissionsFromSubjectObjectToSubjectObject(
                                        daAggiungereADestinazione.getIdUtente(),
                                        daAggiungereADestinazione.getIdStruttura(), utenteStrutturaNew.getIdUtente(),
                                        utenteStrutturaNew.getIdStruttura());
                            } catch (BlackBoxPermissionException ex) {
                                LOGGER.error("errore nella copia dei permessi da unificare", ex);
                                throw new RibaltoneHttpException("errore nella copia dei permessi da unificare", ex);
                            }
                        }
                        repositoryFactory.getEntityManager().persist(unificazione);
                    }

                    case REPLICA -> {
                        //prendo la sorgente e la replico come figlia della destinazione
                        //creo la struttura
                        Struttura nuovaStruttura = new Struttura();
                        nuovaStruttura.setAttiva(Boolean.TRUE);
                        nuovaStruttura.setDataAttivazione(ZonedDateTime.now());
                        nuovaStruttura.setIdAzienda(destinazione.getIdAzienda());
                        nuovaStruttura.setNome(sorgente.getNome());
                        nuovaStruttura.setIdStrutturaPadre(destinazione);
                        nuovaStruttura.setIdStrutturaReplicata(sorgente);
                        nuovaStruttura.setUfficio(sorgente.getUfficio());
                        nuovaStruttura.setSpettrale(sorgente.getSpettrale());
                        nuovaStruttura.setUsaSegreteriaBucataPadre(sorgente.getUsaSegreteriaBucataPadre());
                        nuovaStruttura.setIdStrutturaUnificata(unificazione);
                        //creo lo storico relazione
                        StoricoRelazione sr = new StoricoRelazione();
                        sr.setAttivaDal(ZonedDateTime.now());
                        sr.setIdStrutturaPadre(destinazione);
                        sr.setIdStrutturaFiglia(nuovaStruttura);
                        repositoryFactory.getEntityManager().persist(sr);
                        repositoryFactory.getEntityManager().persist(unificazione);
                        repositoryFactory.getEntityManager().persist(nuovaStruttura);
                        repositoryFactory.getEntityManager().refresh(nuovaStruttura);

                        //creo gli utenti struttura
                        List<UtenteStruttura> utentiStrutturaDaRiportare = sorgente.getUtenteStrutturaList().stream().filter(us -> us.getAttivo()).toList();
                        for (UtenteStruttura utenteStruttura : utentiStrutturaDaRiportare) {
                            Azienda idAziendaDerivazioneUnificazione = utenteStruttura.getIdStruttura().getIdAzienda();
                            Persona idPersona = utenteStruttura.getIdUtente().getIdPersona();
                            Optional<Utente> userOpt = idPersona.getUtenteList().stream().filter(u -> u.getIdAzienda().getId().equals(destinazione.getIdAzienda().getId())).findFirst();

                            //prendo l'utente attivandolo nel caso sia spento
                            if (userOpt.isPresent()) {
                                utente = userOpt.get();
                                utente.setAttivo(Boolean.TRUE);
                            } else {
                                utente = new Utente();
                                utente.setAttivo(Boolean.TRUE);
                                utente.setIdAzienda(destinazione.getIdAzienda());
                                utente.setIdPersona(idPersona);
                                utente.setUsername(idPersona.getUtenteList().get(0).getUsername());
                            }
                            //creazione degli utenti struttura veri e propri
                            UtenteStruttura us = new UtenteStruttura();
                            us.setAttivo(Boolean.TRUE);
                            us.setIdStruttura(nuovaStruttura);
                            us.setAttivoDal(ZonedDateTime.now());
//                        us.setBitRuoli(utenteStruttura.getBitRuoli());
                            us.setIdAfferenzaStruttura(idAfferenzaStruttura);
                            us.setIdAziendaDerivazioneUnificazione(idAziendaDerivazioneUnificazione);
                            us.setIdStrutturaDerivazioneUnificazione(utenteStruttura.getIdStruttura());
                            us.setIdUtente(utente);
                            us.setResponsabile(utenteStruttura.getResponsabile());
                            repositoryFactory.getEntityManager().persist(us);

                            try {
                                permissionManager.copyActiveFlowPermissionsFromSubjectObjectToSubjectObject(
                                        utenteStruttura.getIdUtente(),
                                        utenteStruttura.getIdStruttura(), us.getIdUtente(), us.getIdStruttura());
                            } catch (BlackBoxPermissionException ex) {
                                throw new RibaltoneHttpException("errore nel copiare i permessi di " + utenteStruttura.getIdUtente().getId(), ex);
                            }
                        }
                    }
                    default ->
                        throw new AssertionError();
                }
            }
        } else {
            throw new RibaltoneHttpException("Non posso lanciare l'unificazione perche non ne ho il permesso");
        }
    }

    @Transactional(rollbackOn = Throwable.class)
    @RequestMapping(value = "/strutturaunificata/spegniunificazione", method = RequestMethod.POST)
    public void spegniUnificazione(
            @RequestBody(required = true) Integer idUnificazione
    ) throws RibaltoneHttpException {
        if (hoPermessoPerLanciareRibaltone()) {

            JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
            QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
            QStruttura qStruttura = QStruttura.struttura;
            QAfferenzaStruttura qAfferenzaStruttura = QAfferenzaStruttura.afferenzaStruttura;
            QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;
            AfferenzaStruttura idAfferenzaStruttura = queryFactory
                    .select(qAfferenzaStruttura).from(qAfferenzaStruttura).where(qAfferenzaStruttura.codice.equalsIgnoreCase(AfferenzaStruttura.CodiciAfferenzaStruttura.UNIFICATA.toString())).fetchOne();
            if (idAfferenzaStruttura != null) {

                StrutturaUnificata unificazione = queryFactory
                        .select(qStrutturaUnificata)
                        .from(qStrutturaUnificata)
                        .where(
                                qStrutturaUnificata.id.eq(idUnificazione)
                        ).fetchOne();

                if (unificazione == null) {
                    throw new RibaltoneHttpException("unificazione non spegnibile perche non esiste");
                }
                unificazione.setDataDisattivazione(ZonedDateTime.now());

                StrutturaUnificata.TipoUnificazione tipoUnificazione = unificazione.getTipoOperazione();
                Struttura sorgente = unificazione.getIdStrutturaSorgente();
                Struttura destinazione = unificazione.getIdStrutturaDestinazione();

                switch (tipoUnificazione) {

                    case FUSIONE -> {
                        List<UtenteStruttura> sorgenteUtentiStrutturaDaSpegnereList = sorgente.getUtenteStrutturaList().stream().filter(
                                us -> us.getAttivo()
                                && us.getIdAfferenzaStruttura().getId().equals(idAfferenzaStruttura.getId())
                                && us.getIdAziendaDerivazioneUnificazione().getId().equals(destinazione.getIdAzienda().getId())
                        ).toList();

                        List<UtenteStruttura> destinazioneUtenteStrutturaDaSpegnereList = destinazione.getUtenteStrutturaList().stream().filter(
                                us -> us.getAttivo() && us.getIdAfferenzaStruttura().getId().equals(idAfferenzaStruttura.getId())
                                && us.getIdAziendaDerivazioneUnificazione().getId().equals(sorgente.getIdAzienda().getId())
                        ).toList();

                        for (UtenteStruttura daSpegnereASorgente : sorgenteUtentiStrutturaDaSpegnereList) {
                            daSpegnereASorgente = spegniUtenteStruttura(daSpegnereASorgente, repositoryFactory.getEntityManager());
                            spegniPermessiUtenteStrutturaMorto(daSpegnereASorgente, permissionManager, "spegni unificazione id " + unificazione.getId());
                            LOGGER.info("spento utente_struttura con id=" + daSpegnereASorgente.getId());
                        }
                        for (UtenteStruttura daSpegnereADestinazione : destinazioneUtenteStrutturaDaSpegnereList) {
                            daSpegnereADestinazione = spegniUtenteStruttura(daSpegnereADestinazione, repositoryFactory.getEntityManager());
                            spegniPermessiUtenteStrutturaMorto(daSpegnereADestinazione, permissionManager, "spegni unificazione id " + unificazione.getId());
                        }
                    }

                    case REPLICA -> {
                        //prendo la sorgente vado a cercare tutte le strutture figlie e figlie di figlie fino alle foglie
                        //quindi le vado a cercare in idAziendaDestinazione
                        //spengo le strutture trovate,
                        //cerco gli utentistruttura unificati di quelle strutture e li spengo
                        //cerco gli utenti che non hanno piu senso e li spegno
                        unificazione.getIdStrutturaSorgente();

                        List<Struttura> struttureDaChiudereList = queryFactory
                                .select(qStruttura)
                                .from(qStruttura)
                                .where(
                                        qStruttura.idStrutturaUnificata.id.eq(unificazione.getId()).and(qStruttura.attiva)).fetch();
                        for (Struttura struttura : struttureDaChiudereList) {
                            OperationsUtils.chiudiStruttura(
                                    struttura,
                                    queryFactory,
                                    qStruttura,
                                    qStoricoRelazione);
                            for (UtenteStruttura usDaSpegnere : struttura.getUtenteStrutturaList()) {
                                spegniUtenteStruttura(usDaSpegnere, repositoryFactory.getEntityManager());
                                spegniPermessiUtenteStrutturaMorto(usDaSpegnere, permissionManager, "spegni unificazione id " + unificazione.getId());
                            }
                            //devo spegnere i permessi veicolati e i permessi per id struttura morti
                            try {
                                repositoryFactory.getPermissionManager().deletePermissionByObject(struttura, null, null, null, null, BlackBoxConstants.Ambito.PICO.toString(), BlackBoxConstants.Tipo.FLUSSO.toString(), "ribaltone");
                                repositoryFactory.getPermissionManager().deletePermissionByObject(struttura, null, null, null, null, BlackBoxConstants.Ambito.DELI.toString(), BlackBoxConstants.Tipo.FLUSSO.toString(), "ribaltone");
                                repositoryFactory.getPermissionManager().deletePermissionByObject(struttura, null, null, null, null, BlackBoxConstants.Ambito.DETE.toString(), BlackBoxConstants.Tipo.FLUSSO.toString(), "ribaltone");
                                //chiudere i permessi veicolati
                                repositoryFactory.getPermissionManager().deleteVeicoledPermission(struttura, "ribaltone");
                            } catch (BlackBoxPermissionException ex) {
                                LOGGER.error("non sono stati rimossi i permessi di struttura con id " + struttura.getId());
                            }
                        }
                    }
                    default ->
                        throw new AssertionError();
                }
                repositoryFactory.getEntityManager().persist(unificazione);
            }
        } else {
            throw new RibaltoneHttpException("Non posso lanciare l'unificazione perche non ne ho il permesso");
        }
    }

    private void spegniPermessiUtenteStrutturaMorto(UtenteStruttura us, PermissionManager pm, String spentoDa) {
        try {
            pm.deletePermission(
                    us.getIdUtente(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    BlackBoxConstants.Ambito.PICO.toString(),
                    BlackBoxConstants.Tipo.FLUSSO.toString(),
                    spentoDa
            );
            pm.deletePermission(
                    us.getIdUtente(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    BlackBoxConstants.Ambito.DELI.toString(),
                    BlackBoxConstants.Tipo.FLUSSO.toString(),
                    spentoDa
            );
            pm.deletePermission(
                    us.getIdUtente(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    BlackBoxConstants.Ambito.DETE.toString(),
                    BlackBoxConstants.Tipo.FLUSSO.toString(),
                    spentoDa
            );

        } catch (BlackBoxPermissionException ex) {
            LOGGER.error("non sono riuscito a spengere il permesso perche ", ex);
        }
    }

    private boolean hoPermessoPerLanciareRibaltone() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
        QRuolo qRuolo = QRuolo.ruolo;
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente realUser = authenticatedUserProperties.getRealUser();
        Persona realPerson = authenticatedUserProperties.getRealPerson();
        Utente user = authenticatedUserProperties.getUser();
        Persona person = authenticatedUserProperties.getPerson();
        Integer bitRuoliPersona = realPerson != null ? realPerson.getBitRuoli() : person.getBitRuoli();
        Integer bitRuoliUtente = realUser != null ? realUser.getBitRuoli() : user.getBitRuoli();

        List<Ruolo> ruoli = queryFactory.select(qRuolo).from(qRuolo).where(
                qRuolo.nomeBreve.eq(Ruolo.CodiciRuolo.CA.toString()).or(
                        qRuolo.nomeBreve.eq(Ruolo.CodiciRuolo.SD.toString()))
        ).fetch();

        Boolean lanciaRibaltone = false;
        for (Ruolo ruolo : ruoli) {
            if (((ruolo.getNomeBreve().equals(Ruolo.CodiciRuolo.CA) && (bitRuoliUtente & ruolo.getMascheraBit()) == ruolo.getMascheraBit()))
                    || ((ruolo.getNomeBreve().equals(Ruolo.CodiciRuolo.SD))
                    && (bitRuoliPersona & ruolo.getMascheraBit()) == ruolo.getMascheraBit())) {
                lanciaRibaltone = true;
                break;
            }
        }
        return lanciaRibaltone;
    }

    private void setImportazioneOrganigrammaFinito(Integer idImportazioneOgranigramma, String esito) {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(action -> {
            JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
            QImportazioniOrganigramma qImportazioniOrganigramma = QImportazioniOrganigramma.importazioniOrganigramma;
            jPAQueryFactory
                    .update(qImportazioniOrganigramma)
                    .set(qImportazioniOrganigramma.esito, esito)
                    .where(qImportazioniOrganigramma.id.eq(idImportazioneOgranigramma))
                    .execute();
        });
    }

    private UtenteStruttura spegniUtenteStruttura(UtenteStruttura usDaSpegnere, EntityManager entityManager) {
        usDaSpegnere.setAttivo(false);
        usDaSpegnere.setAttivoAl(ZonedDateTime.now());

        Utente idUtente = usDaSpegnere.getIdUtente();
        List<UtenteStruttura> altriUtentiStruttura = idUtente.getUtenteStrutturaList().stream().filter(us -> us.getAttivo() && !us.getId().equals(usDaSpegnere.getId())).toList();
        //devo spegnere anche utente
        if (altriUtentiStruttura == null || altriUtentiStruttura.isEmpty()) {
            idUtente.setAttivo(false);
            idUtente.setDataSpegnimento(ZonedDateTime.now());
            usDaSpegnere.setIdUtente(idUtente);
            entityManager.persist(idUtente);
            Persona persona = idUtente.getIdPersona();
            List<Utente> altriUtenti = persona.getUtenteList().stream().filter(u -> u.getAttivo()).toList();
            if (altriUtenti == null || altriUtenti.isEmpty()) {
                persona.setAttiva(false);
                idUtente.setIdPersona(persona);
                entityManager.persist(persona);
            }
            entityManager.persist(idUtente);
            entityManager.persist(persona);
            entityManager.persist(usDaSpegnere);
        }
        return usDaSpegnere;
    }

}
