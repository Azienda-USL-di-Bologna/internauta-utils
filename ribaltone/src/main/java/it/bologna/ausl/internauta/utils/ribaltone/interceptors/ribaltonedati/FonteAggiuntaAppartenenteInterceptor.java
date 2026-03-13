package it.bologna.ausl.internauta.utils.ribaltone.interceptors.ribaltonedati;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.authorizationutils.session.AuthenticatedSessionData;
import it.bologna.ausl.internauta.utils.ribaltone.interceptors.RibaltoneBaseInterceptor;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationsUtils;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.FonteAggiuntaAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiAppartenente;
import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
@NextSdrInterceptor(name = "fonteaggiuntaappartenente-interceptor")
public class FonteAggiuntaAppartenenteInterceptor extends RibaltoneBaseInterceptor {

    @Autowired
    private RepositoryFactory repositoryFactory;

    @Override
    public Class getTargetEntityClass() {
        return FonteAggiuntaAppartenente.class;
    }

    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        FonteAggiuntaAppartenente fonteAggiuntaAppartenente = (FonteAggiuntaAppartenente) entity;
        JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
        QDatiImportatiAppartenente qDatiImportatiAppartenente = QDatiImportatiAppartenente.datiImportatiAppartenente;
        QStruttura qStruttura = QStruttura.struttura;
        fonteAggiuntaAppartenente.setCodiceMatricola(fonteAggiuntaAppartenente.getCodiceMatricola());
        Struttura struttura = queryFactory
                .select(qStruttura)
                .from(qStruttura)
                .where(
                        qStruttura.attiva.and(
                                qStruttura.idCasella.eq(fonteAggiuntaAppartenente.getIdCasella())
                        ).and(
                                qStruttura.idAzienda.id.eq(fonteAggiuntaAppartenente.getIdAzienda())
                        )).fetchOne();

        if (struttura != null) {
            DatiImportatiAppartenente datiImportatiAppartenente = queryFactory
                    .select(qDatiImportatiAppartenente)
                    .from(qDatiImportatiAppartenente)
                    .where(
                            qDatiImportatiAppartenente.codiceFiscale.eq(fonteAggiuntaAppartenente.getCodiceFiscale())
                                    .and(qDatiImportatiAppartenente.idAzienda.eq(struttura.getIdAzienda().getId()))
                    )
                    .limit(1)
                    .fetchOne();
            if (datiImportatiAppartenente == null) {
                datiImportatiAppartenente = queryFactory
                        .select(qDatiImportatiAppartenente)
                        .from(qDatiImportatiAppartenente)
                        .where(
                                qDatiImportatiAppartenente.codiceFiscale.eq(fonteAggiuntaAppartenente.getCodiceFiscale())
                        )
                        .limit(1)
                        .fetchOne();
            }
            fonteAggiuntaAppartenente.setCodiceMatricola(datiImportatiAppartenente != null ? datiImportatiAppartenente.getCodiceMatricola() : null);
            fonteAggiuntaAppartenente.setCodiceAzienda(struttura.getIdAzienda().getCodice());
            fonteAggiuntaAppartenente.setCodiceEnte(struttura.getIdAzienda().getCodice() + "01");
        }
        return fonteAggiuntaAppartenente;
    }

    @Override
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        QStruttura qStruttura = QStruttura.struttura;
        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
