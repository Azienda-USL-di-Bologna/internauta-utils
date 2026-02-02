package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.projections.generated.DatiImportatiStrutturaWithPlainFields;
import it.nextsw.common.data.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
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
 *
 */
@Component("DatiImportatiStrutturaRepository")
@RepositoryRestResource(collectionResourceRel = "datiimportatistruttura", path = "datiimportatistruttura", exported = false)
@NextSdrRepository(repositoryPath = "${ribaltone.mapping.url.root}/datiimportatistruttura", defaultProjection = DatiImportatiStrutturaWithPlainFields.class)
public interface DatiImportatiStrutturaRepository extends
    QuerydslPredicateExecutor<DatiImportatiStruttura>,
    JpaRepository<DatiImportatiStruttura, Integer>,
    NextSdrQueryDslRepository<DatiImportatiStruttura, Integer, QDatiImportatiStruttura> {

    public List<DatiImportatiStruttura> findByCodiceAzienda(String codiceAzienda);

    @Modifying
    @Query(value = "INSERT INTO ribaltone_dati.dati_importati_strutture "
        + "(id_casella, id_padre, descrizione, datain, datafi, tipo_legame, codice_ente, codice_azienda,id_azienda) "
        + "SELECT id_casella, id_padre, descrizione,datain, datafi, tipo_legame,  codice_ente, codice_azienda, id_azienda "
        + "FROM ribaltone_dati.csv_da_importare_strutture "
        + "WHERE codice_azienda=?1 AND (datafi IS NULL OR datafi > now()) AND (datain < now() or datain is null)", nativeQuery = true)
    public void fromCSVDaImportareToDatiImportati(String codiceAzienda);

    @Modifying
    @Query(value = "INSERT INTO ribaltone_dati.dati_importati_strutture "
        + "(id_casella, id_padre, descrizione, datain, datafi, tipo_legame, codice_ente, codice_azienda,id_azienda) "
        + "SELECT id_casella, id_padre, descrizione,datain, datafi, tipo_legame,  codice_ente, codice_azienda, id_azienda "
        + "FROM ribaltone_dati.dati_da_importare_strutture "
        + "WHERE codice_azienda=?1", nativeQuery = true)
    public void fromDatiDaImportareToDatiImportati(String codiceAzienda);

    @Modifying
    @Query(value = "INSERT INTO ribaltone_dati.dati_importati_strutture "
        + "(id_casella, id_padre, descrizione, datain, datafi, tipo_legame, codice_ente, codice_azienda,id_azienda) "
        + "SELECT id_casella, id_padre, descrizione,datain, datafi, tipo_legame,  codice_ente, codice_azienda, id_azienda "
        + "FROM ribaltone_dati.fonte_aggiunta_strutture "
        + "WHERE codice_azienda=?1 AND (datafi IS NULL OR datafi > now()) AND datain < now()", nativeQuery = true)
    public void fromFonteAggiuntaToDatiImportati(String codiceAzienda);

    @Modifying
    @Query(value = "DELETE FROM ribaltone_dati.dati_importati_strutture WHERE codice_azienda = ?1", nativeQuery = true)
    public void deleteAllByCodiceAzienda(String codiceAzienda);

}
