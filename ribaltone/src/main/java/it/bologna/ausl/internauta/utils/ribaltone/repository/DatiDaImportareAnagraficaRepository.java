/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
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
@Component("DatiDaImportareAnagraficaRepository")
@RepositoryRestResource(collectionResourceRel = "datidaimportareanagrafica", path = "datidaimportareanagrafica", exported = false)
public interface DatiDaImportareAnagraficaRepository extends QuerydslPredicateExecutor<DatiDaImportareAnagrafica>, JpaRepository<DatiDaImportareAnagrafica, Integer> {

}
