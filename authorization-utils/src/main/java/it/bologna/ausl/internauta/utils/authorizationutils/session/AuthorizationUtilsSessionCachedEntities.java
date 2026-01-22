package it.bologna.ausl.internauta.utils.authorizationutils.session;

import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Utente;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class AuthorizationUtilsSessionCachedEntities {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Cacheable(value = "personaFromUtenteAuthorizationUtils__ribaltorg__", key = "{#idUtente}")
    public Object getPersonaFromUtente(Integer idUtente) {
        Utente refreshedUtente = entityManager.find(Utente.class, idUtente);
        Persona persona = entityManager.find(Persona.class, refreshedUtente.getIdPersona().getId());
        return persona;
    }
}
