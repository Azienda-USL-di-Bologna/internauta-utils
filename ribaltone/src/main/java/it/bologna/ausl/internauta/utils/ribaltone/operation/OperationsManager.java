package it.bologna.ausl.internauta.utils.ribaltone.operation;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportare;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.repository.DatiImportatiAppartenenteRepository;
import it.bologna.ausl.internauta.utils.ribaltone.repository.DatiImportatiStrutturaRepository;
import it.bologna.ausl.internauta.utils.ribaltone.repository.DatiImportatiAnagraficaRepository;
import it.bologna.ausl.internauta.utils.ribaltone.repository.DatiImportatiTrasformazioneRepository;
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
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Top
 *
 * OperationsManager classe che contiene: metodo per capire, tramite confronto,
 * le operazioni da svolgere
 *
 */
public class OperationsManager {

    @Autowired
    private DatiImportatiAnagraficaRepository datiImportatiAnagraficaRepository;
    @Autowired
    private DatiImportatiAppartenenteRepository datiImportatiAppartenenteRepository;
    @Autowired
    private DatiImportatiStrutturaRepository datiImportatiStrutturaRepository;
    @Autowired
    private DatiImportatiTrasformazioneRepository datiImportatiTrasformazioneRepository;

    private DatiDaImportare datiDaImportare;
    private List<DatiImportatiAnagrafica> anagraficheImportate;
    private List<DatiImportatiAppartenente> appartenentiImportati;
    private List<DatiImportatiStruttura> struttureImportate;
    private Integer trasformazioniImportateUltimoProgressivoRiga;
    Map<String, Integer> indexAnagraficheImportate;
    Map<String, Integer> indexAppartenentiImportati;
    Map<String, Integer> indexStruttureImportate;
    Map<String, Integer> indexTrasformazioniImportate;

    public OperationsManager(DatiDaImportare datiDaImportare, String codiceAzienda) {
        this.datiDaImportare = datiDaImportare;
        this.anagraficheImportate = datiImportatiAnagraficaRepository.findByCodiceAzienda(codiceAzienda);
        this.appartenentiImportati = datiImportatiAppartenenteRepository.findByCodiceAzienda(codiceAzienda);
        this.struttureImportate = datiImportatiStrutturaRepository.findByCodiceAzienda(codiceAzienda);
        this.trasformazioniImportateUltimoProgressivoRiga = datiImportatiTrasformazioneRepository.findTopByCodiceAziendaOrderByProgressivoRigaDesc(codiceAzienda);
        this.indexAnagraficheImportate = RibaltoneUtils.generateIndex2(anagraficheImportate, DatiImportatiAnagrafica::getKey);
        this.indexAppartenentiImportati = RibaltoneUtils.generateIndex2(appartenentiImportati, DatiImportatiAppartenente::getKey);
        this.indexStruttureImportate = RibaltoneUtils.generateIndex2(struttureImportate, DatiImportatiStruttura::getKey);

    }

    /**
     *
     * @return
     *
     * genera tutte le operazioni che sono da fare da queste si possono generare
     * i report per l'utente o si puo proseguire col ribaltone
     */
    public Operations buildOperations() {
        List<OperationStruttura> operationsStrutture = buildOperationsStrutture(datiDaImportare.getStruttureDaImportare(), this.struttureImportate, this.indexStruttureImportate, RibaltoneUtils.generateIndex2(this.datiDaImportare.getTrasformazioniDaImportare(), DatiDaImportareTrasformazione::getIdCasellaPartenza));
        List<OperationAppartenente> operationsAppartenenti = buildOperationsAppartenenti(datiDaImportare.getAppartenentiDaImportare(), this.appartenentiImportati, this, indexAppartenentiImportati);
        List<OperationAnagrafica> operationsAnagrafiche = buildOperationsAnagrafiche(datiDaImportare.getAnagraficheDaImportare(), this.anagraficheImportate, this.indexAnagraficheImportate);
        List<OperationTrasformazione> operationsTraformazioni = buildOperationsTrasformazioni(datiDaImportare.getTrasformazioniDaImportare(), this.trasformazioniImportateUltimoProgressivoRiga, this.indexTrasformazioniImportate);
        return new Operations(operationsStrutture, operationsAppartenenti, operationsAnagrafiche, operationsTraformazioni);
    }

