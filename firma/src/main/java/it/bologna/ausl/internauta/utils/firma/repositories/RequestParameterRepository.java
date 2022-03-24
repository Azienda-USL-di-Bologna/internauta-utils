package it.bologna.ausl.internauta.utils.firma.repositories;

import it.bologna.ausl.model.entities.firma.RequestParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


/**
 *
 * @author gdm
 */
//@NextSdrRepository(repositoryPath = "${messaggero.mapping.url.root}/amministrazionemessaggio", defaultProjection = AmministrazioneMessaggioWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "requestparameter", path = "requestparameter", exported = false)
public interface RequestParameterRepository extends QuerydslPredicateExecutor<RequestParameter>, JpaRepository<RequestParameter, Integer> {
}