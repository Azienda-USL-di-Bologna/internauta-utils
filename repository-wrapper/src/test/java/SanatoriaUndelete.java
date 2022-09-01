
import com.mongodb.MongoException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import it.bologna.ausl.mongowrapper.MongoWrapperMinIO;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import it.bologna.ausl.sql2o.tools.ArrayConverter;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.type.TypeReference;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import org.springframework.util.StringUtils;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

/**
 *
 * @author gdm
 */
public class SanatoriaUndelete {
    private static String DBDriver= "org.postgresql.Driver";
    private static String minIODBUrl = "jdbc:postgresql://babel-big-auslbo.avec.emr.it:5432/minirepo?stringtype=unspecified";
    private static String minIODBUsername= "minirepo";
    private static String minIODBPassword ="la password";
    
    private static String minirepoRestoreDBUrl = "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minireporestore?stringtype=unspecified";
    private static String minirepoRestoreDBUsername= "postgres";
    private static String minirepoRestoreDBPassword ="la password";
    
    private static Sql2o sql2oConnectionMinirepoRestore ;
    
    private static Map<String, String> mongoUriMap;
    private static Map<String, MongoWrapperMinIO> mongoWrapperMap;
    
    public static enum StatiRipristino {
        NOT_CHECKED, RESTORED, MISSING, LOST, ERROR, SKIPPED, UNKNOWN, TO_RESTORE, NEW
    }
    
