package it.bologna.ausl.internauta.utils.versatore.test.sdico;

import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.SdicoVersatoreService;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.VersamentoBuilder;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.model.entities.versatore.Provider;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.persistence.EntityManager;
import javax.xml.bind.JAXBException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 *
 * @author Andrea
 */
//@RunWith(SpringRunner.class)
@SpringBootTest
public class TestSdico {
    
    private static final Logger log = LoggerFactory.getLogger(TestSdico.class);
   
    private static VersamentoDocInformation versamentoDocInformation;
    
    public static void main(String[] args) throws IOException, JAXBException {
        
        //DocDetail docDetail = new DocDetail();
        //docDetail.setTipologia(DocDetailInterface.TipologiaDoc.DELIBERA);
        
        SdicoVersatoreService service = new SdicoVersatoreService();
        
        FileInputStream fstream = new FileInputStream("C:\\tmp\\metadati.xml");
        
        String result = IOUtils.toString(fstream, StandardCharsets.UTF_8);
        
        //service.getDoc(docDetail, archivioScelto);
        
        
        //String jwt = service.getJWT();
        //log.info("jwt: {}", jwt);
        System.out.println(service.versa(result));
        
    }
    
//    @Test
//    public void test() throws Throwable {
//                
////        String test = "test";
////        log.info("SDICO login URI: {}", test);
//
//        SdicoVersatoreService service = new SdicoVersatoreService();
//        String jwt = service.getJWT();
//        log.info("jwt: {}", jwt);
//    }
}
