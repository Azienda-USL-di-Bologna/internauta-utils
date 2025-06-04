package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportare;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.utils.RibaltoneUtils;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiTrasformazione;
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
        Map<String, List<? extends Operation<DatiRibaltoneInterface>>> buildOperationsStruttureETrasformazione = buildOperationsStrutture(datiDaImportare.getStruttureDaImportare(), this.struttureImportate, this.indexStruttureImportate, RibaltoneUtils.generateIndex(this.datiDaImportare.getTrasformazioniDaImportare(), DatiDaImportareTrasformazione::getIdCasellaPartenza));
        List<OperationStruttura> operationsStrutture = (List<OperationStruttura>) buildOperationsStruttureETrasformazione.get("strutture");
        List<OperationAppartenente> operationsAppartenenti = buildOperationsAppartenenti(datiDaImportare.getAppartenentiDaImportare(), this.appartenentiImportati, indexAppartenentiImportati, this.struttureImportate, datiDaImportare.getStruttureDaImportare());
        List<OperationAnagrafica> operationsAnagrafiche = buildOperationsAnagrafiche(datiDaImportare.getAnagraficheDaImportare(), this.anagraficheImportate, this.indexAnagraficheImportate);
        List<OperationTrasformazione> operationsTraformazioni = buildOperationsTrasformazioni(datiDaImportare.getTrasformazioniDaImportare(), this.trasformazioniImportateUltimoProgressivoRiga);
        operationsTraformazioni.addAll((Collection<? extends OperationTrasformazione>) buildOperationsStruttureETrasformazione.get("trasformazioni"));
        return new Operations(operationsStrutture, operationsAppartenenti, operationsAnagrafiche, operationsTraformazioni);
    }

    private Map<String, List<? extends Operation<DatiRibaltoneInterface>>> buildOperationsStrutture(
        List<DatiDaImportareStruttura> struttureDaImportare,
        List<DatiImportatiStruttura> struttureImportate,
        Map<String, Integer> indexStruttureImportate,
        Map<String, Integer> indexIdCasellaPartenzaTrasformazioni
    ) throws RibaltoneHttpException {
        List<OperationStruttura> operationStrutturaList = new ArrayList<>();
        List<OperationTrasformazione> operationTrasformazioneList = new ArrayList<>();
        Map<String, List<? extends Operation<DatiRibaltoneInterface>>> mapToReturn = new HashMap<>();
        EntityManager entityManager = repositoryFactory.getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QStruttura qStruttura = QStruttura.struttura;
        Map<String, Integer> indexStruttureDaImportare = RibaltoneUtils.generateIndex(struttureDaImportare, DatiDaImportareStruttura::getKey);
        //capiamo i cambi di padre
        this.struttureChiuse = 0;
        for (DatiDaImportareStruttura daImportareStruttura : struttureDaImportare) {
            Integer posizione = indexStruttureImportate.get(daImportareStruttura.getKey());

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
                    operationStrutturaList.add(new OperationStruttura(Operation.Azione.CAMBIO_PADRE, daImportareStruttura, repositoryFactory.getEntityManager()));
                    trasf.setMotivo("T");
                    operationTrasformazioneList.add(new OperationTrasformazione(Operation.Azione.CAMBIO_PADRE, trasf, repositoryFactory.getEntityManager()));
                } else if (!struttureImportate.get(posizione).getDescrizione().equals(daImportareStruttura.getDescrizione())) {
                    //rinomina
                    operationStrutturaList.add(new OperationStruttura(Operation.Azione.RINOMINA, daImportareStruttura, repositoryFactory.getEntityManager()));
                    trasf.setMotivo("R");
                    operationTrasformazioneList.add(new OperationTrasformazione(Operation.Azione.RINOMINA, trasf, repositoryFactory.getEntityManager()));
                } else {
                    //non è successo nulla è come era prima
                }

            } else {
                //allora è una nuova
                operationStrutturaList.add(new OperationStruttura(Operation.Azione.INSERT, daImportareStruttura, repositoryFactory.getEntityManager()));
                //(mi salvo anche quante ne ho aperto per capire se è una cosa coerente o c'è un grave errore sulla fonte dati)
                this.struttureChiuse--;
            }
        }
        //capire le chiusure (mi salvo anche quante ne ho da chiudere per capire se è una cosa coerente o c'è un grave errore sulla fonte dati)
        for (DatiImportatiStruttura strutturaImportata : struttureImportate) {
            if (!indexStruttureDaImportare.containsKey(strutturaImportata.getKey())
                && !indexIdCasellaPartenzaTrasformazioni.containsKey(strutturaImportata.getIdCasella().toString())) {
                operationStrutturaList.add(new OperationStruttura(Operation.Azione.CHIUSURA, strutturaImportata, repositoryFactory.getEntityManager()));
                this.struttureChiuse++;
            }
        }

        mapToReturn.put("strutture", operationStrutturaList);
        mapToReturn.put("trasformazioni", operationTrasformazioneList);

        return mapToReturn;
    }

    private List<OperationAppartenente> buildOperationsAppartenenti(
        List<DatiDaImportareAppartenente> appartenentiDaImportare,
        List<DatiImportatiAppartenente> appartenentiImportati,
        Map<String, Integer> indexAppartenentiImportati,
        List<DatiImportatiStruttura> struttureImportateList,
        List<DatiDaImportareStruttura> struttureDaImportareList
    ) {
        List<OperationAppartenente> operationAppartenentiList = new ArrayList<>();
        //prendo in considerazione tutte le modifiche e gli inserimenti dei nuovi utenti
        Map<String, Integer> indexIdCasellaStruttureImportate = RibaltoneUtils.generateIndex(struttureImportateList, DatiImportatiStruttura::getIdCasella);

        Map<String, Integer> indexIdCasellaStruttureDaImportare = RibaltoneUtils.generateIndex(struttureDaImportareList, DatiDaImportareStruttura::getIdCasella);

        for (DatiDaImportareAppartenente datiDaImportareAppartenente : appartenentiDaImportare) {
            Integer posizione = indexAppartenentiImportati.get(datiDaImportareAppartenente.getKey());
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
                    operationAppartenentiList.add(new OperationAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager(), editString, struttura.getDescrizione()));
                }
            } else {
//             se posizione è null allora non ho una riga tra le importate che mi rappresenta l'appartenente'
                DatiImportatiStruttura struttura = struttureImportateList.get(indexIdCasellaStruttureImportate.get(datiDaImportareAppartenente.getIdCasella().toString()));
                String descrizione;
                if (struttura == null) {
                    DatiDaImportareStruttura strutturaDaImportare = struttureDaImportareList.get(indexIdCasellaStruttureDaImportare.get(datiDaImportareAppartenente.getIdCasella().toString()));
                    descrizione = strutturaDaImportare.getDescrizione();
                } else {
                    descrizione = struttura.getDescrizione();
                }
                operationAppartenentiList.add(new OperationAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager(), editString, descrizione));
                this.utentiStrutturaChiusi--;
            }
        }
        //ora pensiamo a tutte le chiusure
        Map<String, Integer> indexDaImportare = RibaltoneUtils.generateIndex(appartenentiDaImportare, DatiDaImportareAppartenente::getKey);
        for (DatiImportatiAppartenente appartenenteImportato : appartenentiImportati) {
            if (indexDaImportare != null && !indexDaImportare.isEmpty() && !indexDaImportare.containsKey(appartenenteImportato.getKey())) {
                DatiImportatiStruttura struttura = struttureImportateList.get(indexIdCasellaStruttureImportate.get(appartenenteImportato.getIdCasella().toString()));
                operationAppartenentiList.add(new OperationAppartenente(Operation.Azione.CHIUSURA, appartenenteImportato, repositoryFactory.getEntityManager(), null, struttura.getDescrizione()));
                this.utentiStrutturaChiusi++;
            }
        }
        return operationAppartenentiList;
    }

    private List<OperationAnagrafica> buildOperationsAnagrafiche(List<DatiDaImportareAnagrafica> anagraficheDaImportare, List<DatiImportatiAnagrafica> anagraficheImportate, Map<String, Integer> indexAnagraficheImportate) {
        List<OperationAnagrafica> operationAnagraficheList = new ArrayList<>();
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
                operationAnagraficheList.add(new OperationAnagrafica(azione, datiDaImportareAnagrafica, repositoryFactory.getEntityManager()));
            }
        }
        return operationAnagraficheList;
    }

    private List<OperationTrasformazione> buildOperationsTrasformazioni(List<DatiDaImportareTrasformazione> trasformazioniDaImportare, Integer ultimoProgressivoRiga) {
        List<OperationTrasformazione> operationTrasformazioneList = new ArrayList<>();

        for (DatiDaImportareTrasformazione datiDaImportareTrasformazione : trasformazioniDaImportare) {
            if (datiDaImportareTrasformazione.getProgressivoRiga() > ultimoProgressivoRiga) {
                operationTrasformazioneList.add(new OperationTrasformazione(Operation.Azione.CONFLUENZA, datiDaImportareTrasformazione, repositoryFactory.getEntityManager()));
            }
        }
        return operationTrasformazioneList;
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
}
