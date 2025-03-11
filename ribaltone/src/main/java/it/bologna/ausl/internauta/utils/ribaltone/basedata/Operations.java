package it.bologna.ausl.internauta.utils.ribaltone.basedata;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAnagrafica;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAppartenente;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationStruttura;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationTrasformazione;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReport.UserReportType;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReportManager;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Top
 */
public class Operations implements Serializable {
    
    @PersistenceContext
    private EntityManager entityManager;

    private List<OperationStruttura> listOfOperationStruttura;
    private List<OperationAppartenente> listOfOperationAppartenenti;
    private List<OperationAnagrafica> listOfOperationAnagrafiche;
    private List<OperationTrasformazione> listOfOperationTrasformazioni;
    private Object workToDo;
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

    public void execute(RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        for (OperationStruttura operation : listOfOperationStruttura) {
            operation.esegui(workToDo,repositoryFactory);
        }
        workToDo = null;
        for (OperationTrasformazione operation : listOfOperationTrasformazioni) {
            operation.esegui(workToDo,repositoryFactory);
        }
        workToDo = null;
        for (OperationAppartenente operation : listOfOperationAppartenenti) {
            operation.esegui(workToDo, repositoryFactory);
        }
        risistemaAfferenze();
        workToDo = null;
        for (OperationAnagrafica operation : listOfOperationAnagrafiche) {
            operation.esegui(workToDo,repositoryFactory);
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

    /**
     * funzione che data un'azienda prende tutti gli utenti struttura diretti attivi
     * li confronta con il numero di utenti attivi se ci sono più di un'afferenza diretta per utente
     * mette tutte quelle di troppo come funzionali. (ritorna gli utenti struttura cambiati)
     */
    private void risistemaAfferenze() {
        //faccio la query di select
        //count e poi vedo che fare
        QUtenteStruttura us = QUtenteStruttura.utenteStruttura;
        QUtente u = QUtente.utente;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        
        List<Integer> idUtentiConNAfferenzeDirette = queryFactory
                .select(us.idUtente.id)
                .from(us)
                .where(us.attivo.isTrue()
                        .and(us.idUtente.idAzienda.id.eq(2))
                        .and(us.idAfferenzaStruttura.id.eq(1)))
                .groupBy(us.idUtente)
                .having(us.id.count().gt(1))  // COUNT(id_afferenza_struttura) > 1
                .fetch();
        
        List<UtenteStruttura> utentiStrutturaDaSistemare = queryFactory
                .select(us)
                .from(us)
                .where(us.attivo.isTrue()
                        .and(us.idUtente.idAzienda.id.eq(2))
                        .and(us.idAfferenzaStruttura.id.eq(1))
                        .and(us.idUtente.id.in(idUtentiConNAfferenzeDirette)))  // Join con il risultato della prima query
                .fetch();
        //ora ho tutte le afferenze plurime dirette devo andare a sistemarle
        //direi che una a caso (direi dalla seconda che esamino) diverranno funzionali
        
    }
    
}
