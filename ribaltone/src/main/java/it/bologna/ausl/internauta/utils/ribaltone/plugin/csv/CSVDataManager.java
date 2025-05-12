/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.plugin.csv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.GruDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SourceDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SpecificData;
import it.bologna.ausl.model.entities.ribaltonedati.CSVDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.CSVDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.QCSVDaImportareAppartenente;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author MicheleD'Onza
 */
public class CSVDataManager extends SourceDataManager {

    private static final Logger LOG = LoggerFactory.getLogger(CSVDataManager.class);

    private CSVSpecificData csvSpecificData;
    private EntityManager entityManager;

    public CSVDataManager(SpecificData specificDataConf, ObjectMapper objectMapper, String codiceAzienda, Integer idAzienda, EntityManager em) {
        super(specificDataConf, objectMapper, codiceAzienda, idAzienda);

        setCSVSpecificData((CSVSpecificData) specificDataConf);
        //this.csvSpecificData = csvSpecificData;
        this.entityManager = em;

    }

    private void setCSVSpecificData(CSVSpecificData csvSpecificData) {
        this.csvSpecificData = csvSpecificData;
    }

    public CSVSpecificData getGruSpecificData() {
        return csvSpecificData;
    }

    @Override
    public List<DatiDaImportareAppartenente> getAppartenenti() {

        List<DatiDaImportareAppartenente> resultList = entityManager.createNativeQuery(
            csvSpecificData.getQueryRecuperoDati().getQueryAppartenenti(), CSVDaImportareAppartenente.class)
            .setParameter(1, idAzienda.toString()).getResultList();

        //select(qCSVDaImportareAppartenente).from(qCSVDaImportareAppartenente).where(qCSVDaImportareAppartenente.idAzienda.eq(this.idAzienda)).fetch();
        return resultList;
    }

    @Override
    public List<DatiDaImportareStruttura> getStrutture() {
        List<DatiDaImportareStruttura> resultList = entityManager.createNativeQuery(
            csvSpecificData.getQueryRecuperoDati().getQueryAppartenenti(), DatiDaImportareStruttura.class)
            .setParameter(1, idAzienda.toString()).getResultList();
        return resultList;
    }

    @Override
    public List<DatiDaImportareTrasformazione> getTrasformazioni() {
        List<DatiDaImportareTrasformazione> resultList = entityManager.createNativeQuery(
            csvSpecificData.getQueryRecuperoDati().getQueryAppartenenti(), DatiDaImportareTrasformazione.class)
            .setParameter(1, idAzienda.toString()).getResultList();
        return resultList;
    }

    @Override
    public List<DatiDaImportareAnagrafica> getAnagrafica() {
        List<DatiDaImportareAnagrafica> resultList = entityManager.createNativeQuery(
            csvSpecificData.getQueryRecuperoDati().getQueryAppartenenti(), DatiDaImportareAnagrafica.class)
            .setParameter(1, idAzienda.toString()).getResultList();
        return resultList;
    }

}