//        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        FonteAggiuntaAppartenente fonteAggiuntaAppartenente = (FonteAggiuntaAppartenente) entity;
        DatiImportatiAppartenente datoImportatoAppartenente = fonteAggiuntaAppartenente.buildDatoImportatoAppartenente();
        JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
        Struttura struttura = queryFactory
                .select(qStruttura)
                .from(qStruttura)
                .where(qStruttura.attiva.and(qStruttura.idCasella.eq(fonteAggiuntaAppartenente.getIdCasella()))
                        .and(qStruttura.idAzienda.id.eq(fonteAggiuntaAppartenente.getIdAzienda())))
                .fetchOne();
        //vuol dire che sto aggiungendo un utente alla struttura sorgente di replica

        OperationsUtils.insertUtenteInStruttura(queryFactory, fonteAggiuntaAppartenente.buildDatidaImportare(), struttura, repositoryFactory.getEntityManager(), repositoryFactory.getPermissionManager(), null, fonteAggiuntaAppartenente.getId());

        //gestito le repliche
        List<Struttura> struttureReplicate = queryFactory
                .select(qStruttura)
                .from(qStruttura)
                .where(qStruttura.attiva.and(qStruttura.idStrutturaReplicata.id.eq(struttura.getId()))).limit(1)
                .fetch();
        if (struttureReplicate != null) {
            for (Struttura strutturaReplica : struttureReplicate) {
                OperationsUtils.insertUtenteInStruttura(
                        queryFactory,
                        fonteAggiuntaAppartenente.buildDatidaImportare(),
                        strutturaReplica,
                        repositoryFactory.getEntityManager(),
                        repositoryFactory.getPermissionManager(),
                        null);
            }
        }
        // gestisco le fusioni
        List<StrutturaUnificata> struttureUnificate = queryFactory
                .select(qStrutturaUnificata)
                .from(qStrutturaUnificata)
                .where(
                        (qStrutturaUnificata.idStrutturaSorgente.idCasella.eq(fonteAggiuntaAppartenente.getIdCasella())
                                .or(qStrutturaUnificata.idStrutturaDestinazione.idCasella.eq(fonteAggiuntaAppartenente.getIdCasella())))
                                .and(qStrutturaUnificata.dataAccensioneAttivazione.before(ZonedDateTime.now()))
                                .and(qStrutturaUnificata.dataDisattivazione.isNull())
                                .and(qStrutturaUnificata.tipoOperazione.eq(StrutturaUnificata.TipoUnificazione.FUSIONE)))
                .fetch();
        for (StrutturaUnificata strutturaUnificata : struttureUnificate) {
            Struttura strutturaDoveInserire = null;
            if (strutturaUnificata.getIdStrutturaDestinazione().getIdCasella().equals(fonteAggiuntaAppartenente.getIdCasella())) {
                //da aggiungere a sorgente
                strutturaDoveInserire = strutturaUnificata.getIdStrutturaSorgente();
            } else {
                //da aggiungere a destinazione
                strutturaDoveInserire = strutturaUnificata.getIdStrutturaDestinazione();
            }
            OperationsUtils.insertUtenteInStruttura(
                    queryFactory,
                    fonteAggiuntaAppartenente.buildDatidaImportare(),
                    strutturaDoveInserire,
                    repositoryFactory.getEntityManager(),
                    repositoryFactory.getPermissionManager(),
                    null);
        }
        repositoryFactory.getEntityManager().persist(datoImportatoAppartenente);

        return entity;
    }

    @Override
    public void afterDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
