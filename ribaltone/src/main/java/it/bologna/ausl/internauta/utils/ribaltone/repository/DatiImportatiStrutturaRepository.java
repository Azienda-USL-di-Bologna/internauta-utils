/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component("DatiImportatiStrutturaRepository")
@RepositoryRestResource(collectionResourceRel = "datiimportatistruttura", path = "datiimportatistruttura", exported = false)
public interface DatiImportatiStrutturaRepository extends QuerydslPredicateExecutor<DatiImportatiStruttura>, JpaRepository<DatiImportatiStruttura, Integer> {

    public List<DatiImportatiStruttura> findByCodiceAzienda(String codiceAzienda);

}
