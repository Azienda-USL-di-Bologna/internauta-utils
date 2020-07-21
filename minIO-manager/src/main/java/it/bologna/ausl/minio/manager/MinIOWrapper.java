package it.bologna.ausl.minio.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.ErrorCode;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.sql2o.tools.ArrayConverter;
import it.bologna.ausl.sql2o.tools.Sql2oArray;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGobject;
import org.springframework.util.StringUtils;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

/**
 *
 * @author gdm
 */
public class MinIOWrapper {

    public enum FileTableOperations {
        INSERT, UPDATE, DELETE
    }
    
    private final String TRASH_BUCKET = "trash";

    // ci possono essere più minIO server, per cui creo una mappa contenente il serverId(campo della tabella servers) e il client associato
    private static final Map<Integer, MinioClient> minIOServerClientMap = new HashMap<>();
    private static final Map<Integer, Integer> minIOServerAziendaMap = new HashMap<>();
    private static Sql2o sql2oConnection = null;
    private ObjectMapper objectMapper;

    public MinIOWrapper(String minIODBDriver, String minIODBUrl, String minIODBUsername, String minIODBPassword) {
        this(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, new ObjectMapper());
    }

    public MinIOWrapper(String minIODBDriver, String minIODBUrl, String minIODBUsername, String minIODBPassword, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        initialize(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword);
    }

    private synchronized void initialize(String minIODBDriver, String minIODBUrl, String minIODBUsername, String minIODBPassword) {
        initializeDBConnectionPool(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword);
        buildConnectionsMap();
    }

    private void initializeDBConnectionPool(String minIODBDriver, String minIODBUrl, String minIODBUsername, String minIODBPassword) {
        if (sql2oConnection == null) {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName(minIODBDriver);
            hikariConfig.setJdbcUrl(minIODBUrl);
            hikariConfig.setUsername(minIODBUsername);
            hikariConfig.setPassword(minIODBPassword);
            // hikariConfig.setLeakDetectionThreshold(20000);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setMaximumPoolSize(5);
            // hikariConfig.getConnectionTimeout();
            hikariConfig.setConnectionTimeout(60000);
            HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
//            Sql2o sql2o = new Sql2o(hikariDataSource);
//            Sql2o sql2o = new Sql2o(hikariDataSource, new PostgresQuirks());
            Sql2o sql2o = new Sql2o(hikariDataSource, ArrayConverter.arrayConvertingQuirksForPostgres());
            sql2oConnection = sql2o;
        }
    }

    private void buildConnectionsMap() {
        try (Connection conn = (Connection) sql2oConnection.open()) {
            List<Map<String, Object>> res = conn.createQuery(" select su.codice_azienda as codice_azienda, s.id as server_id, s.urls as urls, s.access_key as access_key, s.secret_key as secret_key "
                    + "from repo.servers_upload su join repo.servers s on su.server_id = s.id ")
                    .executeAndFetchTable().asList();
            for (Map<String, Object> row : res) {
                Integer serverId = (Integer) row.get("server_id");
                Integer codiceAzienda = (Integer) row.get("codice_azienda");
                minIOServerAziendaMap.put(codiceAzienda, serverId);
                if (minIOServerClientMap.get(serverId) == null) {
                    String endPointUrl = (String) row.get("urls");
                    String accessKey = (String) row.get("access_key");
                    String secretKey = (String) row.get("secret_key");
                    MinioClient minioClient = MinioClient.builder().endpoint(endPointUrl).credentials(accessKey, secretKey).build();
                    minIOServerClientMap.put(serverId, minioClient);
                }
            }

            System.out.println(res);
        }
    }

    private MinioClient getMinIOClientFromCodiceAzienda(Integer codiceAzienda) {
        Integer serverId = this.minIOServerAziendaMap.get(codiceAzienda);
        return this.minIOServerClientMap.get(serverId);
    }

    private String generatePhysicalPath(String fileName, String uuid) {
        String[] prefixPath = uuid.split("-", 2)[0].split("(?<=\\G..)");
        return StringUtils.arrayToDelimitedString(prefixPath, "/") + "/" + uuid + "/" + fileName;
    }

