package it.bologna.ausl.internauta.utils.versatore.utils;

import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.repositories.ParameterRepository;
import it.bologna.ausl.model.entities.versatore.Parameter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

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
    @Qualifier("VersatoreParameterRepository")
    private ParameterRepository parameterRepository;
        
    private List<Map<String, Object>> versatoreParams;
       
    /**
     * Questo metodo viene eseguito in fase di boot dell'applicazione.
     * Inizializza il tutto
     * @throws UnknownHostException
     * @throws IOException
     * @throws VersatoreProcessingException 
     */
    @PostConstruct
    public void init() throws UnknownHostException, IOException, VersatoreProcessingException {
        
        // lettura dei parametri
        List<Parameter> parameters = parameterRepository.findAll();
        if (!parameters.isEmpty()) {
            this.versatoreParams = new ArrayList<>();
            parameters.stream().forEach(p -> this.versatoreParams.add(p.getValue()));
        }
    }    
}
