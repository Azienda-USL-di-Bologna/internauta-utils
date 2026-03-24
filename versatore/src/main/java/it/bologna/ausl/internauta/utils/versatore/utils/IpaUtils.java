package it.bologna.ausl.internauta.utils.versatore.utils;

import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatorePluginException;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.data.Row;
import org.sql2o.data.Table;

/**
 * classe utile per prendere i dati della PAI dal db IPA
 * @author boria
 */
public class IpaUtils {

    private static final Logger log = LoggerFactory.getLogger(IpaUtils.class);

    /**
    Restituisce una mappa che ha come chiave un id contatto e come valore i dati della pai contenuti nel db ipa a cui la funzione si collega
    @param contattiList lista di contatti di cui si vogliono sapere i dati ipa
    @param url
    @param user
    @param password
    @return
    @throws VersatorePluginException
     */
    public static Map<Integer, Object> getIpaMap(List<Contatto> contattiList, String url, String user, String password) throws VersatorePluginException {
        Map<Integer, Object> ipaMap = new HashMap<>();
        //voglio i contatti che siano effettivamente solo PAI
        List<Contatto> contattiPaiList = contattiList
            .stream()
            .filter(contattoObj -> contattoObj.getTipo().equals(Contatto.TipoContatto.PUBBLICA_AMMINISTRAZIONE_ITALIANA))
            .collect(Collectors.toList());
        if (contattiPaiList != null && !contattiPaiList.isEmpty()) {
            for (Contatto contatto : contattiPaiList) {
                //l'id esterno di un contatto ottenuto tramite syncIPA è composto in modo da avere concatenati (secondo una sua logica) i codici della pai.
                //uso questa funzione per ottenerli
                Map<String, String> codiciIPAeDescrizioni = getCodiciIPAbyIdEsterno(contatto.getIdEsterno());
                String cod_amm = codiciIPAeDescrizioni.get("cod_amm");
                String cod_aoo = codiciIPAeDescrizioni.get("cod_aoo");
                String cod_ou = codiciIPAeDescrizioni.get("cod_ou");
                Sql2o sql2o = new Sql2o(url, user, password);
                //connessione a db IPA
                try (Connection conn = sql2o.open()) {
                    //in base ai codici che sono riuscito ad avere dall'id_esterno del contatto posso interrogare il db ipa con query differenti
                    if (StringUtils.hasText(cod_amm)) {
                        if (StringUtils.hasText(cod_ou)) {
                            Table codiciEdescrizioniTable = conn.createQuery(
                                "SELECT ao.cod_aoo, ao.des_aoo, a.des_amm, o.des_ou, o.cod_uni_ou "
                                + "FROM ipa.ou o "
                                + "LEFT JOIN ipa.aoo ao ON o.cod_aoo = ao.cod_aoo AND o.cod_amm = ao.cod_amm "
                                + "JOIN ipa.amministrazioni a ON o.cod_amm = a.cod_amm "
                                + "WHERE o.cod_amm = :cod_amm AND o.cod_ou = :cod_ou"
                            )
                                .addParameter("cod_amm", cod_amm)
                                .addParameter("cod_ou", cod_ou)
                                .executeAndFetchTable();
                            Row codiciEdescrizioniRow = codiciEdescrizioniTable.rows().get(0);
                            codiciIPAeDescrizioni.put("cod_aoo", codiciEdescrizioniRow.getString("cod_aoo"));
                            codiciIPAeDescrizioni.put("des_aoo", titleCase(codiciEdescrizioniRow.getString("des_aoo")));
                            codiciIPAeDescrizioni.put("des_amm", titleCase(codiciEdescrizioniRow.getString("des_amm")));
                            codiciIPAeDescrizioni.put("des_ou", titleCase(codiciEdescrizioniRow.getString("des_ou")));
                            codiciIPAeDescrizioni.put("cod_uni_ou", codiciEdescrizioniRow.getString("cod_uni_ou"));
                        } else if (StringUtils.hasText(cod_aoo)) {
                            Table codiciEdescrizioniTable = conn.createQuery(
                                "SELECT a.des_amm, ao.des_aoo "
                                + "FROM ipa.amministrazioni a "
                                + "JOIN ipa.aoo ao ON a.cod_amm = ao.cod_amm "
                                + "WHERE ao.cod_amm = :cod_amm"
                            )
                                .addParameter("cod_amm", cod_amm)
                                .executeAndFetchTable();
                            Row codiciEdescrizioniRow = codiciEdescrizioniTable.rows().get(0);
                            codiciIPAeDescrizioni.put("des_aoo", titleCase(codiciEdescrizioniRow.getString("des_aoo")));
                            codiciIPAeDescrizioni.put("des_amm", titleCase(codiciEdescrizioniRow.getString("des_amm")));
                        } else {
                            String des_amm = conn.createQuery(
                                "SELECT des_amm "
                                + "FROM ipa.amministrazioni "
                                + "WHERE cod_amm = :cod_amm"
                            )
                                .addParameter("cod_amm", cod_amm)
                                .executeScalar(String.class);
                            codiciIPAeDescrizioni.put("des_amm", titleCase(des_amm));
                            if (!StringUtils.hasText(des_amm)) {
                                codiciIPAeDescrizioni.put("des_amm", titleCase(contatto.getDescrizione()));
                            }
                        }
                    } else {
                        log.error("Codice Amministrazione non indicato");
                        throw new VersatorePluginException("Codice Amministrazione non indicato");
                    }
                }
                ipaMap.put(contatto.getId(), codiciIPAeDescrizioni);
            }
        }
        return ipaMap;
    }

