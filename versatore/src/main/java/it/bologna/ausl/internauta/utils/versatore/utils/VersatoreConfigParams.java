package it.bologna.ausl.internauta.utils.versatore.utils;



import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreConfigurationException;
import it.bologna.ausl.internauta.utils.versatore.repositories.ParameterRepository;
import it.bologna.ausl.model.entities.versatore.Parameter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

/**
 * Questa classe legge, i parametri di configurazione dal database (tabella firma.parameters)
 * 
 * @author gdm
 */
@Service
public class VersatoreConfigParams {

    private static Logger logger = LoggerFactory.getLogger(VersatoreConfigParams.class);

    public enum ParameterIds {
        downloader,
        minIOConfig,
        externalCheckCertificate,
    }
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ParameterRepository parameterRepository;
        
    private List<Map<String, Object>> versatoreParams;
       
    /**
     * Questo metodo viene eseguito in fase di boot dell'applicazione.
     * Inizializza il tutto
     * @throws UnknownHostException
     * @throws IOException
     * @throws VersatoreConfigurationException 
     */
    @PostConstruct
    public void init() throws UnknownHostException, IOException, VersatoreConfigurationException {
        
        // lettura dei parametr di MinIO
        List<Parameter> parameters = parameterRepository.findAll();
        if (!parameters.isEmpty()) {
            this.versatoreParams = new ArrayList<>();
            parameters.stream().forEach(p -> this.versatoreParams.add(p.getValue()));
        }
    }    
}
