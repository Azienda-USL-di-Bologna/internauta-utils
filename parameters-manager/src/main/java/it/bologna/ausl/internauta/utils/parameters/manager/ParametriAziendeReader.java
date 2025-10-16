package it.bologna.ausl.internauta.utils.parameters.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanTemplate;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.utils.parameters.manager.repositories.ParametroAziendeRepository;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.configurazione.QParametroAziende;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Classe che contiene tutti i metodi per leggere i parametri_aziende.
 *
 *
 * @author gdm
 */
@Component
public class ParametriAziendeReader {

    /**
     * TO DO: Popolare questo enum mano a mano che vengono utilizzati i vari
     * parametri, così da averli tutti raccolti in un punto.
     */
    public enum ParametriAzienda {
        minIOConfig,
        minIOTrashAndDownloadCleaner,
        minIOServiceBucketsCleaner,
        mongoConfig,
        mongoAndMinIOActive,
        firmaRemota,
        firmaRemotaConfiguration,
        fascicoliSAI,
        downloader,
        versatoreConfiguration,
        attivitaMailSender,
        usaGediInternauta,
        chiusuraArchivio,
        ricalcoloPermessiArchivi,
        usaRitentaVersamentiAutomatico,
        inadConfiguration,
        escludiArchiviChiusiFromAbilitazioniMassiveGedi,
        loginConfig,
        babelshareConfig,
        oliammConfiguration,
        tabRegistrazioniScriptaActive,
        firmaJNJ,
        ribaltoneConf,
        processaDati,
        usaNuovoRibaltoneVeloce,
        abilitaGestioneMassivaArchiviPerResponsabiliAndVicari,
        maxSchedePerFiltriEdiEfiWithAutoMode,
        notificaGestioneMassivaArchiviPerUtentiCoinvolti
    }

    @Autowired
    @Qualifier(value = "ParametroAziendeRepositoryParametersManager")
    private ParametroAziendeRepository parametroAziendeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public ParametriAziendeReader() {
    }

    public List<ParametroAziende> getParameters(String nome) {
        return getParameters(nome, null, null);
    }

    public List<ParametroAziende> getParameters(ParametriAzienda nome) {
        return this.getParameters(nome.toString(), null, null);
    }

    public List<ParametroAziende> getParameters(ParametriAzienda nome, Integer[] idAziende, String[] idApplicazioni) {
        return this.getParameters(nome.toString(), idAziende, idApplicazioni);
    }

    public List<ParametroAziende> getParameters(ParametriAzienda nome, String[] idApplicazioni) {
        return this.getParameters(nome.toString(), null, idApplicazioni);
    }

    public List<ParametroAziende> getParameters(String nome, Integer[] idAziende) {
        return getParameters(nome, idAziende, null);
    }

    public List<ParametroAziende> getParameters(ParametriAzienda nome, Integer[] idAziende) {
        return getParameters(nome.toString(), idAziende, null);
    }

    public List<ParametroAziende> getParameters(String nome, String[] idApplicazioni) {
        return getParameters(nome, null, idApplicazioni);
    }

    /**
     * Metodo che prende in ingresso
     *
     * @param nome
     * @param idAziende
     * @param idApplicazioni
     *                       e restituisce
     * @return List di ParametroAziende
     *         Se non si esplicitano le aziende o le applicazioni, non viene applicato il filtro su quei due campi.
     */
    public List<ParametroAziende> getParameters(String nome, Integer[] idAziende, String[] idApplicazioni) {
        BooleanExpression filter = QParametroAziende.parametroAziende.nome.eq(nome);
        if (idAziende != null) {
            BooleanTemplate filterAzienda = Expressions.booleanTemplate("cast(tools.array_overlap({0}, tools.string_to_integer_array({1}, ',')) as boolean)=true",
                QParametroAziende.parametroAziende.idAziende, org.apache.commons.lang3.StringUtils.join(idAziende, ","));
            filter = filter.and(filterAzienda.or(QParametroAziende.parametroAziende.idAziende.isNull()));
        }
        if (idApplicazioni != null) {
            BooleanTemplate filterApplicazioni = Expressions.booleanTemplate("cast(tools.array_overlap({0}, string_to_array({1}, ',')) as boolean)=true",
                QParametroAziende.parametroAziende.idApplicazioni, org.apache.commons.lang3.StringUtils.join(idApplicazioni, ","));
            filter = filter.and(filterApplicazioni.or(QParametroAziende.parametroAziende.idApplicazioni.isNull()));
        }

        Iterable<ParametroAziende> parametriFound = parametroAziendeRepository.findAll(filter);

        List<ParametroAziende> res = new ArrayList();
        parametriFound.forEach(res::add);
        return res;
    }

