package it.bologna.ausl.internauta.utils.ribaltone.basedata.datachecker;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportare;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;

/**
 *
 * @author Top
 *
 * OperationsManager classe che contiene: metodo per capire, tramite confronto,
 * le operazioni da svolgere
 *
 */
public class OperationsManager {

    private DatiDaImportare datiDaImportareValidati;

    public OperationsManager(DatiDaImportare datiDaImportareValidati) {
        this.datiDaImportareValidati = datiDaImportareValidati;
    }

    /**
     *
     * @return
     *
     * genera tutte le operazioni che sono da fare da queste si possono generare
     * i report per l'utente o si puo proseguire col ribaltone
     */
    public Operations buildOperations() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody

    }

}
