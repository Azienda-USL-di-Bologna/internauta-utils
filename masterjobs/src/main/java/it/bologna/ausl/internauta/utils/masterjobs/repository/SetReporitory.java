package it.bologna.ausl.internauta.utils.masterjobs.repository;

import it.bologna.ausl.model.entities.masterjobs.QSet;
import it.bologna.ausl.model.entities.masterjobs.Set;
import it.bologna.ausl.model.entities.masterjobs.projections.generated.SetWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author gusgus
 */
@NextSdrRepository(repositoryPath = "${masterjobs.manager.api.url}/job", defaultProjection = SetWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "set", path = "set", exported = false, excerptProjection = SetWithPlainFields.class)
public interface SetReporitory extends
        NextSdrQueryDslRepository<Set, Long, QSet>,
        JpaRepository<Set, Long>  {
    
    /**
     * torna una stringa che rappresenta un json che contiene una lista di set in cui per ognuno c'è l'id del set, la sua priprotà e la lista degli id dei suoi jobs
     * es. 
     *  [
     *      {
     *          "set_id": set_id,
     *          "jobs_ids": [
     *              job_id_1,
     *              job_id_2,
     *              ...
     *          ],
     *          "set_prority": "NORMAL/HIGH/HIGHEST"
     *      },
     *  ...
     *  ]
     * 
     * 
     * @return 
     */
    @Query(value = "select cast(jsonb_agg(r) as text) from (" +
                        "select s.id as set_id,  s.priority as set_prority, jsonb_agg(j.id) as jobs_ids " +
                        "from masterjobs.sets s inner join masterjobs.jobs j on s.id = j.set " +
                        "group by s.id" +
                    ") r",
            nativeQuery = true
    )
    public String getSetWithJobsArray(); 
}