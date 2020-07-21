package it.bologna.ausl.minio.manager.test;

import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.DigestUtils;
import org.sql2o.tools.IOUtils;

/**
 *
 * @author gdm
 */
public class Tests {
    
    public static void main(String[] args) throws MinIOWrapperException, IOException, FileNotFoundException, NoSuchAlgorithmException {
        Tests t = new Tests();
        t.testRenameByPathAndFileName();
//        t.testUploadDownloadGetFileInfoAndDeleteByFileId();
//    t.testGetByPathAndFileName();
//        String fileId = "59/5c/bf/51/595cbf51-791e-4679-b143-67784c615a5d/test.txt";
//        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
//        minIOWrapper.removeByFileId(fileId, true);

//        t.testRemoveByFileId(minIOWrapper, fileId);
    }
    
    @Test
    public void testUploadDownloadGetFileInfoDeleteAndRemoveByFileId() throws FileNotFoundException, MinIOWrapperException, IOException, NoSuchAlgorithmException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        Map<String, Object> metadata = new HashMap();
        metadata.put("key1", "val1");
        metadata.put("key2", 2);
        boolean overwrite = true;
        MinIOWrapperFileInfo res = upload(minIOWrapper, "/path/di/test/", "test.txt", metadata, overwrite);
        Assertions.assertNotNull(res);
        testDownloadByFileId(minIOWrapper, res.getFileId(), res.getMd5());
        testDeleteByFileId(minIOWrapper, res.getFileId(), res.getMd5());
        testRemoveByFileId(minIOWrapper, res.getFileId());
    }
    
    private MinIOWrapperFileInfo upload(MinIOWrapper minIOWrapper, String path, String fileName, Map<String, Object> metadata, boolean overwrite) throws MinIOWrapperException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("test.txt");
        MinIOWrapperFileInfo res = minIOWrapper.put(is, 105, path, fileName, metadata, overwrite);
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
    public  void testGetAndDeleteByPathAndFileName() throws MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        Map<String, Object> metadata = new HashMap();
        metadata.put("key1", "val1");
        metadata.put("key2", 2);
        boolean overwrite = true;
        String path = "/path/di/test/";
        String fileName = "test.txt";
        MinIOWrapperFileInfo fileInfoUpload = upload(minIOWrapper, path, fileName, metadata, overwrite);
        Assertions.assertNotNull(fileInfoUpload);
        MinIOWrapperFileInfo fileInfoByPathAndFileName = minIOWrapper.getFileInfoByPathAndFileName(path, fileName, 105, false);
        Assertions.assertNotNull(fileInfoByPathAndFileName);
        Assertions.assertEquals(fileInfoUpload.getMd5(), fileInfoByPathAndFileName.getMd5());
        Assertions.assertEquals(fileInfoUpload.getBucketName(), fileInfoByPathAndFileName.getBucketName());
        Assertions.assertEquals(fileInfoUpload.getFileId(), fileInfoByPathAndFileName.getFileId());
        InputStream file = minIOWrapper.getByPathAndFileName(path, fileName, 105);
        String md5 = DigestUtils.md5DigestAsHex(file);
        Assertions.assertEquals(md5, fileInfoUpload.getMd5());
        minIOWrapper.deleteByPathAndFileName(path, fileName, 105);
        MinIOWrapperFileInfo fileInfoDeleted = minIOWrapper.getFileInfoByPathAndFileName(path, fileName, 105, true);
        Assertions.assertNotNull(fileInfoDeleted);
        Assertions.assertEquals(fileInfoUpload.getMd5(), fileInfoDeleted.getMd5());
        Assertions.assertEquals(fileInfoUpload.getFileId(), fileInfoDeleted.getFileId());
        testRemoveByFileId(minIOWrapper, fileInfoDeleted.getFileId());
    }
    
    public  void testRestoreByFileId() throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String fileId = "51/01/c9/91/5101c991-2ff2-4f44-bf0c-cd7a67fb006c/aaaf.pdf";
        minIOWrapper.restoreByFileId(fileId);
    }
    
    @Test
    public  void testRenameByFileId() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        boolean overwrite = false;
        String path = "/path/di/test";
        String newPath = "/newpath/di/test";
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
    public void testRenameByPathAndFileName() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        boolean overwrite = false;
        String path = "/path/di/test";
        String fileName = "test.txt";
        String newPath = "/newpath/di/test";
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
    
    public  void testRenameByFileId1() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String fileId = "f0/a7/a5/78/f0a7a578-707d-4334-9935-d1ce3bd96896/aaa.pdf";
        
        minIOWrapper.renameByFileId(fileId, "/asfas/asfasf/", "abc.ped");
    }
    
    public  void testRenameByPathAndFileName1() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String path = "/gdm/";
        String fileName = "ffff.pef";
        
        minIOWrapper.renameByPathAndFileName(path, fileName, "/aqaaa/vsdf/gdm/", "ff2ff.pef");
    }

    
    public  void testGetFileInfoByPathAndFileName() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String path = "/gdm/123/hsdfh/";
        String fileName = "aaaf.pdf";
        
        MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByPathAndFileName(path, fileName, 105);
        System.out.println("fileInfo: " + fileInfo.toString());
    }
    
    
    
    public  void testDownloadByPathAndFileName() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String path = "/gdm/";
        String fileName = "aaa.pdf";
        
        try (InputStream res = minIOWrapper.getByPathAndFileName(path, fileName, 105);) {
              File targetFile = new File("D:\\tmp\\testzip\\bbb.pdf");
              java.nio.file.Files.copy(res, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
    

}
