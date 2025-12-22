package it.bologna.ausl.internauta.utils.ribaltone.plugin.csv;

import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.QueryRecuperoDati;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SpecificData;

/**
 *
 * @author MicheleD'Onza
 */
public class CSVSpecificData extends SpecificData {

    private QueryRecuperoDati queryRecuperoDati;

    public QueryRecuperoDati getQueryRecuperoDati() {
        return queryRecuperoDati;
    }

    public void setQueryRecuperoDati(QueryRecuperoDati queryRecuperoDati) {
        this.queryRecuperoDati = queryRecuperoDati;
    }

}
