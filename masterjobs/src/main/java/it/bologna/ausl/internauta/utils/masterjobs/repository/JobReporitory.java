package it.bologna.ausl.internauta.utils.masterjobs.repository;

import it.bologna.ausl.model.entities.masterjobs.Job;
import it.bologna.ausl.model.entities.masterjobs.QJob;
import it.bologna.ausl.model.entities.masterjobs.projections.generated.JobWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author gusgus
 */
@NextSdrRepository(repositoryPath = "${masterjobs.mapping.url.root}/job", defaultProjection = JobWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "job", path = "job", exported = false, excerptProjection = JobWithPlainFields.class)
public interface JobReporitory extends
        NextSdrQueryDslRepository<Job, Long, QJob>,
        JpaRepository<Job, Long>  {
    
    @Query(value = "select masterjobs.calcola_md5(?1,cast(?2 as jsonb),?3)", nativeQuery = true)
    public String calcolaMD5(
            @Param("nome") String nome,
            @Param("dati") String data,
            @Param("differito") Boolean oggetti
    );
}