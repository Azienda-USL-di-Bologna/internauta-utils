package it.bologna.ausl.documentgenerator.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

@Service
public final class AziendaParamsManager {

    private List<AziendaParams> aziendaParamsList;
    private Map<String, Sql2o> dbConnectionMap;
    private Map<String, MongoWrapper> storageConnectionMap;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.datasource.driver-class-name}")
    String driverClass;
    @Value("${spring.datasource.url}")
    String jdbcUrl;
    @Value("${spring.datasource.username}")
    String dbUsername;
    @Value("${spring.datasource.password}")
    String dbPassword;
    @Value("${spring.datasource.hikari.maximumPoolSize}")
    Integer maximumPoolSize;
    @Value("${spring.datasource.hikari.minimumIdle}")
    Integer minimumIdle;
    @Value("${spring.datasource.hikari.idleTimeout}")
    Integer idleTimeout;
    @Value("${spring.datasource.hikari.connectionTimeout}")
    Integer connectionTimeout;

    @PostConstruct
    public void init() throws IOException, UnknownHostException, MongoException, MongoWrapperException {

        // carico i parametri dalle aziende
        HikariConfig hikariConfigInternauta = new HikariConfig();
        hikariConfigInternauta.setDriverClassName(driverClass);
        hikariConfigInternauta.setJdbcUrl(jdbcUrl);
        hikariConfigInternauta.setUsername(dbUsername);
        hikariConfigInternauta.setPassword(dbPassword);
        hikariConfigInternauta.setLeakDetectionThreshold(20000);
        hikariConfigInternauta.setMaximumPoolSize(maximumPoolSize);
        hikariConfigInternauta.setMinimumIdle(minimumIdle);
        hikariConfigInternauta.setIdleTimeout(idleTimeout);
        hikariConfigInternauta.setConnectionTimeout(connectionTimeout);
        HikariDataSource hikariDataSourceInternauta = new HikariDataSource(hikariConfigInternauta);

        Sql2o sql2oInternauta = new Sql2o(hikariDataSourceInternauta);

        aziendaParamsList = getConnParams(sql2oInternauta);

        dbConnectionMap = new HashMap<>();
        storageConnectionMap = new HashMap<>();

        // popolo le mappe con le connssioni per ogni azienda
        for (AziendaParams aziendaConnParams : aziendaParamsList) {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName(driverClass);
            hikariConfig.setJdbcUrl(aziendaConnParams.getJdbcUrl());
            hikariConfig.setUsername(aziendaConnParams.getDbUsername());
            hikariConfig.setPassword(aziendaConnParams.getDbPassword());
            hikariConfig.setLeakDetectionThreshold(20000);
            hikariConfig.setMaximumPoolSize(maximumPoolSize);

            HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
            Sql2o sql2o = new Sql2o(hikariDataSource);
            dbConnectionMap.put(aziendaConnParams.getCodiceAzienda(), sql2o);

            Map<String, Object> minIOConfig = aziendaConnParams.getMinIOConfigMap(objectMapper);
            Boolean minIOActive = (Boolean) minIOConfig.get("active");
            String minIODBDriver = (String) minIOConfig.get("DBDriver");
            String minIODBUrl = (String) minIOConfig.get("DBUrl");
            String minIODBUsername = (String) minIOConfig.get("DBUsername");
            String minIODBPassword = (String) minIOConfig.get("DBPassword");
            MongoWrapper mongoWrapper = MongoWrapper.getWrapper(minIOActive, aziendaConnParams.getStorageConnString(), minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, aziendaConnParams.getCodiceAzienda(), objectMapper);
            storageConnectionMap.put(aziendaConnParams.getCodiceAzienda(), mongoWrapper);
        }
    }

    public Sql2o getDbConnection(String codiceAzienda) {
        return dbConnectionMap.get(codiceAzienda);
    }

    public MongoWrapper getStorageConnection(String codiceAzienda) {
        MongoWrapper mongoWrapper = storageConnectionMap.get(codiceAzienda);
        return mongoWrapper;
    }

    public List<AziendaParams> getConnParams(Sql2o sql2o) {
        String sqlAziende = "select "
                + "codice as codiceAzienda, "
                + "parametri -> 'dbConnParams' ->> 'jdbcUrl' as jdbcUrl, "
                + "parametri -> 'dbConnParams' ->> 'username' as dbUsername, "
                + "parametri -> 'dbConnParams' ->> 'password' as dbPassword, "
                + "parametri -> 'mongoParams' ->> 'connectionString' as storageConnString, "
                + "parametri ->> 'babelSuiteWebApiUrl' as babelSuiteWebApiUrl, "
                + "(select valore from configurazione.parametri_aziende pa where pa.nome = 'minIOConfig' and a.id = any(id_aziende)) as minIOConfig "
                + "from baborg.aziende a";
        try (Connection conn = (Connection) sql2o.open()) {
            return conn.createQuery(sqlAziende)
                    .executeAndFetch(AziendaParams.class);
        }

    }

    public AziendaParams getAziendaParam(String codiceAzienda) {
        return aziendaParamsList.stream().filter(a -> (a.getCodiceAzienda().equals(codiceAzienda))).findFirst().orElse(null);
    }
}
