package it.bologna.ausl.internauta.utils.parameters.manager.repositories;

import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.configurazione.QParametroAziende;
import it.bologna.ausl.model.entities.configurazione.projections.generated.ParametroAziendeWithPlainFields;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component("ParametroAziendeRepositoryParametersManager")
@RepositoryRestResource(collectionResourceRel = "parametroaziende", path = "parametroaziende", exported = false, excerptProjection = ParametroAziendeWithPlainFields.class)
public interface ParametroAziendeRepository extends
        NextSdrQueryDslRepository<ParametroAziende, Integer, QParametroAziende>,
        JpaRepository<ParametroAziende, Integer> {
}