    /**
     *
     * @param file il file da cariare sul repository
     * @param codiceAzienda es. 105, 902, 908, ecc.
     * @param path il path che il file dovrà avere (path logico, quello fisico sul repository verrà generato random)
     * @param fileName il nome che il file dovrà avere (NB: in caso overwrite=false e path logico e nome file già esistente, questo verrà cambiato aggiungendo un numero alla fine)
     * @param metadata eventuali metadati da inserite, se non si vogliono inserire passare null
     * @param overWrite se esiste già un file con lo stesso path logico e nome file e overwrite=true, questo viene sovrascritto, se overwrite=false, viene cambiato il nome file aggiungendo un numero alla fine e inserito come nuovo file
     * @return un oggetto contenente tutte le informazioni sul file caricato
     * @throws MinIOWrapperException
     */
    public MinIOWrapperFileInfo put(File file, Integer codiceAzienda, String path, String fileName, Map<String, Object> metadata, boolean overWrite) throws MinIOWrapperException, FileNotFoundException, IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            MinIOWrapperFileInfo res = put(fis, codiceAzienda, path, fileName, metadata, overWrite);
            return res;
        }
    }

    /**
     *
     * @param obj lo stream da cariare sul repository
     * @param codiceAzienda es. 105, 902, 908, ecc.
     * @param path il path che il file dovrà avere (path logico, quello fisico sul repository verrà generato random)
     * @param fileName il nome che il file dovrà avere (NB: in caso overwrite=false e path logico e nome file già esistente, questo verrà cambiato aggiungendo un numero alla fine)
     * @param metadata eventuali metadati da inserite, se non si vogliono inserire passare null
     * @param overWrite se esiste già un file con lo stesso path logico e nome file e overwrite=true, questo viene sovrascritto, se overwrite=false, viene cambiato il nome file aggiungendo un numero alla fine e inserito come nuovo file
     * @return un oggetto contenente tutte le informazioni sul file caricato
     * @throws MinIOWrapperException
     */
    public MinIOWrapperFileInfo put(InputStream obj, Integer codiceAzienda, String path, String fileName, Map<String, Object> metadata, boolean overWrite) throws MinIOWrapperException {
        try {
            // wrappo lo stream dentro uno DigestInputStream per poter calcolare md5
            DigestInputStream digestInputStream = new DigestInputStream(obj, MessageDigest.getInstance("MD5"));

            path = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(path), '/');
            Integer serverId = minIOServerAziendaMap.get(codiceAzienda);
            MinioClient minIOClient = minIOServerClientMap.get(serverId);
            String uuid = UUID.randomUUID().toString();
            String physicalPath = generatePhysicalPath(fileName, uuid);
            String bucketName = codiceAzienda.toString();

            boolean bucketExists = minIOClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minIOClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            FileTableOperations fileTableOperation = FileTableOperations.INSERT;
            Integer fileTableId = null;
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime uploadDate = now;
            ZonedDateTime modifiedDate = null;
            try (Connection conn = (Connection) sql2oConnection.beginTransaction()) {
                List<Map<String, Object>> res = conn.createQuery(
                        "select id, file_id, uuid, upload_date, modified_date " +
                        "from repo.files " +
                        "where path = :path and filename = :filename and codice_azienda = :codice_azienda and deleted = false for update")
                        .addParameter("path", path)
                        .addParameter("filename", fileName)
                        .addParameter("codice_azienda", codiceAzienda)
                        .executeAndFetchTable().asList();
                if (!res.isEmpty()) {
                    if (overWrite) {
                        fileTableOperation = FileTableOperations.UPDATE;
                        Map<String, Object> foundFile = res.get(0);
                        physicalPath = (String) foundFile.get("file_id");
                        fileTableId = (Integer) foundFile.get("id");
                        uuid = (String) foundFile.get("uuid");
                        uploadDate = getZonedDateTime(foundFile.get("upload_date"));
                        modifiedDate = now;
                    } else {
                        Integer fileNameIndex = conn.createQuery("select nextval(:filename_seq)").addParameter("filename_seq", "repo.file_names_seq")
                                .executeAndFetchFirst(Integer.class);
                        fileName = StringUtils.stripFilenameExtension(fileName) + "_" + fileNameIndex.toString() + "." + StringUtils.getFilenameExtension(fileName);
                        physicalPath = physicalPath.replace(StringUtils.getFilename(physicalPath), fileName);
                    }
                }
                minIOClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(physicalPath).stream(digestInputStream, -1, 10485760).build());
                
                long size = getSize(physicalPath, serverId, bucketName);
                String md5 = String.format(Locale.ROOT, "%032x", new BigInteger(1, digestInputStream.getMessageDigest().digest()));
                System.out.println("md5: " + md5);
                switch (fileTableOperation) {
                    case INSERT:
                        String insertQuery
                                = "INSERT INTO repo.files "
                                + "(file_id, uuid, bucket, metadata, \"path\", filename, codice_azienda, server_id, \"size\", upload_date, deleted, md5) "
                                + "VALUES(:file_id, :uuid, :bucket, :metadata::jsonb, :path, :filename, :codice_azienda, :server_id, :size, :upload_date, false, :md5)";
                        fileTableId = conn.createQuery(insertQuery, true)
                                .addParameter("file_id", physicalPath)
                                .addParameter("uuid", uuid)
                                .addParameter("bucket", bucketName)
                                .addParameter("metadata", objectMapper.writeValueAsString(metadata))
                                .addParameter("path", path)
                                .addParameter("filename", fileName)
                                .addParameter("codice_azienda", codiceAzienda)
                                .addParameter("server_id", minIOServerAziendaMap.get(codiceAzienda))
                                .addParameter("upload_date", Timestamp.valueOf(uploadDate.toLocalDateTime()))
                                .addParameter("size", size)
                                .addParameter("md5", md5)
                                .executeUpdate().getKey(Integer.class);
                        break;

                    case UPDATE:
                        String updateQuery
                                = "UPDATE repo.files "
                                + "SET filename = :filename, metadata=:metadata, server_id=:server_id, size=:size, modified_date=:modified_date , md5=:md5 "
                                + "where id = :id and deleted=false";
                        conn.createQuery(updateQuery, true)
                                .addParameter("filename", fileName)
                                .addParameter("metadata", objectMapper.writeValueAsString(metadata))
                                .addParameter("server_id", minIOServerAziendaMap.get(codiceAzienda))
                                .addParameter("size", size)
                                .addParameter("modified_date", Timestamp.valueOf(modifiedDate.toLocalDateTime()))
                                .addParameter("md5", md5)
                                .addParameter("id", fileTableId)
                                .executeUpdate();
                        break;
                }
                
                conn.commit();
                MinIOWrapperFileInfo uploadRes = new MinIOWrapperFileInfo(
                        fileTableId,
                        physicalPath,
                        path,
                        fileName,
                        Math.toIntExact(size),
                        md5,
                        serverId,
                        uuid,
                        codiceAzienda,
                        bucketName,
                        metadata,
                        false,
                        uploadDate,
                        modifiedDate,
                        null
                );
                return uploadRes;
            }
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nell'upload del file", ex);
        }
    }

    public InputStream getByFileId(String fileId) throws MinIOWrapperException {
        return getByFileId(fileId, false);
    }
    
    public InputStream getByFileId(String fileId, boolean includeDeleted) throws MinIOWrapperException {
        MinIOWrapperFileInfo fileInfo = getFileInfoByFileId(fileId, includeDeleted);
        if (fileInfo != null) {
            return directGetFromMinIO(fileInfo.getFileId(), fileInfo.getServerId(), fileInfo.getBucketName());
        } else {
            return null;
        }
    }
    
    public InputStream getByPathAndFileName(String path, String fileName, Integer codiceAzienza) throws MinIOWrapperException {
        return getByPathAndFileName(path, fileName, codiceAzienza, false);
    }

    public InputStream getByPathAndFileName(String path, String fileName, Integer codiceAzienza, boolean includeDeleted) throws MinIOWrapperException {
        MinIOWrapperFileInfo fileInfo = getFileInfoByPathAndFileName(path, fileName, codiceAzienza, includeDeleted);
        if (fileInfo != null) {
            return directGetFromMinIO(fileInfo.getFileId(), fileInfo.getServerId(), fileInfo.getBucketName());
        } else {
            return null;
        }
    }
    
    public MinIOWrapperFileInfo getFileInfoByFileId(String fileId) throws MinIOWrapperException {
        return getFileInfoByFileId(fileId, false);
    }

    public MinIOWrapperFileInfo getFileInfoByFileId(String fileId, boolean includeDeleted) throws MinIOWrapperException {
        try (Connection conn = (Connection) sql2oConnection.open()) {
            List<Map<String, Object>> res = conn.createQuery(
                    "select id, path, filename, size, md5, server_id, codice_azienda, uuid, bucket, metadata, deleted, upload_date, modified_date, delete_date " +
                    "from repo.files where file_id = :file_id" + (!includeDeleted? " and deleted = false": ""))
                    .addParameter("file_id", fileId)
                    .executeAndFetchTable().asList();
            if (!res.isEmpty()) {
                Map<String, Object> foundFile = res.get(0);
                Integer fileTableId = (Integer) foundFile.get("id");
                String path = (String) foundFile.get("path");
                String fileName = (String) foundFile.get("filename");
                Integer size = (Integer) foundFile.get("size");
                String md5 = (String) foundFile.get("md5");
                Integer serverId = (Integer) foundFile.get("server_id");
                Integer codiceAzienda = (Integer) foundFile.get("codice_azienda");
                String uuid = (String) foundFile.get("uuid");
                String bucket = (String) foundFile.get("bucket");
                Map<String, Object> metadata = getJsonField(foundFile.get("metadata"), new TypeReference<Map<String, Object>>() {});
                Boolean deleted = (Boolean) foundFile.get("deleted");
                ZonedDateTime uploadDate = getZonedDateTime(foundFile.get("upload_date"));
                ZonedDateTime modifiedDate = getZonedDateTime(foundFile.get("modified_date"));
                ZonedDateTime deleteDate = getZonedDateTime(foundFile.get("delete_date"));
                MinIOWrapperFileInfo fileInfo = new MinIOWrapperFileInfo(
                        fileTableId,
                        fileId,
                        path,
                        fileName,
                        size,
                        md5,
                        serverId,
                        uuid,
                        codiceAzienda,
                        bucket,
                        metadata,
                        deleted,
                        uploadDate,
                        modifiedDate,
                        deleteDate
                );
                return fileInfo;
            }
        }
        return null;
    }

    public MinIOWrapperFileInfo getFileInfoByPathAndFileName(String path, String fileName, Integer codiceAzienda) throws MinIOWrapperException {
        return getFileInfoByPathAndFileName(path, fileName, codiceAzienda, false);
    }
    
    public MinIOWrapperFileInfo getFileInfoByPathAndFileName(String path, String fileName, Integer codiceAzienda, boolean includeDeleted) throws MinIOWrapperException {
        try (Connection conn = (Connection) sql2oConnection.open()) {
            path = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(path), '/');
            List<Map<String, Object>> res = conn.createQuery(
                    "select id, file_id, size, md5, metadata, server_id, uuid, bucket, deleted, upload_date, modified_date, delete_date " +
                    "from repo.files " +
                    "where path = :path and filename = :filename and codice_azienda = :codice_azienda" + (!includeDeleted? " and deleted = false": ""))
                    .addParameter("path", path)
                    .addParameter("filename", fileName)
                    .addParameter("codice_azienda", codiceAzienda)
                    .executeAndFetchTable().asList();
            if (!res.isEmpty()) {
                Map<String, Object> foundFile = res.get(0);
                Integer fileTableId = (Integer) foundFile.get("id");
                String fileId = (String) foundFile.get("file_id");
                Integer size = (Integer) foundFile.get("size");
                String md5 = (String) foundFile.get("md5");
                Map<String, Object> metadata = getJsonField(foundFile.get("metadata"), new TypeReference<Map<String, Object>>() {});
                Integer serverId = (Integer) foundFile.get("server_id");
                String uuid = (String) foundFile.get("uuid");
                String bucket = (String) foundFile.get("bucket");
                Boolean deleted = (Boolean) foundFile.get("deleted");
                ZonedDateTime uploadDate = getZonedDateTime(foundFile.get("upload_date"));
                ZonedDateTime modifiedDate = getZonedDateTime(foundFile.get("modified_date"));
                ZonedDateTime deleteDate = getZonedDateTime(foundFile.get("delete_date"));

                MinIOWrapperFileInfo fileInfo = new MinIOWrapperFileInfo(
                        fileTableId,
                        fileId,
                        path,
                        fileName,
                        size,
                        md5,
                        serverId,
                        uuid,
                        codiceAzienda,
                        bucket,
                        metadata,
                        deleted,
                        uploadDate,
                        modifiedDate,
                        deleteDate
                );
                return fileInfo;
            }
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nel reperimento dell informazioni sul file", ex);
        }
        return null;
    }

    public void renameByFileId(String fileId, String newFileName) throws MinIOWrapperException {
        renameByFileId(fileId, newFileName, false);
    }
    
    public void renameByFileId(String fileId, String newFileName, boolean includeDeleted) throws MinIOWrapperException {
        try (Connection conn = (Connection) sql2oConnection.beginTransaction()) {
            List<Map<String, Object>> res = conn.createQuery(
                    "select 1 " +
                    "from repo.files " +
                    "where \"path\" = (select \"path\" from repo.files where file_id = :file_id) " +
                    "and filename = :new_filename" + (!includeDeleted? " and deleted = false": "") + " for update")
                    .addParameter("file_id", fileId)
                    .addParameter("new_filename", newFileName)
                    .executeAndFetchTable().asList();
            if (res.isEmpty()) {
                conn.createQuery(
                        "update repo.files set filename = :new_filename, modified_date = now() " +
                        "where file_id = :file_id" + (!includeDeleted? " and deleted = false": ""))
                        .addParameter("new_filename", newFileName)
                        .addParameter("file_id", fileId)
                        .executeUpdate();
                conn.commit();
            } else {
                throw new MinIOWrapperException("impossibile rinominare: esiste già un file con lo stesso nome nello stesso path");
            }
        }
    }

    public void renameByFileId(String fileId, String newPath, String newFileName) throws MinIOWrapperException {
        renameByFileId(fileId, newPath, newFileName, false);
    }
    
    public void renameByFileId(String fileId, String newPath, String newFileName, boolean includeDeleted) throws MinIOWrapperException {
        newPath = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(newPath), '/');
        try (Connection conn = (Connection) sql2oConnection.beginTransaction()) {
            List<Map<String, Object>> res = conn.createQuery(
                    "select 1 " +
                    "from repo.files " +
                    "where \"path\" = :newPath and filename = :new_filename" + (!includeDeleted? " and deleted = false": "") + " for update")
                    .addParameter("newPath", newPath)
                    .addParameter("new_filename", newFileName)
                    .executeAndFetchTable().asList();
            if (res.isEmpty()) {
                conn.createQuery(
                        "update repo.files set path = :new_path, filename = :new_filename, modified_date = now() " +
                        "where file_id = :file_id" + (!includeDeleted? " and deleted = false": ""))
                        .addParameter("new_path", newPath)
                        .addParameter("new_filename", newFileName)
                        .addParameter("file_id", fileId)
                        .executeUpdate();
                conn.commit();
            } else {
                throw new MinIOWrapperException("impossibile rinominare: esiste già un file con lo stesso nome nello stesso path");
            }
        }
    }

    public void renameByPathAndFileName(String path, String fileName, String newPath, String newFileName) throws MinIOWrapperException {
        renameByPathAndFileName(path, fileName, newPath, newFileName, false);
    }
    
    public void renameByPathAndFileName(String path, String fileName, String newPath, String newFileName, boolean includeDeleted) throws MinIOWrapperException {
        path = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(path), '/');
        newPath = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(newPath), '/');
        try (Connection conn = (Connection) sql2oConnection.beginTransaction()) {
            List<Map<String, Object>> res = conn.createQuery(
                    "select 1 " +
                    "from repo.files " +
                    "where \"path\" = :newPath and filename = :new_filename" + (!includeDeleted? " and deleted = false": "") + " for update")
                    .addParameter("newPath", newPath)
                    .addParameter("new_filename", newFileName)
                    .executeAndFetchTable().asList();
            if (res.isEmpty()) {
                conn.createQuery(
                        "update repo.files set path = :new_path, filename = :new_filename, modified_date = now() " +
                        "where path = :path and filename = :filename" + (!includeDeleted? " and deleted = false": ""))
                        .addParameter("new_path", newPath)
                        .addParameter("new_filename", newFileName)
                        .addParameter("path", path)
                        .addParameter("filename", fileName)
                        .executeUpdate();
                conn.commit();
            } else {
                throw new MinIOWrapperException("impossibile rinominare: esiste già un file con lo stesso nome nello stesso path");
            }
        }
    }

    public void renameByPathAndFileName(String path, String fileName, String newFileName) throws MinIOWrapperException {
        renameByPathAndFileName(path, fileName, newFileName, false);
    }
    
    public void renameByPathAndFileName(String path, String fileName, String newFileName, boolean includeDeleted) throws MinIOWrapperException {
        path = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(path), '/');
        try (Connection conn = (Connection) sql2oConnection.beginTransaction()) {
            List<Map<String, Object>> res = conn.createQuery(
                    "select 1 " +
                    "from repo.files " +
                    "where \"path\" = (select \"path\" from repo.files where path = :path and filename = :filename) " +
                    "and filename = :new_filename" + (!includeDeleted? " and deleted = false": "") + " for update")
                    .addParameter("path", path)
                    .addParameter("filename", fileName)
                    .addParameter("new_filename", newFileName)
                    .executeAndFetchTable().asList();
            if (res.isEmpty()) {
                conn.createQuery(
                    "update repo.files set filename = :new_filename, modified_date = now() " +
                    "where path = :path and filename = :filename" + (!includeDeleted? " and deleted = false": ""))
                    .addParameter("new_filename", newFileName)
                    .addParameter("path", path)
                    .addParameter("filename", fileName)
                    .executeUpdate();
                conn.commit();
            } else {
                throw new MinIOWrapperException("impossibile rinominare: esiste già un file con lo stesso nome nello stesso path");
            }
        }
    }
    
    public void deleteByFileId(String fileId) throws MinIOWrapperException {
        try (Connection conn = (Connection) sql2oConnection.open()) {
            PgArray keyPgArray = (PgArray) conn.createQuery(
                    "update repo.files " +
                    "set deleted = true, delete_date = now(), bucket = :bucket " +
                    "where file_id = :file_id and deleted = false " +
                    "returning (select array[server_id::text, bucket] from repo.files where file_id = :file_id and deleted = false)", true)
                    .addParameter("file_id", fileId)
                    .addParameter("bucket", TRASH_BUCKET)
                    .executeUpdate().getKey();
            // ((org.postgresql.jdbc.PgArray)executeUpdate.getKey()).getArray()[2]
            if (keyPgArray != null) {
                Object[] array = (Object[]) keyPgArray.getArray();
                Integer serverId = Integer.parseInt((String) array[0]);                //String fileName = (String) array[1];
                String bucket = (String) array[1];
                moveToTrash(fileId, bucket, serverId);
            }
            
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nella cancellazione", ex);
        }
    }
    
    public void deleteByPathAndFileName(String path, String fileName, Integer codiceAzienda) throws MinIOWrapperException {
        path = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(path), '/');
        try (Connection conn = (Connection) sql2oConnection.open()) {
           PgArray keyPgArray = (PgArray) conn.createQuery(
                    "update repo.files " +
                    "set deleted = true,  delete_date = now(), bucket = :bucket " +
                    "where path = :path and filename = :filename and codice_azienda = :codice_azienda and deleted = false " +
                    "returning (select array[server_id::text, bucket, file_id] from repo.files where path = :path and filename = :filename and codice_azienda = :codice_azienda and deleted = false)", true)
                   
                    .addParameter("path", path)
                    .addParameter("filename", fileName)
                    .addParameter("codice_azienda", codiceAzienda)
                    .addParameter("bucket", TRASH_BUCKET)
                    .executeUpdate().getKey();
            if (keyPgArray != null) {
                Object[] array = (Object[]) keyPgArray.getArray();
                Integer serverId = Integer.parseInt((String) array[0]);
                String bucket = (String) array[1];
                String fileId = (String) array[2];
                moveToTrash(fileId, bucket, serverId);
            }
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nella cancellazione", ex);
        }
    }
    
    public void restoreByFileId(String fileId) throws MinIOWrapperException {
        try (Connection conn = (Connection) sql2oConnection.beginTransaction()) {
            PgArray keyPgArray = (PgArray) conn.createQuery(
                    "update repo.files set deleted = false, delete_date = null, bucket = codice_azienda " +
                    "where file_id = :file_id and deleted = true returning array[server_id::text, bucket]", true)
                    .addParameter("file_id", fileId)
                    .executeUpdate().getKey();
            // ((org.postgresql.jdbc.PgArray)executeUpdate.getKey()).getArray()[2]
            if (keyPgArray != null) {
                Object[] array = (Object[]) keyPgArray.getArray();
                Integer serverId = Integer.parseInt((String) array[0]);
                //String fileName = (String) array[1];
                String bucket = (String) array[1];
                restoreFromTrash(fileId, bucket, serverId);
                conn.commit();
            }
            
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nella cancellazione", ex);
        }
    }
    
    private void moveToTrash(String fileId, String srcBucket, Integer serverId) throws MinIOWrapperException {
        try {
            MinioClient minIOClient = minIOServerClientMap.get(serverId);
            boolean trashBucketExists = minIOClient.bucketExists(BucketExistsArgs.builder().bucket(TRASH_BUCKET).build());
            if (!trashBucketExists) {
                minIOClient.makeBucket(MakeBucketArgs.builder().bucket(TRASH_BUCKET).build());
            }
            minIOClient.copyObject(CopyObjectArgs.builder().bucket(TRASH_BUCKET).object(fileId)
                    .source(CopySource.builder().bucket(srcBucket).object(fileId).build()).build());
            minIOClient.removeObject(RemoveObjectArgs.builder().bucket(srcBucket).object(fileId).build());
        } catch (Exception ex) {
             throw new MinIOWrapperException("errore nello spostamento del file all'interno del bucket " + TRASH_BUCKET, ex);
        }
    }
    
    private void restoreFromTrash(String fileId, String srcBucket, Integer serverId) throws MinIOWrapperException {
        try {
            MinioClient minIOClient = minIOServerClientMap.get(serverId);
            boolean srcBucketExists = minIOClient.bucketExists(BucketExistsArgs.builder().bucket(srcBucket).build());
            if (!srcBucketExists) {
                minIOClient.makeBucket(MakeBucketArgs.builder().bucket(srcBucket).build());
            }
            minIOClient.copyObject(CopyObjectArgs.builder().bucket(srcBucket).object(fileId)
                    .source(CopySource.builder().bucket(TRASH_BUCKET).object(fileId).build()).build());
            minIOClient.removeObject(RemoveObjectArgs.builder().bucket(TRASH_BUCKET).object(fileId).build());
        } catch (Exception ex) {
             throw new MinIOWrapperException("errore nello ripristino del file dal bucket " + TRASH_BUCKET, ex);
        }
    }
    
    public void removeByFileId(String fileId, boolean onlyDeleted) throws MinIOWrapperException {
        try (Connection conn = (Connection) sql2oConnection.open()) {
            String query =
                    "delete " +
                    "from repo.files " +
                    "where file_id = :file_id" + (onlyDeleted? " and deleted = true ": " ") +
                    "returning array[server_id::text, bucket]";
            PgArray keyPgArray = (PgArray) conn.createQuery(query, true)
                    .addParameter("file_id", fileId)
                    .executeUpdate().getKey();
            if (keyPgArray != null) {
                Object[] array = (Object[]) keyPgArray.getArray();
                Integer serverId = Integer.parseInt((String) array[0]);
                String bucket = (String) array[1];
                remove(fileId, bucket, serverId);
            }
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nel reperimento dei files", ex);
        }
    }
    
    public void remove(String fileId, String bucket, Integer serverId) throws MinIOWrapperException {
        try {
            MinioClient minIOClient = minIOServerClientMap.get(serverId);
            minIOClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(fileId).build());
        } catch (Exception ex) {
             throw new MinIOWrapperException("errore nella cancellazione del file dal bucket " + bucket, ex);
        }
    }
    
    public List<MinIOWrapperFileInfo> getFilesInPath(String path) throws MinIOWrapperException {
        return getFilesInPath(path, false);
    }
    
    public List<MinIOWrapperFileInfo> getFilesInPath(String path, boolean includeDeleted) throws MinIOWrapperException {
        path = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(path), '/');
        try (Connection conn = (Connection) sql2oConnection.open()) {
            Sql2oArray pathsArray = new Sql2oArray(StringUtils.delimitedListToStringArray(StringUtils.trimLeadingCharacter(path, '/'), "/"));

            String query = 
                        "select id, file_id, uuid, bucket, metadata, path, filename, codice_azienda, server_id, size, md5, deleted, upload_date, modified_date, delete_date " +
                        "from repo.files " +
                        "where :path_array::text[] <@ paths_array and path like :path || '%'" + (!includeDeleted? " and deleted = false": "");
            List<Map<String, Object>> queryRes = conn.createQuery(query)
                    .addParameter("path_array", pathsArray)
                    .addParameter("path", path)
                    .executeAndFetchTable().asList();
            List<MinIOWrapperFileInfo> res = null;
             if (!queryRes.isEmpty()) {
                res = new ArrayList<>();
                for (Map<String, Object> foundFile : queryRes) {
                    Integer fileTableId = (Integer) foundFile.get("id");
                    String fileId = (String) foundFile.get("file_id");
                    String uuid = (String) foundFile.get("uuid");
                    String bucket = (String) foundFile.get("bucket");
                    Map<String, Object> metadata = getJsonField(foundFile.get("metadata"), new TypeReference<Map<String, Object>>() {});
                    String filePath = (String) foundFile.get("path");
                    String fileName = (String) foundFile.get("filename");
                    Integer codiceAzienda = (Integer) foundFile.get("codice_azienda");
                    Integer serverId = (Integer) foundFile.get("server_id");
                    Integer size = (Integer) foundFile.get("size");
                    String md5 = (String) foundFile.get("md5");
                    Boolean deleted = (Boolean) foundFile.get("deleted");
                    ZonedDateTime uploadDate = getZonedDateTime(foundFile.get("upload_date"));
                    ZonedDateTime modifiedDate = getZonedDateTime(foundFile.get("modified_date"));
                    ZonedDateTime deleteDate = getZonedDateTime(foundFile.get("delete_date"));

                    MinIOWrapperFileInfo fileInfo = new MinIOWrapperFileInfo(
                            fileTableId,
                            fileId,
                            filePath,
                            fileName,
                            size,
                            md5,
                            serverId,
                            uuid,
                            codiceAzienda,
                            bucket,
                            metadata,
                            deleted,
                            uploadDate,
                            modifiedDate,
                            deleteDate
                    );
                    res.add(fileInfo);
                }
            }
            return res;
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nel reperimento dei files", ex);
        }
    }

