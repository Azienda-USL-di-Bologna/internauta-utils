package it.bologna.ausl.internauta.utils.ribaltone.plugin.gru;

import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.oracledata.Anagrafica;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;

import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SourceDataManager;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import it.bologna.ausl.internauta.utils.ribaltone.pluginutils.SpecificData;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.oracledata.Appartenente;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.oracledata.Responsabile;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.oracledata.Struttura;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.oracledata.Trasformazione;
import it.bologna.ausl.internauta.utils.ribaltone.utils.RibaltoneUtils;
import java.util.List;
import java.util.Map;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import java.util.ArrayList;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Top
 */
public class GruDataManager extends SourceDataManager {

    private static final Logger LOG = LoggerFactory.getLogger(GruDataManager.class);

    private GruSpecificData gruSpecificData;

    public GruDataManager(SpecificData specificDataConf, ObjectMapper objectMapper, String codiceAzienda, Integer idAzienda) throws RibaltoneHttpException {
        super(specificDataConf, objectMapper, codiceAzienda, idAzienda);

        setGruSpecificData((GruSpecificData) specificDataConf);
        Boolean connessioneOk = gruSpecificData.getConnessione().build();
        if (!connessioneOk) {
            throw new RibaltoneHttpException("errore nella connessione a oracle di gru");
        }

    }

    @Override
    public List<DatiDaImportareAppartenente> getAppartenenti() {
        List<DatiDaImportareAppartenente> appartenenti;
        Sql2o sql2oConnecion = getGruSpecificData().getConnessione().getSql2oConnecion();

        try (Connection connessione = sql2oConnecion.open()) {
            //preparo i dati da inserire nelle query appartenenti e responsabili
            //CODICI ENTI VALIDI
            String codiceEnteStr;
            if (gruSpecificData.getCodiciEntiValidi() != null && !gruSpecificData.getCodiciEntiValidi().isEmpty()) {
                String codiciEntiValidiStr = RibaltoneUtils.formatStringsWithCommasAndQuotes(gruSpecificData.getCodiciEntiValidi());
                codiceEnteStr = "codice_ente in (" + codiciEntiValidiStr + ") ";
            } else {
                codiceEnteStr = "codice_ente LIKE '" + codiceAzienda + "'|| '%' ";
            }

            //prendo gli appartenenti
            String queryAppartenentiStr = getGruSpecificData().getQueryRecuperoDati().getQueryAppartenenti();
            queryAppartenentiStr = queryAppartenentiStr.replaceAll(":codici_enti_validi", codiceEnteStr);
            // commentato perche per ora non serve ma non si sa mai
//            // PERSONE NON SPEGNIBILI
//            if (gruSpecificData.getPersoneNonSpegnibili() != null && !gruSpecificData.getPersoneNonSpegnibili().isEmpty()) {
//                String personeNonSpegnibiliStr = RibaltoneUtils.formatStringsWithCommasAndQuotes(gruSpecificData.getPersoneNonSpegnibili());
//                String codiciFiscaliValidi = " or u.CODICE_FISCALE IN (" + personeNonSpegnibiliStr + ") ";
//                queryAppartenentiStr = queryAppartenentiStr.replaceAll(":codici_fiscali_validi", codiciFiscaliValidi);
//            } else {
            queryAppartenentiStr = queryAppartenentiStr.replaceAll(":codici_fiscali_validi", "");
//            }
//
//            //PERSONE DA SPEGNERE commentato perche per ora non serve ma non si sa mai
//            if (gruSpecificData.getPersoneDaSpegnere() != null && !gruSpecificData.getPersoneDaSpegnere().isEmpty()) {
//                String personeDaSpegnereStr = RibaltoneUtils.formatStringsWithCommasAndQuotes(gruSpecificData.getPersoneDaSpegnere());
//                String codiciFiscaliDaSpegnereQuery = "u.CODICE_FISCALE not in (" + personeDaSpegnereStr + ")";
//                queryAppartenentiStr = queryAppartenentiStr.replaceAll(":codici_fiscali_non_validi", codiciFiscaliDaSpegnereQuery);
//            } else {
            queryAppartenentiStr = queryAppartenentiStr.replaceAll(":codici_fiscali_non_validi", "");
//            }
            LOG.info("----------queryAppartenentiStr------------\n" + queryAppartenentiStr);

            List<Appartenente> appartenentiOracle = connessione
                .createQuery(queryAppartenentiStr)
                .setAutoDeriveColumnNames(true)
                .setCaseSensitive(false)
                .executeAndFetch(Appartenente.class);

            // prendo i responsabili per settare gli utenti responsabili come responsabili e creare le eventuali afferenze mancanti
            String queryResponsabiliStr = getGruSpecificData().getQueryRecuperoDati().getQueryResponsabili()
                .replaceAll(":codici_enti_validi", codiceEnteStr);

            LOG.info("----------queryResponsabiliStr------------\n" + queryResponsabiliStr);

            List<Responsabile> responsabiliOracle = connessione.createQuery(queryResponsabiliStr)
                .setAutoDeriveColumnNames(true)
                .setCaseSensitive(false)
                .executeAndFetch(Responsabile.class);
            appartenenti = mergeAccendiSpegniUtentiWithRespo(appartenentiOracle, responsabiliOracle, getGruSpecificData().getPersoneNonSpegnibili(), getGruSpecificData().getPersoneDaSpegnere());

        } catch (Exception e) {
            throw new RuntimeException("Errore durante il recupero degli appartenenti.", e);
        }
        return appartenenti;
    }

