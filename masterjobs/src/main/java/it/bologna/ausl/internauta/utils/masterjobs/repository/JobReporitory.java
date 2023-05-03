package it.bologna.ausl.internauta.utils.masterjobs.repository;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import it.bologna.ausl.model.entities.masterjobs.Job;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author Top
 */
@RepositoryRestResource(collectionResourceRel = "job", path = "job", exported = false)
public interface JobReporitory extends QuerydslPredicateExecutor<Job>, JpaRepository<Job, Long> {
    
    @Query(value = "select masterjobs.calcola_md5(?1,cast(?2 as jsonb),?3)", nativeQuery = true)
    public String calcolaMD5(
            @Param("nome") String nome,
            @Param("dati") String data,
            @Param("differito") Boolean oggetti
    );
}