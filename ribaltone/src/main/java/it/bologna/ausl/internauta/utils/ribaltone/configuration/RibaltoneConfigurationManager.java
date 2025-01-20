package it.bologna.ausl.internauta.utils.ribaltone.configuration;

import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.RibaltoneParamConf;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 *
 * @author Top
 * @param <T> classe che estende RibaltoneConf
 */

public abstract class RibaltoneConfigurationManager<T extends RibaltoneParamConf> {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private T ribaltoneConfiguration;
    
    public  T initRibaltone(RibaltoneParamConf ribaltoneConf){
        ribaltoneConfiguration =  objectMapper.convertValue(ribaltoneConf.getSpecifiche(), new TypeReference<T>(){});
        return ribaltoneConfiguration;
    };

    public T getRibaltoneConfiguration() {
        return ribaltoneConfiguration;
    }

    public void setRibaltoneConfiguration(T ribaltoneConfiguration) {
        this.ribaltoneConfiguration = ribaltoneConfiguration;
    }
}