//    private Map<String, Object> getMetadata(Object metadata) throws MinIOWrapperException {
//        try {
//            return objectMapper.readValue(((PGobject) metadata).getValue(), new TypeReference<Map<String, Object>>() {
//            });
//        } catch (Exception ex) {
//            throw new MinIOWrapperException("errore nel reperimento dei metadati", ex);
//        }
//    }
    
    private <T> T getJsonField(Object field, TypeReference<T> type) throws MinIOWrapperException {
        try {
            return objectMapper.readValue(((PGobject) field).getValue(), type);
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nel reperimento dei metadati", ex);
        }
    }
    
    private ZonedDateTime getZonedDateTime(Object timestamptzField) throws MinIOWrapperException {
        try {
            ZonedDateTime date = timestamptzField != null ? ZonedDateTime.ofInstant(((Timestamp)timestamptzField).toInstant(), ZoneId.systemDefault()): null;
            return date;
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nel reperimento del campo data", ex);
        }
    }

    public InputStream directGetFromMinIO(String fileId, Integer serverId, String bucket) throws MinIOWrapperException {
        MinioClient minIOClient = minIOServerClientMap.get(serverId);
        try {
            return minIOClient.getObject(GetObjectArgs.builder().bucket(bucket).object(fileId).build());
        } catch (ErrorResponseException ex) {
            if (ex.errorResponse().errorCode() == ErrorCode.NO_SUCH_OBJECT) {
                return null;
            } else {
                throw new MinIOWrapperException("errore nel reperimento del file", ex);
            }
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nel reperimento del file", ex);
        }
    }

    private long getSize(String fileId, Integer serverId, String bucket) throws MinIOWrapperException {
        MinioClient minIOClient = minIOServerClientMap.get(serverId);
        try {
            return minIOClient.statObject(io.minio.StatObjectArgs.builder().bucket(bucket).object(fileId).build()).length();
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nel reperimento della dimesione del file", ex);
        }
    }

    public void test() {
        MinioClient minIOClient = minIOServerClientMap.get(1);
        try {
            ObjectStat statObject = minIOClient.statObject(io.minio.StatObjectArgs.builder().bucket("105").object("f0/a7/a5/78/f0a7a578-707d-4334-9935-d1ce3bd96896/aaa.pdf").build());
            System.out.println(statObject);
        } catch (Exception ex) {
            Logger.getLogger(MinIOWrapper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
