/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.sdico;

import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.Registro;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    //TODO serve?
    private Registro registro;
    private List<Persona> firmatari = new ArrayList<>();
    private Map<String, Object> parametriVersamento = new HashMap<>();
    
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
     * @return 
     */
    public VersamentoBuilder build() {
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Map<String, String> mappaParametri = (Map<String, String>) parametriVersamento.get(CODICE);
        String docType = (String) mappaParametri.get("idTipoDoc");
        String codiceEneteVersatore = (String) parametriVersamento.get("ente");
        String idClassifica = (String) mappaParametri.get("idClassifica");
        String classificazioneArchivistica = (String) mappaParametri.get("classificazioneArchivistica");
        String descrizioneClassificazione = (String) mappaParametri.get("descrizioneClassificazione");
        String codiceRegistro = (String) mappaParametri.get("codiceRegistro");
        //TODO vedere come chiamare il registro
//        String codiceRegistro = null;
//        if (registro.getCodice() != null) {
//            codiceRegistro = registro.getCodice().toString();
//        }
        String anniTenuta = "illimitato";
        if (archivio.getAnniTenuta() != 999) {
            anniTenuta = Integer.toString(archivio.getAnniTenuta());
        }
        DecimalFormat df = new DecimalFormat("0000000");
        //TODO il documento deve essere per forza registrato per essere versato?
        String numeroDocumento = df.format(docDetail.getNumeroRegistrazione());
        String nomeSistemaVersante = (String) parametriVersamento.get("idSistemaVersante");
        String tipologiaDiFlusso = (String) mappaParametri.get("tipologiaDiFlusso");
        String ufficioProduttore = (String) mappaParametri.get("ufficioProduttore");
        String firmatoDigitalmente = (String) mappaParametri.get("firmatoDigitalmente");
        String marcaturaTemporale = (String) mappaParametri.get("marcaturaTemporale");
        String riservato = (String) mappaParametri.get("riservato");
        String stringaDiFirmatari = "";
        for (Persona firmatario : firmatari) {
            stringaDiFirmatari += firmatario.getCodiceFiscale() + " - ";
        }
        String stringaAllegati = "";
        for (Allegato allegato : doc.getAllegati()) {
            stringaAllegati += Integer.toString(allegato.getId()) + " - ";
        }
        HashMap<String, Object> additionalData = doc.getAdditionalData();
        String dataEsecutiva = null;
        if (additionalData != null) {
            if (additionalData.containsKey("dati_pubblicazione")) {
                HashMap<String, Object> datiPubblicazione = (HashMap<String, Object>) additionalData.get("dati_pubblicazione");
                if (datiPubblicazione.containsKey("data_esecutivita"))
                dataEsecutiva = additionalData.get("data_esecutivita").toString();
            }
        }

        versamentoBuilder.setDocType(docType);
        versamentoBuilder.addSinglemetadataByParams(true, "id_ente_versatore", Arrays.asList(codiceEneteVersatore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "idTipoDoc", Arrays.asList(docType), TESTO);
        //TODO da vedere se cambiano in base alla tipologia
        versamentoBuilder.addSinglemetadataByParams(true, "idClassifica", Arrays.asList(idClassifica), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "classificazioneArchivistica", Arrays.asList(classificazioneArchivistica), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "amministrazioneTitolareDelProcedimento", Arrays.asList(codiceEneteVersatore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "aooDiRiferimento", Arrays.asList((String) parametriVersamento.get("aooDiRiferimento")), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "descrizione_classificazione", Arrays.asList(descrizioneClassificazione), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "tempo_di_conservazione", Arrays.asList(anniTenuta), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "oggettodocumento", Arrays.asList(doc.getOggetto()), TESTO);
        //TODO diverso da descrizione classificazione?
        versamentoBuilder.addSinglemetadataByParams(false, "repertorio", Arrays.asList(descrizioneClassificazione), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "numero_documento", Arrays.asList(numeroDocumento), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "data_di_registrazione", Arrays.asList(docDetail.getDataRegistrazione().format(formatter)), DATA);
//        versamentoBuilder.addSinglemetadataByParams(false, "numeroProtocollo", Arrays.asList(""), "TESTO");
//        versamentoBuilder.addSinglemetadataByParams(false, "dataRegistrazioneProtocollo", Arrays.asList(""), "DATA");
        versamentoBuilder.addSinglemetadataByParams(false, "tipologia_di_flusso", Arrays.asList(tipologiaDiFlusso), TESTO);
        //TODO da chiedere ad AZERO cosa inserire
        versamentoBuilder.addSinglemetadataByParams(false, "ufficioProduttore", Arrays.asList(ufficioProduttore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "responasbileProcedimento", Arrays.asList(docDetail.getIdPersonaResponsabileProcedimento().getDescrizione()), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "idFascicolo", Arrays.asList(Integer.toString(archivio.getId()) + "_" + archivio.getOggetto()), TESTO_MULTIPLO);
        //TODO da aggiungere nel caso ci sia bisogno dei controlli if (registro != null && registro.getCodice() != null) {
            versamentoBuilder.addSinglemetadataByParams(false, "registro", Arrays.asList(codiceRegistro), TESTO);
        //}
//        versamentoBuilder.addSinglemetadataByParams(false, "serie", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "idSistemaVersante", Arrays.asList(nomeSistemaVersante), TESTO);
//        versamentoBuilder.addSinglemetadataByParams(false, "numeroAltraRegistrazione", Arrays.asList(""), "TESTO");
//        versamentoBuilder.addSinglemetadataByParams(false, "dataAltraRegistrazione", Arrays.asList(""), "DATA");
        versamentoBuilder.addSinglemetadataByParams(false, "applicativoProduzione", Arrays.asList((String) parametriVersamento.get("applicativoProduzione")), TESTO);
//        versamentoBuilder.addSinglemetadataByParams(false, "note1", Arrays.asList(""), "TESTO");
//        versamentoBuilder.addSinglemetadataByParams(false, "note2", Arrays.asList(""), "TESTO");
//        versamentoBuilder.addSinglemetadataByParams(false, "annotazione", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "firmato_digitalmente", Arrays.asList(firmatoDigitalmente), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "marcatura_temporale", Arrays.asList(marcaturaTemporale), TESTO); 
        versamentoBuilder.addSinglemetadataByParams(false, "idDocumentoOriginale", Arrays.asList(Integer.toString(doc.getId())), TESTO); 
        versamentoBuilder.addSinglemetadataByParams(false, "numero_allegati", Arrays.asList(Integer.toString(doc.getAllegati().size())), TESTO);
//        versamentoBuilder.addSinglemetadataByParams(false, "livelloRiservatezza", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "riservato", Arrays.asList(riservato), TESTO);   
//        versamentoBuilder.addSinglemetadataByParams(false, "esistenzaOriginaleAnalogico", Arrays.asList(""), "TESTO");
//        versamentoBuilder.addSinglemetadataByParams(false, "oggettoProcedimento", Arrays.asList(""), "TESTO");
//        versamentoBuilder.addSinglemetadataByParams(false, "materiaargomentostruttura_procedimento", Arrays.asList(""), "TESTO");
//        versamentoBuilder.addSinglemetadataByParams(false, "conformita_copie_immagine_su_supporto_informatico", Arrays.asList(""), "TESTO");
//        versamentoBuilder.addSinglemetadataByParams(false, "sigillato_elettronicamente", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "cfTitolareFirma", Arrays.asList(stringaDiFirmatari.substring(0, stringaDiFirmatari.length() - 3)), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "prodotto_software", Arrays.asList(nomeSistemaVersante), TESTO);
//        versamentoBuilder.addSinglemetadataByParams(false, "prodotto_software_nome_prodotto", Arrays.asList(""), "TESTO");
//        versamentoBuilder.addSinglemetadataByParams(false, "prodotto_software_versione_prodotto", Arrays.asList(""), "TESTO");
//        versamentoBuilder.addSinglemetadataByParams(false, "prodotto_software_produttore", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "tipo_registro", Arrays.asList(doc.getTipologia().toString()), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "codice_registro", Arrays.asList(codiceRegistro), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "id_doc_allegati", Arrays.asList(stringaAllegati.substring(0, stringaAllegati.length() - 3)), TESTO); 
//        versamentoBuilder.addSinglemetadataByParams(false, "piano_di_classificazione", Arrays.asList(""), "TESTO");
        //TODO abbiamo solo anno proposta
        //versamentoBuilder.addSinglemetadataByParams(false, "data_proposta", Arrays.asList(""), "DATA"); 
        versamentoBuilder.addSinglemetadataByParams(false, "numero_proposta", Arrays.asList(Integer.toString(docDetail.getNumeroProposta())), TESTO);
        if (dataEsecutiva != null) {
            versamentoBuilder.addSinglemetadataByParams(false, "data_esecutiva", Arrays.asList(dataEsecutiva), DATA);
        }
        versamentoBuilder.addSinglemetadataByParams(false, "natura_documento", Arrays.asList(codiceRegistro), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "data_pubblicazione", Arrays.asList(docDetail.getDataPubblicazione().format(formatter)), DATA);
        //versamentoBuilder.addSinglemetadataByParams(false, "autore_pubblicazione", Arrays.asList(""), "TESTO");

        return versamentoBuilder;

    }

}
