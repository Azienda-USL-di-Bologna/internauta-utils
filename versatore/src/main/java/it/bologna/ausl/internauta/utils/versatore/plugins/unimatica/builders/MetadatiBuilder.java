package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders;

import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatorePluginException;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.AggType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.AllegatiType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.ChiaveDescrittivaType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.ClassificazioneType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.DatiDiRegistrazioneType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.Documento;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.DocumentoAmministrativoInformaticoType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.IdAggType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.IdDocType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.ImprontaCrittograficaDelDocumentoType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.IndiceAllegatiType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.ModalitaDiFormazioneType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.TipoAggregazioneType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.TipoRegistroType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.TipologiaDiFlussoType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.VerificaType;
import static it.bologna.ausl.internauta.utils.versatore.utils.SdicoVersatoreUtils.buildIdFascicolo;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.AttoreDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.Registro;
import it.bologna.ausl.model.entities.scripta.RegistroDoc;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author boria
 */
public class MetadatiBuilder {

    private static final Logger log = LoggerFactory.getLogger(MetadatiBuilder.class);
    private Map<String, Object> parametriVersamento;
    private Doc doc;
    private Archivio archivio;
    private AllegatoUnimatica documentoPrincipale;
    private List<AllegatoUnimatica> allegatiSecondariList;
    private Documento documento;
    private Documento.Intestazione intestazione;
    private Documento.Profilo profilo;
    private Documento.Profilo.MetadatiAGID metadatiAGID;
    private Marshaller marshaller;
    private String codificaMarshaller;

