package it.bologna.ausl.internauta.utils.ribaltone.basedata;

import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAnagrafica;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAppartenente;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationStruttura;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationTrasformazione;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReport.UserReportType;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReportManager;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Top
 */
public class Operations implements Serializable {

    private List<OperationStruttura> listOfOperationStruttura;
    private List<OperationAppartenente> listOfOperationAppartenenti;
    private List<OperationAnagrafica> listOfOperationAnagrafiche;
    private List<OperationTrasformazione> listOfOperationTrasformazioni;

    public Operations(List<OperationStruttura> listOfOperationStruttura,
            List<OperationAppartenente> listOfOperationAppartenenti,
            List<OperationAnagrafica> listOfOperationAnagrafiche,
            List<OperationTrasformazione> listOfOperationTrasformazioni) {

        this.listOfOperationStruttura = listOfOperationStruttura;
        this.listOfOperationAppartenenti = listOfOperationAppartenenti;
        this.listOfOperationAnagrafiche = listOfOperationAnagrafiche;
        this.listOfOperationTrasformazioni = listOfOperationTrasformazioni;
    }

    public Operations() {

    }

    public void execute() {
        for (OperationStruttura operation : listOfOperationStruttura) {
            operation.esegui();
        }
        for (OperationTrasformazione operation : listOfOperationTrasformazioni) {
            operation.esegui();
        }
        for (OperationAppartenente operation : listOfOperationAppartenenti) {
            operation.esegui();
        }
        for (OperationAnagrafica operation : listOfOperationAnagrafiche) {
            operation.esegui();
        }
    }

    public UserReportManager generateUserReport(UserReportType userReportType) {

        return null;
    }

    public UserReportManager generateUserReport() {

        return null;
    }

    public List<OperationStruttura> getListOfOperationStruttura() {
        return listOfOperationStruttura;
    }

    public void setListOfOperationStruttura(List<OperationStruttura> listOfOperationStruttura) {
        this.listOfOperationStruttura = listOfOperationStruttura;
    }

    public List<OperationAppartenente> getListOfOperationAppartenenti() {
        return listOfOperationAppartenenti;
    }

    public void setListOfOperationAppartenenti(List<OperationAppartenente> listOfOperationAppartenenti) {
        this.listOfOperationAppartenenti = listOfOperationAppartenenti;
    }

    public List<OperationAnagrafica> getListOfOperationAnagrafiche() {
        return listOfOperationAnagrafiche;
    }

    public void setListOfOperationAnagrafiche(List<OperationAnagrafica> listOfOperationAnagrafiche) {
        this.listOfOperationAnagrafiche = listOfOperationAnagrafiche;
    }

    public List<OperationTrasformazione> getListOfOperationTrasformazioni() {
        return listOfOperationTrasformazioni;
    }

    public void setListOfOperationTrasformazioni(List<OperationTrasformazione> listOfOperationTrasformazioni) {
        this.listOfOperationTrasformazioni = listOfOperationTrasformazioni;
    }
    
}
