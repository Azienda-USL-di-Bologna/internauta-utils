package it.bologna.ausl.model.entities.baborg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;

/**
 *
 * @author gdm
 */
public class AziendaParametriJson implements Serializable {

    private String babelSuiteBdsToolsUrl;
    private String babelSuiteWebApiUrl;
    private String descBreve;
    private String loginSSOField;
    private String loginDBField;
    private String loginDBFieldBaborg;
    private String shalboApiUrl;
    private MasterChefParmas masterchefParams;
    private String entityId;
    private String loginPath;
    private String basePath;
    private String InternetBasePath;
    private String crossLoginUrlTemplate;
    private String simpleCrossLoginUrlTemplate;
    private MongoParams mongoParams;
    private String logoutUrl;
    private String InternetLogoutUrl;
    private MailParams mailParams;
    private DbConnParams dbConnParams;
    //private String mongoConnectionString;

    public AziendaParametriJson() {
    }

    public AziendaParametriJson(String babelSuiteBdsToolsUrl, String babelSuiteWebApiUrl, String descBreve, String loginSSOField, String loginDBField, String loginDBFieldBaborg, String shalboApiUrl, MasterChefParmas masterchefParams, String entityId, String loginPath, String basePath, String InternetBasePath, String crossLoginUrlTemplate, String simpleCrossLoginUrlTemplate, MongoParams mongoParams, String logoutUrl, String InternetLogoutUrl, MailParams mailParams, DbConnParams dbConnParams) {
        this.babelSuiteBdsToolsUrl = babelSuiteBdsToolsUrl;
        this.babelSuiteWebApiUrl = babelSuiteWebApiUrl;
        this.descBreve = descBreve;
        this.loginSSOField = loginSSOField;
        this.loginDBField = loginDBField;
        this.loginDBFieldBaborg = loginDBFieldBaborg;
        this.shalboApiUrl = shalboApiUrl;
        this.masterchefParams = masterchefParams;
        this.entityId = entityId;
        this.loginPath = loginPath;
        this.basePath = basePath;
        this.InternetBasePath = InternetBasePath;
        this.crossLoginUrlTemplate = crossLoginUrlTemplate;
        this.simpleCrossLoginUrlTemplate = simpleCrossLoginUrlTemplate;
        this.mongoParams = mongoParams;
        this.logoutUrl = logoutUrl;
        this.InternetLogoutUrl = InternetLogoutUrl;
        this.mailParams = mailParams;
        this.dbConnParams = dbConnParams;
    }

    public MailParams getMailParams() {
        return mailParams;
    }

    public void setMailParams(MailParams mailParams) {
        this.mailParams = mailParams;
    }

    public String getBabelSuiteBdsToolsUrl() {
        return babelSuiteBdsToolsUrl;
    }

    public void setBabelSuiteBdsToolsUrl(String babelSuiteBdsToolsUrl) {
        this.babelSuiteBdsToolsUrl = babelSuiteBdsToolsUrl;
    }

    public String getBabelSuiteWebApiUrl() {
        return babelSuiteWebApiUrl;
    }

    public void setBabelSuiteWebApiUrl(String babelSuiteWebApiUrl) {
        this.babelSuiteWebApiUrl = babelSuiteWebApiUrl;
    }

    public String getDescBreve() {
        return descBreve;
    }

    public void setDescBreve(String descBreve) {
        this.descBreve = descBreve;
    }

    public String getLoginSSOField() {
        return loginSSOField;
    }

    public void setLoginSSOField(String loginSSOField) {
        this.loginSSOField = loginSSOField;
    }

    public String getLoginDBField() {
        return loginDBField;
    }

    public void setLoginDBField(String loginDBField) {
        this.loginDBField = loginDBField;
    }

    public String getLoginDBFieldBaborg() {
        return loginDBFieldBaborg;
    }

    public void setLoginDBFieldBaborg(String loginDBFieldBaborg) {
        this.loginDBFieldBaborg = loginDBFieldBaborg;
    }

    public String getShalboApiUrl() {
        return shalboApiUrl;
    }

    public void setShalboApiUrl(String shalboApiUrl) {
        this.shalboApiUrl = shalboApiUrl;
    }

    public MasterChefParmas getMasterchefParams() {
        return masterchefParams;
    }

    public void setMasterchefParams(MasterChefParmas masterchefParams) {
        this.masterchefParams = masterchefParams;
    }

    public MongoParams getMongoParams() {
        return mongoParams;
    }

