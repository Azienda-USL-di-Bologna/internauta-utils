/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.QueryBuilder;
import com.mongodb.gridfs.GridFSDBFile;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;

/**
 *
 * @author user
 */
public class TestClass {

    public static void main(String[] args) throws UnknownHostException, MongoException, MongoWrapperException, IOException, MinIOWrapperException {

        String	minIODBDriver= "org.postgresql.Driver";
//	String minIODBUrl = "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified";
	String minIODBUrl = "jdbc:postgresql://babel-big-auslbo.avec.emr.it:5432/minirepo?stringtype=unspecified";
	String minIODBUsername= "minirepo";
//	String minIODBPassword ="siamofreschi";
	String minIODBPassword ="4fHkXbSEA6D3";
        
        String path = "/Procton/Documenti/2020-65400";
//        String path = "/Relate";

//       MongoWrapper m = new MongoWrapper("mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true");
//       MongoWrapper m = new MongoWrapper("mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/downloadgdml?safe=true");
//        MongoWrapper m = new MongoWrapper("mongodb://argo:siamocaldi@babelmongo1,babelmongo2/prod?safe=true&replicaSet=prod0");
//         MongoWrapper m = new MongoWrapper("mongodb://argo102:e37jTcIeTp0w@babel102mongo.avec.emr.it/doc102?safe=true");
//        MongoWrapper m = new MongoWrapper("mongodb://argo106:Ushaez4ajei2@babel106mongo1.avec.emr.it,babel106mongo2.avec.emr.it/doc106?safe=true&replicaSet=avec0");
//        MongoWrapper m = new MongoWrapper("mongodb://argo109:xJSE3Sxvy7Dr@babel109mongo1.avec.emr.it,babel109mongo2.avec.emr.it/doc109?safe=true&replicaSet=avec0");
//        MongoWrapper m = new MongoWrapper("mongodb://argo908:mPw8DApKLaPR@babel908mongo1.avec.emr.it,babel908mongo2.avec.emr.it/doc908?safe=true&replicaSet=avec0");
//        MongoWrapper m = new MongoWrapper("mongodb://argo908:mPw8DApKLaPR@babel908mongo1.avec.emr.it/doc908?safe=true");
//          MongoWrapper m = new MongoWrapper("mongodb://argo909:BIKJBnwosLs7@babel909mongo1.avec.emr.it/doc909?safe=true");
//          String mongoUri = "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true"; // gdml
//          String mongoUri = "mongodb://argo960:Fj0pdiENBdNU@babel960mongo1.avec.emr.it,babel960mongo2.avec.emr.it/doc960?safe=true&replicaSet=avec0";
//          String mongoUri = "mongodb://argo106:Ushaez4ajei2@babel106mongo1.avec.emr.it,babel106mongo2.avec.emr.it/doc106?safe=true&replicaSet=avec0";
//          String mongoUri = "mongodb://argo102:e37jTcIeTp0w@babel102mongo.avec.emr.it/doc102?safe=true";
//           MongoWrapper m = MongoWrapper.getWrapper(true, mongoUri, minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, "105", null);
//          IOUtils.copy(m.get("5825d792e4b0527a032cbbbc"), new FileOutputStream("aaaaa.eml"));
//        System.out.println(m.getFileName("5a68b996e4b0824256b7ef93"));
        MinIOWrapper m = new MinIOWrapper(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword);
//        System.out.println(m.getByFileId("43/5a8/44d/435a844d-405a-4f0d-8341-15462b7825dc/5916446_18 May 2020 07 03 36 GMT UTF-8BQeKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjA UTF-8B4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCMcnXigIzigIzigIzigIw UTF-8B4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM UTF-8B4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCMYuKAjOKAjOKAjA UTF-8B4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM UTF-8B4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCMYS5pdA UTF-8BIOKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjA UTF-8B4oCM4oCM4oCM4oCMUKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjOKAjA UTF-8B4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCMLlA UTF-8B4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM UTF-8B4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM UTF-8B4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM4oCM UTF-8B4oCM4oCM4oCMLkE fatturazione@enom.com.eml"));
//        IOUtils.copy(m.getByFileId("8e/e24/367/8ee24367-27bf-4cde-aabb-5c43dcbde458/269081_11 Jul 2017 12 46 42 GMT protocollo@pec.ospfe.it.eml"), 
//                new FileOutputStream("269081_11 Jul 2017 12 46 42 GMT protocollo@pec.ospfe.it.eml"));
        m.removeByFileId("7f/db1/fdb/7fdb1fdb-17bf-4fa5-88fa-0445ce0d4944/200431_13 Apr 2018 10 06 55 GMT utf-8BUkEgLSBVLk8uIFNBTklUQSBBTklNQUxFLCBJR0lFTkUgREVHTEkgQQutf-8BTElNRU5USSBESSBPUklHSU5FIEFOSU1BTEUsIElHSUVORSBERUdMSQutf-8BIEFMTEVWQU1FTlRJIEUgREVMTEUgUFJPRFVaSU9OSSBaT09URUNOSQutf-8BQ0hF vet.ra.dsp@pec.auslromagna.it.eml", false);
        m.removeByFileId("31/bfb/469/31bfb469-60fe-4322-904b-2bd11a6e0638/294480_12 Sep 2018 07 35 52 GMT utf-8BUkEgLSBVLk8uIFNBTklUQSBBTklNQUxFLCBJR0lFTkUgREVHTEkgQQutf-8BTElNRU5USSBESSBPUklHSU5FIEFOSU1BTEUsIElHSUVORSBERUdMSQutf-8BIEFMTEVWQU1FTlRJIEUgREVMTEUgUFJPRFVaSU9OSSBaT09URUNOSQutf-8BQ0hF vet.ra.dsp@pec.auslromagna.it.eml", false);
        m.removeByFileId("fa/11e/903/fa11e903-e0b1-44fa-b43a-a40b3444c734/326728_22 Nov 2018 11 09 24 GMT utf-8BUk4gLSBVLk8uIFNBTklUQScgQU5JTUFMRSwgSUdJRU5FIERFR0xJIAutf-8BQUxJTUVOVEkgREkgT1JJR0lORSBBTklNQUxFLCBJR0lFTkUgREVHTAutf-8BSSBBTExFVkFNRU5USSBFIERFTExFIFBST0RVWklPTkkgWk9PVEVDTgutf-8BSUNIRQ vet.rn.dsp@pec.auslromagna.it.eml", false);
        m.removeByFileId("63/9d9/1e5/639d91e5-9d1d-4945-a634-47fa6cd384b6/4937722_17 Feb 2020 08 31 06 GMT utf-8BUk4gLSBVLk8uIFNBTklUQScgQU5JTUFMRSwgSUdJRU5FIERFutf-8BR0xJIEFMSU1FTlRJIERJIE9SSUdJTkUgQU5JTUFMRSwgSUdJutf-8BRU5FIERFR0xJIEFMTEVWQU1FTlRJIEUgREVMTEUgUFJPRFVautf-8BSU9OSSBaT09URUNOSUNIRQ vet.rn.dsp@pec.auslromagna.it.eml", false);
        m.removeByFileId("82/362/28b/8236228b-057e-47cf-8dcb-fa8489eff246/9677686_02 Mar 2021 10 43 15 GMT utf-8BUk4gLSBVLk8uIFNBTklUQScgQU5JTUFMRSwgSUdJRU5FIERFutf-8BR0xJIEFMSU1FTlRJIERJIE9SSUdJTkUgQU5JTUFMRSwgSUdJutf-8BRU5FIERFR0xJIEFMTEVWQU1FTlRJIEUgREVMTEUgUFJPRFVautf-8BSU9OSSBaT09URUNOSUNIRQ vet.rn.dsp@pec.auslromagna.it.eml", false);
        System.exit(0);
//        Arrays.asList("5e1a6f5fd5de52bfe69b56f4", "5e1a6e08d5de52bfe69b56ee", "5e1a6944d5de52bfe69b56c4", "5e17115ad5de52bfe69a9f7b", "5e1a6fc2d5de52bfe69b56fa")
//                .stream().forEach(s -> {
//                    MinIOWrapperFileInfo fileInfoByUuid = m.getFileInfoByUuid(s);
//                    System.out.println(fileInfoByUuid.getFileId());
////                    m.deleteByFileUuid(s);
////                    m.removeByFileId(fileInfoByUuid.getFileId(), false);
//        });
//        String res = m.put(new File("288660_24 Jan 2018 10 45 10 GMT medicina.legale@pec.aosp.bo.it.eml"), "5a68b996e4b0824256b7ef93", "288660_24 Jan 2018 10 45 10 GMT medicina.legale@pec.aosp.bo.it.eml", "/TRASH_DIR/Argo/PECGW_STORE/medicina.legale@pec.aosp.bo.it/SENT/288660_24 Jan 2018 10 45 10 GMT medicina.legale@pec.aosp.bo.it.eml/288660_24 Jan 2018 10 45 10 GMT medicina.legale@pec.aosp.bo.it.eml", true);
//        System.out.println("res: " + res);
        m.rename("603e16eed5ded924bc8527db", "9677686_02 Mar 2021 10 43 15 GMT utf-8-vet.rn.dsp@pec.auslromagna.it.eml");
        m.rename("5e4a414fd5de86d3e192ec22", "4937722_17 Feb 2020 08 31 06 GMT utf-8-vet.rn.dsp@pec.auslromagna.it.eml");
        m.rename("5b98c323e4b0af21111bd36d", "294480_12 Sep 2018 07 35 52 GMT utf-8-vet.ra.dsp@pec.auslromagna.it.eml");
        m.rename("5bf68fd0e4b01b1741bb367c", "326728_22 Nov 2018 11 09 24 GMT utf-8-vet.rn.dsp@pec.auslromagna.it.eml");
        m.rename("5ad08157e4b0c6635daf3420", "200431_13 Apr 2018 10 06 55 GMT utf-8-vet.ra.dsp@pec.auslromagna.it.eml");
//        m.rename("5ed38607d5deb0d7e28e53c3", "5982041_31 May 2020 12 23 57 GMT UTF-8-fatturazione@enom.com.eml");
//        m.rename("5ec776ecd5dee4006891651f", "5945865_22 May 2020 08 52 20 GMT UTF-8-securestarr@enom.com.eml");
//        m.rename("5ec39798d5de1e125f0ed459", "5923395_19 May 2020 10 18 27 GMT UTF-8-fatturazione@enom.com.eml");
//        m.rename("5ec21769d5de0fb3c89ab68c", "5916446_18 May 2020 07 03 36 GMT UTF-8-fatturazione@enom.com.eml");
//        m.rename("5e1a6944d5de52bfe69b56c4", "4148657_12 Jan 2020 01 20 26 GMT UTF-servizi-fattutrazione@enom.com.eml");
//        m.rename("5e17115ad5de52bfe69a9f7b", "4142745_09 Jan 2020 12 36 38 GMT UTF-nnnn@enom.com.eml");
//        m.rename("5e1a6fc2d5de52bfe69b56fa", "4148657_12 Jan 2020 01 20 26 GMT UTF-servizi-fattutrazione@enom.com_3.eml");
        System.exit(0);
//       MongoWrapper m = new MongoWrapper("mongodb://argo:siamocaldi@babelmongo1,babelmongo2/prod?safe=true&replicaSet=prod0");
//        m.getDirFiles(path).stream().forEach(uuid -> {System.out.println(uuid + ": " + m.getFileName(uuid));});
        MinIOWrapper minIOWrapper = new MinIOWrapper(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword);
    String toUndelete = "42/d1/f3/18/42d1f318-f4a4-4603-b460-f572e6295b01/2021-80689_Allegato_orig1.xlsx";
    //minIOWrapper.renameByFileId(toUndelete, "PG0029996_2021_Allegato1.pdf", true);
    minIOWrapper.restoreByFileId(toUndelete);
//    for (String uuid : toUndelete.split(",")) {
//        m.unDelete(uuid);
//           String fileName = m.getFileName(uuid);
//           System.out.println("filename: " + fileName);
//    }
//        System.out.println("aaaa: " + fileName);
        System.exit(0);
        //upload frontespizio
//        String uuid_frontespizio = m.put(new File("C:\\Users\\Giuseppe Russo\\Desktop\\deli_proposta_21\\DELI0000009_2020_frontespizio.pdf"), "DELI0000009_2020_frontespizio.pdf", path, true);
//        System.out.println("uuid_frontespizio: " + uuid_frontespizio);
//        
//        // upload stampaunica
//        String uuid_delibera_stampaunica = m.put(new File("C:\\Users\\Giuseppe Russo\\Desktop\\deli_proposta_21\\DELI0000009_2020_stampaunica.pdf"), "DELI0000009_2020_stampaunica.pdf", path, true);
//        System.out.println("uuid_delibera_stampaunica: " + uuid_delibera_stampaunica);
//        
//        m.getDirFiles(path).stream().forEach(uuid -> {
//            try {
//                System.out.println(uuid + ": " + m.getFileName(uuid));
//            } catch (MongoWrapperException ex) {
//                Logger.getLogger(TestClass.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        });

//        System.out.println(m.getFidByPath("/Deli/Documenti/2016-49/delibera_2016-49_firmata.pdf"));
//        String uuid = "5e3acbabd5dec0c1d511f52a";
//        System.out.println(uuid + ": " + m.getFileName(uuid));
//        System.out.println(uuid + ": " + m.getFilePathByUuid(uuid));
        // mongodb://argo:siamocaldi@babelmongo1,babelmongo2/prod?safe=true&replicaSet=prod0
        // mongodb://argo:siamofreschi@toshiro,procton3/doc?safe=true&replicaSet=rs0
//        m.rename("5c5c1942e4b0cd7d3eba6761", "allegato_senza_nome.eml");
//        m.getDirFiles("/tmp").stream().forEach(uuid -> {
//            System.out.println(uuid + ": " + m.getFileName(uuid));
//                        });
//        InputStream file = m.get(uuid);
//        InputStream file2 = m.get("5e43fb6bd5de4086ed3c4de7");
        File targetFile = new File("C:\\Users\\Top\\Desktop\\BabelCare\\AuslBO105\\PG0058376_2020_Pec id_5996483_testo_21.pdf");
        File targetFile2 = new File("C:\\Users\\Top\\Desktop\\BabelCare\\AuslBO105\\PG0058376_2020_Pec id_5998135_testo_22.pdf");
//        File targetFile3 = new File("C:\\Users\\Giuseppe Russo\\Desktop\\deli_42\\DELI0000042_2020_frontespizio.pdf");
//        File targetFile4 = new File("C:\\Users\\Giuseppe Russo\\Desktop\\deli_42\\DELI0000042_2020_stampaunica.pdf");
////        
        System.out.println(m.put(targetFile, "PG0058376_2020_Pec id_5996483_testo_21.pdf", path, true));
        System.out.println(m.put(targetFile2, "PG0058376_2020_Pec id_5998135_testo_22.pdf", path, true));
//        System.out.println(m.put(targetFile3, "DELI0000042_2020_Frontespizio.pdf", path, true));
//        System.out.println(m.put(targetFile4, "DELI0000042_2020_Stampa_unica.pdf", path, true));
        m.getDirFiles(path).stream().forEach(uuid -> {
            try {
                System.out.println(uuid + ": " + m.getFileName(uuid) + " size: " + m.getSizeByUuid(uuid));
            } catch (MongoWrapperException ex) {
                Logger.getLogger(TestClass.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
//        System.out.println(m.put(targetFile, "eml_di_prod.eml", "/tmp", true));
//        System.out.println(m.put(targetFile2, "eml_di_prod.eml", "/tmp", true));
//        java.nio.file.Files.copy(file2, targetFile.toPath(), 
//            StandardCopyOption.REPLACE_EXISTING);
//        
//        java.nio.file.Files.copy(file, targetFile.toPath(), 
//            StandardCopyOption.REPLACE_EXISTING);
// 
//        IOUtils.closeQuietly(file);
    }

}
