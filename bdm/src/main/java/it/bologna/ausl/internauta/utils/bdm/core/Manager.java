package it.bologna.ausl.internauta.utils.bdm.core;

import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess.BdmStatus;
import it.bologna.ausl.internauta.utils.bdm.utilities.Dumpable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author gdm
 */
@Component
public class Manager {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private BdmStatus executeProcess(String processId) throws FileNotFoundException, IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream("c:/job.json");
        BdmProcess process = Dumpable.load(fis, BdmProcess.class, objectMapper);
        return null;
        
    }
}
