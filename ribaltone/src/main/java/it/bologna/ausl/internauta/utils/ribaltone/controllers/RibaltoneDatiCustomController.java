package it.bologna.ausl.internauta.utils.ribaltone.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeWriter;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RibaltoneDataConfigurationRepository;
import it.bologna.ausl.internauta.utils.ribaltone.utils.ExportDatiManager;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.configurazione.data.ConfigRibaltoneView;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration.SpecificheNonSensibiliKeys;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "${ribaltonedati.mapping.url.root}")
public class RibaltoneDatiCustomController implements ControllerHandledExceptions {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ParametriAziendeWriter parametriWriter;

    @Autowired
    private ParametriAziendeReader parametriReader;

    @Autowired
    private RibaltoneDataConfigurationRepository ribaltoneDataConfigurationRepository;

    @RequestMapping(value = "downloadCSVFileFromIdAzienda", method = RequestMethod.GET)
    public void downloadCSVFileFromIdAzienda(
        @RequestParam("idAzienda") Integer idAzienda,
        @RequestParam("tipo") TipologiaCsv tipo,
        HttpServletResponse response,
        HttpServletRequest request) {
        File buildCSV = null;

        //SELECT codice_ente, codice_matricola, cognome, nome, codice_fiscale, id_casella, datain, datafi, tipo_appartenenza, username, data_assunzione, data_dimissione, id_azienda FROM gru.mdr_appartenenti WHERE id_azienda = ?1
        QDatiImportatiAnagrafica qDatiImportatiAnagrafica = QDatiImportatiAnagrafica.datiImportatiAnagrafica;
        QDatiImportatiAppartenente qDatiImportatiAppartenente = QDatiImportatiAppartenente.datiImportatiAppartenente;
        QDatiImportatiStruttura qDatiImportatiStruttura = QDatiImportatiStruttura.datiImportatiStruttura;
        QDatiImportatiTrasformazione qDatiImportatiTrasformazione = QDatiImportatiTrasformazione.datiImportatiTrasformazione;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        List<Expression<?>> expressions = new ArrayList<>();
        List<Tuple> selectRigheByIdAzienda = new ArrayList<>();

        switch (tipo) {
            case APPARTENENTI:
                expressions = List.of(qDatiImportatiAppartenente.codiceEnte,
                    qDatiImportatiAppartenente.codiceMatricola,
                    qDatiImportatiAppartenente.cognome,
                    qDatiImportatiAppartenente.nome,
                    qDatiImportatiAppartenente.codiceFiscale,
                    qDatiImportatiAppartenente.idCasella,
                    qDatiImportatiAppartenente.datain,
                    qDatiImportatiAppartenente.datafi,
                    qDatiImportatiAppartenente.tipoAppartenenza,
                    qDatiImportatiAppartenente.username,
                    qDatiImportatiAppartenente.responsabile,
                    qDatiImportatiAppartenente.dataAssunzione,
                    qDatiImportatiAppartenente.dataDimissione);

                selectRigheByIdAzienda = queryFactory
                    .select(expressions.toArray(new Expression[0]))
                    .from(qDatiImportatiAppartenente)
                    .where(qDatiImportatiAppartenente.idAzienda.eq(idAzienda)).fetch();

                break;

            case STRUTTURE:
                expressions = List.of(qDatiImportatiStruttura.idCasella,
                    qDatiImportatiStruttura.idPadre,
                    qDatiImportatiStruttura.descrizione,
                    qDatiImportatiStruttura.datain,
                    qDatiImportatiStruttura.datafi,
                    qDatiImportatiStruttura.tipoLegame,
                    qDatiImportatiStruttura.codiceEnte);

                selectRigheByIdAzienda = queryFactory
                    .select(expressions.toArray(new Expression[0]))
                    .from(qDatiImportatiStruttura)
                    .where(qDatiImportatiStruttura.idAzienda.eq(idAzienda)).fetch();
                break;

            case TRASFORMAZIONI:
                expressions = List.of(qDatiImportatiTrasformazione.progressivoRiga,
                    qDatiImportatiTrasformazione.idCasellaPartenza,
                    qDatiImportatiTrasformazione.idCasellaArrivo,
                    qDatiImportatiTrasformazione.codiceEnte,
                    qDatiImportatiTrasformazione.dataTrasformazione,
                    qDatiImportatiTrasformazione.motivo,
                    qDatiImportatiTrasformazione.datainPartenza,
                    qDatiImportatiTrasformazione.dataoraOper
                );

                selectRigheByIdAzienda = queryFactory
                    .select(expressions.toArray(new Expression[0]))
                    .from(qDatiImportatiTrasformazione)
                    .where(qDatiImportatiTrasformazione.idAzienda.eq(idAzienda)).fetch();
                break;

            case ANAGRAFICHE:
                expressions = List.of(qDatiImportatiAnagrafica.codiceEnte.as("pippo"),
                    qDatiImportatiAnagrafica.codiceMatricola,
                    qDatiImportatiAnagrafica.cognome,
                    qDatiImportatiAnagrafica.nome,
                    qDatiImportatiAnagrafica.codiceFiscale,
                    qDatiImportatiAnagrafica.email
                );

                selectRigheByIdAzienda = queryFactory
                    .select(expressions.toArray(new Expression[0]))
                    .from(qDatiImportatiAnagrafica)
                    .where(qDatiImportatiAnagrafica.idAzienda.eq(idAzienda)).fetch();
                break;
        }

        buildCSV = ExportDatiManager.buildCSV(selectRigheByIdAzienda, tipo);

        if (buildCSV != null) {
            try {
                StreamUtils.copy(new FileInputStream(buildCSV), response.getOutputStream());
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(RibaltoneDatiCustomController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /*
     * Servlet che si occupa di salvare i dati di configuraione del ribaltone smistando i dati su più entità: parametroAziende e ribaltoneDataConfiguration
     */
    @RequestMapping(value = "updateRibaltoneConf", method = RequestMethod.POST)
    public ResponseEntity<?> updateRibaltoneConf(
        @RequestBody String configRibaltoneViewDaSalvareString,
        @RequestParam Integer idAzienda,
        HttpServletRequest request) throws IOException {
        Integer[] idAziende = new Integer[]{idAzienda};
        ObjectMapper om = new ObjectMapper();
        ConfigRibaltoneView configRibaltoneViewDaSalvareObject = om.readValue(configRibaltoneViewDaSalvareString, ConfigRibaltoneView.class);

        //salvo prima la parte di dati che va nel parametro azienda. Quindi tutto tranne i codici enti
        List<ParametroAziende> par = parametriReader.getParameters(ParametriAziendeReader.ParametriAzienda.ribaltoneConf.toString(), idAziende);
        Map<String, Object> map = parametriReader.getValue(par.get(0), new TypeReference<Map<String, Object>>() {
        });
        map.put(ConfigRibaltoneView.ConfigKeys.fonteSelezionata.toString(), configRibaltoneViewDaSalvareObject.getFonteSelezionata());
        map.put(ConfigRibaltoneView.ConfigKeys.fonti.toString(), configRibaltoneViewDaSalvareObject.getFonti());
        map.put(ConfigRibaltoneView.ConfigKeys.attivo.toString(), configRibaltoneViewDaSalvareObject.isAttivo());
        map.put(ConfigRibaltoneView.ConfigKeys.mailDaNotificare.toString(), configRibaltoneViewDaSalvareObject.getMailDaNotificare());
        map.put(ConfigRibaltoneView.ConfigKeys.tolleranzaStrutture.toString(), configRibaltoneViewDaSalvareObject.getTolleranzaStrutture());
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
        specifiche.put(SpecificheNonSensibiliKeys.codiciEntiValidi.toString(), configRibaltoneViewDaSalvareObject.getCodiciEntiValidi());
        try {
            RibaltoneDataConfiguration ribaltoneDataConfigurationSaved = ribaltoneDataConfigurationRepository.save(r);
        } catch (Exception e) {
            return new ResponseEntity("Errore mentre si tentava di salvare ribaltoneDataConfiguration con i nuovi codici enti", HttpStatus.CONFLICT);
        }

        return new ResponseEntity(om.writeValueAsString(configRibaltoneViewDaSalvareObject), HttpStatus.OK);
    }

    private static class TypeReferenceImpl extends TypeReference<Map<String, Object>> {

        public TypeReferenceImpl() {
        }
    }

}