    @Override
    public List<DatiDaImportareStruttura> getStrutture() {
        //da fare come gli appartenenti ma attenzione che codici_enti_validi devono avete solo l'ente 01 per alcune aziende ad esempio bologna
        Sql2o sql2oConnecion = getGruSpecificData().getConnessione().getSql2oConnecion();
        List<DatiDaImportareStruttura> fonteIntermediaStrutture = new ArrayList<>();
        LOG.info("----------queryStruttureStr------------\n" + getGruSpecificData().getQueryRecuperoDati().getQueryStrutture().replaceAll(":codice_azienda", codiceAzienda));
        try (Connection con = sql2oConnecion.open()) {
            List<Struttura> struttureOracle = con.createQuery(getGruSpecificData().getQueryRecuperoDati().getQueryStrutture())
                .addParameter("codice_azienda", this.codiceAzienda)
                .setAutoDeriveColumnNames(true)
                .setCaseSensitive(false)
                .executeAndFetch(Struttura.class);
            struttureOracle.forEach(struttura -> fonteIntermediaStrutture.add(struttura.toFonteIntermedia(codiceAzienda, idAzienda)));

            return fonteIntermediaStrutture;
        } catch (Exception e) {
            throw new RuntimeException("Errore durante il recupero delle strutture.", e);
        }
    }

    @Override
    public List<DatiDaImportareTrasformazione> getTrasformazioni() {
        Sql2o sql2oConnecion = getGruSpecificData().getConnessione().getSql2oConnecion();
        List<DatiDaImportareTrasformazione> fonteIntermediaTrasformazioni = new ArrayList<>();
        try (Connection con = sql2oConnecion.open()) {
            List<Trasformazione> trasformazioniOracle = con.createQuery(getGruSpecificData().getQueryRecuperoDati().getQueryTrasformazioni())
                .addParameter("codice_azienda", this.codiceAzienda)
                .addParameter("progressivo_ultima_trasformazione", getGruSpecificData().getQueryRecuperoDati().getProgressivoUltimaTrasformazione())
                .setAutoDeriveColumnNames(true)
                .setCaseSensitive(false)
                .executeAndFetch(Trasformazione.class);
            trasformazioniOracle.forEach(traformazione -> fonteIntermediaTrasformazioni.add(traformazione.toFonteIntermedia(codiceAzienda, idAzienda)));
            return fonteIntermediaTrasformazioni;
        } catch (Exception e) {
            throw new RuntimeException("Errore durante il recupero degli appartenenti.", e);
        }
    }

    public GruSpecificData getGruSpecificData() {
        return gruSpecificData;
    }

    private void setGruSpecificData(GruSpecificData gruSpecificData) {
        this.gruSpecificData = gruSpecificData;
    }

