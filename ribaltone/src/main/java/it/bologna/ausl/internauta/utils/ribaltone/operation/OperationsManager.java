package it.bologna.ausl.internauta.utils.ribaltone.operation;

import org.apache.commons.lang3.tuple.Pair;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportare;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneStrutturaInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationUnificazioneAppartenente.UnificazionePair;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.utils.RibaltoneUtils;
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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private Map<Integer, List<StrutturaUnificata>> mappaReplichePerIdCasellaSorgente;
    private Map<Integer, List<StrutturaUnificata>> mappaReplichePerIdCasellaDestinazione;
    private Map<Integer, List<StrutturaUnificata>> mappaFusioniPerIdCasellaSorgente;
    private Map<Integer, List<StrutturaUnificata>> mappaFusioniPerIdCasellaDestinazione;
    private List<OperationUnificazioneStruttura> operationsUnificazioneStruttura = new ArrayList<>();
    private List<OperationUnificazioneAppartenente> operationsUnificazioneAppartenente = new ArrayList<>();
    private Map<Integer, UnificazionePair> idCaselleUnificateMap = new HashMap<>();
    private Map<String, Integer> indexStruttureDaImportare;

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
    }

    /**
     *
     * @return
     *
     * genera tutte le operazioni che sono da fare da queste si possono generare
     * i report per l'utente o si puo proseguire col ribaltone
     */
    public Operations buildOperations() throws RibaltoneHttpException {
        EntityManager entityManager = repositoryFactory.getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        this.popolaMappeUnificazioni(queryFactory);

        Map<String, List<? extends Operation<DatiRibaltoneInterface>>> buildedOperationsStruttureETrasformazione = buildOperationsStrutture(datiDaImportare.getStruttureDaImportare(), this.struttureImportate, this.indexStruttureImportate, RibaltoneUtils.generateIndex(this.datiDaImportare.getTrasformazioniDaImportare(), DatiDaImportareTrasformazione::getIdCasellaPartenza));
        List<OperationStruttura> operationsStrutture = (List<OperationStruttura>) buildedOperationsStruttureETrasformazione.get("strutture");
        List<OperationAppartenente> operationsAppartenenti = buildedOperationsAppartenenti(datiDaImportare.getAppartenentiDaImportare(), this.appartenentiImportati, indexAppartenentiImportati, this.struttureImportate, datiDaImportare.getStruttureDaImportare());
        List<OperationAnagrafica> operationsAnagrafiche = buildedOperationsAnagrafiche(datiDaImportare.getAnagraficheDaImportare(), this.anagraficheImportate, this.indexAnagraficheImportate);
        Map<String, List<? extends Operation<DatiRibaltoneInterface>>> buildedOperationsTrasformazioniUnificazioni = buildedOperationsTrasformazioni(datiDaImportare.getTrasformazioniDaImportare(), this.trasformazioniImportateUltimoProgressivoRiga);
        List<OperationTrasformazione> operationsTraformazioni = (List<OperationTrasformazione>) buildedOperationsTrasformazioniUnificazioni.get("trasformazioni");

        operationsTraformazioni.addAll((Collection<? extends OperationTrasformazione>) buildedOperationsStruttureETrasformazione.get("trasformazioni"));
        return new Operations(operationsStrutture, operationsAppartenenti, operationsAnagrafiche, operationsTraformazioni, operationsUnificazioneStruttura, operationsUnificazioneAppartenente);
    }

    private void popolaMappeUnificazioni(JPAQueryFactory queryFactory) {
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

        // Mappa: idStrutturaSorgente -> lista di StrutturaUnificata
        this.mappaReplichePerIdCasellaSorgente = listaUnificazioni.stream().filter(u -> u.getTipoOperazione().equals(StrutturaUnificata.TipoUnificazione.REPLICA))
            .collect(Collectors.groupingBy(su -> su.getIdStrutturaSorgente().getIdCasella()));

        // Mappa: idStrutturaDestinazione -> lista di StrutturaUnificata
        this.mappaReplichePerIdCasellaDestinazione = listaUnificazioni.stream().filter(u -> u.getTipoOperazione().equals(StrutturaUnificata.TipoUnificazione.REPLICA))
            .collect(Collectors.groupingBy(su -> su.getIdStrutturaDestinazione().getIdCasella()));

        this.mappaFusioniPerIdCasellaSorgente
            = listaUnificazioni.stream().filter(u -> u.getTipoOperazione().equals(StrutturaUnificata.TipoUnificazione.FUSIONE))
                .collect(Collectors.groupingBy(su -> su.getIdStrutturaSorgente().getIdCasella()));

        // Mappa: idStrutturaDestinazione -> lista di StrutturaUnificata
        this.mappaFusioniPerIdCasellaDestinazione = listaUnificazioni.stream().filter(u -> u.getTipoOperazione().equals(StrutturaUnificata.TipoUnificazione.FUSIONE))
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
            if (posizione != null) {
                DatiDaImportareTrasformazione trasf = new DatiDaImportareTrasformazione();
                trasf.setCodiceAzienda(daImportareStruttura.getCodiceAzienda());
                trasf.setCodiceEnte(daImportareStruttura.getCodiceEnte());
                trasf.setDataTrasformazione(ZonedDateTime.now());
                Struttura strutturaVecchia = queryFactory.select(qStruttura).from(qStruttura).where(qStruttura.idAzienda.id.eq(daImportareStruttura.getIdAzienda()).and(qStruttura.attiva.and(qStruttura.idCasella.eq(daImportareStruttura.getIdCasella())))).fetchOne();
                if (strutturaVecchia != null) {
                    trasf.setDatainPartenza(strutturaVecchia.getDataAttivazione());
                } else {
                    throw new RibaltoneHttpException("rilevata trasformazione su struttura non trovata o inesistente! id casella:" + daImportareStruttura.getIdCasella().toString());
                }
                trasf.setIdCasellaPartenza(daImportareStruttura.getIdCasella());
                trasf.setDataoraOper(ZonedDateTime.now());

                if (struttureImportate.get(posizione).getIdPadre() != null && !struttureImportate.get(posizione).getIdPadre().equals(daImportareStruttura.getIdPadre())) {
                    //cambio di padre

                    DatiDaImportareStruttura casellaPadre = struttureDaImportare.get(indexStruttureDaImportare.get(daImportareStruttura.getIdPadre().toString()));
                    DatiImportatiStruttura casellaPadreVecchio = struttureImportate.get(indexStruttureImportate.get(struttureImportate.get(posizione).getIdPadre().toString()));
                    HashMap<String, String> descrizioniAggiuntive = new HashMap<>();
                    descrizioniAggiuntive.put("descrizioneCasellaPadre", casellaPadre.getDescrizione());
                    descrizioniAggiuntive.put("descrizioneCasellaPadreVecchio", casellaPadreVecchio.getDescrizione());
                    operationStrutturaList.add(new OperationStruttura(Operation.Azione.CAMBIO_PADRE, daImportareStruttura, repositoryFactory.getEntityManager(), descrizioniAggiuntive));
                    trasf.setMotivo("T");
                    operationTrasformazioneList.add(new OperationTrasformazione(Operation.Azione.CAMBIO_PADRE, trasf, repositoryFactory.getEntityManager(), descrizioniAggiuntive));
                    //sto trasferendo una struttura unificata?
                    manageOperationUnificazioniStruttura(daImportareStruttura, Operation.Azione.CAMBIO_PADRE, queryFactory);
                } else if (!struttureImportate.get(posizione).getDescrizione().equals(daImportareStruttura.getDescrizione())) {
                    //rinomina
                    HashMap<String, String> descrizioniAggiuntive = new HashMap<>();
                    descrizioniAggiuntive.put("descrizioneCasellaVecchia", struttureImportate.get(posizione).getDescrizione());
                    descrizioniAggiuntive.put("descrizioneCasellaNuova", daImportareStruttura.getDescrizione());

                    operationStrutturaList.add(new OperationStruttura(Operation.Azione.RINOMINA, daImportareStruttura, repositoryFactory.getEntityManager(), descrizioniAggiuntive));
                    trasf.setMotivo("R");
                    operationTrasformazioneList.add(new OperationTrasformazione(Operation.Azione.RINOMINA, trasf, repositoryFactory.getEntityManager(), descrizioniAggiuntive));
                    //sto rinominando una struttura unificata?
                    manageOperationUnificazioniStruttura(daImportareStruttura, Operation.Azione.RINOMINA, queryFactory);
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
                manageOperationUnificazioniStruttura(daImportareStruttura, Operation.Azione.INSERT, queryFactory);
                //(mi salvo anche quante ne ho aperto per capire se è una cosa coerente o c'è un grave errore sulla fonte dati)
                this.struttureChiuse--;
            }
        }
        //capire le chiusure (mi salvo anche quante ne ho da chiudere per capire se è una cosa coerente o c'è un grave errore sulla fonte dati)
        for (DatiImportatiStruttura strutturaImportata : struttureImportate) {
            if (!indexStruttureDaImportare.containsKey(strutturaImportata.getKey())
                && !indexIdCasellaPartenzaTrasformazioni.containsKey(strutturaImportata.getIdCasella().toString())) {
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

        mapToReturn.put("strutture", operationStrutturaList);
        mapToReturn.put("trasformazioni", operationTrasformazioneList);

        return mapToReturn;
    }

    private List<OperationAppartenente> buildedOperationsAppartenenti(
        List<DatiDaImportareAppartenente> appartenentiDaImportare,
        List<DatiImportatiAppartenente> appartenentiImportati,
        Map<String, Integer> indexAppartenentiImportati,
        List<DatiImportatiStruttura> struttureImportateList,
        List<DatiDaImportareStruttura> struttureDaImportareList
    ) {
        log.info("Faccio il build delle operations appartenenti");
        List<OperationAppartenente> operationAppartenentiList = new ArrayList<>();
        //prendo in considerazione tutte le modifiche e gli inserimenti dei nuovi utenti
        Map<String, Integer> indexIdCasellaStruttureImportate = RibaltoneUtils.generateIndex(struttureImportateList, DatiImportatiStruttura::getIdCasella);

        Map<String, Integer> indexIdCasellaStruttureDaImportare = RibaltoneUtils.generateIndex(struttureDaImportareList, DatiDaImportareStruttura::getIdCasella);
        Map<String, Integer> indexDaImportare = RibaltoneUtils.generateIndex(appartenentiDaImportare, DatiDaImportareAppartenente::getKey);
        for (DatiDaImportareAppartenente datiDaImportareAppartenente : appartenentiDaImportare) {
            Integer posizione = indexAppartenentiImportati.get(datiDaImportareAppartenente.getKey());
            log.info("sto controllando le operation per " + datiDaImportareAppartenente.getCodiceFiscale() + " su casella " + datiDaImportareAppartenente.getIdCasella());
            Operation.Azione azione = Operation.Azione.INSERT;
            List<String> editString = new ArrayList();
            if (posizione != null) {
                if (!appartenentiImportati.get(posizione).getResponsabile().equals(datiDaImportareAppartenente.getResponsabile())) {
                    editString.add("responsabile");
                    azione = Operation.Azione.EDIT;
                }
                if (!appartenentiImportati.get(posizione).getCognome().equals(datiDaImportareAppartenente.getCognome())) {
                    editString.add("cognome");
                    azione = Operation.Azione.EDIT;
                }
                if (!appartenentiImportati.get(posizione).getNome().equals(datiDaImportareAppartenente.getNome())) {
                    editString.add("nome");
                    azione = Operation.Azione.EDIT;
                }
                if (!appartenentiImportati.get(posizione).getTipoAppartenenza().equals(datiDaImportareAppartenente.getTipoAppartenenza())) {
                    editString.add("afferenza");
                    azione = Operation.Azione.EDIT;
                }
                if (!appartenentiImportati.get(posizione).getCodiceMatricola().equals(datiDaImportareAppartenente.getCodiceMatricola())) {
                    editString.add("codice_matricola");
                    azione = Operation.Azione.EDIT;
                }
                if (appartenentiImportati.get(posizione).getDataAssunzione() != null && !appartenentiImportati.get(posizione).getDataAssunzione().equals(datiDaImportareAppartenente.getDataAssunzione())) {
                    editString.add("dataAssunzione");
                    azione = Operation.Azione.EDIT;
                }
                if (appartenentiImportati.get(posizione).getDataDimissione() != null && !appartenentiImportati.get(posizione).getDataDimissione().equals(datiDaImportareAppartenente.getDataDimissione())) {
                    editString.add("dataDimissione");
                    azione = Operation.Azione.EDIT;
                }
                if (appartenentiImportati.get(posizione).getUsername() != null && !appartenentiImportati.get(posizione).getUsername().equals(datiDaImportareAppartenente.getUsername())) {
                    editString.add("username");
                    azione = Operation.Azione.EDIT;
                }
                if (azione.equals(Operation.Azione.EDIT)) {
                    DatiImportatiStruttura struttura = struttureImportateList.get(indexIdCasellaStruttureImportate.get(datiDaImportareAppartenente.getIdCasella().toString()));
                    HashMap<String, String> descrizioniAggiuntive = new HashMap<>();
                    descrizioniAggiuntive.put("descrizioneCasella", struttura.getDescrizione());
                    operationAppartenentiList.add(new OperationAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager(), editString, descrizioniAggiuntive));
                    if (idCaselleUnificateMap.containsKey(datiDaImportareAppartenente.getIdCasella())) {
                        operationsUnificazioneAppartenente.add(new OperationUnificazioneAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager(), idCaselleUnificateMap.get(datiDaImportareAppartenente.getIdCasella()), null));
                    } else if (isCoinvoltoInFusione(datiDaImportareAppartenente.getIdCasella(), mappaFusioniPerIdCasellaSorgente, mappaFusioniPerIdCasellaDestinazione)) {
                        List<StrutturaUnificata> strutturaUnificataList = mappaFusioniPerIdCasellaSorgente.get(datiDaImportareAppartenente.getIdCasella()) != null ? mappaFusioniPerIdCasellaSorgente.get(datiDaImportareAppartenente.getIdCasella()) : mappaFusioniPerIdCasellaDestinazione.get(datiDaImportareAppartenente.getIdCasella());
                        UnificazionePair unificazionePair = new UnificazionePair(StrutturaUnificata.TipoUnificazione.FUSIONE, strutturaUnificataList);
                        operationsUnificazioneAppartenente.add(new OperationUnificazioneAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager(), unificazionePair, null));
                    }

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
                if (idCaselleUnificateMap.containsKey(datiDaImportareAppartenente.getIdCasella())) {
                    operationsUnificazioneAppartenente.add(new OperationUnificazioneAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager(), idCaselleUnificateMap.get(datiDaImportareAppartenente.getIdCasella()), null));
                } else if (isCoinvoltoInFusione(datiDaImportareAppartenente.getIdCasella(), mappaFusioniPerIdCasellaSorgente, mappaFusioniPerIdCasellaDestinazione)) {
                    List<StrutturaUnificata> strutturaUnificataList = mappaFusioniPerIdCasellaSorgente.get(datiDaImportareAppartenente.getIdCasella()) != null ? mappaFusioniPerIdCasellaSorgente.get(datiDaImportareAppartenente.getIdCasella()) : mappaFusioniPerIdCasellaDestinazione.get(datiDaImportareAppartenente.getIdCasella());
                    //Pair<StrutturaUnificata.TipoUnificazione, List<StrutturaUnificata>> pair = Pair.of(StrutturaUnificata.TipoUnificazione.FUSIONE, strutturaUnificataList);
                    UnificazionePair pair = new UnificazionePair(StrutturaUnificata.TipoUnificazione.FUSIONE, strutturaUnificataList);
                    operationsUnificazioneAppartenente.add(new OperationUnificazioneAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager(), pair, null));
                }
                HashMap<String, String> descrizioniAggiuntive = new HashMap<>();
                descrizioniAggiuntive.put("nomeCasella", descrizione);
                operationAppartenentiList.add(new OperationAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager(), editString, descrizioniAggiuntive));
                this.utentiStrutturaChiusi--;
            }
        }
        //ora pensiamo a tutte le chiusure

        for (DatiImportatiAppartenente appartenenteImportato : appartenentiImportati) {
            if (indexDaImportare != null && !indexDaImportare.isEmpty() && !indexDaImportare.containsKey(appartenenteImportato.getKey())) {
                DatiImportatiStruttura struttura = struttureImportateList.get(indexIdCasellaStruttureImportate.get(appartenenteImportato.getIdCasella().toString()));
                HashMap<String, String> descrizioniAggiuntive = new HashMap<>();
                descrizioniAggiuntive.put("nomeCasella", struttura.getDescrizione());
                operationAppartenentiList.add(new OperationAppartenente(Operation.Azione.CHIUSURA, appartenenteImportato, repositoryFactory.getEntityManager(), null, descrizioniAggiuntive));
                this.utentiStrutturaChiusi++;
                if (idCaselleUnificateMap.containsKey(appartenenteImportato.getIdCasella())) {
                    operationsUnificazioneAppartenente.add(new OperationUnificazioneAppartenente(Operation.Azione.CHIUSURA, appartenenteImportato, repositoryFactory.getEntityManager(), idCaselleUnificateMap.get(appartenenteImportato.getIdCasella()), null));
                } else if (isCoinvoltoInFusione(appartenenteImportato.getIdCasella(), mappaFusioniPerIdCasellaSorgente, mappaFusioniPerIdCasellaDestinazione)) {
                    List<StrutturaUnificata> strutturaUnificataList = mappaFusioniPerIdCasellaSorgente.get(appartenenteImportato.getIdCasella()) != null ? mappaFusioniPerIdCasellaSorgente.get(appartenenteImportato.getIdCasella()) : mappaFusioniPerIdCasellaDestinazione.get(appartenenteImportato.getIdCasella());
                    //Pair<StrutturaUnificata.TipoUnificazione, List<StrutturaUnificata>> paira = Pair.of(StrutturaUnificata.TipoUnificazione.FUSIONE, strutturaUnificataList);
                    UnificazionePair pair = new UnificazionePair(StrutturaUnificata.TipoUnificazione.FUSIONE, strutturaUnificataList);
                    operationsUnificazioneAppartenente.add(new OperationUnificazioneAppartenente(Operation.Azione.CHIUSURA, appartenenteImportato, repositoryFactory.getEntityManager(), pair, null));
                }
            }
        }
        return operationAppartenentiList;
    }

    private List<OperationAnagrafica> buildedOperationsAnagrafiche(List<DatiDaImportareAnagrafica> anagraficheDaImportare, List<DatiImportatiAnagrafica> anagraficheImportate, Map<String, Integer> indexAnagraficheImportate) {
        List<OperationAnagrafica> operationAnagraficheList = new ArrayList<>();
        log.info("Faccio il build delle operations anagrafiche");
        for (DatiDaImportareAnagrafica datiDaImportareAnagrafica : anagraficheDaImportare) {
            Operation.Azione azione = Operation.Azione.INSERT;
            Boolean salva = true;
            Integer posizione = indexAnagraficheImportate.get(datiDaImportareAnagrafica.getKey());

            if (posizione != null) {
                if (!anagraficheImportate.get(posizione).getCognome().equals(datiDaImportareAnagrafica.getCognome())
                    || !anagraficheImportate.get(posizione).getNome().equals(datiDaImportareAnagrafica.getNome())
                    || !anagraficheImportate.get(posizione).getEmail().equals(datiDaImportareAnagrafica.getEmail())) {
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
                descrizioniAggiuntive.put("nomeCasellaSorgente", datiDaImportare.getStruttureDaImportare().get(indexStruttureDaImportare.get(datiDaImportareTrasformazione.getIdCasellaPartenza().toString())).getDescrizione());
                descrizioniAggiuntive.put("nomeCasellaDestinazione", datiDaImportare.getStruttureDaImportare().get(indexStruttureDaImportare.get(datiDaImportareTrasformazione.getIdCasellaArrivo().toString())).getDescrizione());

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
            Integer percentualeStruttureValide = (nStruttureDaImportare * 100) / nStruttureImportate;
            if (tolleranzaStrutture > percentualeStruttureValide) {
                throw new RibaltoneHttpException("Errore nell'importazione bloccante. Il numero di strutture che si vogliono importare non supera la tolleranza minima richiesta");
            }
        }

        Integer nAppartenentiImportati = this.appartenentiImportati.size();
        Integer nAppartenenti = nAppartenentiImportati - this.utentiStrutturaChiusi;
        if (nAppartenentiImportati > 0) {
            Integer percentualeAppartenentiValidi = (nAppartenenti * 100) / nAppartenentiImportati;
            if (tolleranzaAppartenenti > percentualeAppartenentiValidi) {
                throw new RibaltoneHttpException("Errore nell'importazione bloccante. Il numero di afferenze utente-struttura che si vogliono importare non supera la tolleranza minima richiesta");
            }
        }
        return true;
    }

    //funzione che serve a capire se sono replicato o se ho un antenato che è replicato
    private List<StrutturaUnificata> isCoinvoltoInReplica(DatiRibaltoneStrutturaInterface struttura,
        Map<Integer, List<StrutturaUnificata>> mappaUnificazioniPerIdCasellaSorgente,
        Map<Integer, List<StrutturaUnificata>> mappaUnificazioniPerIdCasellaDestinazione,
        JPAQueryFactory jPAQueryFactory
    ) {
        if (struttura == null) {
            return null;
        } else if (mappaUnificazioniPerIdCasellaSorgente.containsKey(struttura.getIdCasella())) {
            List<StrutturaUnificata> strutturaUnificataList = mappaUnificazioniPerIdCasellaSorgente.get(struttura.getIdCasella());
//            Pair<StrutturaUnificata.TipoUnificazione, List<StrutturaUnificata>> pair = Pair.of(StrutturaUnificata.TipoUnificazione.REPLICA, strutturaUnificataList);
            UnificazionePair pair = new UnificazionePair(StrutturaUnificata.TipoUnificazione.REPLICA, strutturaUnificataList);
            idCaselleUnificateMap.put(struttura.getIdCasella(), pair);
            return mappaUnificazioniPerIdCasellaSorgente.get(struttura.getIdCasella());
        } else if (mappaUnificazioniPerIdCasellaDestinazione.containsKey(struttura.getIdCasella())) {
            List<StrutturaUnificata> strutturaUnificataList = mappaUnificazioniPerIdCasellaDestinazione.get(struttura.getIdCasella());
//            Pair<StrutturaUnificata.TipoUnificazione, List<StrutturaUnificata>> pair = Pair.of(StrutturaUnificata.TipoUnificazione.REPLICA, strutturaUnificataList);
            UnificazionePair pair = new UnificazionePair(StrutturaUnificata.TipoUnificazione.REPLICA, strutturaUnificataList);
            idCaselleUnificateMap.put(struttura.getIdCasella(), pair);
            return mappaUnificazioniPerIdCasellaDestinazione.get(struttura.getIdCasella());
        } else if (struttura.getIdCasellaPadre() != null && !struttura.getIdCasellaPadre().equals(0)) {
            boolean isDatiDaImportare = struttura.getClass().isAssignableFrom(DatiDaImportareStruttura.class);
            DatiRibaltoneStrutturaInterface strutturaPadre;
            if (isDatiDaImportare) {
                QDatiDaImportareStruttura qDatiDaImportareStruttura = QDatiDaImportareStruttura.datiDaImportareStruttura;
                strutturaPadre = jPAQueryFactory
                    .select(qDatiDaImportareStruttura)
                    .from(qDatiDaImportareStruttura)
                    .where(qDatiDaImportareStruttura.idCasella.eq(struttura.getIdCasellaPadre())
                        .and(qDatiDaImportareStruttura.idAzienda.eq(struttura.getIdAzienda()))
                    ).fetchFirst();
            } else {
                QDatiImportatiStruttura qDatiImportatiStruttura = QDatiImportatiStruttura.datiImportatiStruttura;
                strutturaPadre = jPAQueryFactory
                    .select(qDatiImportatiStruttura)
                    .from(qDatiImportatiStruttura)
                    .where(qDatiImportatiStruttura.idCasella.eq(struttura.getIdCasellaPadre())
                        .and(qDatiImportatiStruttura.idAzienda.eq(struttura.getIdAzienda()))
                    ).fetchFirst();
            }
            List<StrutturaUnificata> coinvoltoInReplica = isCoinvoltoInReplica(strutturaPadre, mappaUnificazioniPerIdCasellaSorgente, mappaUnificazioniPerIdCasellaDestinazione, jPAQueryFactory);
            if (coinvoltoInReplica != null) {
//                Pair<StrutturaUnificata.TipoUnificazione, List<StrutturaUnificata>> paiar = Pair.of(StrutturaUnificata.TipoUnificazione.REPLICA, coinvoltoInReplica);
                UnificazionePair pair = new UnificazionePair(StrutturaUnificata.TipoUnificazione.REPLICA, coinvoltoInReplica);
                idCaselleUnificateMap.put(struttura.getIdCasella(), pair);
            }
            return coinvoltoInReplica;
        } else {
            return null;
        }
    }

    private boolean isCoinvoltoInFusione(Integer idCasella,
        Map<Integer, List<StrutturaUnificata>> mappaFusioniPerIdCasellaSorgente,
        Map<Integer, List<StrutturaUnificata>> mappaFusioniPerIdCasellaDestinazione) {
        return mappaFusioniPerIdCasellaSorgente.get(idCasella) != null || mappaFusioniPerIdCasellaDestinazione.get(idCasella) != null;
    }

    private <T extends DatiRibaltoneStrutturaInterface> void manageOperationUnificazioniStruttura(T struttura, Operation.Azione azione, JPAQueryFactory queryFactory) {

        List<StrutturaUnificata> coinvoltoInRepliche = isCoinvoltoInReplica(struttura, mappaReplichePerIdCasellaSorgente, mappaReplichePerIdCasellaDestinazione, queryFactory);
        if (coinvoltoInRepliche != null) {
            List<UnificazioneDaGestire> unificazioniEseguite = new ArrayList<>();
            for (StrutturaUnificata strutturaUnificata : coinvoltoInRepliche) {
                unificazioniEseguite.add(UnificazioneDaGestire.buildUnificazioneEseguita(strutturaUnificata));
            }
            operationsUnificazioneStruttura.add(new OperationUnificazioneStruttura(azione, struttura, repositoryFactory.getEntityManager(), StrutturaUnificata.TipoUnificazione.REPLICA, unificazioniEseguite, null));
        } else if (isCoinvoltoInFusione(struttura.getIdCasella(), mappaFusioniPerIdCasellaSorgente, mappaFusioniPerIdCasellaDestinazione)) {
            List<UnificazioneDaGestire> unificazioniEseguite = new ArrayList<>();
            List<StrutturaUnificata> strutturaUnificataList = mappaFusioniPerIdCasellaSorgente.containsKey(struttura.getIdCasella()) ? mappaFusioniPerIdCasellaSorgente.get(struttura.getIdCasella()) : mappaFusioniPerIdCasellaDestinazione.get(struttura.getIdCasella());
//            Pair<StrutturaUnificata.TipoUnificazione, List<StrutturaUnificata>> paiar = Pair.of(StrutturaUnificata.TipoUnificazione.FUSIONE, strutturaUnificataList);
            UnificazionePair pair = new UnificazionePair(StrutturaUnificata.TipoUnificazione.FUSIONE, strutturaUnificataList);
            for (StrutturaUnificata strutturaUnificata : strutturaUnificataList) {
                unificazioniEseguite.add(UnificazioneDaGestire.buildUnificazioneEseguita(strutturaUnificata));
            }
            idCaselleUnificateMap.put(struttura.getIdCasella(), pair);
            operationsUnificazioneStruttura.add(new OperationUnificazioneStruttura(azione, struttura, repositoryFactory.getEntityManager(), StrutturaUnificata.TipoUnificazione.FUSIONE, unificazioniEseguite, null));
        }
    }
}
