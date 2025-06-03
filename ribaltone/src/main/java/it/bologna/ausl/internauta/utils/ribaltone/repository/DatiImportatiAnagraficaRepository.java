/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAnagrafica;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component("DatiImportatiAnagraficaRepository")
@RepositoryRestResource(collectionResourceRel = "datiimportatianagrafica", path = "datiimportatianagrafica", exported = false)
public interface DatiImportatiAnagraficaRepository extends QuerydslPredicateExecutor<DatiImportatiAnagrafica>, JpaRepository<DatiImportatiAnagrafica, Integer> {

    public List<DatiImportatiAnagrafica> findByCodiceAzienda(String codiceAzienda);

}
