package it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.data;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author MicheleD'Onza
 */
public class ColonneImportazioneCSVRibaltoneEnums {

    public static enum ColonneAppartenente implements ColonneImportazioneCSVRibaltone {
        codiceMatricola(Arrays.asList("codiceMatricola", "codice matricola", "codice_matricola")),
        nome(Arrays.asList("nome")),
        cognome(Arrays.asList("cognome")),
        codiceEnte(Arrays.asList("codiceEnte", "codice_ente")),
        codiceFiscale(Arrays.asList("codiceFiscale", "codice fiscale", "codice_fiscale")),
        idCasella(Arrays.asList("idCasella", "id casella", "id_casella")),
        tipoAppartenenza(Arrays.asList("tipoAppartenenza", "tipo appartenenza", "tipo_appartenenza")),
        username(Arrays.asList("username")),
        responsabile(Arrays.asList("responsabile")),
        datain(Arrays.asList("dataIn", "data attivazione", "data inizio")),
        datafi(Arrays.asList("dataFi", "data cessazione", "data fine")),
        errori(Arrays.asList("errori"));

        private final List<String> valuesList;

        ColonneAppartenente(List<String> values) {
            this.valuesList = values;
        }

        /**
         * torna la lista dei valori associati alla chiave enum (di fatto sono i
         * nomi degli header che rappresentano il campo dell'entità)
         *
         * @return la lista dei valori associati alla chiave enum
         */
        @Override
        public List<String> getValue() {
            return valuesList;
        }

        @Override
        public ColonneImportazioneCSVRibaltone getErroriColumn() {
            return errori;
        }
    }

    public static enum ColonneStruttura implements ColonneImportazioneCSVRibaltone {
        idCasella(Arrays.asList("idCasella", "id casella", "id_casella")),
        idPadre(Arrays.asList("idPadre", "id casella padre", "id padre", "id_padre")),
        descrizione(Arrays.asList("descrizione")),
        tipoLegame(Arrays.asList("tipoLegame", "tipo legame", "tipo_legame")),
        datain(Arrays.asList("dataIn", "data attivazione", "data inizio", "datain")),
        datafi(Arrays.asList("dataFi", "data cessazione", "data fine", "datafi")),
        errori(Arrays.asList("errori"));

        private final List<String> valuesList;

        ColonneStruttura(List<String> values) {
            this.valuesList = values;
        }

        /**
         * torna la lista dei valori associati alla chiave enum (di fatto sono i
         * nomi degli header che rappresentano il campo dell'entità)
         *
         * @return la lista dei valori associati alla chiave enum
         */
        @Override
        public List<String> getValue() {
            return valuesList;
        }

        @Override
        public ColonneImportazioneCSVRibaltone getErroriColumn() {
            return errori;
        }
    }

    public static enum ColonneAnagrafica implements ColonneImportazioneCSVRibaltone {
        codiceMatricola(Arrays.asList("codiceMatricola", "codice matricola", "codice_matricola")),
        cognome(Arrays.asList("cognome")),
        nome(Arrays.asList("nome")),
        codiceFiscale(Arrays.asList("codiceFiscale", "cf", "Codice Fiscale", "codice_fiscale")),
        email(Arrays.asList("email", "e-mail", "e_mail")),
        errori(Arrays.asList("errori"));

        private final List<String> valuesList;

        ColonneAnagrafica(List<String> values) {
            this.valuesList = values;
        }

        /**
         * torna la lista dei valori associati alla chiave enum (di fatto sono i
         * nomi degli header che rappresentano il campo dell'entità)
         *
         * @return la lista dei valori associati alla chiave enum
         */
        @Override
        public List<String> getValue() {
            return valuesList;
        }

        @Override
        public ColonneImportazioneCSVRibaltone getErroriColumn() {
            return errori;
        }
    }

    public static enum ColonneTrasformazione implements ColonneImportazioneCSVRibaltone {
        progressivoRiga(Arrays.asList("progressivoRiga", "progressivo riga", "progressivo_riga")),
        idCasellaPartenza(Arrays.asList("idCasellaPartenza", "id casella partenza", "id_casella_partenza")),
        idCasellaArrivo(Arrays.asList("idCasellaArrivo", "id casella arrivo", "id_casella_arrivo")),
        dataTrasformazione(Arrays.asList("dataTrasformazione", "data trasformazione", "data_trasformazione")),
        dataOraOper(Arrays.asList("dataOraOper", "dataora_oper", "dataOraOper")),
        motivo(Arrays.asList("motivo")),
        datainPartenza(Arrays.asList("datainPartenza", "data inizio casella di partenza", "datain_partenza")),
        errori(Arrays.asList("errori"));

        private final List<String> valuesList;

        ColonneTrasformazione(List<String> values) {
            this.valuesList = values;
        }

        /**
         * torna la lista dei valori associati alla chiave enum (di fatto sono i
         * nomi degli header che rappresentano il campo dell'entità)
         *
         * @return la lista dei valori associati alla chiave enum
         */
        @Override
        public List<String> getValue() {
            return valuesList;
        }

        @Override
        public ColonneImportazioneCSVRibaltone getErroriColumn() {
            return errori;
        }
    }
}
