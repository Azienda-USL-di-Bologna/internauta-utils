/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Top
 */
public class OperationTrasformazione extends Operation<DatiRibaltoneInterface> implements Serializable {

    public OperationTrasformazione(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager) {
        super(azione, entitaCoinvolta, entityManager);
    }

    @Override
    public void esegui(Object workToDo) throws RibaltoneHttpException {
        EntityManager em = getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QStruttura qStruttura = QStruttura.struttura;
        QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;
        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;

        switch (getAzione()) {
            case INSERT:
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
                                qStruttura.idAzienda.id.eq(trasformazioneDaEseguire.getIdAzienda()).and(
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

                break;


            default:
                throw new AssertionError();
        }
    }

}
