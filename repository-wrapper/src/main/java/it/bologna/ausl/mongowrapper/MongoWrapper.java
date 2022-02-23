package it.bologna.ausl.mongowrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoURI;
import com.mongodb.QueryBuilder;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import it.bologna.ausl.mongowrapper.exceptions.FileDeletedExceptions;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.bson.types.ObjectId;

/**
 * Questa classe si occupa dell'interazione con MongoDB utilizzando la libreria
 * mongo-java-driver: http://docs.mongodb.org/ecosystem/drivers/java/ La
 * connessione con mongoDB avviene secondo il pattern "Singleton". Viene
 * utilizzata una struttura a directory, anche se nativamente mongo non la
 * supporta, per reallizzarla si setta per ogni file caricato una proprietà che
 * ne indica la directory. Inoltre in una collection "dirs" inseriamo l'elenco
 * delle directory create.
 *
 * @author Giuseppe De Marco (gdm)
 */
public class MongoWrapper {

//    static {
//        Properties env = System.getProperties();
//        Set<Object> keySet = env.keySet();
//        Iterator<Object> iterator = keySet.iterator();
//        while (iterator.hasNext()) {
//            String next = (String) iterator.next();
//            System.out.println("--------env--------" + next + ":"+env.get(next));
//        }
//        String catalinaHome = System.getProperty("catalina.home");
//        System.out.println("CatalinaHome: " + catalinaHome);
//        if (catalinaHome != null && !catalinaHome.equals(""))
//            PropertyConfigurator.configure(Thread.currentThread().getContextClassLoader().getResource("it/bologna/ausl/mongowrapper/conf/log4j.properties"));
//        else
//     configuro il logger per la console
//        BasicConfigurator.configure();
//        System.setProperty("log4j.configurationFile", Thread.currentThread().getContextClassLoader().getResource("it/bologna/ausl/mongowrapper/conf/log4j2.xml").getFile());
//    }
//    private static final Logger log = LogManager.getLogger(MongoWrapper.class);
    private static final Logger log = LogManager.getLogger(MongoWrapper.class);
    private Mongo m = null;
    private static final Mongo.Holder holder = Mongo.Holder.singleton();

    private DB db;
    private GridFS gfs;
    private static final String TRASH_DIR = "/TRASH_DIR";
    private static final String IS_TEMP_COLLECTION_NAME = "isTemp";

    private static final String NO_MONGO = "DONTUSEMONGO";

    public static MongoWrapper getWrapper(boolean useMinIO, String mongoUri, String minIODBDriver, String minIODBUrl, String minIODBUsername, String minIODBPassword, String codiceAzienda, ObjectMapper objectMapper) throws UnknownHostException, MongoException, MongoWrapperException {
        if (mongoUri.equals(NO_MONGO)) {
            return new WrapperMinIO(mongoUri, minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, codiceAzienda, 5, objectMapper);
        } else if (useMinIO) {
            return new MongoWrapperMinIO(mongoUri, minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, codiceAzienda, 5, objectMapper);
        } else {
            return new MongoWrapper(mongoUri);
        }
    }

    public static MongoWrapper getWrapper(boolean useMinIO, String mongoUri, String minIODBDriver, String minIODBUrl,
            String minIODBUsername, String minIODBPassword, String codiceAzienda, Integer maxPoolSize, ObjectMapper objectMapper) throws UnknownHostException, MongoException, MongoWrapperException {
        if (mongoUri.equals(NO_MONGO)) {
            return new WrapperMinIO(mongoUri, minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, codiceAzienda, maxPoolSize, objectMapper);
        } else if (useMinIO) {
            return new MongoWrapperMinIO(mongoUri, minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, codiceAzienda, maxPoolSize, objectMapper);
        } else {
            return new MongoWrapper(mongoUri);
        }
    }

    /**
     * istanzia la classe collegandosi a mongo
     *
     * @param mongoUri uri per la connessione a mongo
     * @throws UnknownHostException UnknownHostException
     * @throws MongoException MongoException
     * @throws MongoWrapperException MongoWrapperException
     */
    public MongoWrapper(String mongoUri) throws UnknownHostException, MongoException, MongoWrapperException {
        mongoWrapperInit(mongoUri);
    }

    public MongoWrapper(String mongoUri, String babelT2Uri) throws UnknownHostException, MongoException, MongoWrapperException {
        this(mongoUri);
    }

    private void mongoWrapperInit(String mongoUri) throws UnknownHostException, MongoException, MongoWrapperException {
        if (mongoUri != null && !mongoUri.equals("") && !mongoUri.equals(NO_MONGO)) {
            MongoURI u = new MongoURI(mongoUri);

            log.debug("connecting on: " + mongoUri + "...");
            m = holder.connect(u);
            log.debug("connected to: " + mongoUri);
            this.db = m.getDB(u.getDatabase());
            log.debug("authenticating to: " + mongoUri + "...");
            if (!this.db.isAuthenticated()) {
                if (u.getUsername() != null) {
                    boolean auth = this.db.authenticate(u.getUsername(), u.getPassword());
                    if (auth != true) {
                        throw new MongoWrapperException("Auth failed");
                    }
                }
            }
            log.debug("authenticated to: " + mongoUri);
            gfs = new GridFS(this.db);
        }
    }

    public DB getDB() {
        return db;
    }

    /**
     * inserisce il riferimento a una nuova cartella nell'elenco delle
     * directory. Usata per inserire il riferimento alla directory dopo il
     * caricamento di un file
     *
     * @param dirname il path da inserire
     * @return true se tutto ok, false altrimenti
     */
    public boolean createDir(String dirname) {
        DBCollection dirs = db.getCollection("dirs");
        String root = "";
        boolean res = false;
        ObjectId o = null;
        for (String p : dirname.split("/")) {
            if (p.equals("")) {
                continue;
            }
            root += "/" + p;
            BasicDBObject d = new BasicDBObject().append("dirname", root);
            try {
                dirs.insert(d, com.mongodb.WriteConcern.ACKNOWLEDGED);
                o = (ObjectId) d.get("_id");
                res = true;
            } catch (com.mongodb.MongoException.DuplicateKey e) {

            }
        }
        return res;
    }

    public void regenerateAllDirs() throws MongoWrapperException {
        DBCollection fileCollection = db.getCollection("fs.files");
        Set<String> dirsSet;
        try (DBCursor files = fileCollection.find()) {
            dirsSet = new HashSet<>();
            while (files.hasNext()) {
                DBObject file = files.next();
                log.debug("analyzing file: " + file.toString() + "...");
                String filename = (String) file.get("filename");
                if (filename.contains("\\")) {
                    filename = filename.replace("\\", "/");
                    move(file.get("_id").toString(), filename);
                }
                if (filename.matches("^.*?/{2,}.*?$")) {
                    filename = filename.replaceAll("/{2,}", "/");
                    move(file.get("_id").toString(), filename);
                }
                String dname = getDname(filename);
                if (dname != null) {
                    dirsSet.add(dname);
                }
            }
        }
        DBCollection dirs = db.getCollection("dirs");
        dirs.drop();
        dirs.createIndex(new BasicDBObject().append("dirname", 1), "dir_index", true);
        for (String dir : dirsSet) {
            log.debug("creating dir: " + dir + "...");
            createDir(dir);
        }
    }

