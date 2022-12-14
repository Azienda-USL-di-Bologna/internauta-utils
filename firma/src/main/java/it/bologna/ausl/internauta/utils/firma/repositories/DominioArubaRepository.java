package it.bologna.ausl.internauta.utils.firma.repositories;

import it.bologna.ausl.model.entities.firma.DominioAruba;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


/**
 *
 * @author gdm
 */
@RepositoryRestResource(collectionResourceRel = "dominioaruba", path = "dominioaruba", exported = false)
public interface DominioArubaRepository extends QuerydslPredicateExecutor<DominioAruba>, JpaRepository<DominioAruba, String> {
    
    @Query(value = "select host_id from firma.configurations c where to_jsonb(cast(:dominio as text)) <@ (params -> 'ArubaSignService' -> 'SupportedDomini')", nativeQuery = true)
    public String getHostId(String dominio);
}