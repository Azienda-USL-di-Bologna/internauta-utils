/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CAMBIO_PADRE;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.RINOMINA;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Top
 */
public class OperationTrasformazione extends Operation<DatiRibaltoneInterface> implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(OperationTrasformazione.class);

    public OperationTrasformazione(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager) {
        super(azione, entitaCoinvolta, entityManager);
    }

    @Override
    public void esegui(Object workToDo, RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        EntityManager em = repositoryFactory.getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QStruttura qStruttura = QStruttura.struttura;
        QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;
        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
        HashMap<Integer, List<Integer>> struttureDaAggiornareConPadreNonAncoraInserito = (HashMap<Integer, List<Integer>>) workToDo;
        switch (getAzione()) {
            case CONFLUENZA -> {
                //se si tratta di una confluenza
                DatiDaImportareTrasformazione trasformazioneDaEseguire = (DatiDaImportareTrasformazione) getEntitaCoinvolta();
                Struttura strutturaChiusa = OperationsUtils.chiudiStruttura(
                    trasformazioneDaEseguire.getIdCasellaPartenza(),
                    trasformazioneDaEseguire.getIdAzienda(),
                    queryFactory,
                    qStruttura,
                    qStoricoRelazione,
                    qStrutturaUnificata,
                    false);

                List<Struttura> struttureArrivo = queryFactory
                    .select(qStruttura)
                    .from(qStruttura)
                    .where(qStruttura.attiva.and(
                        qStruttura.idAzienda.codice.eq(trasformazioneDaEseguire.getCodiceAzienda()).and(
                            qStruttura.idCasella.eq(trasformazioneDaEseguire.getIdCasellaArrivo())))
                    ).fetch();
                if (struttureArrivo == null || struttureArrivo.isEmpty() || struttureArrivo.size() > 1) {
                    throw new RibaltoneHttpException("non trovata struttura destinazione o trovate piu di una struttura attiva con id casella " + trasformazioneDaEseguire.getIdCasellaArrivo().toString());
                }
                //aggiustare unificazione
                Struttura strutturaDestinazione = struttureArrivo.get(0);
                OperationsUtils.aggiustaUnificazioni(
                    trasformazioneDaEseguire.getIdCasellaPartenza(),
                    strutturaDestinazione,
                    em,
                    queryFactory,
                    qStrutturaUnificata);
                //lanciare sposta strutture
                OperationsUtils.spostaStruttura(em, strutturaChiusa.getId(), strutturaDestinazione.getId(), "X", strutturaDestinazione.getDataAttivazione().toString());

            }
            case CAMBIO_PADRE, RINOMINA -> {
                String operazione = getAzione().equals(RINOMINA) ? "R" : "T";
                DatiDaImportareTrasformazione trasformazioneDaEseguire = (DatiDaImportareTrasformazione) getEntitaCoinvolta();

                List<Struttura> struttureCoinvolteInTrasformazione = queryFactory
                    .select(qStruttura)
                    .from(qStruttura)
                    .where(qStruttura.idAzienda.codice.eq(trasformazioneDaEseguire.getCodiceAzienda()).and(
                        qStruttura.idCasella.eq(trasformazioneDaEseguire.getIdCasellaPartenza()))
                    ).orderBy(qStruttura.dataAttivazione.desc()).limit(2).fetch();
                if (struttureCoinvolteInTrasformazione != null
                    && struttureCoinvolteInTrasformazione.size() == 2) {
                    Struttura strutturaChiusa = struttureCoinvolteInTrasformazione.get(1);
                    Struttura strutturaAperta = struttureCoinvolteInTrasformazione.get(0);
                    OperationsUtils.spostaStruttura(em, strutturaChiusa.getId(), strutturaAperta.getId(), operazione, strutturaAperta.getDataAttivazione().toString());

                } else {
                    throw new RibaltoneHttpException("strutture coinvolte in trasformaione di cambio padre o rinomina non trovate");
                }
                //chiudere su baborg strutture old
                //chiudere su baborg storico relazione old
            }
            default -> {
                throw new AssertionError();
            }
        }
    }

    /**
     * per quanto riguarda le strutture
     * nel caso di una rinomina e cambio padre della struttura l'idCasella non cambia quindi
     * in caso di rinomina andrò a cercare il contatto lo andrò a updatare con nuovo nome e nuovo puntamento
     * in caso di un cambio padre della struttura il nome resta il medesimo quindi solo nuovo puntamento
     * in caso di confluenza devo togliere dai gruppi il vecchio contatto e mettere quello della struttura conlfuita
     * per quanto riguarda gli appartenenti
     * il dettaglio di tipo UTENTE_STRUTTURA nel caso di confluenza è un nuovo dettaglio e devo cambiarlo anche nei gruppi
     * il dettaglio di tipo UTENTE_STRUTTURA nel caso di rinomina è lo stesso ma col nome nuovo (cosi non devo gestire i gruppi)
     * il dettaglio di tipo UTENTE_STRUTTURA nel caso di cambio padre è lo stesso non devo fare nulla
     * @param repositoryFactory
     */
    public void menageContattiTrasformati(RepositoryFactory repositoryFactory) {
        QStruttura qStruttura = QStruttura.struttura;
        EntityManager em = repositoryFactory.getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QUtenteStruttura qUtenteStruttura = QUtenteStruttura.utenteStruttura;
        switch (getAzione()) {
            case CONFLUENZA -> {
                //per le strutture avro gia avuto la insert della struttura e quindi ci sarà gia il contatto della destinzazione
                //devo andare nei gruppi e sostituire il contatto della struttura
                //per gli appartenenti invece devo fare tutto
                // 1 cercare contatto utente struttura da morire
                // 2 cercare contatto utente struttura vivo (se utente struttura c'era gia prima di confluenza)
                // 2.1 se non ci sono i dettagli contatti vivi creare il dettaglio vivo
                // 3 nei gruppi dove c'era il contatto ormai morto mettere il vivo
                DatiDaImportareTrasformazione trasformazione = (DatiDaImportareTrasformazione) getEntitaCoinvolta();

                Struttura strutturaPartenza = queryFactory.select(qStruttura).from(qStruttura)
                    .where(qStruttura.idCasella.eq(trasformazione.getIdCasellaPartenza())
                        .and(qStruttura.idAzienda.codice.eq(trasformazione.getCodiceAzienda())))
                    .orderBy(qStruttura.dataAttivazione.desc()).fetchOne();

                Struttura strutturaDestinazione = queryFactory.select(qStruttura).from(qStruttura)
                    .where(qStruttura.idCasella.eq(trasformazione.getIdCasellaArrivo())
                        .and(qStruttura.idAzienda.codice.eq(trasformazione.getCodiceAzienda()))).orderBy(qStruttura.dataAttivazione.desc()).limit(1).fetchOne();
                if (strutturaPartenza != null && strutturaDestinazione != null) {
                    List<GruppiContatti> gruppiDelContattoDaRimuovoreList = strutturaPartenza.getIdContatto().getGruppiDelContattoList();
                    //vado nei gruppi e sostituisco il contatto della struttura vecchia con quello nuovo
                    for (GruppiContatti gruppoConContattoDaRimuovere : gruppiDelContattoDaRimuovoreList) {
                        gruppoConContattoDaRimuovere.setEliminato(Boolean.TRUE);
                        gruppoConContattoDaRimuovere.setEliminatoDa("ribaltone");
                        em.persist(gruppoConContattoDaRimuovere);
                        GruppiContatti gruppiContatti = new GruppiContatti();
                        gruppiContatti.setEliminato(false);
                        gruppiContatti.setIdGruppo(gruppoConContattoDaRimuovere.getIdGruppo());
                        gruppiContatti.setIdContatto(strutturaDestinazione.getIdContatto());
                        gruppiContatti.setIdDettaglioContatto(strutturaDestinazione.getIdContatto().getDettaglioContattoList().stream().filter(dc -> dc.getPrincipale()).toList().get(0));
                        em.persist(gruppiContatti);
                    }
                    // 1 cercare contatto utente struttura da morire
                    List<UtenteStruttura> usDaConsiderare = strutturaPartenza.getUtenteStrutturaList().stream().filter(us -> us.getIdDettaglioContatto() != null)
                        .collect(Collectors.collectingAndThen(
                            Collectors.toMap(
                                us -> Arrays.asList(us.getIdUtente().getId(), us.getIdStruttura().getId()),
                                Function.identity(),
                                (us1, us2) -> us1
                            ),
                            map -> new ArrayList<>(map.values())
                        ));

                    for (UtenteStruttura us : usDaConsiderare) {
                        List<GruppiContatti> gruppiDelDettaglioList = us.getIdDettaglioContatto().getGruppiDelDettaglioList();
//                        2 cercare contatto utente struttura vivo (se utente struttura c'era gia prima di confluenza)
                        UtenteStruttura usNew = us.getIdUtente().getUtenteStrutturaList().stream().filter(u -> u.getAttivo() && u.getIdStruttura().getId().equals(strutturaDestinazione.getId())).toList().get(0);
                        if (usNew != null) {
                            if (usNew.getIdDettaglioContatto() == null) {
                                Contatto idContatto = usNew.getIdUtente().getIdPersona().getIdContatto();
                                DettaglioContatto dettaglioContatto = new DettaglioContatto();
                                dettaglioContatto.setIdContatto(idContatto);
                                dettaglioContatto.setTipo(DettaglioContatto.TipoDettaglio.UTENTE_STRUTTURA);
                                dettaglioContatto.setPrincipale(usNew.getIdAfferenzaStruttura().getCodice().equals(AfferenzaStruttura.CodiciAfferenzaStruttura.DIRETTA));
                                dettaglioContatto.setDescrizione(strutturaDestinazione.getIdContatto().getDescrizione());
                                dettaglioContatto.setIdContattoEsterno(strutturaDestinazione.getIdContatto());
                                usNew.setIdDettaglioContatto(dettaglioContatto);
                                em.persist(usNew);
                                em.refresh(usNew);
                            }
                            for (GruppiContatti gruppoConUsDettaglioDaRimuovere : gruppiDelDettaglioList) {
                                gruppoConUsDettaglioDaRimuovere.setEliminato(Boolean.TRUE);
                                gruppoConUsDettaglioDaRimuovere.setEliminatoDa("ribaltone");
                                em.persist(gruppoConUsDettaglioDaRimuovere);
                                GruppiContatti gruppiContatti = new GruppiContatti();
                                gruppiContatti.setEliminato(false);
                                gruppiContatti.setIdGruppo(gruppoConUsDettaglioDaRimuovere.getIdGruppo());
                                gruppiContatti.setIdContatto(usNew.getIdDettaglioContatto().getIdContatto());
                                gruppiContatti.setIdDettaglioContatto(usNew.getIdDettaglioContatto());
                                em.persist(gruppiContatti);
                            }
                        }

                    }
                }

            }
            case CAMBIO_PADRE -> {
                //devo cambiare il puntamento
                DatiDaImportareTrasformazione entitaRinominata = (DatiDaImportareTrasformazione) getEntitaCoinvolta();
                List<Struttura> struttureCoinvolte = queryFactory.select(qStruttura).from(qStruttura)
                    .where(qStruttura.idCasella.eq(entitaRinominata.getIdCasellaPartenza())
                        .and(qStruttura.idAzienda.codice.eq(entitaRinominata.getCodiceAzienda()))).orderBy(qStruttura.dataAttivazione.desc()).limit(2).fetch();
                Struttura strutturaAttiva;
                Struttura strutturaDisattiva;
                if (struttureCoinvolte != null) {
                    strutturaAttiva = struttureCoinvolte.get(0);
                    strutturaDisattiva = struttureCoinvolte.get(1);
                    Contatto cDisattivo = strutturaDisattiva.getIdContatto();

                    cDisattivo.setIdStruttura(strutturaAttiva);
                    strutturaAttiva.setIdContatto(cDisattivo);
                    strutturaDisattiva.setIdContatto(null);

                    em.persist(cDisattivo);
                    em.persist(strutturaAttiva);
                    em.persist(strutturaDisattiva);

                }
            }
            case RINOMINA -> {
                //per la struttura devo cambiare il la descrizione del contatto e del dettaglio e il puntamento
                //per gli appartenenti devo cambiare la descrizione del dettaglio
                DatiDaImportareTrasformazione entitaRinominata = (DatiDaImportareTrasformazione) getEntitaCoinvolta();
                List<Struttura> struttureCoinvolte = queryFactory.select(qStruttura).from(qStruttura)
                    .where(qStruttura.idCasella.eq(entitaRinominata.getIdCasellaPartenza())
                        .and(qStruttura.idAzienda.codice.eq(entitaRinominata.getCodiceAzienda()))).orderBy(qStruttura.dataAttivazione.desc()).limit(2).fetch();
                Struttura strutturaAttiva;
                Struttura strutturaDisattiva;
                if (struttureCoinvolte != null && struttureCoinvolte.size() == 2) {
                    strutturaAttiva = struttureCoinvolte.get(0);
                    strutturaDisattiva = struttureCoinvolte.get(1);
                    Contatto idContattoStruttura = strutturaDisattiva.getIdContatto();

                    String descrizioneContatto = strutturaAttiva.getNome() + " [" + strutturaAttiva.getIdCasella().toString() + "]";
                    idContattoStruttura.setNome(strutturaAttiva.getNome());
                    idContattoStruttura.setDescrizione(descrizioneContatto);

                    DettaglioContatto dc = idContattoStruttura.getDettaglioContattoList().get(0);
                    dc.setDescrizione("Babel degli utenti di " + descrizioneContatto);

                    em.persist(idContattoStruttura);
                    em.persist(dc);

                    //ora devo pensare a tutti gli utenti struttura
                    List<UtenteStruttura> usAttivi = strutturaAttiva.getUtenteStrutturaList().stream().filter(us -> us.getAttivo()).toList();
                    List<UtenteStruttura> usDisattivi = strutturaAttiva.getUtenteStrutturaList().stream().filter(us -> !us.getAttivo()).toList();
                    for (UtenteStruttura us : usAttivi) {
                        DettaglioContatto idDettaglioContatto = us.getIdDettaglioContatto();
                        if (idDettaglioContatto == null) {
                            idDettaglioContatto = new DettaglioContatto();
                            idDettaglioContatto.setIdContatto(us.getIdUtente().getIdPersona().getIdContatto());
                            idDettaglioContatto.setEliminato(false);
                        }
                        idDettaglioContatto.setIdContattoEsterno(idContattoStruttura);
                        idDettaglioContatto.setDescrizione(descrizioneContatto + " [" + us.getIdUtente().getIdAzienda().getNome() + "]");
                        em.persist(idDettaglioContatto);
                    }

                    for (UtenteStruttura usDis : usDisattivi) {
                        usDis.setIdDettaglioContatto(null);
                        em.persist(usDis);
                    }

                }
            }
            default -> {
                throw new AssertionError();
            }
        }
    }
}
