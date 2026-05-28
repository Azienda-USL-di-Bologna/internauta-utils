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
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.PAEType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.PAIType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.PFType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.PGType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.ProdottoSoftwareType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.ProtocolloType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.RuoloType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.SWType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.SoggettiType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.TipoAggregazioneType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.TipoRegistroType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.TipoSoggetto1Type;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.TipoSoggetto31Type;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.TipoSoggetto32Type;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.TipoSoggetto41Type;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.TipoSoggetto5Type;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.TipologiaDiFlussoType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.VerificaType;
import it.bologna.ausl.internauta.utils.versatore.utils.IpaUtils;
import it.bologna.ausl.internauta.utils.versatore.utils.UnimaticaVersatoreUtils;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import static it.bologna.ausl.model.entities.rubrica.Contatto.TipoContatto.AZIENDA;
import static it.bologna.ausl.model.entities.rubrica.Contatto.TipoContatto.FORNITORE;
import static it.bologna.ausl.model.entities.rubrica.Contatto.TipoContatto.ORGANIGRAMMA;
import static it.bologna.ausl.model.entities.rubrica.Contatto.TipoContatto.PERSONA_FISICA;
import static it.bologna.ausl.model.entities.rubrica.Contatto.TipoContatto.PUBBLICA_AMMINISTRAZIONE_ESTERA;
import static it.bologna.ausl.model.entities.rubrica.Contatto.TipoContatto.PUBBLICA_AMMINISTRAZIONE_ITALIANA;
import static it.bologna.ausl.model.entities.rubrica.Contatto.TipoContatto.VARIO;
import it.bologna.ausl.model.entities.rubrica.Email;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.AttoreDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.Registro;
import it.bologna.ausl.model.entities.scripta.RegistroDoc;
import it.bologna.ausl.model.entities.scripta.Related;
import it.bologna.ausl.model.entities.titolario.Titolo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Comparator;
import org.springframework.util.StringUtils;

/**
 *
 * @author boria
 */
public class MetadatiBuilder {

    private static final Logger log = LoggerFactory.getLogger(MetadatiBuilder.class);
    private Map<String, Object> parametriVersamento;
    private Doc doc;
    private List<ArchivioDoc> archiviDocList;
    private AllegatoUnimatica documentoPrincipale;
    private List<AllegatoUnimatica> allegatiSecondariList;
    private Documento documento;
    private Documento.Intestazione intestazione;
    private Documento.Profilo profilo;
    private Documento.Profilo.MetadatiAGID metadatiAGID;
    private Marshaller marshaller;
    private String codificaMarshaller;

