package it.bologna.ausl.internauta.utils.ribaltone;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportare;
import it.bologna.ausl.internauta.utils.ribaltone.cache.RibaltoneCache;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.CSVDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.CSVSpecificData;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.GruDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.GruSpecificData;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.FonteAggiuntaDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SourceDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author Top
 */
public class RibaltoneManagerUtils {

    private static final Logger log = LoggerFactory.getLogger(RibaltoneManagerUtils.class);

//    public static UserReportManager importDataAndGenerateUserReportWithCache(ObjectMapper objectMapper, EntityManager entityManager, String codiceAzienda, ConfigRibaltoneView configRibaltoneView, RepositoryFactory repositoryFactory) throws RibaltoneHttpException, JsonProcessingException {
//        RibaltoneDataConfiguration ribaltoneConf = getRibaltoneConf(entityManager, (String) configRibaltoneView.getFonteSelezionata());
//        RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), entityManager);
//        OperationsCacheManager operationsCacheManager = new OperationsCacheManager(ribaltoneCache, objectMapper);
//
//        DatiDaImportare datiDaImportareValidated = getSourceDataAndValidateAndTransfer(objectMapper, codiceAzienda, ribaltoneConf, repositoryFactory);
//        datiDaImportareValidated.transfer();
//        OperationsManager operationsManager = new OperationsManager(
//            datiDaImportareValidated,
//            codiceAzienda,
//            configRibaltoneView.getTolleranzaAppartenenti(),
//            configRibaltoneView.getTolleranzaStrutture(),
//            repositoryFactory);
//        Operations operations = operationsManager.buildOperations();
//        //controllo sul numero minimo di dati
//        operationsManager.isQuantitaDatiOk();
//        operationsCacheManager.dump(operations);
//
//        return operations.generateUserReport(UserReport.UserReportType.HTML);
//    }
    public static RibaltoneDataConfiguration getRibaltoneConf(EntityManager entityManager, String idConfiguration) throws RibaltoneHttpException {
        RibaltoneDataConfiguration ribaltoneConf = entityManager.find(RibaltoneDataConfiguration.class, idConfiguration);
        if (ribaltoneConf == null) {
            throw new RibaltoneHttpException("parametro ribaltoneConf non trovato Questo non puo accadere!");
        }
        return ribaltoneConf;
    }

    public static RibaltoneCache getRibaltoneCache(ObjectMapper objectMapper, HashMap<String, Object> cacheConfig, EntityManager entityManager) throws RibaltoneHttpException {
        RibaltoneCache ribaltoneCache = RibaltoneCache.build(cacheConfig, objectMapper, entityManager);
        if (ribaltoneCache == null) {
            throw new RibaltoneHttpException("parametro ribaltoneConf non trovato Questo non puo accadere!");
        }
        return ribaltoneCache;
    }

    public static DatiDaImportare getSourceDataAndValidateAndTransfer(ObjectMapper objectMapper, String codiceAzienda, RibaltoneDataConfiguration ribaltoneConf, RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        //recupero i dati da dove dice la conf
        DatiDaImportare sourceData = getSourceData(objectMapper, codiceAzienda, ribaltoneConf, repositoryFactory);
        DatiDaImportare datiDaImportareValidated = sourceData.validate();
        datiDaImportareValidated.transfer();
        return datiDaImportareValidated;
    }