    /**
     * carica un file su mongo
     *
     * @param is InputStream del file da caricare
     * @param filename nome che il file assumerà
     * @param dirname directory in cui inserirlo
     * @param overwrite se true, in caso esista un file con lo stesso nome e
     * nella stessa direcory, sarà sovrascritto; se false al file corrente sarà
     * aggiunto un suffisso "_1", oppure "_2", ecc.
     * @return l'uuid del file creato
     * @throws IOException IOException
     */
    public String put(InputStream is, String filename, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        return _put(null, is, null, filename, dirname, null, null, overwrite);
    }

    /**
     * carica un file su mongo
     *
     * @param f il file da caricare
     * @param filename nome che il file assumerà
     * @param dirname directory in cui inserirlo
     * @param overwrite se true, in caso esista un file con lo stesso nome e
     * nella stessa direcory, sarà sovrascritto; se false al file corrente sarà
     * aggiunto un suffisso "_1", oppure "_2", ecc.
     * @return l'uuid del file creato
     * @throws IOException IOException
     */
    public String put(File f, String filename, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        return _put(f, null, null, filename, dirname, null, null, overwrite);
    }

    /**
     * carica un file su mongo con un determinato id
     *
     * @param is InputStream del file da caricare
     * @param uuid l'uuid che il file dovrà assumere
     * @param filename nome che il file assumerà
     * @param dirname directory in cui inserirlo
     * @param overwrite se true, in caso esista un file con lo stesso nome e
     * nella stessa direcory, sarà sovrascritto; se false al file corrente sarà
     * aggiunto un suffisso "_1", oppure "_2", ecc.
     * @return l'uuid del file creato
     * @throws IOException IOException
     */
    public String put(InputStream is, String uuid, String filename, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        return _put(null, is, uuid, filename, dirname, null, null, overwrite);
    }

    /**
     * carica un file su mongo con un determinato id
     *
     * @param f il file da caricare
     * @param uuid l'uuid che il file dovrà assumere
     * @param filename nome che il file assumerà
     * @param dirname directory in cui inserirlo
     * @param overwrite se true, in caso esista un file con lo stesso nome e
     * nella stessa direcory, sarà sovrascritto; se false al file corrente sarà
     * aggiunto un suffisso "_1", oppure "_2", ecc.
     * @return l'uuid del file creato
     * @throws IOException IOException
     */
    public String put(File f, String uuid, String filename, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        return _put(f, null, uuid, filename, dirname, null, null, overwrite);
    }

    /**
     * carica un file su mongo settando la proprietà creator e category
     *
     * @param is InputStream del file da caricare
     * @param filename nome che il file assumerà
     * @param dirname directory in cui inserirlo
     * @param creator creatore del file
     * @param category categoria di appartenenza del file
     * @param overwrite se true, in caso esista un file con lo stesso nome e
     * nella stessa direcory, sarà sovrascritto; se false al file corrente sarà
     * aggiunto un suffisso "_1", oppure "_2", ecc.
     * @return l'uuid del file creato
     * @throws IOException IOException
     */
    public String put(InputStream is, String filename, String dirname, String creator, String category, boolean overwrite) throws IOException, MongoWrapperException {
        return _put(null, is, null, filename, dirname, creator, category, overwrite);
    }

    /**
     * carica un file su mongo con un determinato id
     *
     * @param f il file da caricare
     * @param filename nome che il file assumerà
     * @param dirname directory in cui inserirlo
     * @param creator creatore del file
     * @param category categoria di appartenenza del file
     * @param overwrite se true, in caso esista un file con lo stesso nome e
     * nella stessa direcory, sarà sovrascritto; se false al file corrente sarà
     * aggiunto un suffisso "_1", oppure "_2", ecc.
     * @return l'uuid del file creato
     * @throws IOException IOException
     */
    public String put(File f, String filename, String dirname, String creator, String category, boolean overwrite) throws IOException, MongoWrapperException {
        return _put(f, null, null, filename, dirname, creator, category, overwrite);
    }

    /**
     * carica un file su mongo settando la proprietà creator e category
     *
     * @param is InputStream del file da caricare
     * @param uuid l'uuid che il file dovrà assumere
     * @param filename nome che il file assumerà
     * @param dirname directory in cui inserirlo
     * @param creator creatore del file
     * @param category categoria di appartenenza del file
     * @param overwrite se true, in caso esista un file con lo stesso nome e
     * nella stessa direcory, sarà sovrascritto; se false al file corrente sarà
     * aggiunto un suffisso "_1", oppure "_2", ecc.
     * @return l'uuid del file creato
     * @throws IOException IOException
     */
    public String put(InputStream is, String uuid, String filename, String creator, String category, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        return _put(null, is, uuid, filename, dirname, creator, category, overwrite);
    }

    /**
     * carica un file su mongo settando la proprietà creator e category
     *
     * @param f il file da caricare
     * @param uuid l'uuid che il file dovrà assumere
     * @param filename nome che il file assumerà
     * @param dirname directory in cui inserirlo
     * @param creator creatore del file
     * @param category categoria di appartenenza del file
     * @param overwrite se true, in caso esista un file con lo stesso nome e
     * nella stessa direcory, sarà sovrascritto; se false al file corrente sarà
     * aggiunto un suffisso "_1", oppure "_2", ecc.
     * @return l'uuid del file creato
     * @throws IOException IOException
     */
    public String put(File f, String uuid, String filename, String dirname, String creator, String category, boolean overwrite) throws IOException, MongoWrapperException {
        return _put(f, null, uuid, filename, dirname, creator, category, overwrite);
    }

