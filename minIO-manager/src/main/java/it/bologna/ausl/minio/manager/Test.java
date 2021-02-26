package it.bologna.ausl.minio.manager;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.MinioException;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.sql2o.tools.IOUtils;

/**
 *
 * @author gdm
 */
public class Test {

    public static void main(String[] args) throws IllegalArgumentException, IOException, ErrorResponseException, InsufficientDataException, FileNotFoundException, MinIOWrapperException, NoSuchAlgorithmException {
//        MessageDigest digest = MessageDigest.getInstance("MD2");
//        byte[] hashbytes1 = digest.digest("ciao gdm1".getBytes(StandardCharsets.UTF_8));
//        System.out.println(new BigInteger(1, hashbytes1));
//        byte[] hashbytes2 = digest.digest("ciao gdm1".getBytes(StandardCharsets.UTF_8));
//        System.out.println(new BigInteger(1, hashbytes2));
//        byte[] hashbytes3 = digest.digest("ciao gdm2".getBytes(StandardCharsets.UTF_8));
//        System.out.println(new BigInteger(1, hashbytes3));
//        BigInteger bigInteger1 = new BigInteger(1, DigestUtils.md5Digest("ciao gdm1".getBytes()));
//        BigInteger bigInteger2 = new BigInteger(1, DigestUtils.md5Digest("ciao gdm1".getBytes()));
//        BigInteger bigInteger3 = new BigInteger(1, DigestUtils.md5Digest("ciao gdm2".getBytes()));
//        System.out.println(bigInteger1);
//        System.out.println(bigInteger2);
//        System.out.println(bigInteger3);
//        System.out.println("ciao gdm1".hashCode());
//        System.out.println("ciao gdm1".hashCode());
//        System.out.println("ciao gdm2".hashCode());
//        System.out.println("ciao gdm2".hashCode());
//        System.out.println("gdm ciao".hashCode());
//        genericTest();
        testUpload();
//        testDelFilesInPath();
//            testGetDeleted();
//        testDownloadByFileId();
//        testDownloadByPathAndFileName();
//        testGetFileInfoByFileId();
//        testGetFileInfoByPathAndFileName();
//        testRenameByFileId1();
//        testRenameByPathAndFileName1();
//        testDeleteByFileId();
//        testDeleteByPathAndFileName();
//        testRestoreByFileId();
//        testGetFilesInPath();
//        testDirectRemoveFromMinIO();
    }
    
    public static void genericTest() throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
//        minIOWrapper.restoreByFileId("2d/ab/5d/8c/2dab5d8c-cd83-4b67-aabf-a5780d9bafbc/aaaf_68.pdf");
//        minIOWrapper.restoreByFileId("b9/61/6f/c9/b9616fc9-3e9d-48b3-99ae-8cf4599c4b7d/aaaf_69.pdf");
        //5b8937cc-e4b0-70b0-8419-9fb8
        //d2857b0e-4122-4354-bd6b-4bac7e0c83c0
        System.out.println(UUID.randomUUID().toString());
    }
    
    private static <T extends Object> Stream<T> collectionToStreamNullSafe(Collection<T> collection) {
    return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
    }
    
    public static void testGetDeleted() throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        List<MinIOWrapperFileInfo> deleted = minIOWrapper.getDeleted("105", ZonedDateTime.parse("2020-07-28T10:15:30+01:00[Europe/Rome]"));
        List<String> collect = collectionToStreamNullSafe(deleted).map(file -> file.getFileId()).collect(Collectors.toList());
        //List<String> collect = deleted.stream().map(file -> file.getFileId()).collect(Collectors.toList());
        for (String string : collect) {
            System.out.println(string);
        }
//        for (MinIOWrapperFileInfo file : deleted) {
//            System.out.println(file);
//        }
    }
    
    
    public static void testDelFilesInPath() throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String path = "/gdm";
        minIOWrapper.delFilesInPath(path, true);
    }
    
    public static void testDirectRemoveFromMinIO() throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String fileId = "38/31/61/12/38316112-5c0e-4790-872b-6bc652a13911/test.txt";
        minIOWrapper.remove(fileId, "trash", 1);
    }
    
    public static void testGetFilesInPath() throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String path = "/gdm";