    private static DatiDaImportare getSourceData(ObjectMapper objectMapper, String codiceAzienda, RibaltoneDataConfiguration ribaltoneConf, RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        SourceDataManager sourceDataManager;
        List<DatiDaImportareAppartenente> appartenenti;
        List<DatiDaImportareAnagrafica> anagrafiche;
        List<DatiDaImportareStruttura> strutture;
        List<DatiDaImportareTrasformazione> trasformazioni;
        Integer progressivoUltimaTrasformazione;
        // NB: in JPQL si deve usare il nome dell'entità Java, in questo caso Azienda
        Azienda idAzienda = repositoryFactory.getEntityManager().createQuery("select a from Azienda a where codice = :codice", Azienda.class)
            .setParameter("codice", codiceAzienda)
            .getSingleResult();
        switch (ribaltoneConf.getFonte()) {
            case "GRU" -> {
                GruSpecificData gruSpecificData = objectMapper.convertValue(ribaltoneConf.getSpecifiche(), GruSpecificData.class);

                if (idAzienda == null) {
                    throw new RibaltoneHttpException("impossibile trovare l'azienda corrispondente");
                } else {
                    sourceDataManager = new GruDataManager(gruSpecificData, objectMapper, codiceAzienda, idAzienda.getId());
                    appartenenti = sourceDataManager.getAppartenenti();
                    anagrafiche = sourceDataManager.getAnagrafica();
                    strutture = sourceDataManager.getStrutture();
                    trasformazioni = sourceDataManager.getTrasformazioni();
                    progressivoUltimaTrasformazione = gruSpecificData.getQueryRecuperoDati().getProgressivoUltimaTrasformazione();

                }
            }
            case "CSV" -> {
                CSVSpecificData csvSpecificData = objectMapper.convertValue(ribaltoneConf.getSpecifiche(), CSVSpecificData.class);
                if (idAzienda == null) {
                    throw new RibaltoneHttpException("impossibile trovare l'azienda corrispondente");
                } else {
                    sourceDataManager = new CSVDataManager(csvSpecificData, objectMapper, codiceAzienda, idAzienda.getId(), repositoryFactory.getEntityManager());
                    appartenenti = sourceDataManager.getAppartenenti();
                    anagrafiche = sourceDataManager.getAnagrafica();
                    strutture = sourceDataManager.getStrutture();
                    trasformazioni = sourceDataManager.getTrasformazioni();
                    progressivoUltimaTrasformazione = csvSpecificData.getQueryRecuperoDati().getProgressivoUltimaTrasformazione();
                }
            }

            case "ASTRA" ->
                throw new RibaltoneHttpException("astra non ancora implementato");
            default ->
                throw new AssertionError();
        }
        FonteAggiuntaDataManager fonteAggiuntaDataManager = new FonteAggiuntaDataManager(null, objectMapper, codiceAzienda, null, repositoryFactory.getEntityManager());
        List<DatiDaImportareAppartenente> fonteAggiuntaAppartenenti = fonteAggiuntaDataManager.getAppartenenti();
        List<DatiDaImportareAnagrafica> fonteAggiuntaAnagrafica = fonteAggiuntaDataManager.getAnagrafica();
        //List<DatiDaImportareStruttura> fonteAggiuntaStrutture = fonteAggiuntaDataManager.getStrutture();
        //List<DatiDaImportareTrasformazione> fonteAggiuntaTrasformazioni = fonteAggiuntaDataManager.getTrasformazioni();
        appartenenti = unisciListeUnichePerCodiceFiscaleIdCasella(appartenenti, fonteAggiuntaAppartenenti);
        anagrafiche = mergeAnagraficheListsOverrideOnCodiceFiscale(anagrafiche, fonteAggiuntaAnagrafica);
        //strutture = mergeDatiDaImportareStruttureListsOnConflicIdCasellaExpandIntervallo(strutture, fonteAggiuntaStrutture);
        DatiDaImportare datiDaImportare = new DatiDaImportare(anagrafiche, strutture, appartenenti, trasformazioni, progressivoUltimaTrasformazione, repositoryFactory, idAzienda.getId());
        return datiDaImportare;
    }

    public static List<DatiDaImportareAppartenente> unisciListeUnichePerCodiceFiscaleIdCasella(
        List<DatiDaImportareAppartenente> appartenenti,
        List<DatiDaImportareAppartenente> fonteAggiuntaAppartenenti) {

        // Mappa con chiave composta: codiceFiscale_idCasella
        Map<String, DatiDaImportareAppartenente> mappa = new HashMap<>();

        // Aggiungiamo prima tutti gli elementi della lista1
        for (DatiDaImportareAppartenente item : appartenenti) {
            mappa.putIfAbsent(item.getKey(), item);  // non sovrascrive se già presente
        }

        // Aggiungiamo (sovrascrivendo) gli elementi della lista2
        for (DatiDaImportareAppartenente item : fonteAggiuntaAppartenenti) {
            mappa.put(item.getKey(), item);  // sovrascrive
        }

        return new ArrayList<>(mappa.values());
    }

