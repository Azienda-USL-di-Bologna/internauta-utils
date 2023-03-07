package it.bologna.ausl.internauta.utils.versatore.plugins.infocert;

import it.bologna.ausl.internauta.utils.versatore.plugins.*;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 * 
 * Implementa il controllo di idoneità Infocert
 * 
 * Per Infocert gli archivi candicati al controllo (quindi chiusi e di livello 1) sono tutti idonei, 
 * mentre non supporta il controllo di idoneità sui singoli Doc, in quanto, vengono versati solo i doc all'interno
 * degli archivi chiusi
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
