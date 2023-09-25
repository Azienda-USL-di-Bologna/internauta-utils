/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.sdico;

import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
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
    //TODO serve?
    private Registro registro;
    private List<Persona> firmatari = new ArrayList<>();
    private Map<String, Object> parametriVersamento = new HashMap<>();
    
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
     * @return 
     */
    public VersamentoBuilder build() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        DecimalFormat df = new DecimalFormat("0000000");
        Map<String, String> mappaParametri = (Map<String, String>) parametriVersamento.get(CODICE);
        String docType = (String) mappaParametri.get("idTipoDoc");
        String codiceEneteVersatore = (String) parametriVersamento.get("ente");
        String idClassifica = (String) mappaParametri.get("idClassifica");
        String classificazioneArchivistica = (String) mappaParametri.get("classificazioneArchivistica");
        String nomeSistemaVersante = (String) parametriVersamento.get("idSistemaVersante");
        String numeroProtocollo = df.format(docDetail.getNumeroRegistrazione());
        String stringaDiFirmatari = "";
        for (Persona firmatario : firmatari) {
            stringaDiFirmatari += firmatario.getCodiceFiscale() + " - ";
        }
        String descrizioneClassificazione = (String) mappaParametri.get("descrizioneClassificazione");
        //TODO vedere come viene chiamato
        String codiceRegistro = (String) mappaParametri.get("codiceRegistro");
        
        
        versamentoBuilder.setDocType(docType);
        versamentoBuilder.addSinglemetadataByParams(true, "id_ente_versatore", Arrays.asList(codiceEneteVersatore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "idTipoDoc", Arrays.asList(docType), TESTO);
        //TODO da vedere se cambiano in base alla tipologia
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
        //TODO è id_struttura_registrazione? o codice interno dell'ente?
        versamentoBuilder.addSinglemetadataByParams(false, "ufficioProduttore", Arrays.asList(docDetail.getIdStrutturaRegistrazione().getCodice().toString()), TESTO);
        //TODO da inserire??
        versamentoBuilder.addSinglemetadataByParams(false, "DENOMINAZIONE_STRUTTURA", Arrays.asList(docDetail.getIdStrutturaRegistrazione().getNome()), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "responasbileProcedimento", Arrays.asList(docDetail.getIdPersonaResponsabileProcedimento().getDescrizione()), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "idFascicolo", Arrays.asList(Integer.toString(archivio.getId()) + "_" + archivio.getOggetto()), TESTO_MULTIPLO);
        versamentoBuilder.addSinglemetadataByParams(false, "cfTitolareFirma", Arrays.asList(stringaDiFirmatari.substring(0, stringaDiFirmatari.length() - 3)), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "repertorio", Arrays.asList(descrizioneClassificazione), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "idDocumentoOriginale", Arrays.asList(Integer.toString(doc.getId())), TESTO); 
        versamentoBuilder.addSinglemetadataByParams(false, "registro", Arrays.asList(codiceRegistro), TESTO);
        
        return versamentoBuilder;
    }
    
}
