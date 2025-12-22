/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAppartenente;
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
@Component("DatiImportatiAppartenenteRepository")
@RepositoryRestResource(collectionResourceRel = "datiimportatiappartenente", path = "datiimportatiappartenente", exported = false)
public interface DatiImportatiAppartenenteRepository extends QuerydslPredicateExecutor<DatiImportatiAppartenente>, JpaRepository<DatiImportatiAppartenente, Integer> {

    public List<DatiImportatiAppartenente> findByCodiceAzienda(String codiceAzienda);

    @Modifying
    @Query(value = """
        INSERT INTO ribaltone_dati.dati_importati_appartenenti
        (codice_ente, codice_matricola, cognome, nome, codice_fiscale, id_casella, datain, datafi, tipo_appartenenza, username, responsabile, data_assunzione, data_dimissione, codice_azienda, id_azienda)
        SELECT codice_ente, codice_matricola, cognome, nome, codice_fiscale, id_casella, datain, datafi, tipo_appartenenza, username, responsabile, data_assunzione, data_dimissione, codice_azienda, id_azienda
        FROM ribaltone_dati.dati_da_importare_appartenenti WHERE codice_azienda =?1
        """, nativeQuery = true)
    public void fromDatiDaImportareToDatiImportati(String codiceAzienda);

    @Modifying
    @Query(value = """
        INSERT INTO ribaltone_dati.dati_importati_appartenenti
        (codice_ente, codice_matricola, cognome, nome, codice_fiscale, id_casella, datain, datafi, tipo_appartenenza, username, responsabile, data_assunzione, data_dimissione, codice_azienda, id_azienda)
        SELECT codice_ente, codice_matricola, cognome, nome, codice_fiscale, id_casella, datain, datafi, tipo_appartenenza, username, responsabile, data_assunzione, data_dimissione, codice_azienda, id_azienda
        FROM ( SELECT distinct a.id,a.version,a.codice_ente, a.codice_matricola, a.cognome, a.nome, a.codice_fiscale, a.id_casella, a.tipo_appartenenza, a.username, a.responsabile, a.codice_azienda, a.id_azienda, a.datain, a.datafi, a.data_assunzione, a.data_dimissione, a.errore
        FROM ribaltone_dati.csv_da_importare_strutture s
        JOIN ribaltone_dati.csv_da_importare_appartenenti a ON s.id_casella = a.id_casella
        WHERE  s.datain < now() AND (s.datafi IS NULL OR s.datafi > now()) AND a.codice_azienda = ?1 AND a.datain < now() AND (a.datafi IS NULL OR a.datafi > now()))
        """, nativeQuery = true)
    public void fromCSVDaImportareToDatiImportati(String codiceAzienda);

    @Modifying
    @Query(value = """
        INSERT INTO ribaltone_dati.dati_importati_appartenenti
        (codice_ente, codice_matricola, cognome, nome, codice_fiscale, id_casella, datain, datafi, tipo_appartenenza, username, responsabile, data_assunzione, data_dimissione, codice_azienda, id_azienda)
        SELECT codice_ente, codice_matricola, cognome, nome, codice_fiscale, id_casella, datain, datafi, tipo_appartenenza, username, responsabile, data_assunzione, data_dimissione, codice_azienda, id_azienda
        FROM ribaltone_dati.fonte_aggiunta_appartenenti WHERE codice_azienda =?1 AND  datain < now() AND (datafi IS NULL OR datafi > now())
        """, nativeQuery = true)
    public void fromFonteAggiuntaToDatiImportati(String codiceAzienda);

    @Modifying
    @Query(value = "DELETE FROM ribaltone_dati.dati_importati_appartenenti WHERE codice_azienda = ?1", nativeQuery = true)
    public void deleteAllByCodiceAzienda(String codiceAzienda);

}