    /**
     * esegue il caricamento vero e proprio (chiamata da tutte le altre
     * funzionin di caricamento)
     *
     * @param is InputStream del file da caricare
     * @param uuid l'uuid che il file dovrà assumere
     * @param filename nome che il file assumerà
     * @param dirname directory in cui inserirlo
     * @param creator creatore del file
     * @param category categoria di appartenenza del file
     * @param overwrite se true, in caso esista un file con lo stesso nome e
     * nella stessa direcory, sarà sovrascritto; se false al file corrente sarà
     * aggiunto un suffisso "_1", oppure "_2", ecc.
     * @return l'uuid del file creato
     * @throws IOException IOException
     */
    private String _put(File f, InputStream is, String uuid, String filename, String dirname, String creator, String category, boolean overwrite) throws IOException, MongoWrapperException {
        GridFSInputFile inf;
        String path = dirname + '/' + filename;
        log.info("putting " + filename + " in: " + dirname + (overwrite ? " overwriting..." : "..."));
        if (overwrite) {
            deleteByPath(path);
        } else {
            int i = 1;
            while (existsObjectByPathNoOverride(path)) {
                path = dirname + '/' + removeExtensionFromFileName(filename) + "_" + i + "." + getExtensionFromFileName(filename);
                i++;
            }
            filename = getFname(path);
        }
        if (f != null) {
            inf = gfs.createFile(f);
        } else {
            inf = gfs.createFile(is);
        }
        inf.setFilename(dirname + '/' + filename);
        if (uuid != null) {
            inf.setId(new ObjectId(uuid));
        }
        DBObject metadata = new BasicDBObject();
        metadata.put("creator", creator);
        metadata.put("category", category);
        inf.setMetaData(metadata);
        //inf.setContentType("application/pdf");
        inf.save();
        createDir(dirname);
        String resUuid;
        if (uuid != null) {
            resUuid = uuid;
        } else {
            resUuid = ObjectId.massageToObjectId(inf.getId()).toString();
        }
        return resUuid;
    }

    /**
     * scarica da mongo il file identificato dall'uuid passato e ne torna
     * l'InputStream
     *
     * @param uuid uuid del file da scaricare
     * @return l'InputStream del file identificato dall'uuid passato
     */
    public InputStream get(String uuid) throws MongoWrapperException {
        GridFSDBFile tmp = null;
        try {
            tmp = getGridFSFile(uuid);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }
        if (tmp != null) {
            return tmp.getInputStream();
        }
        return null;
    }

    /**
     * scarica da mongo il file identificato dal filepath passato e ne torna
     * l'InputStream
     *
     * @param filepath su mongo del file da scaricare
     * @return l'InputStream del file identificato dall'uuid passato
     */
    public InputStream getByPath(String filepath) throws MongoWrapperException {
        GridFSDBFile tmp = null;
        try {
            tmp = getGridFSFileByPath(filepath);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }
        if (tmp != null) {
            return tmp.getInputStream();
        }
        return null;
    }

    /**
     * Ritorna MD5 di un file indentificato con il suo uuid
     *
     * @param uuid uuid del file
     * @return MD5 del file passato come parametro
     */
    public String getMD5ByUuid(String uuid) throws MongoWrapperException {
        GridFSDBFile tmp = null;
        try {
            tmp = getGridFSFile(uuid);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }
        if (tmp != null) {
            return tmp.getMD5();
        }
        return null;
    }

    /**
     * torna l'uuid del file passato per filepath
     *
     * @param filepath il filepath su mongo del file
     * @return l'uuid del file passato per filepath
     */
    public String getFidByPath(String filepath) throws MongoWrapperException {
//        DBCollection c = db.getCollection("fs.files");
//        DBObject dbo = new BasicDBObject("filename", filepath);
//        DBObject res = c.findOne(dbo);
//        if (res != null) {
//            return res.get("_id").toString();
//        } else {
//            return null;
//        }
        GridFSDBFile tmp = null;
        try {
            tmp = getGridFSFileByPath(filepath);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }
        if (tmp != null) {
            return tmp.get("_id").toString();
        }
        return null;
    }

    /**
     * Ritorna MD5 di un file passandogli il suo path
     *
     * @param filepath
     * @return MD5 del file passato per filepath
     */
    public String getMD5ByPath(String filepath) throws MongoWrapperException {
        GridFSDBFile tmp = null;
        try {
            tmp = getGridFSFileByPath(filepath);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }
        if (tmp != null) {
            return tmp.getMD5();
        }
        return null;
    }

    /**
     * torna il GridFS del file su mongo identificato dal filepath passato
     *
     * @param filepath il filepath su mongo del file
     * @return il GridFS del file su mongo identificato dal filepath passato
     * @throws it.bologna.ausl.mongowrapper.exceptions.FileDeletedExceptions se
     * il file è stato eliminato logicamente
     */
    public GridFSDBFile getGridFSFileByPath(String filepath) throws FileDeletedExceptions, MongoWrapperException {
        List<GridFSDBFile> files = getGridFSFileByPath(filepath, false);
        if (files != null) {
            return files.get(0);
        } else {
            return null;
        }
    }

    /**
     * torna il GridFS del file su mongo identificato dal filepath passato
     *
     * @param filepath il filepath su mongo del file
     * @param includeDeleted indica se includere anche i file eliminati
     * logicamente
     * @return il GridFS del file su mongo identificato dal filepath passato
     * @throws it.bologna.ausl.mongowrapper.exceptions.FileDeletedExceptions se
     * includeDeleted = false e il file è stato eliminato logicamente
     */
    private List<GridFSDBFile> getGridFSFileByPath(String filepath, boolean includeDeleted) throws MongoWrapperException, FileDeletedExceptions {
        DBObject query = QueryBuilder.start("filename").is(filepath).get();
        if (includeDeleted) {
            query = QueryBuilder.start().
                    or(
                            QueryBuilder.start("metadata.oldPath").is(filepath).get(),
                            query
                    ).get();
        }
        List<GridFSDBFile> files = gfs.find(query);
        if (files == null) {
            log.warn("--file not found--");
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String stackToString = Arrays.toString(stackTrace);
            log.warn(stackToString);
            log.warn("--    --    --");
        }
//        if (includeDeleted) {
//            if (files == null) {
//                files = new ArrayList<>();
//            }
//            files.addAll(gfs.find(TRASH_DIR + filepath));
//        }
        if (!includeDeleted) {
            for (GridFSDBFile f : files) {
                if (f.getMetaData() == null || !f.getMetaData().containsField("deleteDate")) {
                    return Arrays.asList(f);
                }
            }
        }
        throw new FileDeletedExceptions("all files with this filepath are logically deleted");
    }

    /**
     * torna il GridFS del file su mongo identificato dall'uuid passato
     *
     * @param uuid l'uuid del file
     * @return il GridFS del file su mongo identificato dall'uuid passato
     * @throws it.bologna.ausl.mongowrapper.exceptions.FileDeletedExceptions se
     * il file è stato eliminato logicamente
     */
    public GridFSDBFile getGridFSFile(String uuid) throws MongoWrapperException, FileDeletedExceptions {
        return getGridFSFile(uuid, false);
    }

    /**
     * torna il GridFS del file su mongo identificato dall'uuid passato
     *
     * @param uuid l'uuid del file
     * @param includeDeleted indica se includere anche i file eliminati
     * logicamente
     * @return il GridFS del file su mongo identificato dall'uuid passato
     * @throws it.bologna.ausl.mongowrapper.exceptions.FileDeletedExceptions se
     * includeDeleted = false e il file è stato eliminato logicamente
     */
    private GridFSDBFile getGridFSFile(String uuid, boolean includeDeleted) throws MongoWrapperException, FileDeletedExceptions {
        BasicDBObject id = null;
        ObjectId oid = null;
        try {
            oid = new ObjectId(uuid);
        } catch (Exception e) {
            id = new BasicDBObject().append("_id", uuid);
        }
        //GridFSDBFile f=gfs.findOne(new ObjectId(fid));
        GridFSDBFile f;
        if (oid != null) {
            f = gfs.findOne(oid);
        } else {
            f = gfs.findOne(id);
        }
        if (f == null) {
            log.warn("--file not found--");
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String stackToString = Arrays.toString(stackTrace);
            log.warn(stackToString);
            log.warn("--    --    --");
        } else if (!includeDeleted && f.getMetaData() != null && f.getMetaData().containsField("deleteDate")) {
            throw new FileDeletedExceptions("file logically deleted");
        }
        return f;
    }

