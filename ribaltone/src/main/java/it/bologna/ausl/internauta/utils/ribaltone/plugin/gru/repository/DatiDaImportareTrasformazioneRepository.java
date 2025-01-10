/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.repository;

import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component("DatiDaImportareTrasformazioneRepository")
@RepositoryRestResource(collectionResourceRel = "datidaimportaretrasformazione", path = "datidaimportaretrasformazione", exported = false)
public interface DatiDaImportareTrasformazioneRepository extends QuerydslPredicateExecutor<DatiDaImportareTrasformazione>, JpaRepository<DatiDaImportareTrasformazione, Integer> {

}
