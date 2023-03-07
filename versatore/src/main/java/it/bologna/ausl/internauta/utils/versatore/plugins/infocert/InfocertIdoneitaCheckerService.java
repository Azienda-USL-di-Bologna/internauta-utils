package it.bologna.ausl.internauta.utils.versatore.plugins.infocert;

import it.bologna.ausl.internauta.utils.versatore.plugins.*;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class InfocertIdoneitaCheckerService extends IdoneitaChecker {

    @Override
    public Boolean checkDocImpl(Integer id) throws VersatoreProcessingException {
        throw new UnsupportedOperationException("Non supportato dal plugin Infocert, spegnere il parametro aziendale");
    }

    @Override
    public Boolean checkArchivioImpl(Integer id) throws VersatoreProcessingException {
        return true;
    }
    

}
