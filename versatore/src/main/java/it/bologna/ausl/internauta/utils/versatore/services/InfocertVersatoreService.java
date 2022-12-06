package it.bologna.ausl.internauta.utils.versatore.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sun.xml.ws.fault.ServerSOAPFaultException;
import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.enums.InfocertAttributesEnum;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreConfigurationException;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreServiceException;
import it.bologna.ausl.internauta.utils.versatore.utils.VersatoreConfigParams;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.AttoreDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QAttoreDoc;
import it.bologna.ausl.model.entities.scripta.QRegistro;
import it.bologna.ausl.model.entities.scripta.QRegistroDoc;
import it.bologna.ausl.model.entities.scripta.QTitolo;
import it.bologna.ausl.model.entities.scripta.RegistroDoc;
import it.bologna.ausl.model.entities.scripta.Titolo;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import it.bologna.ausl.utils.versatore.infocert.wsclient.DocumentAttribute;
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
import java.util.HashMap;
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

    private static final Logger log = LoggerFactory.getLogger(InfocertVersatoreService.class);
    private static final String INFOCERT_VERSATORE_SERVICE = "InfocertVersatoreService";

    private final String infocertVersatoreServiceEndPointUri;

    public InfocertVersatoreService(EntityManager entityManager, TransactionTemplate transactionTemplate, VersatoreRepositoryConfiguration versatoreRepositoryConfiguration, VersatoreConfigParams versatoreConfigParams, VersatoreConfiguration configuration) {
        super(entityManager, transactionTemplate, versatoreRepositoryConfiguration, versatoreConfigParams, configuration);

        Map<String, Object> versatoreConfiguration = configuration.getParams();
        Map<String, Object> infocertServiceConfiguration = (Map<String, Object>) versatoreConfiguration.get(INFOCERT_VERSATORE_SERVICE);
        infocertVersatoreServiceEndPointUri = infocertServiceConfiguration.get("InfocertVersatoreServiceEndPointUri").toString();
        log.info(String.format("URI: %s", infocertVersatoreServiceEndPointUri));
    }

    @Override
    public VersamentoDocInformation versaAbstract(VersamentoDocInformation versamentoInformation) throws VersatoreConfigurationException {
              
        Integer idDoc = versamentoInformation.getIdDoc();
        Doc doc = entityManager.find(Doc.class, idDoc);
        versamentoInformation = versaDoc(doc, versamentoInformation);
        return versamentoInformation;
    }
    
    private VersamentoDocInformation versaDoc(Doc doc, VersamentoDocInformation versamentoDocInformation) throws VersatoreConfigurationException {
        
        DocDetail docDetail = entityManager.find(DocDetail.class, doc.getId());
        Archivio archivio = entityManager.find(Archivio.class, versamentoDocInformation.getIdArchivio());
        
        Struttura strutturaRegistrazione = docDetail.getIdStrutturaRegistrazione();
        String docID = doc.getId().toString();
        int index = 2;
        List<DocumentAttribute> docAttributes = new ArrayList();
        addNewAttribute(docAttributes, InfocertAttributesEnum.IDENTIFICATIVO_DOCUMENTO, docDetail.getId().toString())
                .addNewAttribute(docAttributes, InfocertAttributesEnum.MODALITA_DI_FORMAZIONE, "b")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPOLOGIA_DOCUMENTALE, "Protocolli") // BOOOOH
                .addNewAttribute(docAttributes, InfocertAttributesEnum.DATA_REGISTRAZIONE, docDetail.getDataRegistrazione().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_REGISTRO, "Registro_PG") // Protocollo emergenza??
                .addNewAttribute(docAttributes, InfocertAttributesEnum.CODICE_REGISTRO, getNumeroRegistro(doc)) // Protocollo emergenza??
                .addNewAttribute(docAttributes, InfocertAttributesEnum.NUMERO_DOCUMENTO,
                        String.join("/", docDetail.getNumeroRegistrazione().toString(), docDetail.getAnnoRegistrazione().toString()))
                .addNewAttribute(docAttributes, InfocertAttributesEnum.OGGETTO, docDetail.getOggetto())
                .addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO, "produttore")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO, "PAI")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.DENOMINAZIONE, strutturaRegistrazione.getNome())
                .addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO_N, index, "assegnatario")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PAI")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.DENOMINAZIONE_N, index++, "//TODO")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.RISERVATO, docDetail.getRiservato().toString())
                .addNewAttribute(docAttributes, InfocertAttributesEnum.VERSIONE_DEL_DOCUMENTO, "1")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.ID_AGGREGAZIONE, archivio.getNumerazioneGerarchica())
                .addNewAttribute(docAttributes, 
                        InfocertAttributesEnum.TEMPO_DI_CONSERVAZIONE,
                        archivio.getAnniTenuta() != null ? archivio.getAnniTenuta().toString() : "999")
                .addNewAttribute(docAttributes, InfocertAttributesEnum.SEGNATURA, "b"); //TODO: Da chiarire

        Titolo titolo = getTitolo(doc, archivio);
        if (titolo != null) {
           addNewAttribute(docAttributes, InfocertAttributesEnum.INDICE_DI_CLASSIFICAZIONE, titolo.getClassificazione())
                   .addNewAttribute(docAttributes, InfocertAttributesEnum.DESCRIZIONE_CLASSIFICAZIONE, titolo.getNome());
        }
        
        //TODO: Questa parte è tutta da rivedere
        switch (docDetail.getTipologia()) {
            case PROTOCOLLO_IN_USCITA:
                addNewAttribute(docAttributes, InfocertAttributesEnum.DATA_DOCUMENTO, docDetail.getDataRegistrazione().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPOLOGIA_DI_FLUSSO, "U")
                        .addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO_N, index, "mittente")
                        .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PAI");
   
                // Se il PU ha il campo mittente è in smistamento
                if (docDetail.getMittente() == null || !StringUtils.hasLength(docDetail.getMittente())) {
                    addNewAttribute(docAttributes, InfocertAttributesEnum.DENOMINAZIONE_N, index++, doc.getIdAzienda().getDescrizione()); // Codice IPA?
                } else {
                    addNewAttribute(docAttributes, InfocertAttributesEnum.DENOMINAZIONE_N, index++, docDetail.getMittente()) // Codice IPA?
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO_N, index, "redattore")
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PF")
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.COGNOME_N, index, docDetail.getIdPersonaRedattrice().getCognome())
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.NOME_N, index++, docDetail.getIdPersonaRedattrice().getNome())
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO_N, index, "destinatario")
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PG")
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.DENOMINAZIONE_N, index++, getDestinatari(docDetail))
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO_N, index, "responsabile procedimento")
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PF")
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.COGNOME_N, index, docDetail.getIdPersonaResponsabileProcedimento().getCognome())
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.NOME_N, index++, docDetail.getIdPersonaResponsabileProcedimento().getNome())
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO_N, index, "direttore uo mittente")
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PF")
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.COGNOME_N, index, "Direttore uo")
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.NOME_N, index++, "Direttore uo");
                    
                    List<AttoreDoc> pareratori = getAttoriDoc(doc, "PARERI");
                    if (!pareratori.isEmpty()) {
                        for (AttoreDoc att: pareratori) {
                            addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO_N, index, "parere")
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PF")
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.COGNOME_N, index, att.getIdPersona().getCognome())
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.NOME_N, index++, att.getIdPersona().getNome()); 
                        }
                    }
                    
                    List<AttoreDoc> firmatari = getAttoriDoc(doc, "FIRMA");
                    if (!firmatari.isEmpty()) {
                        for (AttoreDoc att: firmatari) {
                            addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO_N, index, "firmatario")
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PF")
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.COGNOME_N, index, att.getIdPersona().getCognome())
                            .addNewAttribute(docAttributes, InfocertAttributesEnum.NOME_N, index++, att.getIdPersona().getNome()); 
                        }
                    }
                }
                break;


            case PROTOCOLLO_IN_ENTRATA:
                addNewAttribute(docAttributes, InfocertAttributesEnum.TIPOLOGIA_DI_FLUSSO, "E")
                        .addNewAttribute(docAttributes, InfocertAttributesEnum.RUOLO_N, index, "mittente")
                        .addNewAttribute(docAttributes, InfocertAttributesEnum.TIPO_SOGGETTO_N, index, "PAI")
                        .addNewAttribute(docAttributes, InfocertAttributesEnum.DENOMINAZIONE_N, index++, docDetail.getIdStrutturaRegistrazione().getNome());

                break;
        }
        
        ObjectMapper objectMapper = new ObjectMapper();
        HashMap<String,List<DocumentAttribute>> allAttributesMerged = new HashMap();
        versamentoDocInformation.setDataVersamento(ZonedDateTime.now());
        try {
            GenericDocumentService iss = new GenericDocumentService(new URL(infocertVersatoreServiceEndPointUri));

            GenericDocument is = iss.getGenericDocumentPort();
            BindingProvider bp = (BindingProvider) is;
            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                    infocertVersatoreServiceEndPointUri);
            
            MinIOWrapper minIOWrapper = versatoreRepositoryConfiguration.getVersatoreRepositoryManager().getMinIOWrapper();
            
            List<Allegato> allegati = doc.getAllegati();
            List<DocumentAttribute> fileAttributes;
            List<DocumentAttribute> docFileAttributes;
            
            List<VersamentoAllegatoInformation> versamentiAllegatiInfo = new ArrayList();
            allAttributesMerged.put("doc", docAttributes);
            int i = 1;
            boolean errorOccured = false;
            for (Allegato allegato: allegati) {
                for (Allegato.DettagliAllegato.TipoDettaglioAllegato tipoDettaglioAllegato : Allegato.DettagliAllegato.TipoDettaglioAllegato.values()) {
                    Allegato.DettaglioAllegato dettaglioAllegato = allegato.getDettagli().getByKey(tipoDettaglioAllegato);
                    
                    if (dettaglioAllegato == null) continue;
           
                    fileAttributes = new ArrayList();
                    String mimeType = "application/pdf";
                    if (dettaglioAllegato.getMimeType() != null) {
                        mimeType = dettaglioAllegato.getMimeType();
                    }
                    VersamentoAllegatoInformation allegatoInformation = new VersamentoAllegatoInformation();
                    try ( InputStream fileStream = minIOWrapper.getByFileId(dettaglioAllegato.getIdRepository())) {
                        if (fileStream != null) {
                            addNewAttribute(fileAttributes, InfocertAttributesEnum.IDENTIFICATIVO_DEL_FORMATO, mimeType)
                                    .addNewAttribute(fileAttributes, InfocertAttributesEnum.NOME_FILE, dettaglioAllegato.getNome()) // Metadato sconosciuto
                                    .addNewAttribute(fileAttributes, InfocertAttributesEnum.IMPRONTA, dettaglioAllegato.getHashMd5())
                                    .addNewAttribute(fileAttributes, InfocertAttributesEnum.ALGORITMO, "md5")
                                    .addNewAttribute(fileAttributes, InfocertAttributesEnum.ALLEGATI_NUMERO, String.valueOf(i++))
                                    .addNewAttribute(fileAttributes, InfocertAttributesEnum.ID_DOC_INDICE_ALLEGATI, docID)
                                    .addNewAttribute(fileAttributes, InfocertAttributesEnum.DESCRIZIONE_ALLEGATI, allegato.getTipo().toString());

                            ByteArrayDataSource byteArrayDataSource = new ByteArrayDataSource(fileStream, mimeType);
                            DataHandler data = new DataHandler(byteArrayDataSource);
                            docFileAttributes = new ArrayList();
                            docFileAttributes.addAll(docAttributes);
                            docFileAttributes.addAll(fileAttributes);
                            
                            allegatoInformation.setIdAllegato(allegato.getId());
                            allegatoInformation.setDataVersamento(ZonedDateTime.now());
                            allegatoInformation.setTipoDettaglioAllegato(tipoDettaglioAllegato);
                            allegatoInformation.setMetadatiVersati(objectMapper.writeValueAsString(docFileAttributes));
                            
//                            String hash = is.submitDocument(docID, docFileAttributes, data);
                            allegatoInformation.setRapporto(null);
                            allegatoInformation.setStatoVersamento(Versamento.StatoVersamento.VERSATO);     
                        } else {
                            log.error("file stream is null");
                        }
                    } catch (ConnectException ex) {
                        log.error(ex.getMessage());
                        allegatoInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE_RITENTABILE);
                        allegatoInformation.setDescrizioneErrore(ex.getMessage());
                        errorOccured = true;
                    } catch (ServerSOAPFaultException | MinIOWrapperException | IOException ex) {
                        log.error(ex.getMessage());
                        allegatoInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
                        allegatoInformation.setDescrizioneErrore(ex.getMessage());
                        errorOccured = true;
                    } finally {
                        versamentiAllegatiInfo.add(allegatoInformation);
                        allAttributesMerged.put(tipoDettaglioAllegato.name().toLowerCase(), fileAttributes);
                    }
                }                
            }
            versamentoDocInformation.setVeramentiAllegatiInformations(versamentiAllegatiInfo);
            versamentoDocInformation.setMetadatiVersati(objectMapper.writeValueAsString(allAttributesMerged));
            
            if (errorOccured) {
                throw new VersatoreServiceException("Versamenti di allegati in errore");
            }
            
            versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.VERSATO);
        } catch (MalformedURLException | VersatoreServiceException ex) {
            log.error(ex.getMessage());
            versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
            versamentoDocInformation.setDescrizioneErrore(ex.getMessage());
        } catch (JsonProcessingException ex) {
            log.error("Errore nel parsing dei metadati versati");
        }
        
        return versamentoDocInformation;
    }

    public InfocertVersatoreService addNewAttribute(List<DocumentAttribute> fileAttributes, final InfocertAttributesEnum name, final String value) {
        return addNewAttribute(fileAttributes, name, null, value);
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
            log.error(String.format("Attori %s not found", ruolo));
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
     * Restituisce la concatenazione dei destinatari del documento detail
     * passato.
     *
     * @param docDetail Il documento.
     * @return La stringa concatenata dei destinatari separati da ','.
     */
    private String getDestinatari(DocDetail docDetail) {
        List<String> destinatari = new ArrayList();
        docDetail.getDestinatari().stream().forEach(d -> destinatari.add(d.getNome() != null ? d.getNome() : d.getIndirizzo()));

        return String.join(",", destinatari);
    }
}
