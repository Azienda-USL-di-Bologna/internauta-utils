/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiTrasformazione;
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
@Component("DatiImportatiTrasformazioneRepository")
@RepositoryRestResource(collectionResourceRel = "datiimportatitrasformazione", path = "datiimportatitrasformazione", exported = false)
public interface DatiImportatiTrasformazioneRepository extends QuerydslPredicateExecutor<DatiImportatiTrasformazione>, JpaRepository<DatiImportatiTrasformazione, Integer> {

    public List<DatiImportatiTrasformazione> findByCodiceAzienda(String codiceAzienda);

    public DatiImportatiTrasformazione findTopByCodiceAziendaOrderByProgressivoRigaDesc(String codiceAzienda);

    @Modifying
    @Query(value = "INSERT INTO ribaltone_dati.dati_importati_trasformazioni "
        + "(progressivo_riga, id_casella_partenza, id_casella_arrivo, data_trasformazione, motivo, datain_partenza, dataora_oper, codice_ente, codice_azienda, id_azienda) "
        + "SELECT progressivo_riga, id_casella_partenza, id_casella_arrivo, data_trasformazione, motivo, datain_partenza, dataora_oper, codice_ente, codice_azienda, id_azienda "
        + "FROM ribaltone_dati.csv_da_importare_trasformazioni WHERE codice_azienda = ?1", nativeQuery = true)
    public void fromCSVDaImportareToDatiImportati(String codiceAzienda);

    @Modifying
    @Query(value = "INSERT INTO ribaltone_dati.dati_importati_trasformazioni "
        + "(progressivo_riga, id_casella_partenza, id_casella_arrivo, data_trasformazione, motivo, datain_partenza, dataora_oper, codice_ente, codice_azienda, id_azienda) "
        + "SELECT progressivo_riga, id_casella_partenza, id_casella_arrivo, data_trasformazione, motivo, datain_partenza, dataora_oper, codice_ente, codice_azienda, id_azienda "
        + "FROM ribaltone_dati.dati_da_importare_trasformazioni WHERE codice_azienda = ?1", nativeQuery = true)
    public void fromDatiDaImportareToDatiImportati(String codiceAzienda);

    @Modifying
    @Query(value = "INSERT INTO ribaltone_dati.dati_importati_trasformazioni "
        + "(progressivo_riga, id_casella_partenza, id_casella_arrivo, data_trasformazione, motivo, datain_partenza, dataora_oper, codice_ente, codice_azienda, id_azienda) "
        + "SELECT progressivo_riga, id_casella_partenza, id_casella_arrivo, data_trasformazione, motivo, datain_partenza, dataora_oper, codice_ente, codice_azienda, id_azienda "
        + "FROM ribaltone_dati.fonte_aggiunta_trasformazioni WHERE codice_azienda = ?1", nativeQuery = true)
    public void fromFonteAggiuntaToDatiImportati(String codiceAzienda);

    @Modifying
    @Query(value = "DELETE FROM ribaltone_dati.dati_importati_trasformazioni WHERE codice_azienda = ?1", nativeQuery = true)
    public void deleteAllByCodiceAzienda(String codiceAzienda);

}
