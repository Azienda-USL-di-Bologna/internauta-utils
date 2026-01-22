package it.bologna.ausl.internauta.utils.ribaltone.plugin.gru;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SpecificData;
import java.util.List;
import org.springframework.util.StringUtils;
import org.sql2o.Sql2o;

/**
 *
 * @author Top
 */
public class GruSpecificData extends SpecificData {
    private QueryRecuperoDati queryRecuperoDati;
    private List<String> codiciEntiValidi;
    private Connessione connessione;

    public GruSpecificData() {
    }
    
    public QueryRecuperoDati getQueryRecuperoDati() {
        return queryRecuperoDati;
    }

    public void setQueryRecuperoDati(QueryRecuperoDati queryRecuperoDati) {
        this.queryRecuperoDati = queryRecuperoDati;
    }

    public List<String> getCodiciEntiValidi() {
        return codiciEntiValidi;
    }

    public void setCodiciEntiValidi(List<String> codiciEntiValidi) {
        this.codiciEntiValidi = codiciEntiValidi;
    }

    public Connessione getConnessione() {
        return connessione;
    }

    public void setConnessione(Connessione connessione) {
        this.connessione = connessione;
    }

    public static class Connessione {
        private String tipologia;
        private String driver;
        private String url;
        private String port;
        private String username;
        private String password;
        private String sql2oMinIdleSize;
        private String sql2oMaxPoolSize;
        private String sql2oConnectionTimeout;
        
        private Boolean inizialized = false;
        private Sql2o sql2oConnecion;

        public Connessione(String driver, String url, String port, String username, String password, String sql2oMinIdleSize, String sql2oMaxPoolSize, String sql2oConnectionTimeout,String tipologia) {
            this.driver = driver;
            this.url = url;
            this.port = port;
            this.username = username;
            this.password = password;
            this.sql2oMinIdleSize = sql2oMinIdleSize;
            this.sql2oMaxPoolSize = sql2oMaxPoolSize;
            this.sql2oConnectionTimeout = sql2oConnectionTimeout;
            this.tipologia = tipologia;
                 
        }
        public Boolean build(){
            
            this.sql2oConnecion = getConnection();
            
            if (this.sql2oConnecion != null) {
                this.inizialized = true;
            }
            return this.inizialized;
        }

        public Connessione() {
        }
        
        public String getDriver() {
            return driver;
        }

        public void setDriver(String driver) {
            this.driver = driver;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getSql2oMinIdleSize() {
            return sql2oMinIdleSize;
        }

        public void setSql2oMinIdleSize(String sql2oMinIdleSize) {
            this.sql2oMinIdleSize = sql2oMinIdleSize;
        }

        public String getSql2oMaxPoolSize() {
            return sql2oMaxPoolSize;
        }

        public void setSql2oMaxPoolSize(String sql2oMaxPoolSize) {
            this.sql2oMaxPoolSize = sql2oMaxPoolSize;
        }

        public String getSql2oConnectionTimeout() {
            return sql2oConnectionTimeout;
        }

        public void setSql2oConnectionTimeout(String sql2oConnectionTimeout) {
            this.sql2oConnectionTimeout = sql2oConnectionTimeout;
        }

        public Boolean getInizialized() {
            return inizialized;
        }

        public void setInizialized(Boolean inizialized) {
            this.inizialized = inizialized;
        }

        public Sql2o getSql2oConnecion() {
            return sql2oConnecion;
        }

        public void setSql2oConnecion(Sql2o sql2oConnecion) {
            this.sql2oConnecion = sql2oConnecion;
        }

        public String getTipologia() {
            return tipologia;
        }

        public void setTipologia(String tipologia) {
            this.tipologia = tipologia;
        }
        
        public Sql2o getConnection() {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName(this.getDriver());
            if (StringUtils.hasText(this.getUrl()) && StringUtils.hasText(this.getPort())) {
                hikariConfig.setJdbcUrl(((String) this.getUrl()).replaceAll("(jdbc:oracle:thin:@)(.+):(\\d+)\\/(.+)",
                        String.format("$1%s:%s/$4", this.getUrl(), this.getPort())));

            } else {
                hikariConfig.setJdbcUrl((String) this.getUrl());
            }
            hikariConfig.setUsername((String) this.getUsername());
            hikariConfig.setPassword((String) this.getPassword());
            hikariConfig.setMinimumIdle(Integer.parseInt(this.getSql2oMinIdleSize()));
            hikariConfig.setMaximumPoolSize(Integer.parseInt(this.getSql2oMaxPoolSize()));
            hikariConfig.setConnectionTimeout(Integer.parseInt(this.getSql2oConnectionTimeout()));
            HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
            return new Sql2o(hikariDataSource);
        }

    }    
}