    /**
     * torna il GridFS del file su mongo identificato dalla query passata
     * (vengono esclusi in automatico i file eliminati logicamente)
     *
     * @param query la query
     * @return una lista di GridFSDBFile risultato della query passata (vengono
     * esclusi in automatico i file eliminati logicamente)
     */
    public GridFSDBFile getGridFSFile(DBObject query) {
        return getGridFSFile(query, false);
    }

    /**
     * torna il GridFS del file su mongo identificato dalla query passata
     * (vengono esclusi in automatico i file eliminati logicamente)
     *
     * @param query la query
     * @param includeDeleted indica se includere anche i file eliminati
     * logicamente
     * @return una lista di GridFSDBFile risultato della query passata (se
     * includeDeleted = false vengono esclusi in automatico i file eliminati
     * logicamente)
     */
    private GridFSDBFile getGridFSFile(DBObject query, boolean includeDeleted) {
        if (!includeDeleted) {
            query = QueryBuilder.start().and(
                    query,
                    QueryBuilder.start("metadata.deleteDate").not().exists("metadata.deleteDate").get()
            ).get();
        }
        GridFSDBFile f = gfs.findOne(query);
        return f;
    }

    /**
     * torna una lista di GridFSDBFile risultato della query passata (vengono
     * esclusi in automatico i file eliminati logicamente)
     *
     * @param query la query
     * @return una lista di GridFSDBFile risultato della query passata (vengono
     * esclusi in automatico i file eliminati logicamente)
     */
    private List<GridFSDBFile> getFiles(DBObject query) {
        return getFiles(query, false);
    }

    /**
     * torna una lista di GridFSDBFile risultato della query passata (vengono
     * esclusi in automatico i file eliminati logicamente)
     *
     * @param query la query
     * @param includeDeleted indica se includere anche i file eliminati
     * logicamente
     * @return una lista di GridFSDBFile risultato della query passata (vengono
     * esclusi in automatico i file eliminati logicamente)
     */
    private List<GridFSDBFile> getFiles(DBObject query, boolean includeDeleted) {
        if (!includeDeleted) {
            // devo escludere dalla query i file eliminati logicamente
            query = QueryBuilder.start().and(
                    query,
                    QueryBuilder.start("metadata.deleteDate").not().exists("metadata.deleteDate").get()
            ).get();
        }
        List<GridFSDBFile> list = gfs.find(query);
        return list;
    }

    /**
     * torna solo il filename di un filepath passato
     *
     * @param filepath il filepath
     * @return solo il filename di un filepath
     */
    public static String getFname(String filepath) {
        return new File(filepath).getName().replace("\\", "/");
    }

    /**
     * torna solo il path del filepath passato
     *
     * @param filepath il filepath
     * @return solo il path del filepath passato
     */
    public static String getDname(String filepath) {
        String parent = new File(filepath).getParent();
        if (parent != null) {
            return parent.replace("\\", "/");
        } else {
            return null;
        }
    }

    /**
     * torna il filename passato senza l'estensione
     *
     * @param filename il filename
     * @return il filename passato senza l'estensione
     */
    private static String removeExtensionFromFileName(String filename) {
        String res = filename;
        int pos = filename.lastIndexOf(".");
        if (pos > 0) {
            res = res.substring(0, pos);
        }
        return res;
    }

    /**
     * torna solo l'estensione del filename passato
     *
     * @param filename il filename
     * @return solo l'estensione del filename passato
     */
    private static String getExtensionFromFileName(String filename) {
        String res = "";
        int pos = filename.lastIndexOf(".");
        if (pos > 0) {
            res = filename.substring(pos + 1, filename.length());
        }
        return res;
    }

    /**
     * torna l'elenco dei file di una directory, ma non nelle sottodirectory
     *
     * @param dirname nome della directory
     * @return un ArrayList contente l'elenco dei file nella directory passata
     */
    public List<String> getDirFiles(String dirname) throws MongoWrapperException {
        if (!dirname.endsWith("/")) {
            dirname += "/";
        }
        BasicDBObject query = new BasicDBObject().append("filename", java.util.regex.Pattern.compile("^" + dirname + "([^/])+$"));
        return getFilesId(getFiles(query));
    }

    /**
     * torna l'elenco dei file di una directory, ma non nelle sottodirectory
     *
     * @param dirname nome della directory
     * @return un ArrayList contente l'elenco dei file nella directory passata
     */
    private List<String> getDirFilesNoOverride(String dirname) throws MongoWrapperException {
        if (!dirname.endsWith("/")) {
            dirname += "/";
        }
        BasicDBObject query = new BasicDBObject().append("filename", java.util.regex.Pattern.compile("^" + dirname + "([^/])+$"));
        return getFilesId(getFiles(query));
    }

    /**
     * torna l'elenco dei file di una directory, e nelle sottodirectory
     *
     * @param dirname nome della directory
     * @return un ArrayList contente l'elenco dei file nella directory passata
     * @throws it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException
     */
    public List<String> getDirFilesAndFolders(String dirname) throws MongoWrapperException {
        if (!dirname.endsWith("/")) {
            //NON TOCCARE se no non stiamo piu' partendo da una directory
            dirname += "/";
        }
        BasicDBObject query = new BasicDBObject().append("filename", java.util.regex.Pattern.compile("^" + dirname + ".*$"));
        return getFilesId(getFiles(query));
    }

    private List<String> getDirFilesAndFoldersNoOverride(String dirname) throws MongoWrapperException {
        if (!dirname.endsWith("/")) {
            //NON TOCCARE se no non stiamo piu' partendo da una directory
            dirname += "/";
        }
        BasicDBObject query = new BasicDBObject().append("filename", java.util.regex.Pattern.compile("^" + dirname + ".*$"));
        return getFilesId(getFiles(query));
    }

    /**
     * elimina da mongo il file identificato dall'uuid passato
     *
     * @param uuid l'uuid del file da eliminare
     */
    @Deprecated
    public void oldDelete(String uuid) {
        BasicDBObject id = null;
        ObjectId oid = null;
        try {
            oid = new ObjectId(uuid);
        } catch (Exception e) {
            id = new BasicDBObject().append("_id", uuid);

        }
        log.info("deleting from uuid: " + uuid + "...");
        if (oid != null) {
            gfs.remove(oid);
        } else {
            gfs.remove(id);
        }
        log.info("deleted uuid: " + uuid);
    }

