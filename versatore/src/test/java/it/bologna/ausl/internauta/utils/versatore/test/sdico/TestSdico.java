package it.bologna.ausl.internauta.utils.versatore.test.sdico;

import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.SdicoVersatoreService;
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
@SpringBootTest
public class TestSdico {

    private static final Logger log = LoggerFactory.getLogger(TestSdico.class);

    public static void main(String[] args) throws IOException, JAXBException, Exception {
        
        
    }
}