    public void setMongoParams(MongoParams mongoParams) {
        this.mongoParams = mongoParams;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getLoginPath() {
        return loginPath;
    }

    public void setLoginPath(String loginPath) {
        this.loginPath = loginPath;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getCrossLoginUrlTemplate() {
        return crossLoginUrlTemplate;
    }

    public void setCrossLoginUrlTemplate(String crossLoginUrlTemplate) {
        this.crossLoginUrlTemplate = crossLoginUrlTemplate;
    }

    public String getSimpleCrossLoginUrlTemplate() {
        return simpleCrossLoginUrlTemplate;
    }

    public void setSimpleCrossLoginUrlTemplate(String simpleCrossLoginUrlTemplate) {
        this.simpleCrossLoginUrlTemplate = simpleCrossLoginUrlTemplate;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }

    public String getInternetBasePath() {
        return InternetBasePath;
    }

    public void setInternetBasePath(String InternetBasePath) {
        this.InternetBasePath = InternetBasePath;
    }

    public String getInternetLogoutUrl() {
        return InternetLogoutUrl;
    }

    public void setInternetLogoutUrl(String InternetLogoutUrl) {
        this.InternetLogoutUrl = InternetLogoutUrl;
    }

    public DbConnParams getDbConnParams() {
        return dbConnParams;
    }

    public void setDbConnParams(DbConnParams dbConnParams) {
        this.dbConnParams = dbConnParams;
    }

//    public String getMongoConnectionString() {
//        return mongoConnectionString;
//    }
//
//    public void setMongoConnectionString(String mongoConnectionString) {
//        this.mongoConnectionString = mongoConnectionString;
//    }
    public static AziendaParametriJson parse(ObjectMapper objectMapper, String src) throws IOException {
        return objectMapper.readValue(src, AziendaParametriJson.class);
    }

    public static String dumpToString(ObjectMapper objectMapper, AziendaParametriJson aziendaParametriJson) throws JsonProcessingException {
        return objectMapper.writeValueAsString(aziendaParametriJson);
    }

    public class MasterChefParmas {

        private String redisHost;
        private Integer redisPort;
        private String inQueue;

        public MasterChefParmas() {
        }

        public MasterChefParmas(String redisHost, Integer redisPort, String inQueue) {
            this.redisHost = redisHost;
            this.redisPort = redisPort;
            this.inQueue = inQueue;
        }

        public String getRedisHost() {
            return redisHost;
        }

        public void setRedisHost(String redisHost) {
            this.redisHost = redisHost;
        }

        public Integer getRedisPort() {
            return redisPort;
        }

        public void setRedisPort(Integer redisPort) {
            this.redisPort = redisPort;
        }

        public String getInQueue() {
            return inQueue;
        }

        public void setInQueue(String inQueue) {
            this.inQueue = inQueue;
        }
    }

    public static class MongoParams {

        private String connectionString;
        private String root;

        public MongoParams() {
        }

        public MongoParams(String connectionString, String root) {
            this.connectionString = connectionString;
            this.root = root;
        }

        public String getConnectionString() {
            return connectionString;
        }

        public void setConnectionString(String connectionString) {
            this.connectionString = connectionString;
        }

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }
    }

    public class MailParams {

        private String mailServerSmtpUrl;
        private Integer mailServerSmtpPort;
        private String mailFrom;
        private Boolean sslAuth = false;
        private String username;
        private String password;

        public MailParams() {
        }

        public MailParams(Integer mailServerSmtpPort, String mailServerSmtpUrl, String mailFrom, Boolean sslAuth, String username, String password) {
            this.mailServerSmtpPort = mailServerSmtpPort;
            this.mailServerSmtpUrl = mailServerSmtpUrl;
            this.mailFrom = mailFrom;
            this.sslAuth = sslAuth;
            this.username = username;
            this.password = password;
        }

        public Integer getMailServerSmtpPort() {
            return mailServerSmtpPort;
        }

        public void setMailServerSmtpPort(Integer mailServerSmtpPort) {
            this.mailServerSmtpPort = mailServerSmtpPort;
        }

        public String getMailServerSmtpUrl() {
            return mailServerSmtpUrl;
        }

        public void setMailServerSmtpUrl(String mailServerSmtpUrl) {
            this.mailServerSmtpUrl = mailServerSmtpUrl;
        }

        public String getMailFrom() {
            return mailFrom;
        }

        public void setMailFrom(String mailFrom) {
            this.mailFrom = mailFrom;
        }

        public Boolean getSslAuth() {
            return sslAuth;
        }

        public void setSslAuth(Boolean sslAuth) {
            this.sslAuth = sslAuth;
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
    }

    public class DbConnParams {

        private String jdbcUrl;
        private String password;
        private String username;
        private String driverClass;
        private Integer maximumPoolSize;
        private Integer leakDetectionThreshold;

        public DbConnParams() {
        }

        public DbConnParams(String jdbcUrl, String password, String username, String driverClass, Integer maximumPoolSize, Integer leakDetectionThreshold) {
            this.jdbcUrl = jdbcUrl;
            this.password = password;
            this.username = username;
            this.driverClass = driverClass;
            this.maximumPoolSize = maximumPoolSize;
            this.leakDetectionThreshold = leakDetectionThreshold;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getDriverClass() {
            return driverClass;
        }

        public void setDriverClass(String driverClass) {
            this.driverClass = driverClass;
        }

        public Integer getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(Integer maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public Integer getLeakDetectionThreshold() {
            return leakDetectionThreshold;
        }

        public void setLeakDetectionThreshold(Integer leakDetectionThreshold) {
            this.leakDetectionThreshold = leakDetectionThreshold;
        }

    }

}
