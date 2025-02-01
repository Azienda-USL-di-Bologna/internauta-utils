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
import jakarta.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private List<DatiImportatiTrasformazione> trasformazioniImportate;
    Map<String, Integer> indexAnagraficheImportate;
    Map<String, Integer> indexAppartenentiImportati;
    Map<String, Integer> indexStruttureImportate;
    Map<String, Integer> indexTrasformazioniImportate;

    public OperationsManager(DatiDaImportare datiDaImportare, String codiceAzienda) {
        this.datiDaImportare = datiDaImportare;
        this.anagraficheImportate = datiImportatiAnagraficaRepository.findByCodiceAzienda(codiceAzienda);
        this.appartenentiImportati = datiImportatiAppartenenteRepository.findByCodiceAzienda(codiceAzienda);
        this.struttureImportate = datiImportatiStrutturaRepository.findByCodiceAzienda(codiceAzienda);
        this.trasformazioniImportate = datiImportatiTrasformazioneRepository.findByCodiceAzienda(codiceAzienda);
        this.indexAnagraficheImportate = RibaltoneUtils.generateIndex2(anagraficheImportate, DatiImportatiAnagrafica::getKey);
        this.indexAppartenentiImportati = RibaltoneUtils.generateIndex2(appartenentiImportati, DatiImportatiAppartenente::getKey);
        this.indexStruttureImportate = RibaltoneUtils.generateIndex2(struttureImportate, DatiImportatiStruttura::getKey);
        this.indexTrasformazioniImportate = RibaltoneUtils.generateIndex2(trasformazioniImportate, DatiImportatiTrasformazione::getKey);
    }

    /**
     *
     * @return
     *
     * genera tutte le operazioni che sono da fare da queste si possono generare
     * i report per l'utente o si puo proseguire col ribaltone
     */
    public Operations buildOperations() {
        Operations operationsStrutture = buildOperationsStrutture(datiDaImportare.getStruttureDaImportare(), this.struttureImportate, this.indexStruttureImportate, RibaltoneUtils.generateIndex2(this.datiDaImportare.getTrasformazioniDaImportare(), DatiDaImportareTrasformazione::getIdCasellaPartenza));
        Operations operationsAppartenenti = buildOperationsAppartenenti(datiDaImportare.getAppartenentiDaImportare(), this.appartenentiImportati, this, indexAppartenentiImportati);
        Operations operationsAnagrafiche = buildOperationsAnagrafiche(datiDaImportare.getAnagraficheDaImportare(), this.anagraficheImportate, this.indexAnagraficheImportate);
        Operations operationsTraformazioni = buildOperationsTrasformazioni(datiDaImportare.getTrasformazioniDaImportare(), this.trasformazioniImportate, this.indexTrasformazioniImportate);
        return marge(operationsStrutture, operationsAppartenenti, operationsAnagrafiche, operationsTraformazioni);
    }

    private Operations buildOperationsStrutture(List<DatiDaImportareStruttura> struttureDaImportare, List<DatiImportatiStruttura> struttureImportate, Map<String, Integer> indexStruttureImportate, Map<String, Integer> indexIdCasellaPartenzaTrasformazioni) {
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

        return new Operations(operationStrutturaList);
    }

    private Operations buildOperationsAppartenenti(List<DatiDaImportareAppartenente> appartenentiDaImportare, List<DatiImportatiAppartenente> appartenentiImportati, OperationsManager aThis, Map<String, Integer> indexAppartenentiImportati) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private Operations buildOperationsAnagrafiche(List<DatiDaImportareAnagrafica> anagraficheDaImportare, List<DatiImportatiAnagrafica> anagraficheImportate, Map<String, Integer> indexAnagraficheImportate) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private Operations buildOperationsTrasformazioni(List<DatiDaImportareTrasformazione> trasformazioniDaImportare, List<DatiImportatiTrasformazione> trasformazioniImportate, Map<String, Integer> indexTrasformazioniImportate) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private Operations marge(Operations operationsStrutture, Operations operationsAppartenenti, Operations operationsAnagrafiche, Operations operationsTraformazioni) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
