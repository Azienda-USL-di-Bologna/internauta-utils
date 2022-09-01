package it.bologna.ausl.internauta.utils.firma.repositories;

import it.bologna.ausl.model.entities.firma.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


/**
 *
 * @author gdm
 */
@RepositoryRestResource(collectionResourceRel = "credential", path = "credential", exported = false)
public interface CredentialRepository extends QuerydslPredicateExecutor<Credential>, JpaRepository<Credential, String> {
}