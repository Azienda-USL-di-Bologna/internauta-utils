package it.bologna.ausl.internauta.utils.firma.repositories;

import it.bologna.ausl.model.entities.firma.FirmaDelega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


/**
 *
 * @author gdm
 */
@RepositoryRestResource(collectionResourceRel = "firmadelega", path = "firmadelega", exported = false)
public interface FirmaDelegaRepository extends QuerydslPredicateExecutor<FirmaDelega>, JpaRepository<FirmaDelega, String> {
}