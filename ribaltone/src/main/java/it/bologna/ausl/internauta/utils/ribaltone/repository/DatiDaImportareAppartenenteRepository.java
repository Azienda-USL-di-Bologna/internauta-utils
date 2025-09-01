/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.nextsw.common.data.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component("DatiDaImportareAppartenenteRepository")
@RepositoryRestResource(collectionResourceRel = "datidaimportareappartenente", path = "datidaimportareappartenente", exported = false)
public interface DatiDaImportareAppartenenteRepository extends QuerydslPredicateExecutor<DatiDaImportareAppartenente>, JpaRepository<DatiDaImportareAppartenente, Integer> {

    public void deleteByIdAzienda(Integer idAzienda);
}
