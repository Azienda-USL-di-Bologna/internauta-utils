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
import it.bologna.ausl.model.entities.scripta.AttoreDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.model.entities.scripta.Registro;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author boria
 */
public class PicoBuilder {

    private static final Logger log = LoggerFactory.getLogger(PicoBuilder.class);
    private static final String CODICE = "PROTOCOLLO";
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

    public PicoBuilder(Doc doc, DocDetail docDetail, Archivio archivio, Registro registro, List<Persona> firmatari, Map<String, Object> parametriVersamento) {
        versamentoBuilder = new VersamentoBuilder();
        this.doc = doc;
        this.docDetail = docDetail;
        this.archivio = archivio;
        this.registro = registro;
        this.firmatari = firmatari;
        this.parametriVersamento = parametriVersamento;
    }

    /**
     * Metodo che costruisce i metadati per i protocolli (id tipo doc 800NEW)
     *
     * @return
     */
    public VersamentoBuilder build() throws VersatoreSdicoException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        DecimalFormat df = new DecimalFormat("0000000");
        Map<String, Object> mappaParametri = (Map<String, Object>) parametriVersamento.get(CODICE);
        String docType = (String) mappaParametri.get("idTipoDoc");
        String codiceEneteVersatore = (String) parametriVersamento.get("ente");
        String idClassifica = archivio.getIdTitolo().getIdClassificaDaEsterno().toString();
        String classificazioneArchivistica = archivio.getIdTitolo().getClassificazione();
        Map<String, Object> parametriSoloPU = (Map<String, Object>) mappaParametri.get("PROTOCOLLO_IN_USCITA");
        Map<String, String> parametriSoloPE = (Map<String, String>) mappaParametri.get("PROTOCOLLO_IN_ENTRATA");
        String nomeSistemaVersante = (String) parametriVersamento.get("idSistemaVersante");
        String numeroProtocollo = df.format(docDetail.getNumeroRegistrazione());
        String stringaDiFirmatari = "";
        String repertorio = (String) mappaParametri.get("repertorio");
        //String codiceRegistro = registro.getCodice().toString();
        //TODO in futuro prendere da db scripta.registro
        String codiceRegistro = (String) mappaParametri.get("codiceRegistro");
        String responsabileProcedimento;
        String anniTenuta = "illimitato";
        if (archivio.getAnniTenuta() != 999) {
            anniTenuta = Integer.toString(archivio.getAnniTenuta());
        }
        //String nomeDelDocumento = "PG" + numeroProtocollo + "/" + docDetail.getAnnoRegistrazione(); ridondante secondo SDICO
        String firmatoDigitalmente = "";
        String modalitaDiFormazione = (String) parametriVersamento.get("modalitaDiFormazione");
        String descrizioneClassificazione = archivio.getIdTitolo().getNome();
        String produttore = (String) parametriVersamento.get("produttore");
        //String identificativoDocumentoPrimario = ""; dato ridondante
        List<Allegato> listaAllegati = doc.getAllegati();
        String riservato = docDetail.getRiservato() ? "Vero" : "Falso";
        String stringaAllegati = "";
        for (Allegato allegato : doc.getAllegati()) {
            stringaAllegati += Integer.toString(allegato.getId()) + " - ";
        }
        stringaAllegati = stringaAllegati.substring(0, stringaAllegati.length() - 3);
        String descrizioneSoftware = (String) parametriVersamento.get("descrizioneSoftware");
        String tipologiaDiFlusso = "";
        String sigillatoElettronicamente = (String) mappaParametri.get("sigillatoElettronicamente");
        String pianoDiClassificazione = (String) parametriVersamento.get("pianoDiClassificazione");

