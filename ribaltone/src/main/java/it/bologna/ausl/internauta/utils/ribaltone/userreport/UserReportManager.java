package it.bologna.ausl.internauta.utils.ribaltone.userreport;

import it.bologna.ausl.internauta.utils.ribaltone.Ribaltone.TipologiaTabellaBaborg;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import java.io.File;

/**
 *
 * @author Top
 */
public class UserReportManager {

    Operations operations;

    public UserReportManager(Operations operations) {
        this.operations = operations;
    }

    /**
     *
     * @return uno zip con dentro i 4 file csv del report
     */
    public File getCSV() {
        throw new UnsupportedOperationException("Not supported yet.");

    }
    /**
     * 
     * @param tipologiaTabella tipo di tabella coinvolta
     * @return file csv che contiene il report
     */
    public File getCSV(TipologiaTabellaBaborg tipologiaTabella) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    /**
     * 
     * @return stringa html da far vedere all'utente 
     * è l'unione delle 4 tipologie
     */
    public Object getFrontendObject() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    /**
     * 
     * @param tipologiaTabella tipo di tabella coinvolta
     * @return stringa html che contiene il report della tipologia indicata
     */
    public String gethtml(TipologiaTabellaBaborg tipologiaTabella) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Object get() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