    private List<DatiDaImportareAppartenente> mergeAccendiSpegniUtentiWithRespo(List<Appartenente> appartenenti, List<Responsabile> responsabili, List<String> personeDaAccendere, List<String> personeDaSpegnere) {
        List<DatiDaImportareAppartenente> fonteIntermediaAppartenenti = new ArrayList<>();
        Map<String, Map<String, Object>> appartententeMap = new HashMap<>();

        for (Appartenente appartenente : appartenenti) {
//            codiciFiscaliPerAnagrafica.add(appartenente.getCodiceFiscale());
            DatiDaImportareAppartenente fonteIntermediaAppartenente = appartenente.toFonteIntermedia(codiceAzienda, idAzienda);
            Map<String, Object> appartenenteData = new HashMap<>();
            appartenenteData.put("codiceEnte", appartenente.getCodiceEnte());
            appartenenteData.put("codiceMatricola", appartenente.getCodiceMatricola());
            appartenenteData.put("cognome", appartenente.getCognome());
            appartenenteData.put("nome", appartenente.getNome());
            appartenenteData.put("codiceFiscale", appartenente.getCodiceFiscale());
            appartenenteData.put("idCasella", appartenente.getIdCasella());
//            appartenenteData.put("datain", appartenente.getDatain().toInstant().atZone(ZoneId.systemDefault()));
//            appartenenteData.put("datafi", appartenente.getDatafi().toInstant().atZone(ZoneId.systemDefault()));
            appartenenteData.put("tipoAfferenza", appartenente.getTipoAppartenenza());
            appartenenteData.put("username", appartenente.getUsername());
//            appartenenteData.put("dataAssunzione", appartenente.getDataAssunzione().toInstant().atZone(ZoneId.systemDefault()));
//            appartenenteData.put("dataDimissione", appartenente.getDataDimissione().toInstant().atZone(ZoneId.systemDefault()));

            appartententeMap.put(appartenente.getCodiceMatricola(), appartenenteData);

            int j = 0;
            Boolean trovato = false;
            //inizio a settare i responsabili che trovo nell'apposita lista
            while (!trovato && j < responsabili.size()) {
                Responsabile responsabile = responsabili.get(j);
                if (fonteIntermediaAppartenente.getCodiceMatricola().equals(responsabile.getCodiceMatricola())
                    && fonteIntermediaAppartenente.getIdCasella().equals(responsabile.getIdCasella())
                    && fonteIntermediaAppartenente.getCodiceEnte().equals(responsabile.getCodiceEnte())) {

                    fonteIntermediaAppartenente.setResponsabile(true);
                    responsabili.remove(j);//
                    trovato = true;
                }
                j++;
            }
            fonteIntermediaAppartenenti.add(fonteIntermediaAppartenente);
        }

        for (Responsabile responsabileTotale : responsabili) {
            DatiDaImportareAppartenente fonteIntermediaAppartenente = new DatiDaImportareAppartenente();
            Map<String, Object> appartenenteData = appartententeMap.get(responsabileTotale.getCodiceMatricola());
            //caso di responsabile che ha afferenze attive e ne aggiungo una come responsabile
            if (appartenenteData != null) {
                fonteIntermediaAppartenente.setCodiceEnte(responsabileTotale.getCodiceEnte());
                fonteIntermediaAppartenente.setCodiceMatricola(responsabileTotale.getCodiceMatricola());
                fonteIntermediaAppartenente.setCodiceAzienda(codiceAzienda);
                fonteIntermediaAppartenente.setCodiceFiscale(appartenenteData.get("codiceFiscale").toString());
                fonteIntermediaAppartenente.setCognome(appartenenteData.get("cognome").toString());
                fonteIntermediaAppartenente.setNome(appartenenteData.get("nome").toString());
                if (appartenenteData.get("username") != null) {
                    fonteIntermediaAppartenente.setUsername(appartenenteData.get("username").toString());
                }
//                fonteIntermediaAppartenente.setDataAssunzione(((Date) appartenenteData.get("dataAssunzione")).toInstant().atZone(ZoneId.systemDefault()));
//                fonteIntermediaAppartenente.setDataDimissione(appartenenteData.get("dataDimissione") != null ? ((Date) appartenenteData.get("dataDimissione")).toInstant().atZone(ZoneId.systemDefault()) : null);
//                fonteIntermediaAppartenente.setDatain(responsabileTotale.getDatain().toInstant().atZone(ZoneId.systemDefault()));
//                fonteIntermediaAppartenente.setDatafi(responsabileTotale.getDatafi().toInstant().atZone(ZoneId.systemDefault()));
                fonteIntermediaAppartenente.setResponsabile(true);
                fonteIntermediaAppartenente.setTipoAppartenenza("F");
                fonteIntermediaAppartenenti.add(fonteIntermediaAppartenente);
            } else {
                //caso in cui il responsabile non ha afferenze attive
                //da implementare nel caso si debba far entrare lo stesso
                //per ora pero non si fa
            }

        }

        return fonteIntermediaAppartenenti;
    }

    @Override
    public List<DatiDaImportareAnagrafica> getAnagrafica() {
        Sql2o sql2oConnecion = getGruSpecificData().getConnessione().getSql2oConnecion();
        List<DatiDaImportareAnagrafica> fonteIntermediaAnagrafiche = new ArrayList<>();
        try (Connection con = sql2oConnecion.open()) {
            //CODICI ENTI VALIDI
            String codiceEnteStr;
            if (gruSpecificData.getCodiciEntiValidi() != null && !gruSpecificData.getCodiciEntiValidi().isEmpty()) {
                String codiciEntiValidiStr = RibaltoneUtils.formatStringsWithCommasAndQuotes(gruSpecificData.getCodiciEntiValidi());
                codiceEnteStr = "codice_ente in (" + codiciEntiValidiStr + ") ";
            } else {
                codiceEnteStr = "codice_ente LIKE '" + codiceAzienda + "'|| '%' ";
            }
            String queryAnagraficaStr = getGruSpecificData().getQueryRecuperoDati().getQueryAnagrafiche()
                .replaceAll(":codici_enti_validi", codiceEnteStr)
                .replaceAll(":codice_azienda", codiceAzienda);
            LOG.info("-----------------queryAnagraficaStr--------" + queryAnagraficaStr);
            List<Anagrafica> anagraficheOracle = con.createQuery(queryAnagraficaStr)
                .setAutoDeriveColumnNames(true)
                .setCaseSensitive(false)
                .executeAndFetch(Anagrafica.class);
            anagraficheOracle.forEach(anagrafica -> fonteIntermediaAnagrafiche.add(anagrafica.toFonteIntermedia(codiceAzienda, idAzienda)));
            return fonteIntermediaAnagrafiche;
        }
    }
}
