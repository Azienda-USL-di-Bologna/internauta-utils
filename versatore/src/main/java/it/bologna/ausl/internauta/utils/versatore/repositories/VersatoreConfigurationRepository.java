package it.bologna.ausl.internauta.utils.versatore.repositories;

import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Component;


/**
 *
 * @author gdm
 */
@Component("VersatoreConfigurationRepository")
@RepositoryRestResource(collectionResourceRel = "configuration", path = "configuration", exported = false)
public interface VersatoreConfigurationRepository extends QuerydslPredicateExecutor<VersatoreConfiguration>, JpaRepository<VersatoreConfiguration, String> {
}