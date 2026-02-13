package it.bologna.ausl.internauta.utils.firma.remota;

import it.bologna.ausl.internauta.utils.firma.repositories.FirmaDelegaRepository;
import it.bologna.ausl.model.entities.firma.FirmaDelega;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class FirmeDelegaManager {
    
    @Autowired
    private FirmaDelegaRepository firmaDelegaRepository;
    
    public FirmaDelega getFirmaDelega(String id) {
        return firmaDelegaRepository.getReferenceById(id);
    }
    
}
