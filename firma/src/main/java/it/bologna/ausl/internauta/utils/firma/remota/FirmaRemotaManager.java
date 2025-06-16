package it.bologna.ausl.internauta.utils.firma.remota;

import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation;
import it.bologna.ausl.internauta.utils.firma.exceptions.FirmaHttpException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class FirmaRemotaManager {
    private static Logger logger = LoggerFactory.getLogger(FirmaRemotaManager.class);
    
    private final FirmaRemotaFactory firmaRemotaFactory;

    public FirmaRemotaManager(FirmaRemotaFactory firmaRemotaFactory) {
        this.firmaRemotaFactory = firmaRemotaFactory;
    }
    
    public FirmaRemotaInformation firma(
        FirmaRemotaInformation firmaRemotaInformation, String hostId, String codiceAzienda, HttpServletRequest request)  throws FirmaHttpException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = firmaRemotaFactory.getFirmaRemotaInstance(hostId);
        try {
            FirmaRemotaInformation res = firmaRemotaInstance.firma(firmaRemotaInformation, codiceAzienda, request);
            return res;
        } catch (Exception ex) {
            logger.error("errore nella firma", ex);
            throw ex;
        }
    }
}
