package it.bologna.ausl.internauta.utils.ribaltone.plugin.csv;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SourceDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SpecificData;
import it.bologna.ausl.model.entities.ribaltonedati.CSVDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.CSVDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.CSVDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.CSVDaImportareTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
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
        List<CSVDaImportareAppartenente> csvDaImportareList = entityManager.createNativeQuery(
            csvSpecificData.getQueryRecuperoDati().getQueryAppartenenti(), CSVDaImportareAppartenente.class)
            .setParameter(1, idAzienda.toString()).getResultList();

        List<DatiDaImportareAppartenente> datiDaImportareList = new ArrayList<>();
        for (CSVDaImportareAppartenente cSVDaImportareAppartenente : csvDaImportareList) {
            datiDaImportareList.add(cSVDaImportareAppartenente.buildDatiDaImportareAppartenente());
        }
        return datiDaImportareList;
    }

    @Override
    public List<DatiDaImportareStruttura> getStrutture() {
        List<CSVDaImportareStruttura> csvList = entityManager.createNativeQuery(
            csvSpecificData.getQueryRecuperoDati().getQueryStrutture(), CSVDaImportareStruttura.class)
            .setParameter(1, idAzienda.toString()).getResultList();
        List<DatiDaImportareStruttura> resultList = new ArrayList<>();
        for (CSVDaImportareStruttura cSVDaImportareStruttura : csvList) {
            resultList.add(cSVDaImportareStruttura.buildDatiDaImportareStruttura());
        }
        return resultList;
    }

    @Override
    public List<DatiDaImportareTrasformazione> getTrasformazioni() {
        List<CSVDaImportareTrasformazione> csvList = entityManager.createNativeQuery(
            csvSpecificData.getQueryRecuperoDati().getQueryTrasformazioni(), CSVDaImportareTrasformazione.class)
            .setParameter(1, idAzienda.toString()).getResultList();
        List<DatiDaImportareTrasformazione> resultList = new ArrayList<>();
        for (CSVDaImportareTrasformazione cSVDaImportareTrasformazione : csvList) {
            resultList.add(cSVDaImportareTrasformazione.buildDatiDaImportareTrasformazione());
        }
        return resultList;
    }

    @Override
    public List<DatiDaImportareAnagrafica> getAnagrafica() {
        List<CSVDaImportareAnagrafica> csvList = entityManager.createNativeQuery(
            csvSpecificData.getQueryRecuperoDati().getQueryAnagrafiche(), CSVDaImportareAnagrafica.class)
            .setParameter(1, idAzienda.toString()).getResultList();
        List<DatiDaImportareAnagrafica> resultList = new ArrayList<>();
        for (CSVDaImportareAnagrafica cSVDaImportareAnagrafica : csvList) {
            resultList.add(cSVDaImportareAnagrafica.buildDatiDaImportareAnagrafica());
        }
        return resultList;
    }

}
