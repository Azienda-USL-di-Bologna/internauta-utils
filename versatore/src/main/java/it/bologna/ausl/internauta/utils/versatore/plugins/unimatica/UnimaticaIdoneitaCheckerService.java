package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica;

import com.querydsl.core.util.StringUtils;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.IdoneitaChecker;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import static it.bologna.ausl.model.entities.scripta.Doc.TipologiaDoc.PROTOCOLLO_IN_ENTRATA;
import static it.bologna.ausl.model.entities.scripta.Doc.TipologiaDoc.PROTOCOLLO_IN_USCITA;
import static it.bologna.ausl.model.entities.scripta.Doc.TipologiaDoc.RGPICO;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author boria
 */
@Component
@Scope("prototype")
public class UnimaticaIdoneitaCheckerService extends IdoneitaChecker {

    private static final Logger log = LoggerFactory.getLogger(UnimaticaIdoneitaCheckerService.class);

    private final String UNIMATICA = "unimatica";
    private final String DATA_REGISTRAZIONE = "dataRegistrazione";
    private final String PROTOCOLLO = "protocollo";
    private final String DAL = "dal";
    private final String AL = "al";
    private final String DAYS = "days";
    private final String DIRECTION = "direction";
    private final String AFTER = "after";
    private final String BEFORE = "before";

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");

    // flag per stampare il range temporale una sola volta per esecuzione (checkDocImpl è per-doc)
    private boolean rangeLogged = false;