    /**
     * elimina da mongo il file identificato dal filepath passato
     *
     * @param filepath il filepath del file da eliminare
     */
    @Deprecated
    public void oldDeletebyPath(String filepath) {
        log.info("deleting from filepath: " + filepath + "...");
        gfs.remove(filepath);
        log.info("deleted filepath: " + filepath);
    }

    /**
     * elimina logicamente da mongo il file identificato dall'uuid passato
     * (aggiunge il preffiso "/TRASH_DIR" davanti al filepath del file e
     * aggiunge gli attributi nei metadati: deleteDate e oldPath con il
     * filepath)
     *
     * @param uuid l'uuid del file da eliminare logicamente
     * @throws it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException
     */
    public void delete(String uuid) throws MongoWrapperException {
        GridFSDBFile f = null;
        try {
            f = getGridFSFile(uuid);
        } catch (FileDeletedExceptions ex) {
            // log.warn(ex);
        }
        if (f != null) {
            delete(f);
        }
    }

    private void deleteNoOverride(String uuid) throws MongoWrapperException {
        GridFSDBFile f = null;
        try {
            f = getGridFSFile(uuid);
        } catch (FileDeletedExceptions ex) {
            // log.warn(ex);
        }
        if (f != null) {
            delete(f);
        }
    }

    /**
     * elimina logicamente da mongo il file identificato dal filepath passato
     * (aggiunge il preffiso "/TRASH_DIR" davanti al filepath del file e
     * aggiunge gli attributi nei metadati: deleteDate e oldPath con il
     * filepath)
     *
     * @param filepath il filepath del file da eliminare logicamente
     * @throws it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException
     */
    public void deleteByPath(String filepath) throws MongoWrapperException {
        GridFSDBFile f = null;
        try {
            f = getGridFSFileByPath(filepath);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }
        if (f != null) {
            delete(f);
        }
    }

    /**
     * esegue l'eliminazione logica vera e propria (aggiunge il preffiso
     * "/TRASH_DIR" davanti al filepath del file e aggiunge gli attributi nei
     * metadati: deleteDate e oldPath con il filepath)
     *
     * @param f il GridFSDBFile da eliminare
     */
    private void delete(GridFSDBFile f) {
        DBObject metadata = f.getMetaData();
        if (metadata == null) {
            metadata = new BasicDBObject();
        }
        metadata.put("deleteDate", System.currentTimeMillis());
        metadata.put("oldPath", f.getFilename());
        f.setMetaData(metadata);
        f.put("filename", TRASH_DIR + f.getFilename());
        f.save();
        createDir(getDname(f.getFilename()));
    }

    /**
     * elimina definitivamente da mongo il file identificato dall'uuid passato
     *
     * @param uuid l'uuid del file da eliminare definitivamente
     * @throws it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException
     */
    public void erase(String uuid) throws MongoWrapperException {
        BasicDBObject id = null;
        ObjectId oid = null;
        try {
            oid = new ObjectId(uuid);
        } catch (Exception e) {
            id = new BasicDBObject().append("_id", uuid);

        }
        log.info("deleting from uuid: " + uuid + "...");
        if (oid != null) {
            gfs.remove(oid);
        } else {
            gfs.remove(id);
        }
        log.info("deleted uuid: " + uuid);
    }

    /**
     * ripristina un file eliminato logicamente da mongo identificato dall'uuid
     * passato (rimuove l'attributo deleteDate dai metadati)
     *
     * @param uuid l'uuid del file da ripristinare
     * @throws it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException
     * file non presente oppure se esiste già il file da ripristinare
     */
    public void unDelete(String uuid) throws MongoWrapperException {
        GridFSDBFile f = null;
        try {
            f = getGridFSFile(uuid, true);
        } catch (FileDeletedExceptions ex) {
            throw new MongoWrapperException("this shouldn't happen", ex);
        }

        if (f != null && f.getMetaData() != null && f.getMetaData().containsField("deleteDate")) {
            String filePath = (String) f.getMetaData().get("oldPath");
            if (filePath != null && existsObjectByPathNoOverride(filePath)) {
                throw new MongoWrapperException("filepath: " + filePath + " already exists, delete it first");
            }
            unDelete(f);
        } else {
            log.warn("nothing to undelete");
        }
    }

    /**
     * NON FUNZIONA: non si può fare perché ce ne possono essere tanti eliminati
     * con lo stesso path quale ripristino? forse potrei ripristinare solo se ce
     * n'è solo 1, altrimenti errore.
     *
     * ripristina un file eliminato logicamente da mongo identificato dal
     * filepath passato (rimuove l'attributo deleteDate dai metadati)
     *
     * @param filepath il filepath del file da ripristinare
     * @throws it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException
     * MongoWrapperException
     */
    @Deprecated
    public void unDeleteByPath(String filepath) throws MongoWrapperException {
        if (existsObjectByPathNoOverride(filepath)) {
            throw new MongoWrapperException("filepath already exist, delete it first");
        }
        List<GridFSDBFile> files = null;
        try {
            files = getGridFSFileByPath(filepath, true);
        } catch (FileDeletedExceptions ex) {
            throw new MongoWrapperException("this shouldn't happen", ex);
        }
//        if (files != null) {
//            unDelete(f);
//        }
//        else {
//            log.warn("nothing to undelete");
//        }
    }

    /**
     * ripristina un GridFSDBFile eliminato logicamente da mongo (rimuove gli
     * attributi deleteDate e oldPath dai metadati e ripristina il filePath
     * mettendoci l'oldPath)
     *
     * @param f il GridFSDBFile da ripristinare
     * @throws MongoWrapperException MongoWrapperException
     */
    private void unDelete(GridFSDBFile f) throws MongoWrapperException {
        DBObject metadata = f.getMetaData();
        if (metadata != null && metadata.containsField("deleteDate")) {
            String filepath = (String) metadata.get("oldPath");
            metadata.removeField("deleteDate");
            metadata.removeField("oldPath");
            if (metadata.keySet().isEmpty()) {
                metadata = null;
            }
            f.setMetaData(metadata);
            f.put("filename", filepath);
            f.save();
        } else {
            log.warn("nothing to undelete");
        }
    }

    /**
     * Torna l'elenco degli uuid dei file eliminati logicamente fino alla data
     * passata
     *
     * @param timeInMillis data passata
     * @return l'elenco degli uuid dei file eliminati logicamente fino alla data
     * passata
     * @throws it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException
     */
    public List<String> getDeletedLessThan(long timeInMillis) throws MongoWrapperException {
        DBObject query = QueryBuilder.start("metadata.deleteDate").lessThanEquals(timeInMillis).get();
        return getFilesId(getFiles(query, true));
    }

