package it.bologna.ausl.internauta.utils.ribaltone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportare;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.cache.OperationsCacheManager;
import it.bologna.ausl.internauta.utils.ribaltone.cache.RibaltoneCache;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReport;
import it.bologna.ausl.internauta.utils.ribaltone.userreport.UserReportManager;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationTrasformazione;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationsManager;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.CSVDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.CSVSpecificData;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.CsvImportManager;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.GruDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.GruSpecificData;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.FonteAggiuntaDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SourceDataManager;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.configurazione.data.ConfigRibaltoneView;
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
import java.util.OptionalInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Top
 */
public class RibaltoneManagerUtils {

    private static final Logger log = LoggerFactory.getLogger(RibaltoneManagerUtils.class);

    public static UserReportManager importDataAndGenerateUserReportWithCache(ObjectMapper objectMapper, EntityManager entityManager, String codiceAzienda, ConfigRibaltoneView configRibaltoneView, RepositoryFactory repositoryFactory) throws RibaltoneHttpException, JsonProcessingException {
        RibaltoneDataConfiguration ribaltoneConf = getRibaltoneConf(entityManager, (String) configRibaltoneView.getFonteSelezionata());
        RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), entityManager);
        OperationsCacheManager operationsCacheManager = new OperationsCacheManager(ribaltoneCache, objectMapper);

        DatiDaImportare datiDaImportareValidated = getAndValidateSourceData(objectMapper, codiceAzienda, ribaltoneConf, repositoryFactory);
        OperationsManager operationsManager = new OperationsManager(
            datiDaImportareValidated,
            codiceAzienda,
            configRibaltoneView.getTolleranzaAppartenenti(),
            configRibaltoneView.getTolleranzaStrutture(),
            repositoryFactory);
        Operations operations = operationsManager.buildOperations();
        //controllo sul numero minimo di dati
        operationsManager.isQuantitaDatiOk();
        operationsCacheManager.dump(operations);

        return operations.generateUserReport(UserReport.UserReportType.HTML);
    }

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

    public static DatiDaImportare getAndValidateSourceData(ObjectMapper objectMapper, String codiceAzienda, RibaltoneDataConfiguration ribaltoneConf, RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
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
        List<DatiDaImportareAppartenente> lista1,
        List<DatiDaImportareAppartenente> lista2) {

        // Mappa con chiave composta: codiceFiscale_idCasella
        Map<String, DatiDaImportareAppartenente> mappa = new HashMap<>();

        // Aggiungiamo prima tutti gli elementi della lista1
        for (DatiDaImportareAppartenente item : lista1) {
            mappa.putIfAbsent(item.getKey(), item);  // non sovrascrive se già presente
        }

        // Aggiungiamo (sovrascrivendo) gli elementi della lista2
        for (DatiDaImportareAppartenente item : lista2) {
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
}
