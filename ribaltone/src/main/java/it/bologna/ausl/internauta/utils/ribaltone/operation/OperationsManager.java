package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportare;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneStrutturaInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationUnificazioneAppartenente.UnificazionePair;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationUnificazioneAppartenente.UnificazionePair.DirezioneReplica;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.utils.RibaltoneUtils;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.UnificazioneDaGestire;
import jakarta.persistence.EntityManager;
import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.util.StringUtils;

/**
 *
 * @author Top
 *
 * OperationsManager classe che contiene: metodo per capire, tramite confronto,
 * le operazioni da svolgere, metodo per capire, se la quantita di dati è
 * coerente con lo storico delle importazioni
 *
 *
 */
public class OperationsManager {

    private static final Logger log = LoggerFactory.getLogger(OperationsManager.class);
    private DatiDaImportare datiDaImportare;
    private List<DatiImportatiAnagrafica> anagraficheImportate;
    private List<DatiImportatiAppartenente> appartenentiImportati;
    private List<DatiImportatiStruttura> struttureImportate;
    private Integer trasformazioniImportateUltimoProgressivoRiga = 0;
    private Integer struttureChiuse = 0;
    private Integer utentiStrutturaChiusi = 0;
    private Integer tolleranzaStrutture;
    private Integer tolleranzaAppartenenti;
    Map<String, Integer> indexAnagraficheImportate;
    Map<String, Integer> indexAppartenentiImportati;
    Map<String, Integer> indexStruttureImportate;
    Map<String, Integer> indexTrasformazioniImportate;
    private RepositoryFactory repositoryFactory;
//    private Map<Integer, List<StrutturaUnificata>> mappaReplichePerIdCasellaSorgente;
//    private Map<Integer, List<StrutturaUnificata>> mappaReplichePerIdCasellaDestinazione;
    private Map<Integer, List<StrutturaUnificata>> mappaFusioniPerIdCasellaSorgente;
    private Map<Integer, List<StrutturaUnificata>> mappaFusioniPerIdCasellaDestinazione;
    private List<OperationUnificazioneStruttura> operationsUnificazioneStruttura = new ArrayList<>();
    private List<OperationUnificazioneAppartenente> operationsUnificazioneAppartenente = new ArrayList<>();
    private Map<Integer, UnificazionePair> idCaselleReplicateMap = new HashMap<>();
    private Map<String, Integer> indexStruttureDaImportare;
    Set<Integer> idCaselleNonReplicateSet = new HashSet<>();
    Map<Long, Map<OperationsUtils.KeyMapReplica, Object>> mappaReplicheStruttureBaborg = null;
    Map<Long, Map<OperationsUtils.KeyMapReplica, Object>> mappaReplicheStruttureDaImportare = null;
    Map<Long, Map<OperationsUtils.KeyMapReplica, Object>> mappaReplicheStruttureImportate = null;
    Azienda idAzienda = null;

    public OperationsManager(DatiDaImportare datiDaImportare, String codiceAzienda, Integer tolleranzaAppartenenti, Integer tolleranzaStrutture, RepositoryFactory repositoryFactory) {
        this.datiDaImportare = datiDaImportare;
        this.anagraficheImportate = repositoryFactory.getDatiImportatiAnagraficaRepository().findByCodiceAzienda(codiceAzienda);
        this.appartenentiImportati = repositoryFactory.getDatiImportatiAppartenenteRepository().findByCodiceAzienda(codiceAzienda);
        this.struttureImportate = repositoryFactory.getDatiImportatiStrutturaRepository().findByCodiceAzienda(codiceAzienda);
        DatiImportatiTrasformazione findTopByCodiceAziendaOrderByProgressivoRigaDesc = repositoryFactory.getDatiImportatiTrasformazioneRepository().findTopByCodiceAziendaOrderByProgressivoRigaDesc(codiceAzienda);
        if (findTopByCodiceAziendaOrderByProgressivoRigaDesc != null) {
            this.trasformazioniImportateUltimoProgressivoRiga = findTopByCodiceAziendaOrderByProgressivoRigaDesc.getProgressivoRiga();
        }
        this.indexAnagraficheImportate = RibaltoneUtils.generateIndex(anagraficheImportate, DatiImportatiAnagrafica::getKey);
        this.indexAppartenentiImportati = RibaltoneUtils.generateIndex(appartenentiImportati, DatiImportatiAppartenente::getKey);
        this.indexStruttureImportate = RibaltoneUtils.generateIndex(struttureImportate, DatiImportatiStruttura::getKey);
        this.tolleranzaAppartenenti = tolleranzaAppartenenti;
        this.tolleranzaStrutture = tolleranzaStrutture;
        this.repositoryFactory = repositoryFactory;
        JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
        QAzienda qAzienda = QAzienda.azienda;
        idAzienda = queryFactory.select(qAzienda).from(qAzienda).where(qAzienda.codice.eq(codiceAzienda)).fetchOne();
    }

    /**
     *
     * @return
     *
     * genera tutte le operazioni che sono da fare da queste si possono generare
     * i report per l'utente o si puo proseguire col ribaltone
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     */
    public Operations buildOperations() throws RibaltoneHttpException, JsonProcessingException {
        EntityManager entityManager = repositoryFactory.getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        this.popolaMappeUnificazioni(queryFactory, idAzienda.getId());

        Map<String, List<? extends Operation<DatiRibaltoneInterface>>> buildedOperationsStruttureETrasformazione = buildOperationsStrutture(datiDaImportare.getStruttureDaImportare(), this.struttureImportate, this.indexStruttureImportate, RibaltoneUtils.generateIndex(this.datiDaImportare.getTrasformazioniDaImportare(), DatiDaImportareTrasformazione::getIdCasellaPartenza));
        List<OperationStruttura> operationsStrutture = (List<OperationStruttura>) buildedOperationsStruttureETrasformazione.get("strutture");
        List<OperationAppartenente> operationsAppartenenti = buildedOperationsAppartenenti(datiDaImportare.getAppartenentiDaImportare(), this.appartenentiImportati, indexAppartenentiImportati, this.struttureImportate, datiDaImportare.getStruttureDaImportare(), queryFactory);
        List<OperationAnagrafica> operationsAnagrafiche = buildedOperationsAnagrafiche(datiDaImportare.getAnagraficheDaImportare(), this.anagraficheImportate, this.indexAnagraficheImportate);
        log.info("progressivo " + trasformazioniImportateUltimoProgressivoRiga);
        Map<String, List<? extends Operation<DatiRibaltoneInterface>>> buildedOperationsTrasformazioniUnificazioni = buildedOperationsTrasformazioni(datiDaImportare.getTrasformazioniDaImportare(), this.trasformazioniImportateUltimoProgressivoRiga);
        List<OperationTrasformazione> operationsTraformazioni = (List<OperationTrasformazione>) buildedOperationsTrasformazioniUnificazioni.get("trasformazioni");

        operationsTraformazioni.addAll((Collection<? extends OperationTrasformazione>) buildedOperationsStruttureETrasformazione.get("trasformazioni"));
        return new Operations(operationsStrutture, operationsAppartenenti, operationsAnagrafiche, operationsTraformazioni, operationsUnificazioneStruttura, operationsUnificazioneAppartenente);
    }

