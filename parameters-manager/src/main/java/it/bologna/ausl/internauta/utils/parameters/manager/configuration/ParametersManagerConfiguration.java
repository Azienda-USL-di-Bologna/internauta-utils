package it.bologna.ausl.internauta.utils.parameters.manager.configuration;

import java.util.Map;

/**
 * Da implementare per poter fornire a parameters-manager (e suoi utilizzatori) la mappa codice-azienda->id-azienda
 * @author gdm
 */
public abstract class ParametersManagerConfiguration {
    
    /**
     * Deve tornare la mappa codice-azienda->id-azienda
     * @return 
     */
    public abstract Map<String, Object> getCodiceAziendaIdAziendaMap();
}