    @Override
    protected void finalize() throws Throwable {
        super.finalize(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public Boolean checkArchivioImpl(Integer id, Map<String, Object> params) throws VersatoreProcessingException {
        return false;
    }

    @Override
    public Boolean checkDocImpl(Integer id, Map<String, Object> params)
        throws VersatoreProcessingException {

        Doc doc = entityManager.find(Doc.class, id);

        // logga una sola volta il range temporale effettivamente letto dai parametri (utile per diagnosi)
        if (!rangeLogged) {
            logRangeTemporale(doc);
            rangeLogged = true;
        }

        log.debug("Sto calcolando l'idoneita del doc id " + id + ", registrato il " + doc.getDataRegistrazione());

        boolean idoneo = resolveIdoneitaByDateRange(doc);

        // I documenti pregressi non vengono mai versati
        if (Boolean.TRUE.equals(doc.getPregresso())) {
            idoneo = false;
        }

        if (idoneo) {
            log.info("Prendo da versare il documento id " + id + ", registrato il " + doc.getDataRegistrazione());
        }

        return idoneo;
    }

    /**
     * Controllo se viene passato un range di date in cui scegliere i documenti da versare.
     * Se il range non c'è pocedo col check.
     */
    private boolean resolveIdoneitaByDateRange(Doc doc) {
        //nel caso la mappa ritornata sia empty vuol dire che non sono state date condizioni di data, perciò, in quel caso, eseguo il controllo di idoneità sempre
        return getCurrentConfigMapByAzienda(doc, DATA_REGISTRAZIONE)
            .map(cond -> {
                String dal = (String) cond.get(DAL);
                String al = (String) cond.get(AL);
                return isInDateRange(doc.getDataRegistrazione(), dal, al)
                    ? checkIdoneitaDoc(doc)
                    : false;
            })
            .orElseGet(() -> checkIdoneitaDoc(doc));
    }

    /**
     * Controllo l'idoneità del doc in base alla tipologia
    @param doc
    @return
     */
    public Boolean checkIdoneitaDoc(Doc doc) {
        switch (doc.getTipologia()) {

            case RGPICO:
                // Gli RGPICO vengono versati sempre
                return true;

            case PROTOCOLLO_IN_ENTRATA:
            case PROTOCOLLO_IN_USCITA:
                return checkIdoneitaProtocollo(doc);

            default:
                return false;
        }
    }

    /**
     * Verifica l'idoneità per PROTOCOLLO_IN_ENTRATA / PROTOCOLLO_IN_USCITA.
     * Un protocollo deve avere almeno un'associazione archivio attiva e soddisfare
     * la condizione giorni-direzione configurata (se presente).
     */
    private boolean checkIdoneitaProtocollo(Doc doc) {
        //il protocollo deve essere fascicolato
        List<ArchivioDoc> activeArchivi = doc.getArchiviDocList()
            .stream()
            .filter(a -> a.getDataEliminazione() == null)
            .collect(Collectors.toList());

        if (activeArchivi.isEmpty()) {
            return false;
        }

        //se ho impostato delle condizioni temporali eseguo il controllo:
        //days sono i giorni da sottrarre alla data di oggi e la direzione (before/after) indica se prendere prima o dopo  di quella data.
        //ad esempio se la direzione è before e days 10 prendo quei doc registrati da almeno 10 giorni
        //se invece la direzione è after e days è 1 prenderò quelli registrati da non più di un giorno
        return getCurrentConfigMapByAzienda(doc, PROTOCOLLO, DATA_REGISTRAZIONE)
            .map(cond -> {
                Integer days = (Integer) cond.get(DAYS);
                String direction = (String) cond.get(DIRECTION);
                return isDateMatchingDirection(doc.getDataRegistrazione(), days, direction);
            })
            .orElse(true); // nessuna condizione configurata → sempre idoneo
    }

    /**
     * Controlla se la dataRegistrazione è nel range passato
     */
    private boolean isInDateRange(ZonedDateTime dataRegistrazione,
        String dal, String al) {
        // "dal" incluso: idoneo se registrato a partire da mezzanotte del giorno "dal" (confronto non stretto)
        boolean afterDal = StringUtils.isNullOrEmpty(dal)
            || !dataRegistrazione.isBefore(parseDate(dal));

        // "al" incluso: l'intera giornata "al" è valida, quindi il limite è mezzanotte del giorno successivo
        boolean beforeAl = StringUtils.isNullOrEmpty(al)
            || dataRegistrazione.isBefore(parseDate(al).plusDays(1));

        return afterDal && beforeAl;
    }

    /**
     * Restituisce true quando dataRegistrazione soddisfa la condizione
     * giorni-direzione configurata:  AFTER → la registrazione è successiva a (oggi - giorni);
     *                                BEFORE → la registrazione è precedente a (oggi - giorni).
     */
    private boolean isDateMatchingDirection(ZonedDateTime dataRegistrazione,
        Integer days, String direction) {
        ZonedDateTime threshold = ZonedDateTime.now().minusDays(days);

        switch (direction) {
            case AFTER:
                return dataRegistrazione.isAfter(threshold);
            case BEFORE:
                return dataRegistrazione.isBefore(threshold);
            default:
                log.warn("Direzione non riconosciuta: {}", direction);
                return false;
        }
    }

    /** Parses una data stringa ISO-8601 a midnight Rome time. */
    private ZonedDateTime parseDate(String date) {
        return LocalDate.parse(date).atStartOfDay(ROME);
    }

    // stampa il range temporale attivo per l'azienda del doc: dal/al (range assoluto) e days/direction (finestra relativa protocollo)
    private void logRangeTemporale(Doc doc) {
        String dal = getCurrentConfigMapByAzienda(doc, DATA_REGISTRAZIONE).map(c -> (String) c.get(DAL)).orElse(null);
        String al = getCurrentConfigMapByAzienda(doc, DATA_REGISTRAZIONE).map(c -> (String) c.get(AL)).orElse(null);
        Integer days = getCurrentConfigMapByAzienda(doc, PROTOCOLLO, DATA_REGISTRAZIONE).map(c -> (Integer) c.get(DAYS)).orElse(null);
        String direction = getCurrentConfigMapByAzienda(doc, PROTOCOLLO, DATA_REGISTRAZIONE).map(c -> (String) c.get(DIRECTION)).orElse(null);
        log.info("[Unimatica idoneità] Range temporale attivo (azienda {}): dal={}, al={}, protocollo days={}, direction={}", doc.getIdAzienda().getId(), dal, al, days, direction);
    }

    /**
     * Recupera le condizioni di idoneità configurate per l'azienda del doc,
     * cercandole sotto "unimatica" -> "&lt;idAzienda&gt;" -> chiavi passate.
     * Se per quell'azienda non è configurato nulla, ricade sulle condizioni
     * generiche del provider ("unimatica" -> chiavi passate).
     * @param doc il doc di cui si sta valutando l'idoneità, da cui si ricava l'azienda
     * @param keys le chiavi da navigare sotto il livello azienda
     * @return la mappa delle condizioni, empty se non configurate a nessuno dei due livelli
     */
    private Optional<Map<String, Object>> getCurrentConfigMapByAzienda(Doc doc, String... keys) {
        String idAzienda = String.valueOf(doc.getIdAzienda().getId());
        String[] keysAzienda = Stream.concat(Stream.of(UNIMATICA, idAzienda), Arrays.stream(keys)).toArray(String[]::new);
        String[] keysProvider = Stream.concat(Stream.of(UNIMATICA), Arrays.stream(keys)).toArray(String[]::new);

        Optional<Map<String, Object>> configAzienda = getCurrentConfigMap(keysAzienda);
        return configAzienda.isPresent() ? configAzienda : getCurrentConfigMap(keysProvider);
    }

    /**
     * Funzione che, partendo dai configParams, naviga le mappe fino ad arrivare all'ultima passata in firma
     * Ciclanedole restituisce quindi quella voluta risalendo la struttura
     * (es. "unimatica" -> "protocollo" -> "dataRegistrazione", ultima chiave passata e mappa che voglio come risultato)
     * Se un gradino manca restituisce empty
     */
    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> getCurrentConfigMap(String... keys) {
        if (configParams.getIdoneitaEligibilityConditionsParams() == null) {
            return Optional.empty();
        }

        Map<String, Object> currentMap = configParams.getIdoneitaEligibilityConditionsParams();

        for (String key : keys) {
            if (!(currentMap.get(key) instanceof Map)) {
                return Optional.empty();
            }
            currentMap = (Map<String, Object>) currentMap.get(key);
        }

        return Optional.of(currentMap);
    }

}