    public void initialize() throws UnknownHostException, MongoException, MongoWrapperException {
        
        Logger root = (Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        
        mongoUriMap = new HashMap();
        mongoUriMap.put("102", "mongodb://argo102:e37jTcIeTp0w@babel102mongo1.avec.emr.it,babel102mongo2.avec.emr.it/doc102?safe=true&replicaSet=avec0");
        mongoUriMap.put("105", "mongodb://argo:siamocaldi@babelmongo1,babelmongo2/prod?safe=true&replicaSet=prod0");
        mongoUriMap.put("106", "mongodb://argo106:Ushaez4ajei2@babel106mongo1.avec.emr.it,babel106mongo2.avec.emr.it/doc106?safe=true&replicaSet=avec0");
        mongoUriMap.put("109", "mongodb://argo109:xJSE3Sxvy7Dr@babel109mongo1.avec.emr.it,babel109mongo2.avec.emr.it/doc109?safe=true&replicaSet=avec0");
        mongoUriMap.put("902", "mongodb://argo902:gfaL3ssBVo4n@babel902mongo1.avec.emr.it,babel902mongo2.avec.emr.it/doc902?safe=true&replicaSet=avec0");
        mongoUriMap.put("908", "mongodb://argo908:mPw8DApKLaPR@babel908mongo1.avec.emr.it,babel908mongo2.avec.emr.it/doc908?safe=true&replicaSet=avec0");
        mongoUriMap.put("909", "mongodb://argo909:BIKJBnwosLs7@babel909mongo1.avec.emr.it,babel909mongo2.avec.emr.it/doc909?safe=true&replicaSet=avec0");
        mongoUriMap.put("960", "mongodb://argo960:Fj0pdiENBdNU@babel960mongo1.avec.emr.it,babel960mongo2.avec.emr.it/doc960?safe=true&replicaSet=avec0");
        
        mongoWrapperMap = new HashMap();
        for (String codiceAzienda : Arrays.asList("102", "105", "106", "109", "902", "908", "909", "960")) {
            mongoWrapperMap.put(codiceAzienda, (MongoWrapperMinIO) MongoWrapper.getWrapper(true, mongoUriMap.get(codiceAzienda), DBDriver, minIODBUrl, minIODBUsername, minIODBPassword, codiceAzienda, null));  
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(DBDriver);
        hikariConfig.setJdbcUrl(minirepoRestoreDBUrl);
        hikariConfig.setUsername(minirepoRestoreDBUsername);
        hikariConfig.setPassword(minirepoRestoreDBPassword);
        // hikariConfig.setLeakDetectionThreshold(20000);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(5);
        // hikariConfig.getConnectionTimeout();
        hikariConfig.setConnectionTimeout(60000);
        HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
//            Sql2o sql2o = new Sql2o(hikariDataSource);
//            Sql2o sql2o = new Sql2o(hikariDataSource, new PostgresQuirks());
        Sql2o sql2o = new Sql2o(hikariDataSource, ArrayConverter.arrayConvertingQuirksForPostgres());
        sql2oConnectionMinirepoRestore = sql2o;
    }
    
    public void restorPath(String path, String codiceAzienda) throws MongoWrapperException, UnknownHostException {
        initialize();
        MongoWrapperMinIO mongoWrapper = mongoWrapperMap.get(codiceAzienda);
        List<String> dirFiles = mongoWrapper.getDirFiles(path, true, false);
        if (dirFiles != null) {
            for (String file : dirFiles) {
                mongoWrapper.unDelete(file);
            }
        }
    }
    
    public void restore(StatiRipristino stato) throws MinIOWrapperException, UnknownHostException, MongoException, MongoWrapperException {
       initialize();
       try (Connection minirepoRestoreConn = (Connection) sql2oConnectionMinirepoRestore.open()) {
            List<Map<String, Object>> toRestore = minirepoRestoreConn.createQuery(
                "select \"path\", bucket from public.v_cancellati where bucket not like az || '%' and bucket != 'trash' and stato = :stato order by bucket")
                .addParameter("stato", stato.toString())
                .executeAndFetchTable().asList();
            StatiRipristino statoRipristino = null;
            String minIOPath = null;
            try {
                for (Map<String, Object> rowToRestore : toRestore) {
                    minIOPath = (String) rowToRestore.get("path");
                    String codiceAzienda = (String) rowToRestore.get("bucket");
                    String fileId = minIOPath.replace("/" + codiceAzienda + "/", "");

                    System.out.println("minIOPath: " + minIOPath);
                    System.out.println("fileId: " + fileId);

                    MongoWrapperMinIO mongoWrapper = mongoWrapperMap.get(codiceAzienda);
                    MinIOWrapper minIOWrapper = mongoWrapper.getMinIOWrapper();
                    MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByFileId(fileId, true);
                    if (fileInfo == null) {
                        System.out.println("file mancante");
                        statoRipristino = StatiRipristino.MISSING;
                    } else {
                        if (fileInfo.getDeleted()) {
                            System.out.println("ripristino file");
                            minIOWrapper.restoreByFileId(fileId);
                            statoRipristino = StatiRipristino.RESTORED;
                        } else {
                            statoRipristino = StatiRipristino.SKIPPED;
                            System.out.println("il file non risulta cancellato, niente da fare");
                        }
                    }
                    System.out.println("update stato...");
                    updateStato(minirepoRestoreConn, minIOPath, statoRipristino);
                }
            } catch (Throwable t) {
                statoRipristino = StatiRipristino.ERROR;
                System.out.println("errore");
                t.printStackTrace(System.out);
                System.out.println("update stato...");
                updateStato(minirepoRestoreConn, minIOPath, statoRipristino);
            }
       }
        
    }
    
    private void updateStato(Connection conn, String minIOPath, StatiRipristino stato) {
         String updateQuery
                    = "UPDATE public.cancellati "
                    + "SET stato = :stato "
                    + "WHERE \"path\" = :path";
            conn.createQuery(updateQuery, true)
                .addParameter("stato", stato.toString())
                .addParameter("path", minIOPath)
                .executeUpdate();
//            conn.commit();
            //System.out.println("file end");
    }
    
    @Deprecated
    private void restoreMissing() throws UnknownHostException, MongoException, MongoWrapperException {
        initialize();
        try (Connection minirepoRestoreConn = (Connection) sql2oConnectionMinirepoRestore.open()) {
            List<Map<String, Object>> toRestore = minirepoRestoreConn.createQuery(
                "select \"path\", bucket from public.v_cancellati where bucket not like az || '%' and bucket != 'trash' and stato = :stato order by bucket")
                .addParameter("stato", StatiRipristino.MISSING.toString())
                .executeAndFetchTable().asList();
            StatiRipristino statoRipristino = null;
            String minIOPath = null;
            try {
                for (Map<String, Object> rowToRestore : toRestore) {
                    minIOPath = (String) rowToRestore.get("path");
                    String codiceAzienda = (String) rowToRestore.get("bucket");
                    String fileId = minIOPath.replace("/" + codiceAzienda + "/", "");

                    System.out.println("minIOPath: " + minIOPath);
                    System.out.println("fileId: " + fileId);

                    File f = new File("restore" + minIOPath);
                    System.out.println(f.getAbsolutePath());
                    System.out.println(f.exists());
                    
//                    MongoWrapperMinIO mongoWrapper = mongoWrapperMap.get(codiceAzienda);
//                    MinIOWrapper minIOWrapper = mongoWrapper.getMinIOWrapper();
                }
            } catch (Throwable t) {
                System.out.println("errore");
            }
        }
    }
    
    private void restoreMissingTrash(File restoreDir) throws UnknownHostException, MongoException, MongoWrapperException {
        initialize();
        try (Connection minirepoRestoreConn = (Connection) sql2oConnectionMinirepoRestore.open()) {
            List<Map<String, Object>> toRestore = minirepoRestoreConn.createQuery(
                "select \"path\", bucket from public.v_cancellati where bucket = 'trash' and stato = :stato order by bucket")
                .addParameter("stato", StatiRipristino.NOT_CHECKED.toString())
                .executeAndFetchTable().asList();
            StatiRipristino statoRipristino = null;
            String minIOPath = null;
            MinIOWrapper minIOWrapper = new MinIOWrapper(DBDriver, minIODBUrl, minIODBUsername, minIODBPassword, null);
            try {
                for (Map<String, Object> rowToRestore : toRestore) {
                    minIOPath = (String) rowToRestore.get("path");
                    String codiceAzienda = (String) rowToRestore.get("bucket");
                    String fileId = minIOPath.replace("/" + codiceAzienda + "/", "");

                    System.out.println("fileId: " + fileId);
                    MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByFileId(fileId, true);
                    if (fileInfo == null) {
                        System.out.println("file mancante sul db prod, cerco nel db backup...");
                            List<Map<String, Object>> toRestoreFromBackup = minirepoRestoreConn.createQuery(
                            "select file_id, uuid, bucket, metadata, \"path\", filename, codice_azienda, server_id, mongo_uuid, \"size\", upload_date, modified_date, delete_date, deleted, md5 " +
                            "from repo.files f " +
                            "where file_id = :file_id")
                            .addParameter("file_id", fileId)
                            .executeAndFetchTable().asList();
                            if (toRestoreFromBackup.isEmpty()) {
                                File fileToRestore = new File(restoreDir, StringUtils.trimLeadingCharacter(minIOPath, '/'));
                                if (fileToRestore.exists()) {
                                    System.out.println("riga DB PERSA, ma il file esiste");
                                    statoRipristino = StatiRipristino.UNKNOWN;
                                } else {
                                    statoRipristino = StatiRipristino.LOST;
                                    System.out.println("riga DB PERSA, e file PERSO");
                                }
                            } else {
                                Map<String, Object> foundFile = toRestoreFromBackup.get(0);
                                String fileIdFound = (String) foundFile.get("file_id");
                                String mongoUuidFound = (String) foundFile.get("mongo_uuid");
                                String pathFound = (String) foundFile.get("path");
                                String fileNameFound = (String) foundFile.get("filename");
                                Integer size = (Integer) foundFile.get("size");
                                String md5 = (String) foundFile.get("md5");
                                Integer serverId = (Integer) foundFile.get("server_id");
                                String codiceAziendaFound = (String) foundFile.get("codice_azienda");
                                String uuidFound = (String) foundFile.get("uuid");
                                String bucket = (String) foundFile.get("bucket");
                                Object metadata = foundFile.get("metadata");
                                Boolean deleted = (Boolean) foundFile.get("deleted");
                                Object uploadDate = foundFile.get("upload_date");
                                Object modifiedDate = foundFile.get("modified_date");
                                Object deleteDate = foundFile.get("delete_date");
                                if (deleted) {
                                    System.out.println("file presente nel db backup e deleted");
                                } else {
                                    System.out.println("file presente nel db backup e NON deleted");
                                }
                                System.out.println("cerco il file nella cartella di backup...");
                                File fileToRestore = new File(restoreDir, StringUtils.trimLeadingCharacter(minIOPath, '/'));
                                if (fileToRestore.exists()) {
                                    System.out.println("file trovato nella cartella di backup, vedo se per caso è comunque presente nel repository di prod...");
                                    try (FileInputStream fis = new FileInputStream(fileToRestore)) {
                                        MinioClient minIOClient = minIOWrapper.getMinIOClient(serverId);
                                        Boolean alreadyExist = minIOWrapper.directGetFromMinIO(fileId, serverId, bucket) != null;
                                        if (!alreadyExist) {
                                            System.out.println("il file non è presente, lo carico dal backup");
                                            ObjectWriteResponse res = minIOClient.putObject(PutObjectArgs.builder().bucket(bucket).object(fileIdFound).stream(fis, -1, 10485760).build());
                                        } else {
                                            System.out.println("il file è già presente");
                                        }
                                        System.out.println("ok, inserimento della riga dal backup nel db di prod...");
                                        Sql2o sql2oConnectionMinirepoProd = minIOWrapper.getSql2oConnection();
                                        try (Connection minirepoProdConn = (Connection) sql2oConnectionMinirepoProd.open()) {
                                            minirepoProdConn.createQuery(
                                            "INSERT INTO repo.files" +
                                            "(file_id, uuid, bucket, metadata, \"path\", filename, codice_azienda, server_id, mongo_uuid, \"size\", upload_date, modified_date, delete_date, deleted, md5)" +
                                            "VALUES(:file_id, :uuid, :bucket, :metadata, :path, :filename, :codice_azienda, :server_id, :mongo_uuid, :size, :upload_date, :modified_date, :delete_date, :deleted, :md5)")
                                            .addParameter("file_id", fileIdFound)
                                            .addParameter("uuid", uuidFound)
                                            .addParameter("bucket", bucket)
                                            .addParameter("metadata", metadata)
                                            .addParameter("path", pathFound)
                                            .addParameter("filename", fileNameFound)
                                            .addParameter("codice_azienda", codiceAziendaFound)
                                            .addParameter("server_id", serverId)
                                            .addParameter("mongo_uuid", mongoUuidFound)
                                            .addParameter("size", size)
                                            .addParameter("upload_date", uploadDate)
                                            .addParameter("modified_date", modifiedDate)
                                            .addParameter("delete_date", deleteDate)
                                            .addParameter("deleted", deleted)
                                            .addParameter("md5", md5)
                                            .executeUpdate();
                                            statoRipristino = StatiRipristino.TO_RESTORE;
                                            System.out.println("ok");
                                        } catch (Throwable t) {
                                            statoRipristino = StatiRipristino.ERROR;
                                            System.out.println("errore nell' inserimento della riga dal backup nel db di prod");
                                        }
                                    } catch (Throwable t) {
                                        statoRipristino = StatiRipristino.ERROR;
                                        System.out.println("errore nel ripristino del file");
                                    }
                                } else {
                                    System.out.println("file PERSO");
                                    statoRipristino = StatiRipristino.LOST;
                                }
                            }
//                        }
                    } else {
                        if (fileInfo.getDeleted()) {
                            System.out.println("file presente e deleted");
                            //minIOWrapper.restoreByFileId(fileId);
                            statoRipristino = StatiRipristino.SKIPPED;
                        } else {
                            statoRipristino = StatiRipristino.SKIPPED;
                            System.out.println("file presente e NON deleted");
                        }
                    }
                    updateStato(minirepoRestoreConn, minIOPath, statoRipristino);
                }
            } catch (Throwable t) {
                statoRipristino = StatiRipristino.ERROR;
                System.out.println("errore non meglio identificato");
            }
        }
    }
    
    public static void main(String[] args) throws MinIOWrapperException, UnknownHostException, MongoException, MongoWrapperException, IOException {
        SanatoriaUndelete sanatoriaUndelete = new SanatoriaUndelete();
//        sanatoriaUndelete.restorPath("/Procton/Documenti/2021-13810", "106");
        File dir = new File("restore");
        sanatoriaUndelete.restoreMissingTrash(dir);
//        sanatoriaUndelete.restore(StatiRipristino.NEW);
//        File f = new File("restore/102/1c/3b/57/a3/1c3b57a3-a24b-4a8f-9769-0c9c9c618681/2021-29470_Lettera_firmata.pdf");
//        System.out.println(f.exists());
//            File dir = new File("restore");
//          printFileId(dir);
//        MinIOWrapper minIOWrapperRestore = new MinIOWrapper(DBDriver, minirepoRestoreDBUrl, minirepoRestoreDBUsername, minirepoRestoreDBPassword, null);
//        MinIOWrapperFileInfo fileInfo = minIOWrapperRestore.getFileInfoByFileId("fb/a2/2a/e4/fba22ae4-ae91-4789-a2ca-36741e5c16e4/2021-30581.pdf", true);
//        System.out.println(fileInfo);
    }
    
    public static void printFileId(File dir) throws IOException {
        for (File f: dir.listFiles()) {
            if (f.isDirectory()) {
                printFileId(f);
            } else {
                System.out.println(f.getCanonicalPath().replace(new File("restore").getAbsolutePath(), "").replace("\\", "/"));
//                System.out.println(f.toPath().relativize(new File("restore").toPath()));
            }
        }
    }
}