//        String path = "/gdm/fshdfsh";
        List<MinIOWrapperFileInfo> filesInPath = minIOWrapper.getFilesInPath(path, true, true);
        System.out.println("filesInPath:");
        if (filesInPath != null) {
            filesInPath.stream().forEach(f -> {System.out.println(f.toString());});
        }
    }
    
    public static void testRestoreByFileId() throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String fileId = "51/01/c9/91/5101c991-2ff2-4f44-bf0c-cd7a67fb006c/aaaf.pdf";
        minIOWrapper.restoreByFileId(fileId);
    }
    
    public static void testDeleteByFileId() throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String fileId = "51/01/c9/91/5101c991-2ff2-4f44-bf0c-cd7a67fb006c/aaaf.pdf";
        minIOWrapper.deleteByFileId(fileId);
    }
    
    public static void testDeleteByPathAndFileName() throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
         String path = "/gdm/fshdfsh/hsdfh";
        String fileName = "aaaf.pdf";
        minIOWrapper.deleteByPathAndFileName(path, fileName, "105");
    }
    
    public static void testRenameByFileId() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String fileId = "f0/a7/a5/78/f0a7a578-707d-4334-9935-d1ce3bd96896/aaa.pdf";
        
        minIOWrapper.renameByFileId(fileId, "a2bc.ped");
    }
    
    public static void testRenameByFileId1() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String fileId = "f0/a7/a5/78/f0a7a578-707d-4334-9935-d1ce3bd96896/aaa.pdf";
        
        minIOWrapper.renameByFileId(fileId, "/asfas/asfasf/", "abc.ped");
    }
    
    public static void testRenameByPathAndFileName() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String path = "/gdm/";
        String fileName = "aaa_10.pdf";
        
        minIOWrapper.renameByPathAndFileName(path, fileName, "ffff.pef");
    }
    
    public static void testRenameByPathAndFileName1() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String path = "/gdm/";
        String fileName = "ffff.pef";
        
        minIOWrapper.renameByPathAndFileName(path, fileName, "/aqaaa/vsdf/gdm/", "ff2ff.pef");
    }
    
    public static void testGetFileInfoByFileId() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
//        String fileId = "b5/3e/32/76/b53e3276-0c0d-497f-a747-565676616763/aaaf.pdf";
        String fileId = "51/01/c9/91/5101c991-2ff2-4f44-bf0c-cd7a67fb006c/aaaf.pdf";
        
        MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByFileId(fileId, true);
        if (fileInfo != null)
            System.out.println("fileInfo: " + fileInfo.toString());
    }
    
    public static void testGetFileInfoByPathAndFileName() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String path = "/gdm/123/hsdfh/";
        String fileName = "aaaf.pdf";
        
        MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByPathAndFileName(path, fileName, "105");
        System.out.println("fileInfo: " + fileInfo.toString());
    }
    
    public static void testDownloadByFileId() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String fileId = "f0/a7/a5/78/f0a7a578-707d-4334-9935-d1ce3bd96896/aaa.pdf";
        
        try (InputStream res = minIOWrapper.getByFileId(fileId);) {
              File targetFile = new File("D:\\tmp\\testzip\\bbb.pdf");
              java.nio.file.Files.copy(res, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    public static void testDownloadByPathAndFileName() throws FileNotFoundException, MinIOWrapperException, IOException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        String path = "/gdm/";
        String fileName = "aaa.pdf";
        
        try (InputStream res = minIOWrapper.getByPathAndFileName(path, fileName, "105");) {
              File targetFile = new File("D:\\tmp\\testzip\\bbb.pdf");
              java.nio.file.Files.copy(res, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    public static void testUpload() throws FileNotFoundException, MinIOWrapperException {
        MinIOWrapper minIOWrapper = new MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi");
        Map<String, Object> metadata = new HashMap();
        metadata.put("key1", "val1");
        metadata.put("key2", 4);
        boolean overwrite = true;
        MinIOWrapperFileInfo res = minIOWrapper.put(new FileInputStream("D:\\tmp\\tmp\\mail_grossa.eml"), "105", "/gdm/123/1/", "aaaf.pdf", metadata, overwrite, "1");
        System.out.println("res: " + res.toString());
    }
    
    public static void testUploadStatico() {
        try {
            // Create a minioClient with the MinIO Server name, Port, Access key and Secret key.
            MinioClient minioClient = MinioClient.builder().endpoint("https://babelcloud.avec.emr.it:9001/").credentials("YMnEA5dvG5NAt5GAow4e", "iFqo2ZVkVJhlEfXMWOIEg5TBTLUzKs9k6peqILGl").build();

            // Check if the bucket already exists.
            boolean isExist
                    = minioClient.bucketExists(BucketExistsArgs.builder().bucket("105").build());
            if (isExist) {
                System.out.println("Bucket already exists.");
            }

            try {                
                ObjectStat statObject = minioClient.statObject(StatObjectArgs.builder().bucket("105").object("caretella27/cartella34/aaaa.pdf").build());
                System.out.println("statObject: " + statObject.toString());
            } catch (ErrorResponseException e) {
                e.printStackTrace();
            }
            // Upload the zip file to the bucket with putObject
            //ObjectWriteResponse putObject = minioClient.putObject(PutObjectArgs.builder().bucket("105").object("caretella27/cartella4/aaaa.pdf").stream(new FileInputStream("d:/tmp/pdf.pdf"), -1, 10485760).build());
            System.out.println("successfully uploaded");
        } catch (Exception e) {
            System.out.println("Error occurred: " + e);
        }
    }

}
