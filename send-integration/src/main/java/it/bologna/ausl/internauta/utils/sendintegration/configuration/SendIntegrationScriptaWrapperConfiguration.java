package it.bologna.ausl.internauta.utils.sendintegration.configuration;

import org.springframework.stereotype.Component;

/**
 * Questa classe contiene l'istanza di SendIntegrationScriptaWrapperManager che serve per poter interagire con scripta di internauta.
 * L'istanza deve essere settata dall'applicazione internauta tramite il metodo setScriptaWrapperManger
 * 
 * @author gdm
 */
@Component
public class SendIntegrationScriptaWrapperConfiguration {
    
    private SendIntegrationScriptaWrapperManager scriptaWrapperManager;

    public SendIntegrationScriptaWrapperManager getScriptaWrapperManger() {
        return scriptaWrapperManager;
    }

    public void setScriptaWrapperManger(SendIntegrationScriptaWrapperManager scriptaWrapperManager) {
        this.scriptaWrapperManager = scriptaWrapperManager;
    }
}
