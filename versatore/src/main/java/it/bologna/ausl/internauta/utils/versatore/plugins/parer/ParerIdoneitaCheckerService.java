package it.bologna.ausl.internauta.utils.versatore.plugins.parer;

import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.IdoneitaChecker;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class ParerIdoneitaCheckerService extends IdoneitaChecker {

    @Override
    public Boolean checkDocImpl(Integer id, Map<String,Object> params) throws VersatoreProcessingException {
        //TODO: implementare il controllo parer
        throw new UnsupportedOperationException("Non supportato dal plugin Parer, spegnere il parametro aziendale");
    }

    @Override
    public Boolean checkArchivioImpl(Integer id, Map<String,Object> params) throws VersatoreProcessingException {
        throw new UnsupportedOperationException("Non supportato dal plugin Parer, spegnere il parametro aziendale");
    }

}
