/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAnagrafica;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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

    @Modifying
    @Query(value = "INSERT INTO ribaltone_dati.dati_importati_anagrafica "
        + "(codice_ente, codice_matricola, cognome, nome, codice_fiscale, email, codice_azienda, password_hash, id_azienda) "
        + "SELECT codice_ente, codice_matricola, cognome, nome, codice_fiscale, email, codice_azienda, password_hash, id_azienda "
        + "FROM ribaltone_dati.csv_da_importare_anagrafiche "
        + "WHERE codice_azienda = ?1", nativeQuery = true)
    public void fromCSVDaImportareToDatiImportati(String codiceAzienda);

    @Modifying
    @Query(value = "INSERT INTO ribaltone_dati.dati_importati_anagrafica "
        + "(codice_ente, codice_matricola, cognome, nome, codice_fiscale, email, codice_azienda, password_hash, id_azienda) "
        + "SELECT codice_ente, codice_matricola, cognome, nome, codice_fiscale, email, codice_azienda,password_hash, id_azienda "
        + "FROM ribaltone_dati.dati_da_importare_anagrafica "
        + "WHERE codice_azienda = ?1", nativeQuery = true)
    public void fromDatiDaImportareToDatiImportati(String codiceAzienda);

    @Modifying
    @Query(value = "INSERT INTO ribaltone_dati.dati_importati_anagrafica "
        + "(codice_ente, codice_matricola, cognome, nome, codice_fiscale, email, codice_azienda, password_hash, id_azienda) "
        + "SELECT codice_ente, codice_matricola, cognome, nome, codice_fiscale, email, codice_azienda,password_hash, id_azienda "
        + "FROM ribaltone_dati.fonte_aggiunta_anagrafica "
        + "WHERE codice_azienda = ?1", nativeQuery = true)
    public void fromFonteAggiuntaToDatiImportati(String codiceAzienda);

    @Modifying
    @Query(value = "DELETE FROM ribaltone_dati.dati_importati_anagrafica WHERE codice_azienda = ?1", nativeQuery = true)
    public void deleteAllByCodiceAzienda(String codiceAzienda);

}