    /**
     * Metodo per estrarre tutti i parametri di un'applicazione, in una determinata azienda.Tipico di un processo di inizializzazione.
     * Combina il filtro dell'azienda, che deve esserci, con quello dell'applicazione, se presente nel db.
     * Se app è null allora il filtro viene ignorato e vengono presi i parametri indipendentemene dal valore della colonna id_applicazione
     *
     * @param app
     * @param idAzienda
     * @param includeHiddenFromApi
     * @return mappa di nome-valore dei parametri
     */
    public Map<String, Object> getAllAziendaApplicazioneParameters(String app, Integer idAzienda, boolean includeHiddenFromApi) {

        BooleanTemplate filterAzienda = Expressions.booleanTemplate(
            "cast(tools.array_overlap({0}, tools.string_to_integer_array({1}, ',')) as boolean)=true",
            QParametroAziende.parametroAziende.idAziende, idAzienda.toString());
        BooleanExpression filterAziendaOrNull = filterAzienda.or(QParametroAziende.parametroAziende.idAziende.isNull());

        BooleanExpression applicazioniEmptyArray = Expressions.TRUE;
        BooleanExpression applicazioniOverlap = Expressions.TRUE;
        BooleanExpression applicazioniIsNull = Expressions.TRUE;

        if (app != null) {
            applicazioniOverlap = Expressions.booleanTemplate(
                "cast(tools.array_overlap({0}, string_to_array({1}, ',')) as boolean)=true",
                QParametroAziende.parametroAziende.idApplicazioni, app);

            applicazioniIsNull = QParametroAziende.parametroAziende.idApplicazioni.isNull();
            applicazioniEmptyArray = Expressions.booleanTemplate("cast (cardinality({0}) as integer) = 0", QParametroAziende.parametroAziende.idApplicazioni);
        }

        BooleanExpression filter = filterAziendaOrNull.and(applicazioniOverlap
            .or(applicazioniEmptyArray)
            .or(applicazioniIsNull));
        if (!includeHiddenFromApi) {
            BooleanExpression onlyVisibleOnApi = QParametroAziende.parametroAziende.hideFromApi.eq(false);
            filter = filter.and(onlyVisibleOnApi);
        }

        Iterable<ParametroAziende> parametriFound = parametroAziendeRepository.findAll(filter);
        Map<String, Object> hashMapParams = new HashMap();

        if (parametriFound != null) {
            for (ParametroAziende parametroAziende : parametriFound) {
                hashMapParams.put(parametroAziende.getNome(), parametroAziende.getValore());
            }
        }

        return hashMapParams;
    }

    /**
     * Metodo per estrarre tutti i parametri di una azienda. Tipico di un processo di inizializzazione.
     *
     * @param idAzienda
     * @param includeHiddenFromApi
     * @return mappa di nome-valore dei parametri
     */
    public Map<String, Object> getAllAziendaParameters(Integer idAzienda, boolean includeHiddenFromApi) {

        return getAllAziendaApplicazioneParameters(null, idAzienda, includeHiddenFromApi);
    }

    /**
     * Estrae il valore del parametro e lo converte nel tipo passato
     *
     * @param <T>
     * @param parametroAziende
     * @param valueType
     * @return
     */
    public <T extends Object> T getValue(ParametroAziende parametroAziende, Class<T> valueType) {
        try {
            return objectMapper.readValue(parametroAziende.getValore(), valueType);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    /**
     * Estrae il valore del parametro e lo converte nel tipo passato
     *
     * @param <T>
     * @param parametroAziende
     * @param typeReference
     * @return
     */
    public <T extends Object> T getValue(ParametroAziende parametroAziende, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(parametroAziende.getValore(), typeReference);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
