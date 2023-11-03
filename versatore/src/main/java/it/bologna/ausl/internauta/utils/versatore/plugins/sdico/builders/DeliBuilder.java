/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders;

import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreSdicoException;
import it.bologna.ausl.internauta.utils.versatore.utils.SdicoVersatoreUtils;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.Registro;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author boria
 */
public class DeliBuilder {

    private static final Logger log = LoggerFactory.getLogger(DeliBuilder.class);
    private static final String CODICE = "DELIBERA";
    private static final String TESTO = "TESTO";
    private static final String DATA = "DATA";
    private static final String TESTO_MULTIPLO = "TESTO MULTIPLO";

    private VersamentoBuilder versamentoBuilder;
    private Doc doc;
    private DocDetail docDetail;
    private Archivio archivio;
    private Registro registro;
    private List<Persona> firmatari;
    private Map<String, Object> parametriVersamento;

    public DeliBuilder(Doc doc, DocDetail docDetail, Archivio archivio, Registro registro, List<Persona> firmatari, Map<String, Object> parametriVersamento) {
        versamentoBuilder = new VersamentoBuilder();
        this.doc = doc;
        this.docDetail = docDetail;
        this.archivio = archivio;
        this.registro = registro;
        this.firmatari = firmatari;
        this.parametriVersamento = parametriVersamento;
    }

