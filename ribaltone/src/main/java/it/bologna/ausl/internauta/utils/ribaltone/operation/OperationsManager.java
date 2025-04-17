package it.bologna.ausl.internauta.utils.ribaltone.operation;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportare;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.utils.RibaltoneUtils;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiTrasformazione;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public Operations buildOperations() {
        List<OperationStruttura> operationsStrutture = buildOperationsStrutture(datiDaImportare.getStruttureDaImportare(), this.struttureImportate, this.indexStruttureImportate, RibaltoneUtils.generateIndex(this.datiDaImportare.getTrasformazioniDaImportare(), DatiDaImportareTrasformazione::getIdCasellaPartenza));
        List<OperationAppartenente> operationsAppartenenti = buildOperationsAppartenenti(datiDaImportare.getAppartenentiDaImportare(), this.appartenentiImportati, indexAppartenentiImportati);
        List<OperationAnagrafica> operationsAnagrafiche = buildOperationsAnagrafiche(datiDaImportare.getAnagraficheDaImportare(), this.anagraficheImportate, this.indexAnagraficheImportate);
        List<OperationTrasformazione> operationsTraformazioni = buildOperationsTrasformazioni(datiDaImportare.getTrasformazioniDaImportare(), this.trasformazioniImportateUltimoProgressivoRiga);
        return new Operations(operationsStrutture, operationsAppartenenti, operationsAnagrafiche, operationsTraformazioni);
    }

    private List<OperationStruttura> buildOperationsStrutture(List<DatiDaImportareStruttura> struttureDaImportare, List<DatiImportatiStruttura> struttureImportate, Map<String, Integer> indexStruttureImportate, Map<String, Integer> indexIdCasellaPartenzaTrasformazioni) {
        List<OperationStruttura> operationStrutturaList = new ArrayList<>();
        Map<String, Integer> indexStruttureDaImportare = RibaltoneUtils.generateIndex(struttureDaImportare, DatiDaImportareStruttura::getKey);
        //capiamo i cambi di padre    
        this.struttureChiuse = 0;
        for (DatiDaImportareStruttura daImportareStruttura : struttureDaImportare) {
            Integer posizione = indexStruttureImportate.get(daImportareStruttura.getKey());
            if (posizione != null) {
                if (!struttureImportate.get(posizione).getIdPadre().equals(daImportareStruttura.getIdPadre())) {
                    //cambio di padre
                    operationStrutturaList.add(new OperationStruttura(Operation.Azione.CAMBIO_PADRE, daImportareStruttura, repositoryFactory.getEntityManager()));
                } else if (!struttureImportate.get(posizione).getDescrizione().equals(daImportareStruttura.getDescrizione())) {
                    //rinomina
                    operationStrutturaList.add(new OperationStruttura(Operation.Azione.RINOMINA, daImportareStruttura, repositoryFactory.getEntityManager()));
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

        return operationStrutturaList;
    }

    private List<OperationAppartenente> buildOperationsAppartenenti(List<DatiDaImportareAppartenente> appartenentiDaImportare, List<DatiImportatiAppartenente> appartenentiImportati,  Map<String, Integer> indexAppartenentiImportati) {
        List<OperationAppartenente> operationAppartenentiList = new ArrayList<>();
        //prendo in considerazione tutte le modifiche e gli inserimenti dei nuovi utenti
        for (DatiDaImportareAppartenente datiDaImportareAppartenente : appartenentiDaImportare) {
            Integer posizione = indexAppartenentiImportati.get(datiDaImportareAppartenente.getKey());
            Operation.Azione azione = Operation.Azione.INSERT;
            Boolean salva = true;
            if (posizione != null) {
                if (!appartenentiImportati.get(posizione).getResponsabile().equals(datiDaImportareAppartenente.getResponsabile())
                        || !appartenentiImportati.get(posizione).getCognome().equals(datiDaImportareAppartenente.getCognome())
                        || !appartenentiImportati.get(posizione).getNome().equals(datiDaImportareAppartenente.getNome())
                        || !appartenentiImportati.get(posizione).getTipoAppartenenza().equals(datiDaImportareAppartenente.getTipoAppartenenza())
                        || !appartenentiImportati.get(posizione).getCodiceMatricola().equals(datiDaImportareAppartenente.getCodiceMatricola())
                        || !appartenentiImportati.get(posizione).getDataAssunzione().equals(datiDaImportareAppartenente.getDataAssunzione())
                        || !appartenentiImportati.get(posizione).getDataDimissione().equals(datiDaImportareAppartenente.getDataDimissione())
                        || !appartenentiImportati.get(posizione).getUsername().equals(datiDaImportareAppartenente.getUsername())) {
                    azione = Operation.Azione.EDIT;
                } else {
                    salva = false;
                }
            }
            if (salva) {
                if (azione == Operation.Azione.INSERT) {
                    this.utentiStrutturaChiusi--;
                }
                operationAppartenentiList.add(new OperationAppartenente(azione, datiDaImportareAppartenente, repositoryFactory.getEntityManager()));
            }
        }
        //ora pensiamo a tutte le chiusure
        Map<String, Integer> indexDaImportare = RibaltoneUtils.generateIndex(appartenentiDaImportare, DatiDaImportareAppartenente::getKey);
        for (DatiImportatiAppartenente appartenenteImportato : appartenentiImportati) {
            if (!indexDaImportare.containsKey(appartenenteImportato.getKey())) {
                operationAppartenentiList.add(new OperationAppartenente(Operation.Azione.CHIUSURA, appartenenteImportato, repositoryFactory.getEntityManager()));
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
                operationTrasformazioneList.add(new OperationTrasformazione(Operation.Azione.INSERT, datiDaImportareTrasformazione,repositoryFactory.getEntityManager()));
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
