package it.bologna.ausl.minio.manager.test;

import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.util.DigestUtils;

/**
 *
 * @author gdm
 */
public class Tests {
    
    private static String minIODBUrl = "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified";
    public static void main(String[] args) throws MinIOWrapperException, IOException, FileNotFoundException, NoSuchAlgorithmException {
        Tests t = new Tests();
        t.uploadFile();
//        t.deleteFile("ce/97/9e/9b/ce979e9b-95d4-4f60-a227-c90791885d8e/test.txt");
//        t.restoreFile("ce/97/9e/9b/ce979e9b-95d4-4f60-a227-c90791885d8e/test.txt");
//        t.testUploadDownloadGetFileInfoAndDeleteByFileId();
//    t.testGetByPathAndFileName();
//        String fileId = "59/5c/bf/51/595cbf51-791e-4679-b143-67784c615a5d/test.txt";
//        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi");
//        minIOWrapper.removeByFileId(fileId, true);

//        t.testRemoveByFileId(minIOWrapper, fileId);
    }
    
    public void uploadFile() throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi");
        Map<String, Object> metadata = null;
        metadata = new HashMap();
        metadata.put("key1", "val1");
        metadata.put("key2", 2);
        boolean overwrite = false;
        MinIOWrapperFileInfo res = upload(minIOWrapper, "/" + getClass().getCanonicalName() + "/path/di/test/", "test.txt", metadata, overwrite);
        System.out.println("res: " + res.toString());
    }
    
    public void deleteFile(String fileId) throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi");
        minIOWrapper.deleteByFileId(fileId);
    }
    
    public void restoreFile(String fileId) throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi");
        minIOWrapper.restoreByFileId(fileId);
    }
    
    @BeforeAll
    @AfterAll
    public static void clearAllGarbage() throws MinIOWrapperException {
        System.out.println("clear all gatbage...");
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi");
        List<MinIOWrapperFileInfo> files = minIOWrapper.getFilesInPath("/" + Tests.class.getCanonicalName(), true, true);
        if (files != null) {
            for (MinIOWrapperFileInfo file : files) {
                minIOWrapper.removeByFileId(file.getFileId(), false);
            }
        }
    }
    
    @Test
    @Order(2)
    public void testUploadDownloadGetFileInfoDeleteAndRemoveByFileId() throws FileNotFoundException, MinIOWrapperException, IOException, NoSuchAlgorithmException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi");
        Map<String, Object> metadata = new HashMap();
        metadata.put("key1", "val1");
        metadata.put("key2", 2);
        boolean overwrite = false;
        MinIOWrapperFileInfo res = upload(minIOWrapper, "/" + getClass().getCanonicalName() + "/path/di/test/", "test.txt", metadata, overwrite);
        Assertions.assertNotNull(res);
        testDownloadByFileId(minIOWrapper, res.getFileId(), res.getMd5());
        testDeleteByFileId(minIOWrapper, res.getFileId(), res.getMd5());
        testRemoveByFileId(minIOWrapper, res.getFileId());
    }
    
    private MinIOWrapperFileInfo upload(MinIOWrapper minIOWrapper, String path, String fileName, Map<String, Object> metadata, boolean overwrite) throws MinIOWrapperException {
//        if (metadata == null) {
//            metadata = new HashMap<>();
//            metadata.put(getClass().getCanonicalName(), "testCases");
//        }
//        path = "/" + getClass().getCanonicalName() + path;
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("test.txt");
        MinIOWrapperFileInfo res = minIOWrapper.put(is, "105t", path, fileName, metadata, overwrite);
        return res;
    }
    
    private  void testDownloadByFileId(MinIOWrapper minIOWrapper, String fileId, String uploadedFileMd5) throws FileNotFoundException, MinIOWrapperException, IOException, NoSuchAlgorithmException {        
        try (
            InputStream is = minIOWrapper.getByFileId(fileId);) {
            String md5 = DigestUtils.md5DigestAsHex(is);
            Assertions.assertEquals(uploadedFileMd5, md5);
        }
    }
    
    private void testDeleteByFileId(MinIOWrapper minIOWrapper, String fileId, String uploadedFileMd5) throws MinIOWrapperException, IOException {
        testGetFileInfoByFileId(minIOWrapper, fileId, uploadedFileMd5);
        minIOWrapper.deleteByFileId(fileId);
        MinIOWrapperFileInfo fileInfo = getFileInfo(minIOWrapper, fileId);
        Assertions.assertNull(fileInfo);
    }

    private MinIOWrapperFileInfo testGetFileInfoByFileId(MinIOWrapper minIOWrapper, String fileId, String uploadedFileMd5) throws FileNotFoundException, MinIOWrapperException, IOException {        
        MinIOWrapperFileInfo fileInfo = getFileInfo(minIOWrapper, fileId);
        Assertions.assertNotNull(fileInfo);
        Assertions.assertEquals(uploadedFileMd5, fileInfo.getMd5());
        return fileInfo;
    }
    
    private MinIOWrapperFileInfo getFileInfo(MinIOWrapper minIOWrapper, String fileId) throws MinIOWrapperException {
        return minIOWrapper.getFileInfoByFileId(fileId);
    }
    
    private void testRemoveByFileId(MinIOWrapper minIOWrapper, String fileId) throws MinIOWrapperException, IOException {
        MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByFileId(fileId, true);
        Assertions.assertNotNull(fileInfo);
        InputStream file = minIOWrapper.directGetFromMinIO(fileInfo.getFileId(), fileInfo.getServerId(), fileInfo.getBucketName());
        Assertions.assertNotNull(file);
        String md5 = DigestUtils.md5DigestAsHex(file);
        Assertions.assertEquals(md5, fileInfo.getMd5());
        minIOWrapper.removeByFileId(fileId, true);
        MinIOWrapperFileInfo fileInfoRemoved = minIOWrapper.getFileInfoByFileId(fileId, true);
        Assertions.assertNull(fileInfoRemoved);
        InputStream fileRemoved = minIOWrapper.directGetFromMinIO(fileInfo.getFileId(), fileInfo.getServerId(), fileInfo.getBucketName());
        Assertions.assertNull(fileRemoved);
    }

    @Test
    @Order(3)
    public  void testGetAndDeleteByPathAndFileName() throws MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi");
        Map<String, Object> metadata = new HashMap();
        metadata.put("key1", "val1");
        metadata.put("key2", 2);
        boolean overwrite = true;
        String path = "/" + getClass().getCanonicalName() + "/path/di/test/";
        String fileName = "test.txt";
        MinIOWrapperFileInfo fileInfoUpload = upload(minIOWrapper, path, fileName, metadata, overwrite);
        Assertions.assertNotNull(fileInfoUpload);
        MinIOWrapperFileInfo fileInfoByPathAndFileName = minIOWrapper.getFileInfoByPathAndFileName(path, fileName, "105t", false);
        Assertions.assertNotNull(fileInfoByPathAndFileName);
        Assertions.assertEquals(fileInfoUpload.getMd5(), fileInfoByPathAndFileName.getMd5());
        Assertions.assertEquals(fileInfoUpload.getBucketName(), fileInfoByPathAndFileName.getBucketName());
        Assertions.assertEquals(fileInfoUpload.getFileId(), fileInfoByPathAndFileName.getFileId());
        InputStream file = minIOWrapper.getByPathAndFileName(path, fileName, "105t");
        String md5 = DigestUtils.md5DigestAsHex(file);
        Assertions.assertEquals(md5, fileInfoUpload.getMd5());
        minIOWrapper.deleteByPathAndFileName(path, fileName, "105t");
        MinIOWrapperFileInfo fileInfoDeleted = minIOWrapper.getFileInfoByPathAndFileName(path, fileName, "105t", true);
        Assertions.assertNotNull(fileInfoDeleted);
        Assertions.assertEquals(fileInfoUpload.getMd5(), fileInfoDeleted.getMd5());
        Assertions.assertEquals(fileInfoUpload.getFileId(), fileInfoDeleted.getFileId());
        testRemoveByFileId(minIOWrapper, fileInfoDeleted.getFileId());
    }
    
    public  void testRestoreByFileId() throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi");
        String fileId = "51/01/c9/91/5101c991-2ff2-4f44-bf0c-cd7a67fb006c/aaaf.pdf";
        minIOWrapper.restoreByFileId(fileId);
    }
    
    @Test
    @Order(4)
    public  void testRenameByFileId() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi");
        boolean overwrite = false;
        String path = "/" + getClass().getCanonicalName() + "/path/di/test";
        String newPath = "/" + getClass().getCanonicalName() + "/newpath/di/test";
        String fileName = "test.txt";
        String newFileName = "new_name.txt";
        MinIOWrapperFileInfo fileInfoUpload = upload(minIOWrapper, path, fileName, null, overwrite);
        minIOWrapper.renameByFileId(fileInfoUpload.getFileId(), newFileName);
        MinIOWrapperFileInfo fileInfoRenamed = minIOWrapper.getFileInfoByFileId(fileInfoUpload.getFileId());
        Assertions.assertEquals(fileInfoRenamed.getFileName(), newFileName);
        minIOWrapper.renameByFileId(fileInfoUpload.getFileId(), newPath, fileName);
        MinIOWrapperFileInfo fileInfoRenamed2 = minIOWrapper.getFileInfoByFileId(fileInfoUpload.getFileId());
        Assertions.assertAll(
                () -> Assertions.assertEquals(fileInfoRenamed2.getFileName(), fileName),
                () -> Assertions.assertEquals(fileInfoRenamed2.getPath(), newPath)
        );
        testDeleteByFileId(minIOWrapper, fileInfoUpload.getFileId(), fileInfoUpload.getMd5());
        testRemoveByFileId(minIOWrapper, fileInfoUpload.getFileId());
    }
    
    @Test
    @Order(5)
    public void testRenameByPathAndFileName() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi");
        boolean overwrite = false;
        String path = "/" + getClass().getCanonicalName() + "/path/di/test";
        String fileName = "test.txt";
        String newPath = "/" + getClass().getCanonicalName() + "/newpath/di/test";
        String newFileName = "new_name.txt";
        MinIOWrapperFileInfo fileInfoUpload = upload(minIOWrapper, path, fileName, null, overwrite);
        minIOWrapper.renameByPathAndFileName(path, fileName, newFileName);
        MinIOWrapperFileInfo fileInfoRenamed = minIOWrapper.getFileInfoByFileId(fileInfoUpload.getFileId());
        Assertions.assertEquals(fileInfoRenamed.getFileName(), newFileName);
        minIOWrapper.renameByPathAndFileName(path, newFileName, newPath, fileName);
        MinIOWrapperFileInfo fileInfoRenamed2 = minIOWrapper.getFileInfoByFileId(fileInfoUpload.getFileId());
        Assertions.assertAll(
                () -> Assertions.assertEquals(fileInfoRenamed2.getFileName(), fileName),
                () -> Assertions.assertEquals(fileInfoRenamed2.getPath(), newPath)
        );
        testDeleteByFileId(minIOWrapper, fileInfoUpload.getFileId(), fileInfoUpload.getMd5());
        testRemoveByFileId(minIOWrapper, fileInfoUpload.getFileId());
    }

    @Test
    @Order(6)
    public void testGetFilesInPath() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi");
        boolean overwrite = false;
        String path1 = "/" + getClass().getCanonicalName() + "/path/di/test";
        String fileName1 = "test1.txt";
        Map<String, Object> metadata1 = new HashMap();
        metadata1.put("key1", "val11");
        metadata1.put("key2", 12);
        String path2 = "/" + getClass().getCanonicalName() + "/path/di";
        String fileName2 = "test2.txt";
        Map<String, Object> metadata2 = new HashMap();
        metadata2.put("key1", "val12");
        metadata2.put("key2", 22);
        String path3 = "/" + getClass().getCanonicalName() + "/path/di/test";
        String fileName3 = "test3.txt";
        Map<String, Object> metadata3 = new HashMap();
        metadata3.put("key1", "val13");
        metadata3.put("key2", 23);
        
        String pathToCheck = "/" + getClass().getCanonicalName() + "/path";
        
        MinIOWrapperFileInfo fileInfoUpload1 = upload(minIOWrapper, path1, fileName1, null, overwrite);
        MinIOWrapperFileInfo fileInfoUpload2 = upload(minIOWrapper, path2, fileName2, null, overwrite);
        MinIOWrapperFileInfo fileInfoUpload3 = upload(minIOWrapper, path3, fileName3, null, overwrite);
        Assertions.assertAll("tutti i file devono essere stati caricati",
                () -> Assertions.assertNotNull(fileInfoUpload1),
                () -> Assertions.assertNotNull(fileInfoUpload2),
                () -> Assertions.assertNotNull(fileInfoUpload3)
        );
        
        List<MinIOWrapperFileInfo> filesInPath = minIOWrapper.getFilesInPath(pathToCheck);
        Assertions.assertAll("deve aver trovato 3 risultati",
                () ->  Assertions.assertNotNull(filesInPath),
                () -> Assertions.assertEquals(3, filesInPath.size())
        );
        MinIOWrapperFileInfo fileInfo1 = filesInPath.stream().filter(f -> f.getFileId().equals(fileInfoUpload1.getFileId())).findFirst().get();
        Assertions.assertAll("il file 1 è stato trovato e i campi sono corretti",
                () -> Assertions.assertNotNull(fileInfo1),
                () -> Assertions.assertEquals(fileInfo1.getPath(), path1),
                () -> Assertions.assertEquals(fileInfo1.getFileName(), fileName1)
        );
        
        MinIOWrapperFileInfo fileInfo2 = filesInPath.stream().filter(f -> f.getFileId().equals(fileInfoUpload2.getFileId())).findFirst().get();
        Assertions.assertAll("il file 2 è stato trovato e i campi sono corretti",
                () -> Assertions.assertNotNull(fileInfo2),
                () -> Assertions.assertEquals(fileInfo2.getPath(), path2),
                () -> Assertions.assertEquals(fileInfo2.getFileName(), fileName2)
        );
        
        MinIOWrapperFileInfo fileInfo3 = filesInPath.stream().filter(f -> f.getFileId().equals(fileInfoUpload3.getFileId())).findFirst().get();
        Assertions.assertAll("il file 3 è stato trovato e i campi sono corretti",
                () -> Assertions.assertNotNull(fileInfo3),
                () -> Assertions.assertEquals(fileInfo3.getPath(), path3),
                () -> Assertions.assertEquals(fileInfo3.getFileName(), fileName3)
        );
        
        testDeleteByFileId(minIOWrapper, fileInfo1.getFileId(), fileInfo1.getMd5());
        testRemoveByFileId(minIOWrapper, fileInfo1.getFileId());
        testDeleteByFileId(minIOWrapper, fileInfo2.getFileId(), fileInfo2.getMd5());
        testRemoveByFileId(minIOWrapper, fileInfo2.getFileId());
        testDeleteByFileId(minIOWrapper, fileInfo3.getFileId(), fileInfo2.getMd5());
        testRemoveByFileId(minIOWrapper, fileInfo3.getFileId());
    }
    
    @Test
    @Order(7)
    public void testGetFilesLessThan() throws MinIOWrapperException, IOException {
        ZonedDateTime nowBefore = ZonedDateTime.now();
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi");
        Map<String, Object> metadata = new HashMap();
        metadata.put("key1", "val1");
        metadata.put("key2", 2);
        boolean overwrite = true;
        String path = "/" + getClass().getCanonicalName() + "/path/di/test/";
        String fileName = "test.txt";
        MinIOWrapperFileInfo fileInfoUpload = upload(minIOWrapper, path, fileName, metadata, overwrite);
        Assertions.assertNotNull(fileInfoUpload);
        List<MinIOWrapperFileInfo> filesLessThanNow = minIOWrapper.getFilesLessThan("105t", ZonedDateTime.now(), false);
        Assertions.assertNotNull(filesLessThanNow);
        Assertions.assertTrue(filesLessThanNow.stream().anyMatch(f -> f.getFileId().equals(fileInfoUpload.getFileId())));
        List<MinIOWrapperFileInfo> filesLessThanHoursAgo = minIOWrapper.getFilesLessThan("105t", nowBefore, false);
        if (filesLessThanHoursAgo != null) {
            Assertions.assertFalse(filesLessThanHoursAgo.stream().anyMatch(f -> f.getFileId().equals(fileInfoUpload.getFileId())));
        }
        testDeleteByFileId(minIOWrapper, fileInfoUpload.getFileId(), fileInfoUpload.getMd5());
        testRemoveByFileId(minIOWrapper, fileInfoUpload.getFileId());
    }
    
}