    private void popolaMappeUnificazioni(JPAQueryFactory queryFactory, Integer idAzienda) {
        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
        List<StrutturaUnificata> listaUnificazioni = queryFactory
            .select(qStrutturaUnificata)
            .from(qStrutturaUnificata)
            .where(qStrutturaUnificata.dataAttivazione.before(ZonedDateTime.now())
                .and(qStrutturaUnificata.dataDisattivazione.isNull()
                    .or(qStrutturaUnificata.dataDisattivazione.after(ZonedDateTime.now()))
                )
            )
            .fetch();

        mappaReplicheStruttureBaborg = OperationsUtils.getMappaReplicheStrutture(repositoryFactory, "BABORG", idAzienda);
        mappaReplicheStruttureDaImportare = OperationsUtils.getMappaReplicheStrutture(repositoryFactory, "DA_IMPORTARE", idAzienda);
        mappaReplicheStruttureImportate = OperationsUtils.getMappaReplicheStrutture(repositoryFactory, "IMPORTATE", idAzienda);

        // Mappa: idStrutturaSorgente -> lista di StrutturaUnificata
//        this.mappaReplichePerIdCasellaSorgente = listaUnificazioni.stream().filter(u -> u.getTipoOperazione().equals(StrutturaUnificata.TipoUnificazione.REPLICA))
//            .collect(Collectors.groupingBy(su -> su.getIdStrutturaSorgente().getIdCasella()));
        // Mappa: idStrutturaDestinazione -> lista di StrutturaUnificata
//        this.mappaReplichePerIdCasellaDestinazione = listaUnificazioni.stream().filter(u -> u.getTipoOperazione().equals(StrutturaUnificata.TipoUnificazione.REPLICA))
//            .collect(Collectors.groupingBy(su -> su.getIdStrutturaDestinazione().getIdCasella()));
        this.mappaFusioniPerIdCasellaSorgente = listaUnificazioni.stream().filter(u -> (u.getIdStrutturaSorgente().getIdAzienda().getId().equals(idAzienda) || u.getIdStrutturaDestinazione().getIdAzienda().getId().equals(idAzienda)) && u.getTipoOperazione().equals(StrutturaUnificata.TipoUnificazione.FUSIONE))
            .collect(Collectors.groupingBy(su -> su.getIdStrutturaSorgente().getIdCasella()));

        // Mappa: idStrutturaDestinazione -> lista di StrutturaUnificata
        this.mappaFusioniPerIdCasellaDestinazione = listaUnificazioni.stream().filter(u -> (u.getIdStrutturaSorgente().getIdAzienda().getId().equals(idAzienda) || u.getIdStrutturaDestinazione().getIdAzienda().getId().equals(idAzienda)) && u.getTipoOperazione().equals(StrutturaUnificata.TipoUnificazione.FUSIONE))
            .collect(Collectors.groupingBy(su -> su.getIdStrutturaDestinazione().getIdCasella()));
    }