//        QStruttura qStruttura = QStruttura.struttura;
//        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
//        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
//        FonteAggiuntaAppartenente fonteAggiuntaAppartenente = (FonteAggiuntaAppartenente) entity;
//        JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
//        Struttura struttura = queryFactory
//            .select(qStruttura)
//            .from(qStruttura)
//            .where(qStruttura.attiva.and(qStruttura.idCasella.eq(fonteAggiuntaAppartenente.getIdCasella()))).limit(1)
//            .fetchOne();
//        //da ragionare bene su utenti unificati
//        OperationsUtils.chiudiUtenteStruttura(fonteAggiuntaAppartenente.buildDatidaImportare().buildDatiImportati(), struttura, fonteAggiuntaAppartenente.getIdAzienda(), queryFactory, repositoryFactory.getPermissionManager(), repositoryFactory.getEntityManager(), null);
//
//        //gestito le repliche
//        List<Struttura> struttureReplicate = queryFactory
//            .select(qStruttura)
//            .from(qStruttura)
//            .where(qStruttura.attiva.and(qStruttura.idStrutturaReplicata.idCasella.eq(fonteAggiuntaAppartenente.getIdCasella()))).limit(1)
//            .fetch();
//        if (struttureReplicate != null) {
//            for (Struttura strutturaReplica : struttureReplicate) {
//                OperationsUtils.chiudiUtenteStruttura(
//                    fonteAggiuntaAppartenente.buildDatidaImportare().buildDatiImportati(),
//                    strutturaReplica,
//                    strutturaReplica.getIdAzienda().getId(),
//                    queryFactory,
//                    repositoryFactory.getPermissionManager(),
//                    repositoryFactory.getEntityManager(),
//                    null);
//            }
//        }
//        // gestisco le fusioni
//        List<StrutturaUnificata> struttureUnificate = queryFactory
//            .select(qStrutturaUnificata)
//            .from(qStrutturaUnificata)
//            .where(
//                (qStrutturaUnificata.idStrutturaSorgente.idCasella.eq(fonteAggiuntaAppartenente.getIdCasella())
//                    .or(qStrutturaUnificata.idStrutturaDestinazione.idCasella.eq(fonteAggiuntaAppartenente.getIdCasella())))
//                    .and(qStrutturaUnificata.dataAccensioneAttivazione.after(ZonedDateTime.now()))
//                    .and(qStrutturaUnificata.dataDisattivazione.isNull())
//                    .and(qStrutturaUnificata.tipoOperazione.eq(StrutturaUnificata.TipoUnificazione.FUSIONE)))
//            .fetch();
//        for (StrutturaUnificata strutturaUnificata : struttureUnificate) {
//            Struttura strutturaDoveRimuovere = null;
//            if (strutturaUnificata.getIdStrutturaDestinazione().getIdCasella().equals(fonteAggiuntaAppartenente.getIdCasella())) {
//                //da aggiungere a sorgente
//                strutturaDoveRimuovere = strutturaUnificata.getIdStrutturaSorgente();
//            } else {
//                //da aggiungere a destinazione
//                strutturaDoveRimuovere = strutturaUnificata.getIdStrutturaDestinazione();
//            }
//            OperationsUtils.chiudiUtenteStruttura(
//                fonteAggiuntaAppartenente.buildDatidaImportare().buildDatiImportati(),
//                strutturaDoveRimuovere,
//                strutturaDoveRimuovere.getIdAzienda().getId(),
//                queryFactory,
//                repositoryFactory.getPermissionManager(),
//                repositoryFactory.getEntityManager(),
//                null);
//        }
    }

    @Override
    public Object afterUpdateEntityInterceptor(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException {
        QStruttura qStruttura = QStruttura.struttura;
        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
//        AuthenticatedSessionData authenticatedSessionData = getAuthenticatedUserProperties();
        FonteAggiuntaAppartenente fonteAggiuntaAppartenente = (FonteAggiuntaAppartenente) entity;
        JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
        Struttura struttura = queryFactory
                .select(qStruttura)
                .from(qStruttura)
                .where(qStruttura.attiva.and(qStruttura.idCasella.eq(fonteAggiuntaAppartenente.getIdCasella()))).limit(1)
                .fetchOne();
        //da ragionare bene su utenti unificati
        OperationsUtils.chiudiUtenteStruttura(fonteAggiuntaAppartenente.buildDatidaImportare().buildDatiImportati(), struttura, fonteAggiuntaAppartenente.getIdAzienda(), queryFactory, repositoryFactory.getPermissionManager(), repositoryFactory.getEntityManager(), null);

        //gestito le repliche
        List<Struttura> struttureReplicate = queryFactory
                .select(qStruttura)
                .from(qStruttura)
                .where(qStruttura.attiva.and(qStruttura.idStrutturaReplicata.idCasella.eq(fonteAggiuntaAppartenente.getIdCasella()))).limit(1)
                .fetch();
        if (struttureReplicate != null) {
            for (Struttura strutturaReplica : struttureReplicate) {
                OperationsUtils.chiudiUtenteStruttura(
                        fonteAggiuntaAppartenente.buildDatidaImportare().buildDatiImportati(),
                        strutturaReplica,
                        strutturaReplica.getIdAzienda().getId(),
                        queryFactory,
                        repositoryFactory.getPermissionManager(),
                        repositoryFactory.getEntityManager(),
                        null);
            }
        }
        // gestisco le fusioni
        List<StrutturaUnificata> struttureUnificate = queryFactory
                .select(qStrutturaUnificata)
                .from(qStrutturaUnificata)
                .where(
                        (qStrutturaUnificata.idStrutturaSorgente.idCasella.eq(fonteAggiuntaAppartenente.getIdCasella())
                                .or(qStrutturaUnificata.idStrutturaDestinazione.idCasella.eq(fonteAggiuntaAppartenente.getIdCasella())))
                                .and(qStrutturaUnificata.dataAccensioneAttivazione.before(ZonedDateTime.now()))
                                .and(qStrutturaUnificata.dataDisattivazione.isNull())
                                .and(qStrutturaUnificata.tipoOperazione.eq(StrutturaUnificata.TipoUnificazione.FUSIONE)))
                .fetch();
        for (StrutturaUnificata strutturaUnificata : struttureUnificate) {
            Struttura strutturaDoveRimuovere = null;
            if (strutturaUnificata.getIdStrutturaDestinazione().getIdCasella().equals(fonteAggiuntaAppartenente.getIdCasella())) {
                //da aggiungere a sorgente
                strutturaDoveRimuovere = strutturaUnificata.getIdStrutturaSorgente();
            } else {
                //da aggiungere a destinazione
                strutturaDoveRimuovere = strutturaUnificata.getIdStrutturaDestinazione();
            }
            OperationsUtils.chiudiUtenteStruttura(
                    fonteAggiuntaAppartenente.buildDatidaImportare().buildDatiImportati(),
                    strutturaDoveRimuovere,
                    strutturaDoveRimuovere.getIdAzienda().getId(),
                    queryFactory,
                    repositoryFactory.getPermissionManager(),
                    repositoryFactory.getEntityManager(),
                    null);
        }
        return entity;
    }

}
