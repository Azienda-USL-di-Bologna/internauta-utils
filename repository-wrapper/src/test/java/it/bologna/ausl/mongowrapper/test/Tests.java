package it.bologna.ausl.mongowrapper.test;


import com.mongodb.MongoException;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
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
import org.springframework.core.annotation.Order;

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
    public void uploadMinIODownloadDeleteErase() throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        System.out.println("test with upload in minIO");
        MongoWrapper wrapper = MongoWrapperMinIO.getWrapper(true, "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true", "org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi", 105, null);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("test.txt");
        String uuid = wrapper.put(is, "test.txt", getClass().getCanonicalName(), "Tests", "/" + getClass().getCanonicalName() + "/path/di/test", false);
        testDownloadDeleteErase(wrapper, uuid);
    }
    
    @Test
    public void uploadMongoDownloadDeleteErase() throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        System.out.println("test with upload in mongo");
        MongoWrapper wrapper = MongoWrapperMinIO.getWrapper(true, "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true", "org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi", 105, null);
        MongoWrapper mongoWrapper = MongoWrapperMinIO.getWrapper(false, "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true", null, null, null, null, 105, null);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("test.txt");
        String uuidMongo = mongoWrapper.put(is, "test.txt", "/" + getClass().getCanonicalName() + "/path/di/test", true);
        testDownloadDeleteErase(wrapper, uuidMongo);
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