    private List<OperationStruttura> buildOperationsStrutture(List<DatiDaImportareStruttura> struttureDaImportare, List<DatiImportatiStruttura> struttureImportate, Map<String, Integer> indexStruttureImportate, Map<String, Integer> indexIdCasellaPartenzaTrasformazioni) {
        List<OperationStruttura> operationStrutturaList = new ArrayList<>();
        Map<String, Integer> indexStruttureDaImportare = RibaltoneUtils.generateIndex(struttureDaImportare);
        //capiamo i cambi di padre    

        for (DatiDaImportareStruttura daImportareStruttura : struttureDaImportare) {
            Integer posizione = indexStruttureImportate.get(daImportareStruttura.getKey());
            if (posizione != null) {
                if (!struttureImportate.get(posizione).getIdPadre().equals(daImportareStruttura.getIdPadre())) {
                    //cambio di padre
                    operationStrutturaList.add(new OperationStruttura(Operation.Azione.EDIT, daImportareStruttura));
                } else if (!struttureImportate.get(posizione).getDescrizione().equals(daImportareStruttura.getDescrizione())) {
                    //rinomina
                    operationStrutturaList.add(new OperationStruttura(Operation.Azione.EDIT, daImportareStruttura));
                } else {
                    //non è successo nulla è come era prima
                }
            } else {
                //allora è una nuova
                operationStrutturaList.add(new OperationStruttura(Operation.Azione.INSERT, daImportareStruttura));
            }
        }
        //capire le chiusure
        for (DatiImportatiStruttura strutturaImportata : struttureImportate) {
            if (!indexStruttureDaImportare.containsKey(strutturaImportata.getKey())
                    && !indexIdCasellaPartenzaTrasformazioni.containsKey(strutturaImportata.getIdCasella().toString())) {
                operationStrutturaList.add(new OperationStruttura(Operation.Azione.EDIT, strutturaImportata));
            }
        }

        return operationStrutturaList;
    }

    private List<OperationAppartenente> buildOperationsAppartenenti(List<DatiDaImportareAppartenente> appartenentiDaImportare, List<DatiImportatiAppartenente> appartenentiImportati, OperationsManager aThis, Map<String, Integer> indexAppartenentiImportati) {
        List<OperationAppartenente> operationAppartenentiList = new ArrayList<>();
        //prendo in considerazione tutte le modifiche e gli inserimenti dei nuovi utenti
        for (DatiDaImportareAppartenente datiDaImportareAppartenente : appartenentiDaImportare) {
            Integer posizione = indexAppartenentiImportati.get(datiDaImportareAppartenente.getKey());
            Operation.Azione azione = Operation.Azione.INSERT;
            Boolean salva = true;
            if (posizione != null) {
                if (!appartenentiImportati.get(posizione).getResposabile().equals(datiDaImportareAppartenente.getResposabile())
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
                operationAppartenentiList.add(new OperationAppartenente(azione, datiDaImportareAppartenente));
            }
        }
        //ora pensiamo a tutte le chiusure
        Map<String, Integer> indexDaImportare = RibaltoneUtils.generateIndex2(appartenentiDaImportare, DatiDaImportareAppartenente::getKey);
        for (DatiImportatiAppartenente appartenenteImportato : appartenentiImportati) {
            if (!indexDaImportare.containsKey(appartenenteImportato.getKey())) {
                operationAppartenentiList.add(new OperationAppartenente(Operation.Azione.CHIUSURA, appartenenteImportato));
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
                operationAnagraficheList.add(new OperationAnagrafica(azione, datiDaImportareAnagrafica));
            }
        }
        return operationAnagraficheList;
    }

    private List<OperationTrasformazione> buildOperationsTrasformazioni(List<DatiDaImportareTrasformazione> trasformazioniDaImportare, Integer ultimoProgressivoRiga, Map<String, Integer> indexTrasformazioniImportate) {
        List<OperationTrasformazione> operationTrasformazioneList = new ArrayList<>();

        for (DatiDaImportareTrasformazione datiDaImportareTrasformazione : trasformazioniDaImportare) {
            if (datiDaImportareTrasformazione.getProgressivoRiga() > ultimoProgressivoRiga) {
                operationTrasformazioneList.add(new OperationTrasformazione(Operation.Azione.INSERT, datiDaImportareTrasformazione));
            }
        }
        return operationTrasformazioneList;
    }

    private Operations marge(Operations operationsStrutture, Operations operationsAppartenenti, Operations operationsAnagrafiche, Operations operationsTraformazioni) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
