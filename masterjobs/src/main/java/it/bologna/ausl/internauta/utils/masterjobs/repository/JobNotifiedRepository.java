package it.bologna.ausl.internauta.utils.masterjobs.repository;

import it.bologna.ausl.model.entities.masterjobs.JobNotified;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author gusgus
 */
@RepositoryRestResource(collectionResourceRel = "jobnotified", path = "jobnotified", exported = false)
public interface JobNotifiedRepository extends QuerydslPredicateExecutor<JobNotified>, JpaRepository<JobNotified, Long> {
    
}