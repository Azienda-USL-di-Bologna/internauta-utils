
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Top
 */
public class mongoSpostaPico {
    public static void main(String[] args) throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        String	minIODBDriver= "org.postgresql.Driver";
	String minIODBUrl = "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified";
	String minIODBUsername= "minirepo";
	String minIODBPassword ="siamofreschi";
    MongoWrapper m = MongoWrapper.getWrapper(true, "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true", minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, "105", null);
    //String uuid="88/38/2e/b4/88382eb4-3404-42f6-b7a9-5ae098c2f666/aosp delibere_determine.xls";
    String folder = "/Procton\\/Documenti\\/2021-683";
//    m.move(uuid,folder);

        System.out.println("sjkashkjashkdhkasdhaskdhasdlas"+m.getFilePathByUuid("ea294248-f86e-49e9-8bdb-9f58b50c7785"));
        InputStream get = m.get("ea294248-f86e-49e9-8bdb-9f58b50c7785");
        cmisSaveToDisc(get,m.getFileName("ea294248-f86e-49e9-8bdb-9f58b50c7785"));
//        System.out.println();
//        List<String> dirFilesAndFolders = m.getDirFilesAndFolders(folder);
//              dirFilesAndFolders.stream().forEach(f -> {try {
//            System.out.println(f + " " + m.getFileName(f));
////            m.erase(f);
//            } catch (MongoWrapperException ex) {
//                Logger.getLogger(mongoSpostaPico.class.getName()).log(Level.SEVERE, null, ex);
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