        versamentoBuilder.setDocType(docType);
        versamentoBuilder.addSinglemetadataByParams(true, "id_ente_versatore", Arrays.asList(codiceEneteVersatore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "idTipoDoc", Arrays.asList(docType), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "idClassifica", Arrays.asList(idClassifica), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "classificazioneArchivistica", Arrays.asList(classificazioneArchivistica), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "amministrazioneTitolareDelProcedimento", Arrays.asList(codiceEneteVersatore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "aooDiRiferimento", Arrays.asList((String) parametriVersamento.get("aooDiRiferimento")), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "idSistemaVersante", Arrays.asList(nomeSistemaVersante), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "applicativoProduzione", Arrays.asList((String) parametriVersamento.get("applicativoProduzione")), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "oggettodocumento", Arrays.asList(doc.getOggetto()), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "numeroProtocollo", Arrays.asList(numeroProtocollo), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "dataRegistrazioneProtocollo", Arrays.asList(docDetail.getDataRegistrazione().format(formatter)), DATA);
        versamentoBuilder.addSinglemetadataByParams(false, "NUMERO_ALLEGATI", Arrays.asList(Integer.toString(doc.getAllegati().size())), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "ufficioProduttore", Arrays.asList(docDetail.getIdStrutturaRegistrazione().getNome()), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "idFascicolo", Arrays.asList(SdicoVersatoreUtils.buildIdFascicoli(doc, archivio)), TESTO_MULTIPLO);
        versamentoBuilder.addSinglemetadataByParams(false, "repertorio", Arrays.asList(repertorio), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "idDocumentoOriginale", Arrays.asList(Integer.toString(doc.getId())), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "registro", Arrays.asList(codiceRegistro), TESTO);
        //campi non più in uso
        //versamentoBuilder.addSinglemetadataByParams(false, "annotazione", Arrays.asList(anniTenuta), TESTO);
        //versamentoBuilder.addSinglemetadataByParams(false, "note1", Arrays.asList("Tipologia di flusso: " + doc.getTipologia().toString()), TESTO);

        //campi aggiunti con il passaggio alla tipologia 800NEW
        //versamentoBuilder.addSinglemetadataByParams(false, "nome_del_documento", Arrays.asList(nomeDelDocumento), TESTO); dato ridondante
        versamentoBuilder.addSinglemetadataByParams(false, "prodotto_software", Arrays.asList(descrizioneSoftware), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "modalita_di_formazione", Arrays.asList(modalitaDiFormazione), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "descrizione_classificazione", Arrays.asList(descrizioneClassificazione), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "prodotto_software_produttore", Arrays.asList(produttore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "tipo_registro", Arrays.asList(doc.getTipologia().toString()), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "riservato", Arrays.asList(riservato), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "codice_registro", Arrays.asList(codiceRegistro), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "tempo_di_conservazione", Arrays.asList(anniTenuta), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "id_doc_allegati", Arrays.asList(stringaAllegati), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "sigillato_elettronicamente", Arrays.asList(sigillatoElettronicamente), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "indice_di_classificazione", Arrays.asList(classificazioneArchivistica + " - " + descrizioneClassificazione), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "piano_di_classificazione", Arrays.asList(pianoDiClassificazione), TESTO);

        //attributi presenti solo nei pu
        if (doc.getTipologia().equals(DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA)) {
            if (firmatari != null) {
                for (Persona firmatario : firmatari) {
                    stringaDiFirmatari += firmatario.getCodiceFiscale() + " - " + firmatario.getDescrizione() + ", ";
                }
                stringaDiFirmatari = stringaDiFirmatari.substring(0, stringaDiFirmatari.length() - 2);
                versamentoBuilder.addSinglemetadataByParams(false, "cfTitolareFirma", Arrays.asList(stringaDiFirmatari), TESTO);
            } else {
                throw new VersatoreSdicoException("Il protocollo non ha firmatari");
            }
            if (docDetail.getIdPersonaResponsabileProcedimento() != null) {
                responsabileProcedimento = docDetail.getIdPersonaResponsabileProcedimento().getDescrizione();
                versamentoBuilder.addSinglemetadataByParams(false, "responsabileProcedimento", Arrays.asList(responsabileProcedimento), TESTO);
            } else {
                throw new VersatoreSdicoException("Il Protocollo non ha Responsabile di Procedimento");
            }
            firmatoDigitalmente = (String) parametriSoloPU.get("firmatoDigitalmente");
            //blocco rimosso perché il metadato è ridondadante
//            for (Allegato allegato : listaAllegati) {
//                if (allegato.getTipo().equals(Allegato.TipoAllegato.TESTO)) {
//                    identificativoDocumentoPrimario = allegato.getId().toString();
//                    break;
//                }
//            }
            //tipologiaDiFlusso = parametriSoloPU.get("tipologiaDiFlusso");
            Map<String, Object> tipiDiFlusso = (Map<String, Object>) parametriSoloPU.get("tipologiaDiFlusso");
            List<AttoreDoc> listaAttori = doc.getAttoriList();
            tipologiaDiFlusso = (String) tipiDiFlusso.get("esterno");
            for (AttoreDoc attore : listaAttori) {
                if (attore.getRuolo().equals(AttoreDoc.RuoloAttoreDoc.ASSEGNATARIO) 
                        || attore.getRuolo().equals(AttoreDoc.RuoloAttoreDoc.RESPONSABILE)
                        || attore.getRuolo().equals(AttoreDoc.RuoloAttoreDoc.SEGRETARIO)) {
                    tipologiaDiFlusso = (String) tipiDiFlusso.get("interno");
                    break;
                }
            }
        }

        //attributi presenti solo nei pe
        if (doc.getTipologia().equals(DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA)) {
            for (Allegato allegato : listaAllegati) {
                if (allegato.getPrincipale()) {
                    firmatoDigitalmente = allegato.getFirmato() ? "Vero" : "Falso";
                    //identificativoDocumentoPrimario = allegato.getId().toString(); metadato ridondante
                    break;
                }
            }
            tipologiaDiFlusso = parametriSoloPE.get("tipologiaDiFlusso");
        }
        
        //attributi differenti tra pe e pu
        versamentoBuilder.addSinglemetadataByParams(false, "firmato_digitalmente", Arrays.asList(firmatoDigitalmente), TESTO);
        //versamentoBuilder.addSinglemetadataByParams(false, "identificativo_documento_primario", Arrays.asList(identificativoDocumentoPrimario), TESTO); metadato ridondante
        versamentoBuilder.addSinglemetadataByParams(false, "tipologia_di_flusso", Arrays.asList(tipologiaDiFlusso), TESTO);
        
        //Questa coppia di metadati serve per non fare andare in errore la struttura di Scryba
        versamentoBuilder.addSinglemetadataByParams(false, "numero_documento", Arrays.asList(numeroProtocollo), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "data_di_registrazione", Arrays.asList(docDetail.getDataRegistrazione().format(formatter)), DATA);

        return versamentoBuilder;
    }

}
