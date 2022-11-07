package it.bologna.ausl.internauta.utils.versatore.repositories;

import it.bologna.ausl.model.entities.versatore.Parameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


/**
 *
 * @author gdm
 */
@RepositoryRestResource(collectionResourceRel = "parameter", path = "parameter", exported = false)
public interface ParameterRepository extends QuerydslPredicateExecutor<Parameter>, JpaRepository<Parameter, String> {
}