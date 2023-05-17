package it.bologna.ausl.internauta.utils.versatore.plugins.sdico;

import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.IdoneitaChecker;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 *
 * @author Andrea
 */
@Component
public class SdicoIdoneitaCheckerService extends IdoneitaChecker{

    @Override
    public Boolean checkDocImpl(Integer id, Map<String, Object> params) throws VersatoreProcessingException {
        // momentaneamente per i test ritorno sempre true
        return true;
    }

    @Override
    public Boolean checkArchivioImpl(Integer id, Map<String, Object> params) throws VersatoreProcessingException {
        // momentaneamente per i test ritorno sempre true
        return true;
    }
    
}
