package it.bologna.ausl.internauta.utils.masterjobs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsObjectNotFoundException;
import it.bologna.ausl.model.entities.masterjobs.DebuggingOption;
import it.bologna.ausl.model.entities.masterjobs.QDebuggingOption;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class DebuggingOptionsManager {
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public <T> T getDebuggingParam(DebuggingOption.Key key, TypeReference<T> typeReference) throws MasterjobsObjectNotFoundException{
        return objectMapper.convertValue(getDebuggingParam(key), typeReference);
    }
    
    public <T> T getDebuggingParam(DebuggingOption.Key key, Class<T> classz) throws MasterjobsObjectNotFoundException{
        return objectMapper.convertValue(getDebuggingParam(key), classz);
    }
    
    private Object getDebuggingParam(DebuggingOption.Key key) throws MasterjobsObjectNotFoundException {
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        QDebuggingOption qDebuggingOption = QDebuggingOption.debuggingOption;
        Object valueObj = jPAQueryFactory
            .select(qDebuggingOption.value)
            .from(qDebuggingOption)
            .where(qDebuggingOption.key.eq(key.toString()))
            .fetchOne();
        if (valueObj == null)
            throw new MasterjobsObjectNotFoundException(String.format("debugging option with keys %s not found", key.toString()));
        return valueObj;
    }
}
