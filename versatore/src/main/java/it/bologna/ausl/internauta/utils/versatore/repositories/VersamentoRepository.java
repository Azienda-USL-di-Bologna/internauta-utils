package it.bologna.ausl.internauta.utils.versatore.repositories;

import it.bologna.ausl.model.entities.versatore.Versamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


/**
 *
 * @author gdm
 */
@RepositoryRestResource(collectionResourceRel = "versamento", path = "versamento", exported = false)
public interface VersamentoRepository extends QuerydslPredicateExecutor<Versamento>, JpaRepository<Versamento, String> {
}