package it.bologna.ausl.internauta.utils.ribaltone.basedata;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAnagrafica;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAppartenente;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationStruttura;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationTrasformazione;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationUnificazioneAppartenente;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationUnificazioneStruttura;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationsUtils;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReport.UserReportType;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReportManager;
import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QAfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.QContatto;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Top
 */
public class Operations implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(Operations.class);

    private List<OperationStruttura> listOfOperationStruttura;
    private List<OperationAppartenente> listOfOperationAppartenente;
    private List<OperationAnagrafica> listOfOperationAnagrafica;
    private List<OperationTrasformazione> listOfOperationTrasformazione;
    private List<OperationUnificazioneStruttura> listOfOperationUnificazioneStruttura;
    private List<OperationUnificazioneAppartenente> listOfOperationUnificazioneAppartenente;
    private Object workToDo;

    public Operations(
        List<OperationStruttura> listOfOperationStruttura,
        List<OperationAppartenente> listOfOperationAppartenenti,
        List<OperationAnagrafica> listOfOperationAnagrafiche,
        List<OperationTrasformazione> listOfOperationTrasformazioni,
        List<OperationUnificazioneStruttura> listOfOperationUnificazioneStruttura,
        List<OperationUnificazioneAppartenente> listOfOperationUnificazioneAppartenente
    ) {

        this.listOfOperationStruttura = listOfOperationStruttura;
        this.listOfOperationAppartenente = listOfOperationAppartenenti;
        this.listOfOperationAnagrafica = listOfOperationAnagrafiche;
        this.listOfOperationTrasformazione = listOfOperationTrasformazioni;
        this.listOfOperationUnificazioneStruttura = listOfOperationUnificazioneStruttura;
        this.listOfOperationUnificazioneAppartenente = listOfOperationUnificazioneAppartenente;
    }

    public Operations() {

    }

    public void execute(RepositoryFactory repositoryFactory, String codiceAzienda) throws RibaltoneHttpException {
        for (OperationStruttura operation : listOfOperationStruttura) {
            operation.esegui(workToDo, repositoryFactory);
            operation.menageContattoStruttura(repositoryFactory);
        }
//        OperationsUtils.manageUnificazioni(repositoryFactory.getEntityManager(), listOfOperationStruttura);
        workToDo = null;
        for (OperationAppartenente operation : listOfOperationAppartenente) {
            operation.esegui(workToDo, repositoryFactory);
            operation.menageContattoAppartenente(repositoryFactory);
        }
        workToDo = null;
        for (OperationTrasformazione operation : listOfOperationTrasformazione) {
            operation.esegui(workToDo, repositoryFactory);
            operation.menageContattiTrasformati(repositoryFactory);
        }
        risistemaAfferenze(repositoryFactory);
        workToDo = null;
        for (OperationAnagrafica operation : listOfOperationAnagrafica) {
            operation.esegui(workToDo, repositoryFactory);
            operation.menageContatto(repositoryFactory);
        }
        workToDo = null;
        for (OperationUnificazioneStruttura operation : listOfOperationUnificazioneStruttura) {
            operation.esegui(workToDo, repositoryFactory);
            operation.menageContattoStutturaUnificata(repositoryFactory);
        }
        workToDo = null;
        for (OperationUnificazioneAppartenente operation : listOfOperationUnificazioneAppartenente) {
            operation.esegui(workToDo, repositoryFactory);
            operation.menageContattoAppartenenteUnificato(repositoryFactory);
        }
//        finalOperations(repositoryFactory, codiceAzienda);
    }

    /**
     * funzione che si occupa di gestire la parte finale delle operazioni
     * fa i vari check per verificare che il ribaltone sia andato a buon fine
     *
     * @param repositoryFactory
     * @throws RibaltoneHttpException
     */
    private void finalOperations(RepositoryFactory repositoryFactory, String codiceAzienda) throws RibaltoneHttpException {
        //1) generazione e manutenzione dei contatti
        EntityManager entityManager = repositoryFactory.getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QPersona qPersona = QPersona.persona;
        Persona p = queryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq("RIBALTONE")).fetchOne();
        if (p == null) {
            throw new RibaltoneHttpException("peronsa Ribaltone non trovata va inserita sul db");
        }
        QContatto qContatto = QContatto.contatto;
        //contatti struttura
        QStruttura qStruttura = QStruttura.struttura;

        List<Tuple> strutturaContatto = queryFactory
            .select(qStruttura, qContatto)
            .from(qStruttura)
            .leftJoin(qContatto).on(qContatto.idEsterno.eq(qStruttura.id.toString()))
            .where(qStruttura.attiva.and(qStruttura.idAzienda.codice.eq(codiceAzienda))).fetch();
        for (Tuple tuple : strutturaContatto) {
            Struttura struttura = tuple.get(0, Struttura.class);
            Contatto contattoDellaStruttuta = tuple.get(1, Contatto.class);
            if (struttura != null) {
                if (contattoDellaStruttuta == null) {
                    //creo contatto della struttura col suo dettaglio e lo salvo
                    Integer[] idAziende = new Integer[0];
                    idAziende[0] = struttura.getIdAzienda().getId();
                    contattoDellaStruttuta = struttura.buildContattoAndDettaglio(p.getUtenteList().get(0), p, idAziende);
                    entityManager.persist(contattoDellaStruttuta);
                } else {
                    //aggiorno il dato e lo salvo
                    contattoDellaStruttuta.setDescrizione(struttura.getNome() + " [" + struttura.getIdCasella() + "]");
                    DettaglioContatto dettaglioPrincipale = contattoDellaStruttuta.getDettaglioContattoList().stream().filter(dc -> dc.getPrincipale() && !dc.getEliminato()).toList().get(0);
                    if (dettaglioPrincipale != null) {
                        dettaglioPrincipale.setDescrizione(struttura.getNome() + " [" + struttura.getIdCasella() + "]");
                        dettaglioPrincipale.setIdContatto(contattoDellaStruttuta);
                    }
                    entityManager.persist(dettaglioPrincipale);
                }
            }
        }
        //contatti utenteStruttura

        //elimino tutti i contatti eliminati logicamente di tipo organigramma
        //se ne trovo alcuni che sono ancora dentro ai gruppi li segnalo
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

    public List<OperationAppartenente> getListOfOperationAppartenente() {
        return listOfOperationAppartenente;
    }

    public void setListOfOperationAppartenente(List<OperationAppartenente> listOfOperationAppartenente) {
        this.listOfOperationAppartenente = listOfOperationAppartenente;
    }

    public List<OperationAnagrafica> getListOfOperationAnagrafica() {
        return listOfOperationAnagrafica;
    }

    public void setListOfOperationAnagrafica(List<OperationAnagrafica> listOfOperationAnagrafica) {
        this.listOfOperationAnagrafica = listOfOperationAnagrafica;
    }

    public List<OperationTrasformazione> getListOfOperationTrasformazione() {
        return listOfOperationTrasformazione;
    }

    public void setListOfOperationTrasformazione(List<OperationTrasformazione> listOfOperationTrasformazione) {
        this.listOfOperationTrasformazione = listOfOperationTrasformazione;
    }

    public List<OperationUnificazioneStruttura> getListOfOperationUnificazioneStruttura() {
        return listOfOperationUnificazioneStruttura;
    }

    public void setListOfOperationUnificazioneStruttura(List<OperationUnificazioneStruttura> listOfOperationUnificazioneStruttura) {
        this.listOfOperationUnificazioneStruttura = listOfOperationUnificazioneStruttura;
    }

    public List<OperationUnificazioneAppartenente> getListOfOperationUnificazioneAppartenente() {
        return listOfOperationUnificazioneAppartenente;
    }

    public void setListOfOperationUnificazioneAppartenente(List<OperationUnificazioneAppartenente> listOfOperationUnificazioneAppartenente) {
        this.listOfOperationUnificazioneAppartenente = listOfOperationUnificazioneAppartenente;
    }

    /**
     * funzione che data un'azienda prende tutti gli utenti struttura diretti
     * attivi li confronta con il numero di utenti attivi se ci sono più di
     * un'afferenza diretta per utente mette tutte quelle di troppo come
     * funzionali. (ritorna gli utenti struttura cambiati)
     */
    private void risistemaAfferenze(RepositoryFactory repositoryFactory) {
        //faccio la query di select
        //count e poi vedo che fare
        QUtenteStruttura us = QUtenteStruttura.utenteStruttura;
        QUtente u = QUtente.utente;
        QAfferenzaStruttura qAfferenzaStruttura = QAfferenzaStruttura.afferenzaStruttura;
        JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
        Integer idAfferenzaStruttura = queryFactory.select(qAfferenzaStruttura.id).from(qAfferenzaStruttura).where(qAfferenzaStruttura.codice.eq(AfferenzaStruttura.CodiciAfferenzaStruttura.FUNZIONALE.toString())).fetchOne();
        List<Integer> idUtentiConNAfferenzeDirette = queryFactory
            .select(us.idUtente.id)
            .from(us)
            .where(us.attivo.isTrue()
                .and(us.idUtente.idAzienda.id.eq(2))
                .and(us.idAfferenzaStruttura.id.eq(1)))
            .groupBy(us.idUtente.id)
            .having(us.id.count().gt(1)) // COUNT(id_afferenza_struttura) > 1
            .fetch();

        List<UtenteStruttura> utentiStrutturaDaSistemare = queryFactory
            .select(us)
            .from(us)
            .where(us.attivo.isTrue()
                .and(us.idUtente.idAzienda.id.eq(2))
                .and(us.idAfferenzaStruttura.id.eq(1))
                .and(us.idUtente.id.in(idUtentiConNAfferenzeDirette))) // Join con il risultato della prima query
            .fetch();
        //ora ho tutte le afferenze plurime dirette devo andare a sistemarle
        //direi che una a caso (direi dalla seconda che esamino) diverranno funzionali
        Map<Integer, Map<Integer, Integer>> utentiConGiaAfferenzaDiretta = new HashMap<>();
        for (UtenteStruttura utenteStruttura : utentiStrutturaDaSistemare) {
            if (utentiConGiaAfferenzaDiretta.get(utenteStruttura.getIdUtente().getId()) != null
                && utentiConGiaAfferenzaDiretta.get(utenteStruttura.getIdUtente().getId()).get(1) != null
                && utentiConGiaAfferenzaDiretta.get(utenteStruttura.getIdUtente().getId()).get(2) == null) {
                Integer utenteStrutturaDaNonToccare = utentiConGiaAfferenzaDiretta.get(utenteStruttura.getIdUtente().getId()).get(1);
                queryFactory
                    .update(us)
                    .set(us.idAfferenzaStruttura.id, idAfferenzaStruttura)
                    .where(us.id.ne(utenteStrutturaDaNonToccare).and(us.idUtente.id.eq(utenteStruttura.getIdUtente().getId())));
                //segno gia sistemata questa persona
                utentiConGiaAfferenzaDiretta.get(utenteStruttura.getIdUtente().getId()).put(2, 1);
            } else if (utentiConGiaAfferenzaDiretta.get(utenteStruttura.getIdUtente().getId()) == null) {
                Map<Integer, Integer> usSalvo = new HashMap<>();
                usSalvo.put(1, utenteStruttura.getId());
                utentiConGiaAfferenzaDiretta.put(utenteStruttura.getIdUtente().getId(), usSalvo);
            }
        }
    }

}
