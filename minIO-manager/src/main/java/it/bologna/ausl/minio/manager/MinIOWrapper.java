package it.bologna.ausl.minio.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
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
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

/**
 *
 * @author gdm
 *
 * Tramite questa classes può interagire con il repository minIO. Oltre che
 * all'upload, download e gestione dei files, questa classe si occupa di
 * scriverne le informazioni in una tabella apposita. Inoltre dà la possibilità
 * di avere più server minIO e gestire dove caricare i files in base
 * all'azienda. (Queste informazioni sono da configurare nella tabelle
 * repo.servers e servers_upload)
 */
public class MinIOWrapper {

    private static final Logger logger = LoggerFactory.getLogger(MinIOWrapper.class);

    public enum FileTableOperations {
        INSERT, UPDATE, DELETE
    }

    // nome del bucket cestino
    private String trashBucket = "trash";

    private static final Pattern patternUuid = Pattern.compile("\\b([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})\\b");

    // ci possono essere più minIO server, per cui creo una mappa contenente il serverId(campo della tabella servers) e il client associato
    private static final Map<Integer, MinioClient> minIOServerClientMap = new HashMap<>();

    // mappo anche il serverId di ogni azienda (serve solo per l'upload, per il resto prendo il serverId presente in tabella repo.files)
    private static final Map<String, Integer> minIOServerAziendaMap = new HashMap<>();
    
    // Mappa dei parametri di configurazione per il repository S3
    private static final Map<String, String> repositoryConfigurations = new HashMap<>();

    // connection al Db con Connection Pool
    private static Sql2o sql2oConnection = null;

    private final ObjectMapper objectMapper;
    
    public Map<String, String> getRepositoryConfigurations() {
        return repositoryConfigurations;
    }

    /**
     * Costruisce l'oggetto MinIOWrapper per l'interazione con il repository
     *
     * @param minIODBDriver il driver per la connessione jdbc al DB (es.
     * org.postgresql.Driver)
     * @param minIODBUrl url del DB (es.
     * jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified)
     * @param minIODBUsername username per la connessione al DB
     * @param minIODBPassword password per la connessione al DB
     * @param maxPoolSize numero massimo di connessioni al DB
     */
    public MinIOWrapper(String minIODBDriver, String minIODBUrl, String minIODBUsername, String minIODBPassword, Integer maxPoolSize) {
        this(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, maxPoolSize, new ObjectMapper());
    }

    /**
     * Costruisce l'oggetto MinIOWrapper per l'interazione con il repository
     *
     * @param minIODBDriver il driver per la connessione jdbc al DB (es.
     * org.postgresql.Driver)
     * @param minIODBUrl url del DB (es.
     * jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified)
     * @param minIODBUsername username per la connessione al DB
     * @param minIODBPassword password per la connessione al DB
     * @param maxPoolSize numero massimo di connessioni al DB
     * @param objectMapper passare se si desidera usare il proprio objectMapper
     * (es in internauta)
     */
    public MinIOWrapper(String minIODBDriver, String minIODBUrl, String minIODBUsername, String minIODBPassword, Integer maxPoolSize, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        initialize(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, maxPoolSize);
    }

    /**
     * Inizializza in modo thread-safe il connectionPool e i server di minIO
     * popolando anche le due mappe (minIOServerClientMap e
     * minIOServerAziendaMap)
     *
     * @param minIODBDriver
     * @param minIODBUrl
     * @param minIODBUsername
     * @param minIODBPassword
     */
    private synchronized void initialize(String minIODBDriver, String minIODBUrl, String minIODBUsername, String minIODBPassword, Integer maxPoolSize) {
        initializeDBConnectionPool(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, maxPoolSize);
        buildConnectionsMap();
        loadConfigurations();
        trashBucket = getBucketName(trashBucket);
    }

    private void initializeDBConnectionPool(String minIODBDriver, String minIODBUrl, String minIODBUsername, String minIODBPassword, Integer maxPoolSize) {
        if (sql2oConnection == null) {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName(minIODBDriver);
            hikariConfig.setJdbcUrl(minIODBUrl);
            hikariConfig.setUsername(minIODBUsername);
            hikariConfig.setPassword(minIODBPassword);
            // hikariConfig.setLeakDetectionThreshold(20000);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setMaximumPoolSize(maxPoolSize);
            // hikariConfig.getConnectionTimeout();
            hikariConfig.setConnectionTimeout(60000);
            HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
//            Sql2o sql2o = new Sql2o(hikariDataSource);
//            Sql2o sql2o = new Sql2o(hikariDataSource, new PostgresQuirks());
            Sql2o sql2o = new Sql2o(hikariDataSource, ArrayConverter.arrayConvertingQuirksForPostgres());
            sql2oConnection = new Sql2o(hikariDataSource, ArrayConverter.arrayConvertingQuirksForPostgres());
        }
    }

    /**
     * costrisce le due mappe (minIOServerClientMap e minIOServerAziendaMap)
     * tramite la lettura dei dati dalle tabelle repo.servers e servers_upload.
     */
    private void buildConnectionsMap() {
        try ( Connection conn = (Connection) sql2oConnection.open()) {
            List<Map<String, Object>> res = conn.createQuery(
                    " select su.codice_azienda as codice_azienda, s.id as server_id, s.urls as urls, s.access_key as access_key, s.secret_key as secret_key, s.retry_on_connection_failure " +
                    "from repo.servers_upload su join repo.servers s on su.server_id = s.id ")
                    .executeAndFetchTable().asList();
            for (Map<String, Object> row : res) {
                Integer serverId = (Integer) row.get("server_id");
                String codiceAzienda = (String) row.get("codice_azienda");
                minIOServerAziendaMap.put(codiceAzienda, serverId);
                if (minIOServerClientMap.get(serverId) == null) {
                    String endPointUrl = (String) row.get("urls");
//                    endPointUrl = "http://babelminioprod02.avec.emr.it:9000";
//                    endPointUrl = "http://babelminiotest01-auslbo.avec.emr.it:9001";
//                    endPointUrl = "https://s3.eu-west-1.amazonaws.com";
                    String accessKey = (String) row.get("access_key");
                    String secretKey = (String) row.get("secret_key");
                    Boolean retryOnConnectionFailure = (Boolean) row.get("retry_on_connection_failure");
                    
//                    ConnectionPool p = new ConnectionPool(10, 60, TimeUnit.SECONDS);
                    OkHttpClient customHttpClient = new OkHttpClient().newBuilder()
                            .connectTimeout(1, TimeUnit.HOURS)
                            .readTimeout(1, TimeUnit.HOURS)
                            .writeTimeout(1, TimeUnit.HOURS)
                            .callTimeout(1, TimeUnit.HOURS)
                            .protocols(Arrays.asList(Protocol.HTTP_1_1))
                            .retryOnConnectionFailure(retryOnConnectionFailure)
//                            .connectionPool(p)
                    .build();
//                    httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, Integer.MAX_VALUE);
                    MinioClient minioClient = MinioClient.builder()
                            .httpClient(customHttpClient)
                            .endpoint(endPointUrl)
                            .credentials(accessKey, secretKey)
                            .build();
//                    minioClient.setTimeout(0, 0, 0);
//                    minioClient.setTimeout(TimeUnit.HOURS.toMillis(1), TimeUnit.HOURS.toMillis(1), TimeUnit.HOURS.toMillis(1));
                    minIOServerClientMap.put(serverId, minioClient);
                }
            }
        }
    }
    
    /**
     * Carica i parametri di configurazione del repository S3 e popola la mappa statica.
     */
    private void loadConfigurations() {
        try (Connection conn = (Connection) sql2oConnection.open()) {
            List<Map<String, Object>> res = conn.createQuery("select c.key, c.value from repo.configurations c")
                    .executeAndFetchTable().asList();
            for (Map<String, Object> row : res) {
                try {
                    repositoryConfigurations.put((String) row.get("key"), getJsonField(row.get("value"), String.class));
                } catch (MinIOWrapperException ex) {
                    logger.error("Error reading configurations.");
                }
            }
        }
    }
    
    /**
     * Il metodo restituisce il nome del bucket aggiungendo un prefisso se presente nella configurazione
     * al parametro passato altrimenti restituisce il parametro stesso.
     * @param bucketName Il nome del bucket al quale verrà aggiunto il prefisso.
     * @return il nome del bucket.
     */
    public String getBucketName(String bucketName) {
        // Controlla se il bucket ha già il prefisso.
        String prefixBucket = repositoryConfigurations.get("prefix-bucket");
        if (prefixBucket != null && (!bucketName.startsWith(prefixBucket) || bucketName.equals(prefixBucket))) {
            return prefixBucket + bucketName;
        } else {
            return bucketName;
        }
    }

    public MinioClient getMinIOClientFromCodiceAzienda(String codiceAzienda) {
        Integer serverId = minIOServerAziendaMap.get(codiceAzienda);
        return minIOServerClientMap.get(serverId);
    }

