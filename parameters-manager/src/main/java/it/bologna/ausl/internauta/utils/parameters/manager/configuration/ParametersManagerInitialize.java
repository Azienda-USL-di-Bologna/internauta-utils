package it.bologna.ausl.internauta.utils.parameters.manager.configuration;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definisce il Bean per avere mappa codice-azienda->id-azienda
 * @author gdm
 */
@Configuration
public class ParametersManagerInitialize {
    
    // Questa sarà in realtà l'istanza del'oggetto concreto implementato dall'applicazione utilizzatrice (es. internauta-service)
    @Autowired
    private ParametersManagerConfiguration parametersManagerConfiguration;
    
    /**
     * Questo Bean permette di poter avere la mappa codice-azienda->id-azienda inserendo la proprieta di classe:
     * @Autowired
     * @Qualifier("codiceAziendaIdAziendaMap")
     * private Map<String, Integer> codiceAziendaIdAziendaMap;
     * @return 
     */
    @Bean(name = "codiceAziendaIdAziendaMap")
    public Map<String, Object> codiceAziendaIdAziendaMap() {
        return this.parametersManagerConfiguration.getCodiceAziendaIdAziendaMap();
    }
}
