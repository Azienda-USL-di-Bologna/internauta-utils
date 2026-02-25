package it.bologna.ausl.internauta.utils.ribaltone.controllers;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.authorizationutils.session.AuthenticatedSessionData;
import it.bologna.ausl.internauta.utils.authorizationutils.session.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeWriter;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneConfiguration;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.krint.RibaltoneKrintWrapperManager;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RibaltoneDataConfigurationRepository;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.configurazione.data.ConfigRibaltoneView;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.QImportazioniOrganigramma;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration.SpecificheNonSensibiliKeys;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping(value = "${ribaltonedati.mapping.url.root}")
public class RibaltoneDatiCustomController implements ControllerHandledExceptions {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RibaltoneDatiCustomController.class);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ParametriAziendeWriter parametriWriter;

    @Autowired
    private ParametriAziendeReader parametriAziendeReader;

    @Autowired
    private RibaltoneDataConfigurationRepository ribaltoneDataConfigurationRepository;

    @Autowired
    private RibaltoneConfiguration ribaltoneConfiguration;

    @Autowired
    private RepositoryFactory repositoryFactory;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    @RequestMapping(value = "downloadCSVFileFromIdAzienda", method = RequestMethod.GET)
    public void downloadCSVFileFromIdAzienda(
        @RequestParam("idAzienda") Integer idAzienda,
        @RequestParam("tipo") TipologiaCsv tipo,
        HttpServletResponse response,
        HttpServletRequest request) throws FileNotFoundException, IOException {
        File buildCSV = null;

        //SELECT codice_ente, codice_matricola, cognome, nome, codice_fiscale, id_casella, datain, datafi, tipo_appartenenza, username, data_assunzione, data_dimissione, id_azienda FROM gru.mdr_appartenenti WHERE id_azienda = ?1
        QDatiImportatiAnagrafica qDatiImportatiAnagrafica = QDatiImportatiAnagrafica.datiImportatiAnagrafica;
        QDatiImportatiAppartenente qDatiImportatiAppartenente = QDatiImportatiAppartenente.datiImportatiAppartenente;
        QDatiImportatiStruttura qDatiImportatiStruttura = QDatiImportatiStruttura.datiImportatiStruttura;
        QDatiImportatiTrasformazione qDatiImportatiTrasformazione = QDatiImportatiTrasformazione.datiImportatiTrasformazione;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        List<Expression<?>> expressions = new ArrayList<>();
        List<Tuple> selectRigheByIdAzienda = new ArrayList<>();
        QImportazioniOrganigramma qImportazioniOrganigramma = QImportazioniOrganigramma.importazioniOrganigramma;
        String mongoUuid
            = queryFactory.select(qImportazioniOrganigramma.path_csv_error)
                .from(qImportazioniOrganigramma)
                .where(qImportazioniOrganigramma.tipo.equalsIgnoreCase(tipo.toString())
                    .and(qImportazioniOrganigramma.idAzienda.id.eq(idAzienda)))
                .orderBy(qImportazioniOrganigramma.id.desc()).limit(1).fetchOne();
        try {
            InputStream in = ribaltoneConfiguration.getMinIOWrapper().getByFileId(mongoUuid);
            StreamUtils.copy(in, response.getOutputStream());
            response.flushBuffer();
        } catch (MinIOWrapperException ex) {
            LOGGER.error("errore nel reperire il file su mongo", ex);
            throw new RibaltoneHttpException("errore nel reperire il file su mongo", ex);
        }
//        switch (tipo) {
//            case APPARTENENTI:
//
//                expressions = List.of(
//                    qDatiImportatiAppartenente.codiceEnte,
//                    qDatiImportatiAppartenente.codiceMatricola,
//                    qDatiImportatiAppartenente.cognome,
//                    qDatiImportatiAppartenente.nome,
//                    qDatiImportatiAppartenente.codiceFiscale,
//                    qDatiImportatiAppartenente.idCasella,
//                    qDatiImportatiAppartenente.responsabile,
//                    qDatiImportatiAppartenente.datain,
//                    qDatiImportatiAppartenente.datafi,
//                    qDatiImportatiAppartenente.tipoAppartenenza,
//                    qDatiImportatiAppartenente.username,
//                    qDatiImportatiAppartenente.dataAssunzione,
//                    qDatiImportatiAppartenente.dataDimissione
//                );
//
//                selectRigheByIdAzienda = queryFactory
//                    .select(expressions.toArray(new Expression[0]))
//                    .from(qDatiImportatiAppartenente)
//                    .where(qDatiImportatiAppartenente.idAzienda.eq(idAzienda)).fetch();
//
//                break;
//
//            case STRUTTURE:
//                expressions = List.of(qDatiImportatiStruttura.idCasella,
//                    qDatiImportatiStruttura.idPadre,
//                    qDatiImportatiStruttura.descrizione,
//                    qDatiImportatiStruttura.datain,
//                    qDatiImportatiStruttura.datafi,
//                    qDatiImportatiStruttura.tipoLegame,
//                    qDatiImportatiStruttura.codiceEnte);
//
//                selectRigheByIdAzienda = queryFactory
//                    .select(expressions.toArray(new Expression[0]))
//                    .from(qDatiImportatiStruttura)
//                    .where(qDatiImportatiStruttura.idAzienda.eq(idAzienda)).fetch();
//                break;
//
//            case TRASFORMAZIONI:
//                expressions = List.of(qDatiImportatiTrasformazione.progressivoRiga,
//                    qDatiImportatiTrasformazione.idCasellaPartenza,
//                    qDatiImportatiTrasformazione.idCasellaArrivo,
//                    qDatiImportatiTrasformazione.dataTrasformazione,
//                    qDatiImportatiTrasformazione.motivo,
//                    //qDatiImportatiTrasformazione.datainPartenza,
//                    qDatiImportatiTrasformazione.dataoraOper,
//                    qDatiImportatiTrasformazione.codiceEnte
//                );
//
//                selectRigheByIdAzienda = queryFactory
//                    .select(expressions.toArray(new Expression[0]))
//                    .from(qDatiImportatiTrasformazione)
//                    .where(qDatiImportatiTrasformazione.idAzienda.eq(idAzienda)).fetch();
//                break;
//
//            case ANAGRAFICHE:
//                expressions = List.of(qDatiImportatiAnagrafica.codiceEnte.as("pippo"),
//                    qDatiImportatiAnagrafica.codiceMatricola,
//                    qDatiImportatiAnagrafica.cognome,
//                    qDatiImportatiAnagrafica.nome,
//                    qDatiImportatiAnagrafica.codiceFiscale,
//                    qDatiImportatiAnagrafica.email
//                );
//
//                selectRigheByIdAzienda = queryFactory
//                    .select(expressions.toArray(new Expression[0]))
//                    .from(qDatiImportatiAnagrafica)
//                    .where(qDatiImportatiAnagrafica.idAzienda.eq(idAzienda)).fetch();
//                break;
//        }
//
//        buildCSV = ExportDatiManager.buildCSV(selectRigheByIdAzienda, tipo);
//
//        response.setContentType("text/csv");
//        response.setHeader("Content-Disposition", "attachment; filename=\"" + buildCSV.getName() + "\"");
//
//        try (FileInputStream in = new FileInputStream(buildCSV)) {
//            StreamUtils.copy(in, response.getOutputStream());
//            response.flushBuffer();
//        }

        // if (buildCSV != null) {
        //     try {
        //         StreamUtils.copy(new FileInputStream(buildCSV), response.getOutputStream());
        //     } catch (IOException ex) {
        //         java.util.logging.Logger.getLogger(RibaltoneDatiCustomController.class.getName()).log(Level.SEVERE, null, ex);
        //     }
        // }
    }

    /*
     * Servlet che si occupa di salvare i dati di configuraione del ribaltone smistando i dati su più entità: parametroAziende e ribaltoneDataConfiguration
     */
    @RequestMapping(value = "updateRibaltoneConf", method = RequestMethod.POST)
    public ResponseEntity<?> updateRibaltoneConf(
        @RequestBody String configRibaltoneViewDaSalvareString,
        @RequestParam Integer idAzienda,
        HttpServletRequest request) throws IOException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        RibaltoneKrintWrapperManager ribaltoneKrintWrapperManager = ribaltoneConfiguration.getRibaltoneKrintWrapperManager();

        Integer[] idAziende = new Integer[]{idAzienda};
        ObjectMapper om = new ObjectMapper();
        ConfigRibaltoneView configRibaltoneViewDaSalvareObject = om.readValue(configRibaltoneViewDaSalvareString, ConfigRibaltoneView.class);

        //salvo prima la parte di dati che va nel parametro azienda. Quindi tutto tranne i codici enti
        List<ParametroAziende> par = parametriAziendeReader.getParameters(ParametriAziendeReader.ParametriAzienda.ribaltoneConf.toString(), idAziende);
        Map<String, Object> map = parametriAziendeReader.getValue(par.get(0), new TypeReference<Map<String, Object>>() {
        });
        ConfigRibaltoneView configRibaltoneViewOld = parametriAziendeReader.getValue(par.get(0), new TypeReference<ConfigRibaltoneView>() {
        });

        map.put(ConfigRibaltoneView.ConfigKeys.fonteSelezionata.toString(), configRibaltoneViewDaSalvareObject.getFonteSelezionata());
        map.put(ConfigRibaltoneView.ConfigKeys.fonti.toString(), configRibaltoneViewDaSalvareObject.getFonti());
        map.put(ConfigRibaltoneView.ConfigKeys.attivo.toString(), configRibaltoneViewDaSalvareObject.isAttivo());
        map.put(ConfigRibaltoneView.ConfigKeys.mailDaNotificare.toString(), configRibaltoneViewDaSalvareObject.getMailDaNotificare());
        map.put(ConfigRibaltoneView.ConfigKeys.tolleranzaStrutture.toString(), configRibaltoneViewDaSalvareObject.getTolleranzaStrutture());
        map.put(ConfigRibaltoneView.ConfigKeys.lanciaSoloLocale.toString(), configRibaltoneViewDaSalvareObject.getLanciaSoloLocale());
        map.put(ConfigRibaltoneView.ConfigKeys.idPersoneDaNotificare.toString(), configRibaltoneViewDaSalvareObject.getIdPersoneDaNotificare());
        map.put(ConfigRibaltoneView.ConfigKeys.tolleranzaAnagrafica.toString(), configRibaltoneViewDaSalvareObject.getTolleranzaAnagrafica());
        map.put(ConfigRibaltoneView.ConfigKeys.tolleranzaAppartenenti.toString(), configRibaltoneViewDaSalvareObject.getTolleranzaAppartenenti());
        map.put(ConfigRibaltoneView.ConfigKeys.tolleranzaTrasformazioni.toString(), configRibaltoneViewDaSalvareObject.getTolleranzaTrasformazioni());

        try {
            parametriWriter.editParametersValue(om.writeValueAsString(map), ParametriAziendeWriter.ParametriAzienda.ribaltoneConf, idAziende);
        } catch (Exception e) {
            return new ResponseEntity("Errore mentre si tentava di aggiornare il valore del parametro azienda 'ribaltoneConf'", HttpStatus.CONFLICT);
        }

        //salvo la seconda parte che va su tabella ribaltone.configuration, ovvero i codici enti che vanno inseriti all'interno del json contenuto in 'specifiche'
        RibaltoneDataConfiguration r = ribaltoneDataConfigurationRepository.getReferenceById(configRibaltoneViewDaSalvareObject.getFonteSelezionata().toString());
        HashMap<String, Object> specifiche = (HashMap<String, Object>) r.getSpecifiche();
        configRibaltoneViewOld.setCodiciEntiValidi((List<Integer>) specifiche.get(SpecificheNonSensibiliKeys.codiciEntiValidi.toString()));
        specifiche.put(SpecificheNonSensibiliKeys.codiciEntiValidi.toString(), configRibaltoneViewDaSalvareObject.getCodiciEntiValidi());
        try {
            RibaltoneDataConfiguration ribaltoneDataConfigurationSaved = ribaltoneDataConfigurationRepository.save(r);
        } catch (Exception e) {
            return new ResponseEntity("Errore mentre si tentava di salvare ribaltoneDataConfiguration con i nuovi codici enti", HttpStatus.CONFLICT);
        }
        //loggo nel krint
        ribaltoneKrintWrapperManager.scriviNelKrint(configRibaltoneViewDaSalvareObject, configRibaltoneViewOld, authenticatedUserProperties.getRealUser() != null ? authenticatedUserProperties.getRealUser() : authenticatedUserProperties.getUser(), idAzienda);
        return new ResponseEntity(om.writeValueAsString(configRibaltoneViewDaSalvareObject), HttpStatus.OK);
    }

    @RequestMapping(value = "sincronizzaDatiImportati", method = RequestMethod.POST)
    public ResponseEntity<?> sincronizzaDatiImportati(
        @RequestParam Integer idAzienda,
        @RequestParam Boolean anteprima,
        HttpServletRequest request) throws RibaltoneHttpException {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return ResponseEntity.ok(transactionTemplate.execute(status -> {
            SyncResult result = new SyncResult();

            try {
                JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

                // Esegui la function con native query
                String sql = String.format(
                    "SELECT * FROM ribaltone_dati.sincronizza_dati_importati(%d, %b)",
                    idAzienda, !anteprima);

                List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();

                if (!results.isEmpty()) {
                    Object[] row = results.get(0);
                    result.setEseguito((Boolean) row[0]);
                    result.setAziendaId((Integer) row[1]);
                    result.setCodiceAzienda((String) row[2]);
                    result.setNomeAzienda((String) row[3]);
                    result.setStrutturePrima((Integer) row[4]);
                    result.setStruttureDopo((Integer) row[5]);
                    result.setStruttureDifferenza((Integer) row[6]);
                    result.setAppartenentiPrima((Integer) row[7]);
                    result.setAppartenentiDopo((Integer) row[8]);
                    result.setAppartenentiDifferenza((Integer) row[9]);
                    result.setMessaggio((String) row[10]);
                }
                result.setNotices(new ArrayList<>());

            } catch (Exception e) {
                throw new RibaltoneHttpException("Errore durante la sincronizzazione", e);
            }
            return result;
        }));
    }

    public class SyncResult {

        private boolean eseguito;
        private int aziendaId;
        private String codiceAzienda;
        private String nomeAzienda;
        private int strutturePrima;
        private int struttureDopo;
        private int struttureDifferenza;
        private int appartenentiPrima;
        private int appartenentiDopo;
        private int appartenentiDifferenza;
        private String messaggio;
        private List<String> notices; // I messaggi NOTICE

        public boolean isEseguito() {
            return eseguito;
        }

        public void setEseguito(boolean eseguito) {
            this.eseguito = eseguito;
        }

        public int getAziendaId() {
            return aziendaId;
        }

        public void setAziendaId(int aziendaId) {
            this.aziendaId = aziendaId;
        }

        public String getCodiceAzienda() {
            return codiceAzienda;
        }

        public void setCodiceAzienda(String codiceAzienda) {
            this.codiceAzienda = codiceAzienda;
        }

        public String getNomeAzienda() {
            return nomeAzienda;
        }

        public void setNomeAzienda(String nomeAzienda) {
            this.nomeAzienda = nomeAzienda;
        }

        public int getStrutturePrima() {
            return strutturePrima;
        }

        public void setStrutturePrima(int strutturePrima) {
            this.strutturePrima = strutturePrima;
        }

        public int getStruttureDopo() {
            return struttureDopo;
        }

        public void setStruttureDopo(int struttureDopo) {
            this.struttureDopo = struttureDopo;
        }

        public int getStruttureDifferenza() {
            return struttureDifferenza;
        }

        public void setStruttureDifferenza(int struttureDifferenza) {
            this.struttureDifferenza = struttureDifferenza;
        }

        public int getAppartenentiPrima() {
            return appartenentiPrima;
        }

        public void setAppartenentiPrima(int appartenentiPrima) {
            this.appartenentiPrima = appartenentiPrima;
        }

        public int getAppartenentiDopo() {
            return appartenentiDopo;
        }

        public void setAppartenentiDopo(int appartenentiDopo) {
            this.appartenentiDopo = appartenentiDopo;
        }

        public int getAppartenentiDifferenza() {
            return appartenentiDifferenza;
        }

        public void setAppartenentiDifferenza(int appartenentiDifferenza) {
            this.appartenentiDifferenza = appartenentiDifferenza;
        }

        public String getMessaggio() {
            return messaggio;
        }

        public void setMessaggio(String messaggio) {
            this.messaggio = messaggio;
        }

        public List<String> getNotices() {
            return notices;
        }

        public void setNotices(List<String> notices) {
            this.notices = notices;
        }

    }
}
