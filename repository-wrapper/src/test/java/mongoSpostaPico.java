
import com.mongodb.MongoException;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Top
 */
public class MongoSpostaPico {
    public static void main(String[] args) throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        String	minIODBDriver= "org.postgresql.Driver";
	String minIODBUrl = "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified";
	String minIODBUsername= "minirepo";
	String minIODBPassword ="siamofreschi";
    MongoWrapper m = MongoWrapper.getWrapper(true, "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true", minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, "105", null);
    //String uuid="88/38/2e/b4/88382eb4-3404-42f6-b7a9-5ae098c2f666/aosp delibere_determine.xls";
    String folder = "/Procton/Documenti/2021-836";
    System.out.println(m.getFileName("95b567ef-d9cb-4abf-aa4e-e91a23accb48"));
    Stream.of("d4668655-04bb-4735-9372-d48d5042a64a","95b567ef-d9cb-4abf-aa4e-e91a23accb48","9c48ee43-a27c-4317-a180-4197bc5bd0e4",
            "ea2fe4df-bc9a-4249-8c45-39b20d17ae64","76c34984-e407-4c25-a094-5dc2d15ff406","556017c0-f068-4241-93bf-25aea63004b8",
            "114414c1-8ab7-4a2a-90a7-232d80832343").forEach(uuid -> {
            try {
                m.erase(uuid);
            } catch (MongoWrapperException ex) {
                Logger.getLogger(MongoSpostaPico.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    
//    m.rename("52ec614d-7f4d-42c3-baa4-0e8c268905ac", "2021-836_Allegato_orig1.jpg");
//    m.unDelete("d2c25d32-feb1-407e-a923-9e154c94925a");

        //System.out.println("sjkashkjashkdhkasdhaskdhasdlas"+m.getFilePathByUuid("ea294248-f86e-49e9-8bdb-9f58b50c7785"));
        //InputStream get = m.get("ea294248-f86e-49e9-8bdb-9f58b50c7785");
        //cmisSaveToDisc(get,m.getFileName("ea294248-f86e-49e9-8bdb-9f58b50c7785"));
//        System.out.println();
//        List<String> dirFilesAndFolders = m.getDirFilesAndFolders(folder);
//              dirFilesAndFolders.stream().forEach(f -> {try {
//            System.out.println(f + " " + m.getFileName(f));
////            m.erase(f);
//            } catch (MongoWrapperException ex) {
//                Logger.getLogger(MongoSpostaPico.class.getName()).log(Level.SEVERE, null, ex);
//            }
//              });
    }
    
    public static void cmisSaveToDisc(InputStream is, String path) throws IOException
  {
    File f=new File(path);
      OutputStream out=new FileOutputStream(f);
      byte buf[]=new byte[1024];
      int len;
      while((len=is.read(buf))>0)
        out.write(buf,0,len);
      
      out.close();
      is.close();
  }
}
