package it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.data;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author MicheleD'Onza
 */
public class ColonneImportazioneCSVRibaltoneEnums {

    public static enum ColonneAppartenente implements ColonneImportazioneCSVRibaltone {
        codiceMatricola(Arrays.asList("codiceMatricola", "codice matricola")),
        nome(Arrays.asList("nome")),
        codiceFiscale(Arrays.asList("codiceFiscale", "codice fiscale")),
        idCasella(Arrays.asList("idCasella", "id casella")),
        datain(Arrays.asList("datain", "data inizio")),
        datafi(Arrays.asList("datafi", "data fine")),
        tipoAppartenenza(Arrays.asList("tipoAppartenenza", "tipo appartenenza")),
        username(Arrays.asList("username")),
        dataAssunzione(Arrays.asList("dataAssunzione", "data assunzione")),
        dataDimissione(Arrays.asList("dataDimissione", "data dimissione")),
        resposabile(Arrays.asList("resposabile")),
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
        idCasella(Arrays.asList("idCasella", "id casella")),
        idPadre(Arrays.asList("idPadre", "id casella padre", "id padre")),
        descrizione(Arrays.asList("descrizione")),
        datain(Arrays.asList("datain", "data inizio")),
        datafi(Arrays.asList("datafi", "data fine")),
        tipoLegame(Arrays.asList("tipoLegame", "tipo legame")),
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
        codiceMatricola(Arrays.asList("codiceMatricola", "codice matricola")),
        cognome(Arrays.asList("cognome")),
        nome(Arrays.asList("nome")),
        codiceFiscale(Arrays.asList("codiceFiscale", "cf", "Codice Fiscale")),
        email(Arrays.asList("email", "e-mail")),
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
        progressivoRiga(Arrays.asList("progressivoRiga", "progressivo riga")),
        idCasellaPartenza(Arrays.asList("idCasellaPartenza", "id casella partenza")),
        idCasellaArrivo(Arrays.asList("idCasellaArrivo", "id casella arrivo")),
        dataTrasformazione(Arrays.asList("dataTrasformazione", "data trasformazione")),
        motivo(Arrays.asList("motivo")),
        datainPartenza(Arrays.asList("datainPartenza", "data inizio casella di partenza")),
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
