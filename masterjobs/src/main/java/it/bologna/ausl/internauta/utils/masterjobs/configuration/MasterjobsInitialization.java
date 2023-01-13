package it.bologna.ausl.internauta.utils.masterjobs.configuration;

import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsConfigurationException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.Worker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author gdm
 */
@Configuration
public class MasterjobsInitialization {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsInitialization.class);
    
    @Autowired
    private BeanFactory beanFactory;
    
    /**
     * Crea una mappa con chiave il nome del Worker e valore la classe corrispondente
     * @return
     * @throws MasterjobsConfigurationException 
     */
    @Bean
    public Map<String, Class<? extends Worker>> workerMap() throws MasterjobsConfigurationException {
        Map<String, Class<? extends Worker>> workerMap = new HashMap();
        
        Set<Class<?>> workersSet = new Reflections(Worker.class.getPackage().getName()).getTypesAnnotatedWith(MasterjobsWorker.class);
//        Set<Class<? extends Worker>> workersSet = (Set<Class<? extends Worker>>)typesAnnotatedWith;
        
        for (Class<?> workerClass : workersSet) {
            Class<? extends Worker> workerClassCasted = (Class<? extends Worker>) workerClass;
            Worker workerInstance;
            try {
                workerInstance = beanFactory.getBean(workerClassCasted);
            } catch (Exception ex) {
                String errorMessage = "errore nella creazione della mappa dei worker";
                log.error("errore nella creazione della mappa dei worker", ex);
                throw new MasterjobsConfigurationException(errorMessage);
            }
            workerMap.put(workerInstance.getName(), workerClassCasted);
        }
        
        return workerMap;
    }
}
