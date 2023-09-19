/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.sdico;

import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.Registro;
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
public class RgPicoBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(DeliBuilder.class);
    private static final String CODICE = "RGPICO";
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

    public RgPicoBuilder(VersamentoBuilder versamentoBuilder, Doc doc, DocDetail docDetail, Archivio archivio, Registro registro, List<Persona> firmatari, Map<String, Object> parametriVersamento) {
        this.versamentoBuilder = versamentoBuilder;
        this.doc = doc;
        this.docDetail = docDetail;
        this.archivio = archivio;
        this.registro = registro;
        this.firmatari = firmatari;
        this.parametriVersamento = parametriVersamento;
    }
    
    /**
     * Metodo che costruisce i metadati per i registri giornalieri di PICO (id tipo doc 8001)
     * @return 
     */
    public VersamentoBuilder build() {
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Map<String, String> mappaParametri = (Map<String, String>) parametriVersamento.get(CODICE);
        String docType = (String) mappaParametri.get("idTipoDoc");
        String codiceEneteVersatore = (String) parametriVersamento.get("ente");
        String idClassifica = (String) mappaParametri.get("idClassifica");
        String classificazioneArchivistica = (String) mappaParametri.get("classificazioneArchivistica");
        String nomeSistemaVersante = (String) parametriVersamento.get("idSistemaVersante");
        
        versamentoBuilder.setDocType(docType);
        versamentoBuilder.addSinglemetadataByParams(true, "id_ente_versatore", Arrays.asList(codiceEneteVersatore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "idTipoDoc", Arrays.asList(docType), TESTO);
        //TODO da vedere se cambiano in base alla tipologia
        versamentoBuilder.addSinglemetadataByParams(true, "idClassifica", Arrays.asList(idClassifica), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "classificazioneArchivistica", Arrays.asList(classificazioneArchivistica), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "oggettodocumento", Arrays.asList(doc.getOggetto()), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "amministrazioneTitolareDelProcedimento", Arrays.asList(codiceEneteVersatore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "aooDiRiferimento", Arrays.asList((String) parametriVersamento.get("aooDiRiferimento")), TESTO);
        //TODO da azero sapere se aggiungere: Cognome_responsabile_gestione_documentale
        //TODO da azero sapere se aggiungere: Nome_responsabile_gestione_documentale
        //TODO da azero sapere se aggiungere: Codice_fiscale_responsabile_gestione_documentale
        //TODO da azero sapere se aggiungere: Denominazione_dellamministrazione
        versamentoBuilder.addSinglemetadataByParams(false, "idSistemaVersante", Arrays.asList(nomeSistemaVersante), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "applicativoProduzione", Arrays.asList((String) parametriVersamento.get("applicativoProduzione")), TESTO);
        //TODO da azero sapere se aggiungere: ufficioProduttore
        
        return versamentoBuilder;
        
    }
    
}
