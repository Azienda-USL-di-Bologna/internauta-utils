package it.bologna.ausl.internauta.utils.ribaltone.plugin.csv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.model.entities.tip.data.ColonneImportazioneOggetto;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.data.ColonneImportazioneCSVRibaltone;
import it.bologna.ausl.internauta.utils.ribaltone.utils.service.ConversionServices;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.bologna.ausl.model.entities.ribaltonedati.CSVDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.CSVDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.CSVDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.CSVDaImportareTrasformazione;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.ConversionService;

/**
 *
 * @author MicheleD'Onza
 */
public class CsvImportManager {

    private static final Logger log = LoggerFactory.getLogger(CsvImportManager.class);

    private ObjectMapper objectMapper;
    private EntityManager entityManager;
    private ConversionService conversionService;

    public CsvImportManager(ObjectMapper objectMapper, EntityManager entityManager, ConversionService conversionService) {
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
        this.conversionService = conversionService;
    }

    public void csvImportAndValidate(
        String separatore,
        File csv,
        TipologiaCsv tipologia,
        String codiceAzienda
    ) throws RibaltoneHttpException, IOException {
        List<String> notFoundHeaders = validateCsvColums(
            separatore,
            csv,
            tipologia
        );

        if (notFoundHeaders != null && !notFoundHeaders.isEmpty()) {
            String errorMessage = String.format("non sono stati trovati i seguenti headers: %s . Controllare che la tipologia selezionata sia quella corretta.", Arrays.toString(notFoundHeaders.toArray()));
            throw new RibaltoneHttpException(errorMessage);
        }
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QAzienda qAzienda = QAzienda.azienda;
        Azienda azienda = queryFactory.select(qAzienda).from(qAzienda).where(qAzienda.codice.eq(codiceAzienda)).fetchFirst();
        validateDataAndImport(tipologia, csv, csv.getName(), separatore, azienda);
    }

    private List<String> validateCsvColums(String separatore, File csv, TipologiaCsv tipologia) throws FileNotFoundException, IOException {
        try (
            Reader csvReader = new FileReader(csv); CSVParser csvParser = getCSVParser(csvReader, separatore);) {
            List<String> headersNotFound = new ArrayList<>();
            List<String> headerNames = csvParser.getHeaderNames();

            Class aEnum = ColonneImportazioneCSVRibaltone.getColumnsEnum(tipologia);
            Object[] enumConstants = aEnum.getEnumConstants();
            ColonneImportazioneCSVRibaltone[] columns = (ColonneImportazioneCSVRibaltone[]) enumConstants;
            for (ColonneImportazioneCSVRibaltone column : columns) {
                if (!column.equals(column.getErroriColumn())) {
                    boolean headerFound = headerNames.stream().anyMatch(h -> h.equalsIgnoreCase(column.toString()) || column.getValue().contains(h.toLowerCase()));
                    if (!headerFound) {
                        headersNotFound.add(column.toString());
                    }
                }
            }
            return headersNotFound;
        }
    }

