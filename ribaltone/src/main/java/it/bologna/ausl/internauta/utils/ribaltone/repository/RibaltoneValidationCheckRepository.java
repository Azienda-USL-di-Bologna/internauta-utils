package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.model.entities.ribaltonedati.checks.RibaltoneValidationCheck;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component("RibaltoneValidationCheckRepository")
@RepositoryRestResource(collectionResourceRel = "ribaltonevalidationcheck", path = "ribaltonevalidationcheck", exported = false)
public interface RibaltoneValidationCheckRepository extends QuerydslPredicateExecutor<RibaltoneValidationCheck>, JpaRepository<RibaltoneValidationCheck, Integer> {

    @Query(value = "SELECT r.* FROM ribaltone_dati.ribaltone_validation_checks r "
            + "JOIN baborg.aziende a ON a.id = ANY(r.id_aziende) "
            + "WHERE r.attivo = true AND a.codice = :codiceAzienda order by r.id",
            nativeQuery = true)
    List<RibaltoneValidationCheck> findCheckActiveByCodiceAzienda(@Param("codiceAzienda") String codiceAzienda);

}
