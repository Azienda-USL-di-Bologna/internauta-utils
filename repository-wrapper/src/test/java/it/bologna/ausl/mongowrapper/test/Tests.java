package it.bologna.ausl.mongowrapper.test;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mongodb.MongoException;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import it.bologna.ausl.mongowrapper.MongoWrapperMinIO;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Tests {
    
    private static String minIODBUrl = "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified";
    //private static String minIODBUrl = "jdbc:postgresql://arena.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified";
    private static String mongoUrl = "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true";
    //private static String mongoUrl = "mongodb://argo:siamofreschi@arena/arena?safe=true";
    
    static {
        Logger root = (Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }
    
    public static void main(String[] args) throws MongoException, MongoWrapperException, IOException {
        Tests t = new Tests();
        t.testGetFilesLessThan();
    }
    
    @BeforeAll
    @AfterEach
    public void clearAllGarbage() throws MinIOWrapperException, MongoWrapperException, UnknownHostException {
        System.out.println("clear all gatbage...");
        MongoWrapperMinIO wrapper = (MongoWrapperMinIO) MongoWrapperMinIO.getWrapper(true, mongoUrl, "org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi", "105t", null);
        List<String> uuids = wrapper.getDirFiles("/" + getClass().getCanonicalName(), true, true);
        if (uuids != null) {
            for (String uuid : uuids) {
                wrapper.erase(uuid);
            }
        }
    }
    
    public void testGetFilesLessThan() throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        MongoWrapper wrapper =  MongoWrapperMinIO.getWrapper(true, mongoUrl, "org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi", "105t", null);
//        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
//        InputStream is = classloader.getResourceAsStream("test_1.txt");
//        String mongoUuid = mongoWrapper.put(is, "test.txt", "/" + getClass().getCanonicalName() + "/path/di/test", null, null, true);
//        System.out.println("uploaded mongoUuid: " + mongoUuid);
        
//        DB db = mongoWrapper.getDB();
//        DBCollection files = db.getCollection("fs.files");

        long toEpochMilli = ZonedDateTime.now().toInstant().toEpochMilli()
//        c.add(Calendar.HOUR_OF_DAY, - 1 * intervalHour);
;

//        DBObject filter = new BasicDBObject("uploadDate", new BasicDBObject("$lte", new Date(toEpochMilli)));
//        GridFSDBFile gridFSFile = mongoWrapper.getGridFSFile(mongoUuid);
//        try (DBCursor results = files.find(filter)) {
//            while (results.hasNext()) {
//                DBObject next = results.next();
//                System.out.println("found: " + next.toString());
//            }
//        }
//        System.out.println("found: " + gridFSFile.getUploadDate().toString());
        List<String> filesLessThan = wrapper.getFilesLessThan(ZonedDateTime.now());
        System.out.println("files: ");
        for (String file: filesLessThan) {
            Calendar uploadDateByUuid = wrapper.getUploadDateByUuid(file);
            System.out.println("found: " + uploadDateByUuid.toString());
        }
    }
    
    @Test
    public void testMixedUploadWithOverwrite() throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        MongoWrapperMinIO wrapper = (MongoWrapperMinIO) MongoWrapper.getWrapper(true, mongoUrl, "org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi", "105t", null);
        MongoWrapper mongoWrapper = MongoWrapper.getWrapper(false, mongoUrl, null, null, null, null, "105t", null);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("test_1.txt");
        String mongoUuid = mongoWrapper.put(is, "test.txt", "/" + getClass().getCanonicalName() + "/path/di/test", null, null, true);
        IOUtils.closeQuietly(is);
        InputStream mongoIsBefore = wrapper.get(mongoUuid);
        IOUtils.closeQuietly(mongoIsBefore);
        Assertions.assertNotNull(mongoIsBefore, "il file appena caricato su mongo deve esistere");
        is = classloader.getResourceAsStream("test_1.txt");
        String minIOUUid = wrapper.put(is, "test.txt", "/" + getClass().getCanonicalName() + "/path/di/test", null, null, true);
        IOUtils.closeQuietly(is);
        InputStream minIOIs = wrapper.get(minIOUUid);
        IOUtils.closeQuietly(minIOIs);
        Assertions.assertNotNull(minIOIs, "il file appena caricato su minIO deve esistere");
        InputStream mongoIsAfter = wrapper.get(mongoUuid);
        IOUtils.closeQuietly(mongoIsAfter);
        Assertions.assertNull(mongoIsAfter, "il file appena caricato su mongo non deve più esistere perchè è stato sovrascritto");
    }
    
    @Test
    public void testMixedUploadWithoutOverwrite() throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        MongoWrapper wrapper = MongoWrapper.getWrapper(true, mongoUrl, "org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi", "105t", null);
        MongoWrapper mongoWrapper = MongoWrapper.getWrapper(false, mongoUrl, null, null, null, null, "105t", null);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("test_1.txt");
        String mongoUuid = mongoWrapper.put(is, "test.txt", "/" + getClass().getCanonicalName() + "/path/di/test", null, null, true);
        IOUtils.closeQuietly(is);
        InputStream mongoIsBefore = wrapper.get(mongoUuid);
        IOUtils.closeQuietly(mongoIsBefore);
        Assertions.assertNotNull(mongoIsBefore, "il file appena caricato su mongo deve esistere");
        is = classloader.getResourceAsStream("test_1.txt");
        String minIOUUid = wrapper.put(is, "test.txt", "/" + getClass().getCanonicalName() + "/path/di/test", null, null, false);
        IOUtils.closeQuietly(is);
        InputStream minIOIs = wrapper.get(minIOUUid);
        IOUtils.closeQuietly(minIOIs);
        Assertions.assertNotNull(minIOIs, "il file appena caricato su minIO deve esistere");
        String fileName = wrapper.getFileName(minIOUUid);
        Assertions.assertTrue(fileName != null && fileName.matches(".*_\\d+\\..*"), "al nome del file deve essere stato aggiunto \"_numero\"");
        InputStream mongoIsAfter = wrapper.get(mongoUuid);
        IOUtils.closeQuietly(mongoIsAfter);
        Assertions.assertNotNull(mongoIsAfter, "il file appena caricato su mongo deve esistere ancora perchè non è stato sovrascritto");
    }
    
    @Test
    public void uploadMinIODownloadDeleteErase() throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        System.out.println("test with upload in minIO");
        MongoWrapper wrapper = MongoWrapper.getWrapper(true, mongoUrl, "org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi", "105t", null);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("test_1.txt");
        String uuid = wrapper.put(is, "test_1.txt", getClass().getCanonicalName(), "Tests", "/" + getClass().getCanonicalName() + "/path/di/test", false);
        testDownloadDeleteErase(wrapper, uuid);
    }
    
    @Test
    public void uploadMongoDownloadDeleteErase() throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        System.out.println("test with upload in mongo");
        MongoWrapper wrapper = MongoWrapper.getWrapper(true, mongoUrl, "org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi", "105t", null);
        MongoWrapper mongoWrapper = MongoWrapper.getWrapper(false, mongoUrl, null, null, null, null, "105t", null);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("test_1.txt");
        String uuidMongo = mongoWrapper.put(is, "test_1.txt", "/" + getClass().getCanonicalName() + "/path/di/test", true);
        testDownloadDeleteErase(wrapper, uuidMongo);
    }
    
    @Test
    public void testGetFilesInPath() throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        System.out.println("test with upload in minIO");
        MongoWrapper mongoWrapper = MongoWrapper.getWrapper(false, mongoUrl, null, null, null, null, "105t", null);
        MongoWrapper wrapper = MongoWrapper.getWrapper(true, mongoUrl, "org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi", "105t", null);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream isMongo1 = classloader.getResourceAsStream("test_1.txt");
        String uuidMongo1 = mongoWrapper.put(isMongo1, "test_1_Mongo.txt", "/" + getClass().getCanonicalName() + "/path/di/test", true);
        InputStream isMongo2 = classloader.getResourceAsStream("test_2.txt");
        String uuidMongo2 = mongoWrapper.put(isMongo2, "test_2_Mongo.txt", "/" + getClass().getCanonicalName() + "/path/di/test/sotto", true);
        
        InputStream isMinIO1 = classloader.getResourceAsStream("test_1.txt");
        String uuidMinIO1 = wrapper.put(isMinIO1, "test_1_MinIO.txt",  "/" + getClass().getCanonicalName() + "/path/di/test", "Tests", getClass().getCanonicalName(), false);
        InputStream isMinIO2 = classloader.getResourceAsStream("test_2.txt");
        String uuidMinIO2 = wrapper.put(isMinIO2, "test_2_MinIO.txt",  "/" + getClass().getCanonicalName() + "/path/di/test/sotto", "Tests", getClass().getCanonicalName(), false);
     
        Assertions.assertNotNull(uuidMongo1, "gli uuid non devono essere nulli");
        Assertions.assertNotNull(uuidMongo2, "gli uuid non devono essere nulli");
        Assertions.assertNotNull(uuidMinIO1, "gli uuid non devono essere nulli");
        Assertions.assertNotNull(uuidMinIO2, "gli uuid non devono essere nulli");
        
        List<String> dirFiles = wrapper.getDirFiles("/" + getClass().getCanonicalName() + "/path/di/test");
        Assertions.assertTrue((dirFiles != null && dirFiles.size() == 2), "devo trovare 2 elementi");
        Assertions.assertEquals(1, dirFiles.stream().filter(uuid -> uuid.equals(uuidMongo1)).count(), "devo trovare l'elemento con uuidMongo1");
        Assertions.assertEquals(1, dirFiles.stream().filter(uuid -> uuid.equals(uuidMinIO1)).count(), "devo trovare l'elemento con uuidMinIO1");
        
        List<String> dirFilesAndFolders = wrapper.getDirFilesAndFolders("/" + getClass().getCanonicalName() + "/path/di/test");
        Assertions.assertTrue(dirFilesAndFolders != null && dirFilesAndFolders.size() == 4, "devo trovare 4 elementi");
        Assertions.assertEquals(1, dirFilesAndFolders.stream().filter(uuid -> uuid.equals(uuidMongo1)).count(), "devo trovare l'elemento con uuidMongo1");
        Assertions.assertEquals(1, dirFilesAndFolders.stream().filter(uuid -> uuid.equals(uuidMongo2)).count(), "devo trovare l'elemento con uuidMongo2");
        Assertions.assertEquals(1, dirFilesAndFolders.stream().filter(uuid -> uuid.equals(uuidMinIO1)).count(), "devo trovare l'elemento con uuidMinIO1");
        Assertions.assertEquals(1, dirFilesAndFolders.stream().filter(uuid -> uuid.equals(uuidMinIO2)).count(), "devo trovare l'elemento con uuidMinIO2");
        
        wrapper.delDirFilesAndFolders("/" + getClass().getCanonicalName() + "/path/di/test");
        dirFilesAndFolders = wrapper.getDirFilesAndFolders("/" + getClass().getCanonicalName() + "/path/di/test");
        Assertions.assertTrue(dirFilesAndFolders == null || dirFilesAndFolders.isEmpty(), "devo trovare 0 elementi perché gli ho cancellati");
        dirFilesAndFolders = ((MongoWrapperMinIO) wrapper).getDirFiles("/" + getClass().getCanonicalName() + "/path/di/test", true, true);
        Assertions.assertTrue(dirFilesAndFolders != null && dirFilesAndFolders.size() == 2, "devo trovare 2 elementi perché sto cercando anche nei cancellati (quelli su mongo non me li da comunque)");
        wrapper.erase(uuidMongo1);
        wrapper.erase(uuidMongo2);
        wrapper.erase(uuidMinIO1);
        wrapper.erase(uuidMinIO2);
        dirFilesAndFolders = ((MongoWrapperMinIO) wrapper).getDirFiles("/" + getClass().getCanonicalName() + "/path/di/test", true, true);
        Assertions.assertTrue(dirFilesAndFolders == null || dirFilesAndFolders.isEmpty(), "devo trovare 0 elementi anche cercando nei cancellati perché gli ho eliminati definitivamente");
    }
    
    private void testDownloadDeleteErase(MongoWrapper wrapper, String uuid) throws MongoWrapperException {
        InputStream file = wrapper.get(uuid);
        IOUtils.closeQuietly(file);
        Assertions.assertNotNull(file, "il file caricato è nullo");
        String filePath = wrapper.getFilePathByUuid(uuid);
        Assertions.assertNotNull(filePath, "devo trovare un path");
        InputStream fileByPath = wrapper.getByPath(filePath);
        IOUtils.closeQuietly(fileByPath);
        Assertions.assertNotNull(fileByPath, "devo trovare il file cercando per il suo path");
        String fidByPath = wrapper.getFidByPath(filePath);
        Assertions.assertEquals(fidByPath, uuid, "l'uuid del file carcato per path non è uguale all'uuid restituito in fase di caricamento");
        wrapper.delete(uuid);
        InputStream getByUuid = wrapper.get(uuid);
        IOUtils.closeQuietly(getByUuid);
        Assertions.assertNull(getByUuid, "non devo trovare il file cercando per il suo uuid perché è stato eliminato");
        InputStream getByPath = wrapper.getByPath(filePath);
        IOUtils.closeQuietly(getByPath);
        Assertions.assertNull(getByPath, "non devo trovare il file cercando per il suo path perché è stato eliminato");
        List<String> filesInfo = wrapper.getFilesInfo(Arrays.asList(new String[] {uuid}), true);
        Assertions.assertNotNull(filesInfo, "devo trovare le informazioni del file cercando anche i cancellati");
        Assertions.assertFalse(filesInfo.isEmpty(), "devo trovare le informazioni del file cercando anche i cancellati");
        wrapper.unDelete(uuid);
        InputStream getByUuidUndelete = wrapper.get(uuid);
        IOUtils.closeQuietly(getByUuidUndelete);
        Assertions.assertNotNull(getByUuidUndelete, "devo trovare il file cercando per il suo uuid perché è stato ripristinato");
        wrapper.erase(uuid);
        filesInfo = wrapper.getFilesInfo(Arrays.asList(new String[] {uuid}), true);
        Assertions.assertTrue(filesInfo == null || filesInfo.isEmpty(), "non devo trovare le informazioni del file cercando anche i cancellati perché è stato eliminato definitivamente");
    }
}