    public MetadatiBuilder(Map<String, Object> parametriVersamento, Doc doc, List<ArchivioDoc> archiviDocList, AllegatoUnimatica documentoPrincipale, List<AllegatoUnimatica> allegatiSecondariList) {
        try {
            this.doc = doc;
            this.documentoPrincipale = documentoPrincipale;
            this.allegatiSecondariList = allegatiSecondariList;
            this.parametriVersamento = parametriVersamento;
            this.archiviDocList = archiviDocList;

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

        //**prendo i parametri**
        String tipoDocumento = doc.getTipologia().toString();
        Map<String, Object> mappaParametri = (Map<String, Object>) parametriVersamento.get(tipoDocumento);
        //dati amministrazione
        String denominazioneAmministrazione = (String) parametriVersamento.get("denominazioneAmministrazione");
        String codiceIpa = (String) parametriVersamento.get("codiceIpa");
        String codiceAOO = (String) parametriVersamento.get("codiceAOO");
        String denominazioneAOO = (String) parametriVersamento.get("denominazioneAOO");
        String mailAmministrazione = (String) parametriVersamento.get("mailAmministrazione");

        //**creo l'intestazione del documento principale**
        intestazione.setIdFile(String.valueOf(documentoPrincipale.getIdFile()));
        intestazione.setNomeFile(documentoPrincipale.getNomeFile());
        intestazione.setPrincipale(true);

        //**creo il profilo**
        //creo i metadati agid creando il Documento Amministrativo Informatico e settandone i campi
        DocumentoAmministrativoInformaticoType documentoAmministrativoInformatico = new DocumentoAmministrativoInformaticoType();
        //Nei commenti identifico così la sturttura del xpath delle xml:
        //- è il primo livello, ogni - è un livello inferiore dell'xpath, ad esempio - è il padre, --il figlio, --- il nipote e così via

        //-IdDoc
        IdDocType idDocPrimario = new IdDocType();
        //--Impronta crittografica del documento
        ImprontaCrittograficaDelDocumentoType improntaCrittograficaDelDocumento = new ImprontaCrittograficaDelDocumentoType();
        //---Impronta
        //impronta del documento principale
        improntaCrittograficaDelDocumento.setImpronta(documentoPrincipale.getImpronta().getBytes(StandardCharsets.UTF_8));
        //---Algoritmo
        //algoritmo del documento principale
        improntaCrittograficaDelDocumento.setAlgoritmo((String) parametriVersamento.get("algoritmo"));
        idDocPrimario.setImprontaCrittograficaDelDocumento(improntaCrittograficaDelDocumento);
        //--Idetificativo
        //come identificativo prendo l'id dell'allegato
        idDocPrimario.setIdentificativo(documentoPrincipale.getIdFile().toString());
        //--Segnatura (opzionale)
        //rgpico e documenti gedi non hanno segnatura
        if (doc.getTipologia().equals(Doc.TipologiaDoc.RGPICO) || doc.getTipologia().equals(Doc.TipologiaDoc.DOCUMENT_UTENTE)) {
            idDocPrimario.setSegnatura("Segnatura non presente");
        } else {
            idDocPrimario.setSegnatura("Fare riferimento all'allegato segnatura.xml");
        }
        //setto l'identificativo del documento
        documentoAmministrativoInformatico.setIdDoc(idDocPrimario);

        //-Modalità di formazione
        documentoAmministrativoInformatico.setModalitaDiFormazione(ModalitaDiFormazioneType.CREAZIONE_TRAMITE_UTILIZZO_DI_STRUMENTI_SOFTWARE_CHE_ASSICURINO_LA_PRODUZIONE_DI_DOCUMENTI_NEI_FORMATI_PREVISTI_IN_ALLEGATO_2);

        //-Tipologia documentale
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

        //-Dati di registrazione
        DatiDiRegistrazioneType datiDiRegistrazione = new DatiDiRegistrazioneType();
        //--Tipologia di flusso
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
                log.error("Tipologia di flusso assente o non prevista per il documento con id " + doc.getId());
                throw new VersatorePluginException("Tipologia di flusso assente o non prevista per il documento con id " + doc.getId());
        }
        datiDiRegistrazione.setTipologiaDiFlusso(tipologiaDiFlussoType);
        //--Tipo registro
        TipoRegistroType tipoRegistro = new TipoRegistroType();
        if (doc.getTipologia().equals(Doc.TipologiaDoc.PROTOCOLLO_IN_ENTRATA) || doc.getTipologia().equals(Doc.TipologiaDoc.PROTOCOLLO_IN_USCITA)) {
            //---Protocollo Ordinario/Protocollo Emergenza
            ProtocolloType protocolloType = new ProtocolloType();
            protocolloType.setTipoRegistro("ProtocolloOrdinario\\ProtocolloEmergenza");
            protocolloType.setDataProtocollazioneDocumento(UnimaticaVersatoreUtils.toXMLGregorianDate(registroDocDocumentoPrincipale.getDataRegistrazione()));
            protocolloType.setNumeroProtocolloDocumento(registroDocDocumentoPrincipale.getNumero().toString());
            protocolloType.setCodiceRegistro(registro.getCodice().toString());
            tipoRegistro.setProtocolloOrdinarioProtocolloEmergenza(protocolloType);
        } else {
            //---Repertorio/Registro
            NoProtocolloType noProtocolloType = new NoProtocolloType();
            noProtocolloType.setTipoRegistro("Repertorio\\Registro");
            noProtocolloType.setDataRegistrazioneDocumento(UnimaticaVersatoreUtils.toXMLGregorianDate(registroDocDocumentoPrincipale.getDataRegistrazione()));
            noProtocolloType.setNumeroRegistrazioneDocumento(registroDocDocumentoPrincipale.getNumero().toString());
            noProtocolloType.setCodiceRegistro(registro.getCodice().toString());
            tipoRegistro.setRepertorioRegistro(noProtocolloType);
        }
        datiDiRegistrazione.setTipoRegistro(tipoRegistro);
        //setto i dati di registrazione
        documentoAmministrativoInformatico.setDatiDiRegistrazione(datiDiRegistrazione);

        //-Soggetti
        SoggettiType soggetti = new SoggettiType();
        //--Ruolo
        RuoloType ruoloAmministrazione = new RuoloType();
        //---Amministrazione che effettua la registrazione
        TipoSoggetto1Type tipoAmministrazione = new TipoSoggetto1Type();
        List<String> mailEnteList = new ArrayList<>();
        mailEnteList.add(mailAmministrazione);
        PAIType paiAmministrazione = buildPAI(
            denominazioneAmministrazione,
            codiceIpa,
            denominazioneAOO,
            codiceAOO,
            null,
            null,
            mailEnteList
        );
        tipoAmministrazione.setTipoRuolo("Amministrazione Che Effettua La Registrazione");
        tipoAmministrazione.setPAI(paiAmministrazione);
        ruoloAmministrazione.setAmministrazioneCheEffettuaLaRegistrazione(tipoAmministrazione);
        soggetti.getRuolo().add(ruoloAmministrazione);
        //ottengo la mappa che contiente i dati delle pubbliche amministrazioni italiane presenti tra i contatti dei related, se ce ne sono
        List<Contatto> contattiList = new ArrayList<>();
        for (Related related : doc.getRelated()) {
            if (related.getIdContatto() != null) {
                contattiList.add(related.getIdContatto());
            }
        }
        Map<Integer, Object> relatedsPAIdatasMap = IpaUtils.getIpaMap(
            contattiList,
            (String) parametriVersamento.get("ipaDBURL"),
            (String) parametriVersamento.get("ipaDBuser"),
            (String) parametriVersamento.get("ipaDBpassword")
        );
        //---Mittente/Autore
        //controllo di che tipo di documento si tratta
        switch (doc.getTipologia()) {
            case PROTOCOLLO_IN_ENTRATA:
                //se pe il mittente
                Related mittente = doc.getRelated()
                    .stream()
                    .filter(relatedObj -> relatedObj.getTipo().equals(Related.TipoRelated.MITTENTE))
                    .findFirst()
                    .orElse(null);
                if (mittente == null) {
                    log.error("Il protocollo in entrata non ha MITTENTE");
                    throw new VersatorePluginException("Il protocollo in entrata non ha MITTENTE");
                }
                RuoloType ruoloMittente = new RuoloType();
                TipoSoggetto32Type tipoMittete = new TipoSoggetto32Type();
                tipoMittete.setTipoRuolo("Mittente");
                if (mittente.getIdContatto() != null) {
                    switch (mittente.getIdContatto().getTipo()) {
                        case AZIENDA:
                        case FORNITORE:
                            tipoMittete.setPG(buildPG(mittente.getDescrizione()));
                            break;
                        case PERSONA_FISICA:
                            tipoMittete.setPF(
                                buildPF(mittente.getIdContatto().getNome(),
                                    mittente.getIdContatto().getCognome(),
                                    mittente.getIdContatto().getCodiceFiscale()));
                            break;
                        case PUBBLICA_AMMINISTRAZIONE_ITALIANA:
                            Map<String, String> codiciIPAeDescrizioni = (Map<String, String>) relatedsPAIdatasMap.get(mittente.getIdContatto().getId());
                            List<String> mailsMittentePAIPEList = ((List<Email>) mittente.getIdContatto().getEmailList())
                                .stream()
                                .map(email -> email.getEmail())
                                .collect(Collectors.toList());
                            if (mailsMittentePAIPEList == null || mailsMittentePAIPEList.isEmpty()) {
                                log.error("Non vi sono Indirizzi Digitali di Riferimento per la Pubblica Amministrazione Italiana (il contatto mittente non ha email)");
                                throw new VersatorePluginException("Non vi sono Indirizzi Digitali di Riferimento per la Pubblica Amministrazione Italiana (il contatto mittente non ha email)");
                            }
                            tipoMittete.setPAI(
                                buildPAI(
                                    codiciIPAeDescrizioni.get("des_amm"),
                                    codiciIPAeDescrizioni.get("cod_amm"),
                                    codiciIPAeDescrizioni.get("des_aoo"),
                                    codiciIPAeDescrizioni.get("cod_aoo"),
                                    codiciIPAeDescrizioni.get("des_ou"),
                                    codiciIPAeDescrizioni.get("cod_uni_ou"),
                                    mailsMittentePAIPEList
                                )
                            );
                            break;

                        case ORGANIGRAMMA:
                            if (mittente.getIdContatto().getCategoria() == Contatto.CategoriaContatto.PERSONA) {
                                tipoMittete.setPF(
                                    buildPF(mittente.getIdContatto().getNome(),
                                        mittente.getIdContatto().getCognome(),
                                        mittente.getIdContatto().getCodiceFiscale()));
                            } else if (mittente.getIdContatto().getCategoria() == Contatto.CategoriaContatto.STRUTTURA) {
                                tipoMittete.setPG(buildPG(mittente.getDescrizione()));
                            }
                            break;
                        case PUBBLICA_AMMINISTRAZIONE_ESTERA:
                            List<String> mailsMittentePAIPUList = ((List<Email>) mittente.getIdContatto().getEmailList())
                                .stream()
                                .map(email -> email.getEmail())
                                .collect(Collectors.toList());
                            if (mailsMittentePAIPUList == null || mailsMittentePAIPUList.isEmpty()) {
                                log.error("Non vi sono Indirizzi Digitali di Riferimento per la Pubblica Amministrazione Estera (il contatto mittente non ha email)");
                                throw new VersatorePluginException("Non vi sono Indirizzi Digitali di Riferimento per la Pubblica Amministrazione Estera (il contatto mittente non ha email)");
                            }
                            tipoMittete.setPAE(buildPAE(mittente.getDescrizione(), mailsMittentePAIPUList));
                            break;
                        case VARIO:
                            tipoMittete.setPG(buildPG(mittente.getDescrizione()));
                            break;
                    }
                } else {
                    tipoMittete.setPG(buildPG(mittente.getDescrizione()));
                }
                ruoloMittente.setMittente(tipoMittete);
                soggetti.getRuolo().add(ruoloMittente);
                break;
            case PROTOCOLLO_IN_USCITA:
                //se pu gli autori sono i firmatari
                List<AttoreDoc> firmatariList = doc.getAttoriList()
                    .stream()
                    .filter(attoreObj -> attoreObj.getRuolo().equals(AttoreDoc.RuoloAttoreDoc.FIRMA))
                    .collect(Collectors.toList());
                if (firmatariList == null || firmatariList.isEmpty()) {
                    log.error("Il Protocollo in Uscita non ha autori (firmatari)");
                    throw new VersatorePluginException("Il Protocollo in Uscita non ha autori (firmatari)");
                }
                for (AttoreDoc firmatarioAttore : firmatariList) {
                    RuoloType ruoloAutore = new RuoloType();
                    TipoSoggetto41Type tipoAutore = new TipoSoggetto41Type();
                    tipoAutore.setTipoRuolo("Autore");
                    Persona firmatarioPersona = firmatarioAttore.getIdPersona();
                    tipoAutore.setPF(buildPF(firmatarioPersona.getNome(), firmatarioPersona.getCognome(), firmatarioPersona.getCodiceFiscale()));
                    ruoloAutore.setAutore(tipoAutore);
                    soggetti.getRuolo().add(ruoloAutore);
                }
                break;
            case RGPICO:
                //se rgpico metto l'ente
                RuoloType ruoloAutore = new RuoloType();
                TipoSoggetto41Type tipoAutore = new TipoSoggetto41Type();
                tipoAutore.setTipoRuolo("Autore");
                tipoAutore.setPAI(paiAmministrazione);
                ruoloAutore.setAutore(tipoAutore);
                soggetti.getRuolo().add(ruoloAutore);
                break;
            default:
                log.error("Tipologia di documento non prevista per il versamento a UNIMATICA");
                throw new VersatorePluginException("Tipologia di documento non prevista per il versamento a UNIMATICA");
        }
        //---Destinatari (opzionali)
        List<Related> destinatariList = doc.getRelated()
            .stream()
            .filter(relatedObj -> relatedObj.getTipo().equals(Related.TipoRelated.A) || relatedObj.getTipo().equals(Related.TipoRelated.CC))
            .collect(Collectors.toList());
        for (Related destinatario : destinatariList) {
            RuoloType ruoloDestinatario = new RuoloType();
            TipoSoggetto31Type tipoDestinatario = new TipoSoggetto31Type();
            tipoDestinatario.setTipoRuolo("Destinatario");
            if (destinatario.getIdContatto() != null) {
                switch (destinatario.getIdContatto().getTipo()) {
                    case AZIENDA:
                    case FORNITORE:
                        tipoDestinatario.setPG(buildPG(destinatario.getDescrizione()));
                        break;
                    case PERSONA_FISICA:
                        tipoDestinatario.setPF(
                            buildPF(destinatario.getIdContatto().getNome(),
                                destinatario.getIdContatto().getCognome(),
                                destinatario.getIdContatto().getCodiceFiscale()));
                        break;
                    case PUBBLICA_AMMINISTRAZIONE_ITALIANA:
                        Map<String, String> codiciIPAeDescrizioni = (Map<String, String>) relatedsPAIdatasMap.get(destinatario.getIdContatto().getId());
                        List<String> mailsDestinatarioPAIPEList = ((List<Email>) destinatario.getIdContatto().getEmailList())
                            .stream()
                            .map(email -> email.getEmail())
                            .collect(Collectors.toList());
                        if (mailsDestinatarioPAIPEList == null || mailsDestinatarioPAIPEList.isEmpty()) {
                            log.error("Non vi sono Indirizzi Digitali di Riferimento per la Pubblica Amministrazione Italiana (il contatto destinatario non ha email)");
                            throw new VersatorePluginException("Non vi sono Indirizzi Digitali di Riferimento per la Pubblica Amministrazione Italiana (il contatto destinatario non ha email)");
                        }
                        tipoDestinatario.setPAI(
                            buildPAI(
                                codiciIPAeDescrizioni.get("des_amm"),
                                codiciIPAeDescrizioni.get("cod_amm"),
                                codiciIPAeDescrizioni.get("des_aoo"),
                                codiciIPAeDescrizioni.get("cod_aoo"),
                                codiciIPAeDescrizioni.get("des_ou"),
                                codiciIPAeDescrizioni.get("cod_uni_ou"),
                                mailsDestinatarioPAIPEList
                            )
                        );
                        break;
                    case ORGANIGRAMMA:
                        if (destinatario.getIdContatto().getCategoria() == Contatto.CategoriaContatto.PERSONA) {
                            tipoDestinatario.setPF(
                                buildPF(destinatario.getIdContatto().getNome(),
                                    destinatario.getIdContatto().getCognome(),
                                    destinatario.getIdContatto().getCodiceFiscale()));
                        } else if (destinatario.getIdContatto().getCategoria() == Contatto.CategoriaContatto.STRUTTURA) {
                            tipoDestinatario.setPG(buildPG(destinatario.getDescrizione()));
                        }
                        break;
                    case PUBBLICA_AMMINISTRAZIONE_ESTERA:
                        List<String> mailsDestinatarioList = ((List<Email>) destinatario.getIdContatto().getEmailList())
                            .stream()
                            .map(email -> email.getEmail())
                            .collect(Collectors.toList());
                        if (mailsDestinatarioList == null || mailsDestinatarioList.isEmpty()) {
                            log.error("Non vi sono Indirizzi Digitali di Riferimento per la Pubblica Amministrazione Estera (il contatto destinatario non ha email)");
                            throw new VersatorePluginException("Non vi sono Indirizzi Digitali di Riferimento per la Pubblica Amministrazione Estera (il contatto destinatario non ha email)");
                        }
                        tipoDestinatario.setPAE(buildPAE(destinatario.getDescrizione(), mailsDestinatarioList));
                        break;
                    case VARIO:
                        tipoDestinatario.setPG(buildPG(destinatario.getDescrizione()));
                        break;
                }
            } else {
                tipoDestinatario.setPG(buildPG(destinatario.getDescrizione()));
            }
            ruoloDestinatario.setDestinatario(tipoDestinatario);
            soggetti.getRuolo().add(ruoloDestinatario);
        }
        //TODO responsabile gestione documentale opz
        //TODO responsabile servizio protocollo opz
        //--- Produttore (opzionale)
        RuoloType ruoloProduttore = new RuoloType();
        TipoSoggetto5Type tipoProduttore = new TipoSoggetto5Type();
        tipoProduttore.setTipoRuolo("Produttore");
        SWType sWType = new SWType();
        sWType.setDenominazioneSistema((String) parametriVersamento.get("descrizioneSoftware"));
        tipoProduttore.setSW(sWType);
        ruoloProduttore.setProduttore(tipoProduttore);
        soggetti.getRuolo().add(ruoloProduttore);
        //setto i soggetti
        documentoAmministrativoInformatico.setSoggetti(soggetti);

        //-Chiave descrittiva
        ChiaveDescrittivaType chiaveDescrittiva = new ChiaveDescrittivaType();
        //--Oggetto
        chiaveDescrittiva.setOggetto(doc.getOggetto());
        documentoAmministrativoInformatico.setChiaveDescrittiva(chiaveDescrittiva);

        //-Allegati
        if (allegatiSecondariList.size() > 0) {
            AllegatiType allegati = new AllegatiType();
            //--Numero allegati
            allegati.setNumeroAllegati(allegatiSecondariList.size());
            for (AllegatoUnimatica allegatoUnimatica : allegatiSecondariList) {
                IndiceAllegatiType indiceAllegati = new IndiceAllegatiType();
                //---IdDoc
                IdDocType idDocAllegato = new IdDocType();
                ImprontaCrittograficaDelDocumentoType improntaCrittograficaDelDocumentoAllegato = new ImprontaCrittograficaDelDocumentoType();
                improntaCrittograficaDelDocumentoAllegato.setImpronta(allegatoUnimatica.getImpronta().getBytes(StandardCharsets.UTF_8));
                improntaCrittograficaDelDocumentoAllegato.setAlgoritmo((String) parametriVersamento.get("algoritmo"));
                idDocAllegato.setImprontaCrittograficaDelDocumento(improntaCrittograficaDelDocumentoAllegato);
                idDocAllegato.setIdentificativo(allegatoUnimatica.getIdFile().toString());
                indiceAllegati.setIdDoc(idDocAllegato);
                //---Descrizione
                indiceAllegati.setDescrizione(allegatoUnimatica.getNomeFile());
                allegati.getIndiceAllegati().add(indiceAllegati);
            }
            documentoAmministrativoInformatico.setAllegati(allegati);
        }

        //-Riservato
        documentoAmministrativoInformatico.setRiservato(doc.getVisibilita().equals(Doc.VisibilitaDoc.RISERVATO));

        //-Identificativo del formato
        IdentificativoDelFormatoType identificativoDelFormato = new IdentificativoDelFormatoType();
        //--Formato
        identificativoDelFormato.setFormato(documentoPrincipale.getFormato());
        //--Prodotto software
        ProdottoSoftwareType prodottoSoftware = new ProdottoSoftwareType();
        //---Nome prodotto
        prodottoSoftware.setNomeProdotto((String) parametriVersamento.get("descrizioneSoftware"));
        //---Produttore
        prodottoSoftware.setProduttore((String) parametriVersamento.get("produttore"));
        identificativoDelFormato.setProdottoSoftware(prodottoSoftware);
        documentoAmministrativoInformatico.setIdentificativoDelFormato(identificativoDelFormato);

        //-Verifica
        VerificaType verifica = new VerificaType();
        //--Firmato digitalmente
        if (doc.getTipologia().equals(Doc.TipologiaDoc.PROTOCOLLO_IN_ENTRATA)) {
            verifica.setFirmatoDigitalmente(documentoPrincipale.getFirmato());
        } else {
            verifica.setFirmatoDigitalmente((boolean) mappaParametri.get("firmatoDigitalmente"));
        }
        //--Sigillato elettornicamente
        verifica.setSigillatoElettronicamente((boolean) mappaParametri.get("sigillatoElettronicamente"));
        //--Marcatura temporale
        verifica.setMarcaturaTemporale((boolean) mappaParametri.get("marcaturaTemporale"));
        documentoAmministrativoInformatico.setVerifica(verifica);

        //-Agg
        //fascicoli
        AggType agg = new AggType();
        for (ArchivioDoc archivioDoc : archiviDocList) {
            //--Tipo agg (1-n)
            IdAggType idAgg = new IdAggType();
            //---Tipo aggregazione
            idAgg.setTipoAggregazione(TipoAggregazioneType.FASCICOLO);
            //---Id aggregazione
            idAgg.setIdAggregazione(buildIdFascicolo(archivioDoc.getIdArchivio()));
            agg.getTipoAgg().add(idAgg);
        }
        documentoAmministrativoInformatico.setAgg(agg);
        documentoAmministrativoInformatico.setIdIdentificativoDocumentoPrimario(idDocPrimario);

        //-Nome del documento
        documentoAmministrativoInformatico.setNomeDelDocumento(documentoPrincipale.getNomeFile());

        //-Tempo di conservazione*
        Integer tempoDiConservazione = 9999;
        Archivio archivioPrincipale = new Archivio();
        //trovo l'archivio principale (il primo in cui il documento è fascicolato, quello che detta la classificazione)
        if (!doc.getTipologia().equals(Doc.TipologiaDoc.RGPICO)) {
            archivioPrincipale = archiviDocList
                .stream()
                .filter(archivioDocObj -> !archivioDocObj.getIdArchivio().getSpeciale())
                .min(Comparator.comparing(archivioDocObj -> archivioDocObj.getDataArchiviazione()))
                .map(archivioDocObj -> archivioDocObj.getIdArchivio())
                .orElse(null);
            if (archivioPrincipale == null) {
                log.error("Fascicolo principale (il primo in cui il documento è fascicolato, quello che detta la classificazione) non individuabile");
                throw new VersatorePluginException("Fascicolo principale (il primo in cui il documento è fascicolato, quello che detta la classificazione) non individuabile");
            }
            //imposto gli anni di conservazione, mentre in caso di RGPICO metto sempre illimitata la conservazione
            if (archivioPrincipale.getAnniTenuta() != null && archivioPrincipale.getAnniTenuta() != 999) {
                tempoDiConservazione = archivioPrincipale.getAnniTenuta();
            }
        } else {
            archivioPrincipale = archiviDocList.get(0).getIdArchivio();
            if (archivioPrincipale == null) {
                log.error("Fascicolo principale (quello speciale per gli RGPICO) non individuabile");
                throw new VersatorePluginException("Fascicolo principale (quello speciale per gli RGPICO) non individuabile");
            }
        }
        documentoAmministrativoInformatico.setTempoDiConservazione(tempoDiConservazione);

        //-Classificazione
        ClassificazioneType classificazione = new ClassificazioneType();
        //--Indice di classificazione
        //prendo il titolo, se l'archivio principale e sottofascicolo o inserto e non è specificato il titolo prendo quello dell'archivio radice
        Titolo titolo = new Titolo();
        if (archivioPrincipale.getIdTitolo() != null) {
            titolo = archivioPrincipale.getIdTitolo();
        } else if (archivioPrincipale.getIdArchivioPadre() != null) {
            titolo = archivioPrincipale.getIdArchivioRadice().getIdTitolo();
        } else {
            log.error("Non esiste titolo associato al fascicolo principale");
            throw new VersatorePluginException("Non esiste titolo associato al fascicolo principale");
        }
        if (titolo == null) {
            log.error("Non esiste titolo associato al fascicolo principale");
            throw new VersatorePluginException("Non esiste titolo associato al fascicolo principale");
        }
        classificazione.setIndiceDiClassificazione(titolo.getClassificazione());
        //--Descrizione
        classificazione.setDescrizione(titolo.getNome());
        //TODO piano di classificazione opz
        documentoAmministrativoInformatico.setClassificazione(classificazione);

        //TODO note
        //set dei metadati AGID
        metadatiAGID.setDocumentoAmministrativoInformatico(documentoAmministrativoInformatico);
    }

