package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders;

import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities.AllegatoUnimatica;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatorePluginException;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.AggType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.AllegatiType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.ChiaveDescrittivaType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.ClassificazioneType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.CodiceIPAType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.DatiDiRegistrazioneType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.Documento;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.DocumentoAmministrativoInformaticoType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.IdAggType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.IdDocType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.IdentificativoDelFormatoType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.ImprontaCrittograficaDelDocumentoType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.IndiceAllegatiType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.ModalitaDiFormazioneType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.NoProtocolloType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.PAIType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.ProdottoSoftwareType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.ProtocolloType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.RuoloType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.SoggettiType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.TipoAggregazioneType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.TipoRegistroType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.TipoSoggetto1Type;
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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.tika.detect.zip.IPADetector;
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

    public void build() throws VersatorePluginException, Exception {
        //prendo i parametri
        String tipoDocumento = doc.getTipologia().toString();
        Map<String, Object> mappaParametri = (Map<String, Object>) parametriVersamento.get(tipoDocumento);
        //dati amministrazione
        String denominazioneAmministrazione = (String) parametriVersamento.get("denominazioneAmministrazione");
        String codiceIpa = (String) parametriVersamento.get("codiceIpa");
        String codiceAOO = (String) parametriVersamento.get("codiceAOO");
        String denominazioneAOO = (String) parametriVersamento.get("denominazioneAOO");
        String mailAmministrazione = (String) parametriVersamento.get("mailAmministrazione");
        //creo l'intestazione del documento principale
        intestazione.setIdFile("idDoc" + doc.getId() + "_idArchivio" + archivio.getId() + "_" + documentoPrincipale.getNomeFile());
        intestazione.setNomeFile(documentoPrincipale.getNomeFile());
        intestazione.setPrincipale(true);
        //creo il profilo
        //TODO devo inserire i metadati specifici? al momento no
        //creo i metadati agid creando il Documento Amministrativo Informatico e settandone i campi
        DocumentoAmministrativoInformaticoType documentoAmministrativoInformatico = new DocumentoAmministrativoInformaticoType();
        IdDocType idDocPrimario = new IdDocType();
        ImprontaCrittograficaDelDocumentoType improntaCrittograficaDelDocumento = new ImprontaCrittograficaDelDocumentoType();
        //impronta del documento principale
        improntaCrittograficaDelDocumento.setImpronta(documentoPrincipale.getImpronta());
        //algoritmo del documento principale
        improntaCrittograficaDelDocumento.setAlgoritmo((String) parametriVersamento.get("algoritmo"));
        idDocPrimario.setImprontaCrittograficaDelDocumento(improntaCrittograficaDelDocumento);
        //come identificativo prendo l'id dell'allegato
        idDocPrimario.setIdentificativo(documentoPrincipale.getIdFile().toString());
        //segnatura
        //rgpico e documenti gedi non hanno segnatura
        if (doc.getTipologia().equals(Doc.TipologiaDoc.RGPICO) || doc.getTipologia().equals(Doc.TipologiaDoc.DOCUMENT_UTENTE)) {
            idDocPrimario.setSegnatura("Segnatura non presente");
        } else {
            idDocPrimario.setSegnatura("Fare riferimento all'allegato segnatura.xml");
        }

        //identificativo del documento
        documentoAmministrativoInformatico.setIdDoc(idDocPrimario);
        //modalità di formazione
        documentoAmministrativoInformatico.setModalitaDiFormazione(ModalitaDiFormazioneType.CREAZIONE_TRAMITE_UTILIZZO_DI_STRUMENTI_SOFTWARE_CHE_ASSICURINO_LA_PRODUZIONE_DI_DOCUMENTI_NEI_FORMATI_PREVISTI_IN_ALLEGATO_2);
        //tipologia documentale
        //Prendo il registro ufficiale e attivo del documento
        List<RegistroDoc> registroDocList = doc.getRegistroDocList();
        Registro registro = null;
        RegistroDoc registroDocDocumentoPrincipale = null;
        for (RegistroDoc registroDoc : registroDocList) {
            if (registroDoc.getIdRegistro().getAttivo() && registroDoc.getIdRegistro().getUfficiale()) {
                registro = registroDoc.getIdRegistro();
                registroDocDocumentoPrincipale = registroDoc;
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
        //tipo registro
        TipoRegistroType tipoRegistro = new TipoRegistroType();
        if (doc.getTipologia().equals(Doc.TipologiaDoc.PROTOCOLLO_IN_ENTRATA) || doc.getTipologia().equals(Doc.TipologiaDoc.PROTOCOLLO_IN_USCITA)) {
            ProtocolloType protocolloType = new ProtocolloType();
            protocolloType.setTipoRegistro("ProtocolloOrdinario\\ProtocolloEmergenza");
            protocolloType.setDataProtocollazioneDocumento(toXMLGregorianCalendar(registroDocDocumentoPrincipale.getDataRegistrazione()));
            protocolloType.setNumeroProtocolloDocumento(registroDocDocumentoPrincipale.getNumero().toString());
            protocolloType.setCodiceRegistro(registro.getCodice().toString());
            tipoRegistro.setProtocolloOrdinarioProtocolloEmergenza(protocolloType);
        } else {
            NoProtocolloType noProtocolloType = new NoProtocolloType();
            noProtocolloType.setTipoRegistro("Repertorio\\Registro");
            noProtocolloType.setDataRegistrazioneDocumento(toXMLGregorianCalendar(registroDocDocumentoPrincipale.getDataRegistrazione()));
            noProtocolloType.setNumeroRegistrazioneDocumento(registroDocDocumentoPrincipale.getNumero().toString());
            noProtocolloType.setCodiceRegistro(registro.getCodice().toString());
            tipoRegistro.setRepertorioRegistro(noProtocolloType);
        }
        datiDiRegistrazione.setTipoRegistro(tipoRegistro);
        documentoAmministrativoInformatico.setDatiDiRegistrazione(datiDiRegistrazione);
        //soggetti
        SoggettiType soggetti = new SoggettiType();
        //amministrazione
        RuoloType ruoloAmministrazione = new RuoloType();
        TipoSoggetto1Type tipoAmministrazione = new TipoSoggetto1Type();
        PAIType paiAmministrazione = new PAIType();
        CodiceIPAType ipaammAmministrazione = new CodiceIPAType();
        ipaammAmministrazione.setDenominazione(denominazioneAmministrazione);
        ipaammAmministrazione.setCodiceIPA(codiceIpa);
        paiAmministrazione.setIPAAmm(ipaammAmministrazione);
        CodiceIPAType ipaaooAmministrazione = new CodiceIPAType();
        ipaaooAmministrazione.setDenominazione(denominazioneAOO);
        ipaaooAmministrazione.setCodiceIPA(codiceAOO);
        paiAmministrazione.setIPAAOO(ipaaooAmministrazione);
        paiAmministrazione.getIndirizziDigitaliDiRiferimento().add(mailAmministrazione);
        tipoAmministrazione.setTipoRuolo("Amministrazione Che Effettua La Registrazione");
        tipoAmministrazione.setPAI(paiAmministrazione);
        ruoloAmministrazione.setAmministrazioneCheEffettuaLaRegistrazione(tipoAmministrazione);
        soggetti.getRuolo().add(ruoloAmministrazione);
        //TODO assegnatari per ora non mandati
        //destinatari

        documentoAmministrativoInformatico.setSoggetti(soggetti);

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
        //formato
        IdentificativoDelFormatoType identificativoDelFormato = new IdentificativoDelFormatoType();
        identificativoDelFormato.setFormato(documentoPrincipale.getFormato());
        ProdottoSoftwareType prodottoSoftware = new ProdottoSoftwareType();
        prodottoSoftware.setNomeProdotto((String) parametriVersamento.get("descrizioneSoftware"));
        prodottoSoftware.setProduttore((String) parametriVersamento.get("produttore"));
        identificativoDelFormato.setProdottoSoftware(prodottoSoftware);
        documentoAmministrativoInformatico.setIdentificativoDelFormato(identificativoDelFormato);
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

    /**
    metodo per la conversione delle date nell'xml
    @param zdt
    @return
    @throws Exception
     */
    public static XMLGregorianCalendar toXMLGregorianCalendar(ZonedDateTime zdt) throws Exception {
        GregorianCalendar gregorianCalendar = GregorianCalendar.from(zdt);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
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