    public MetadatiBuilder(Map<String, Object> parametriVersamento, Doc doc, Archivio archivio, AllegatoUnimatica documentoPrincipale, List<AllegatoUnimatica> allegatiSecondariList) {
        try {
            this.doc = doc;
            this.archivio = archivio;
            this.documentoPrincipale = documentoPrincipale;
            this.allegatiSecondariList = allegatiSecondariList;
            this.parametriVersamento = parametriVersamento;

            JAXBContext jaxb = JAXBContext.newInstance(Documento.class);
            codificaMarshaller = "UTF-8";
            marshaller = jaxb.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, codificaMarshaller);
            documento = new Documento();
            intestazione = new Documento.Intestazione();
            profilo = new Documento.Profilo();
            metadatiAGID = new Documento.Profilo.MetadatiAGID();
            documento.setIntestazione(intestazione);
            profilo.setMetadatiAGID(metadatiAGID);
            documento.setProfilo(profilo);

        } catch (JAXBException ex) {
            log.error("errore nella costruzione di VersamentoBuilder", ex);
        }
    }

    public void build() throws VersatorePluginException {
        //prendo i parametri
        String tipoDocumento = doc.getTipologia().toString();
        Map<String, Object> mappaParametri = (Map<String, Object>) parametriVersamento.get(tipoDocumento);
        //creo l'intestazione del documento principale
        intestazione.setIdFile(documentoPrincipale.getIdFile().toString());
        intestazione.setNomeFile(documentoPrincipale.getNomeFile());
        intestazione.setPrincipale(true);
        //creo il profilo
        //TODO devo inserire i metadati specifici?
        //creo i metadati agid creando il Documento Amministrativo Informatico e settandone i campi
        DocumentoAmministrativoInformaticoType documentoAmministrativoInformatico = new DocumentoAmministrativoInformaticoType();
        IdDocType idDocPrimario = new IdDocType();
        ImprontaCrittograficaDelDocumentoType improntaCrittograficaDelDocumento = new ImprontaCrittograficaDelDocumentoType();
        //impronta del documento principale
        improntaCrittograficaDelDocumento.setImpronta(documentoPrincipale.getImpronta());
        //algoritmo del documento principale
        improntaCrittograficaDelDocumento.setAlgoritmo((String) parametriVersamento.get("algoritmo"));
        idDocPrimario.setImprontaCrittograficaDelDocumento(improntaCrittograficaDelDocumento);
        //TODO identificativo cosa prendo?
        //TODO e segnatura?
        //identificativo del documento
        documentoAmministrativoInformatico.setIdDoc(idDocPrimario);
        //modalità di formazione
        documentoAmministrativoInformatico.setModalitaDiFormazione(ModalitaDiFormazioneType.CREAZIONE_TRAMITE_UTILIZZO_DI_STRUMENTI_SOFTWARE_CHE_ASSICURINO_LA_PRODUZIONE_DI_DOCUMENTI_NEI_FORMATI_PREVISTI_IN_ALLEGATO_2);
        //tipologia documentale
        //Prendo il registro ufficiale e attivo del documento
        List<RegistroDoc> registroDocList = doc.getRegistroDocList();
        Registro registro = null;
        for (RegistroDoc registroDoc : registroDocList) {
            if (registroDoc.getIdRegistro().getAttivo() && registroDoc.getIdRegistro().getUfficiale()) {
                registro = registroDoc.getIdRegistro();
                break;
            }
        }
        if (registro == null) {
            throw new VersatorePluginException("Non è presente un registro ufficiale e attivo per il documento con id " + doc.getId());
        }
        documentoAmministrativoInformatico.setTipologiaDocumentale(registro.getDescrizione());
        DatiDiRegistrazioneType datiDiRegistrazione = new DatiDiRegistrazioneType();
        //tipologia di flusso
        String tipologiaDiFlusso = null;
        if (doc.getTipologia().equals(Doc.TipologiaDoc.PROTOCOLLO_IN_USCITA)) {
            Map<String, Object> tipiDiFlusso = (Map<String, Object>) mappaParametri.get("tipologiaDiFlusso");
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
        } else {
            tipologiaDiFlusso = (String) mappaParametri.get("tipologiaDiFlusso");
        }
        TipologiaDiFlussoType tipologiaDiFlussoType = null;
        switch (tipologiaDiFlusso) {
            case "E":
                tipologiaDiFlussoType = TipologiaDiFlussoType.E;
                break;
            case "U":
                tipologiaDiFlussoType = TipologiaDiFlussoType.U;
                break;
            case "I":
                tipologiaDiFlussoType = TipologiaDiFlussoType.I;
                break;
            default:
                throw new VersatorePluginException("Tipologia di flusso assente o non prevista per il documento con id " + doc.getId());
        }
        datiDiRegistrazione.setTipologiaDiFlusso(tipologiaDiFlussoType);
        //TODO tipo registro
        documentoAmministrativoInformatico.setDatiDiRegistrazione(datiDiRegistrazione);
        //TODO soggetti
        ChiaveDescrittivaType chiaveDescrittiva = new ChiaveDescrittivaType();
        //oggetto
        chiaveDescrittiva.setOggetto(doc.getOggetto());
        documentoAmministrativoInformatico.setChiaveDescrittiva(chiaveDescrittiva);
        //allegati
        if (allegatiSecondariList.size() > 0) {
            AllegatiType allegati = new AllegatiType();
            allegati.setNumeroAllegati(allegatiSecondariList.size());
            for (AllegatoUnimatica allegatoUnimatica : allegatiSecondariList) {
                IndiceAllegatiType indiceAllegati = new IndiceAllegatiType();
                IdDocType idDocAllegato = new IdDocType();
                ImprontaCrittograficaDelDocumentoType improntaCrittograficaDelDocumentoAllegato = new ImprontaCrittograficaDelDocumentoType();
                improntaCrittograficaDelDocumentoAllegato.setImpronta(allegatoUnimatica.getImpronta());
                improntaCrittograficaDelDocumentoAllegato.setAlgoritmo((String) parametriVersamento.get("algoritmo"));
                idDocAllegato.setImprontaCrittograficaDelDocumento(improntaCrittograficaDelDocumentoAllegato);
                idDocAllegato.setIdentificativo(allegatoUnimatica.getIdFile().toString());
                indiceAllegati.setIdDoc(idDocAllegato);
                indiceAllegati.setDescrizione(allegatoUnimatica.getNomeFile());
                allegati.getIndiceAllegati().add(indiceAllegati);
            }
            documentoAmministrativoInformatico.setAllegati(allegati);
        }
        ClassificazioneType classificazione = new ClassificazioneType();
        //classificazione
        classificazione.setIndiceDiClassificazione(archivio.getIdTitolo().getClassificazione());
        classificazione.setDescrizione(archivio.getIdTitolo().getDescrizione());
        documentoAmministrativoInformatico.setClassificazione(classificazione);
        //riservato
        documentoAmministrativoInformatico.setRiservato(doc.getVisibilita().equals(Doc.VisibilitaDoc.RISERVATO));
        //TODO formato
        VerificaType verifica = new VerificaType();
        //firmato digitalmente
        if (doc.getTipologia().equals(Doc.TipologiaDoc.PROTOCOLLO_IN_ENTRATA)) {
            verifica.setFirmatoDigitalmente(documentoPrincipale.getFirmato());
        } else {
            verifica.setFirmatoDigitalmente((boolean) mappaParametri.get("firmatoDigitalmente"));
        }
        //sigillato elettornicamente
        verifica.setSigillatoElettronicamente((boolean) mappaParametri.get("sigillatoElettronicamente"));
        //marcatura temporale
        verifica.setMarcaturaTemporale((boolean) mappaParametri.get("marcaturaTemporale"));
        documentoAmministrativoInformatico.setVerifica(verifica);
        //fascicoli
        AggType agg = new AggType();
        for (ArchivioDoc archivioDoc : doc.getArchiviDocList()) {
            if (archivioDoc.getDataEliminazione() == null && archivioDoc.getIdArchivio().getIdArchivioRadice().getId() == archivio.getIdArchivioRadice().getId()) {
                IdAggType idAgg = new IdAggType();
                idAgg.setTipoAggregazione(TipoAggregazioneType.FASCICOLO);
                idAgg.setIdAggregazione(buildIdFascicolo(archivioDoc.getIdArchivio()));
                agg.getTipoAgg().add(idAgg);
            }
        }
        documentoAmministrativoInformatico.setAgg(agg);
        documentoAmministrativoInformatico.setIdIdentificativoDocumentoPrimario(idDocPrimario);
        //nome del documento principale
        documentoAmministrativoInformatico.setNomeDelDocumento(documentoPrincipale.getNomeFile());
        //TODO versione del documento?
        //tempo di conservazione
        Integer tempoDiConservazione = 9999;
        if (archivio.getAnniTenuta() != 999) {
            tempoDiConservazione = archivio.getAnniTenuta();
        }
        documentoAmministrativoInformatico.setTempoDiConservazione(tempoDiConservazione);
        //note

        metadatiAGID.setDocumentoAmministrativoInformatico(documentoAmministrativoInformatico);
    }

    /**
     * Metodo che formatta il singolo idFascicolo
     * @param archivio
     * @return
     */
    public static String buildIdFascicolo(Archivio archivio) {
        String numero = archivio.getNumero().toString();
        if (archivio.getIdArchivioPadre() != null) {
            numero = archivio.getIdArchivioPadre().getNumero().toString() + "-" + numero;
            if (archivio.getIdArchivioPadre().getIdArchivioPadre() != null) {
                numero = archivio.getIdArchivioPadre().getIdArchivioPadre().getNumero() + "-" + numero;
            }
        }
        return (numero + "/" + archivio.getAnno() + " [id_" + archivio.getId() + "]");
    }

    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String out;
        try {
            marshaller.marshal(documento, baos);
        } catch (JAXBException e) {
            return "Document Error unable to serialize: " + e;
        }

        try {
            out = new String(baos.toByteArray(), codificaMarshaller);
        } catch (UnsupportedEncodingException e) {
            return "Document Error unable to serialize with coding " + codificaMarshaller + ": " + e;
        }

        return out;
    }
}