    /**
     * Metodo che formatta il singolo idFascicolo
     * @param archivio
     * @return
     */
    public static String buildIdFascicolo(Archivio archivio) {
        return (archivio.getNumerazioneGerarchica() + " [id_" + archivio.getId() + "]");
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

            // Trova il tag <DocumentoAmministrativoInformatico>...</DocumentoAmministrativoInformatico>
            int start = out.indexOf("<DocumentoAmministrativoInformatico>");
            int end = out.indexOf("</DocumentoAmministrativoInformatico>") + "</DocumentoAmministrativoInformatico>".length();

            if (start >= 0 && end > start) {
                String daiXml = out.substring(start, end);

                // Aggiungi dichiarazione XML e CDATA
                String daiXmlWithCData = "<![CDATA[\n<?xml version=\"1.0\" encoding=\"" + codificaMarshaller + "\"?>\n"
                    + daiXml + "\n]]>";

                // Sostituisci nell'XML originale
                out = out.substring(0, start) + daiXmlWithCData + out.substring(end);
            }

        } catch (UnsupportedEncodingException e) {
            return "Document Error unable to serialize with coding " + codificaMarshaller + ": " + e;
        }

        return out;
    }

    public PFType buildPF(String nome, String cognome, String codiceFiscale) {
        PFType pFType = new PFType();
        pFType.setNome(nome);
        pFType.setCognome(cognome);
        if (StringUtils.hasText(codiceFiscale)) {
            pFType.setCodiceFiscale(codiceFiscale);
        }
        return pFType;
    }

    public PGType buildPG(String denominazioneOrganizzazione) {
        PGType pGType = new PGType();
        pGType.setDenominazioneOrganizzazione(denominazioneOrganizzazione);
        return pGType;
    }

    public PAIType buildPAI(String denominazioneAmministrazione, String codiceIpaAmm, String denominazioneAOO, String codiceIpaAOO, String denominazioneUOR, String codiceIpaUOR, List<String> indirizziDigitaliDiriferimentoList) throws VersatorePluginException {
        PAIType pAIType = new PAIType();
        if (StringUtils.hasText(codiceIpaAmm)) {
            CodiceIPAType codiceIPATypeAmm = new CodiceIPAType();
            if (StringUtils.hasText(denominazioneAmministrazione)) {
                codiceIPATypeAmm.setDenominazione(denominazioneAmministrazione);
            }
            codiceIPATypeAmm.setCodiceIPA(codiceIpaAmm);
            pAIType.setIPAAmm(codiceIPATypeAmm);
        } else {
            log.error("Codice Amministrazione non indicato");
            throw new VersatorePluginException("Codice Amministrazione non indicato");
        }
        if (StringUtils.hasText(codiceIpaAOO)) {
            CodiceIPAType codiceIPATypeAOO = new CodiceIPAType();
            if (StringUtils.hasText(denominazioneAOO)) {
                codiceIPATypeAOO.setDenominazione(denominazioneAOO);
            }
            codiceIPATypeAOO.setCodiceIPA(codiceIpaAOO);
            pAIType.setIPAAOO(codiceIPATypeAOO);
        }
        if (StringUtils.hasText(codiceIpaUOR)) {
            CodiceIPAType codiceIPATypeUOR = new CodiceIPAType();
            if (StringUtils.hasText(denominazioneUOR)) {
                codiceIPATypeUOR.setDenominazione(denominazioneUOR);
            }
            codiceIPATypeUOR.setCodiceIPA(codiceIpaUOR);
            pAIType.setIPAUOR(codiceIPATypeUOR);
        }
        if (indirizziDigitaliDiriferimentoList
            != null) {
            for (String mail : indirizziDigitaliDiriferimentoList) {
                if (pAIType.getIndirizziDigitaliDiRiferimento() != null) {
                    pAIType.getIndirizziDigitaliDiRiferimento().add(mail);
                }
            }
        }
        return pAIType;
    }

    public PAEType buildPAE(String descrizioneAmminiatrazione, List<String> indirizziDigitaliDiriferimentoList) {
        PAEType pAEType = new PAEType();
        pAEType.setDenominazioneAmministrazione(descrizioneAmminiatrazione);
        if (indirizziDigitaliDiriferimentoList != null) {
            for (String mail : indirizziDigitaliDiriferimentoList) {
                if (pAEType.getIndirizziDigitaliDiRiferimento() != null) {
                    pAEType.getIndirizziDigitaliDiRiferimento().add(mail);
                }
            }
        }
        return pAEType;
    }
}