    private static List<DatiDaImportareAnagrafica> mergeAnagraficheListsOverrideOnCodiceFiscale(
        List<DatiDaImportareAnagrafica> lista1,
        List<DatiDaImportareAnagrafica> lista2) {

        Map<String, DatiDaImportareAnagrafica> mappaPerCodiceFiscale = new HashMap<>();

        // Prima lista: inserisce gli elementi nella mappa
        for (DatiDaImportareAnagrafica item : lista1) {
            if (item.getCodiceFiscale() != null) {
                mappaPerCodiceFiscale.put(item.getCodiceFiscale(), item);
            }
        }

        // Seconda lista: inserisce (sovrascrive) gli elementi nella mappa
        for (DatiDaImportareAnagrafica item : lista2) {
            if (item.getCodiceFiscale() != null) {
                mappaPerCodiceFiscale.put(item.getCodiceFiscale(), item); // sovrascrive se esiste
            }
        }

        // Ritorna una nuova lista con i valori uniti
        return new ArrayList<>(mappaPerCodiceFiscale.values());
    }

    public static List<DatiDaImportareStruttura> mergeDatiDaImportareStruttureListsOnConflicIdCasellaExpandIntervallo(
        List<DatiDaImportareStruttura> lista1,
        List<DatiDaImportareStruttura> lista2) {

        Map<Integer, DatiDaImportareStruttura> mappaPerIdCasella = new HashMap<>();

        // Inseriamo tutti gli elementi della prima lista
        for (DatiDaImportareStruttura item : lista1) {
            if (item.getIdCasella() != null) {
                mappaPerIdCasella.put(item.getIdCasella(), item);
            }
        }

        // Gestiamo gli elementi della seconda lista
        for (DatiDaImportareStruttura nuovo : lista2) {
            Integer idCasella = nuovo.getIdCasella();
            if (idCasella == null) {
                continue;
            }

            if (mappaPerIdCasella.containsKey(idCasella)) {

                // Costruzione del record unito lo faccio perche magari mi sfugge qualcosa
                // ma secondo me va preso solo il nuovo
                DatiDaImportareStruttura unito = new DatiDaImportareStruttura();
                unito.setIdCasella(idCasella);
                unito.setIdPadre(nuovo.getIdPadre());
                unito.setDescrizione(nuovo.getDescrizione());
                unito.setTipoLegame(nuovo.getTipoLegame());
                unito.setCodiceEnte(nuovo.getCodiceEnte());
                unito.setCodiceAzienda(nuovo.getCodiceAzienda());
                unito.setIdAzienda(nuovo.getIdAzienda());

                // Gestione intervallo temporale
                unito.setDatain(nuovo.getDatain());
                unito.setDatafi(nuovo.getDatafi());

                mappaPerIdCasella.put(idCasella, unito);
            } else {
                // Non presente nella prima lista → aggiungilo
                mappaPerIdCasella.put(idCasella, nuovo);
            }
        }

        return new ArrayList<>(mappaPerIdCasella.values());
    }

    public static void updateProgressivoUltimaTrasformazione(String fonte, String codiceEnte, RepositoryFactory repositoryFactory) {
        List<DatiDaImportareTrasformazione> trasformazioniEseguite = repositoryFactory.getDatiDaImportareTrasformazioneRepository().findAll();
        if (trasformazioniEseguite != null && !trasformazioniEseguite.isEmpty()) {
            Integer max = trasformazioniEseguite.stream().mapToInt(DatiDaImportareTrasformazione::getId).max().orElse(0);
            if (max != 0) {
                String sql = """
                    UPDATE ribaltone_dati.configuration t
                    SET specifiche = jsonb_set(
                            t.specifiche,
                            '{queryRecuperoDati,progressivoUltimaTrasformazione}',
                            to_jsonb(
                               ?
                            ),
                            false
                        )
                    WHERE t.id = ?;
                    """;
                repositoryFactory.getJdbcTemplate()
                    .update(sql, max, fonte);

            }
        }

    }

