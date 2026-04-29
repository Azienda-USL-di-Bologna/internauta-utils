package it.bologna.ausl.internauta.utils.sendintegration.configuration;

import it.bologna.ausl.internauta.utils.sendintegration.exceptions.SendIntegrationException;
import it.bologna.ausl.internauta.utils.sendintegration.model.InfoRegistrazioneLotto;
import java.util.Map;

/**
 * Classe astratta che descrive i metodi che internauta deve implementare per potersi integrare con il modulo send-integration
 * 
 * La classe va implementata all'interno di internauta e poi settata tramite il mettodo
 *  setScriptaWrapperManger della classe it.bologna.ausl.internauta.utils.sendintegration.configuration.SendIntegrationScriptaWrapperConfiguration
 * 
 * @author gdm
 */
public abstract class SendIntegrationScriptaWrapperManager {
    
    public abstract InfoRegistrazioneLotto generaDocumentoPUProtocollato(String paId, String lottoId, String cfFirmatario, String outputBasePath, Map<String, Object> paConfiguration) throws SendIntegrationException ;
}
