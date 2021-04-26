/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.mongodb.MongoException;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;

/**
 *
 * @author user
 */
public class TestClass {

    public static void main(String[] args) throws UnknownHostException, MongoException, MongoWrapperException, IOException {

        String	minIODBDriver= "org.postgresql.Driver";
	String minIODBUrl = "jdbc:postgresql://babel-big-auslbo.avec.emr.it:5432/minirepo?stringtype=unspecified";
	String minIODBUsername= "minirepo";
	String minIODBPassword ="4fHkXbSEA6D3";
        
        String path = "/Procton/Documenti/2020-65400";
//        String path = "/Relate";

//       MongoWrapper m = new MongoWrapper("mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true");
//       MongoWrapper m = new MongoWrapper("mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/downloadgdml?safe=true");
//        MongoWrapper m = new MongoWrapper("mongodb://argo:siamocaldi@babelmongo1,babelmongo2/prod?safe=true&replicaSet=prod0");
        // MongoWrapper m = new MongoWrapper("mongodb://argo102:e37jTcIeTp0w@babel102mongo.avec.emr.it/doc102?safe=true");
//        MongoWrapper m = new MongoWrapper("mongodb://argo106:Ushaez4ajei2@babel106mongo1.avec.emr.it,babel106mongo2.avec.emr.it/doc106?safe=true&replicaSet=avec0");
//        MongoWrapper m = new MongoWrapper("mongodb://argo109:xJSE3Sxvy7Dr@babel109mongo1.avec.emr.it,babel109mongo2.avec.emr.it/doc109?safe=true&replicaSet=avec0");
//        MongoWrapper m = new MongoWrapper("mongodb://argo908:mPw8DApKLaPR@babel908mongo1.avec.emr.it,babel908mongo2.avec.emr.it/doc908?safe=true&replicaSet=avec0");
//        MongoWrapper m = new MongoWrapper("mongodb://argo908:mPw8DApKLaPR@babel908mongo1.avec.emr.it/doc908?safe=true");
        //  MongoWrapper m = new MongoWrapper("mongodb://argo909:BIKJBnwosLs7@babel909mongo.avec.emr.it/doc909?safe=true");
//          String mongoUri = "mongodb://argo960:Fj0pdiENBdNU@babel960mongo1.avec.emr.it,babel960mongo2.avec.emr.it/doc960?safe=true&replicaSet=avec0";
          String mongoUri = "mongodb://argo106:Ushaez4ajei2@babel106mongo1.avec.emr.it,babel106mongo2.avec.emr.it/doc106?safe=true&replicaSet=avec0";
           MongoWrapper m = MongoWrapper.getWrapper(true, mongoUri, minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, "106", null);
//       MongoWrapper m = new MongoWrapper("mongodb://argo:siamocaldi@babelmongo1,babelmongo2/prod?safe=true&replicaSet=prod0");
//        m.getDirFiles(path).stream().forEach(uuid -> {System.out.println(uuid + ": " + m.getFileName(uuid));});


    String toUndelete = "aecebc45-167d-453b-aee0-c94da0fe12a4,6a3b85e4-16bf-467d-a006-92f92aedb073,a9b72503-6104-40d2-9bc4-78d8a4344cdb,724e66f6-eafa-434b-876e-17a4ce5ecee2,6efec02f-8e61-430f-9811-24d261d526b6,bdb15418-609b-4b20-8c83-f4557a15cb9e,09300e1c-db18-44a4-9bec-49ff780a0fe2,865ff177-ace4-4b89-9114-e86fef915819,e863bc89-c444-4deb-9933-ad75a2204942";
    for (String uuid : toUndelete.split(",")) {
        m.unDelete(uuid);
           String fileName = m.getFileName(uuid);
           System.out.println("filename: " + fileName);
    }
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
