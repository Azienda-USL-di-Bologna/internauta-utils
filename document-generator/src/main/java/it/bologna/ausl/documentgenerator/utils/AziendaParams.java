package it.bologna.ausl.documentgenerator.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;

/**
 *
 * @author guido
 */
public class AziendaParams {

    private String codiceAzienda;
    private String jdbcUrl;
    private String dbUsername;
    private String dbPassword;
    private String storageConnString;
    private String babelSuiteWebApiUrl;
    private String minIOConfig;

    public AziendaParams() {
    }

    public String getCodiceAzienda() {
        return codiceAzienda;
    }

    public void setCodiceAzienda(String codiceAzienda) {
        this.codiceAzienda = codiceAzienda;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getStorageConnString() {
        return storageConnString;
    }

    public void setStorageConnString(String storageConnString) {
        this.storageConnString = storageConnString;
    }

    public String getBabelSuiteWebApiUrl() {
        return babelSuiteWebApiUrl;
    }

    public void setBabelSuiteWebApiUrl(String babelSuiteWebApiUrl) {
        this.babelSuiteWebApiUrl = babelSuiteWebApiUrl;
    }

    @JsonIgnore
    public Map<String, Object> getMinIOConfigMap(ObjectMapper objm) throws IOException {
        return objm.readValue(minIOConfig, new TypeReference<Map<String, Object>>() {
        });
    }

    public String getMinIOConfig() {
        return minIOConfig;
    }

    public void setMinIOConfig(String minIOConfig) {
        this.minIOConfig = minIOConfig;
    }

}
