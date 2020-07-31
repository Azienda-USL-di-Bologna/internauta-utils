package it.bologna.ausl.mongowrapper.test;


import com.mongodb.MongoException;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import it.bologna.ausl.mongowrapper.MongoWrapperMinIO;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author gdm
 */
public class Tests {
    
    public static void main(String[] args) throws MongoException, MongoWrapperException, IOException {
        Tests t = new Tests();
    }
    
    @Before
    @After
    public void clearAllGarbage() throws MinIOWrapperException, MongoWrapperException, UnknownHostException {
        System.out.println("clear all gatbage...");
        MongoWrapperMinIO wrapper = (MongoWrapperMinIO) MongoWrapperMinIO.getWrapper(true, "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true", "org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi", 105, null);
        List<String> uuids = wrapper.getDirFiles("/" + getClass().getCanonicalName(), true, true);
        if (uuids != null) {
            for (String uuid : uuids) {
                wrapper.erase(uuid);
            }
        }
    }
    
    @Test
    public void testMixedUploadWithOverwrite() throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        MongoWrapperMinIO wrapper = (MongoWrapperMinIO) MongoWrapper.getWrapper(true, "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true", "org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi", 105, null);
        MongoWrapper mongoWrapper = MongoWrapper.getWrapper(false, "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true", null, null, null, null, 105, null);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("test_1.txt");
        String mongoUuid = mongoWrapper.put(is, "test.txt", "/" + getClass().getCanonicalName() + "/path/di/test", null, null, true);
        InputStream mongoIsBefore = wrapper.get(mongoUuid);
        IOUtils.closeQuietly(mongoIsBefore);
        Assert.assertNotNull("il file appena caricato su mongo deve esistere", mongoIsBefore);
        String minIOUUid = wrapper.put(is, "test.txt", "/" + getClass().getCanonicalName() + "/path/di/test", null, null, true);
        InputStream minIOIs = wrapper.get(minIOUUid);
        IOUtils.closeQuietly(minIOIs);
        Assert.assertNotNull("il file appena caricato su minIO deve esistere", minIOIs);
        InputStream mongoIsAfter = wrapper.get(mongoUuid);
        IOUtils.closeQuietly(mongoIsAfter);
        Assert.assertNull("il file appena caricato su mongo non deve più esistere perchè è stato sovrascritto", mongoIsAfter);
    }
    
    @Test
    public void testMixedUploadWithoutOverwrite() throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        MongoWrapper wrapper = MongoWrapper.getWrapper(true, "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true", "org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi", 105, null);
        MongoWrapper mongoWrapper = MongoWrapper.getWrapper(false, "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true", null, null, null, null, 105, null);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("test_1.txt");
        String mongoUuid = mongoWrapper.put(is, "test.txt", "/" + getClass().getCanonicalName() + "/path/di/test", null, null, true);
        InputStream mongoIsBefore = wrapper.get(mongoUuid);
        IOUtils.closeQuietly(mongoIsBefore);
        Assert.assertNotNull("il file appena caricato su mongo deve esistere", mongoIsBefore);
        String minIOUUid = wrapper.put(is, "test.txt", "/" + getClass().getCanonicalName() + "/path/di/test", null, null, false);
        InputStream minIOIs = wrapper.get(minIOUUid);
        IOUtils.closeQuietly(minIOIs);
        Assert.assertNotNull("il file appena caricato su minIO deve esistere", minIOIs);
        String fileName = wrapper.getFileName(minIOUUid);
        Assert.assertTrue("al nome del file deve essere stato aggiunto \"_numero\"", fileName != null && fileName.matches(".*_\\d+\\..*"));
        InputStream mongoIsAfter = wrapper.get(mongoUuid);
        IOUtils.closeQuietly(mongoIsAfter);
        Assert.assertNotNull("il file appena caricato su mongo deve esistere ancora perchè non è stato sovrascritto", mongoIsAfter);
    }
    
    @Test
    public void uploadMinIODownloadDeleteErase() throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        System.out.println("test with upload in minIO");
        MongoWrapper wrapper = MongoWrapper.getWrapper(true, "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true", "org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi", 105, null);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("test_1.txt");
        String uuid = wrapper.put(is, "test_1.txt", getClass().getCanonicalName(), "Tests", "/" + getClass().getCanonicalName() + "/path/di/test", false);
        testDownloadDeleteErase(wrapper, uuid);
    }
    
    @Test
    public void uploadMongoDownloadDeleteErase() throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        System.out.println("test with upload in mongo");
        MongoWrapper wrapper = MongoWrapper.getWrapper(true, "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true", "org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi", 105, null);
        MongoWrapper mongoWrapper = MongoWrapper.getWrapper(false, "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true", null, null, null, null, 105, null);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("test_1.txt");
        String uuidMongo = mongoWrapper.put(is, "test_1.txt", "/" + getClass().getCanonicalName() + "/path/di/test", true);
        testDownloadDeleteErase(wrapper, uuidMongo);
    }
    
    @Test
    public void testGetFilesInPath() throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        System.out.println("test with upload in minIO");
        MongoWrapper mongoWrapper = MongoWrapper.getWrapper(false, "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true", null, null, null, null, 105, null);
        MongoWrapper wrapper = MongoWrapper.getWrapper(true, "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true", "org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi", 105, null);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream isMongo1 = classloader.getResourceAsStream("test_1.txt");
        String uuidMongo1 = mongoWrapper.put(isMongo1, "test_1_Mongo.txt", "/" + getClass().getCanonicalName() + "/path/di/test", true);
        InputStream isMongo2 = classloader.getResourceAsStream("test_2.txt");
        String uuidMongo2 = mongoWrapper.put(isMongo2, "test_2_Mongo.txt", "/" + getClass().getCanonicalName() + "/path/di/test/sotto", true);
        
        InputStream isMinIO1 = classloader.getResourceAsStream("test_1.txt");
        String uuidMinIO1 = wrapper.put(isMinIO1, "test_1_MinIO.txt",  "/" + getClass().getCanonicalName() + "/path/di/test", "Tests", getClass().getCanonicalName(), false);
        InputStream isMinIO2 = classloader.getResourceAsStream("test_2.txt");
        String uuidMinIO2 = wrapper.put(isMinIO2, "test_2_MinIO.txt",  "/" + getClass().getCanonicalName() + "/path/di/test/sotto", "Tests", getClass().getCanonicalName(), false);
     
        Assert.assertNotNull("gli uuid non devono essere nulli", uuidMongo1);
        Assert.assertNotNull("gli uuid non devono essere nulli", uuidMongo2);
        Assert.assertNotNull("gli uuid non devono essere nulli", uuidMinIO1);
        Assert.assertNotNull("gli uuid non devono essere nulli", uuidMinIO2);
        
        List<String> dirFiles = wrapper.getDirFiles("/" + getClass().getCanonicalName() + "/path/di/test");
        Assert.assertTrue("devo trovare 2 elementi", dirFiles != null && dirFiles.size() == 2);
        Assert.assertEquals("devo trovare l'elemento con uuidMongo1", 1, dirFiles.stream().filter(uuid -> uuid.equals(uuidMongo1)).count());
        Assert.assertEquals("devo trovare l'elemento con uuidMinIO1", 1, dirFiles.stream().filter(uuid -> uuid.equals(uuidMinIO1)).count());
        
        List<String> dirFilesAndFolders = wrapper.getDirFilesAndFolders("/" + getClass().getCanonicalName() + "/path/di/test");
        Assert.assertTrue("devo trovare 4 elementi", dirFilesAndFolders != null && dirFilesAndFolders.size() == 4);
        Assert.assertEquals("devo trovare l'elemento con uuidMongo1", 1, dirFilesAndFolders.stream().filter(uuid -> uuid.equals(uuidMongo1)).count());
        Assert.assertEquals("devo trovare l'elemento con uuidMongo2", 1, dirFilesAndFolders.stream().filter(uuid -> uuid.equals(uuidMongo2)).count());
        Assert.assertEquals("devo trovare l'elemento con uuidMinIO1", 1, dirFilesAndFolders.stream().filter(uuid -> uuid.equals(uuidMinIO1)).count());
        Assert.assertEquals("devo trovare l'elemento con uuidMinIO2", 1, dirFilesAndFolders.stream().filter(uuid -> uuid.equals(uuidMinIO2)).count());
        
        wrapper.delDirFilesAndFolders("/" + getClass().getCanonicalName() + "/path/di/test");
        dirFilesAndFolders = wrapper.getDirFilesAndFolders("/" + getClass().getCanonicalName() + "/path/di/test");
        Assert.assertTrue("devo trovare 0 elementi perché gli ho cancellati", dirFilesAndFolders == null || dirFilesAndFolders.isEmpty());
        dirFilesAndFolders = ((MongoWrapperMinIO) wrapper).getDirFiles("/" + getClass().getCanonicalName() + "/path/di/test", true, true);
        Assert.assertTrue("devo trovare 2 elementi perché sto cercando anche nei cancellati (quelli su mongo non me li da comunque)", dirFilesAndFolders != null && dirFilesAndFolders.size() == 2);
        wrapper.erase(uuidMongo1);
        wrapper.erase(uuidMongo2);
        wrapper.erase(uuidMinIO1);
        wrapper.erase(uuidMinIO2);
        dirFilesAndFolders = ((MongoWrapperMinIO) wrapper).getDirFiles("/" + getClass().getCanonicalName() + "/path/di/test", true, true);
        Assert.assertTrue("devo trovare 0 elementi anche cercando nei cancellati perché gli ho eliminati definitivamente", dirFilesAndFolders == null || dirFilesAndFolders.isEmpty());
    }
    
    private void testDownloadDeleteErase(MongoWrapper wrapper, String uuid) throws MongoWrapperException {
        InputStream file = wrapper.get(uuid);
        IOUtils.closeQuietly(file);
        Assert.assertNotNull("il file caricato è nullo", file);
        String filePath = wrapper.getFilePathByUuid(uuid);
        Assert.assertNotNull("devo trovare un path", filePath);
        InputStream fileByPath = wrapper.getByPath(filePath);
        IOUtils.closeQuietly(fileByPath);
        Assert.assertNotNull("devo trovare il file cercando per il suo path", fileByPath);
        String fidByPath = wrapper.getFidByPath(filePath);
        Assert.assertEquals("l'uuid del file carcato per path non è uguale all'uuid restituito in fase di caricamento", fidByPath, uuid);
        wrapper.delete(uuid);
        InputStream getByUuid = wrapper.get(uuid);
        IOUtils.closeQuietly(getByUuid);
        Assert.assertNull("non devo trovare il file cercando per il suo uuid perché è stato eliminato", getByUuid);
        InputStream getByPath = wrapper.getByPath(filePath);
        IOUtils.closeQuietly(getByPath);
        Assert.assertNull("non devo trovare il file cercando per il suo path perché è stato eliminato", getByPath);
        List<String> filesInfo = wrapper.getFilesInfo(Arrays.asList(new String[] {uuid}), true);
        Assert.assertNotNull("devo trovare le informazioni del file cercando anche i cancellati", filesInfo);
        Assert.assertFalse("devo trovare le informazioni del file cercando anche i cancellati", filesInfo.isEmpty());
        wrapper.unDelete(uuid);
        InputStream getByUuidUndelete = wrapper.get(uuid);
        IOUtils.closeQuietly(getByUuidUndelete);
        Assert.assertNotNull("devo trovare il file cercando per il suo uuid perché è stato ripristinato", getByUuidUndelete);
        wrapper.erase(uuid);
        filesInfo = wrapper.getFilesInfo(Arrays.asList(new String[] {uuid}), true);
        Assert.assertTrue("non devo trovare le informazioni del file cercando anche i cancellati perché è stato eliminato definitivamente", filesInfo == null || filesInfo.isEmpty());
    }
}