    /**
     * genera il path fisico (univico) in cui il file sarà caricato sul
     * repository. Questo non è il path logico che viene passato come parametro
     * in fase di upload, ma il path su cio fisicamente il file viene caricato.
     * Questo path è generato in modo da bilanciare i file nelle varie
     * directory, per evitare di avere directory piene di files. Il path si
     * genera a partire da un UUID Esempio di path: a partire dall'uuid
     * ca974e73-ddfc-4974-854f-09aaf85446c0 e fileName file.pdf ->
     * /ca/97/4e/73/ca974e73-ddfc-4974-854f-09aaf85446c0/file.pdf
     *
     * @param fileName nome che dovrà avere il file sul repository (sarebbe la
     * parte finale del path)
     * @param uuid
     * @return il path sul quale andrà fatto l'upload del file.
     */
    private String generatePhysicalPath(String fileName, String uuid) {
        String[] prefixPath = uuid.split("-", 2)[0].split("(?<=\\G..)");
        return StringUtils.arrayToDelimitedString(prefixPath, "/") + "/" + uuid + "/" + fileName;
    }

    /**
     * Carica un file sul repository
     *
     * @param file il file da cariare sul repository
     * @param codiceAzienda es. 105, 902, 908, ecc. Corrisponde anche al nome
     * del bucket, se non diversamente specificato nell'apposito parametro
     * @param path il path che il file dovrà avere (path logico, quello fisico
     * sul repository verrà generato random)
     * @param fileName il nome che il file dovrà avere (NB: in caso
     * overwrite=false e path logico e nome file già esistente, questo verrà
     * cambiato aggiungendo un numero alla fine)
     * @param metadata eventuali metadati da inserite, se non si vogliono
     * inserire passare null
     * @param overWrite se esiste già un file con lo stesso path logico e nome
     * file e overwrite=true, questo viene sovrascritto, se overwrite=false,
     * viene cambiato il nome file aggiungendo un numero alla fine e inserito
     * come nuovo file
     * @param bucket il bucket su cui scrivere il file. Se non passato sarà
     * usato il bucket che si chiama come il codiceAzienda
     * @return un oggetto contenente tutte le informazioni sul file caricato
     * @throws MinIOWrapperException
     */
    public MinIOWrapperFileInfo putWithBucket(File file, String codiceAzienda, String path, String fileName, Map<String, Object> metadata, boolean overWrite, String bucket) throws MinIOWrapperException, FileNotFoundException, IOException {
        try ( FileInputStream fis = new FileInputStream(file)) {
            MinIOWrapperFileInfo res = putWithBucket(fis, codiceAzienda, path, fileName, metadata, overWrite, null, bucket);
            return res;
        }
    }

    /**
     * Carica un file sul repository
     *
     * @param file il file da cariare sul repository
     * @param codiceAzienda es. 105, 902, 908, ecc. Corrisponde anche al nome
     * del bucket
     * @param path il path che il file dovrà avere (path logico, quello fisico
     * sul repository verrà generato random)
     * @param fileName il nome che il file dovrà avere (NB: in caso
     * overwrite=false e path logico e nome file già esistente, questo verrà
     * cambiato aggiungendo un numero alla fine)
     * @param metadata eventuali metadati da inserite, se non si vogliono
     * inserire passare null
     * @param overWrite se esiste già un file con lo stesso path logico e nome
     * file e overwrite=true, questo viene sovrascritto, se overwrite=false,
     * viene cambiato il nome file aggiungendo un numero alla fine e inserito
     * come nuovo file
     * @return un oggetto contenente tutte le informazioni sul file caricato
     * @throws MinIOWrapperException
     * @throws java.io.FileNotFoundException
     */
    public MinIOWrapperFileInfo put(File file, String codiceAzienda, String path, String fileName, Map<String, Object> metadata, boolean overWrite) throws MinIOWrapperException, FileNotFoundException, IOException {
        try ( FileInputStream fis = new FileInputStream(file)) {
            return put(fis, codiceAzienda, path, fileName, metadata, overWrite);
        }
    }
    
    /**
     * Carica un file sul repository
     *
     * @param obj il file da cariare sul repository
     * @param codiceAzienda es. 105, 902, 908, ecc. Corrisponde anche al nome
     * del bucket
     * @param path il path che il file dovrà avere (path logico, quello fisico
     * sul repository verrà generato random)
     * @param fileName il nome che il file dovrà avere (NB: in caso
     * overwrite=false e path logico e nome file già esistente, questo verrà
     * cambiato aggiungendo un numero alla fine)
     * @param metadata eventuali metadati da inserite, se non si vogliono
     * inserire passare null
     * @param overWrite se esiste già un file con lo stesso path logico e nome
     * file e overwrite=true, questo viene sovrascritto, se overwrite=false,
     * viene cambiato il nome file aggiungendo un numero alla fine e inserito
     * come nuovo file
     * @return un oggetto contenente tutte le informazioni sul file caricato
     * @throws MinIOWrapperException
     * @throws java.io.FileNotFoundException
     */
    public MinIOWrapperFileInfo put(InputStream obj, String codiceAzienda, String path, String fileName, Map<String, Object> metadata, boolean overWrite) throws MinIOWrapperException, FileNotFoundException, IOException {
        MinIOWrapperFileInfo res = putWithBucket(obj, codiceAzienda, path, fileName, metadata, overWrite, null, null);
        return res;
    }

    /**
     * Carica un file sul repository
     *
     * @param file il file da cariare sul repository
     * @param codiceAzienda es. 105, 902, 908, ecc. Corrisponde anche al nome
     * del bucket, se non diversamente specificato nell'apposito parametro
     * @param path il path che il file dovrà avere (path logico, quello fisico
     * sul repository verrà generato random)
     * @param fileName il nome che il file dovrà avere (NB: in caso
     * overwrite=false e path logico e nome file già esistente, questo verrà
     * cambiato aggiungendo un numero alla fine)
     * @param metadata eventuali metadati da inserite, se non si vogliono
     * inserire passare null
     * @param overWrite se esiste già un file con lo stesso path logico e nome
     * file e overwrite=true, questo viene sovrascritto, se overwrite=false,
     * viene cambiato il nome file aggiungendo un numero alla fine e inserito
     * come nuovo file
     * @param mongoUuid l'uuid di mongo, viene usato dal mongowrapper per
     * retrocompatibilità
     * @param bucket il bucket su cui scrivere il file. Se non passato sarà
     * usato il bucket che si chiama come il codiceAzienda
     * @return un oggetto contenente tutte le informazioni sul file caricato
     * @throws MinIOWrapperException
     * @throws java.io.FileNotFoundException
     */
    public MinIOWrapperFileInfo putWithBucket(File file, String codiceAzienda, String path, String fileName, Map<String, Object> metadata, boolean overWrite, String mongoUuid, String bucket) throws MinIOWrapperException, FileNotFoundException, IOException {
        try ( FileInputStream fis = new FileInputStream(file)) {
            MinIOWrapperFileInfo res = putWithBucket(fis, codiceAzienda, path, fileName, metadata, overWrite, mongoUuid, bucket);
            return res;
        }
    }

    /**
     * Carica un file sul repository
     *
     * @param obj il file da cariare sul repository
     * @param codiceAzienda es. 105, 902, 908, ecc. Corrisponde anche al nome
     * del bucket, se non diversamente specificato nell'apposito parametro
     * @param path il path che il file dovrà avere (path logico, quello fisico
     * sul repository verrà generato random)
     * @param fileName il nome che il file dovrà avere (NB: in caso
     * overwrite=false e path logico e nome file già esistente, questo verrà
     * cambiato aggiungendo un numero alla fine)
     * @param metadata eventuali metadati da inserite, se non si vogliono
     * inserire passare null
     * @param overWrite se esiste già un file con lo stesso path logico e nome
     * file e overwrite=true, questo viene sovrascritto, se overwrite=false,
     * viene cambiato il nome file aggiungendo un numero alla fine e inserito
     * come nuovo file
     * @param bucket il bucket su cui scrivere il file. Se non passato sarà
     * usato il bucket che si chiama come il codiceAzienda
     * @return un oggetto contenente tutte le informazioni sul file caricato
     * @throws MinIOWrapperException
     */
    public MinIOWrapperFileInfo putWithBucket(InputStream obj, String codiceAzienda, String path, String fileName, Map<String, Object> metadata, boolean overWrite, String bucket) throws MinIOWrapperException {
        return putWithBucket(obj, codiceAzienda, path, fileName, metadata, overWrite, null, bucket);
    }

    /**
     * Restituisce una stringa di massimo 255 caratteri inserendo un un uuid
     * prima dell'estensione. Se l'estesione è più lunga di 8 caratteri viene
     * accorciata anche questa a 8. Se la stringa passata come parametro non
     * supera i 255 caratteri, viene subito ritornata senza modifiche.
     *
     * @param name il nome da accorciare
     */
    private String getMinioTruncatedName(String name) {
        if (name.length() >= 255) {

            // ricavo l'estensione
            String extension = Files.getFileExtension(name);

            if (extension.length() >= 8) {
                // se l'estensione è più lunga di 8 caratteri (SIS/SISX) la tagliamo,
                // perché non è un'estensione: è un'anomalia
                extension = extension.substring(0, 7);
            }

            String uuidNameReplace = UUID.randomUUID().toString();

            // togliamo da 255 la lunghezza del uuid, un punto e l'estensione
            int positionBefore = 255 - (extension.length() + 1 + uuidNameReplace.length());

            name = name.substring(0, positionBefore) + uuidNameReplace;

            if (!extension.isEmpty()) {
                name = name + "." + extension;
            }
        }

        return name;

    }