    /**
     * Torna l'elenco degli uuid dei file caricati prima della data passata (la
     * data passata è inclusa)
     *
     * @param time la data
     * @return l'elenco degli uuid dei file caricati prima della data passata
     * (la data passata è inclusa)
     * @throws MongoWrapperException
     */
    public List<String> getFilesLessThan(ZonedDateTime time) throws MongoWrapperException {
        DBObject query = QueryBuilder.start("uploadDate").lessThanEquals(new Date(time.toInstant().toEpochMilli())).get();
        return getFilesId(getFiles(query, true));
    }

    /**
     * Torna l'elenco degli uuid di tutti i file eliminati logicamente
     *
     * @return l'elenco degli uuid di tutti i file eliminati logicamente
     */
    public List<String> getDeleted() throws MongoWrapperException {
        DBObject query = QueryBuilder.start("metadata.deleteDate").exists("metadata.deleteDate").get();
        return getFilesId(getFiles(query, true));
    }

    /**
     * Torna l'elenco degli uuid dei GridFSDBFile passati
     *
     * @param gridFSFiles gridFSFiles
     * @return l'elenco degli uuid dei GridFSDBFile passati
     */
    private List<String> getFilesId(List<GridFSDBFile> gridFSFiles) throws MongoWrapperException {
        List<String> res = new ArrayList<>();
        gridFSFiles.stream().forEach((f) -> {
            res.add(f.get("_id").toString());
        });
        return res;
    }

    public List<String> getFilesInfo(List<String> uuids, boolean includeDeleted) throws MongoWrapperException {
        List<String> res = new ArrayList<>();
        for (String uuid : uuids) {
            GridFSDBFile f;
            try {
                f = getGridFSFile(uuid, includeDeleted);
            } catch (FileDeletedExceptions ex) {
                throw new MongoWrapperException("this shouldn't happen", ex);
            }
            if (f != null) {
                String deleteDateString = "none";
                if (f.getMetaData() != null && f.getMetaData().get("deleteDate") != null) {
                    DateFormat df1 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    deleteDateString = df1.format(new Date((long) f.getMetaData().get("deleteDate")));
                }
                String deleteDateMillis = "none";
                if (f.getMetaData() != null && f.getMetaData().get("deleteDate") != null) {
                    deleteDateMillis = Long.toString((long) f.getMetaData().get("deleteDate"));
                }
                String oldPath = "none";
                if (f.getMetaData() != null && f.getMetaData().get("oldPath") != null) {
                    oldPath = (String) f.getMetaData().get("oldPath");
                }
                res.add(" -uuid: " + uuid
                        + "\n - filepath: " + f.getFilename()
                        + "\n - deleteDate: " + deleteDateString
                        + "\n - deleteDate: " + deleteDateMillis
                        + "\n - oldPath: " + oldPath
                        + "\n");
            }
        }
        return res;
    }

    /**
     * elimina i file nella directory passata, ma non nelle sottodirectory
     *
     * @param dirname nome della directory
     * @throws it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException
     */
    public void delDirFiles(String dirname) throws MongoWrapperException {
        log.info("deleting dir: " + dirname + "...");
        List<String> dirFiles = getDirFilesNoOverride(dirname);
        for (String file : dirFiles) {
            this.deleteNoOverride(file);
        }
        log.info("deleted dir: " + dirname);
    }

    /**
     * elimina i file nella direcotory passata e in tutte le sottodirectory
     *
     * @param dirname nome della directory
     * @throws it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException
     * MongoWrapperException
     */
    public void delDirFilesAndFolders(String dirname) throws MongoWrapperException {
        //Safety check non facciamo cancella path troppo corti
        if (dirname.split("/").length <= 4) {
            throw new MongoWrapperException("dirname : " + dirname + " too short use delDirFilesAndFoldersNoSafe");
        }
        delDirFilesAndFoldersNoSafe(dirname);
    }

    public void delDirFilesAndFoldersNoSafe(String dirname) throws MongoWrapperException {
        log.info("deleting deep dir: " + dirname + "...");
        List<String> dirFilesAndFolders = getDirFilesAndFoldersNoOverride(dirname);
        if (dirFilesAndFolders != null) {
            for (String file : dirFilesAndFolders) {
                this.deleteNoOverride(file);
            }
        }
        log.info("deleted deep dir: " + dirname);
    }

    /**
     * rinomina il file identificato dall'uuid passato. se il file esiste già
     * viene lanciata una MongoWrapperException
     *
     * @param uuid uuid del file da rinominare
     * @param newName nuovo nome del file
     * @throws it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException se
     * esiste già un file con il nome passato
     */
    public void rename(String uuid, String newName) throws MongoWrapperException {
//        BasicDBObject id = null;
//        ObjectId oid = null;
//        DBObject f;
//        try {
//            oid = new ObjectId(uuid);
//        } catch (Exception e) {
//            id = new BasicDBObject().append("_id", uuid);
//        }
//        DBCollection c = db.getCollection("fs.files");
//        if (oid != null) {
//            f = c.findOne(oid);
//        } else {
//            f = c.findOne(id);
//        }
//        if (existsObjectbyPath(newName)) {
//            throw new MongoWrapperException("esiste già il file: " + newName);
//        }
//        else {
//            BasicDBObject fname = new BasicDBObject().append("$set", new BasicDBObject().append("filename", newName));
//            if (oid != null) {
//                c.update(new BasicDBObject().append("_id", oid), fname, false, false, com.mongodb.WriteConcern.JOURNAL_SAFE);
//            } else {
//                c.update(id, fname, false, false, com.mongodb.WriteConcern.JOURNAL_SAFE);
//            }
//        }

        GridFSDBFile f = null;
        try {
            f = getGridFSFile(uuid);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }
        if (f != null) {
            if (existsObjectByPathNoOverride(newName)) {
                throw new MongoWrapperException("esiste già il file: " + newName);
            }
            newName = getDname(f.get("filename").toString()) + "/" + newName;
            newName = newName.replace("\\", "/");
            f.put("filename", newName);
            f.save();
        }
    }

