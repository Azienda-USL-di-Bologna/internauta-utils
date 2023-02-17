package it.bologna.ausl.internauta.utils.versatore.plugins.infocert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sun.xml.ws.fault.ServerSOAPFaultException;
import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.enums.InfocertAttributesEnum;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.mimetypeutilities.Detector;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Pec;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import static it.bologna.ausl.model.entities.rubrica.Contatto.TipoContatto.AZIENDA;
import static it.bologna.ausl.model.entities.rubrica.Contatto.TipoContatto.FORNITORE;
import static it.bologna.ausl.model.entities.rubrica.Contatto.TipoContatto.ORGANIGRAMMA;
import static it.bologna.ausl.model.entities.rubrica.Contatto.TipoContatto.PERSONA_FISICA;
import static it.bologna.ausl.model.entities.rubrica.Contatto.TipoContatto.PUBBLICA_AMMINISTRAZIONE_ESTERA;
import static it.bologna.ausl.model.entities.rubrica.Contatto.TipoContatto.PUBBLICA_AMMINISTRAZIONE_ITALIANA;
import static it.bologna.ausl.model.entities.rubrica.Contatto.TipoContatto.VARIO;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.AttoreDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import static it.bologna.ausl.model.entities.scripta.DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA;
import static it.bologna.ausl.model.entities.scripta.DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA;
import it.bologna.ausl.model.entities.scripta.QAttoreDoc;
import it.bologna.ausl.model.entities.scripta.Titolo;
import it.bologna.ausl.model.entities.versatore.QVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.model.entities.versatore.VersamentoAllegato;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import it.bologna.ausl.utils.versatore.infocert.wsclient.DocumentAttribute;
import it.bologna.ausl.utils.versatore.infocert.wsclient.DocumentStatus;
import static it.bologna.ausl.utils.versatore.infocert.wsclient.DocumentStatusCode.SENDING_FAILED;
import static it.bologna.ausl.utils.versatore.infocert.wsclient.DocumentStatusCode.SUCCESS;
import static it.bologna.ausl.utils.versatore.infocert.wsclient.DocumentStatusCode.VERIFICATION_FAILED;
import it.bologna.ausl.utils.versatore.infocert.wsclient.GenericDocument;
import it.bologna.ausl.utils.versatore.infocert.wsclient.GenericDocumentService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.ws.BindingProvider;
import org.apache.tika.mime.MimeTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@Component
public class InfocertVersatoreService extends VersatoreDocs {
    
    private enum AzioneVersamento {
        VERSA, CONTROLLO
    };
    private static final Logger log = LoggerFactory.getLogger(InfocertVersatoreService.class);
    private static final String INFOCERT_VERSATORE_SERVICE = "InfocertVersatoreService";
    private static final String ERROR_PARSING_JSON = "Errore nel parsing dei metadati";

    private String infocertVersatoreServiceEndPointUri;
 
    @Override
    public void init(VersatoreConfiguration versatoreConfiguration) {
        super.init(versatoreConfiguration);
        Map<String, Object> versatoreConfigurationMap = this.versatoreConfiguration.getParams();
        Map<String, Object> infocertServiceConfiguration = (Map<String, Object>) versatoreConfigurationMap.get(INFOCERT_VERSATORE_SERVICE);
        infocertVersatoreServiceEndPointUri = infocertServiceConfiguration.get("InfocertVersatoreServiceEndPointUri").toString();
        log.info("URI: {}", infocertVersatoreServiceEndPointUri);
    }