    private Map<String, List<? extends Operation<DatiRibaltoneInterface>>> buildOperationsStrutture(
        List<DatiDaImportareStruttura> struttureDaImportare,
        List<DatiImportatiStruttura> struttureImportate,
        Map<String, Integer> indexStruttureImportate,
        Map<String, Integer> indexIdCasellaPartenzaTrasformazioni
    ) throws RibaltoneHttpException {
        log.info("Faccio il build delle operations strutture");
        List<OperationStruttura> operationStrutturaList = new ArrayList<>();
        List<OperationTrasformazione> operationTrasformazioneList = new ArrayList<>();
        Map<String, List<? extends Operation<DatiRibaltoneInterface>>> mapToReturn = new HashMap<>();
        EntityManager entityManager = repositoryFactory.getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QStruttura qStruttura = QStruttura.struttura;

        this.indexStruttureDaImportare = RibaltoneUtils.generateIndex(struttureDaImportare, DatiDaImportareStruttura::getKey);
        //capiamo i cambi di padre
        this.struttureChiuse = 0;

        Integer totaleStruttureDaImportare = struttureDaImportare.size();
        Integer strutturaNumero = 0;
        for (DatiDaImportareStruttura daImportareStruttura : struttureDaImportare) {
            Integer posizione = indexStruttureImportate.get(daImportareStruttura.getKey());
            strutturaNumero += 1;
            log.info("siamo a " + strutturaNumero + " su " + totaleStruttureDaImportare);
            log.info(ZonedDateTime.now().toString());
            log.info("id_casella " + daImportareStruttura.getIdCasella());
            if (posizione != null) {
                DatiDaImportareTrasformazione trasf = new DatiDaImportareTrasformazione();
                trasf.setCodiceAzienda(daImportareStruttura.getCodiceAzienda());
                trasf.setCodiceEnte(daImportareStruttura.getCodiceEnte());
                trasf.setDataTrasformazione(ZonedDateTime.now());
                log.info("inizio query");
                Struttura strutturaVecchia = queryFactory
                    .select(qStruttura)
                    .from(qStruttura)
                    .where(qStruttura.idAzienda.id.eq(daImportareStruttura.getIdAzienda())
                        .and(qStruttura.attiva)
                        .and(qStruttura.idCasella.eq(daImportareStruttura.getIdCasella()))
                    ).fetchOne();
                log.info("fine query");
                if (strutturaVecchia != null) {
                    trasf.setDatainPartenza(strutturaVecchia.getDataAttivazione());
                } else {
                    throw new RibaltoneHttpException("rilevata struttura da importare già importata ma non attiva! id casella:" + daImportareStruttura.getIdCasella().toString());
                }
                trasf.setIdCasellaPartenza(daImportareStruttura.getIdCasella());
                trasf.setDataoraOper(ZonedDateTime.now());

                if (struttureImportate.get(posizione).getIdPadre() != null && !struttureImportate.get(posizione).getIdPadre().equals(daImportareStruttura.getIdPadre())) {
                    //cambio di padre

                    DatiDaImportareStruttura casellaPadre = struttureDaImportare.get(indexStruttureDaImportare.get(daImportareStruttura.getIdPadre().toString()));
                    DatiImportatiStruttura casellaPadreVecchio = struttureImportate.get(indexStruttureImportate.get(struttureImportate.get(posizione).getIdPadre().toString()));
                    HashMap<String, String> descrizioniAggiuntive = new HashMap<>();
                    descrizioniAggiuntive.put("descrizioneCasellaPadreNuovo", casellaPadre.getDescrizione());
                    descrizioniAggiuntive.put("descrizioneCasellaPadreVecchio", casellaPadreVecchio.getDescrizione());
                    descrizioniAggiuntive.put("idCasellaPadreNuovo", casellaPadre.getIdCasella().toString());
                    descrizioniAggiuntive.put("idCasellaPadreVecchio", casellaPadreVecchio.getIdCasella().toString());
                    descrizioniAggiuntive.put("descrizioneCasella", daImportareStruttura.getDescrizione());
                    operationStrutturaList.add(new OperationStruttura(Operation.Azione.CAMBIO_PADRE, daImportareStruttura, repositoryFactory.getEntityManager(), descrizioniAggiuntive));
                    trasf.setMotivo("T");
                    operationTrasformazioneList.add(new OperationTrasformazione(Operation.Azione.CAMBIO_PADRE, trasf, repositoryFactory.getEntityManager(), descrizioniAggiuntive));
                    //sto trasferendo una struttura unificata?
                    log.info("inizio manageOperationUnificazioniStruttura");
                    manageOperationUnificazioniStruttura(daImportareStruttura, Operation.Azione.CAMBIO_PADRE, queryFactory);
                    log.info("fine manageOperationUnificazioniStruttura");
                } else if (!WordUtils.capitalizeFully(struttureImportate.get(posizione).getDescrizione().trim()).equals(daImportareStruttura.getDescrizione())) {
                    //rinomina
                    HashMap<String, String> descrizioniAggiuntive = new HashMap<>();
                    descrizioniAggiuntive.put("descrizioneCasellaVecchia", struttureImportate.get(posizione).getDescrizione());
                    descrizioniAggiuntive.put("descrizioneCasellaNuova", daImportareStruttura.getDescrizione());

                    operationStrutturaList.add(new OperationStruttura(Operation.Azione.RINOMINA, daImportareStruttura, repositoryFactory.getEntityManager(), descrizioniAggiuntive));
                    trasf.setMotivo("R");
                    operationTrasformazioneList.add(new OperationTrasformazione(Operation.Azione.RINOMINA, trasf, repositoryFactory.getEntityManager(), descrizioniAggiuntive));
                    //sto rinominando una struttura unificata?
                    log.info("inizio manageOperationUnificazioniStruttura rinomina");
                    manageOperationUnificazioniStruttura(daImportareStruttura, Operation.Azione.RINOMINA, queryFactory);
                    log.info("fine manageOperationUnificazioniStruttura rinomina");
                } else {
                    //non è successo nulla è come era prima
                }
            } else {
                //allora è una nuova
                DatiDaImportareStruttura casellaPadre = struttureDaImportare.get(indexStruttureDaImportare.get(daImportareStruttura.getIdPadre().toString()));
                HashMap<String, String> descrizioniAggiuntive = new HashMap<>();
                descrizioniAggiuntive.put("descrizioneCasellaPadre", casellaPadre.getDescrizione());
                operationStrutturaList.add(new OperationStruttura(Operation.Azione.INSERT, daImportareStruttura, repositoryFactory.getEntityManager(), descrizioniAggiuntive));

                //devo controllare che nella gerarchia precedente arrivo ad avere un padre che fa parte di una unificazione
                log.info("inizio manageOperationUnificazioniStruttura nuova");
                manageOperationUnificazioniStruttura(daImportareStruttura, Operation.Azione.INSERT, queryFactory);
                log.info("fine manageOperationUnificazioniStruttura nuova");
                //(mi salvo anche quante ne ho aperto per capire se è una cosa coerente o c'è un grave errore sulla fonte dati)
                this.struttureChiuse--;
            }
        }
        //capire le chiusure (mi salvo anche quante ne ho da chiudere per capire se è una cosa coerente o c'è un grave errore sulla fonte dati)
        log.info("inizio a capire strutture chiuse");
        for (DatiImportatiStruttura strutturaImportata : struttureImportate) {
            if (!indexStruttureDaImportare.containsKey(strutturaImportata.getKey())
                && !indexIdCasellaPartenzaTrasformazioni.containsKey(strutturaImportata.getIdCasella().toString())) {
                log.info("struttura importata id casella " + strutturaImportata.getIdCasella().toString() + " id " + strutturaImportata.getId());
                DatiImportatiStruttura casellaPadre = struttureImportate.get(indexStruttureImportate.get(strutturaImportata.getIdPadre().toString()));
                HashMap<String, String> descrizioniAggiuntive = new HashMap<>();
                descrizioniAggiuntive.put("descrizioneCasellaPadre", casellaPadre.getDescrizione());
                operationStrutturaList.add(new OperationStruttura(Operation.Azione.CHIUSURA, strutturaImportata, repositoryFactory.getEntityManager(), descrizioniAggiuntive));
                this.struttureChiuse++;
                //sto chiudendo una struttura unificata?
                manageOperationUnificazioniStruttura(strutturaImportata, Operation.Azione.CHIUSURA, queryFactory);
            }
            if (!indexStruttureDaImportare.containsKey(strutturaImportata.getKey())
                && indexIdCasellaPartenzaTrasformazioni.containsKey(strutturaImportata.getIdCasella().toString())) {
                manageOperationUnificazioniStruttura(strutturaImportata, Operation.Azione.CONFLUENZA, queryFactory);
            }
        }
        log.info("fine strutture chiuse");

        mapToReturn.put("strutture", operationStrutturaList);
        mapToReturn.put("trasformazioni", operationTrasformazioneList);

        return mapToReturn;
    }