    /**
     * Carica un file sul repository
     *
     * @param obj lo stream da cariare sul repository
     * @param codiceAzienda es. 105, 902, 908, ecc. Corrisponde anche al nome
     * del bucket, se non diversamente specificato nell'apposito parametro
     * @param path il path che il file dovrà avere (path logico, quello fisico
     * sul repository verrà generato random)
     * @param fileName il nome che il file dovrà avere (NB: in caso
     * overwrite=false e path logico e nome file già esistente, questo verrà
     * cambiato aggiungendo un numero alla fine)
     * @param metadata eventuali metadati da inserite, se non si vogliono
     * inserire passare null
     * @param overWrite se esiste già un file con lo stesso path logico e nome
     * file e overwrite=true, questo viene sovrascritto, se overwrite=false,
     * viene cambiato il nome file aggiungendo un numero alla fine e inserito
     * come nuovo file
     * @param mongoUuid l'uuid di mongo, viene usato dal mongowrapper per
     * retrocompatibilità
     * @param bucket il bucket su cui scrivere il file. Se non passato sarà
     * usato il bucket che si chiama come il codiceAzienda
     * @return un oggetto contenente tutte le informazioni sul file caricato
     * @throws MinIOWrapperException
     */
    public MinIOWrapperFileInfo putWithBucket(InputStream obj, String codiceAzienda, String path, String fileName, Map<String, Object> metadata, boolean overWrite, String mongoUuid, String bucket) throws MinIOWrapperException {
        try {
            // wrappo lo stream dentro uno DigestInputStream per poter calcolare md5
            DigestInputStream digestInputStream = new DigestInputStream(obj, MessageDigest.getInstance("MD5"));

            // ripulisco il path e tolgo l'eventuale "/" finale
            path = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(path), '/');

            // in base all'azienda passata, prendo il serverId sul quale il file andrà caricato
            Integer serverId = minIOServerAziendaMap.get(codiceAzienda);

            // in base al serveId letto prendo l'istanza del repository
            MinioClient minIOClient = minIOServerClientMap.get(serverId);

            // Se il nome è più lungo di 255 caratteri minIO da errore:
            // quindi bisogna accorciarlo cercando di mantenere l'estensione
            // NB: in tabella rimane il nome originale: solo il nome salvato su minIO viene accorciato
            if (fileName.length() >= 255) {
                fileName = getMinioTruncatedName(fileName);
            }

            // calcolo il path fisico sul quale fare l'upload del file
            String uuid = UUID.randomUUID().toString();
            String physicalPath = generatePhysicalPath(fileName, uuid);

            // il nome del bucket sul quale andrà fatto l'upload del file (se non passato è il codiceAzienda)
            String bucketName;
            if (StringUtils.hasText(bucket)) {
                bucketName = getBucketName(bucket);
            } else {
                bucketName = getBucketName(codiceAzienda);
            }

            // se il bucket ancora non esiste lo crea
            boolean bucketExists = minIOClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minIOClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            FileTableOperations fileTableOperation = FileTableOperations.INSERT;
            Integer fileTableId = null;
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime uploadDate = now;
            ZonedDateTime modifiedDate = null;

            // prima di fare l'uload verifico se per caso esiste già un file con lo stesso nome e lo stesso path
            // prendo un advisory lock sull'hash della stringa formata dall'unione del path, filename e codice_azienda in modo che sono sicuro che non ne verrà eventualmente caricato uno nel frattempo
            // NB: essendo un hash può essere che scatti il lock anche per una stringa che non sia quella voluta, nel caso quel caricamento si metterà in coda anche se non deve, ma non è un problema
            // TODO: provare a usare la pg_advisory_xact_lock con 2 parametri
            try ( Connection conn = (Connection) sql2oConnection.beginTransaction()) {
                int lockingHash = String.format("%s_%s_%s", path, fileName, codiceAzienda).hashCode();
                conn.createQuery(
                        "SELECT pg_advisory_xact_lock(:locking_hash::bigint)")
                        .addParameter("locking_hash", lockingHash)
                        .executeAndFetchTable();

                List<Map<String, Object>> res = conn.createQuery(
                        "select id, file_id, uuid, upload_date, modified_date "
                        + "from repo.files "
                        + "where path = :path and filename = :filename and codice_azienda = :codice_azienda and deleted = false")
                        .addParameter("path", path)
                        .addParameter("filename", fileName)
                        .addParameter("codice_azienda", codiceAzienda)
                        .executeAndFetchTable().asList();

                // se ne trovo uno...
                if (!res.isEmpty()) {
                    if (overWrite) {
                        // lo devo sovrascrivere; mi setto in fileTableOperation che dovro fare un'update in tabella e setto le variabili che servono
                        // inoltre setto physicalPath uguale a quello presente in tabella in modo da sovrascriverlo effettivamente sul repository
                        fileTableOperation = FileTableOperations.UPDATE;
                        Map<String, Object> foundFile = res.get(0);
                        physicalPath = (String) foundFile.get("file_id");
                        fileTableId = (Integer) foundFile.get("id");
                        uuid = (String) foundFile.get("uuid");
                        uploadDate = getZonedDateTime(foundFile.get("upload_date"));
                        modifiedDate = now;
                    } else {
                        // non lo devo sovrascrivere per cui sarà un normale upload con la differenza che al nome del file aggiungo un numero preso da una sequenza sul db.
                        // non vogliamo avere più file con lo stesso path (path + nomefile) logico.
                        fileName = getFileNameForNotOverwrite(conn, fileName);

                        // dato che il nome del file è cambiato, cambio anche il nome del file sul path fisico
                        physicalPath = physicalPath.replace(StringUtils.getFilename(physicalPath), fileName);
                    }
                }

                // upload del file presente nello stream "digestInputStream" sul bucket "bucketName" nel path "physicalPath"
                minIOClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(physicalPath).stream(digestInputStream, -1, 10485760).build());

                // leggo la dimensione del file dal repository
                long size = getSize(physicalPath, serverId, bucketName);

                // leggo l'md5 dallo stream e lo trasformo in esadecimale
                String md5 = String.format(Locale.ROOT, "%032x", new BigInteger(1, digestInputStream.getMessageDigest().digest()));

                switch (fileTableOperation) {
                    case INSERT: // se devo inserire, faccio l'insert
                        String insertQuery
                                = "INSERT INTO repo.files "
                                + "(file_id, uuid, bucket, metadata, \"path\", filename, codice_azienda, server_id, \"size\", upload_date, deleted, md5, mongo_uuid) "
                                + "VALUES(:file_id, :uuid, :bucket, :metadata::jsonb, :path, :filename, :codice_azienda, :server_id, :size, :upload_date, false, :md5, :mongo_uuid)";
                        fileTableId = conn.createQuery(insertQuery, true)
                                .addParameter("file_id", physicalPath)
                                .addParameter("uuid", uuid)
                                .addParameter("bucket", bucketName)
                                .addParameter("metadata", metadataToStringNullSafe(metadata))
                                .addParameter("path", path)
                                .addParameter("filename", fileName)
                                .addParameter("codice_azienda", codiceAzienda)
                                .addParameter("server_id", minIOServerAziendaMap.get(codiceAzienda))
                                .addParameter("upload_date", Timestamp.valueOf(uploadDate.toLocalDateTime()))
                                .addParameter("size", size)
                                .addParameter("md5", md5)
                                .addParameter("mongo_uuid", mongoUuid)
                                .executeUpdate().getKey(Integer.class);
                        break;

                    case UPDATE: // se devo fare l'update faccio l'update
                        // faccio l'update (solo in caso di overwrite), di quello che può essere cambiato: filename, metadata, server_id, size, modified_date, md5
                        String updateQuery
                                = "UPDATE repo.files "
                                + "SET metadata=:metadata, server_id=:server_id, size=:size, modified_date=:modified_date , md5=:md5, mongo_uuid = :mongo_uuid "
                                + "where id = :id and deleted=false";
                        conn.createQuery(updateQuery, true)
                                //.addParameter("filename", fileName)
                                .addParameter("metadata", metadataToStringNullSafe(metadata))
                                .addParameter("server_id", minIOServerAziendaMap.get(codiceAzienda))
                                .addParameter("size", size)
                                .addParameter("modified_date", Timestamp.valueOf(modifiedDate.toLocalDateTime()))
                                .addParameter("md5", md5)
                                .addParameter("mongo_uuid", mongoUuid)
                                .addParameter("id", fileTableId)
                                .executeUpdate();
                        break;
                }

                // tutto ok, faccio il commit
                conn.commit();

                // creo l'oggetto da tornare, conterrà tutte le informazioni del file
                MinIOWrapperFileInfo uploadRes = new MinIOWrapperFileInfo(
                        fileTableId,
                        physicalPath,
                        mongoUuid,
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

    public String getFileNameForNotOverwrite(String fileName) {
        try ( Connection conn = (Connection) sql2oConnection.open()) {
            return getFileNameForNotOverwrite(conn, fileName);
        }
    }

    public String getFileNameForNotOverwrite(Connection conn, String fileName) {
        Integer fileNameIndex = conn.createQuery("select nextval(:filename_seq)").addParameter("filename_seq", "repo.file_names_seq")
                .executeAndFetchFirst(Integer.class);
        return StringUtils.stripFilenameExtension(fileName) + "_" + fileNameIndex.toString() + "." + StringUtils.getFilenameExtension(fileName);
    }

    private String metadataToStringNullSafe(Map<String, Object> metadata) throws JsonProcessingException {
        if (metadata != null) {
            return objectMapper.writeValueAsString(metadata);
        } else {
            return null;
        }
    }

    /**
     * Torna il file identificato dal fileId passato
     *
     * @param fileId
     * @return il file identificato dal fileId passato
     * @throws MinIOWrapperException
     */
    public InputStream getByFileId(String fileId) throws MinIOWrapperException {
        return getByFileId(fileId, false);
    }

    /**
     * Torna il file identificato dal fileId passato
     *
     * @param fileId
     * @param includeDeleted se true il file verrà cercato anche all'interno del
     * cestino
     * @return il file identificato dal fileId passato
     * @throws MinIOWrapperException
     */
    public InputStream getByFileId(String fileId, boolean includeDeleted) throws MinIOWrapperException {
        MinIOWrapperFileInfo fileInfo = getFileInfoByFileId(fileId, includeDeleted);
        if (fileInfo != null) {
            return directGetFromMinIO(fileInfo.getFileId(), fileInfo.getServerId(), fileInfo.getBucketName());
        } else {
            return null;
        }
    }

    /**
     * Torna il file identificato dall'uuid di mongo passato (da usare per
     * retrocompatibilità, nel caso il file proveniva da mongo)
     *
     * @param mongoUuid
     * @return il file identificato dall'uuid di mongo passato
     * @throws MinIOWrapperException
     */
    public InputStream getByUuid(String mongoUuid) throws MinIOWrapperException {
        return getByUuid(mongoUuid, false);
    }

    /**
     * Torna il file identificato dall'uuid di mongo passato (da usare per
     * retrocompatibilità, nel caso il file proveniva da mongo)
     *
     * @param mongoUuid
     * @param includeDeleted se true il file verrà cercato anche all'interno del
     * cestino
     * @return il file identificato dall'uuid di mongo passato
     * @throws MinIOWrapperException
     */
    public InputStream getByUuid(String mongoUuid, boolean includeDeleted) throws MinIOWrapperException {
        MinIOWrapperFileInfo fileInfo = getFileInfoByUuid(mongoUuid, includeDeleted);
        if (fileInfo != null) {
            return directGetFromMinIO(fileInfo.getFileId(), fileInfo.getServerId(), fileInfo.getBucketName());
        } else {
            return null;
        }
    }

    /**
     * Torna il file identificato dal path e fileName passati sull'azienda
     * passata
     *
     * @param path
     * @param fileName
     * @param codiceAzienza
     * @return il file identificato dal path e fileName passati sull'azienda
     * passata
     * @throws MinIOWrapperException
     */
    public InputStream getByPathAndFileName(String path, String fileName, String codiceAzienza) throws MinIOWrapperException {
        return getByPathAndFileName(path, fileName, codiceAzienza, false);
    }

    /**
     * Torna il file identificato dal path e fileName passati sull'azienda
     * passata
     *
     * @param path
     * @param fileName
     * @param codiceAzienza
     * @param includeDeleted se true il file verrà cercato anche all'interno del
     * cestino
     * @return il file identificato dal path e fileName passati sull'azienda
     * passata
     * @throws MinIOWrapperException
     */
    public InputStream getByPathAndFileName(String path, String fileName, String codiceAzienza, boolean includeDeleted) throws MinIOWrapperException {
        MinIOWrapperFileInfo fileInfo = getFileInfoByPathAndFileName(path, fileName, codiceAzienza, includeDeleted);
        if (fileInfo != null) {
            return directGetFromMinIO(fileInfo.getFileId(), fileInfo.getServerId(), fileInfo.getBucketName());
        } else {
            return null;
        }
    }

    /**
     * Torna le informazioni relative al file identificato dal fileId passato
     *
     * @param fileId
     * @return le informazioni relative al file identificato dal fileId passato
     * @throws MinIOWrapperException
     */
    public MinIOWrapperFileInfo getFileInfoByFileId(String fileId) throws MinIOWrapperException {
        return getFileInfoByFileId(fileId, false);
    }

    public MinIOWrapperFileInfo getFileInfoByFileId(String fileId, boolean includeDeleted) throws MinIOWrapperException {
        return getFileInfo(fileId, null, null, null, null, includeDeleted);
    }

    /**
     * Torna le informazioni relative al file identificato dal fileId passato
     *
     * @param fileId
     * @param includeDeleted se true il file verrà cercato anche all'interno del
     * cestino
     * inferiore alla data passata (la data passata è compresa)
     * @return le informazioni relative al file identificato dal fileId passato
     * @throws MinIOWrapperException
     */
    private MinIOWrapperFileInfo getFileInfo(String fileId, String mongoUuid, String path, String fileName, String codiceAzienda, boolean includeDeleted) throws MinIOWrapperException {

        try ( Connection conn = (Connection) sql2oConnection.open()) {
            Query query = null;
            String queryString = "select id, file_id, mongo_uuid, path, filename, size, md5, server_id, codice_azienda, uuid, bucket, metadata, deleted, upload_date, modified_date, delete_date from repo.files [WHERE]" + (!includeDeleted ? " and deleted = false" : "");
            if (fileId != null) {
                // reperisco le informazioni dalla tabella repo.files cercando il file per file_id
                queryString = queryString.replace("[WHERE]", "where file_id = :file_id");
                query = conn.createQuery(queryString)
                        .addParameter("file_id", fileId);

            } else if (mongoUuid != null) {
                // reperisco le informazioni dalla tabella repo.files cercando il file per uuid di mongo
                queryString = queryString.replace("[WHERE]", "where mongo_uuid = :mongo_uuid");

                query = conn.createQuery(queryString)
                        .addParameter("mongo_uuid", mongoUuid);

            } else if (path != null && fileName != null && codiceAzienda != null) {
                // reperisco le informazioni dalla tabella repo.files cercando il file per path, file_name e codice_azienda
                path = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(path), '/');
                queryString = queryString.replace("[WHERE]", "where path = :path and filename = :filename and codice_azienda = :codice_azienda");

                query = conn.createQuery(queryString)
                        .addParameter("path", path)
                        .addParameter("filename", fileName)
                        .addParameter("codice_azienda", codiceAzienda);

            } else {
                throw new MinIOWrapperException("non è stata passata nessuan clausula di filtro");
            }

            List<Map<String, Object>> res = query.executeAndFetchTable().asList();
            if (!res.isEmpty()) {
                Map<String, Object> foundFile = res.get(0);
                Integer fileTableId = (Integer) foundFile.get("id");
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
                Map<String, Object> metadata = getJsonField(foundFile.get("metadata"), new TypeReference<Map<String, Object>>() {
                });
                Boolean deleted = (Boolean) foundFile.get("deleted");
                ZonedDateTime uploadDate = getZonedDateTime(foundFile.get("upload_date"));
                ZonedDateTime modifiedDate = getZonedDateTime(foundFile.get("modified_date"));
                ZonedDateTime deleteDate = getZonedDateTime(foundFile.get("delete_date"));

                MinIOWrapperFileInfo fileInfo = new MinIOWrapperFileInfo(
                        fileTableId,
                        fileIdFound,
                        mongoUuidFound,
                        pathFound,
                        fileNameFound,
                        size,
                        md5,
                        serverId,
                        uuidFound,
                        codiceAziendaFound,
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

    /**
     * Torna le informazioni relative al file identificato dal path e fileName
     * passati sull'azienda passata
     *
     * @param path
     * @param fileName
     * @param codiceAzienda
     * @return le informazioni relative al file identificato dal path e fileName
     * passati sull'azienda passata
     * @throws MinIOWrapperException
     */
    public MinIOWrapperFileInfo getFileInfoByPathAndFileName(String path, String fileName, String codiceAzienda) throws MinIOWrapperException {
        return getFileInfoByPathAndFileName(path, fileName, codiceAzienda, false);
    }

    /**
     * Torna le informazioni relative al file identificato dal path e fileName
     * passati sull'azienda passata
     *
     * @param path
     * @param fileName
     * @param codiceAzienda
     * @param includeDeleted se true il file verrà cercato anche all'interno del
     * cestino
     * @return le informazioni relative al file identificato dal path e fileName
     * passati sull'azienda passata
     * @throws MinIOWrapperException
     */
    public MinIOWrapperFileInfo getFileInfoByPathAndFileName(String path, String fileName, String codiceAzienda, boolean includeDeleted) throws MinIOWrapperException {
        return getFileInfo(null, null, path, fileName, codiceAzienda, includeDeleted);
    }

    /**
     * Torna le informazioni relative al file identificato dall'uuid di mongo
     * passato (da usare per retrocompatibilità, nel caso il file proveniva da
     * mongo)
     *
     * @param mongoUuid
     * @return le informazioni relative al file identificato dall'uuid di mongo
     * passato
     * @throws MinIOWrapperException
     */
    public MinIOWrapperFileInfo getFileInfoByUuid(String mongoUuid) throws MinIOWrapperException {
        return getFileInfo(null, mongoUuid, null, null, null, false);
    }

    /**
     * Torna le informazioni relative al file identificato dall'uuid di mongo
     * passato (da usare per retrocompatibilità, nel caso il file proveniva da
     * mongo)
     *
     * @param mongoUuid
     * @param includeDeleted se true il file verrà cercato anche all'interno del
     * cestino
     * @return le informazioni relative al file identificato dall'uuid di mongo
     * passato
     * @throws MinIOWrapperException
     */
    public MinIOWrapperFileInfo getFileInfoByUuid(String mongoUuid, boolean includeDeleted) throws MinIOWrapperException {
        return getFileInfo(null, mongoUuid, null, null, null, includeDeleted);
    }

    /**
     * Cambia il filename del file identificato dal fileId passato
     *
     * @param fileId
     * @param newFileName nuovo nome del file
     * @throws MinIOWrapperException nel caso esiste già un file con lo stesso
     * nome, nello stesso path, con lo stesso codice_azienda
     */
    public void renameByFileId(String fileId, String newFileName) throws MinIOWrapperException {
        renameByFileId(fileId, newFileName, false);
    }

    /**
     * Cambia il filename del file identificato dal fileId passato
     *
     * @param fileId
     * @param newFileName nuovo nome del file
     * @param includeDeleted se true il file verrà cercato anche all'interno del
     * cestino
     * @throws MinIOWrapperException nel caso esiste già un file con lo stesso
     * nome, nello stesso path, con lo stesso codice_azienda
     */
    public void renameByFileId(String fileId, String newFileName, boolean includeDeleted) throws MinIOWrapperException {
        try ( Connection conn = (Connection) sql2oConnection.beginTransaction()) {
            // siccome può esistere già un file con il nuovo nome che inserisco, similmente all'upload devo prendere un lock.
            // Così facendo blocco eventuali altre rinomine o upload con lo stesso nome

            // recupero il path e il codice_azienda del file da rinominare
            List<Map<String, Object>> pathAndAzienda = conn.createQuery(
                    "select \"path\", codice_azienda "
                    + "from repo.files "
                    + "where file_id = :file_id")
                    .addParameter("file_id", fileId)
                    .executeAndFetchTable().asList();
            // prendo in lock basato sul path, il nuovo nome e il codice azienda
            int lockingHash = String.format("%s_%s_%s", pathAndAzienda.get(0).get("path"), newFileName, pathAndAzienda.get(0).get("codice_azienda")).hashCode();
            conn.createQuery(
                    "SELECT pg_advisory_xact_lock(:locking_hash::bigint)")
                    .addParameter("locking_hash", lockingHash)
                    .executeAndFetchTable();

            // controllo se esiste già un file nello stesso path con lo stesso nome, se esiste lancio eccezione
            List<Map<String, Object>> res = conn.createQuery(
                    "select 1 "
                    + "from repo.files f1 join repo.files f2 on f1.\"path\" = f2.\"path\" and f1.codice_azienda = f2.codice_azienda "
                    + "where f2.file_id = :file_id "
                    + "and f1.filename = :new_filename" + (!includeDeleted ? " and f1.deleted = false" : ""))
                    .addParameter("file_id", fileId)
                    .addParameter("new_filename", newFileName)
                    .executeAndFetchTable().asList();
            if (res.isEmpty()) {
                conn.createQuery(
                        "update repo.files set filename = :new_filename, modified_date = now() "
                        + "where file_id = :file_id" + (!includeDeleted ? " and deleted = false" : ""))
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
        // siccome può esistere già un file con il nuovo nome che inserisco, similmente all'upload devo prendere un lock.
        // Così facendo blocco eventuali altre rinomine o upload con lo stesso nome

        // recupero il codice_azienda del file da rinominare
        try ( Connection conn = (Connection) sql2oConnection.beginTransaction()) {
            List<Map<String, Object>> pathAndAzienda = conn.createQuery(
                    "select codice_azienda "
                    + "from repo.files "
                    + "where file_id = :file_id ")
                    .addParameter("file_id", fileId)
                    .executeAndFetchTable().asList();

            // prendo in lock basato sul path, il nuovo nome e il codice azienda
            int lockingHash = String.format("%s_%s_%s", newPath, newFileName, pathAndAzienda.get(0).get("codice_azienda")).hashCode();
            conn.createQuery(
                    "SELECT pg_advisory_xact_lock(:locking_hash::bigint)")
                    .addParameter("locking_hash", lockingHash)
                    .executeAndFetchTable();

            // controllo se esiste già un file nello stesso path con lo stesso nome, se esiste lancio eccezione
            List<Map<String, Object>> res = conn.createQuery(
                    "select 1 "
                    + "from repo.files "
                    + "where \"path\" = :newPath and filename = :new_filename" + (!includeDeleted ? " and deleted = false" : ""))
                    .addParameter("newPath", newPath)
                    .addParameter("new_filename", newFileName)
                    .executeAndFetchTable().asList();
            if (res.isEmpty()) {
                conn.createQuery(
                        "update repo.files set path = :new_path, filename = :new_filename, modified_date = now() "
                        + "where file_id = :file_id" + (!includeDeleted ? " and deleted = false" : ""))
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

    public void renameByPathAndFileName(String path, String fileName, String newPath, String newFileName, String codiceAzienda) throws MinIOWrapperException {
        renameByPathAndFileName(path, fileName, newPath, newFileName, false, codiceAzienda);
    }

    public void renameByPathAndFileName(String path, String fileName, String newPath, String newFileName, boolean includeDeleted, String codiceAzienda) throws MinIOWrapperException {
        // ripulisco i path e elimino l'eventuale "/" alla fine
        path = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(path), '/');
        newPath = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(newPath), '/');

        // siccome può esistere già un file con il nuovo nome che inserisco, similmente all'upload devo prendere un lock.
        // Così facendo blocco eventuali altre rinomine o upload con lo stesso nome
        // recupero il codice_azienda del file da rinominare
        try ( Connection conn = (Connection) sql2oConnection.beginTransaction()) {

            // prendo in lock basato sul path, il nuovo nome e il codice azienda
            int lockingHash = String.format("%s_%s_%s", newPath, newFileName, codiceAzienda).hashCode();
            conn.createQuery(
                    "SELECT pg_advisory_xact_lock(:locking_hash::bigint)")
                    .addParameter("locking_hash", lockingHash)
                    .executeAndFetchTable();

            // controllo se esiste già un file nello stesso path con lo stesso nome, se esiste lancio eccezione
            List<Map<String, Object>> res = conn.createQuery(
                    "select 1 "
                    + "from repo.files "
                    + "where \"path\" = :newPath and filename = :new_filename and codice_azienda = :codice_azienda" + (!includeDeleted ? " and deleted = false" : ""))
                    .addParameter("newPath", newPath)
                    .addParameter("new_filename", newFileName)
                    .addParameter("codice_azienda", codiceAzienda)
                    .executeAndFetchTable().asList();
            if (res.isEmpty()) {
                conn.createQuery(
                        "update repo.files set path = :new_path, filename = :new_filename, modified_date = now() "
                        + "where path = :path and filename = :filename and codice_azienda = :codice_azienda" + (!includeDeleted ? " and deleted = false" : ""))
                        .addParameter("new_path", newPath)
                        .addParameter("new_filename", newFileName)
                        .addParameter("path", path)
                        .addParameter("filename", fileName)
                        .addParameter("codice_azienda", codiceAzienda)
                        .executeUpdate();
                conn.commit();
            } else {
                throw new MinIOWrapperException("impossibile rinominare: esiste già un file con lo stesso nome nello stesso path");
            }
        }
    }

    public void renameByPathAndFileName(String path, String fileName, String newFileName, String codiceAzienda) throws MinIOWrapperException {
        renameByPathAndFileName(path, fileName, newFileName, false, codiceAzienda);
    }

    public void renameByPathAndFileName(String path, String fileName, String newFileName, boolean includeDeleted, String codiceAzienda) throws MinIOWrapperException {
        // ripulisco il path e elimino la "/" finale
        path = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(path), '/');

        // siccome può esistere già un file con il nuovo nome che inserisco, similmente all'upload devo prendere un lock.
        // Così facendo blocco eventuali altre rinomine o upload con lo stesso nome
        // recupero il path e il codice_azienda del file da rinominare
        try ( Connection conn = (Connection) sql2oConnection.beginTransaction()) {
            List<Map<String, Object>> pathFromDB = conn.createQuery(
                    "select \"path\" "
                    + "from repo.files "
                    + "where path = :path and filename = :filename and codice_azienda = :codice_azienda")
                    .addParameter("path", path)
                    .addParameter("filename", fileName)
                    .addParameter("codice_azienda", codiceAzienda)
                    .executeAndFetchTable().asList();

            // prendo in lock basato sul path, il nuovo nome e il codice azienda
            int lockingHash = String.format("%s_%s_%s", pathFromDB.get(0).get("path"), newFileName, codiceAzienda).hashCode();
            conn.createQuery(
                    "SELECT pg_advisory_xact_lock(:locking_hash::bigint)")
                    .addParameter("locking_hash", lockingHash)
                    .executeAndFetchTable();

            // controllo se esiste già un file nello stesso path con lo stesso nome, se esiste lancio eccezione
            List<Map<String, Object>> res = conn.createQuery(
                    "select 1 "
                    + "from repo.files f1 join repo.files f2 on f1.\"path\" = f2.\"path\" and f1.codice_azienda = f2.codice_azienda "
                    + "where f2.\"path\" = :path and f2.filename = :filename "
                    + "and f1.filename = :new_filename and f1.codice_azienda = :codice_azienda" + (!includeDeleted ? " and f1.deleted = false" : ""))
                    .addParameter("path", path)
                    .addParameter("filename", fileName)
                    .addParameter("new_filename", newFileName)
                    .addParameter("codice_azienda", codiceAzienda)
                    .executeAndFetchTable().asList();
            if (res.isEmpty()) {
                conn.createQuery(
                        "update repo.files set filename = :new_filename, modified_date = now() "
                        + "where path = :path and filename = :filename and codice_azienda = :codice_azienda" + (!includeDeleted ? " and deleted = false" : ""))
                        .addParameter("new_filename", newFileName)
                        .addParameter("path", path)
                        .addParameter("filename", fileName)
                        .addParameter("codice_azienda", codiceAzienda)
                        .executeUpdate();
                conn.commit();
            } else {
                throw new MinIOWrapperException("impossibile rinominare: esiste già un file con lo stesso nome nello stesso path");
            }
        }
    }

    /**
     * Elimina tramite cancellazione logica un file identificato dal fileId
     * passato. Il file viene settato a deleted su DB e spostato nel bucket di
     * TRASH
     *
     * @param mongoUuid
     * @throws MinIOWrapperException
     */
    public void deleteByFileUuid(String mongoUuid) throws MinIOWrapperException {
        delete(null, mongoUuid, null, null, null);
    }

    /**
     * Elimina tramite cancellazione logica un file identificato dal fileId
     * passato. Il file viene settato a deleted su DB e spostato nel bucket di
     * TRASH
     *
     * @param fileId
     * @throws MinIOWrapperException
     */
    public void deleteByFileId(String fileId) throws MinIOWrapperException {
        delete(fileId, null, null, null, null);
    }

    /**
     * Elimina tramite cancellazione logica un file identificato da path,
     * filename e codice_azienda passati. Il file viene settato a deleted su DB
     * e spostato nel bucket di TRASH
     *
     * @param path
     * @param fileName
     * @param codiceAzienda
     * @throws MinIOWrapperException
     */
    public void deleteByPathAndFileName(String path, String fileName, String codiceAzienda) throws MinIOWrapperException {
        delete(null, null, path, fileName, codiceAzienda);
    }

    private void delete(String fileId, String mongoUuid, String path, String fileName, String codiceAzienda) throws MinIOWrapperException {
        String queryString
                = "update repo.files f "
                + "set deleted = true,  delete_date = now(), bucket = :bucket "
                + "[WHERE] "
                + "returning (select array[server_id::text, bucket, file_id] from repo.files where id = f.id)";
        try ( Connection conn = (Connection) sql2oConnection.beginTransaction()) {
            Query query = null;
            if (fileId != null) {
                // reperisco le informazioni dalla tabella repo.files cercando il file per file_id
                queryString = queryString.replace("[WHERE]", "where file_id = :file_id and deleted = false");
                query = conn.createQuery(queryString, true)
                        .addParameter("file_id", fileId)
                        .addParameter("bucket", trashBucket);
            } else if (mongoUuid != null) {
                // reperisco le informazioni dalla tabella repo.files cercando il file per uuid di mongo
                queryString = queryString.replace("[WHERE]", "where mongo_uuid = :mongo_uuid and deleted = false");
                query = conn.createQuery(queryString, true)
                        .addParameter("mongo_uuid", mongoUuid)
                        .addParameter("bucket", trashBucket);
            } else if (path != null && fileName != null && codiceAzienda != null) {
                // reperisco le informazioni dalla tabella repo.files cercando il file per path, file_name e codice_azienda
                path = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(path), '/');
                queryString = queryString.replace("[WHERE]", "where path = :path and filename = :filename and codice_azienda = :codice_azienda and deleted = false");
                query = conn.createQuery(queryString, true)
                        .addParameter("path", path)
                        .addParameter("filename", fileName)
                        .addParameter("codice_azienda", codiceAzienda)
                        .addParameter("bucket", trashBucket);
            }
            logger.info("query eliminazione: " + query.toString());
            PgArray keyPgArray = (PgArray) query.executeUpdate().getKey();
            if (keyPgArray != null) {
                Object[] array = (Object[]) keyPgArray.getArray();
                Integer serverId = Integer.parseInt((String) array[0]);
                String bucket = (String) array[1];
                String fileIdReturned = (String) array[2];
                moveToTrash(fileIdReturned, bucket, serverId);
                conn.commit();
            }
        } catch (Throwable ex) {
            logger.error("errore eliminazione file da minIO: ", ex);
            throw new MinIOWrapperException("errore eliminazione file da minIO", ex);
        }
    }

    /**
     * Ripristina un file cancellato logicamente
     *
     * @param fileId
     * @throws MinIOWrapperException
     */
    public void restoreByFileId(String fileId) throws MinIOWrapperException {
        try ( Connection conn = (Connection) sql2oConnection.beginTransaction()) {
            List<Map<String, Object>> pathAndAzienda = conn.createQuery(
                    "select \"path\", filename, codice_azienda, bucket, server_id "
                    + "from repo.files "
                    + "where file_id = :file_id")
                    .addParameter("file_id", fileId)
                    .executeAndFetchTable().asList();

            // prendo in lock basato sul path, il nuovo nome e il codice azienda
            String path = (String) pathAndAzienda.get(0).get("path");
            String fileName = (String) pathAndAzienda.get(0).get("filename");
            String codiceAzienda = (String) pathAndAzienda.get(0).get("codice_azienda");
//            String bucket = (String) pathAndAzienda.get(0).get("bucket");
            Integer serverId = (Integer) pathAndAzienda.get(0).get("server_id");
            int lockingHash = String.format("%s_%s_%s", path, fileName, codiceAzienda).hashCode();
            conn.createQuery(
                    "SELECT pg_advisory_xact_lock(:locking_hash::bigint)")
                    .addParameter("locking_hash", lockingHash)
                    .executeAndFetchTable();

            // controllo se esiste già un file nello stesso path con lo stesso nome, se esiste lancio eccezione
            List<Map<String, Object>> res = conn.createQuery(
                    "select 1 "
                    + "from repo.files "
                    + "where \"path\" = :path and filename = :filename and codice_azienda = :codice_azienda and deleted = false ")
                    .addParameter("path", path)
                    .addParameter("filename", fileName)
                    .addParameter("codice_azienda", codiceAzienda)
                    .executeAndFetchTable().asList();
            String fileIdUpdated;
            if (!res.isEmpty()) {
                // per non avere un file con lo stesso nome di un file già presente (nello stesso path per lo stesso codice_azienda, cambio il nome
                Integer fileNameIndex = conn.createQuery("select nextval(:filename_seq)").addParameter("filename_seq", "repo.file_names_seq")
                        .executeAndFetchFirst(Integer.class);
                fileName = StringUtils.stripFilenameExtension(fileName) + "_" + fileNameIndex.toString() + "." + StringUtils.getFilenameExtension(fileName);
            }

            fileIdUpdated = conn.createQuery(
                    "update repo.files set filename = :filename, deleted = false, delete_date = null, bucket = codice_azienda "
                    + "where file_id = :file_id and deleted = true returning file_id", true)
                    .addParameter("file_id", fileId)
                    .addParameter("filename", fileName)
                    .executeUpdate().getKey(String.class);

            if (StringUtils.hasText(fileIdUpdated)) {
                // se ho effettivamente trovato il file da riprostinare, lo ripristino
                restoreFromTrash(fileId, codiceAzienda, serverId);
                conn.commit();
            }
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nel ripristino", ex);
        }
    }

    /**
     * Sposta il file in un altro bucket senza rimuovere il bucket sorgente
     * @param fileId il fileId del file da spostare
     * @param srcBucket il bucket in cui il filesi trova
     * @param serverId il serverId del file
     * @param dstBucket il bucket in cui spostare il file
     * @throws MinIOWrapperException 
     */
    public void moveToAnotherBucket(String fileId, String srcBucket, Integer serverId, String dstBucket) throws MinIOWrapperException {
        moveToAnotherBucket(fileId, srcBucket, serverId, dstBucket, false);
    }
    
    /**
     * Sposta il file in un altro bucket con la possibilità di rimuovere il bucket sorgente
     * @param fileId il fileId del file da spostare
     * @param srcBucket il bucket in cui il filesi trova
     * @param serverId il serverId del file
     * @param dstBucket il bucket in cui spostare il file
     * @param removeSrcBucket passare true se si vuole eliminare il bucket sorgente
     * @throws MinIOWrapperException 
     */
    public void moveToAnotherBucket(String fileId, String srcBucket, Integer serverId, String dstBucket, boolean removeSrcBucket) throws MinIOWrapperException {
        // dato che minIO non supporta lo spostamento, prima copiamo il file nel nuovo bucket e poi eliminiamo dal vecchio
        try {
            MinioClient minIOClient = minIOServerClientMap.get(serverId);
            boolean dstBucketExists = minIOClient.bucketExists(BucketExistsArgs.builder().bucket(dstBucket).build());
            if (!dstBucketExists) {
                minIOClient.makeBucket(MakeBucketArgs.builder().bucket(dstBucket).build());
            }
            minIOClient.copyObject(CopyObjectArgs.builder().bucket(dstBucket).object(fileId)
                    .source(CopySource.builder().bucket(srcBucket).object(fileId).build()).build());
            minIOClient.removeObject(RemoveObjectArgs.builder().bucket(srcBucket).object(fileId).build());
            if (removeSrcBucket) {
                minIOClient.removeBucket(RemoveBucketArgs.builder().bucket(srcBucket).build());
            }
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nello spostamento del file all'interno del bucket " + dstBucket, ex);
        }
    }
    
    /**
     * Sposta un file dal bucket passato al bucket di trash
     *
     * @param fileId
     * @param srcBucket
     * @param serverId
     * @throws MinIOWrapperException
     */
    private void moveToTrash(String fileId, String srcBucket, Integer serverId) throws MinIOWrapperException {
        // dato che minIO non supporta lo postamento, prima copiamo il file nel nuovo bucket e poi eliminiamo dal vecchio
        try {
            MinioClient minIOClient = minIOServerClientMap.get(serverId);
            boolean trashBucketExists = minIOClient.bucketExists(BucketExistsArgs.builder().bucket(trashBucket).build());
            if (!trashBucketExists) {
                minIOClient.makeBucket(MakeBucketArgs.builder().bucket(trashBucket).build());
            }
            minIOClient.copyObject(CopyObjectArgs.builder().bucket(trashBucket).object(fileId)
                    .source(CopySource.builder().bucket(srcBucket).object(fileId).build()).build());
            minIOClient.removeObject(RemoveObjectArgs.builder().bucket(srcBucket).object(fileId).build());
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nello spostamento del file all'interno del bucket " + trashBucket, ex);
        }
    }

    /**
     * Sposta un file dal bucket di trash al bucket passato
     *
     * @param fileId
     * @param srcBucket
     * @param serverId
     * @throws MinIOWrapperException
     */
    private void restoreFromTrash(String fileId, String srcBucket, Integer serverId) throws MinIOWrapperException {
        try {
            // dato che minIO non supporta lo postamento, prima copiamo il file nel nuovo bucket e poi eliminiamo dal vecchio
            MinioClient minIOClient = minIOServerClientMap.get(serverId);
            boolean srcBucketExists = minIOClient.bucketExists(BucketExistsArgs.builder().bucket(srcBucket).build());
            if (!srcBucketExists) {
                minIOClient.makeBucket(MakeBucketArgs.builder().bucket(srcBucket).build());
            }
            minIOClient.copyObject(CopyObjectArgs.builder().bucket(srcBucket).object(fileId)
                    .source(CopySource.builder().bucket(trashBucket).object(fileId).build()).build());
            minIOClient.removeObject(RemoveObjectArgs.builder().bucket(trashBucket).object(fileId).build());
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nello ripristino del file dal bucket " + trashBucket, ex);
        }
    }

    /**
     * Elimina definitavamente un file
     *
     * @param fileId
     * @param onlyDeleted se "true" lo elimina solo se è già stato eliminato
     * logicamente, se "false" lo elimina in ogni caso
     * @throws MinIOWrapperException
     */
    public void removeByFileId(String fileId, boolean onlyDeleted) throws MinIOWrapperException {
        try ( Connection conn = (Connection) sql2oConnection.open()) {
            String queryString
                    = "delete "
                    + "from repo.files "
                    + "where file_id = :file_id" + (onlyDeleted ? " and deleted = true " : " ")
                    + "returning array[server_id::text, bucket]";
            Query query = conn.createQuery(queryString, true)
                    .addParameter("file_id", fileId);

            // logger.info("eseguo la query:" + query.toString());
            PgArray keyPgArray = (PgArray) query.executeUpdate().getKey();
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

    /**
     * Cancella un file dal bucket passato
     *
     * @param fileId
     * @param bucket
     * @param serverId
     * @throws MinIOWrapperException
     */
    private void remove(String fileId, String bucket, Integer serverId) throws MinIOWrapperException {
        try {
            MinioClient minIOClient = minIOServerClientMap.get(serverId);
            minIOClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(fileId).build());
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nella cancellazione del file dal bucket " + bucket, ex);
        }
    }

    /**
     * Torna tutti i file all'interno del path passato, inclusi i file nei
     * sotto-path
     *
     * @param path
     * @param codiceAzienda
     * @return
     * @throws MinIOWrapperException
     */
    public List<MinIOWrapperFileInfo> getFilesInPath(String path, String codiceAzienda) throws MinIOWrapperException {
        return getFilesInPath(path, false, true, codiceAzienda);
    }

    /**
     * Torna tutti i files nel path indicato, inclusi i file nei sotto-path
     *
     * @param path
     * @param includeDeleted se "true" torna anche i file cancellati logicamente
     * @param includeSubDir
     * @param codiceAzienda
     * @return
     * @throws MinIOWrapperException
     */
    public List<MinIOWrapperFileInfo> getFilesInPath(String path, boolean includeDeleted, boolean includeSubDir, String codiceAzienda) throws MinIOWrapperException {
        path = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(path), '/');
        try ( Connection conn = (Connection) sql2oConnection.open()) {
            Sql2oArray pathsArray = new Sql2oArray(StringUtils.delimitedListToStringArray(StringUtils.trimLeadingCharacter(path, '/'), "/"));

            // tramite questa query si riescono a prendere tutte le righe nel path e nei sotto-path.
            // ogni path, al momento dell'iserimento della riga viene convertito in array(viene creato facendo split sulla "/") da un trigger
            // avendo quindi un array si pùò convertire path passato in array, allo stesso modo del path sul DB e prendere tutte le righe in cui l'array è contenuto
            // es. path_cercato_array <@ paths. Una volta trovati gli elementi si affina la ricerca con path like 'path_cercato%'
            // postgres farà prima la ricerca sugli array, dato che c'è un indice.
            Query query;
            String queryString
                    = "select id, file_id, mongo_uuid, uuid, bucket, metadata, path, filename, codice_azienda, server_id, size, md5, deleted, upload_date, modified_date, delete_date "
                    + "from repo.files "
                    + "[WHERE]" + (!includeDeleted ? " and deleted = false" : "");
            if (includeSubDir) {
                queryString = queryString.replace("[WHERE]", "where :path_array::text[] <@ paths_array and path like :path || '%'" + (codiceAzienda != null ? " and codice_azienda = :codice_azienda" : ""));
                query = conn.createQuery(queryString)
                        .addParameter("path_array", pathsArray)
                        .addParameter("path", path)
                        .addParameter("codice_azienda", codiceAzienda);
            } else {
                queryString = queryString.replace("[WHERE]", "where path = :path" + (codiceAzienda != null ? " and codice_azienda = :codice_azienda" : ""));
                query = conn.createQuery(queryString)
                        .addParameter("path", path)
                        .addParameter("codice_azienda", codiceAzienda);
            }
            List<Map<String, Object>> queryRes = query.executeAndFetchTable().asList();
            List<MinIOWrapperFileInfo> res = null;
            if (!queryRes.isEmpty()) {
                res = new ArrayList<>();
                for (Map<String, Object> foundFile : queryRes) {
                    Integer fileTableId = (Integer) foundFile.get("id");
                    String fileId = (String) foundFile.get("file_id");
                    String uuid = (String) foundFile.get("uuid");
                    String mongoUuid = (String) foundFile.get("mongo_uuid");
                    String bucket = (String) foundFile.get("bucket");
                    Map<String, Object> metadata = getJsonField(foundFile.get("metadata"), new TypeReference<Map<String, Object>>() {
                    });
                    String filePath = (String) foundFile.get("path");
                    String fileName = (String) foundFile.get("filename");
                    String codiceAziendaLetto = (String) foundFile.get("codice_azienda");
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
                            mongoUuid,
                            filePath,
                            fileName,
                            size,
                            md5,
                            serverId,
                            uuid,
                            codiceAziendaLetto,
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

    /**
     * Torna tutti i file con data inferiore alla data passata (la data passata
     * è inclusa)
     *
     * @param codiceAzienda
     * @param time
     * @param includeDeleted
     * @return
     * @throws MinIOWrapperException
     */
    public List<MinIOWrapperFileInfo> getFilesLessThan(String codiceAzienda, ZonedDateTime time, boolean includeDeleted) throws MinIOWrapperException {
        try ( Connection conn = (Connection) sql2oConnection.open()) {
            String queryString
                    = "select id, file_id, mongo_uuid, uuid, bucket, metadata, path, filename, codice_azienda, server_id, size, md5, deleted, upload_date, modified_date, delete_date "
                    + "from repo.files "
                    + "where upload_date <= :upload_date and codice_azienda = :codice_azienda" + (!includeDeleted ? " and deleted = false" : "");

            Query query = conn.createQuery(queryString)
                    .addParameter("upload_date", Timestamp.valueOf(time.toLocalDateTime()))
                    .addParameter("codice_azienda", codiceAzienda);

            List<Map<String, Object>> queryRes = query.executeAndFetchTable().asList();
            List<MinIOWrapperFileInfo> res = null;
            if (!queryRes.isEmpty()) {
                res = new ArrayList<>();
                for (Map<String, Object> foundFile : queryRes) {
                    Integer fileTableId = (Integer) foundFile.get("id");
                    String fileId = (String) foundFile.get("file_id");
                    String uuid = (String) foundFile.get("uuid");
                    String mongoUuid = (String) foundFile.get("mongo_uuid");
                    String bucket = (String) foundFile.get("bucket");
                    Map<String, Object> metadata = getJsonField(foundFile.get("metadata"), new TypeReference<Map<String, Object>>() {
                    });
                    String filePath = (String) foundFile.get("path");
                    String fileName = (String) foundFile.get("filename");
                    String codiceAziendaFound = (String) foundFile.get("codice_azienda");
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
                            mongoUuid,
                            filePath,
                            fileName,
                            size,
                            md5,
                            serverId,
                            uuid,
                            codiceAziendaFound,
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

    /**
     * cancella logicamente tutti i files nel path indicato, inclusi i file nei
     * sotto-path
     *
     * @param path
     * @param codiceAzienda
     * @throws MinIOWrapperException
     */
    public void delFilesInPath(String path, String codiceAzienda) throws MinIOWrapperException {
        delFilesInPath(path, true, codiceAzienda);
    }

    /**
     * cancella logicamente tutti i files nel path indicato
     *
     * @param path
     * @param includeSubDir se true cancella anche i file nelle sotto-directory
     * @param codiceAzienda
     * @throws MinIOWrapperException
     */
    public void delFilesInPath(String path, boolean includeSubDir, String codiceAzienda) throws MinIOWrapperException {
        path = StringUtils.trimTrailingCharacter(StringUtils.cleanPath(path), '/');

        try ( Connection conn = (Connection) sql2oConnection.open()) {
            Query query;
            Sql2oArray pathsArray = new Sql2oArray(StringUtils.delimitedListToStringArray(StringUtils.trimLeadingCharacter(path, '/'), "/"));
            String queryString
                    = "update repo.files f "
                    + "set deleted = true, delete_date = now(), bucket = :bucket "
                    + "[WHERE] and deleted = false "
                    + "returning (select array[file_id, bucket, server_id::text] from repo.files where id = f.id)";
            if (includeSubDir) {
                queryString = queryString.replace("[WHERE]", "where :path_array::text[] <@ paths_array and path like :path || '%' and deleted = false and codice_azienda = :codice_azienda");
                query = conn.createQuery(queryString, true)
                        .addParameter("bucket", trashBucket)
                        .addParameter("path_array", pathsArray)
                        .addParameter("path", path)
                        .addParameter("codice_azienda", codiceAzienda);
            } else {
                queryString = queryString.replace("[WHERE]", "where path = :path and codice_azienda = :codice_azienda");
                query = conn.createQuery(queryString, true)
                        .addParameter("bucket", trashBucket)
                        .addParameter("path", path)
                        .addParameter("codice_azienda", codiceAzienda);
            }

            // tramite questa query si riescono a prendere tutte le righe nel path e nei sotto-path.
            // ogni path, al momento dell'iserimento della riga viene convertito in array(viene creato facendo split sulla "/") da un trigger
            // avendo quindi un array si pùò convertire path passato in array, allo stesso modo del path sul DB e prendere tutte le righe in cui l'array è contenuto
            // es. path_cercato_array <@ paths. Una volta trovati gli elementi si affina la ricerca con path like 'path_cercato%'
            // postgres farà prima la ricerca sugli array, dato che c'è un indice.
            Object[] deletedFiles = query.executeUpdate().getKeys();
            if (deletedFiles != null) {
                for (Object deletedFile : deletedFiles) {
                    if (deletedFile != null) {
                        String[] deletedFileArray = (String[]) ((PgArray) deletedFile).getArray();
                        moveToTrash(deletedFileArray[0], deletedFileArray[1], Integer.parseInt(deletedFileArray[2]));
                    }
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw new MinIOWrapperException("errore nella cancellazione logica dei files", ex);
        }
    }

    /**
     * Torna l'elenco dei file cancellati logicamente di tutte le aziende
     *
     * @return l'elenco dei file cancellati logicamente di tutte le aziende
     * @throws MinIOWrapperException
     */
    public List<MinIOWrapperFileInfo> getDeleted() throws MinIOWrapperException {
        return getDeleted(null, null);
    }

    /**
     * Torna l'elenco dei file cancellati logicamente per l'azienda passata fino
     * alla data passata
     *
     * @param codiceAzienda se "null" torna quelli di tutte le aziende
     * @param lessThan se null, torna tutti i file eliminati, altrimenti torna
     * tutti quelli eliminati fino a quella data (compresa)
     * @return l'elenco dei file cancellati logicamente per l'azienda passata
     * @throws MinIOWrapperException
     */
    public List<MinIOWrapperFileInfo> getDeleted(String codiceAzienda, ZonedDateTime lessThan) throws MinIOWrapperException {
        try ( Connection conn = (Connection) sql2oConnection.open()) {
            Query query;
            String queryString
                    = "select id, file_id, mongo_uuid, uuid, bucket, metadata, path, filename, codice_azienda, server_id, size, md5, deleted, upload_date, modified_date, delete_date "
                    + "from repo.files "
                    + "[WHERE] [DATE]";
            if (lessThan != null) {
                queryString = queryString.replace("[DATE]", "and delete_date <= :delete_date");
            } else {
                queryString = queryString.replace("[DATE]", "");
            }
            if (codiceAzienda == null) {
                queryString = queryString.replace("[WHERE]", "where deleted = true");
                query = conn.createQuery(queryString);
            } else {
                queryString = queryString.replace("[WHERE]", "where deleted = true and codice_azienda = :codice_azienda");
                query = conn.createQuery(queryString)
                        .addParameter("codice_azienda", codiceAzienda);
            }
            if (lessThan != null) {
                query.addParameter("delete_date", Timestamp.valueOf(lessThan.toLocalDateTime()));
            }
            List<Map<String, Object>> queryRes = query.executeAndFetchTable().asList();
            List<MinIOWrapperFileInfo> res = null;
            if (!queryRes.isEmpty()) {
                res = new ArrayList<>();
                for (Map<String, Object> foundFile : queryRes) {
                    Integer fileTableId = (Integer) foundFile.get("id");
                    String fileId = (String) foundFile.get("file_id");
                    String uuid = (String) foundFile.get("uuid");
                    String mongoUuid = (String) foundFile.get("mongo_uuid");
                    String bucket = (String) foundFile.get("bucket");
                    Map<String, Object> metadata = getJsonField(foundFile.get("metadata"), new TypeReference<Map<String, Object>>() {
                    });
                    String filePath = (String) foundFile.get("path");
                    String fileName = (String) foundFile.get("filename");
                    String codiceAziendaFound = (String) foundFile.get("codice_azienda");
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
                            mongoUuid,
                            filePath,
                            fileName,
                            size,
                            md5,
                            serverId,
                            uuid,
                            codiceAziendaFound,
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
            throw new MinIOWrapperException("errore nella cancellazione logica dei files", ex);
        }
    }

    private <T> T getJsonField(Object field, TypeReference<T> type) throws MinIOWrapperException {
        try {
            if (field != null) {
                return objectMapper.readValue(((PGobject) field).getValue(), type);
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nel reperimento dei metadati", ex);
        }
    }
    
    private <T> T getJsonField(Object field, Class<T> type) throws MinIOWrapperException {
        return getJsonField(field, new TypeReference<T>() {
            @Override
            public Type getType() {
                return type;
            }
        });
    }

    private ZonedDateTime getZonedDateTime(Object timestamptzField) throws MinIOWrapperException {
        try {
            ZonedDateTime date = timestamptzField != null ? ZonedDateTime.ofInstant(((Timestamp) timestamptzField).toInstant(), ZoneId.systemDefault()) : null;
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
            if (ex.errorResponse().code().equals("NoSuchKey")) {
                return null;
            } else {
                throw new MinIOWrapperException("errore nel reperimento del file", ex);
            }
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nel reperimento del file", ex);
        }
    }

    public static Sql2o getSql2oConnection() {
        return sql2oConnection;
    }

    public static MinioClient getMinIOClient(Integer serverId) {
        return minIOServerClientMap.get(serverId);
    }

    private long getSize(String fileId, Integer serverId, String bucket) throws MinIOWrapperException {
        MinioClient minIOClient = minIOServerClientMap.get(serverId);
        try {
            return minIOClient.statObject(io.minio.StatObjectArgs.builder().bucket(bucket).object(fileId).build()).size();
        } catch (Exception ex) {
            throw new MinIOWrapperException("errore nel reperimento della dimesione del file", ex);
        }
    }

    public String getMongoUuidByFileUuid(String fileUuid)  throws MinIOWrapperException {
        return getMongoUuidByFileUuid(fileUuid, false);
    }

    private String getMongoUuidByFileUuid(String fileUuid, boolean includeDeleted) throws MinIOWrapperException {
        if (fileUuid == null || !patternUuid.matcher(fileUuid).matches()) {
            throw new MinIOWrapperException("Invalid fileUuid: " + fileUuid);
        }

        String queryString = "SELECT mongo_uuid FROM repo.files WHERE uuid = :fileUuid" +
                (!includeDeleted ? " AND deleted = false" : "") + " LIMIT 1";

        try (Connection conn = sql2oConnection.open();
             Query query = conn.createQuery(queryString).addParameter("fileUuid", fileUuid)) {
            return query.executeScalar(String.class);
        }
    }
}
