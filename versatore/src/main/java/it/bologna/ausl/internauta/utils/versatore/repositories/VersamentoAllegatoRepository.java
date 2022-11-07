package it.bologna.ausl.internauta.utils.versatore.repositories;

import it.bologna.ausl.model.entities.versatore.VersamentoAllegato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


/**
 *
 * @author gdm
 */
@RepositoryRestResource(collectionResourceRel = "versamentoallegato", path = "versamentoallegato", exported = false)
public interface VersamentoAllegatoRepository extends QuerydslPredicateExecutor<VersamentoAllegato>, JpaRepository<VersamentoAllegato, String> {
}