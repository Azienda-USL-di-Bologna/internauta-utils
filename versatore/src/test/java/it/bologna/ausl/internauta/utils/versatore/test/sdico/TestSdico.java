package it.bologna.ausl.internauta.utils.versatore.test.sdico;

import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.SdicoVersatoreService;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.VersamentoBuilder;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

/**
 *
 * @author Andrea
 */
//@RunWith(SpringRunner.class)
@SpringBootTest
public class TestSdico {
    
    private static final Logger log = LoggerFactory.getLogger(TestSdico.class);
    
    public static void main(String[] args) throws IOException, JAXBException {
        SdicoVersatoreService service = new SdicoVersatoreService();
//        String jwt = service.getJWT();
//        log.info("jwt: {}", jwt);
       // service.versa();
        service.getDete();
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