    private List<OperationAppartenente> buildedOperationsAppartenenti(
        List<DatiDaImportareAppartenente> appartenentiDaImportare,
        List<DatiImportatiAppartenente> appartenentiImportati,
        Map<String, Integer> indexAppartenentiImportati,
        List<DatiImportatiStruttura> struttureImportateList,
        List<DatiDaImportareStruttura> struttureDaImportareList,
        JPAQueryFactory queryFactory
    ) {
        Integer totaleAppartenentiDaImportare = appartenentiDaImportare.size();
        Integer afferenzaNumero = 0;
        log.info("Faccio il build delle operations appartenenti");
        List<OperationAppartenente> operationAppartenentiList = new ArrayList<>();
        //prendo in considerazione tutte le modifiche e gli inserimenti dei nuovi utenti
        Map<String, Integer> indexIdCasellaStruttureImportate = RibaltoneUtils.generateIndex(struttureImportateList, DatiImportatiStruttura::getIdCasella);

        Map<String, Integer> indexIdCasellaStruttureDaImportare = RibaltoneUtils.generateIndex(struttureDaImportareList, DatiDaImportareStruttura::getIdCasella);
        Map<String, Integer> indexDaImportare = RibaltoneUtils.generateIndex(appartenentiDaImportare, DatiDaImportareAppartenente::getKey);
        for (DatiDaImportareAppartenente datiDaImportareAppartenente : appartenentiDaImportare) {
            log.info("siamo a " + afferenzaNumero + " su " + totaleAppartenentiDaImportare);
            afferenzaNumero += 1;
            Integer posizione = indexAppartenentiImportati.get(datiDaImportareAppartenente.getKey());
            log.info("sto controllando le operation per " + datiDaImportareAppartenente.getCodiceFiscale() + " su casella " + datiDaImportareAppartenente.getIdCasella());
            Operation.Azione azione = Operation.Azione.INSERT;
            List<String> editString = new ArrayList();
            if (posizione != null) {
                if (!appartenentiImportati.get(posizione).getResponsabile().equals(datiDaImportareAppartenente.getResponsabile())) {
                    editString.add("responsabile");
                    azione = Operation.Azione.EDIT;
                }
                if (!WordUtils.capitalizeFully(appartenentiImportati.get(posizione).getCognome()).equals(WordUtils.capitalizeFully(datiDaImportareAppartenente.getCognome()))) {
                    editString.add("cognome");
                    azione = Operation.Azione.EDIT;
                }
                if (!WordUtils.capitalizeFully(appartenentiImportati.get(posizione).getNome()).equals(WordUtils.capitalizeFully(datiDaImportareAppartenente.getNome()))) {
                    editString.add("nome");
                    azione = Operation.Azione.EDIT;
                }
                if (!appartenentiImportati.get(posizione).getTipoAppartenenza().equals(datiDaImportareAppartenente.getTipoAppartenenza())) {
                    editString.add("afferenza");
                    azione = Operation.Azione.EDIT;
                }
//                if (!appartenentiImportati.get(posizione).getCodiceMatricola().equals(datiDaImportareAppartenente.getCodiceMatricola())) {
//                    editString.add("codice_matricola");
//                    azione = Operation.Azione.EDIT;
//                }
//                if (appartenentiImportati.get(posizione).getDataAssunzione() != null && !appartenentiImportati.get(posizione).getDataAssunzione().equals(datiDaImportareAppartenente.getDataAssunzione())) {
//                    editString.add("dataAssunzione");
//                    azione = Operation.Azione.EDIT;
//                }
//                if (appartenentiImportati.get(posizione).getDataDimissione() != null && !appartenentiImportati.get(posizione).getDataDimissione().equals(datiDaImportareAppartenente.getDataDimissione())) {
//                    editString.add("dataDimissione");
//                    azione = Operation.Azione.EDIT;
//                }
                if (appartenentiImportati.get(posizione).getUsername() != null && datiDaImportareAppartenente.getUsername() != null && !appartenentiImportati.get(posizione).getUsername().equals(datiDaImportareAppartenente.getUsername())) {
                    editString.add("username");
                    azione = Operation.Azione.EDIT;
                }
                if (azione.equals(Operation.Azione.EDIT)) {
                    DatiImportatiStruttura struttura = struttureImportateList.get(indexIdCasellaStruttureImportate.get(datiDaImportareAppartenente.getIdCasella().toString()));
                    HashMap<String, String> descrizioniAggiuntive = new HashMap<>();
                    descrizioniAggiuntive.put("descrizioneCasella", struttura.getDescrizione());
                    operationAppartenentiList.add(new OperationAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager(), editString, descrizioniAggiuntive));
                    log.info("INIIZO is utente unificato?");
                    Boolean inerenteAReplica = false;
                    if (!idCaselleReplicateMap.containsKey(datiDaImportareAppartenente.getIdCasella())) {
                        List<StrutturaUnificata> coinvoltoInReplicaFutura = isCoinvoltoInReplica(struttura, mappaReplicheStruttureDaImportare, DirezioneReplica.FUTURO);
                        List<StrutturaUnificata> coinvoltoInReplicapassata = isCoinvoltoInReplica(struttura, mappaReplicheStruttureImportate, DirezioneReplica.PASSATO);

                        if (coinvoltoInReplicaFutura != null || coinvoltoInReplicapassata != null) {
                            operationsUnificazioneAppartenente.add(new OperationUnificazioneAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager(), idCaselleReplicateMap.get(datiDaImportareAppartenente.getIdCasella()), null));
                        } else {
                            log.info("marchio la casella con id_casella " + struttura.getIdCasella() + " come NON coinvolta in replica");
                            idCaselleNonReplicateSet.add(struttura.getIdCasella());
                        }
                    } else {
                        inerenteAReplica = true;
                    }

                    if (inerenteAReplica) {
                        operationsUnificazioneAppartenente.add(new OperationUnificazioneAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager(), idCaselleReplicateMap.get(datiDaImportareAppartenente.getIdCasella()), null));
                    } else if (isCoinvoltoInFusione(datiDaImportareAppartenente.getIdCasella(), mappaFusioniPerIdCasellaSorgente, mappaFusioniPerIdCasellaDestinazione)) {
                        List<StrutturaUnificata> strutturaUnificataList = mappaFusioniPerIdCasellaSorgente.get(datiDaImportareAppartenente.getIdCasella()) != null ? mappaFusioniPerIdCasellaSorgente.get(datiDaImportareAppartenente.getIdCasella()) : mappaFusioniPerIdCasellaDestinazione.get(datiDaImportareAppartenente.getIdCasella());
                        UnificazionePair unificazionePair = new UnificazionePair(StrutturaUnificata.TipoUnificazione.FUSIONE, strutturaUnificataList, DirezioneReplica.FUTURO);
                        operationsUnificazioneAppartenente.add(new OperationUnificazioneAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager(), unificazionePair, null));
                    }
                    log.info("FINE is utente unificato?");
                }
            } else {
//             se posizione è null allora non ho una riga tra le importate che mi rappresenta l'appartenente'

                String descrizione;
                if (indexIdCasellaStruttureImportate.get(datiDaImportareAppartenente.getIdCasella().toString()) == null) {
                    DatiDaImportareStruttura strutturaDaImportare = struttureDaImportareList.get(indexIdCasellaStruttureDaImportare.get(datiDaImportareAppartenente.getIdCasella().toString()));
                    descrizione = strutturaDaImportare.getDescrizione();
                } else {
                    DatiImportatiStruttura struttura = struttureImportateList.get(indexIdCasellaStruttureImportate.get(datiDaImportareAppartenente.getIdCasella().toString()));
                    descrizione = struttura.getDescrizione();
                }
                //non ho fatto bene la comprensione delle repliche
                log.info("INIZIO is utente unificato?");
                DatiDaImportareStruttura struttura = datiDaImportare.getStruttureDaImportare().get(indexStruttureDaImportare.get(datiDaImportareAppartenente.getIdCasella().toString()));
                if (idCaselleReplicateMap.containsKey(datiDaImportareAppartenente.getIdCasella())
                    || isCoinvoltoInReplica(struttura, mappaReplicheStruttureDaImportare, DirezioneReplica.FUTURO) != null
                    || isCoinvoltoInReplica(struttura, mappaReplicheStruttureImportate, DirezioneReplica.PASSATO) != null) {
                    operationsUnificazioneAppartenente.add(new OperationUnificazioneAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager(), idCaselleReplicateMap.get(datiDaImportareAppartenente.getIdCasella()), null));
                } else if (isCoinvoltoInFusione(datiDaImportareAppartenente.getIdCasella(), mappaFusioniPerIdCasellaSorgente, mappaFusioniPerIdCasellaDestinazione)) {
                    List<StrutturaUnificata> strutturaUnificataList = mappaFusioniPerIdCasellaSorgente.get(datiDaImportareAppartenente.getIdCasella()) != null ? mappaFusioniPerIdCasellaSorgente.get(datiDaImportareAppartenente.getIdCasella()) : mappaFusioniPerIdCasellaDestinazione.get(datiDaImportareAppartenente.getIdCasella());
                    //Pair<StrutturaUnificata.TipoUnificazione, List<StrutturaUnificata>> pair = Pair.of(StrutturaUnificata.TipoUnificazione.FUSIONE, strutturaUnificataList);
                    UnificazionePair pair = new UnificazionePair(StrutturaUnificata.TipoUnificazione.FUSIONE, strutturaUnificataList, DirezioneReplica.FUTURO);
                    operationsUnificazioneAppartenente.add(new OperationUnificazioneAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager(), pair, null));
                } else if (!idCaselleReplicateMap.containsKey(datiDaImportareAppartenente.getIdCasella())) {
                    log.info("marchio la casella con id " + struttura.getIdCasella() + " come NON coinvolta in replica");
                    idCaselleNonReplicateSet.add(struttura.getIdCasella());
                }
                log.info("FINE is utente unificato?");
                HashMap<String, String> descrizioniAggiuntive = new HashMap<>();
                descrizioniAggiuntive.put("nomeCasella", descrizione);
                operationAppartenentiList.add(new OperationAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager(), editString, descrizioniAggiuntive));
                this.utentiStrutturaChiusi--;
            }
        }
        //ora pensiamo a tutte le chiusure

        for (DatiImportatiAppartenente appartenenteImportato : appartenentiImportati) {
            if (indexDaImportare != null && !indexDaImportare.isEmpty() && !indexDaImportare.containsKey(appartenenteImportato.getKey())) {
                log.info("appartenenteImportato con id " + appartenenteImportato.getId());
                DatiImportatiStruttura struttura = struttureImportateList.get(
                    indexIdCasellaStruttureImportate.get(
                        appartenenteImportato.getIdCasella().toString()
                    )
                );
                HashMap<String, String> descrizioniAggiuntive = new HashMap<>();
                descrizioniAggiuntive.put("nomeCasella", struttura.getDescrizione());
                operationAppartenentiList.add(new OperationAppartenente(Operation.Azione.CHIUSURA, appartenenteImportato, repositoryFactory.getEntityManager(), null, descrizioniAggiuntive));
                this.utentiStrutturaChiusi++;
                log.info("INIZIO is utente unificato?");
                if (idCaselleReplicateMap.containsKey(appartenenteImportato.getIdCasella())
                    || isCoinvoltoInReplica(struttura, mappaReplicheStruttureDaImportare, DirezioneReplica.FUTURO) != null
                    || isCoinvoltoInReplica(struttura, mappaReplicheStruttureImportate, DirezioneReplica.PASSATO) != null) {
                    operationsUnificazioneAppartenente.add(new OperationUnificazioneAppartenente(Operation.Azione.CHIUSURA, appartenenteImportato, repositoryFactory.getEntityManager(), idCaselleReplicateMap.get(appartenenteImportato.getIdCasella()), null));
                } else if (isCoinvoltoInFusione(appartenenteImportato.getIdCasella(), mappaFusioniPerIdCasellaSorgente, mappaFusioniPerIdCasellaDestinazione)) {
                    List<StrutturaUnificata> strutturaUnificataList = mappaFusioniPerIdCasellaSorgente.get(appartenenteImportato.getIdCasella()) != null ? mappaFusioniPerIdCasellaSorgente.get(appartenenteImportato.getIdCasella()) : mappaFusioniPerIdCasellaDestinazione.get(appartenenteImportato.getIdCasella());
                    //Pair<StrutturaUnificata.TipoUnificazione, List<StrutturaUnificata>> paira = Pair.of(StrutturaUnificata.TipoUnificazione.FUSIONE, strutturaUnificataList);
                    UnificazionePair pair = new UnificazionePair(StrutturaUnificata.TipoUnificazione.FUSIONE, strutturaUnificataList, DirezioneReplica.PASSATO);
                    operationsUnificazioneAppartenente.add(new OperationUnificazioneAppartenente(Operation.Azione.CHIUSURA, appartenenteImportato, repositoryFactory.getEntityManager(), pair, null));
                } else if (!idCaselleReplicateMap.containsKey(appartenenteImportato.getIdCasella())) {
                    log.info("marchio la casella con id " + struttura.getIdCasella() + " come NON coinvolta in replica");
                    idCaselleNonReplicateSet.add(struttura.getIdCasella());
                }
                log.info("FINE is utente unificato?");
            }
        }
        return operationAppartenentiList;
    }

    private List<OperationAnagrafica> buildedOperationsAnagrafiche(List<DatiDaImportareAnagrafica> anagraficheDaImportare, List<DatiImportatiAnagrafica> anagraficheImportate, Map<String, Integer> indexAnagraficheImportate) throws JsonProcessingException {
        List<OperationAnagrafica> operationAnagraficheList = new ArrayList<>();
        log.info("Faccio il build delle operations anagrafiche");
        for (DatiDaImportareAnagrafica datiDaImportareAnagrafica : anagraficheDaImportare) {

            Operation.Azione azione = Operation.Azione.INSERT;
            Boolean salva = true;
            Integer posizione = indexAnagraficheImportate.get(datiDaImportareAnagrafica.getKey());

            if (posizione != null) {
                if (!WordUtils.capitalizeFully(anagraficheImportate.get(posizione).getCognome()).equals(WordUtils.capitalizeFully(datiDaImportareAnagrafica.getCognome()))
                    || !WordUtils.capitalizeFully(anagraficheImportate.get(posizione).getNome()).equals(WordUtils.capitalizeFully(datiDaImportareAnagrafica.getNome()))
                    || !Objects.equals(anagraficheImportate.get(posizione).getEmail(), datiDaImportareAnagrafica.getEmail())) {
                    azione = Operation.Azione.EDIT;
                } else {
                    salva = false;
                }
            }

            if (salva) {
                if (Operation.Azione.INSERT.equals(azione) && !StringUtils.hasText(datiDaImportareAnagrafica.getEmail())) {
                } else {
                    operationAnagraficheList.add(new OperationAnagrafica(azione, datiDaImportareAnagrafica, repositoryFactory.getEntityManager(), null));
                }
            }
        }
        return operationAnagraficheList;
    }

    private Map<String, List<? extends Operation<DatiRibaltoneInterface>>> buildedOperationsTrasformazioni(List<DatiDaImportareTrasformazione> trasformazioniDaImportare, Integer ultimoProgressivoRiga) {
        log.info("Faccio il build delle operations trasformazioni");
        List<OperationTrasformazione> operationTrasformazioneList = new ArrayList<>();
//        List<OperationUnificazione> operationUnificazioneList = new ArrayList<>();
        Map<String, List<? extends Operation<DatiRibaltoneInterface>>> mapToReturn = new HashMap<>();
//        EntityManager entityManager = repositoryFactory.getEntityManager();
//        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
//        QStruttura qStruttura = QStruttura.struttura;
//        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
        for (DatiDaImportareTrasformazione datiDaImportareTrasformazione : trasformazioniDaImportare) {
            if (datiDaImportareTrasformazione.getProgressivoRiga() > ultimoProgressivoRiga) {
                HashMap<String, String> descrizioniAggiuntive = new HashMap<>();
                if (datiDaImportareTrasformazione.getMotivo().equalsIgnoreCase("X")) {
                    descrizioniAggiuntive.put("nomeCasellaSorgente", struttureImportate.get(indexStruttureImportate.get(datiDaImportareTrasformazione.getIdCasellaPartenza().toString())).getDescrizione());
                    descrizioniAggiuntive.put("nomeCasellaDestinazione", struttureImportate.get(indexStruttureImportate.get(datiDaImportareTrasformazione.getIdCasellaArrivo().toString())).getDescrizione());

                } else {
                    descrizioniAggiuntive.put("nomeCasellaSorgente", datiDaImportare.getStruttureDaImportare().get(indexStruttureDaImportare.get(datiDaImportareTrasformazione.getIdCasellaPartenza().toString())).getDescrizione());
                    descrizioniAggiuntive.put("nomeCasellaDestinazione", datiDaImportare.getStruttureDaImportare().get(indexStruttureDaImportare.get(datiDaImportareTrasformazione.getIdCasellaArrivo().toString())).getDescrizione());
                }

                operationTrasformazioneList.add(new OperationTrasformazione(Operation.Azione.CONFLUENZA, datiDaImportareTrasformazione, repositoryFactory.getEntityManager(), descrizioniAggiuntive));

//         Probabilmente non serve piu perche ho gia capito la confluenza ciclando le strutture
//                List<StrutturaUnificata> struttureUnificateList = queryFactory
//                    .select(qStrutturaUnificata)
//                    .from(qStruttura)
//                    .join(qStrutturaUnificata)
//                    .on(qStrutturaUnificata.idStrutturaSorgente.id.eq(qStruttura.id)
//                        .or(qStrutturaUnificata.idStrutturaDestinazione.id.eq(qStruttura.id))
//                    )
//                    .where(qStruttura.idCasella.eq(datiDaImportareTrasformazione.getIdCasellaPartenza()).and(qStruttura.attiva)).fetch();
//                if (struttureUnificateList != null && !struttureUnificateList.isEmpty()) {
//                    for (StrutturaUnificata su : struttureUnificateList) {
//                        operationUnificazioneList.add(new OperationUnificazione(Operation.Azione.CHIUSURA, UnificazioneEseguita.buildUnificazioneEseguita(su), entityManager));
//                    }
//                }
            }
        }
        mapToReturn.put("trasformazioni", operationTrasformazioneList);
//        mapToReturn.put("unificazioni", operationUnificazioneList);

        return mapToReturn;
    }

    public boolean isQuantitaDatiOk() throws RibaltoneHttpException {

        Integer nStruttureImportate = this.struttureImportate.size();
        Integer nStruttureDaImportare = nStruttureImportate - this.struttureChiuse;
        if (nStruttureImportate > 0) {
            Integer percentualeStruttureValide = 100 - ((nStruttureDaImportare * 100) / nStruttureImportate);
            if (!(tolleranzaStrutture >= percentualeStruttureValide)) {
                throw new RibaltoneHttpException("Errore nell'importazione bloccante. Il numero di strutture che si vogliono importare non supera la tolleranza minima richiesta");
            }
        }

        Integer nAppartenentiImportatiPrecedenti = this.appartenentiImportati.size();
        log.info("nAppartenentiImportati" + nAppartenentiImportatiPrecedenti);
        Integer nAppartenentiTotaliAggiornati = nAppartenentiImportatiPrecedenti - this.utentiStrutturaChiusi;
        log.info("this.utentiStrutturaChiusi" + this.utentiStrutturaChiusi);
        log.info("nAppartenentiAttivi" + nAppartenentiTotaliAggiornati);
        if (nAppartenentiImportatiPrecedenti > 0) {
            log.info("Integer percentualeAppartenentiValidi = (nAppartenenti * 100) / nAppartenentiImportati;" + ((nAppartenentiTotaliAggiornati * 100) / nAppartenentiImportatiPrecedenti));
            Integer differenzaPercentualeTraAppartenentiFuturiEPrecedenti = 100 - ((nAppartenentiTotaliAggiornati * 100) / nAppartenentiImportatiPrecedenti);
            log.info("tolleranzaAppartenenti " + tolleranzaAppartenenti + " >= " + differenzaPercentualeTraAppartenentiFuturiEPrecedenti);
            if (!(tolleranzaAppartenenti >= differenzaPercentualeTraAppartenentiFuturiEPrecedenti)) {

                throw new RibaltoneHttpException("Errore nell'importazione bloccante. Il numero di afferenze utente-struttura che si vogliono importare non supera la tolleranza minima richiesta");
            }
        }
        return true;
    }

    //funzione che serve a capire se sono replicato o se ho un antenato che è replicato
    //sia nel vecchio organigramma che nel nuovo
    private List<StrutturaUnificata> isCoinvoltoInReplica(DatiRibaltoneStrutturaInterface struttura,
        Map<Long, Map<OperationsUtils.KeyMapReplica, Object>> mappaUnificazioniPerIdCasellaSorgente,
        DirezioneReplica direzione
    ) {

        if (struttura == null) {
            return null;
        } else if (mappaUnificazioniPerIdCasellaSorgente.containsKey(Long.valueOf(struttura.getIdCasella())) && ((Boolean) mappaUnificazioniPerIdCasellaSorgente.get(Long.valueOf(struttura.getIdCasella())).get(OperationsUtils.KeyMapReplica.HAS_REPLICA))) {
            log.info("coinvolta in replica struttura con id_casella " + struttura.getIdCasella());
            Map<OperationsUtils.KeyMapReplica, Object> mappaUnificazione = mappaUnificazioniPerIdCasellaSorgente.get(Long.valueOf(struttura.getIdCasella()));
            List<StrutturaUnificata> coinvoltoInReplica = (List<StrutturaUnificata>) mappaUnificazione.get(OperationsUtils.KeyMapReplica.REPLICHE);

            UnificazionePair pair = new UnificazionePair(StrutturaUnificata.TipoUnificazione.REPLICA, coinvoltoInReplica, direzione);
            idCaselleReplicateMap.put(struttura.getIdCasella(), pair);
            return coinvoltoInReplica;
        } else {
            log.info("NON coinvolta in replica struttura con id_casella " + struttura.getIdCasella());
            return null;
        }

//        log.info("sono lento");
//        if (struttura == null) {
//            return null;
//        } else if (mappaUnificazioniPerIdCasellaSorgente.containsKey(Long.valueOf(struttura.getIdCasella()))) {
//            log.info("sono lento nuovo controllo idcasellanonreplicate " + struttura.getIdCasella());
//            return null;
//        } else if (mappaUnificazioniPerIdCasellaSorgente.containsKey(struttura.getIdCasella())) {
//            List<StrutturaUnificata> strutturaUnificataList = mappaUnificazioniPerIdCasellaSorgente.get(struttura.getIdCasella());


    ////            Pair<StrutturaUnificata.TipoUnificazione, List<StrutturaUnificata>> pair = Pair.of(StrutturaUnificata.TipoUnificazione.REPLICA, strutturaUnificataList);
//            UnificazionePair pair = new UnificazionePair(StrutturaUnificata.TipoUnificazione.REPLICA, strutturaUnificataList);
//            idCaselleReplicateMap.put(struttura.getIdCasella(), pair);
//            log.info("sono lento1");
//            return mappaUnificazioniPerIdCasellaSorgente.get(struttura.getIdCasella());
//        } else if (struttura.getIdCasellaPadre() != null && !struttura.getIdCasellaPadre().equals(0)) {
//            DatiRibaltoneStrutturaInterface strutturaPadre;
//            log.info("sono lento2");
//            if (direzioneReplica.equals(DirezioneReplica.FUTURO)) {
//                QDatiDaImportareStruttura qDatiDaImportareStruttura = QDatiDaImportareStruttura.datiDaImportareStruttura;
//                strutturaPadre = jPAQueryFactory
//                    .select(qDatiDaImportareStruttura)
//                    .from(qDatiDaImportareStruttura)
//                    .where(qDatiDaImportareStruttura.idCasella.eq(struttura.getIdCasellaPadre())
//                        .and(qDatiDaImportareStruttura.idAzienda.eq(struttura.getIdAzienda()))
//                    ).fetchFirst();
//            } else {
//                QDatiImportatiStruttura qDatiImportatiStruttura = QDatiImportatiStruttura.datiImportatiStruttura;
//                DatiImportatiStruttura strutturaVecchia = jPAQueryFactory
//                    .select(qDatiImportatiStruttura)
//                    .from(qDatiImportatiStruttura)
//                    .where(qDatiImportatiStruttura.idCasella.eq(struttura.getIdCasella())
//                        .and(qDatiImportatiStruttura.idAzienda.eq(struttura.getIdAzienda()))
//                    ).fetchFirst();
//                if (strutturaVecchia == null) {
//                    return null;
//                } else {
//                    strutturaPadre = jPAQueryFactory
//                        .select(qDatiImportatiStruttura)
//                        .from(qDatiImportatiStruttura)
//                        .where(qDatiImportatiStruttura.idCasella.eq(strutturaVecchia.getIdCasellaPadre())
//                            .and(qDatiImportatiStruttura.idAzienda.eq(struttura.getIdAzienda()))
//                        ).fetchFirst();
//                }
//            }
//            log.info("sono lento3");
//            List<StrutturaUnificata> coinvoltoInReplica = isCoinvoltoInReplica(strutturaPadre, mappaUnificazioniPerIdCasellaSorgente, jPAQueryFactory, direzioneReplica);
//
//            if (coinvoltoInReplica != null) {
//                UnificazionePair pair = new UnificazionePair(StrutturaUnificata.TipoUnificazione.REPLICA, coinvoltoInReplica);
//                idCaselleReplicateMap.put(struttura.getIdCasella(), pair);
//            }
//            log.info("sono lento4");
//            return coinvoltoInReplica;
//        } else {
//            log.info("sono lento5");
//            return null;
//        }
    }

    private boolean isCoinvoltoInFusione(Integer idCasella,
        Map<Integer, List<StrutturaUnificata>> mappaFusioniPerIdCasellaSorgente,
        Map<Integer, List<StrutturaUnificata>> mappaFusioniPerIdCasellaDestinazione) {
        return mappaFusioniPerIdCasellaSorgente.get(idCasella) != null || mappaFusioniPerIdCasellaDestinazione.get(idCasella) != null;
    }

    private <T extends DatiRibaltoneStrutturaInterface> void manageOperationUnificazioniStruttura(T struttura, Operation.Azione azione, JPAQueryFactory queryFactory) {

        List<StrutturaUnificata> coinvoltoInReplicheFuture = isCoinvoltoInReplica(struttura, mappaReplicheStruttureDaImportare, DirezioneReplica.FUTURO);
        List<StrutturaUnificata> coinvoltoInReplichePassate = isCoinvoltoInReplica(struttura, mappaReplicheStruttureImportate, DirezioneReplica.PASSATO);
        if (coinvoltoInReplicheFuture == null && coinvoltoInReplichePassate == null) {
            idCaselleNonReplicateSet.add(struttura.getIdCasella());
        }
        if (coinvoltoInReplicheFuture != null && coinvoltoInReplichePassate == null) {
            List<UnificazioneDaGestire> unificazioniEseguite = new ArrayList<>();
            for (StrutturaUnificata strutturaUnificata : coinvoltoInReplicheFuture) {

                log.info("inizio a gestire questa unificazione" + strutturaUnificata.getId() + "con questa struttura " + strutturaUnificata.getIdStruttura());
                unificazioniEseguite.add(UnificazioneDaGestire.buildUnificazioneEseguita(strutturaUnificata));
            }
            operationsUnificazioneStruttura.add(new OperationUnificazioneStruttura(azione, struttura, repositoryFactory.getEntityManager(), StrutturaUnificata.TipoUnificazione.REPLICA, unificazioniEseguite, null, DirezioneReplica.FUTURO));
        } else if (coinvoltoInReplicheFuture == null && coinvoltoInReplichePassate != null) {
            List<UnificazioneDaGestire> unificazioniEseguite = new ArrayList<>();
            for (StrutturaUnificata strutturaUnificata : coinvoltoInReplichePassate) {
                unificazioniEseguite.add(UnificazioneDaGestire.buildUnificazioneEseguita(strutturaUnificata));
            }
            operationsUnificazioneStruttura.add(new OperationUnificazioneStruttura(azione, struttura, repositoryFactory.getEntityManager(), StrutturaUnificata.TipoUnificazione.REPLICA, unificazioniEseguite, null, DirezioneReplica.PASSATO));

        } else if (coinvoltoInReplicheFuture != null && coinvoltoInReplichePassate != null) {
            List<UnificazioneDaGestire> unificazioniEseguite = new ArrayList<>();
            Set<Integer> idUnificazioni = new HashSet<>();

            for (StrutturaUnificata strutturaUnificata : coinvoltoInReplicheFuture) {
                if (!idUnificazioni.contains(strutturaUnificata.getId())) {
                    StrutturaUnificata strutturaUnificataReload = repositoryFactory.getEntityManager().find(StrutturaUnificata.class, strutturaUnificata.getId());
                    //repositoryFactory.getEntityManager().refresh(strutturaUnificataReload);
                    unificazioniEseguite.add(UnificazioneDaGestire.buildUnificazioneEseguita(strutturaUnificataReload));
                    idUnificazioni.add(strutturaUnificata.getId());
                }
            }
//            operationsUnificazioneStruttura.add(new OperationUnificazioneStruttura(azione, struttura, repositoryFactory.getEntityManager(), StrutturaUnificata.TipoUnificazione.REPLICA, unificazioniEseguite, null, DirezioneReplica.FUTURO));

            //unificazioniEseguite = new ArrayList<>();
            for (StrutturaUnificata strutturaUnificata : coinvoltoInReplichePassate) {
                if (!idUnificazioni.contains(strutturaUnificata.getId())) {
                    unificazioniEseguite.add(UnificazioneDaGestire.buildUnificazioneEseguita(strutturaUnificata));
                    idUnificazioni.add(strutturaUnificata.getId());
                }
            }
            operationsUnificazioneStruttura.add(new OperationUnificazioneStruttura(azione, struttura, repositoryFactory.getEntityManager(), StrutturaUnificata.TipoUnificazione.REPLICA, unificazioniEseguite, null, null));

        } else if (isCoinvoltoInFusione(struttura.getIdCasella(), mappaFusioniPerIdCasellaSorgente, mappaFusioniPerIdCasellaDestinazione)) {
            List<UnificazioneDaGestire> unificazioniEseguite = new ArrayList<>();
            List<StrutturaUnificata> strutturaUnificataList = mappaFusioniPerIdCasellaSorgente.containsKey(struttura.getIdCasella()) ? mappaFusioniPerIdCasellaSorgente.get(struttura.getIdCasella()) : mappaFusioniPerIdCasellaDestinazione.get(struttura.getIdCasella());
//            Pair<StrutturaUnificata.TipoUnificazione, List<StrutturaUnificata>> paiar = Pair.of(StrutturaUnificata.TipoUnificazione.FUSIONE, strutturaUnificataList);
            UnificazionePair pair = new UnificazionePair(StrutturaUnificata.TipoUnificazione.FUSIONE, strutturaUnificataList, DirezioneReplica.FUTURO);
            for (StrutturaUnificata strutturaUnificata : strutturaUnificataList) {
                unificazioniEseguite.add(UnificazioneDaGestire.buildUnificazioneEseguita(strutturaUnificata));
            }
            idCaselleReplicateMap.put(struttura.getIdCasella(), pair);
            operationsUnificazioneStruttura.add(new OperationUnificazioneStruttura(azione, struttura, repositoryFactory.getEntityManager(), StrutturaUnificata.TipoUnificazione.FUSIONE, unificazioniEseguite, null, null));
        }
    }
}
