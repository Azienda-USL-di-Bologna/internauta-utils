package it.bologna.ausl.internauta.utils.authorizationutils.cache;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdema
 */
@Component
public class AuthorizationUtilsCachedEntities {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Cacheable(value = "personaFromUtenteAuthorizationUtils__ribaltorg__", key = "{#utente.getId()}")
    public Persona getPersonaFromUtente(Utente utente) {
        Utente refreshedUtente = entityManager.find(Utente.class, utente.getId());
        Persona persona = entityManager.find(Persona.class, refreshedUtente.getIdPersona().getId());
        return persona;
    }
}
