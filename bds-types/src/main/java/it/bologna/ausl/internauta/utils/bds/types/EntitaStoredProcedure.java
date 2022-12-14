package it.bologna.ausl.internauta.utils.bds.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.persistence.Transient;

/**
 * @author gdm
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntitaStoredProcedure implements Serializable {

    private Integer idProvenienza;
    private String schema;
    private String table;
    private String descrizione;
    private String additionalData;
    

    public EntitaStoredProcedure() {
    }

    public EntitaStoredProcedure(Integer id, String schema, String table) {
        this.idProvenienza = id;
        this.schema = schema;
        this.table = table;
    }

    public EntitaStoredProcedure(Object pkValue) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @JsonProperty("id_provenienza")
    public Integer getIdProvenienza() {
        return idProvenienza;
    }

    @JsonProperty("id_provenienza")
    public void setIdProvenienza(Integer idProvenienza) {
        this.idProvenienza = idProvenienza;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(String additionalData) {
        this.additionalData = additionalData;
    }
}
