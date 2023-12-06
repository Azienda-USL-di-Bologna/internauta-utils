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
import java.util.Arrays;
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
    private String numeroIniziale;
    private String numeroFinale;
    private ZonedDateTime dataIniziale;
    private ZonedDateTime dataFinale;

    public RgPicoBuilder(Doc doc, DocDetail docDetail, Archivio archivio, Registro registro, List<Persona> firmatari, Map<String, Object> parametriVersamento, String numeroIniziale, String numeroFinale, ZonedDateTime dataIniziale, ZonedDateTime dataFinale) {
        this.versamentoBuilder = new VersamentoBuilder();
        this.doc = doc;
        this.docDetail = docDetail;
        this.archivio = archivio;
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
        String idClassifica = archivio.getIdTitolo().getId().toString();
        String classificazioneArchivistica = archivio.getIdTitolo().getClassificazione();
        String nomeSistemaVersante = (String) parametriVersamento.get("idSistemaVersante");
        //TODO in futuro prendere da db scripta.registro
        String codiceRegistro = (String) mappaParametri.get("codiceRegistro");
        //String codiceRegistro = registro.getCodice().toString();
        String numeroProgressivo = docDetail.getNumeroRegistrazione().toString();
        String annoRegistrazione = docDetail.getAnnoRegistrazione().toString();
        
        versamentoBuilder.setDocType(docType);
        versamentoBuilder.addSinglemetadataByParams(true, "id_ente_versatore", Arrays.asList(codiceEneteVersatore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "idTipoDoc", Arrays.asList(docType), TESTO);
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
        versamentoBuilder.addSinglemetadataByParams(false, "Numero_progressivo_del_registro", Arrays.asList(numeroProgressivo), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "Anno", Arrays.asList(annoRegistrazione), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "Numero_ultima_registrazione_effettuata_sul_registro", Arrays.asList(numeroFinale), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "Data_ultima_registrazione_effettuata_sul_registro", Arrays.asList(dataFinale.format(formatter)), DATA);
        versamentoBuilder.addSinglemetadataByParams(false, "Numero_prima_registrazione_effettuata_sul_registro", Arrays.asList(numeroIniziale), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "Data_prima_registrazione_effettuata_sul_registro", Arrays.asList(dataIniziale.format(formatter)), DATA);
        versamentoBuilder.addSinglemetadataByParams(false, "idDocumentoOriginale", Arrays.asList(Integer.toString(doc.getId())), TESTO);
        //TODO gli rg pico non hanno anni tenuta, al momento nel tracciato non c'è
        //TODO parametro non presente originariamente nel tracciato
        versamentoBuilder.addSinglemetadataByParams(false, "registro", Arrays.asList(codiceRegistro), TESTO);
        
        return versamentoBuilder;
        
    }
    
}
