package it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders;

import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreSdicoException;
import it.bologna.ausl.internauta.utils.versatore.utils.SdicoVersatoreUtils;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.AttoreDoc;
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
 * @author Andrea
 */
public class DeteBuilder {

    private static final Logger log = LoggerFactory.getLogger(DeteBuilder.class);
    private static final String CODICE = "DETERMINA";
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

    public DeteBuilder(Doc doc, DocDetail docDetail, Archivio archivio, Registro registro, List<Persona> firmatari, Map<String, Object> parametriVersamento) {
        versamentoBuilder = new VersamentoBuilder();
        this.doc = doc;
        this.docDetail = docDetail;
        this.archivio = archivio;
        this.registro = registro;
        this.firmatari = firmatari;
        this.parametriVersamento = parametriVersamento;
    }

    /**
     * Metodo che costruisce i metadati per le determine (id tipo doc 82)
     *
     * @return
     */
    public VersamentoBuilder build() throws VersatoreSdicoException {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Map<String, String> mappaParametri = (Map<String, String>) parametriVersamento.get(CODICE);
        String docType = (String) mappaParametri.get("idTipoDoc");
        String codiceEneteVersatore = (String) parametriVersamento.get("ente");
        String idClassifica = archivio.getIdTitolo().getId().toString();
        String classificazioneArchivistica = archivio.getIdTitolo().getClassificazione();
        String descrizioneClassificazione = archivio.getIdTitolo().getNome();
        String repertorio = mappaParametri.get("repertorio");
        String anniTenuta = "illimitato";
        if (archivio.getAnniTenuta() != 999) {
            anniTenuta = Integer.toString(archivio.getAnniTenuta());
        }
        DecimalFormat df = new DecimalFormat("0000000");
        String numeroDocumento = df.format(docDetail.getNumeroRegistrazione());
        String tipologiaDiFlusso = (String) mappaParametri.get("tipologiaDiFlusso");
        String ufficioProduttore = null;
        List<AttoreDoc> attoriDocList = doc.getAttoriList();
        for (AttoreDoc attoreDoc : attoriDocList) {
            if (attoreDoc.getRuolo() == AttoreDoc.RuoloAttoreDoc.FIRMA) {
                ufficioProduttore = attoreDoc.getIdStruttura().getNome();
            }
        }
        if (ufficioProduttore == null) {
            throw new VersatoreSdicoException("La Determina non ha Ufficio Produttore");
        }
        //String codiceRegistro = registro.getCodice().toString();
        //TODO in futuro prendere da db scripta.registro
        String codiceRegistro = (String) mappaParametri.get("codiceRegistro");
        String nomeSistemaVersante = (String) parametriVersamento.get("idSistemaVersante");
        String firmatoDigitalmente = (String) mappaParametri.get("firmatoDigitalmente");
        String marcaturaTemporale = (String) mappaParametri.get("marcaturaTemporale");
        String riservato = (String) mappaParametri.get("riservato");
        String stringaDiFirmatari = "";
        if (firmatari.size() > 0) {
            for (Persona firmatario : firmatari) {
                stringaDiFirmatari += firmatario.getCodiceFiscale() + " - ";
            }
        } else {
            throw new VersatoreSdicoException("La Determina non ha firmatari");
        }

        versamentoBuilder.setDocType(docType);
        versamentoBuilder.addSinglemetadataByParams(true, "id_ente_versatore", Arrays.asList(codiceEneteVersatore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "idTipoDoc", Arrays.asList(docType), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "idClassifica", Arrays.asList(idClassifica), TESTO);
        versamentoBuilder.addSinglemetadataByParams(true, "classificazioneArchivistica", Arrays.asList(classificazioneArchivistica), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "amministrazioneTitolareDelProcedimento", Arrays.asList(codiceEneteVersatore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "aooDiRiferimento", Arrays.asList((String) parametriVersamento.get("aooDiRiferimento")), TESTO);
        //versamentoBuilder.addSinglemetadataByParams(false, "descrizione_classificazione", Arrays.asList(descrizioneClassificazione), TESTO);
        //TODO per ora così, capire come va inserita
        versamentoBuilder.addSinglemetadataByParams(false, "descrizione_classificazione", Arrays.asList("Determinazioni"), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "tempo_di_conservazione", Arrays.asList(anniTenuta), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "oggettodocumento", Arrays.asList(doc.getOggetto()), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "repertorio", Arrays.asList(repertorio), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "numero_documento", Arrays.asList(numeroDocumento), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "data_di_registrazione", Arrays.asList(docDetail.getDataRegistrazione().format(formatter)), DATA);
        versamentoBuilder.addSinglemetadataByParams(false, "tipologia_di_flusso", Arrays.asList(tipologiaDiFlusso), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "ufficioProduttore", Arrays.asList(ufficioProduttore), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "responasbileProcedimento", Arrays.asList(docDetail.getIdPersonaResponsabileProcedimento().getDescrizione()), TESTO);
        versamentoBuilder.addSinglemetadataByParams(false, "idFascicolo", SdicoVersatoreUtils.buildListaIdFascicolo(doc, archivio), TESTO_MULTIPLO);
        versamentoBuilder.addSinglemetadataByParams(false, "registro", Arrays.asList(codiceRegistro), TESTO);
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

        return versamentoBuilder;
    }
}
