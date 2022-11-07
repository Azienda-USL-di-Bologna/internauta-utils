package it.bologna.ausl.internauta.utils.versatore.repositories;

import it.bologna.ausl.model.entities.versatore.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


/**
 *
 * @author gdm
 */
@RepositoryRestResource(collectionResourceRel = "configuration", path = "configuration", exported = false)
public interface ConfigurationRepository extends QuerydslPredicateExecutor<Configuration>, JpaRepository<Configuration, String> {
}