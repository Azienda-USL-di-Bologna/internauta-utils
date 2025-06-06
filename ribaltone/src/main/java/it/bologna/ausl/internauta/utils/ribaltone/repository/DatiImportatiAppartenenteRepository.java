/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAppartenente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component("DatiImportatiAppartenenteRepository")
@RepositoryRestResource(collectionResourceRel = "datiimportatiappartenente", path = "datiimportatiappartenente", exported = false)
public interface DatiImportatiAppartenenteRepository extends QuerydslPredicateExecutor<DatiImportatiAppartenente>, JpaRepository<DatiImportatiAppartenente, Integer> {

    public List<DatiImportatiAppartenente> findByCodiceAzienda(String codiceAzienda);

    @Query(value = "INSERT INTO ribaltone_dati.dati_importati_appartenenti "
        + "(codice_ente, codice_matricola, cognome, nome, codice_fiscale, id_casella, datain, datafi, tipo_appartenenza, username, responsabile, data_assunzione, data_dimissione, codice_azienda, id_azienda) "
        + "SELECT codice_ente, codice_matricola, cognome, nome, codice_fiscale, id_casella, datain, datafi, tipo_appartenenza, username, responsabile, data_assunzione, data_dimissione, codice_azienda, id_azienda "
        + "FROM ribaltone_dati.dati_da_importare_appartenenti WHERE codice_azienda =?1 AND  datain < now() AND (datafi IS NULL OR datafi > now())", nativeQuery = true)
    public void fromDatiDaImportareToDatiImportati(String codiceAzienda);

    @Query(value = "INSERT INTO ribaltone_dati.dati_importati_appartenenti "
        + "(codice_ente, codice_matricola, cognome, nome, codice_fiscale, id_casella, datain, datafi, tipo_appartenenza, username, responsabile, data_assunzione, data_dimissione, codice_azienda, id_azienda) "
        + "SELECT codice_ente, codice_matricola, cognome, nome, codice_fiscale, id_casella, datain, datafi, tipo_appartenenza, username, responsabile, data_assunzione, data_dimissione, codice_azienda, id_azienda "
        + "FROM ribaltone_dati.csv_da_importare_appartenenti WHERE codice_azienda =?1 AND  datain < now() AND (datafi IS NULL OR datafi > now())", nativeQuery = true)
    public void fromCSVDaImportareToDatiImportati(String codiceAzienda);

    @Query(value = "DELETE FROM ribaltone_dati.dati_importati_appartenenti WHERE codice_azienda = ?1", nativeQuery = true)
    public void deleteAllByCodiceAzienda(String codiceAzienda);

}