    /**
     * Parsa una stringa nei due formati supportati:
     * - Formato 1: "amm=cc|ou=041831"  → {cod_amm: "cc", cod_ou: "041831"}
     * - Formato 2: "istsc_mops030002|fanti_aoo" → {cod_amm: "istsc_mops030002", cod_aoo: "fanti_aoo"}
     */
    public static Map<String, String> getCodiciIPAbyIdEsterno(String id_esterno) {
        if (!StringUtils.hasText(id_esterno)) {
            throw new IllegalArgumentException("id_esterno non può essere nullo o vuoto");
        }

        // Formato 1: contiene '=' → "amm=cc|ou=041831"
        if (id_esterno.contains("=")) {
            return getMapCodiciOUeAmmByIdEsterno(id_esterno);
        }

        // Formato 2: "istsc_mops030002|fanti_aoo"
        if (id_esterno.contains("|")) {
            return getMapCodiciAOOeAmmByIdEsterno(id_esterno);
        }

        // Formato 3: stringa semplice senza '|' né '=' → "istsc_mops030002"
        return getMapCodiceAmmByIdesterno(id_esterno);
    }

    // Formato 1: "amm=valore|ou=valore"
    private static Map<String, String> getMapCodiciOUeAmmByIdEsterno(String id_esterno) {
        Map<String, String> result = new HashMap<>();
        String[] parti = id_esterno.split("\\|");
        for (String parte : parti) {
            String[] kv = parte.split("=", 2);
            if (kv.length != 2) {
                throw new IllegalArgumentException("Coppia chiave=valore non valida: " + parte);
            }
            String chiave = kv[0].trim().toLowerCase();
            String valore = kv[1].trim();

            if (chiave.equals("amm")) {
                result.put("cod_amm", valore);
            } else if (chiave.equals("ou")) {
                result.put("cod_ou", valore);
            }
        }
        if (!result.containsKey("cod_amm") || !result.containsKey("cod_ou")) {
            throw new IllegalArgumentException("Formato 1: mancano chiavi 'amm' e/o 'ou'");
        }
        return result;
    }

    // Formato 2: "amm_value|aoo_value"
    private static Map<String, String> getMapCodiciAOOeAmmByIdEsterno(String id_esterno) {
        String[] parti = id_esterno.split("\\|", 2);
        if (parti.length != 2) {
            throw new IllegalArgumentException("Formato 2: atteso 'amm|aoo', trovato: " + id_esterno);
        }

        Map<String, String> result = new HashMap<>();
        result.put("cod_amm", parti[0].trim());
        result.put("cod_aoo", parti[1].trim());
        return result;
    }

    private static Map<String, String> getMapCodiceAmmByIdesterno(String id_esterno) {
        Map<String, String> result = new HashMap<>();
        result.put("cod_amm", id_esterno.trim());
        return result;
    }

    public static String getDesAooDaDescrizioneContatto(String descrizioneContatto, String des_amm) {
        String completaUpper = descrizioneContatto.toUpperCase().trim();
        String daSottrarreUpper = des_amm.toUpperCase().trim();

        String risultato = completaUpper.replace(daSottrarreUpper, "");

        // Rimuove separatori " - " rimasti all'inizio o alla fine
        risultato = risultato.replaceAll("^\\s*-\\s*", "").replaceAll("\\s*-\\s*$", "").trim();

        return risultato;
    }

    public static String titleCase(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String[] parole = input.trim().toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (String parola : parole) {
            if (!parola.isEmpty()) {
                sb.append(Character.toUpperCase(parola.charAt(0)))
                    .append(parola.substring(1))
                    .append(" ");
            }
        }

        return sb.toString().trim();
    }

}
