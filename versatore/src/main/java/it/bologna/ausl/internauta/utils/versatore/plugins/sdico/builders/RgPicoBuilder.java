/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders;

import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders.DeliBuilder;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.Registro;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
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
    private Registro registro;
    private List<Persona> firmatari;
    private Map<String, Object> parametriVersamento;
    private String numeroIniziale;
    private String numeroFinale;
    private ZonedDateTime dataIniziale;
    private ZonedDateTime dataFinale;

    public RgPicoBuilder(Doc doc, DocDetail docDetail, Registro registro, List<Persona> firmatari, Map<String, Object> parametriVersamento, String numeroIniziale, String numeroFinale, ZonedDateTime dataIniziale, ZonedDateTime dataFinale) {
        this.versamentoBuilder = new VersamentoBuilder();
        this.doc = doc;
        this.docDetail = docDetail;
        this.registro = registro;
        this.firmatari = firmatari;
        this.parametriVersamento = parametriVersamento;
        this.numeroIniziale = numeroIniziale;
        this.numeroFinale = numeroFinale;
        this.dataIniziale = dataIniziale;
        this.dataFinale = dataFinale;
    }
    
    /**
     * Metodo che costruisce i metadati per i registri giornalieri di PICO (id tipo doc 8001)
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
        //TODO in futuro prendere da db scripta.registro
        String codiceRegistro = (String) mappaParametri.get("codiceRegistro");
        //String codiceRegistro = registro.getCodice().toString();
        String annoRegistrazione = docDetail.getAnnoRegistrazione().toString();
        
        versamentoBuilder.setDocType(docType);
        versamentoBuilder.addSinglemetadataByParams(true, "id_ente_versatore", Arrays.asList(codiceEneteVersatore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "idTipoDoc", Arrays.asList(docType), TESTO);
        //TODO da vedere se cambiano in base alla tipologia
        //TODO ANCORA DA avere
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
        versamentoBuilder.addSinglemetadataByParams(false, "Codice_identificativo_del_registro", Arrays.asList(codiceRegistro), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "Numero_progressivo_del_registro", Arrays.asList(annoRegistrazione), TESTO);
        //TODO chiedere se è corretto prendere annoRegistrazione, direi di sì
        versamentoBuilder.addSinglemetadataByParams(false, "Anno", Arrays.asList(docDetail.getAnnoRegistrazione().toString()), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "Numero_ultima_registrazione_effettuata_sul_registro", Arrays.asList(numeroFinale), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "Data_ultima_registrazione_effettuata_sul_registro", Arrays.asList(dataFinale.format(formatter)), DATA);
        versamentoBuilder.addSinglemetadataByParams(false, "Numero_prima_registrazione_effettuata_sul_registro", Arrays.asList(numeroIniziale), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "Data_prima_registrazione_effettuata_sul_registro", Arrays.asList(dataIniziale.format(formatter)), DATA);
        versamentoBuilder.addSinglemetadataByParams(false, "idDocumentoOriginale", Arrays.asList(Integer.toString(doc.getId())), TESTO);
        
        return versamentoBuilder;
        
    }
    
}