    public static void setOmonimiaOnUtentiOmonimi(RepositoryFactory repositoryFactory, String codiceAzienda) {
        String updateOmonimiaTrue = """
            UPDATE baborg.persone p
            SET omonimia = true
            WHERE p.attiva = true
              AND p.omonimia = false
              AND EXISTS (
                SELECT 1
                FROM baborg.persone p2
                WHERE p2.id != p.id
                  AND p2.attiva = true
                  AND p2.descrizione = p.descrizione
              )
        """;
        String updateOmonimiaFalse = """
            UPDATE baborg.persone p
              SET omonimia = false
              WHERE p.omonimia = true
                AND NOT EXISTS (
                  SELECT 1
                  FROM baborg.persone p2
                  WHERE p2.id != p.id
                    AND p2.attiva = true
                    AND p2.descrizione = p.descrizione
                )
        """;
        repositoryFactory.getEntityManager().createNativeQuery(updateOmonimiaTrue).executeUpdate();
        repositoryFactory.getEntityManager().createNativeQuery(updateOmonimiaFalse).executeUpdate();
    }

    public static void setFogliaOnStrutture(RepositoryFactory repositoryFactory) {
        String updateForglia = """
                              UPDATE baborg.strutture s
                              SET foglia = NOT EXISTS (
                                SELECT 1
                                FROM baborg.strutture c
                                WHERE c.id_struttura_padre = s.id AND c.attiva
                              ) WHERE attiva;
                              """;
        repositoryFactory.getEntityManager().createNativeQuery(updateForglia).executeUpdate();
    }

    public static void disableTrigger(RepositoryFactory repositoryFactory) {
        String[] triggerDaDisabilitare = {
            "ALTER TABLE permessi.permessi DISABLE TRIGGER set_permessi_impliciti_da_permesso;",
            "ALTER TABLE permessi.permessi DISABLE TRIGGER aggiungi_rimuovi_passaggio;",
            "ALTER TABLE permessi.permessi DISABLE TRIGGER default_attivo_dal;",
            "ALTER TABLE permessi.permessi DISABLE TRIGGER copia_permessi_su_entita_unificate;",
            "ALTER TABLE permessi.permessi DISABLE TRIGGER z_finally_unify_permission_trigger;",
            "ALTER TABLE permessi.permessi DISABLE TRIGGER aggiungi_rimuovi_pool_figlio_connesso;"
        };
        for (String trigger : triggerDaDisabilitare) {
            repositoryFactory.getEntityManager().createNativeQuery(trigger).executeUpdate();

        }
    }

    public static void enableTrigger(RepositoryFactory repositoryFactory) {
        String[] triggerDaDisabilitare = {
            "ALTER TABLE permessi.permessi ENABLE TRIGGER set_permessi_impliciti_da_permesso;",
            "ALTER TABLE permessi.permessi ENABLE TRIGGER aggiungi_rimuovi_passaggio;",
            "ALTER TABLE permessi.permessi ENABLE TRIGGER default_attivo_dal;",
            "ALTER TABLE permessi.permessi ENABLE TRIGGER copia_permessi_su_entita_unificate;",
            "ALTER TABLE permessi.permessi ENABLE TRIGGER z_finally_unify_permission_trigger;",
            "ALTER TABLE permessi.permessi ENABLE TRIGGER aggiungi_rimuovi_pool_figlio_connesso;"
        };
        for (String trigger : triggerDaDisabilitare) {
            repositoryFactory.getEntityManager().createNativeQuery(trigger).executeUpdate();

        }
    }

    public static void eliminaProtocontatti(RepositoryFactory repositoryFactory) {
        repositoryFactory.getEntityManager().createNativeQuery("select rubrica.elimina_protocontatti()").getSingleResult();
    }
}
