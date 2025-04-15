/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.model.entities.ribaltonedati.QRibaltoneDataConfiguration;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import it.bologna.ausl.model.entities.ribaltonedati.projections.generated.RibaltoneDataConfigurationWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author Top
 */
@NextSdrRepository(repositoryPath = "${ribaltonedati.mapping.url.root}/ribaltonedataconfiguration", defaultProjection = RibaltoneDataConfigurationWithPlainFields.class)
@RepositoryRestResource(collectionResourceRel = "ribaltonedataconfiguration", path = "ribaltonedataconfiguration", exported = false)
//@RepositoryRestResource(collectionResourceRel = "ribaltonedataconfiguration", path = "ribaltonedataconfiguration", exported = false, excerptProjection = RibaltoneDataConfigurationWithPlainFields.class)
public interface RibaltoneDataConfigurationRepository extends NextSdrQueryDslRepository<RibaltoneDataConfiguration, String, QRibaltoneDataConfiguration>, JpaRepository<RibaltoneDataConfiguration, String> {

   
    
}
