package it.bologna.ausl.internauta.utils.ribaltone.basedata;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.RibaltoneManagerUtils;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.finalChecks.QueryChecks;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAnagrafica;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAppartenente;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationStruttura;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationTrasformazione;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationUnificazioneAppartenente;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationUnificazioneStruttura;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReport.UserReportType;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReportManager;
import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QAfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.UnificazioneDaGestire;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private Object workToDo = null;

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
        workToDo = new HashMap<Integer, List<Integer>>();
        //devo disabilitare dei trigger che rallenterebbero troppo il ribaltone
        RibaltoneManagerUtils.disableTrigger(repositoryFactory);
        for (OperationStruttura operation : listOfOperationStruttura) {
            operation.esegui(workToDo, repositoryFactory);
            operation.menageContattoStruttura(repositoryFactory);
            repositoryFactory.getEntityManager().flush();
        }
//        OperationsUtils.manageUnificazioni(repositoryFactory.getEntityManager(), listOfOperationStruttura);
        workToDo = null;
        for (OperationAppartenente operation : listOfOperationAppartenente) {
            log.info(operation.toString());
            operation.esegui(workToDo, repositoryFactory);
            operation.menageContattoAppartenente(repositoryFactory);
            repositoryFactory.getEntityManager().flush();
        }
        //qui bisogna scrivere una funzione che va sul db e per ogni afferenza di utenti in listofOperation
        //deve risistemare afferenze funzionali e dirette e mettere il dettaglio principare corretto

        workToDo = new HashMap<Integer, List<Integer>>();
        for (OperationTrasformazione operation : listOfOperationTrasformazione) {
            operation.esegui(workToDo, repositoryFactory);
            operation.menageContattiTrasformati(repositoryFactory);
            repositoryFactory.getEntityManager().flush();
        }
        risistemaAfferenze(repositoryFactory, codiceAzienda);
        workToDo = null;
        for (OperationAnagrafica operation : listOfOperationAnagrafica) {
            operation.esegui(workToDo, repositoryFactory);
            operation.menageContatto(repositoryFactory);
            repositoryFactory.getEntityManager().flush();
        }

        workToDo = new HashMap<Integer, List<Integer>>();
        for (OperationUnificazioneStruttura operation : listOfOperationUnificazioneStruttura) {
            operation.esegui(workToDo, repositoryFactory);
            operation.menageContattoStutturaUnificata(repositoryFactory);
            repositoryFactory.getEntityManager().flush();
        }
        workToDo = null;
        for (OperationUnificazioneAppartenente operation : listOfOperationUnificazioneAppartenente) {
            operation.esegui(workToDo, repositoryFactory);
            operation.menageContattoAppartenenteUnificato(repositoryFactory);
            repositoryFactory.getEntityManager().flush();
        }
        RibaltoneManagerUtils.setOmonimiaOnUtentiOmonimi(repositoryFactory, codiceAzienda);
        RibaltoneManagerUtils.setFogliaOnStrutture(repositoryFactory);
        repositoryFactory.getPermissionManager().spegniPermessiVeicolatiInvalidi();
        QueryChecks.confomalsDataChecks(repositoryFactory, codiceAzienda);

        //devo ricalcolare la gerarchia delle entita
        ricalcolaGerarchiePerAziende(listOfOperationUnificazioneStruttura, codiceAzienda, repositoryFactory);
        //devo abilitare dei trigger che avrebbbero rallentato troppo il ribaltone
        RibaltoneManagerUtils.enableTrigger(repositoryFactory);

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
    private void risistemaAfferenze(RepositoryFactory repositoryFactory, String codiceAzienda) {
        //faccio la query di select
        //count e poi vedo che fare
        QUtenteStruttura us = QUtenteStruttura.utenteStruttura;
        QUtente u = QUtente.utente;
        QAfferenzaStruttura qAfferenzaStruttura = QAfferenzaStruttura.afferenzaStruttura;
        JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
        Integer idAfferenzaFunzionale = queryFactory.select(qAfferenzaStruttura.id).from(qAfferenzaStruttura).where(qAfferenzaStruttura.codice.eq(AfferenzaStruttura.CodiciAfferenzaStruttura.FUNZIONALE.toString())).fetchOne();
        AfferenzaStruttura idAfferenzaDiretta = queryFactory.select(qAfferenzaStruttura).from(qAfferenzaStruttura).where(qAfferenzaStruttura.codice.eq(AfferenzaStruttura.CodiciAfferenzaStruttura.FUNZIONALE.toString())).fetchOne();
        List<Integer> idUtentiConNAfferenzeDirette = queryFactory
            .select(us.idUtente.id)
            .from(us)
            .where(us.attivo.isTrue()
                .and(us.idUtente.idAzienda.codice.eq(codiceAzienda))
                .and(us.idAfferenzaStruttura.id.eq(1)))
            .groupBy(us.idUtente.id)
            .having(us.id.count().gt(1)) // COUNT(id_afferenza_struttura) > 1
            .fetch();

        List<UtenteStruttura> utentiStrutturaDaSistemare = queryFactory
            .select(us)
            .from(us)
            .where(us.attivo.isTrue()
                .and(us.idUtente.idAzienda.codice.eq(codiceAzienda))
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
                    .set(us.idAfferenzaStruttura.id, idAfferenzaFunzionale)
                    .where(us.id.ne(utenteStrutturaDaNonToccare).and(us.idUtente.id.eq(utenteStruttura.getIdUtente().getId())));
                //segno gia sistemata questa persona
                utentiConGiaAfferenzaDiretta.get(utenteStruttura.getIdUtente().getId()).put(2, 1);
            } else if (utentiConGiaAfferenzaDiretta.get(utenteStruttura.getIdUtente().getId()) == null) {
                Map<Integer, Integer> usSalvo = new HashMap<>();
                usSalvo.put(1, utenteStruttura.getId());
                utentiConGiaAfferenzaDiretta.put(utenteStruttura.getIdUtente().getId(), usSalvo);
            }
        }
        QUtenteStruttura us2 = new QUtenteStruttura("us2");

        List<UtenteStruttura> utentiStrutturaFunzionaliSenzaDirette = queryFactory
            .selectFrom(us)
            .where(
                us.attivo.isTrue()
                    .and(us.idUtente.idAzienda.codice.eq(codiceAzienda))
                    .and(us.idAfferenzaStruttura.id.eq(3))
                    // esclude utenti che hanno almeno una diretta attiva
                    .and(
                        us.idUtente.id.notIn(
                            JPAExpressions
                                .select(us2.idUtente.id)
                                .from(us2)
                                .where(
                                    us2.attivo.isTrue()
                                        .and(us2.idAfferenzaStruttura.id.eq(1))
                                        .and(us2.idUtente.idAzienda.codice.eq(codiceAzienda))
                                )
                        )
                    )
            )
            .orderBy(us.idUtente.id.asc()) //
            .fetch();

        Integer idUtenteDiAppoggio = null;

        for (UtenteStruttura utenteStrutturaFunzionale : utentiStrutturaFunzionaliSenzaDirette) {
            if (!Objects.equals(idUtenteDiAppoggio, utenteStrutturaFunzionale.getIdUtente().getId())) {
                idUtenteDiAppoggio = utenteStrutturaFunzionale.getIdUtente().getId();
                utenteStrutturaFunzionale.setIdAfferenzaStruttura(idAfferenzaDiretta);
                repositoryFactory.getEntityManager().persist(utenteStrutturaFunzionale);
                DettaglioContatto idDettaglioContatto = utenteStrutturaFunzionale.getIdDettaglioContatto();
                if (idDettaglioContatto != null) {
                    idDettaglioContatto.setPrincipale(Boolean.TRUE);
                    repositoryFactory.getEntityManager().persist(idDettaglioContatto);
                    repositoryFactory.getEntityManager().flush();
                }
            }

        }
    }

    private void ricalcolaGerarchiePerAziende(List<OperationUnificazioneStruttura> listOfOperationUnificazioneStruttura, String codiceAzienda, RepositoryFactory repositoryFactory) {

        JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
        QAzienda qAzienda = QAzienda.azienda;
        Azienda azienda = queryFactory.select(qAzienda).from(qAzienda).where(qAzienda.codice.eq(codiceAzienda)).fetchOne();
        Set<Integer> idAziende = new HashSet<>();
        idAziende.add(azienda.getId());

        for (OperationUnificazioneStruttura operationUnificazioneStruttura : listOfOperationUnificazioneStruttura) {
            List<UnificazioneDaGestire> unificazioniDaGestire = operationUnificazioneStruttura.getUnificazioniDaGestire();
            for (UnificazioneDaGestire unificazioneDaGestire : unificazioniDaGestire) {
                StrutturaUnificata unificazione = repositoryFactory.getEntityManager().find(StrutturaUnificata.class, unificazioneDaGestire.getIdUnificazione());
                idAziende.add(unificazione.getIdStrutturaSorgente().getIdAzienda().getId());
                idAziende.add(unificazione.getIdStrutturaDestinazione().getIdAzienda().getId());
            }
        }
        for (Integer idAzienda : idAziende) {
            String sistemaGerarchieEntitaStrutture = """
                                  select baborg.sistema_gerarchia_entita_strutture(array[]::integer[], (select id from baborg.strutture where attiva = true and id_struttura_padre is NULL AND NOT ufficio AND id_azienda = :id_azienda))
                                  """.replaceAll(":id_azienda", idAzienda.toString());
            repositoryFactory.getEntityManager().createNativeQuery(sistemaGerarchieEntitaStrutture);
        }
    }

}
