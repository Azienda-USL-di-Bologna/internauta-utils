package it.bologna.ausl.internauta.utils.versatore.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sun.xml.ws.fault.ServerSOAPFaultException;
import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.enums.InfocertAttributesEnum;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.utils.VersatoreConfigParams;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.AttoreDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import static it.bologna.ausl.model.entities.scripta.DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA;
import static it.bologna.ausl.model.entities.scripta.DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QAttoreDoc;
import it.bologna.ausl.model.entities.scripta.QRegistro;
import it.bologna.ausl.model.entities.scripta.QRegistroDoc;
import it.bologna.ausl.model.entities.scripta.QTitolo;
import it.bologna.ausl.model.entities.scripta.RegistroDoc;
import it.bologna.ausl.model.entities.scripta.Titolo;
import it.bologna.ausl.model.entities.versatore.QVersamentoAllegato;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.ws.BindingProvider;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class InfocertVersatoreService extends VersatoreDocs {
    
    private enum AzioneVersamento {
        VERSA, CONTROLLO
    };

    private static final Logger log = LoggerFactory.getLogger(InfocertVersatoreService.class);
    private static final String INFOCERT_VERSATORE_SERVICE = "InfocertVersatoreService";

    private final String infocertVersatoreServiceEndPointUri;
    private final MinIOWrapper minIOWrapper;

    public InfocertVersatoreService(EntityManager entityManager, TransactionTemplate transactionTemplate, VersatoreRepositoryConfiguration versatoreRepositoryConfiguration, VersatoreConfigParams versatoreConfigParams, VersatoreConfiguration configuration) {
        super(entityManager, transactionTemplate, versatoreRepositoryConfiguration, versatoreConfigParams, configuration);

        Map<String, Object> versatoreConfiguration = configuration.getParams();
        Map<String, Object> infocertServiceConfiguration = (Map<String, Object>) versatoreConfiguration.get(INFOCERT_VERSATORE_SERVICE);
        infocertVersatoreServiceEndPointUri = infocertServiceConfiguration.get("InfocertVersatoreServiceEndPointUri").toString();
        minIOWrapper = versatoreRepositoryConfiguration.getVersatoreRepositoryManager().getMinIOWrapper();
        log.info("URI: {}", infocertVersatoreServiceEndPointUri);
    }

    
    public VersamentoAllegatoInformation controllaStatoVersamento(
            VersamentoAllegato versamentoAllegato,
            GenericDocument infocertService) throws VersatoreProcessingException {
        DocumentStatus status = null;
        VersamentoAllegatoInformation allegatoInformation = new VersamentoAllegatoInformation();
        try {            
            allegatoInformation.setIdAllegato(versamentoAllegato.getIdAllegato().getId());
            allegatoInformation.setDataVersamento(ZonedDateTime.now());
            allegatoInformation.setTipoDettaglioAllegato(versamentoAllegato.getDettaglioAllegato());

            status = infocertService.getDocumentStatus(versamentoAllegato.getRapporto()); 

            if (status.getDocumentError() != null) {
                allegatoInformation.setCodiceErrore(status.getDocumentError().getErrorCode());
                allegatoInformation.setDescrizioneErrore(status.getDocumentError().getErrorDescription());
                allegatoInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
            }

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
            
            ObjectMapper objectMapper = new ObjectMapper(); 
            allegatoInformation.setRapporto(objectMapper.writeValueAsString(status));
            
        } catch (JsonProcessingException ex) {
            log.error("Errore parsing status", ex);
        }
        return allegatoInformation;
    }    

    /**
     * Metodo astratto che implementa il versamento Infocert.
     * Costruisce l'oggetto contentente i metadati del doc che verranno inviati ad Infocert.
     * @param versamentoInformation L'oggetto con l'idDoc e l'idArchivio da versare.
     * @return Lo stesso oggetto VersamentoInformation con i risultati dei versamenti.
     * @throws VersatoreProcessingException Eccezioni.
     */
    @Override
    public VersamentoDocInformation versaImpl(VersamentoDocInformation versamentoDocInformation) throws VersatoreProcessingException {
              
        Integer idDoc = versamentoDocInformation.getIdDoc();
        Doc doc = entityManager.find(Doc.class, idDoc);
        try {
            GenericDocument infocertService = initInfocertService();
            
            if (versamentoDocInformation.isPrimoVersamento() == Boolean.TRUE) {
                versamentoDocInformation = versaDoc(doc, versamentoDocInformation, infocertService);
            } else {
                List<DocumentAttribute> docAttributes = new ArrayList();
                List<VersamentoAllegatoInformation> versamentiAllegatiInfo = new ArrayList();
                List<VersamentoAllegato> versamentiAllegati = getVersamentiAllegati(versamentoDocInformation.getIdVersamentoPrecedente());
                versamentoDocInformation.setDataVersamento(ZonedDateTime.now());
                for (VersamentoAllegato versamentoAllegato : versamentiAllegati) {
                    switch (getAzioneVersamento(versamentoAllegato)) {
                        case VERSA:
                            if (docAttributes.isEmpty()) {
                                docAttributes = buildMetadatiDoc(doc, versamentoDocInformation);
                            }
                            Allegato allegato = versamentoAllegato.getIdAllegato();
                            Integer index = getIndexAllegato(versamentoAllegato.getMetadatiVersati());
                            VersamentoAllegatoInformation allegatoInfo = 
                                    versaAllegato(allegato, versamentoAllegato.getDettaglioAllegato(), docAttributes, index, infocertService);
                            versamentiAllegatiInfo.add(allegatoInfo);
                            break;
                        case CONTROLLO:
                            VersamentoAllegatoInformation allegatoInformation = controllaStatoVersamento(versamentoAllegato, infocertService);
                            versamentiAllegatiInfo.add(allegatoInformation);
                        break;
                    }  
                }
                versamentoDocInformation.setVersamentiAllegatiInformations(versamentiAllegatiInfo);
            }
            boolean errorOccured = versamentoDocInformation.getVersamentiAllegatiInformations().stream().filter(all -> 
                    List.of(Versamento.StatoVersamento.ERRORE, Versamento.StatoVersamento.ERRORE_RITENTABILE).contains(all.getStatoVersamento()))
                    .findFirst().isPresent();
            if (errorOccured) {
                log.error("Versamenti di allegati in errore");
                versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
            } else {
                versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.IN_CARICO);
            }
        } catch (MalformedURLException ex) {
            log.error("Errore URL", ex);
        }        
                
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
        log.info("Inizio versamento del doc: {}", docId);
        
        // Build metadati generici dell'unità documentaria
        List<DocumentAttribute> docAttributes = buildMetadatiDoc(doc, versamentoDocInformation);
              
        // Elaborazione degli allegati
        ObjectMapper objectMapper = new ObjectMapper();
        try {            
            List<Allegato> allegati = doc.getAllegati();
            List<VersamentoAllegatoInformation> versamentiAllegatiInfo = new ArrayList();
            
            versamentoDocInformation.setDataVersamento(ZonedDateTime.now());
            versamentoDocInformation.setMetadatiVersati(objectMapper.writeValueAsString(docAttributes));
            int i = 1; // Indice per il metadato ALLEGATI_NUMERO
            
            for (Allegato allegato: allegati) {
                for (Allegato.DettagliAllegato.TipoDettaglioAllegato tipoDettaglioAllegato : Allegato.DettagliAllegato.TipoDettaglioAllegato.values()) {
                    VersamentoAllegatoInformation versaAllegato = 
                            versaAllegato(allegato, tipoDettaglioAllegato, docAttributes, i++, infocertService);        
                    versamentiAllegatiInfo.add(versaAllegato);
                }
            }
            versamentoDocInformation.setVersamentiAllegatiInformations(versamentiAllegatiInfo);
            
        } catch (JsonProcessingException ex) {
            log.error("Errore nel parsing dei metadati versati");
        }
        log.info("Versamento idDoc: {} concluso.", docId);
        return versamentoDocInformation;
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
    private List<DocumentAttribute> buildMetadatiDoc(Doc doc, VersamentoDocInformation versamentoDocInformation) {
        DocDetail docDetail = entityManager.find(DocDetail.class, doc.getId());
        Archivio archivio = entityManager.find(Archivio.class, versamentoDocInformation.getIdArchivio());
        int index = 2;
        List<DocumentAttribute> docAttributes = new ArrayList();
        
        // Dati generici del documento
        addNewAttribute(docAttributes, InfocertAttributesEnum.IDENTIFICATIVO_DOCUMENTO, docDetail.getId().toString())
                .addNewAttribute(docAttributes, InfocertAttributesEnum.DATA_DOCUMENTO, docDetail.getDataRegistrazione().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addNewAttribute(docAttributes, InfocertAttributesEnum.MODALITA_DI_FORMAZIONE, "b")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPOLOGIA_DOCUMENTALE, "Protocolli") // Da verificare
                .addNewAttribute(docAttributes, InfocertAttributesEnum.DATA_REGISTRAZIONE, docDetail.getDataRegistrazione().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_REGISTRO, docDetail.getRiservato() ? "Protocollo Riservato" : "Protocollo Ordinario") // Registro? Protocolli Emergenza?
                .addNewAttribute(docAttributes, InfocertAttributesEnum.CODICE_REGISTRO, getNumeroRegistro(doc))
                .addNewAttribute(docAttributes, InfocertAttributesEnum.NUMERO_DOCUMENTO,
                        String.join("/", docDetail.getNumeroRegistrazione().toString(), docDetail.getAnnoRegistrazione().toString()))
                .addNewAttribute(docAttributes, InfocertAttributesEnum.OGGETTO, docDetail.getOggetto());
       
        // Metadati degli Agenti (Soggetti)
        addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO, "produttore")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO, "PAI")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.DENOMINAZIONE, docDetail.getIdStrutturaRegistrazione().getNome())
                .addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO_N, index, "responsabile")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PF")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.COGNOME_N, index, docDetail.getIdPersonaResponsabileProcedimento().getCognome())
                .addNewAttribute(docAttributes, InfocertAttributesEnum.NOME_N, index++, docDetail.getIdPersonaResponsabileProcedimento().getNome()); // Index 3 
        //Questa parte è da rivedere
        switch (docDetail.getTipologia()) {
            case PROTOCOLLO_IN_USCITA:
                addNewAttribute(docAttributes, InfocertAttributesEnum.TIPOLOGIA_DI_FLUSSO, "U")
                        .addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO_N, index, "mittente")
                        .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PAI");
                // Se il PU ha il campo mittente è in smistamento
                if (docDetail.getMittente() == null || !StringUtils.hasLength(docDetail.getMittente())) {
                    addNewAttribute(docAttributes, InfocertAttributesEnum.DENOMINAZIONE_N, index++, doc.getIdAzienda().getDescrizione()); // Codice IPA?
                } else {
                    addNewAttribute(docAttributes, InfocertAttributesEnum.DENOMINAZIONE_N, index++, docDetail.getMittente());
                }                        
                break;
            case PROTOCOLLO_IN_ENTRATA:
                addNewAttribute(docAttributes, InfocertAttributesEnum.TIPOLOGIA_DI_FLUSSO, "E")
                        .addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO_N, index, "mittente")
                        .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PAI")
                        .addNewAttribute(docAttributes, InfocertAttributesEnum.DENOMINAZIONE_N, index++, docDetail.getIdStrutturaRegistrazione().getNome());
                break;
        }
        
        // Metadati di archiviazione
        Titolo titolo = getTitolo(doc, archivio);
        if (titolo != null) {
           addNewAttribute(docAttributes, InfocertAttributesEnum.INDICE_DI_CLASSIFICAZIONE, titolo.getClassificazione())
                   .addNewAttribute(docAttributes, InfocertAttributesEnum.DESCRIZIONE_CLASSIFICAZIONE, titolo.getNome());
        }
        
        addNewAttribute(docAttributes, InfocertAttributesEnum.RISERVATO, docDetail.getRiservato().toString())
                .addNewAttribute(docAttributes, InfocertAttributesEnum.VERSIONE_DEL_DOCUMENTO, "1")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.ID_AGGREGAZIONE, archivio.getNumerazioneGerarchica())
                .addNewAttribute(docAttributes, 
                        InfocertAttributesEnum.TEMPO_DI_CONSERVAZIONE,
                        archivio.getAnniTenuta() != null ? archivio.getAnniTenuta().toString() : "999");
        
        return docAttributes;
    }
    
    private VersamentoAllegatoInformation versaAllegato(
            Allegato allegato,
            Allegato.DettagliAllegato.TipoDettaglioAllegato tipoDettaglioAllegato,
            List<DocumentAttribute> docAttributes,
            Integer indexAllegato,
            GenericDocument infocertService) {
        
        List<DocumentAttribute> fileAttributes;
        List<DocumentAttribute> docFileAttributes;
        Allegato.DettaglioAllegato dettaglioAllegato = allegato.getDettagli().getByKey(tipoDettaglioAllegato);

//        if (dettaglioAllegato == null) continue;

        fileAttributes = new ArrayList();
        String mimeType = "application/pdf";
        if (dettaglioAllegato.getMimeType() != null) {
            mimeType = dettaglioAllegato.getMimeType();
        }
        VersamentoAllegatoInformation allegatoInformation = new VersamentoAllegatoInformation();
        try ( InputStream fileStream = minIOWrapper.getByFileId(dettaglioAllegato.getIdRepository())) {
            if (fileStream != null) {
                // Metadati degli allegati
                addNewAttribute(fileAttributes, InfocertAttributesEnum.IDENTIFICATIVO_DEL_FORMATO, mimeType)
//                                    .addNewAttribute(fileAttributes, InfocertAttributesEnum.NOME_FILE, dettaglioAllegato.getNome()) // Metadato sconosciuto
                        .addNewAttribute(fileAttributes, InfocertAttributesEnum.IMPRONTA, dettaglioAllegato.getHashMd5())
                        .addNewAttribute(fileAttributes, InfocertAttributesEnum.ALGORITMO, "md5")
                        .addNewAttribute(fileAttributes, InfocertAttributesEnum.ALLEGATI_NUMERO, String.valueOf(indexAllegato))
                        .addNewAttribute(fileAttributes, InfocertAttributesEnum.ID_DOC_INDICE_ALLEGATI, allegato.getIdDoc().getId().toString())
                        .addNewAttribute(fileAttributes, InfocertAttributesEnum.DESCRIZIONE_ALLEGATI, allegato.getTipo().toString());

                if (tipoDettaglioAllegato.toString().contains("FIRMATO")) {
                    addNewAttribute(fileAttributes, InfocertAttributesEnum.VERIFICA_FIRMA_DIGITALE, "true");
                }

                ByteArrayDataSource byteArrayDataSource = new ByteArrayDataSource(fileStream, mimeType);
                DataHandler data = new DataHandler(byteArrayDataSource);
                docFileAttributes = new ArrayList();
                docFileAttributes.addAll(docAttributes);
                docFileAttributes.addAll(fileAttributes);

                ObjectMapper objectMapper = new ObjectMapper(); 
                allegatoInformation.setIdAllegato(allegato.getId());
                allegatoInformation.setDataVersamento(ZonedDateTime.now());
                allegatoInformation.setTipoDettaglioAllegato(tipoDettaglioAllegato);
                allegatoInformation.setMetadatiVersati(objectMapper.writeValueAsString(docFileAttributes));

                String hash = infocertService.submitDocument(allegato.getIdDoc().getId().toString(), docFileAttributes, data);
                allegatoInformation.setRapporto(hash);
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
    
    private GenericDocument initInfocertService() throws MalformedURLException {
        GenericDocumentService iss = new GenericDocumentService(new URL(infocertVersatoreServiceEndPointUri));
        GenericDocument is = iss.getGenericDocumentPort();
        BindingProvider bp = (BindingProvider) is;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                infocertVersatoreServiceEndPointUri);
        return is;
    }
    
    private Integer getIndexAllegato(String metadatiVersati) {
        Integer indexAllegato = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(metadatiVersati);
            indexAllegato = jsonNode.get(InfocertAttributesEnum.ALLEGATI_NUMERO.toString()).asInt();
        } catch (JsonProcessingException ex) {
            log.error("Errore parsing metadati.");
        }
        return indexAllegato;
    }

    /**
     * Restituisce il numero del registro in cui il documento è registrato.
     * @param doc Il documento.
     * @return Il numero registro.
     */
    private String getNumeroRegistro(final Doc doc) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QRegistroDoc qRegistroDoc = QRegistroDoc.registroDoc;
        QRegistro qRegistro = QRegistro.registro;

        RegistroDoc registroDoc = queryFactory
                .select(qRegistroDoc)
                .from(qRegistroDoc)
                .innerJoin(qRegistroDoc.idRegistro, qRegistro)
                .where(qRegistroDoc.idDoc.eq(doc)
                        .and(qRegistro.idAzienda.eq(doc.getIdAzienda())
                                .and(qRegistro.ufficiale.eq(true)))
                        .and(qRegistro.attivo.eq(true)))
                .fetchFirst();
        if (registroDoc != null) {
            return registroDoc.getNumero().toString();
        } else {
            return "";
        }
    }
    
    /**
     * Restituisce i versamenti allegati del versamento passato in ingresso.
     * @param versamento Il versamento.
     * @return I versamenti allegati.
     */
    private List<VersamentoAllegato> getVersamentiAllegati(final Integer idVersamento) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QVersamentoAllegato qVersamentoAllegato = QVersamentoAllegato.versamentoAllegato;
        List<VersamentoAllegato> versamentiAllegati = queryFactory
                .select(qVersamentoAllegato)
                .from(qVersamentoAllegato)
                .where(qVersamentoAllegato.idVersamento.id.eq(idVersamento))
                .fetch();
        if (versamentiAllegati.isEmpty()) {
            log.error("Versamenti allegati of versamento {} not found", idVersamento);
        }
        return versamentiAllegati;
    }
    
    /**
     * @deprecated Perché non è necessario indicare gli attori del doc attraverso questo metodo.
     * Restituisce la lista degli attori del doc filtrati per il ruolo passato.
     * @param doc Il documento.
     * @param ruolo Il ruolo.
     * @return La lista di attori del documento.
     */
    @Deprecated
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

    /**
     * Restituisce la concatenazione delle classificazioni del documento
     * passato.
     *
     * @param doc Il Documento.
     * @return La stringa concatenata nel formato [CODICE] NOME separate da ",".
     */
    private Titolo getTitolo(final Doc doc, final Archivio archivio) {

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QTitolo qTitoli = QTitolo.titolo;
        QArchivioDoc qArchivioDoc = QArchivioDoc.archivioDoc;
        QArchivio qArchivio = QArchivio.archivio;

        Titolo titolo = queryFactory
                .select(qTitoli)
                .from(qArchivioDoc)
                .innerJoin(qArchivioDoc.idArchivio, qArchivio)
                .innerJoin(qArchivio.idTitolo, qTitoli)
                .where(qArchivioDoc.idDoc.eq(doc).and(qArchivioDoc.idArchivio.eq(archivio)))
                .fetchOne();

        if (titolo == null) {
            //TODO: Handle exception
            log.error("Titoli not found");
        }
        return titolo;
    }

    /**
     * Restituisce la concatenazione dei destinatari del documento detail passato.
     *
     * @param docDetail Il documento.
     * @return La stringa concatenata dei destinatari separati da ','.
     * @deprecated Al momento non è più utile.
     */
    @Deprecated
    private String getDestinatari(DocDetail docDetail) {
        List<String> destinatari = new ArrayList();
        docDetail.getDestinatari().stream().forEach(d -> destinatari.add(d.getNome() != null ? d.getNome() : d.getIndirizzo()));

        return String.join(",", destinatari);
    }
}
