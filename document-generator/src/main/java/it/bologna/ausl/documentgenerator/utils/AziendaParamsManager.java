package it.bologna.ausl.documentgenerator.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.jenesis4java.True;
import org.sql2o.Sql2o;

public final class AziendaParamsManager {

//    private List<AziendaParams> aziendaParamsList;
    Map<String, AziendaParametriJson> aziendeParams;
    private final Map<String, Sql2o> dbConnectionMap;
    private final Map<String, MongoWrapper> storageConnectionMap;

    private ObjectMapper objectMapper;

//    @Value("${spring.datasource.driver-class-name}")
//    String driverClass;
//    @Value("${spring.datasource.url}")
//    String jdbcUrl;
//    @Value("${spring.datasource.username}")
//    String dbUsername;
//    @Value("${spring.datasource.password}")
//    String dbPassword;
//    @Value("${spring.datasource.hikari.maximumPoolSize}")
//    Integer maximumPoolSize;
//    @Value("${spring.datasource.hikari.minimumIdle}")
//    Integer minimumIdle;
//    @Value("${spring.datasource.hikari.idleTimeout}")
//    Integer idleTimeout;
//    @Value("${spring.datasource.hikari.connectionTimeout}")
//    Integer connectionTimeout;
    public AziendaParamsManager(
            ObjectMapper objectMapper,
            Map<String, AziendaParametriJson> aziendeParams,
            Boolean minIOActive,
            Map<String, Object> minIOConfig) throws IOException, UnknownHostException, MongoException, MongoWrapperException {

        // carico i parametri dalle aziende
//        HikariConfig hikariConfigInternauta = new HikariConfig();
//        hikariConfigInternauta.setDriverClassName(driverClass);
//        hikariConfigInternauta.setJdbcUrl(jdbcUrl);
//        hikariConfigInternauta.setUsername(dbUsername);
//        hikariConfigInternauta.setPassword(dbPassword);
//        hikariConfigInternauta.setLeakDetectionThreshold(20000);
//        hikariConfigInternauta.setMaximumPoolSize(maximumPoolSize);
//        hikariConfigInternauta.setMinimumIdle(minimumIdle);
//        hikariConfigInternauta.setIdleTimeout(idleTimeout);
//        hikariConfigInternauta.setConnectionTimeout(connectionTimeout);
//        HikariDataSource hikariDataSourceInternauta = new HikariDataSource(hikariConfigInternauta);
//
//        Sql2o sql2oInternauta = new Sql2o(hikariDataSourceInternauta);
//
//        aziendaParamsList = getConnParams(sql2oInternauta);
        dbConnectionMap = new HashMap<>();
        storageConnectionMap = new HashMap<>();
        this.aziendeParams = aziendeParams;

        // popolo le mappe con le connssioni per ogni azienda
        for (String codiceAzienda : aziendeParams.keySet()) {
            AziendaParametriJson aziendaParams = aziendeParams.get(codiceAzienda);
            AziendaParametriJson.DbConnParams dbConnParams = aziendaParams.getDbConnParams();
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName(dbConnParams.getDriverClass());
            hikariConfig.setJdbcUrl(dbConnParams.getJdbcUrl());
            hikariConfig.setUsername(dbConnParams.getUsername());
            hikariConfig.setPassword(dbConnParams.getPassword());
            hikariConfig.setLeakDetectionThreshold(dbConnParams.getLeakDetectionThreshold());
            hikariConfig.setMaximumPoolSize(dbConnParams.getMaximumPoolSize());

            HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
            Sql2o sql2o = new Sql2o(hikariDataSource);
            dbConnectionMap.put(codiceAzienda, sql2o);

//            Map<String, Object> minIOConfig = aziendaConnParams.getMinIOConfigMap(objectMapper);
            String minIODBDriver = (String) minIOConfig.get("DBDriver");
            String minIODBUrl = (String) minIOConfig.get("DBUrl");
            String minIODBUsername = (String) minIOConfig.get("DBUsername");
            String minIODBPassword = (String) minIOConfig.get("DBPassword");
            Integer minIOMaxPoolSize = (Integer) minIOConfig.get("maxPoolSize");
            MongoWrapper mongoWrapper = MongoWrapper.getWrapper(minIOActive,
                    aziendaParams.getMongoParams().getConnectionString(),
                    minIODBDriver,
                    minIODBUrl,
                    minIODBUsername,
                    minIODBPassword,
                    codiceAzienda,
                    minIOMaxPoolSize,
                    objectMapper);
            storageConnectionMap.put(codiceAzienda, mongoWrapper);
        }
    }

    public Sql2o getDbConnection(String codiceAzienda) {
        return dbConnectionMap.get(codiceAzienda);
    }

    public MongoWrapper getStorageConnection(String codiceAzienda) {
        MongoWrapper mongoWrapper = storageConnectionMap.get(codiceAzienda);
        return mongoWrapper;
    }
//
//    public List<AziendaParams> getConnParams(Sql2o sql2o) {
//        String sqlAziende = "select "
//                + "codice as codiceAzienda, "
//                + "parametri -> 'dbConnParams' ->> 'jdbcUrl' as jdbcUrl, "
//                + "parametri -> 'dbConnParams' ->> 'username' as dbUsername, "
//                + "parametri -> 'dbConnParams' ->> 'password' as dbPassword, "
//                + "parametri -> 'mongoParams' ->> 'connectionString' as storageConnString, "
//                + "parametri ->> 'babelSuiteWebApiUrl' as babelSuiteWebApiUrl, "
//                + "(select valore from configurazione.parametri_aziende pa where pa.nome = 'minIOConfig' and a.id = any(id_aziende)) as minIOConfig "
//                + "from baborg.aziende a";
//        try (Connection conn = (Connection) sql2o.open()) {
//            return conn.createQuery(sqlAziende)
//                    .executeAndFetch(AziendaParams.class);
//        }
//
//    }

    public AziendaParametriJson getAziendaParam(String codiceAzienda) {
        return aziendeParams.get(codiceAzienda);
    }
}
