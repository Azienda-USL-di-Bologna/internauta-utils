package it.bologna.ausl.internauta.utils.ribaltone.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.RibaltoneManagerUtils;
import static it.bologna.ausl.internauta.utils.ribaltone.RibaltoneManagerUtils.getRibaltoneCache;
import it.bologna.ausl.internauta.utils.ribaltone.RibaltoneTotaleManager;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv.ANAGRAFICA;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv.APPARTENENTE;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv.STRUTTURA;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv.TRASFORMAZIONE;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneCache;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.CsvImportManager;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReport;
import it.bologna.ausl.internauta.utils.ribaltone.utils.RibaltoneUtils;
import it.bologna.ausl.internauta.utils.ribaltone.utils.service.ConversionServices;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ConversionService conversionService;

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

    @Transactional(rollbackOn = Throwable.class)
    @RequestMapping(value = "/importaCSV", method = RequestMethod.POST)
    public void importaCSV(
        @RequestParam(required = true, name = "codiceAzienda") String codiceAzienda,
        @RequestParam(required = true, name = "csv") MultipartFile csv,
        @RequestParam(required = true, name = "tipologia") TipologiaCsv tipologia,
        @RequestParam(required = true, name = "separatore") String separatore
    ) throws RibaltoneHttpException {
        File csvFile = null;

        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        switch (tipologia) {
            case APPARTENENTE:
                jPAQueryFactory.delete(QCSVDaImportareAppartenente.cSVDaImportareAppartenente).where(
                    QCSVDaImportareAppartenente.cSVDaImportareAppartenente.codiceAzienda.eq(codiceAzienda)).execute();
                break;
            case STRUTTURA:
                jPAQueryFactory.delete(QCSVDaImportareStruttura.cSVDaImportareStruttura).where(
                    QCSVDaImportareStruttura.cSVDaImportareStruttura.codiceAzienda.eq(codiceAzienda)).execute();
                break;
            case ANAGRAFICA:
                jPAQueryFactory.delete(QCSVDaImportareAnagrafica.cSVDaImportareAnagrafica).where(
                    QCSVDaImportareAnagrafica.cSVDaImportareAnagrafica.codiceAzienda.eq(codiceAzienda)).execute();
                break;
            case TRASFORMAZIONE:
                jPAQueryFactory.delete(QCSVDaImportareTrasformazione.cSVDaImportareTrasformazione).where(
                    QCSVDaImportareTrasformazione.cSVDaImportareTrasformazione.codiceAzienda.eq(codiceAzienda)).execute();
                break;
            default:
                throw new AssertionError();
        }
        try {
            // creo il csv come file temporaneo e lo cancello al termine
            csvFile = File.createTempFile("uploadCSVribaltone_", ".csv");
            csvFile.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(csvFile);) {
                try (InputStream csvIs = csv.getInputStream()) {
                    IOUtils.copy(csvIs, fos);
                }
            }
            CsvImportManager csvImportManager = new CsvImportManager(objectMapper, entityManager, conversionService);
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

    @Transactional(rollbackOn = Throwable.class)
    @RequestMapping(value = "/unifica", method = RequestMethod.POST)
    public void unifica(
        @RequestParam(required = true) Integer idStrutturaSorgente,
        @RequestParam(required = true) Integer idStrutturaDestinazione,
        @RequestParam(required = true) StrutturaUnificata.TipoUnificazione tipoUnificazione
    ) throws RibaltoneHttpException {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QStruttura qStruttura = QStruttura.struttura;
        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
        QAfferenzaStruttura qAfferenzaStruttura = QAfferenzaStruttura.afferenzaStruttura;
        AfferenzaStruttura idAfferenzaStruttura = queryFactory
            .select(qAfferenzaStruttura).from(qAfferenzaStruttura).where(qAfferenzaStruttura.codice.equalsIgnoreCase(AfferenzaStruttura.CodiciAfferenzaStruttura.UNIFICATA.toString())).fetchOne();
        Struttura sorgente = queryFactory.select(qStruttura).from(qStruttura).where(qStruttura.id.eq(idStrutturaSorgente)).fetchOne();
        Struttura destinazione = queryFactory.select(qStruttura).from(qStruttura).where(qStruttura.id.eq(idStrutturaDestinazione)).fetchOne();

        if (sorgente == null || destinazione == null) {
            throw new RibaltoneHttpException("struttura sorgente o stuttura destinazione non presenti impossibile unificare");
        }

        StrutturaUnificata unificazione = queryFactory
            .select(qStrutturaUnificata)
            .from(qStrutturaUnificata)
            .where(
                qStrutturaUnificata.idStrutturaSorgente.id.eq(sorgente.getId())
                    .and(qStrutturaUnificata.dataDisattivazione.isNull().or(qStrutturaUnificata.dataDisattivazione.after(ZonedDateTime.now())))
            ).fetchOne();

        if (unificazione != null) {
            throw new RibaltoneHttpException("unificazione gia presente");
        }
        Utente utente;
        switch (tipoUnificazione) {
            case FUSIONE:
                unificazione = new StrutturaUnificata();
                unificazione.setDataAttivazione(ZonedDateTime.now());
                unificazione.setDataInserimentoRiga(ZonedDateTime.now());
                unificazione.setIdStrutturaSorgente(sorgente);
                unificazione.setIdStrutturaDestinazione(destinazione);
                unificazione.setTipoOperazione(tipoUnificazione);
                unificazione.setDataAccensioneAttivazione(ZonedDateTime.now());

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
                    UtenteStruttura utenteStruttura = new UtenteStruttura();
                    utenteStruttura.setAttivo(Boolean.TRUE);
                    utenteStruttura.setAttivoDal(ZonedDateTime.now());
                    utenteStruttura.setAttributi(daAggiungereASorgente.getAttributi());
                    utenteStruttura.setIdStruttura(sorgente);
                    utenteStruttura.setIdAfferenzaStruttura(idAfferenzaStruttura);
                    boolean responsabile = daAggiungereASorgente.getResponsabile() == null ? false : daAggiungereASorgente.getResponsabile();
                    utenteStruttura.setResponsabile(responsabile);
                    utenteStruttura.setRuoliUtenteStruttura(daAggiungereASorgente.getRuoliUtenteStruttura());
                    utenteStruttura.setIdUtente(utente);
                    LOGGER.info(utente.getIdPersona().getDescrizione());
                    LOGGER.info(utente.getIdAzienda().getId().toString());
                    entityManager.persist(utenteStruttura);
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
                    UtenteStruttura utenteStruttura = new UtenteStruttura();
                    utenteStruttura.setAttivo(Boolean.TRUE);
                    utenteStruttura.setAttivoDal(ZonedDateTime.now());
                    utenteStruttura.setAttributi(daAggiungereADestinazione.getAttributi());
                    utenteStruttura.setIdStruttura(destinazione);
                    utenteStruttura.setIdAfferenzaStruttura(idAfferenzaStruttura);
                    utenteStruttura.setResponsabile(responsabile);
                    utenteStruttura.setRuoliUtenteStruttura(daAggiungereADestinazione.getRuoliUtenteStruttura());
                    utenteStruttura.setIdUtente(utente);
                    LOGGER.info(utente.getIdPersona().getDescrizione());
                    LOGGER.info(utente.getIdAzienda().getId().toString());
                    LOGGER.info(destinazione.getIdAzienda().getId().toString());
                }
                entityManager.persist(unificazione);
                break;

            case REPLICA:
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
                //creo lo storico relazione
                StoricoRelazione sr = new StoricoRelazione();
                sr.setAttivaDal(ZonedDateTime.now());
                sr.setIdStrutturaPadre(destinazione);
                sr.setIdStrutturaFiglia(nuovaStruttura);

                //creo l'unificazione
                unificazione = new StrutturaUnificata();
                unificazione.setDataAttivazione(ZonedDateTime.now());
                unificazione.setDataInserimentoRiga(ZonedDateTime.now());
                unificazione.setIdStrutturaSorgente(sorgente);
                unificazione.setIdStrutturaDestinazione(nuovaStruttura);
                unificazione.setTipoOperazione(tipoUnificazione);
                unificazione.setDataAccensioneAttivazione(ZonedDateTime.now());
                //creo gli utenti struttura
                List<UtenteStruttura> utentiStrutturaDaRiportare = sorgente.getUtenteStrutturaList();
                List<UtenteStruttura> nuoviUtentiStruttura = new ArrayList<UtenteStruttura>();
                for (UtenteStruttura utenteStruttura : utentiStrutturaDaRiportare) {
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
                    us.setAttivoDal(ZonedDateTime.now());
                    us.setBitRuoli(utenteStruttura.getBitRuoli());
                    us.setIdAfferenzaStruttura(idAfferenzaStruttura);
                    us.setIdUtente(utente);
                    us.setResponsabile(utenteStruttura.getResponsabile());
                    nuoviUtentiStruttura.add(us);
                }
                //set degli utenti struttura
                nuovaStruttura.setUtenteStrutturaList(nuoviUtentiStruttura);
                //salvataggio di tutto sul db
                entityManager.persist(unificazione);
                break;
            default:
                throw new AssertionError();
        }

    }

}