    /**
     * sposta il file identificato dall'uuid passato nel nuovo filepath
     * passato.Se esiste già un file nello stesso path al nuovo nome sarà
     * agggiunto il suffisso "_1", "_2", ecc.
     *
     * @param uuid uuid del file da spostare
     * @param newFilepath nuovo filepath
     * @throws it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException
     */
    public void move(String uuid, String newFilepath) throws MongoWrapperException {
//        BasicDBObject id = null;
//        ObjectId oid = null;
//        try {
//            oid = new ObjectId(uuid);
//        } catch (Exception e) {
//            id = new BasicDBObject().append("_id", uuid);
//        }
//        DBCollection c = db.getCollection("fs.files");
//
////		newName=getDname(f.get("filename").toString())+"/"+newName;
//        newFilepath = newFilepath.replace("\\", "/");
////            System.out.println(newPath);
//        if (!getFilePathByUuid(uuid).replace("\\", "/").equals(newFilepath)) {
//            int i = 1;
//            String dirName = getDname(newFilepath);
//            String fileName = getFname(newFilepath);
//            while (existsObjectbyPath(newFilepath)) {
//                newFilepath = dirName + '/' + removeExtensionFromFileName(fileName) + "_" + i + "." + getExtensionFromFileName(fileName);
//                i++;
//            }
//
//            BasicDBObject fname = new BasicDBObject().append("$set", new BasicDBObject().append("filename", newFilepath));
//            if (oid != null) {
//                c.update(new BasicDBObject().append("_id", oid), fname, false, false, com.mongodb.WriteConcern.JOURNAL_SAFE);
//            } else {
//                c.update(id, fname, false, false, com.mongodb.WriteConcern.JOURNAL_SAFE);
//            }
//        }

        String currentFilePath = getFilePathByUuid(uuid);
        // se il file esiste
        if (currentFilePath != null) {
            // se il nuovo path è uguale a quello vecchio non faccio niente
            newFilepath = newFilepath.replace("\\", "/");
            if (!currentFilePath.replace("\\", "/").equals(newFilepath)) {

                // calcolo il nuovo path, nel caso esista già
                int i = 1;
                String dirName = getDname(newFilepath);
                String fileName = getFname(newFilepath);
                while (existsObjectByPathNoOverride(newFilepath)) {
                    newFilepath = dirName + '/' + removeExtensionFromFileName(fileName) + "_" + i + "." + getExtensionFromFileName(fileName);
                    i++;
                }

                // cambio il path del file con il nuovo path
                GridFSDBFile f = null;
                try {
                    f = getGridFSFile(uuid);
                } catch (FileDeletedExceptions ex) {
                    //log.warn(ex);
                }
                if (f != null) {
                    f.put("filename", newFilepath);
                    f.save();
                }
            } else {
                log.warn("current path and new path is the same, there's nothing to do");
            }
        }
    }

    /**
     * torna true se esiste un file identificato dal filepath passato, false
     * altrimenti è possibile passare anche un filepath che identifica una
     * directory
     *
     * @param filepath il path del file
     * @return true se esiste un fil filepath passato, false altrimenti
     */
    public Boolean existsObjectbyPath(String filepath) throws MongoWrapperException {
        String originalFilePath = filepath;
        GridFSDBFile f = null;
        try {
            f = getGridFSFileByPath(filepath);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }
        if (f != null) {
            return true;
        }
        if (!filepath.endsWith("/")) {
            filepath += "/";
        }
        BasicDBObject query = new BasicDBObject().append("filename", java.util.regex.Pattern.compile("^" + filepath + "([^/])+$"));
        if (getGridFSFile(query) != null) {
            return true;
        }
        return false;

    }

    public Boolean existsObjectByPathNoOverride(String filepath) throws MongoWrapperException {
        String originalFilePath = filepath;
        GridFSDBFile f = null;
        try {
            f = getGridFSFileByPath(filepath);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }
        if (f != null) {
            return true;
        }
        if (!filepath.endsWith("/")) {
            filepath += "/";
        }
        BasicDBObject query = new BasicDBObject().append("filename", java.util.regex.Pattern.compile("^" + filepath + "([^/])+$"));
        if (getGridFSFile(query) != null) {
            return true;
        }
        return false;

    }

    /**
     * torna true se esiste un file identificato dall'uuid passato, false
     * altrimenti
     *
     * @param uuid uuid del file
     * @return true se esiste un file identificato dall'uuid passato, false
     * altrimenti
     */
    public Boolean existsObjectbyUUID(String uuid) throws MongoWrapperException {
        GridFSDBFile f = null;
        try {
            f = getGridFSFile(uuid);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }
        if (f != null) {
            return true;
        }
        return false;
    }

    /**
     * torna il filename del file identificato dall'uuid passato
     *
     * @param uuid uuid del file
     * @return il filename del file identificato dall'uuid passato
     */
    public String getFileName(String uuid) throws MongoWrapperException {
        GridFSDBFile f = null;
        try {
            f = getGridFSFile(uuid);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }

        if (f != null) {
            return getFname(f.getFilename());
        }
        return null;
    }
//
//    /**
//     * torna il filepath del file identificato dall'uuid passato
//     *
//     * @param uuid
//     * @return il filepath del file identificato dall'uuid passato
//     */
//    public String getFilePathByUuid(String uuid) {
//        try {
//            return getFilePathByUuid(uuid);
//        } catch (FileDeletedExceptions ex) {
//            log.warn(ex);
//        }
//        return null;
//    }

    /**
     * torna il filepath del file identificato dall'uuid passato
     *
     * @param uuid uuid del file
     * @return il filepath del file identificato dall'uuid passato
     */
    public String getFilePathByUuid(String uuid) throws MongoWrapperException {
        GridFSDBFile tmp = null;
        try {
            tmp = getGridFSFile(uuid);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }
        if (tmp != null) {
            return tmp.getFilename();
        }
        return null;
    }

    /**
     * torna la dimensione in bytes del file identificato dal path passato
     *
     * @param filepath il path del file
     * @return la dimensione in bytes del file identificato dal path passato
     */
    public Long getSizeByPath(String filepath) throws MongoWrapperException {
        GridFSDBFile tmp = null;
        try {
            tmp = getGridFSFileByPath(filepath);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }
        if (tmp != null) {
            return tmp.getLength();
        }
        return null;
    }

    /**
     * torna la dimensione in bytes del file identificato dall'uuid passato
     *
     * @param uuid uuid del file
     * @return la dimensione in bytes del file identificato dall'uuid passato
     */
    public Long getSizeByUuid(String uuid) throws MongoWrapperException {
        GridFSDBFile file = null;
        try {
            file = getGridFSFile(uuid);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }
        if (file != null) {
            return file.getLength();
        }
        return null;
    }

    /**
     * Torna la data di upload del file identificato dall'uuid passato
     *
     * @param uuid uuid del file
     * @return la data di upload del file identificato dall'uuid passato
     * @throws it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException
     */
    public Calendar getUploadDateByUuid(String uuid) throws MongoWrapperException {
        GridFSDBFile file = null;
        try {
            file = getGridFSFile(uuid);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }
        if (file != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(file.getUploadDate());
            return cal;
        }
        return null;

    }

    /**
     * Torna la data di upload del file identificato dal path passato
     *
     * @param filePath il path del file
     * @return la data di upload del file identificato dal path passato
     * @throws it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException
     */
    public Calendar getUploadDateByPath(String filePath) throws MongoWrapperException {
        GridFSDBFile file = null;
        try {
            file = getGridFSFileByPath(filePath);
        } catch (FileDeletedExceptions ex) {
            //log.warn(ex);
        }
        if (file != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(file.getUploadDate());
            return cal;
        }
        return null;

    }

    /**
     *
     * @deprecated non serve più
     */
    @Deprecated
    public void close() {
//		m.close();
//                m=null;
    }

