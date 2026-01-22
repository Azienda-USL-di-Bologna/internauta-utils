package it.bologna.ausl.internauta.utils.ribaltone.pluginutils;

import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author Top
 */
public abstract class SourceDataManager {

    protected ObjectMapper objectMapper;

    protected SpecificData specificDataConf;

    protected String codiceAzienda;

    protected Integer idAzienda;

    protected SourceDataManager(SpecificData specificDataConf, ObjectMapper objectMapper, String codiceAzienda, Integer idAzienda) {
        this.specificDataConf = specificDataConf;
        this.objectMapper = objectMapper;
        this.codiceAzienda = codiceAzienda;
        this.idAzienda = idAzienda;
    }

    public SpecificData getSpecificDataConf() {
        return specificDataConf;
    }

    public void setSpecificDataConf(SpecificData specificDataConf) {
        this.specificDataConf = specificDataConf;
    }

    public abstract List<DatiDaImportareAppartenente> getAppartenenti();

    public abstract List<DatiDaImportareStruttura> getStrutture();

    public abstract List<DatiDaImportareTrasformazione> getTrasformazioni();

    public abstract List<DatiDaImportareAnagrafica> getAnagrafica();

}
