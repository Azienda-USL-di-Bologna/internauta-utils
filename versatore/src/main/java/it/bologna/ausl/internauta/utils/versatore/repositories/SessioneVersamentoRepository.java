package it.bologna.ausl.internauta.utils.versatore.repositories;

import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Component;


/**
 *
 * @author gdm
 */
@Component("VersatoreSessioneVersamentoRepository")
@RepositoryRestResource(collectionResourceRel = "sessioneversamento", path = "sessioneversamento", exported = false)
public interface SessioneVersamentoRepository extends QuerydslPredicateExecutor<SessioneVersamento>, JpaRepository<SessioneVersamento, String> {
}