    /**
     * mette il campo oldPath nei metadati ai file eliminati logicament eche non
     * ce l'hanno
     */
    public void fixDeleted() throws MongoWrapperException {
        List<String> deleted = getDeleted();
        for (String uuid : deleted) {
            try {
                GridFSDBFile f = getGridFSFile(uuid, true);
                if (f == null) {
                    throw new MongoException("file: " + uuid + " not found");
                }
                DBObject metaData = f.getMetaData();
                if (metaData.containsField("deleteDate")) {
                    if (!metaData.containsField("oldPath")) {
                        metaData.put("oldPath", f.getFilename());
                    }
                    f.put("filename", TRASH_DIR + f.getFilename());
                    f.save();
                } else {
                    log.warn("file not deleted");
                }
            } catch (FileDeletedExceptions ex) {
                log.error("this shouldn't happen");
                throw new MongoException("this shouldn't happen", ex);
            }
        }
    }

    public boolean isTemp() {
        return this.db.collectionExists(IS_TEMP_COLLECTION_NAME);
    }

    public Map<String, Object> getMetadataByUuid(String uuid) throws MongoWrapperException {
        GridFSDBFile gridFSFile = null;
        try {
            gridFSFile = getGridFSFile(uuid);
        } catch (FileDeletedExceptions ex) {
        }
        if (gridFSFile != null) {
            DBObject metadata = gridFSFile.getMetaData();
            return metadata.toMap();
        } else {
            return null;
        }
    }

    public Map<String, Object> getMetadataByPath(String path) throws MongoWrapperException {
        GridFSDBFile gridFSFile = null;
        try {
            gridFSFile = getGridFSFileByPath(path);
        } catch (FileDeletedExceptions ex) {
        }
        if (gridFSFile != null) {
            DBObject metadata = gridFSFile.getMetaData();
            return metadata.toMap();
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        if (m != null) {
            return m.toString();
        }
        return null;
    }

    @Deprecated
    public void unSetErrorMovingMinIO(String uuid) {
        GridFSDBFile findOne = gfs.findOne(new ObjectId(uuid));
        DBObject metaData = findOne.getMetaData();
        metaData.removeField("errorMovingMinIO");
        findOne.save();
    }

    @Deprecated
    public void test2() {
        GridFSDBFile findOne = gfs.findOne(new ObjectId("5a68b996e4b0824256b7ef93"));
//        findOne.put("filename", "/TRASH_DIR/Argo/PECGW_STORE/medicina.legale@pec.aosp.bo.it/SENT/288660_24 Jan 2018 10 45 10 GMT medicina.legale@pec.aosp.bo.it.eml/288660_24 Jan 2018 10 45 10 GMT medicina.legale@pec.aosp.bo.it.eml");
//        DBObject metaData = findOne.getMetaData();
//        metaData.put("deleteDate", 1516899217225l);
//        metaData.put("oldPath", "/Argo/PECGW_STORE/medicina.legale@pec.aosp.bo.it/SENT/288660_24 Jan 2018 10 45 10 GMT medicina.legale@pec.aosp.bo.it.eml/288660_24 Jan 2018 10 45 10 GMT medicina.legale@pec.aosp.bo.it.eml");
//        findOne.setMetaData(metaData);
//        findOne.save();
//{'creator': None, 'category': None, 'deleteDate': 1516899217225, 'oldPath': '/Argo/PECGW_STORE/medicina.legale@pec.aosp.bo.it/SENT/288660_24 Jan 2018 10 45 10 GMT medicina.legale@pec.aosp.bo.it.eml/288660_24 Jan 2018 10 45 10 GMT medicina.legale@pec.aosp.bo.it.eml'}
        System.out.println(findOne);
        System.exit(0);
        DBObject query = QueryBuilder.start("filename")
                .is("/TRASH_DIR/Argo/PECGW_STORE/medicina.legale@pec.aosp.bo.it/SENT/288660_24 Jan 2018 10 45 10 GMT medicina.legale@pec.aosp.bo.it.eml/288660_24 Jan 2018 10 45 10 GMT medicina.legale@pec.aosp.bo.it.eml")
                .get();
//            query = QueryBuilder.start().
//                    or(
//                            QueryBuilder.start("metadata.oldPath").is(filepath).get(),
//                            query
//            ).get();

        List<GridFSDBFile> files = gfs.find(query);
        try {
            //IOUtils.copy(files.get(0).getInputStream(), new FileOutputStream("288660_24 Jan 2018 10 45 10 GMT medicina.legale@pec.aosp.bo.it.eml"));
//            gfs.remove("/TRASH_DIR/Argo/PECGW_STORE/medicina.legale@pec.aosp.bo.it/SENT/288660_24 Jan 2018 10 45 10 GMT medicina.legale@pec.aosp.bo.it.eml/288660_24 Jan 2018 10 45 10 GMT medicina.legale@pec.aosp.bo.it.eml");
            files.get(0).toString();
//        DBCursor find = m.getDB().getCollection("fs").find(new BasicDBObject("md5", "4b6660022dd49e306f50c6735070203d"));
//        DBObject next = find.next();
//        System.out.println(next.toMap().toString());
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(MongoWrapper.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Deprecated
    public ArrayList<String> test(String dirname) {

//            String crDt = "2014-04-28T15:28:58.851Z";
//            DateTimeFormatter parser = ISODateTimeFormat.dateTime();
//            DateTime result;
//            Date newResult;
//            result = parser.parseDateTime(crDt);
//            newResult = result.toDate();
        Date d1 = new Date(2013, 03, 27);
        Date d2 = new Date(2015, 9, 9);
        ArrayList<String> res = new ArrayList<>();
        if (!dirname.endsWith("/")) {
            dirname += "/";
        }
//                Pattern pattern = java.util.regex.Pattern.compile("28-mar-2014 11.54.12");
//		BasicDBObject query= new BasicDBObject().append("uploadDate", pattern);
        BasicDBObject gte = new BasicDBObject("$gte", "ISODate(\"2014-04-28\")");
        BasicDBObject lte = new BasicDBObject("lte", "ISODate(\"2014-04-28\")");

        //dateRange.put("$lte", "ISODate(\"2014-04-28\")");
        BasicDBObject gte_query = new BasicDBObject("uploadDate", gte);
        BasicDBObject lte_query = new BasicDBObject("uploadDate", gte);
//                BasicDBList query = new BasicDBList();
        DBObject query = QueryBuilder.start().and(
                QueryBuilder.start("uploadDate").lessThanEquals(d2).get(),
                QueryBuilder.start("uploadDate").greaterThanEquals(d1).get()
        ).get();
//                query.append("uploadDate1", gte);
        System.out.println(query.toString());

        DBCursor cur = gfs.getFileList(query);
        while (cur.hasNext()) {
            DBObject f = cur.next();
            res.add(f.get("_id").toString());
        }
        //1396004052000
        return res;
    }

}
