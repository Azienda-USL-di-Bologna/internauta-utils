/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders;

import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreSdicoException;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders.DeliBuilder;
import it.bologna.ausl.internauta.utils.versatore.utils.SdicoVersatoreUtils;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
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
public class DocumentoGEDIBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(DeliBuilder.class);
    private static final String CODICE = "DOCUMENT_UTENTE";
    private static final String TESTO = "TESTO";
    private static final String DATA = "DATA";
    private static final String TESTO_MULTIPLO = "TESTO MULTIPLO";
    
    private VersamentoBuilder versamentoBuilder;
    private Doc doc;
    private DocDetail docDetail;
    private Archivio archivio;
    private ArchivioDetail archivioDetail;
    private Registro registro;
    private Map<String, Object> parametriVersamento;

    public DocumentoGEDIBuilder(Doc doc, DocDetail docDetail, Archivio archivio, ArchivioDetail archivioDetail, Registro registro, Map<String, Object> parametriVersamento) {
        this.versamentoBuilder = new VersamentoBuilder();
        this.doc = doc;
        this.docDetail = docDetail;
        this.archivio = archivio;
        this.archivioDetail = archivioDetail;
        this.registro = registro;
        this.parametriVersamento = parametriVersamento;
    }
    
     /**
     * Metodo che costruisce i metadati per i documenti GEDI (tipologia documento generico id tipo doc 85)
     * @return 
     */
    public VersamentoBuilder build() throws VersatoreSdicoException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Map<String, String> mappaParametri = (Map<String, String>) parametriVersamento.get(CODICE);
        String docType = (String) mappaParametri.get("idTipoDoc");
        String codiceEneteVersatore = (String) parametriVersamento.get("ente");
        String idClassifica = archivio.getIdTitolo().getIdClassificaDaEsterno().toString();
        String classificazioneArchivistica = archivio.getIdTitolo().getClassificazione();
        String repertorio = mappaParametri.get("repertorio");
        DecimalFormat df = new DecimalFormat("0000000");
        String numeroDocumento = df.format(docDetail.getNumeroRegistrazione());
        String nomeSistemaVersante = (String) parametriVersamento.get("idSistemaVersante");
        String codiceRegistro = registro.getCodice().toString();
        //TODO in futuro prendere da db scripta.registro
        //String codiceRegistro = (String) mappaParametri.get("codiceRegistro");
        String anniTenuta = "illimitato";
        if (archivio.getAnniTenuta() != 999) {
            anniTenuta = Integer.toString(archivio.getAnniTenuta());
        }
        String descrizioneClassificazione = archivio.getIdTitolo().getNome();
        String tipologiaDiFlusso = (String) mappaParametri.get("tipologiaDiFlusso");
        String firmatoDigitalmente = (String) mappaParametri.get("firmatoDigitalmente");
        String marcaturaTemporale = (String) mappaParametri.get("marcaturaTemporale");
        String riservato = (String) mappaParametri.get("riservato");
        String sigillatoElettronicamente = (String) mappaParametri.get("sigillatoElettronicamente");
        String descrizioneSoftware = (String) parametriVersamento.get("descrizioneSoftware");
        String produttore = (String) parametriVersamento.get("produttore");
        String tipoRegistro = (String) mappaParametri.get("tipoRegistro");
        String modalitaDiFormazione = (String) parametriVersamento.get("modalitaDiFormazione");
        String nomeDelDocumento = "";
        List<Allegato> listaAllegati = doc.getAllegati();
        if (listaAllegati.size() > 0 && !listaAllegati.isEmpty()) {
            nomeDelDocumento = listaAllegati.get(0).getDettagli().getOriginale().getNome() 
                    + "." 
                    + listaAllegati.get(0).getDettagli().getOriginale().getEstensione();
        } else {
            throw new VersatoreSdicoException("Il documento GEDI non contiene allegati");
        }
        String pianoDiClassificazione = (String) parametriVersamento.get("pianoDiClassificazione");
        
        versamentoBuilder.setDocType(docType);
        versamentoBuilder.addSinglemetadataByParams(true, "id_ente_versatore", Arrays.asList(codiceEneteVersatore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "idTipoDoc", Arrays.asList(docType), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "idClassifica", Arrays.asList(idClassifica), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "classificazioneArchivistica", Arrays.asList(classificazioneArchivistica), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "oggettodocumento", Arrays.asList(doc.getOggetto()), TESTO);
        //versamentoBuilder.addSinglemetadataByParams(false, "numeroProtocollo", Arrays.asList(numeroDocumento), TESTO);
        //versamentoBuilder.addSinglemetadataByParams(false, "dataRegistrazioneProtocollo", Arrays.asList(docDetail.getDataRegistrazione().format(formatter)), DATA);
        versamentoBuilder.addSinglemetadataByParams(false, "amministrazioneTitolareDelProcedimento", Arrays.asList(codiceEneteVersatore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "aooDiRiferimento", Arrays.asList((String) parametriVersamento.get("aooDiRiferimento")), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "idFascicolo", Arrays.asList(SdicoVersatoreUtils.buildIdFascicoli(doc, archivio)), TESTO_MULTIPLO);
        versamentoBuilder.addSinglemetadataByParams(false, "idSistemaVersante", Arrays.asList(nomeSistemaVersante), TESTO);
        //TODO da concordare: per ora "Registrazioni in fascicolo"
        versamentoBuilder.addSinglemetadataByParams(false, "repertorio", Arrays.asList(repertorio), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "idDocumentoOriginale", Arrays.asList(Integer.toString(doc.getId())), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "applicativoProduzione", Arrays.asList((String) parametriVersamento.get("applicativoProduzione")), TESTO);
        //TODO vedere anche qui come settare il registro
        versamentoBuilder.addSinglemetadataByParams(false, "registro", Arrays.asList(codiceRegistro), TESTO);
        //versamentoBuilder.addSinglemetadataByParams(false, "annotazione", Arrays.asList(anniTenuta), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "ufficioProduttore", Arrays.asList(archivioDetail.getIdStruttura().getNome()), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "dataAltraRegistrazione", Arrays.asList(docDetail.getDataRegistrazione().format(formatter)), DATA);
        
        //metadati aggiunti nel passaggio alla tipologia 85
        
        versamentoBuilder.addSinglemetadataByParams(false, "descrizione_classificazione", Arrays.asList(descrizioneClassificazione), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "tempo_di_conservazione", Arrays.asList(anniTenuta), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "numero_documento", Arrays.asList(numeroDocumento), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "data_di_registrazione", Arrays.asList(docDetail.getDataRegistrazione().format(formatter)), DATA);
        versamentoBuilder.addSinglemetadataByParams(false, "tipologia_di_flusso", Arrays.asList(tipologiaDiFlusso), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "tipo_registro", Arrays.asList(tipoRegistro), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "codice_registro", Arrays.asList(codiceRegistro), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "firmato_digitalmente", Arrays.asList(firmatoDigitalmente), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "marcatura_temporale", Arrays.asList(marcaturaTemporale), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "riservato", Arrays.asList(riservato), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "sigillato_elettronicamente", Arrays.asList(sigillatoElettronicamente), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "prodotto_software_nome_prodotto", Arrays.asList(descrizioneSoftware), TESTO);
        //versamentoBuilder.addSinglemetadataByParams(false, "prodotto_software_versione_prodotto", Arrays.asList(descrizioneSoftware), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "prodotto_software_produttore", Arrays.asList(produttore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "modalita_di_formazione", Arrays.asList(modalitaDiFormazione), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "nome_del_documento", Arrays.asList(nomeDelDocumento), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "indice_di_classificazione", Arrays.asList(classificazioneArchivistica + " - " + descrizioneClassificazione), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "piano_di_classificazione", Arrays.asList(pianoDiClassificazione), TESTO);
        
        return versamentoBuilder;
    }
    
}