    /**
     * Metodo che costruisce i metadati per le delibere (id tipo doc 83)
     *
     * @return
     */
    public VersamentoBuilder build() throws VersatoreSdicoException {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Map<String, String> mappaParametri = (Map<String, String>) parametriVersamento.get(CODICE);
        String docType = (String) mappaParametri.get("idTipoDoc");
        String codiceEnteVersatore = (String) parametriVersamento.get("ente");
        //TODO vedere se sono giuste SPRITZ
        String idClassifica = archivio.getIdTitolo().getId().toString();
        String classificazioneArchivistica = archivio.getIdTitolo().getClassificazione();
        String descrizioneClassificazione = archivio.getIdTitolo().getNome();
        String repertorio = mappaParametri.get("repertorio");
        //String codiceRegistro = registro.getCodice().toString();
        //TODO in futuro prendere da db scripta.registro
        String codiceRegistro = (String) mappaParametri.get("codiceRegistro");
        String anniTenuta = "illimitato";
        if (archivio.getAnniTenuta() != 999) {
            anniTenuta = Integer.toString(archivio.getAnniTenuta());
        }
        DecimalFormat df = new DecimalFormat("0000000");
        String numeroDocumento = df.format(docDetail.getNumeroRegistrazione());
        String nomeSistemaVersante = (String) parametriVersamento.get("idSistemaVersante");
        String tipologiaDiFlusso = (String) mappaParametri.get("tipologiaDiFlusso");
        //TODO controllare da dove deve arrivare
        String ufficioProduttore = (String) mappaParametri.get("ufficioProduttore");
        String firmatoDigitalmente = (String) mappaParametri.get("firmatoDigitalmente");
        String marcaturaTemporale = (String) mappaParametri.get("marcaturaTemporale");
        String riservato = (String) mappaParametri.get("riservato");
        String numeroProposta = docDetail.getAnnoProposta().toString() + "-" + df.format(docDetail.getNumeroProposta());
        String stringaDiFirmatari = "";
        if (firmatari.size() > 0) {
            for (Persona firmatario : firmatari) {
                stringaDiFirmatari += firmatario.getCodiceFiscale() + " - ";
            }
        } else {
            throw new VersatoreSdicoException("La Delibera non ha firmatari");
        }
        String stringaAllegati = "";
        for (Allegato allegato : doc.getAllegati()) {
            stringaAllegati += Integer.toString(allegato.getId()) + " - ";
        }
        HashMap<String, Object> additionalData = doc.getAdditionalData();
        String dataEsecutivita;
        if (additionalData != null) {
            if (additionalData.containsKey("dati_pubblicazione")) {
                HashMap<String, Object> datiPubblicazione = (HashMap<String, Object>) additionalData.get("dati_pubblicazione");
                if (datiPubblicazione.containsKey("data_esecutivita") && datiPubblicazione.get("data_esecutivita") != null) {
                    dataEsecutivita = datiPubblicazione.get("data_esecutivita").toString();
                } else {
                    throw new VersatoreSdicoException("La Delibera non ha data esecutivita");
                }
            } else {
                throw new VersatoreSdicoException("La Delibera non ha i dati di pubblicazione");
            }
        } else {
            throw new VersatoreSdicoException("La Delibera non gli additionalData");
        }

        versamentoBuilder.setDocType(docType);
        versamentoBuilder.addSinglemetadataByParams(true, "id_ente_versatore", Arrays.asList(codiceEnteVersatore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "idTipoDoc", Arrays.asList(docType), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "idClassifica", Arrays.asList(idClassifica), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "classificazioneArchivistica", Arrays.asList(classificazioneArchivistica), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "amministrazioneTitolareDelProcedimento", Arrays.asList(codiceEnteVersatore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "aooDiRiferimento", Arrays.asList((String) parametriVersamento.get("aooDiRiferimento")), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "descrizione_classificazione", Arrays.asList(descrizioneClassificazione), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "tempo_di_conservazione", Arrays.asList(anniTenuta), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "oggettodocumento", Arrays.asList(doc.getOggetto()), TESTO);
        //TODO diverso da descrizione classificazione? si ora metto un valore segnaposto perché ci deve essere dato
        versamentoBuilder.addSinglemetadataByParams(false, "repertorio", Arrays.asList(repertorio), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "numero_documento", Arrays.asList(numeroDocumento), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "data_di_registrazione", Arrays.asList(docDetail.getDataRegistrazione().format(formatter)), DATA);
        versamentoBuilder.addSinglemetadataByParams(false, "tipologia_di_flusso", Arrays.asList(tipologiaDiFlusso), TESTO);
        //TODO la prendo da attori_docs, da chiedere se è id_struttura_registrazione o altrimenti come individuo qual è? SPRITZ (e togliere da db)
        versamentoBuilder.addSinglemetadataByParams(false, "ufficioProduttore", Arrays.asList(ufficioProduttore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "responsabileProcedimento", Arrays.asList(docDetail.getIdPersonaResponsabileProcedimento().getDescrizione()), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "idFascicolo", SdicoVersatoreUtils.buildListaIdFascicolo(archivio), TESTO_MULTIPLO);
        //TODO da aggiungere nel caso ci sia bisogno dei controlli if (registro != null && registro.getCodice() != null) {
        versamentoBuilder.addSinglemetadataByParams(false, "registro", Arrays.asList(codiceRegistro), TESTO);
        //}
        versamentoBuilder.addSinglemetadataByParams(false, "idSistemaVersante", Arrays.asList(nomeSistemaVersante), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "applicativoProduzione", Arrays.asList((String) parametriVersamento.get("applicativoProduzione")), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "firmato_digitalmente", Arrays.asList(firmatoDigitalmente), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "marcatura_temporale", Arrays.asList(marcaturaTemporale), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "idDocumentoOriginale", Arrays.asList(Integer.toString(doc.getId())), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "numero_allegati", Arrays.asList(Integer.toString(doc.getAllegati().size())), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "riservato", Arrays.asList(riservato), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "cfTitolareFirma", Arrays.asList(stringaDiFirmatari.substring(0, stringaDiFirmatari.length() - 3)), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "prodotto_software", Arrays.asList(nomeSistemaVersante), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "tipo_registro", Arrays.asList(doc.getTipologia().toString()), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "codice_registro", Arrays.asList(codiceRegistro), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "id_doc_allegati", Arrays.asList(stringaAllegati.substring(0, stringaAllegati.length() - 3)), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "data_proposta", Arrays.asList(docDetail.getDataCreazione().format(formatter)), DATA);
        versamentoBuilder.addSinglemetadataByParams(false, "numero_proposta", Arrays.asList(numeroProposta), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "data_esecutivita", Arrays.asList(dataEsecutivita), DATA);
        versamentoBuilder.addSinglemetadataByParams(false, "natura_documento", Arrays.asList(codiceRegistro), TESTO);
        if (docDetail.getDataPubblicazione() != null) {
            versamentoBuilder.addSinglemetadataByParams(false, "data_pubblicazione", Arrays.asList(docDetail.getDataPubblicazione().format(formatter)), DATA);
        } else {
            throw new VersatoreSdicoException("La Delibera non ha data pubblicazione");
        }

        return versamentoBuilder;

    }

}
