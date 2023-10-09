package it.bologna.ausl.documentgenerator.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.documentgenerator.exceptions.Http500ResponseException;
import it.bologna.ausl.documentgenerator.exceptions.Sql2oSelectException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.data.Row;

/**
 *
 * @author guido
 */
public class BabelUtils {

    AziendaParamsManager aziendaParamsManager;

    ObjectMapper objectMapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(BabelUtils.class);

    public BabelUtils(AziendaParamsManager aziendaParamsManager, ObjectMapper objectMapper) {
        this.aziendaParamsManager = aziendaParamsManager;
        this.objectMapper = objectMapper;
    }

    /**
     *
     * @param codiceAzienda
     * @param codiceFiscale
     * @param ancheInattivi se passato null viene considerato false
     * @return mappa id_utente -> String, id_struttura -> String
     * @throws Sql2oSelectException se va qualcosa storto nella query o se trova
     * nessun o più di un record -> Vedere causaEccezione
     */
    public Map<String, String> getIdUtenteDaCf(String codiceAzienda, String codiceFiscale, Boolean ancheInattivi) throws Sql2oSelectException {

        if (ancheInattivi == null) {
            ancheInattivi = false;
        }

        Map<String, String> result = new HashMap<>();

        String query = "select id_utente, id_struttura from procton.utenti where cf ilike :cf";
        if (ancheInattivi == false) {
            query += " and attivo != 0";
        }

        List<Row> rows = null;

        try ( Connection conn = aziendaParamsManager.getDbConnection(codiceAzienda).open()) {
            rows = conn.createQuery(query)
                    .addParameter("cf", codiceFiscale)
                    .executeAndFetchTable().rows();
        } catch (Exception e) {
            throw new Sql2oSelectException("Errore nel reperimento del id_utente", e);
        }

        if (rows == null || rows.isEmpty()) {
            throw new Sql2oSelectException(Sql2oSelectException.SelectException.NESSUN_RISULTATO);
        } else if (rows.size() > 1) {
            throw new Sql2oSelectException(Sql2oSelectException.SelectException.PIU_RISULTATI);
        }

        // TODO: se l'id_struttura è null (utenti unificati) dovremmo andare a recuperarla dalle appartenenze funzionali
        result.put("id_utente", rows.get(0).getString("id_utente"));
        result.put("id_struttura", rows.get(0).getString("id_struttura"));

        return result;
    }

    /**
     *
     * @param codiceAzienda
     * @param classificazione
     * @return mappa id_titolo -> String, foglia -> Boolean
     * @throws Sql2oSelectException se va qualcosa storto nella query o se trova
     * nessun o più di un record -> Vedere causaEccezione
     */
    public Map<String, Object> getIdTitoloDaClassificazione(String codiceAzienda, String classificazione) throws Sql2oSelectException {

        Map<String, Object> result = new HashMap<>();

        String query = "select t.id_titolo, foglia "
                + "from procton.titoli t "
                + "join procton.titoli_versioni_cross vtc on vtc.id_titolo = t.id_titolo "
                + "where codice_gerarchico || codice_titolo = :classificazione "
                + "and vtc.id_versione = (select id_versione from procton.versioni_titoli where stato = 'C')";

        List<Row> rows = null;

        try ( Connection conn = aziendaParamsManager.getDbConnection(codiceAzienda).open()) {
            rows = conn.createQuery(query)
                    .addParameter("classificazione", classificazione)
                    .executeAndFetchTable().rows();
        } catch (Exception e) {
            throw new Sql2oSelectException("Errore nel reperimento dell'id_titolo", e);
        }

        if (rows == null || rows.isEmpty()) {
            throw new Sql2oSelectException(Sql2oSelectException.SelectException.NESSUN_RISULTATO);
        } else if (rows.size() > 1) {
            throw new Sql2oSelectException(Sql2oSelectException.SelectException.PIU_RISULTATI);
        }

        result.put("id_titolo", rows.get(0).getString("id_titolo"));
        result.put("foglia", (rows.get(0).getInteger("foglia") != 0));

        return result;
    }

