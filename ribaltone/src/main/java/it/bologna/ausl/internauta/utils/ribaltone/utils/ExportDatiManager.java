package it.bologna.ausl.internauta.utils.ribaltone.utils;

import com.querydsl.core.Tuple;
import java.util.HashMap;
import java.util.List;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import java.io.File;
import java.io.FileWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.io.CsvMapWriter;
import java.util.Map;

/**
 *
 * @author Top
 */
@Component
public class ExportDatiManager {

    private static final Logger log = LoggerFactory.getLogger(ExportDatiManager.class);

    public static File buildCSV(List<Tuple> listaTuple, DatiRibaltoneInterface.TipologiaCsv tipo) {
        log.info("sto generando il csv del tipo" + tipo);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
        String nameCsv = sdf.format(timestamp) + "_" + tipo + ".csv";
        File csvFile = new File(System.getProperty("java.io.tmpdir" + "/csv/"), nameCsv);
        csvFile.deleteOnExit();
        CsvPreference SEMICOLON_DELIMITED = new CsvPreference.Builder('"', ';', "\r\n").build();
        Map<String, Object> row = new HashMap<>();

        try (CsvMapWriter mapWriter = new CsvMapWriter(new FileWriter(csvFile), SEMICOLON_DELIMITED)) {
            String[] headers = headersGenerator(tipo);
            List<Map<String, Object>> elementi = RibaltoneUtils.getMapListFromTupleList(headers, listaTuple);

            CellProcessor[] processorsTipo = getProcessors(tipo);
            mapWriter.writeHeader(headers);
            for (Map<String, Object> elemento : elementi) {
                row.putAll(elemento);
                if (elemento.get("datain") != null && !elemento.get("datain").toString().trim().equals("")) {
                    if (Instant.class.isAssignableFrom(elemento.get("datain").getClass())) {
                        row.put("datain", Timestamp.from((Instant) elemento.get("datain")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("datafi") != null && !elemento.get("datafi").toString().trim().equals("")) {
                    if (Instant.class.isAssignableFrom(elemento.get("datafi").getClass())) {
                        row.put("datafi", Timestamp.from((Instant) elemento.get("datafi")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("data_assunzione") != null && !elemento.get("data_assunzione").toString().trim().equals("")) {
                    if (Instant.class.isAssignableFrom(elemento.get("data_assunzione").getClass())) {
                        row.put("data_assunzione", Timestamp.from((Instant) elemento.get("data_assunzione")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("data_dimissione") != null && !elemento.get("data_dimissione").toString().trim().equals("")) {
                    if (Instant.class.isAssignableFrom(elemento.get("data_dimissione").getClass())) {
                        row.put("data_dimissione", Timestamp.from((Instant) elemento.get("data_dimissione")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("data_trasformazione") != null && !elemento.get("data_trasformazione").toString().trim().equals("")) {
                    if (Instant.class.isAssignableFrom(elemento.get("data_trasformazione").getClass())) {
                        row.put("data_trasformazione", Timestamp.from((Instant) elemento.get("data_trasformazione")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("datain_partenza") != null && !elemento.get("datain_partenza").toString().trim().equals("")) {
                    if (Instant.class.isAssignableFrom(elemento.get("datain_partenza").getClass())) {
                        row.put("datain_partenza", Timestamp.from((Instant) elemento.get("datain_partenza")).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    }
                }
                if (elemento.get("dataora_oper") != null && !elemento.get("dataora_oper").toString().trim().equals("")) {
                    if (Instant.class.isAssignableFrom(elemento.get("dataora_oper").getClass())) {
                        row.put("dataora_oper", Timestamp.from((Instant) elemento.get("dataora_oper")).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
                    }
                }

                mapWriter.write(row, headers, processorsTipo);
                row.clear();
            }

        } catch (Exception e) {
            log.error("ho fallito miseramente", e);
            System.out.println("e" + e);
            return null;
        }
        return csvFile;

    }

    /**
     * Sets up the processors used for APPARTENENTI, RESPONSABILI, STRUTTURA,
     * TRASFORMAZIONI. There are 4 tables. Empty columns are read as null (hence
     * the NotNull() for mandatory columns).
     *
     * @return the cell processors
     */
    private static CellProcessor[] getProcessors(DatiRibaltoneInterface.TipologiaCsv tipo) {
        CellProcessor[] cellProcessor = null;
        log.info("sto generando i processor del tipo" + tipo);
        switch (tipo) {
            case APPARTENENTI -> {
                final CellProcessor[] processorsAPPARTENENTI = new CellProcessor[]{
                    // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
                    new Optional(), // codice_ente
                    new Optional(), // codice_matricola Non Bloccante
                    new Optional(), // cognome Bloccante
                    new Optional(), // nome Bloccante
                    new Optional(), // codice_fiscale bloccante
                    new Optional(), // id_casella bloccante
                    new Optional(), // datain bloccante
                    new Optional(), // datafi
                    new Optional(), // tipo_appartenenza bloccante
                    new Optional(), // username
                    new Optional(), // data_assunzione bloccante
                    new Optional() // data_adimissione
                };
                cellProcessor = processorsAPPARTENENTI;
            }
            
            case TRASFORMAZIONI -> {
                CellProcessor[] processorsTRASFORMAZIONI = new CellProcessor[]{
                    new Optional(), // progressivo_riga
                    new Optional(), // id_casella_partenza
                    new Optional(), // id_casellla_arrivo
                    new Optional(), // data_trasformazione
                    new Optional(), // motivo
                    new Optional(), // datain_partenza
                    new Optional(), // dataora_oper
                    new Optional() // codice_ente
                };
                cellProcessor = processorsTRASFORMAZIONI;
            }

            case STRUTTURE -> {
                final CellProcessor[] processorsSTRUTTURA = new CellProcessor[]{
                    new Optional(), // id_casella
                    new Optional(), // id_padre
                    new Optional(), // descrizione
                    new Optional(), // datain
                    new Optional(), // datafi
                    new Optional(), // tipo_legame
                    // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
                    new Optional() // codice_ente
                };
                cellProcessor = processorsSTRUTTURA;
            }
            case ANAGRAFICHE -> {
                final CellProcessor[] processorsANAGRAFICA = new CellProcessor[]{
                    // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
                    new Optional(), // codice_ente
                    new Optional(), // codice_matricola Non Bloccante
                    new Optional(), // cognome Bloccante
                    new Optional(), // nome Bloccante
                    new Optional(), // codice_fiscale bloccante
                    new Optional(), // EMAIL bloccante
                };
                cellProcessor = processorsANAGRAFICA;
            }
            default -> System.out.println("non dovrebbe essere altro tipo di tabella");
        }
        return cellProcessor;
    }

    private static String[] headersGenerator(DatiRibaltoneInterface.TipologiaCsv tipo) {
        log.info("sto generando l'header del tipo" + tipo);
        String[] headers = null;
        switch (tipo) {
            case APPARTENENTI ->
                headers = new String[]{"codice_ente", "codice_matricola", "cognome",
                    "nome", "codice_fiscale", "id_casella", "datain", "datafi", "tipo_appartenenza",
                    "username", "data_assunzione", "data_dimissione"};

            case STRUTTURE ->
                headers = new String[]{"id_casella", "id_padre", "descrizione",
                    "datain", "datafi", "tipo_legame", "codice_ente"};
            case TRASFORMAZIONI ->
                headers = new String[]{"progressivo_riga", "id_casella_partenza", "id_casella_arrivo", "data_trasformazione",
                    "motivo", "datain_partenza", "dataora_oper", "codice_ente"};
            case ANAGRAFICHE ->
                headers = new String[]{"codice_ente", "codice_matricola", "cognome",
                    "nome", "codice_fiscale", "email"};
            default ->
                System.out.println("Il tipo non corrisponde a nessuno di quelli possibili");
        }
        return headers;
    }

  

}
