/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiTrasformazione;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component("DatiImportatiTrasformazioneRepository")
@RepositoryRestResource(collectionResourceRel = "datiimportatitrasformazione", path = "datiimportatitrasformazione", exported = false)
public interface DatiImportatiTrasformazioneRepository extends QuerydslPredicateExecutor<DatiImportatiTrasformazione>, JpaRepository<DatiImportatiTrasformazione, Integer> {

    public List<DatiImportatiTrasformazione> findByCodiceAzienda(String codiceAzienda);
    
    
    public DatiImportatiTrasformazione findTopByCodiceAziendaOrderByProgressivoRigaDesc(String codiceAzienda);

}
