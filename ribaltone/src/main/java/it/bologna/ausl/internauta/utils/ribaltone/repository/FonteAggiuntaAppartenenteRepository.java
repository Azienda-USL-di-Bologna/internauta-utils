package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.model.entities.ribaltonedati.FonteAggiuntaAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.QFonteAggiuntaAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.projections.generated.FonteAggiuntaAppartenenteWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * per convenzione nostra, collectionResourceRel e path devono avere lo stesso
 * nome tutto in minuscolo
 */
@NextSdrRepository(repositoryPath = "${ribaltonedati.mapping.url.root}/fonteaggiuntaappartenente", defaultProjection = FonteAggiuntaAppartenenteWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "fonteaggiuntaappartenente", path = "fonteaggiuntaappartenente", exported = false, excerptProjection = FonteAggiuntaAppartenenteWithPlainFields.class)
public interface FonteAggiuntaAppartenenteRepository extends
    NextSdrQueryDslRepository<FonteAggiuntaAppartenente, Integer, QFonteAggiuntaAppartenente>, 
    JpaRepository<FonteAggiuntaAppartenente, Integer> {

}
