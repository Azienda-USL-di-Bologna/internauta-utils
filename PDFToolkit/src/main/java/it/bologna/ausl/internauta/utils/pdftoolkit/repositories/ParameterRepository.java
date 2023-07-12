package it.bologna.ausl.internauta.utils.pdftoolkit.repositories;

import it.bologna.ausl.model.entities.pdftoolkit.Parameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Component;

/**
 *
 * @author Giuseppe Russo <g.russo@dilaxia.com>
 */
@Component("pdfToolkitParameterRepository")
@RepositoryRestResource(collectionResourceRel = "parameter", path = "parameter", exported = false)
public interface ParameterRepository extends QuerydslPredicateExecutor<Parameter>, JpaRepository<Parameter, String> {
    
}