    /**
     * Metodo astratto che implementa il versamento Infocert.
     * Costruisce l'oggetto contentente i metadati del doc che verranno inviati ad Infocert.
     * @param versamentoDocInformation L'oggetto con l'idDoc e l'idArchivio da versare.
     * @return Lo stesso oggetto VersamentoInformation con i risultati dei versamenti.
     * @throws VersatoreProcessingException Eventuali eccezioni.
     */
    @Override
    public VersamentoDocInformation versaImpl(VersamentoDocInformation versamentoDocInformation) throws VersatoreProcessingException {
              
        Integer idDoc = versamentoDocInformation.getIdDoc();
        Doc doc = entityManager.find(Doc.class, idDoc);
        try {
            GenericDocument infocertService = initInfocertService();
            log.info("Processing doc: {}", idDoc.toString());
            // Se è il primo versamento del Doc chiamiamo direttamente il metodo versaDoc
            // altrimenti bisogna recuperare tutti i versamenti allegati e controllare per ognuno lo stato del versamento
            // per capire se è un'operazione di controllo oppure ritenta
            if (versamentoDocInformation.isPrimoVersamento() == Boolean.TRUE) {
                versamentoDocInformation = versaDoc(doc, versamentoDocInformation, infocertService);
            } else {
                List<DocumentAttribute> docAttributes = new ArrayList<>();
                List<VersamentoAllegatoInformation> versamentiAllegatiInfo = new ArrayList<>();
                Versamento versamentoDoc = entityManager.find(Versamento.class, versamentoDocInformation.getIdVersamentoPrecedente());
                List<VersamentoAllegato> versamentiAllegati = versamentoDoc.getVersamentoAllegatoList();
                versamentoDocInformation.setDataVersamento(ZonedDateTime.now());
                String metadatiVersati = versamentoDoc.getMetadatiVersati();
                for (VersamentoAllegato versamentoAllegato : versamentiAllegati) {
                    switch (getAzioneVersamento(versamentoAllegato)) {
                        case VERSA:
                            if (docAttributes.isEmpty()) {
                                docAttributes = buildMetadatiDoc(doc, versamentoDocInformation);
                                metadatiVersati = objectMapper.writeValueAsString(docAttributes);
                            }
                            Allegato allegato = versamentoAllegato.getIdAllegato();
                            VersamentoAllegatoInformation allegatoInfo = 
                                    versaAllegato(allegato, versamentoAllegato.getDettaglioAllegato(), docAttributes, versamentiAllegati.size(), infocertService);
                            versamentiAllegatiInfo.add(allegatoInfo);
                            break;
                        case CONTROLLO:
                            VersamentoAllegatoInformation allegatoInformation = controllaStatoVersamento(versamentoAllegato, infocertService);
                            versamentiAllegatiInfo.add(allegatoInformation);
                        break;
                    }  
                }
                versamentoDocInformation.setMetadatiVersati(metadatiVersati);
                versamentoDocInformation.setVersamentiAllegatiInformations(versamentiAllegatiInfo);
            }
            boolean errorOccured = versamentoDocInformation.getVersamentiAllegatiInformations().stream().filter(all -> 
                    Arrays.asList(Versamento.StatoVersamento.ERRORE, Versamento.StatoVersamento.ERRORE_RITENTABILE)
                        .contains(all.getStatoVersamento())).findFirst().isPresent();
            if (errorOccured) {
                log.error("Versamenti di allegati in errore");
                versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
            } else {
                versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.IN_CARICO);
            }
        } catch (MalformedURLException | JsonProcessingException ex) {
            log.error("Errore URL", ex);
            versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
            versamentoDocInformation.setDescrizioneErrore(ex.getMessage());
        }
        log.info("processing doc: {} done.", idDoc.toString());         
        return versamentoDocInformation;
    }
    
    /**
     * Metodo che riceve il Doc e costruisce l'oggetto con i metadati da versare.
     * @param doc Il doc da versare.
     * @param versamentoDocInformation L'oggetto con i parametri.
     * @return I parametri e i risultati dei versamenti.
     * @throws VersatoreProcessingException Eventuali errori.
     */
    private VersamentoDocInformation versaDoc(
            Doc doc, 
            VersamentoDocInformation versamentoDocInformation,
            GenericDocument infocertService) throws VersatoreProcessingException {
        String docId = doc.getId().toString();
          
        // Build metadati generici dell'unità documentaria
        List<DocumentAttribute> docAttributes;             
        // Elaborazione degli allegati
        try {
            versamentoDocInformation.setDataVersamento(ZonedDateTime.now());
           
            docAttributes = buildMetadatiDoc(doc, versamentoDocInformation);
            versamentoDocInformation.setMetadatiVersati(objectMapper.writeValueAsString(docAttributes));
            
            List<Allegato> allegati = doc.getAllegati();
            List<VersamentoAllegatoInformation> versamentiAllegatiInfo = new ArrayList();
            List<Pair<Allegato, Allegato.DettagliAllegato.TipoDettaglioAllegato>> pairsAllegati = new ArrayList<>();
            
            for (Allegato allegato: allegati) {
                for (Allegato.DettagliAllegato.TipoDettaglioAllegato tipoDettaglioAllegato : Allegato.DettagliAllegato.TipoDettaglioAllegato.values()) {
                    Allegato.DettaglioAllegato dettaglioAllegato = allegato.getDettagli().getByKey(tipoDettaglioAllegato);
                    // dettaglioAllegato è null quando per il tipoDettaglio (eg. convertito, etc.) non esiste un allegato
                    if (dettaglioAllegato != null)
                        pairsAllegati.add(Pair.of(allegato, tipoDettaglioAllegato));
                }
            }
            for (Pair<Allegato, Allegato.DettagliAllegato.TipoDettaglioAllegato> pair : pairsAllegati) {
                VersamentoAllegatoInformation allegatoInformation = 
                              versaAllegato(pair.getFirst(), pair.getSecond(), docAttributes, pairsAllegati.size(), infocertService);
                if (allegatoInformation != null) {
                    versamentiAllegatiInfo.add(allegatoInformation);
                }
            }
            
            versamentoDocInformation.setVersamentiAllegatiInformations(versamentiAllegatiInfo);
        } catch (JsonProcessingException ex) {
            log.error(ERROR_PARSING_JSON, ex);
            versamentoDocInformation.setDescrizioneErrore(ex.getMessage());
        } catch (Throwable ex) {
            log.error("Errore generico", ex);
            versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
            versamentoDocInformation.setDescrizioneErrore(ex.getMessage());
        }
        log.info("Versamento idDoc: {} concluso.", docId);
        return versamentoDocInformation;
    }
    
    /**
     * Metodo che controlla lo status del versamento di un allegato effettuando una chiamata http
     * al servizio Infocert.
     * @param versamentoAllegato Il versamento allegato da controllare.
     * @param infocertService Il webservice Infocert.
     * @return L'oggetto VersamentoAllegatoInformation con i riferimenti dell'allegato, il repporto e lo stato.
     */
    public VersamentoAllegatoInformation controllaStatoVersamento(
            VersamentoAllegato versamentoAllegato,
            GenericDocument infocertService) {
        DocumentStatus status;
        VersamentoAllegatoInformation allegatoInformation = new VersamentoAllegatoInformation();
        try {
            RapportoVersamentoInfocert rapporto = objectMapper.readValue(versamentoAllegato.getRapporto(), RapportoVersamentoInfocert.class);
            log.info("call getDocumentStatus allegato: {} with hash: {}", 
                    versamentoAllegato.getIdAllegato().getId().toString(), rapporto.getHash());
            status = infocertService.getDocumentStatus(rapporto.getHash());
            switch (status.getDocumentStatusCode()) {
                case SUCCESS:
                    allegatoInformation.setStatoVersamento(Versamento.StatoVersamento.VERSATO);
                    break;
                case SENDING_FAILED:
                case VERIFICATION_FAILED:
                    allegatoInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
                    break;
                default:
                    allegatoInformation.setStatoVersamento(Versamento.StatoVersamento.IN_CARICO);
                    break;
            }   
            rapporto.setDocumentStatus(status);
            if (status.getDocumentError() != null) {
                allegatoInformation.setCodiceErrore(status.getDocumentError().getErrorCode());
                allegatoInformation.setDescrizioneErrore(status.getDocumentError().getErrorDescription());
                allegatoInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
            }
            allegatoInformation.setIdAllegato(versamentoAllegato.getIdAllegato().getId());
            allegatoInformation.setDataVersamento(ZonedDateTime.now());
            allegatoInformation.setTipoDettaglioAllegato(versamentoAllegato.getDettaglioAllegato());
            allegatoInformation.setMetadatiVersati(versamentoAllegato.getMetadatiVersati());
            allegatoInformation.setRapporto(objectMapper.writeValueAsString(rapporto)); 
        } catch (JsonProcessingException ex) {
            log.error(ERROR_PARSING_JSON, ex);
        }
        return allegatoInformation;
    }   

    public InfocertVersatoreService addNewAttribute(List<DocumentAttribute> attributes, final InfocertAttributesEnum name, final String value) {
        return addNewAttribute(attributes, name, null, value);
    }

    /**
     * Aggiunge un nuovo attributo alla lista di attributi passata in ingresso.
     * @param attributes La lista di attributi a cui aggiungere il nuovo attributo.
     * @param name Il nome dell'attributo (metadato).
     * @param index Indice (Opzionale) utilizzato per gli i metadati ricorsivi.
     * @param value Il valore dell'attributo.
     * @return Restituisce l'oggetto corrente.
     */
    public InfocertVersatoreService addNewAttribute(List<DocumentAttribute> attributes,
            final InfocertAttributesEnum name, final Integer index, final String value) {
        DocumentAttribute attr = new DocumentAttribute();
        attr.setName(index != null ? name.toString().replace("x", index.toString()) : name.toString());
        attr.setValue(value);
        attributes.add(attr);
        return this;
    }
    
    /**
     * Inizializza la lista dei metadati con i dati generici del documento.
     * @param doc Il documento da versare.
     * @param versamentoDocInformation L'oggetto di utilità per il passaggio delle informazioni con il servizio.
     * @return La lista dei metadati generici del doc.
     */
    private List<DocumentAttribute> buildMetadatiDoc(Doc doc, VersamentoDocInformation versamentoDocInformation) throws VersatoreProcessingException {
        DocDetail docDetail = entityManager.find(DocDetail.class, doc.getId());
        Archivio archivio = entityManager.find(Archivio.class, versamentoDocInformation.getIdArchivio());
        List<DocumentAttribute> docAttributes = new ArrayList<>();
        
        // Dati generici del documento
        addNewAttribute(docAttributes, InfocertAttributesEnum.IDENTIFICATIVO_DOCUMENTO, docDetail.getId().toString())
                .addNewAttribute(docAttributes, InfocertAttributesEnum.DATA_DOCUMENTO, ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yy")))
                .addNewAttribute(docAttributes, InfocertAttributesEnum.MODALITA_DI_FORMAZIONE, "b")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.CODICE_REGISTRO, getCodiceRegistro(docDetail))
                .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_REGISTRO, getTipoRegistro(docDetail))
                .addNewAttribute(docAttributes, InfocertAttributesEnum.DATA_REGISTRAZIONE, docDetail.getDataRegistrazione().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .addNewAttribute(docAttributes, InfocertAttributesEnum.NUMERO_DOCUMENTO,
                        String.join("/", docDetail.getNumeroRegistrazione().toString(), docDetail.getAnnoRegistrazione().toString()))
                .addNewAttribute(docAttributes, InfocertAttributesEnum.OGGETTO, docDetail.getOggetto());
       
        int index = 2;  // Indice per i metadati ricorsivi del ruolo, il numero 1 è default ed è il produttore
        // Metadati degli Agenti (Soggetti)
        addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO, "produttore")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO, "SW")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.DENOMINAZIONE, docDetail.getIdAzienda().getDescrizione())
                .addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO_N, index, "redattore")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PF")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.COGNOME_N, index, docDetail.getIdPersonaRedattrice().getCognome())
                .addNewAttribute(docAttributes, InfocertAttributesEnum.NOME_N, index, docDetail.getIdPersonaRedattrice().getNome()); 
        index++;    // Index 3
        
        // Tipologia e Mittente    
        switch (docDetail.getTipologia()) {
            case PROTOCOLLO_IN_USCITA:
                addNewAttribute(docAttributes, InfocertAttributesEnum.TIPOLOGIA_DOCUMENTALE, "Protocolli")
                        .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPOLOGIA_DI_FLUSSO, "U"); 
                if (docDetail.getStato() == DocDetailInterface.StatoDoc.SMISTAMENTO) {
                    addMittenteProtEntrataPuSmistamento(docAttributes, docDetail, index);
                } else {
                    addMittenteAzienda(docAttributes, docDetail, index);
                }
                break;
            case PROTOCOLLO_IN_ENTRATA:
                addNewAttribute(docAttributes, InfocertAttributesEnum.TIPOLOGIA_DOCUMENTALE, "Protocolli")
                    .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPOLOGIA_DI_FLUSSO, "E");
                addMittenteProtEntrataPuSmistamento(docAttributes, docDetail, index);
                break;
            case DETERMINA:
                addNewAttribute(docAttributes, InfocertAttributesEnum.TIPOLOGIA_DOCUMENTALE, "Determine");
                addMittenteAzienda(docAttributes, docDetail, index);
                break;
            case DELIBERA:
                addNewAttribute(docAttributes, InfocertAttributesEnum.TIPOLOGIA_DOCUMENTALE, "Delibere");
                addMittenteAzienda(docAttributes, docDetail, index);
                break;
            case RGPICO:
                addNewAttribute(docAttributes, InfocertAttributesEnum.TIPOLOGIA_DOCUMENTALE, "Registro giornaliero protocollo");
                break;
            case DOCUMENT:
            case DOCUMENT_UTENTE:
                addNewAttribute(docAttributes, InfocertAttributesEnum.TIPOLOGIA_DOCUMENTALE, "Registro documentale");
                break;
        }
        index++;
        
        // Soggetti che registrano, FIRMATARI
        List<AttoreDoc> attori = new ArrayList<>();
        attori.addAll(getAttoriDoc(doc, "FIRMA"));
        attori.addAll(getAttoriDoc(doc, "RICEZIONE"));
        for (AttoreDoc attoreDoc : attori) {
            Persona attore = attoreDoc.getIdPersona();
            addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO_N, index, "Soggetto che effettua la registrazione")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PF")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.COGNOME_N, index, attore.getCognome())
                .addNewAttribute(docAttributes, InfocertAttributesEnum.NOME_N, index, attore.getNome());
            index++;
        }
        
        // Metadati di archiviazione
        Titolo titolo = archivio.getIdTitolo();
        if (titolo != null) {
            addNewAttribute(docAttributes, InfocertAttributesEnum.INDICE_DI_CLASSIFICAZIONE, titolo.getClassificazione())
                   .addNewAttribute(docAttributes, InfocertAttributesEnum.DESCRIZIONE_CLASSIFICAZIONE, titolo.getNome());
        } else {
            log.warn("Titolo non trovato.");
        }
        
        addNewAttribute(docAttributes, InfocertAttributesEnum.RISERVATO, docDetail.getRiservato().toString())
                .addNewAttribute(docAttributes, InfocertAttributesEnum.VERSIONE_DEL_DOCUMENTO, getVersioneDocumento(versamentoDocInformation))
                .addNewAttribute(docAttributes, InfocertAttributesEnum.ID_AGGREGAZIONE, archivio.getNumerazioneGerarchica())
                .addNewAttribute(docAttributes, 
                        InfocertAttributesEnum.TEMPO_DI_CONSERVAZIONE,
                        archivio.getAnniTenuta() != null ? archivio.getAnniTenuta().toString() : "999");
        
        return docAttributes;
    }
    
    /**
     * Effettua il versamento ad Infocert di una specifica tipologia dell'allegato.
     *
     * @param allegato L'allegato da versare.
     * @param tipoDettaglioAllegato La tipologia del file (eg. originale, convertito, convertito firmato, ecc).
     * @param docAttributes La lista degli attributi del doc.
     * @param numeroAllegati Il numero totale degli allegati.
     * @param infocertService Il web service di infocert.
     * @return L'
     */
    private VersamentoAllegatoInformation versaAllegato(
            Allegato allegato,
            Allegato.DettagliAllegato.TipoDettaglioAllegato tipoDettaglioAllegato,
            List<DocumentAttribute> docAttributes,
            Integer numeroAllegati,
            GenericDocument infocertService) {
        
        List<DocumentAttribute> fileAttributes;
        List<DocumentAttribute> docFileAttributes;
        Allegato.DettaglioAllegato dettaglioAllegato = allegato.getDettagli().getByKey(tipoDettaglioAllegato);
        // dettaglioAllegato è null quando per il tipoDettaglio (eg. convertito, etc.) non esiste un allegato
        if (dettaglioAllegato == null) return null;

        fileAttributes = new ArrayList<>();
        String mimeType = "application/octet-stream";
        VersamentoAllegatoInformation allegatoInformation = new VersamentoAllegatoInformation();
        try ( InputStream fileStream = minIOWrapper.getByFileId(dettaglioAllegato.getIdRepository())) {
            if (fileStream != null) {
                if (dettaglioAllegato.getMimeType() != null) {
                    mimeType = dettaglioAllegato.getMimeType();
                } else {
                    try {
                        Detector detector = new Detector();
                        mimeType = detector.getMimeType(fileStream);
                    } catch (UnsupportedEncodingException | MimeTypeException ex) {
                        log.warn("errore nel calcolo del mimetype, lasciamo il default octet-stream", ex);
                    } 
                }
                // Metadati degli allegati
                addNewAttribute(fileAttributes, InfocertAttributesEnum.IDENTIFICATIVO_DEL_FORMATO, mimeType)
                        .addNewAttribute(fileAttributes, InfocertAttributesEnum.NOME_FILE, dettaglioAllegato.getNome())
                        .addNewAttribute(fileAttributes, InfocertAttributesEnum.IMPRONTA, dettaglioAllegato.getHashMd5())
                        .addNewAttribute(fileAttributes, InfocertAttributesEnum.ALGORITMO, "md5")
                        .addNewAttribute(fileAttributes, InfocertAttributesEnum.ALLEGATI_NUMERO, String.valueOf(numeroAllegati))
                        .addNewAttribute(fileAttributes, InfocertAttributesEnum.ID_DOC_INDICE_ALLEGATI, allegato.getId().toString())
                        .addNewAttribute(fileAttributes, InfocertAttributesEnum.DESCRIZIONE_ALLEGATI, allegato.getTipo().toString());
                
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SEGNATURA) {
                    String segnatura = convertInputStreamToString(fileStream);
                    addNewAttribute(fileAttributes, InfocertAttributesEnum.SEGNATURA, segnatura);
                }
                
                if (tipoDettaglioAllegato.toString().contains("FIRMATO")) {
                    addNewAttribute(fileAttributes, InfocertAttributesEnum.VERIFICA_FIRMA_DIGITALE, "true");
                }

                ByteArrayDataSource byteArrayDataSource = new ByteArrayDataSource(fileStream, mimeType);
                DataHandler data = new DataHandler(byteArrayDataSource);
                docFileAttributes = new ArrayList<>();
                docFileAttributes.addAll(docAttributes);
                docFileAttributes.addAll(fileAttributes);
                
                allegatoInformation.setIdAllegato(allegato.getId());
                allegatoInformation.setDataVersamento(ZonedDateTime.now());
                allegatoInformation.setTipoDettaglioAllegato(tipoDettaglioAllegato);
                allegatoInformation.setMetadatiVersati(objectMapper.writeValueAsString(docFileAttributes));
                log.info("chiamata della submitDocument per l'allegato: {}", allegato.getId().toString());
                String hash = infocertService.submitDocument(allegato.getIdDoc().getId().toString(), docFileAttributes, data);
                allegatoInformation.setRapporto(objectMapper.writeValueAsString(new RapportoVersamentoInfocert(hash)));
                allegatoInformation.setStatoVersamento(Versamento.StatoVersamento.IN_CARICO);     
            } else {
                log.error("file stream is null");
            }
        } catch (ConnectException ex) {
            log.error(ex.getMessage());
            allegatoInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE_RITENTABILE);
            allegatoInformation.setDescrizioneErrore(ex.getMessage());
        } catch (ServerSOAPFaultException | MinIOWrapperException | IOException ex) {
            log.error(ex.getMessage());
            allegatoInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
            allegatoInformation.setDescrizioneErrore(ex.getMessage());
        }
        
        return allegatoInformation;
    }
    
    /**
     * Aggiunge come metadato Mittente l'azienda (eg. Azienda USL Bologna)
     * @param docAttributes La lista dei metadati a cui verrà aggiunto il mittente.
     * @param doc Il documento.
     * @param index L'indice del metadato ricorsivo.
     */
    private void addMittenteAzienda(List<DocumentAttribute> docAttributes, DocDetail docDetail, int index) {
        String denominazione = getDenominazione(docDetail);
        addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PAI")
            .addNewAttribute(docAttributes, InfocertAttributesEnum.DENOMINAZIONE_N, index, denominazione);
        Pec pecMittente = Optional.ofNullable(docDetail.getIdPecMittente()).orElse(null);
        if (pecMittente != null) {
            addNewAttribute(docAttributes, InfocertAttributesEnum.INDIRIZZI_DIGITALI_DI_RIFERIMENTO_N, index, pecMittente.getIndirizzo());
        }
    }
    
    /**
     * Aggiunge il metadato Mittente per un protocollo in entrata o un protocollo in uscita che è in smistamento.
     * Se presente, aggiunge anche l'indirizzo email.
     * @param docAttributes La lista dei metadati a cui verrà aggiunto il mittente.
     * @param docDetail Il dettagli del documento.
     * @param index L'indice del metadato ricorsivo.
     * @throws VersatoreProcessingException Errore nel caso non sia presente il mittente sul doc.
     */
    private void addMittenteProtEntrataPuSmistamento(List<DocumentAttribute> docAttributes, DocDetail docDetail, int index) throws VersatoreProcessingException {
        addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO_N, index, "mittente");
        Contatto idContattoMittente = Optional.ofNullable(docDetail.getIdContattoMittente()).orElse(null);
        if  (idContattoMittente != null) { 
            addContattoRubrica(docAttributes, idContattoMittente, index);
        } else if (docDetail.getMittente() != null || StringUtils.hasLength(docDetail.getMittente())) {
            addSoggettoGenerico(docAttributes, docDetail.getMittente(), index);
        } else {
            String message = "Manca il campo mittente";
            log.error(message);
             throw new VersatoreProcessingException(message);
        }
    }
    
    /**
     * Aggiunge alla lista dei metadati un contatto dalla rubrica.
     * @param docAttributes La lista dei metadati a cui verrà aggiunto il contatto. 
     * @param contatto Il contatto della rubrica.
     * @param index L'indice del metadato ricorsivo.
     */
    private void addContattoRubrica(List<DocumentAttribute> docAttributes, Contatto contatto, int index) {
        switch (contatto.getTipo()) {
            case AZIENDA:
            case FORNITORE:
                addContattoAziendaOrPA(docAttributes, contatto, index, "PG");
                break;
            case PERSONA_FISICA:
                addContattoPersonaFisica(docAttributes, contatto, index);
                break;
            case PUBBLICA_AMMINISTRAZIONE_ITALIANA:
                addContattoAziendaOrPA(docAttributes, contatto, index, "PAI");
                break;
            case ORGANIGRAMMA:
                if (contatto.getCategoria() == Contatto.CategoriaContatto.PERSONA) {
                    addContattoPersonaFisica(docAttributes, contatto, index);
                } else if (contatto.getCategoria() == Contatto.CategoriaContatto.STRUTTURA) {
                    addContattoAziendaOrPA(docAttributes, contatto, index, "PG");
                }
                break;
            case PUBBLICA_AMMINISTRAZIONE_ESTERA:
            case VARIO:
                addContattoAziendaOrPA(docAttributes, contatto, index, "PG");
                break;
        }
    }
    
    /**
     * Aggiunge alla lista dei metadati un contatto di persona dalla rubrica. 
     * @param docAttributes La lista dei metadati a cui verrà aggiunto il contatto.
     * @param contatto Il contatto della rubrica.
     * @param index L'indice del metadato ricorsivo.
     */
    private void addContattoPersonaFisica(List<DocumentAttribute> docAttributes, Contatto contatto, int index) {
        addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PF")
            .addNewAttribute(docAttributes, InfocertAttributesEnum.COGNOME_N, index, contatto.getCognome())
            .addNewAttribute(docAttributes, InfocertAttributesEnum.NOME_N, index, contatto.getNome());
        if (StringUtils.hasLength(contatto.getCodiceFiscale())) 
            addNewAttribute(docAttributes, InfocertAttributesEnum.CODICE_FISCALE_N, index, contatto.getCodiceFiscale());
    }
    
    /**
     * Aggiunge alla lista dei metadati un contatto di un'azienda o una Pubblica Amminstrazione dalla rubrica. 
     * @param docAttributes La lista dei metadati a cui verrà aggiunto il contatto.
     * @param contatto Il contatto della rubrica.
     * @param index L'indice del metadato ricorsivo.
     * @param tipo Il tipo "PG" oppure "PAI".
     */
    private void addContattoAziendaOrPA(List<DocumentAttribute> docAttributes, Contatto contatto, int index, String tipo) {
        String descrizione = contatto.getRagioneSociale() != null ? contatto.getRagioneSociale() : contatto.getDescrizione();
        
        addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, tipo)
            .addNewAttribute(docAttributes, InfocertAttributesEnum.DENOMINAZIONE_N, index, descrizione);
        if (StringUtils.hasLength(contatto.getPartitaIva())) 
            addNewAttribute(docAttributes, InfocertAttributesEnum.CODICE_FISCALE_N, index, contatto.getPartitaIva());
        
        Optional<DettaglioContatto> dettaglioContatto = contatto.getDettaglioContattoList()
                .stream()
                .filter(d -> DettaglioContatto.TipoDettaglio.EMAIL.equals(d.getTipo()) && Objects.equals(d.getPrincipale(), Boolean.TRUE))
                .findFirst();
        if (dettaglioContatto.isPresent())
            addNewAttribute(docAttributes, InfocertAttributesEnum.INDIRIZZI_DIGITALI_DI_RIFERIMENTO_N, index, dettaglioContatto.get().getDescrizione());
    }
    
    /**
     * Aggiunge alla lista dei metadati un soggetto con descrizione generica.
     * @param docAttributes La lista dei metadati a cui verrà aggiunto il soggetto.
     * @param descrizione La descrizione del soggetto.
     * @param index L'indice del metadato ricorsivo.
     */
    private void addSoggettoGenerico(List<DocumentAttribute> docAttributes, String descrizione, int index) {
        addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PG")
            .addNewAttribute(docAttributes, InfocertAttributesEnum.DENOMINAZIONE_N, index, descrizione);
    }
    
    /**
     * Metodo che controlla lo stato del versamento di un allegato 
     * e verifica se è di nuovo da versare oppure controllare.
     * @param versamentoAllegato Il versamento allegato.
     * @return Azione versamento VERSA oppure CONTROLLO.
     */
    private AzioneVersamento getAzioneVersamento(final VersamentoAllegato versamentoAllegato) {
        AzioneVersamento azione = null;
        switch (versamentoAllegato.getStato()) {
            case AGGIORNARE:
            case VERSARE:
            case ERRORE_RITENTABILE:
                azione = AzioneVersamento.VERSA;
                break;
            case IN_CARICO:
                azione = AzioneVersamento.CONTROLLO;
                break;
        }
        return azione;
    }
    
    /**
     * Inizializza il web service di Infocert per effettuare le operazioni di Submit e getStatus.
     * @return Il web service.
     * @throws MalformedURLException Errore nell'URL dell'end point del servizio.
     */
    private GenericDocument initInfocertService() throws MalformedURLException {
        GenericDocumentService iss = new GenericDocumentService(new URL(infocertVersatoreServiceEndPointUri));
        GenericDocument is = iss.getGenericDocumentPort();
        BindingProvider bp = (BindingProvider) is;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                infocertVersatoreServiceEndPointUri);
        return is;
    }
    
    /**
     * Recupera un attributo del metadato dal json dei metadati versati.
     * @param metadatiVersati I metadati.
     * @return Il valore dell'attributo.
     */
    private String getAttributoFromMetadati(final String metadatiVersati, final InfocertAttributesEnum attributo) throws VersatoreProcessingException {
        String attr = null;
        try {
            final JsonNode jsonNode = objectMapper.readTree(metadatiVersati);
            attr = jsonNode.get(attributo.toString()).toString();
        } catch (JsonProcessingException ex) {
            log.error(ERROR_PARSING_JSON, ex);
            throw new VersatoreProcessingException(ERROR_PARSING_JSON, ex);
        }
        return attr;
    }

    /**
     * Restituisce la versione del doc effettuanto il controllo sui metadati versati nell'ultimo versamento,
     * altrimenti restituisce il default "1".
     * @param docInfo L'oggetto docInformation.
     * @return La versione del documento.
     * @throws Errori nel parsing dei metadati. 
     */
    private String getVersioneDocumento(final VersamentoDocInformation docInfo) throws VersatoreProcessingException {
        String versione = "1";
        if (docInfo.getStatoVersamento() == Versamento.StatoVersamento.AGGIORNARE) {
            final JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
            final QVersamento qVersamento = QVersamento.versamento;
            final Versamento ultimoVersamento = queryFactory
                    .select(qVersamento)
                    .from(qVersamento)
                    .where(qVersamento.idDoc.id.eq(docInfo.getIdDoc())
                            .and(qVersamento.stato.eq(Versamento.StatoVersamento.VERSATO.toString())))
                    .orderBy(qVersamento.data.desc())
                    .fetchFirst();
            if (ultimoVersamento != null) {
                final String versioneDoc = getAttributoFromMetadati(ultimoVersamento.getMetadatiVersati(), InfocertAttributesEnum.VERSIONE_DEL_DOCUMENTO);
                final Integer versioneInt = Integer.valueOf(versioneDoc) + 1;
                versione = versioneInt.toString();
            }
        }
        return versione;
    }
    
    
    /**
     * Legge l'inputStream e lo restituisce in formato String.
     * @param is L'inputStream del file.
     * @return Il contenuto del file in formato Stringa.
     * @throws IOException.
     */
    private static String convertInputStreamToString(InputStream is) throws IOException {

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }
    /**
     * Restituisce il tipo registro del documento.
     * @param doc Il documento.
     * @return Il tipo registro.
     */
    private String getTipoRegistro(final DocDetail docDetail) {
        String tipoRegistro;
        switch (docDetail.getTipologia()) {
            case PROTOCOLLO_IN_ENTRATA:
            case PROTOCOLLO_IN_USCITA:
                tipoRegistro = "Protocollo Ordinario";
                break;
            default:
                tipoRegistro = "Registro";
                break;
        }
        return tipoRegistro;
    }
    
    /**
     * Restituisce il codice registro del documento.
     * @param doc Il documento.
     * @return Il codice registro.
     */
    private String getCodiceRegistro(final DocDetail docDetail) {
        String codice;
        switch (docDetail.getTipologia()) {
            case PROTOCOLLO_IN_ENTRATA:
            case PROTOCOLLO_IN_USCITA:
                codice = "PG";
                break;
            case DETERMINA:
                codice = "DETE";
                break;
            case DELIBERA:
                codice = "DELI";
                break;
            case DOCUMENT:
            case DOCUMENT_UTENTE:
                codice = "FASCICOLO";
                break;
            default:
                codice = docDetail.getTipologia().toString();
                break;
        }
        return codice;
    }
    
    /**
     * Restituisce la denominazione del produttore.
     * @param docDetail Il documento detail.
     * @return La denominazione.
     */
    private String getDenominazione(final DocDetail docDetail) {
        String denominazione =  docDetail.getIdAzienda().getDescrizione();
        if (docDetail.getIdStrutturaRegistrazione() != null) {
            denominazione = String.join(" - ", denominazione, docDetail.getIdStrutturaRegistrazione().getNome());
        }
        return denominazione;
    }
    
    /**
     * Restituisce la lista degli attori del doc filtrati per il ruolo passato.
     * @param doc Il documento.
     * @param ruolo Il ruolo.
     * @return La lista di attori del documento.
     */
    private List<AttoreDoc> getAttoriDoc(final Doc doc, final String ruolo) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QAttoreDoc qAttoreDoc = QAttoreDoc.attoreDoc;

        List<AttoreDoc> attoriDoc = queryFactory
                .select(qAttoreDoc)
                .from(qAttoreDoc)
                .where(qAttoreDoc.idDoc.eq(doc).and(qAttoreDoc.ruolo.eq(ruolo)))
                .fetch();
        if (attoriDoc.isEmpty()) {
            log.error("Attori {} not found", ruolo);
        }
        return attoriDoc;
    }
}