    public Integer getIdTipoFascicoloDescrizione(String codiceAzienda, String descrizione) throws Sql2oSelectException {
        Integer idTipoFascicolo = null;
        try ( Connection conn = aziendaParamsManager.getDbConnection(codiceAzienda).open()) {
            String query = "select id_tipo_fascicolo from gd.tipi_fascicolo where tipo_fascicolo ilike :tipo_fascicolo";

            idTipoFascicolo = conn.createQuery(query)
                    .addParameter("tipo_fascicolo", descrizione)
                    .executeAndFetchFirst(Integer.class);
        } catch (Exception e) {
            LOGGER.error("Errore nel reperimento dell'id_tipo_fascicolo", e);
            throw new Sql2oSelectException("Errore nel reperimento dell'id_tipo_fascicolo", e);
        }

        if (idTipoFascicolo == null) {
            throw new Sql2oSelectException(Sql2oSelectException.SelectException.NESSUN_RISULTATO);
        }

        return idTipoFascicolo;
    }

    public List<Map<String, String>> getIndeIdAndGuid(String codiceAzienda, Integer size) throws UnsupportedEncodingException, IOException, Http500ResponseException, Sql2oSelectException {

        String urlChiamata = "";
        //urlChiamata = "https://gdml.internal.ausl.bologna.it/Indeutilities/GetIndeId";

        String queryIndeIdUrl = "SELECT val_parametro from bds_tools.parametri_pubblici "
                + "WHERE nome_parametro = :nome_parametro";

        try ( Connection conn = aziendaParamsManager.getDbConnection(codiceAzienda).open()) {
            urlChiamata = conn.createQuery(queryIndeIdUrl)
                    .addParameter("nome_parametro", "getIndeUrlServiceUri")
                    .executeAndFetchFirst(String.class);
        } catch (Exception e) {
            throw new Sql2oSelectException("Errore nel reperimento dell'url di chiamata per generare gli ID del fascicolo", e);
        }

        if ("".equals(urlChiamata) || urlChiamata == null) {
            throw new Sql2oSelectException(Sql2oSelectException.SelectException.NESSUN_RISULTATO);
        }

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("generateidnumber", size.toString());

        RequestBody formBody = formBuilder.build();

        OkHttpClient client = new OkHttpClient.Builder()
                .build();

        Request requestg = new Request.Builder()
                .url(urlChiamata)
                .post(formBody)
                .build();

        Response responseg = client.newCall(requestg).execute();

        if (!responseg.isSuccessful()) {
            throw new Http500ResponseException("500", "Errore nella chiamata alla Web-api");
        }

        List<Map<String, String>> idGuidList = new ArrayList<>();

        idGuidList = objectMapper.readValue(responseg.body().string(), List.class);

        return idGuidList;
    }

    public Boolean isFascicoloGiaPresente(String codiceAzienda, String idFascicoloOrigine, String applicazioneChiamante) throws Sql2oSelectException {

        Boolean giaPrensente = false;

        String query = "SELECT count(id_fascicolo) FROM gd.fascicoligd WHERE "
                + "id_fascicolo_app_origine = :id_fascicolo_app_origine AND servizio_creazione = :servizio_creazione";

        Integer count = null;

        try ( Connection conn = aziendaParamsManager.getDbConnection(codiceAzienda).open()) {
            count = conn.createQuery(query)
                    .addParameter("id_fascicolo_app_origine", idFascicoloOrigine)
                    .addParameter("servizio_creazione", applicazioneChiamante)
                    .executeAndFetchFirst(Integer.class);
        } catch (Exception e) {
            throw new Sql2oSelectException("Errore nel reperimento del fascicolo per verificare se è già presente", e);
        }

        if (count > 0) {
            giaPrensente = true;
        }

        return giaPrensente;
    }

    public Boolean isDocumentoGiaPresenteByIdDocEsterno(String codiceAzienda, Integer idDoc) throws Sql2oSelectException {
        Boolean giaPrensente = false;

        String query = "select count(id_documento) from procton.documenti_extras where "
                + "id_doc_esterno = :id_doc_esterno";

        Integer count = null;

        try ( Connection conn = aziendaParamsManager.getDbConnection(codiceAzienda).open()) {
            count = conn.createQuery(query)
                    .addParameter("id_doc_esterno", idDoc)
                    .executeAndFetchFirst(Integer.class);
        } catch (Exception e) {
            throw new Sql2oSelectException("Errore nel reperimento del documento per verificare se è già presente", e);
        }
        if (count > 0) {
            giaPrensente = true;
        }
        return giaPrensente;
    }

