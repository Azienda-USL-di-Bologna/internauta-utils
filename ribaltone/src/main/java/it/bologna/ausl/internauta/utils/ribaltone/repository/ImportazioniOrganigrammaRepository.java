package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.model.entities.ribaltonedati.ImportazioniOrganigramma;
import it.bologna.ausl.model.entities.ribaltonedati.QImportazioniOrganigramma;
import it.bologna.ausl.model.entities.ribaltonedati.projections.generated.ImportazioniOrganigrammaWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${ribaltonedati.mapping.url.root}/importazioniorganigramma", defaultProjection = ImportazioniOrganigrammaWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "importazioniorganigramma", path = "importazioniorganigramma", exported = false, excerptProjection = ImportazioniOrganigrammaWithPlainFields.class)
public interface ImportazioniOrganigrammaRepository extends
    NextSdrQueryDslRepository<ImportazioniOrganigramma, Integer, QImportazioniOrganigramma>, 
    JpaRepository<ImportazioniOrganigramma, Integer> {

}