    private void validateDataAndImport(TipologiaCsv tipologia, File csvFile, String name, String separatore, Azienda azienda) throws FileNotFoundException, IOException {
        try (
            Reader csvReader = new FileReader(csvFile); CSVParser csvParser = getCSVParser(csvReader, separatore);) {
            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> csvRowMap = buildCsvRowMap(csvParser, csvRecord);
                switch (tipologia) {
                    case APPARTENENTI -> {
                        CSVDaImportareAppartenente csvDaImportareAppartenente = buildImportazioneCSVRow(tipologia, csvRowMap);
                        String errore = validateAppartenente(csvRowMap);
                        csvDaImportareAppartenente.setErrore(errore);
                        csvDaImportareAppartenente.setIdAzienda(azienda.getId());
                        csvDaImportareAppartenente.setCodiceAzienda(azienda.getCodice());
                        entityManager.persist(csvDaImportareAppartenente);
                    }
                    case STRUTTURE -> {
                        CSVDaImportareStruttura csvDaImportareStruttura = buildImportazioneCSVRow(tipologia, csvRowMap);
                        String errore = validateStruttura(csvRowMap);
                        csvDaImportareStruttura.setErrore(errore);
                        csvDaImportareStruttura.setIdAzienda(azienda.getId());
                        csvDaImportareStruttura.setCodiceAzienda(azienda.getCodice());
                        entityManager.persist(csvDaImportareStruttura);
                    }
                    case ANAGRAFICHE -> {
                        CSVDaImportareAnagrafica csvDaImportareAnagrafica = buildImportazioneCSVRow(tipologia, csvRowMap);
                        String errore = validateAnagrafica(csvRowMap);
                        csvDaImportareAnagrafica.setErrore(errore);
                        csvDaImportareAnagrafica.setIdAzienda(azienda.getId());
                        csvDaImportareAnagrafica.setCodiceAzienda(azienda.getCodice());
                        entityManager.persist(csvDaImportareAnagrafica);
                    }
                    case TRASFORMAZIONI -> {
                        CSVDaImportareTrasformazione csvDaImportareTrasformazione = buildImportazioneCSVRow(tipologia, csvRowMap);
                        String errore = validateTrasformazione(csvRowMap);
                        csvDaImportareTrasformazione.setErrore(errore);
                        csvDaImportareTrasformazione.setIdAzienda(azienda.getId());
                        csvDaImportareTrasformazione.setCodiceAzienda(azienda.getCodice());
                        entityManager.persist(csvDaImportareTrasformazione);
                    }
                    default ->
                        throw new AssertionError();
                }
            }
        }
    }

    private CSVParser getCSVParser(Reader fileReader, String separatore) throws FileNotFoundException, IOException {
        CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.builder()
            .setDelimiter(separatore)
            .setQuote('"')
            .setQuoteMode(QuoteMode.MINIMAL)
            .setRecordSeparator("\r\n")
            .setAllowMissingColumnNames(true)
            .setHeader().build());
        return csvParser;
    }

    private Map<String, String> buildCsvRowMap(CSVParser csvParser, CSVRecord csvRecord) {
        Map<String, String> res = new HashMap<>();
        for (Map.Entry<String, Integer> entry : csvParser.getHeaderMap().entrySet()) {
            String header = entry.getKey();
            int columnIndex = entry.getValue();
            String value = csvRecord.get(columnIndex);
            res.put(header, value);
        }
        return res;
    }

    private String validateAppartenente(Map<String, String> csvRowMap) {
        String erroreRiga = "";
        if (csvRowMap.get("codiceMatricola") == null) {
            erroreRiga = erroreRiga + "cordice matricola assente; ";
        }
        if (csvRowMap.get("nome") == null) {
            erroreRiga = erroreRiga + "nome assente; ";
        }
        if (csvRowMap.get("cognome") == null) {
            erroreRiga = erroreRiga + "cognome assente; ";
        }
        if (csvRowMap.get("codiceFiscale") == null) {
            erroreRiga = erroreRiga + "codice fiscale assente; ";
        }
        if (csvRowMap.get("idCasella") == null) {
            erroreRiga = erroreRiga + "idCasella assente; ";
        }
        if (csvRowMap.get("datain") == null) {
            erroreRiga = erroreRiga + "data di inizio afferenza assente; ";
        }
        if (csvRowMap.get("tipoAppartenenza") == null
            || !csvRowMap.get("tipoAppartenenza").equalsIgnoreCase("T")
            || !csvRowMap.get("tipoAppartenenza").equalsIgnoreCase("F")) {
            erroreRiga = erroreRiga + "data di inizio afferenza assente o non accettata; ";
        }
        return erroreRiga;
    }

    private String validateStruttura(Map<String, String> csvRowMap) {
        String erroreRiga = "";
        if (csvRowMap.get("idCasella") == null) {
            erroreRiga = erroreRiga + "idCasella assente; ";
        }
        if (csvRowMap.get("idPadre") == null) {
            erroreRiga = erroreRiga + "idPadre assente; ";
        }
        if (csvRowMap.get("descrizione") == null) {
            erroreRiga = erroreRiga + "nome della struttura assente; ";
        }
        if (csvRowMap.get("datain") == null) {
            erroreRiga = erroreRiga + "data di inizio afferenza assente; ";
        }
        return erroreRiga;
    }

    private String validateAnagrafica(Map<String, String> csvRowMap) {
        String erroreRiga = "";
        if (csvRowMap.get("codiceMatricola") == null) {
            erroreRiga = erroreRiga + "codiceMatricola assente; ";
        }
        if (csvRowMap.get("cognome") == null) {
            erroreRiga = erroreRiga + "cognome assente; ";
        }
        if (csvRowMap.get("nome") == null) {
            erroreRiga = erroreRiga + "nome assente; ";
        }
        if (csvRowMap.get("codiceFiscale") == null) {
            erroreRiga = erroreRiga + "codice fiscale assente; ";
        }
        if (csvRowMap.get("email") == null) {
            erroreRiga = erroreRiga + "email assente; ";
        }
        return erroreRiga;
    }

    private String validateTrasformazione(Map<String, String> csvRowMap) {
        String erroreRiga = "";
        if (csvRowMap.get("progressivoRiga") == null) {
            erroreRiga = erroreRiga + "progressivoRiga assente; ";
        }
        if (csvRowMap.get("idCasellaPartenza") == null) {
            erroreRiga = erroreRiga + "id Casella di Partenza assente; ";
        }
        if (csvRowMap.get("idCasellaArrivo") == null) {
            erroreRiga = erroreRiga + "idCasellaArrivo assente; ";
        }
        if (csvRowMap.get("dataTrasformazione") == null) {
            erroreRiga = erroreRiga + "data Trasformazione assente; ";
        }
        if (csvRowMap.get("motivo") == null || csvRowMap.get("motivo").equalsIgnoreCase("X")) {
            erroreRiga = erroreRiga + "motivo assente o diverso da X; ";
        }
        if (csvRowMap.get("datainPartenza") == null) {
            erroreRiga = erroreRiga + "data inizio della casella di partenza assente; ";
        }

        return erroreRiga;
    }

    private <T extends DatiRibaltoneInterface> T buildImportazioneCSVRow(TipologiaCsv tipologia, Map<String, String> csvRowMap) {
        // istanzia la corretta classe a seconda della tipologia
        DatiRibaltoneInterface rigaCsvDatiRibaltone = DatiRibaltoneInterface.getImportazioneCSVRibaltoneImpl(tipologia);

        // questo oggetto permette di settare il valore di un campo, conoscendone il nome e il valore (senza dover chiamare direttaemente la funzione setter)
        BeanWrapper wrapper = new BeanWrapperImpl(rigaCsvDatiRibaltone);

        /*
        per ogni header bisogna capire in che campo della classe ImportazioneOggetto scriverlo. Per farlo viene usato un enum che ha come chiave il nome del campo
        della classe e come valori i possibili nomi degli header associati
         */
        wrapper.setConversionService(conversionService);
        for (String headerName : csvRowMap.keySet()) {
            // reperisce il valore enum corretto a seconda dell'header
            ColonneImportazioneCSVRibaltone colonnaEnum = ColonneImportazioneCSVRibaltone.findKey(headerName, tipologia);
            if (colonnaEnum != null) {
                /*
                se trovo il campo, ne setto il valore tramite il wrapper istanziato prima
                ignorando la colonna errore cosi che se qualcuno carica il csv di errore
                noi lo prendiamo bene lo stesso
                 */
                if (colonnaEnum != colonnaEnum.getErroriColumn()) {

                    Object convertIfNecessary = wrapper.convertIfNecessary(csvRowMap.get(headerName), wrapper.getPropertyDescriptor(colonnaEnum.toString()).getPropertyType());

                    wrapper.setPropertyValue(colonnaEnum.toString(), convertIfNecessary);
                }
            } else { // se non lo trovo, stampo un errore e lo ignoro
                log.error(String.format("header csv %s non previsto dal tracciato, il campo sarà ignorato", headerName));
            }
        }
        return (T) wrapper.getWrappedInstance();
    }
}