    public Boolean isDocumentoGiaPresenteByDocumentoEsterno(String codiceAzienda, String applicazioneChiamante, String numeroDocumentoOrigine, Integer annoDocumentoOrigine) throws Sql2oSelectException {
        Boolean giaPrensente = false;
        String query = "select count(id_documento) from procton.documenti where "
                + "anno_documento_origine = :anno_documento_origine and protocollo_esterno = :protocollo_esterno "
                + "and applicazione_origine::character varying = :applicazione_origine";

        Integer count = null;

        try ( Connection conn = aziendaParamsManager.getDbConnection(codiceAzienda).open()) {
            count = conn.createQuery(query)
                    .addParameter("anno_documento_origine", annoDocumentoOrigine)
                    .addParameter("protocollo_esterno", numeroDocumentoOrigine)
                    .addParameter("applicazione_origine", applicazioneChiamante)
                    .executeAndFetchFirst(Integer.class);
        } catch (Exception e) {
            throw new Sql2oSelectException("Errore nel reperimento del documento per verificare se è già presente", e);
        }
        if (count > 0) {
            giaPrensente = true;
        }
        return giaPrensente;
    }

    public String getIdFascicoloPerChiusura(String codiceAzienda, String numerazioneGerarchica, String idFascicoloOrigine,
            String applicazione) throws Sql2oSelectException {
        String idFascicoloGd = null;
        String query = "select id_fascicolo from gd.fascicoligd "
                + "where 1 = 1 "
                + (numerazioneGerarchica != null ? " and numerazione_gerarchica = :numerazione_gerarchica" : "")
                + (idFascicoloOrigine != null ? " and id_fascicolo_app_origine = :id_fascicolo_app_origine" : "")
                + (applicazione != null ? " and servizio_creazione = :applicazione_origine" : "");

        try ( Connection conn = aziendaParamsManager.getDbConnection(codiceAzienda).open()) {
            Query q = conn.createQuery(query);
            if (numerazioneGerarchica != null) {
                q = q.addParameter("numerazione_gerarchica", numerazioneGerarchica);
            }
            if (idFascicoloOrigine != null) {
                q = q.addParameter("id_fascicolo_app_origine", idFascicoloOrigine);
            }
            if (applicazione != null) {
                q = q.addParameter("applicazione_origine", applicazione);
            }
            System.out.println("Query ricerca fascicolo: " + q.toString());

            List<String> listaIdFascicoliGd = q.executeAndFetch(String.class);
            if (listaIdFascicoliGd.size() == 0) {
                throw new Sql2oSelectException(Sql2oSelectException.SelectException.NESSUN_RISULTATO, "Fascicolo non trovato!!");
            } else if (listaIdFascicoliGd.size() > 1) {
                throw new Sql2oSelectException(Sql2oSelectException.SelectException.PIU_RISULTATI, "Trovati più fascicoli!");
            } else {
                idFascicoloGd = listaIdFascicoliGd.get(0);
            }
        } catch (Exception e) {
            throw new Sql2oSelectException("Errore nel reperimento del fascicolo da chiudere", e);
        }

        return idFascicoloGd;
    }

    public String getQueryChiusuraFascicolo(String idFascicoloGd, String statoFascicolo) {
        String query = "update gd.fascicoligd where id_fascicolo = ";

        return query;
    }

    public boolean isSupportedMimeType(String codiceAzienda, String mimetype) {

        boolean result = false;

        String query = "SELECT mime_type "
                + "FROM bds_tools.file_supportati "
                + "WHERE mime_type = :mimetype";

        List<Row> rows = null;

        try ( Connection conn = aziendaParamsManager.getDbConnection(codiceAzienda).open()) {
            rows = conn.createQuery(query)
                    .addParameter("mimetype", mimetype)
                    .addColumnMapping("mime_type", "mimetype")
                    .addColumnMapping("convertibile_pdf", "convertibilePDF")
                    .addColumnMapping("estensione", "estensione")
                    .executeAndFetchTable().rows();

            if (rows.size() >= 1) {
                result = true;
            }
        }
        return result;
    }

}
