/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.plugin.csv;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.GruDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SourceDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SpecificData;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
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

    public CSVDataManager(SpecificData specificDataConf, ObjectMapper objectMapper, String codiceAzienda, Integer idAzienda) {
        super(specificDataConf, objectMapper, codiceAzienda, idAzienda);

        setCSVSpecificData((CSVSpecificData) specificDataConf);
        this.csvSpecificData = csvSpecificData;
    }

    private void setCSVSpecificData(CSVSpecificData csvSpecificData) {
        this.csvSpecificData = csvSpecificData;
    }

    public CSVSpecificData getGruSpecificData() {
        return csvSpecificData;
    }

    @Override
    public List<DatiDaImportareAppartenente> getAppartenenti() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
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
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
