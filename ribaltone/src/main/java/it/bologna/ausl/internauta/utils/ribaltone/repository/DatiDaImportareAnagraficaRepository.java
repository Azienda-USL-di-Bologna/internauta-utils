package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component("DatiDaImportareAnagraficaRepository")
@RepositoryRestResource(collectionResourceRel = "datidaimportareanagrafica", path = "datidaimportareanagrafica", exported = false)
public interface DatiDaImportareAnagraficaRepository extends
    QuerydslPredicateExecutor<DatiDaImportareAnagrafica>,
    JpaRepository<DatiDaImportareAnagrafica, Integer> {

    void deleteByIdAzienda(Integer idAzienda);

}
