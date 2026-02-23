/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.pluginutils;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.FonteAggiuntaAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.FonteAggiuntaAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.QFonteAggiuntaAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.QFonteAggiuntaAppartenente;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author Top
 */
public class FonteAggiuntaDataManager extends SourceDataManager {

    private final EntityManager entityManager;
    private final String codiceAzienda;

    public FonteAggiuntaDataManager(SpecificData specificDataConf, ObjectMapper objectMapper, String codiceAzienda, Integer idAzienda, EntityManager entityManager) {
        super(specificDataConf, objectMapper, codiceAzienda, idAzienda);
        this.entityManager = entityManager;
        this.codiceAzienda = codiceAzienda;
    }

    @Override
    public List<DatiDaImportareAppartenente> getAppartenenti() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QFonteAggiuntaAppartenente qFonteAggiuntaAppartenente = QFonteAggiuntaAppartenente.fonteAggiuntaAppartenente;
        List<FonteAggiuntaAppartenente> fonteAggiuntaAppartenenteList = queryFactory
            .select(qFonteAggiuntaAppartenente)
            .from(qFonteAggiuntaAppartenente)
            .where(
                qFonteAggiuntaAppartenente.codiceAzienda.eq(codiceAzienda)
                    .and(qFonteAggiuntaAppartenente.datain.before(ZonedDateTime.now()))
                    .and(qFonteAggiuntaAppartenente.datafi.isNull()
                        .or(qFonteAggiuntaAppartenente.datafi.after(ZonedDateTime.now())
                        )
                    )
            ).fetch();
        List<DatiDaImportareAppartenente> datiDaImportareAppartenenteList = new ArrayList<>();
        for (FonteAggiuntaAppartenente fonteAggiuntaAppartenente : fonteAggiuntaAppartenenteList) {
            datiDaImportareAppartenenteList.add(fonteAggiuntaAppartenente.buildDatidaImportare());
        }
        return datiDaImportareAppartenenteList;
    }

    @Override
    public List<DatiDaImportareStruttura> getStrutture() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public List<DatiDaImportareTrasformazione> getTrasformazioni() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public List<DatiDaImportareAnagrafica> getAnagrafica() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QFonteAggiuntaAnagrafica qFonteAggiuntaAnagrafica = QFonteAggiuntaAnagrafica.fonteAggiuntaAnagrafica;
        List<FonteAggiuntaAnagrafica> fonteAggiuntaAnagraficaList = queryFactory
            .select(qFonteAggiuntaAnagrafica)
            .from(qFonteAggiuntaAnagrafica)
            .where(
                qFonteAggiuntaAnagrafica.codiceAzienda.eq(codiceAzienda)).fetch();
        List<DatiDaImportareAnagrafica> datiDaImportareAnagraficaList = new ArrayList<>();
        for (FonteAggiuntaAnagrafica fonteAggiuntaAnagrafica : fonteAggiuntaAnagraficaList) {
            datiDaImportareAnagraficaList.add(fonteAggiuntaAnagrafica.buildDatidaImportare(idAzienda));
        }
        return datiDaImportareAnagraficaList;
    }

}
