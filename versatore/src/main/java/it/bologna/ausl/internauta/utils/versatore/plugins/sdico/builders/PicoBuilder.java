/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders;

import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreSdicoException;
import it.bologna.ausl.internauta.utils.versatore.utils.SdicoVersatoreUtils;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Archivio;
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
     * Metodo che costruisce i metadati per i protocolli (id tipo doc 800)
     *
     * @return
     */
    public VersamentoBuilder build() throws VersatoreSdicoException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        DecimalFormat df = new DecimalFormat("0000000");
        Map<String, String> mappaParametri = (Map<String, String>) parametriVersamento.get(CODICE);
        String docType = (String) mappaParametri.get("idTipoDoc");
        String codiceEneteVersatore = (String) parametriVersamento.get("ente");
        String idClassifica = archivio.getIdTitolo().getId().toString();
        String classificazioneArchivistica = archivio.getIdTitolo().getClassificazione();
        String nomeSistemaVersante = (String) parametriVersamento.get("idSistemaVersante");
        String numeroProtocollo = df.format(docDetail.getNumeroRegistrazione());
        String stringaDiFirmatari = "";
        String repertorio = (String) mappaParametri.get("repertorio");
        //String codiceRegistro = registro.getCodice().toString();
        //TODO in futuro prendere da db scripta.registro
        String codiceRegistro = (String) mappaParametri.get("codiceRegistro");
        String responsabileProcedimento;
        String anniTenuta = "tenuta illimitata";
        if (archivio.getAnniTenuta() != 999) {
            anniTenuta = Integer.toString(archivio.getAnniTenuta());
        }

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
        versamentoBuilder.addSinglemetadataByParams(false, "numeroProctocollo", Arrays.asList(numeroProtocollo), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "dataRegistrazioneProtocollo", Arrays.asList(docDetail.getDataRegistrazione().format(formatter)), DATA);
        versamentoBuilder.addSinglemetadataByParams(false, "NUMERO_ALLEGATI", Arrays.asList(Integer.toString(doc.getAllegati().size())), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "ufficioProduttore", Arrays.asList(docDetail.getIdStrutturaRegistrazione().getNome()), TESTO);
        //TODO da inserire, capire se è obbligatorio o meno, direi di no?? :: mettiamolo -- è uguale a sopra SPRITZ
        versamentoBuilder.addSinglemetadataByParams(false, "DENOMINAZIONE_STRUTTURA", Arrays.asList(docDetail.getIdStrutturaRegistrazione().getNome()), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "idFascicolo", SdicoVersatoreUtils.buildListaIdFascicolo(archivio), TESTO_MULTIPLO);
        versamentoBuilder.addSinglemetadataByParams(false, "repertorio", Arrays.asList(repertorio), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "idDocumentoOriginale", Arrays.asList(Integer.toString(doc.getId())), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "registro", Arrays.asList(codiceRegistro), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "annotazione", Arrays.asList(anniTenuta), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "note1", Arrays.asList("Tipologia di flusso: " + doc.getTipologia().toString()), TESTO);
        //attributi presenti solo nei pu
        if (doc.getTipologia().equals(DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA)) {
            if (firmatari.size() > 0) {//TODO i pe hanno firmatari? SPRITZ
                for (Persona firmatario : firmatari) {
                    stringaDiFirmatari += firmatario.getCodiceFiscale() + " - ";
                }
                versamentoBuilder.addSinglemetadataByParams(false, "cfTitolareFirma", Arrays.asList(stringaDiFirmatari.substring(0, stringaDiFirmatari.length() - 3)), TESTO);
            } else {
                throw new VersatoreSdicoException("Il protocollo non ha firmatari");
            }
            //TODO è vuoto?:: nei pu c'è sempre, se non c'è è errore, nei pe invece non c'è --- ma se è pe ed è presente è errore? SPRITZ
            if (docDetail.getIdPersonaResponsabileProcedimento() != null) {
                responsabileProcedimento = docDetail.getIdPersonaResponsabileProcedimento().getDescrizione();
                versamentoBuilder.addSinglemetadataByParams(false, "responasbileProcedimento", Arrays.asList(responsabileProcedimento), TESTO);
            } else {
                throw new VersatoreSdicoException("Il Protocollo non ha Responsabile di Procedimento");
            }
        }

        return versamentoBuilder;
    }

}
