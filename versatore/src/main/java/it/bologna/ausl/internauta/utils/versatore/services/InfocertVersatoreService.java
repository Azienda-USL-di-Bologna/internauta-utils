package it.bologna.ausl.internauta.utils.versatore.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.enums.InfocertAttributesEnum;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreConfigurationException;
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
       
        List<DocumentAttribute> attributi = new ArrayList();
        addNewAttribute(attributi, InfocertAttributesEnum.IDENTIFICATIVO_DOCUMENTO, docDetail.getId().toString())
                .addNewAttribute(attributi, InfocertAttributesEnum.MODALITA_DI_FORMAZIONE, "b")
                .addNewAttribute(attributi, InfocertAttributesEnum.TIPOLOGIA_DOCUMENTALE, "Protocolli") // BOOOOH
                .addNewAttribute(attributi, InfocertAttributesEnum.DATA_REGISTRAZIONE, docDetail.getDataRegistrazione().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .addNewAttribute(attributi, InfocertAttributesEnum.TIPO_REGISTRO, "Registro_PG") // Protocollo emergenza??
                .addNewAttribute(attributi, InfocertAttributesEnum.CODICE_REGISTRO, getNumeroRegistro(doc)) // Protocollo emergenza??
                .addNewAttribute(attributi, InfocertAttributesEnum.NUMERO_DOCUMENTO,
                        String.join("/", docDetail.getNumeroRegistrazione().toString(), docDetail.getAnnoRegistrazione().toString()))
                .addNewAttribute(attributi, InfocertAttributesEnum.OGGETTO, docDetail.getOggetto())
                .addNewAttribute(attributi, InfocertAttributesEnum.RUOLO, "produttore")
                .addNewAttribute(attributi, InfocertAttributesEnum.TIPO_SOGGETTO, "PAI")
                .addNewAttribute(attributi, InfocertAttributesEnum.DENOMINAZIONE, strutturaRegistrazione.getNome())
                .addNewAttribute(attributi, InfocertAttributesEnum.RUOLO_N, 2, "assegnatario")
                .addNewAttribute(attributi, InfocertAttributesEnum.TIPO_SOGGETTO_N, 2, "PAI")
                .addNewAttribute(attributi, InfocertAttributesEnum.DENOMINAZIONE_N, 2, "//TODO")
                .addNewAttribute(attributi, InfocertAttributesEnum.RISERVATO, docDetail.getRiservato().toString())
                .addNewAttribute(attributi, InfocertAttributesEnum.VERSIONE_DEL_DOCUMENTO, "1")
                .addNewAttribute(attributi, InfocertAttributesEnum.ID_AGGREGAZIONE, archivio.getNumerazioneGerarchica())
//                .addNewAttribute(attributi, InfocertAttributesEnum.TEMPO_DI_CONSERVAZIONE, archivio.getAnniTenuta().toString())
                .addNewAttribute(attributi, InfocertAttributesEnum.SEGNATURA, "b"); //TODO: Da chiarire

         Titolo titolo = getTitolo(doc, archivio);
         if (titolo != null) {
            addNewAttribute(attributi, InfocertAttributesEnum.INDICE_DI_CLASSIFICAZIONE, titolo.getClassificazione())
                    .addNewAttribute(attributi, InfocertAttributesEnum.DESCRIZIONE_CLASSIFICAZIONE, titolo.getNome());
         }
        
        //TODO: Questa parte è tutta da rivedere
        switch (docDetail.getTipologia()) {
            case PROTOCOLLO_IN_USCITA:
                addNewAttribute(attributi, InfocertAttributesEnum.DATA_DOCUMENTO, docDetail.getDataRegistrazione().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        .addNewAttribute(attributi, InfocertAttributesEnum.TIPOLOGIA_DI_FLUSSO, "U")
                        .addNewAttribute(attributi, InfocertAttributesEnum.NATURA, "PROTOCOLLO")
                        .addNewAttribute(attributi, InfocertAttributesEnum.RUOLO_N, 3, "mittente")
                        .addNewAttribute(attributi, InfocertAttributesEnum.TIPO_SOGGETTO_N, 3, "PAI")
                        .addNewAttribute(attributi, InfocertAttributesEnum.FIRMATARIO, getAttoriDoc(doc, "FIRMA"));
                String pareratori = getAttoriDoc(doc, "PARERI");
                if (pareratori != null) {
                    addNewAttribute(attributi, InfocertAttributesEnum.PARERE, pareratori);
                }
                // Se il PU ha il campo mittente è in smistamento
                if (docDetail.getMittente() == null || !StringUtils.hasLength(docDetail.getMittente())) {
                    addNewAttribute(attributi, InfocertAttributesEnum.DENOMINAZIONE_N, 3, doc.getIdAzienda().getDescrizione()); // Codice IPA?
                } else {
                    addNewAttribute(attributi, InfocertAttributesEnum.DENOMINAZIONE_N, 3, docDetail.getMittente()) // Codice IPA?
                            .addNewAttribute(attributi, InfocertAttributesEnum.RUOLO_N, 4, "redattore")
                            .addNewAttribute(attributi, InfocertAttributesEnum.TIPO_SOGGETTO_N, 4, "PF")
                            .addNewAttribute(attributi, InfocertAttributesEnum.COGNOME_N, 4, docDetail.getIdPersonaRedattrice().getCognome())
                            .addNewAttribute(attributi, InfocertAttributesEnum.NOME_N, 4, docDetail.getIdPersonaRedattrice().getNome())
                            .addNewAttribute(attributi, InfocertAttributesEnum.RUOLO_N, 5, "destinatario")
                            .addNewAttribute(attributi, InfocertAttributesEnum.TIPO_SOGGETTO_N, 5, "PG")
                            .addNewAttribute(attributi, InfocertAttributesEnum.DENOMINAZIONE_N, 5, getDestinatari(docDetail))
                            .addNewAttribute(attributi, InfocertAttributesEnum.RUOLO_N, 6, "responsabile procedimento")
                            .addNewAttribute(attributi, InfocertAttributesEnum.TIPO_SOGGETTO_N, 6, "PF")
                            .addNewAttribute(attributi, InfocertAttributesEnum.COGNOME_N, 6, docDetail.getIdPersonaResponsabileProcedimento().getCognome())
                            .addNewAttribute(attributi, InfocertAttributesEnum.NOME_N, 6, docDetail.getIdPersonaResponsabileProcedimento().getNome())
                            .addNewAttribute(attributi, InfocertAttributesEnum.RUOLO_N, 7, "direttore uo mittente")
                            .addNewAttribute(attributi, InfocertAttributesEnum.TIPO_SOGGETTO_N, 7, "PF")
                            .addNewAttribute(attributi, InfocertAttributesEnum.COGNOME_N, 7, "Direttore uo")
                            .addNewAttribute(attributi, InfocertAttributesEnum.NOME_N, 7, "Direttore uo");
                }
                break;

            case PROTOCOLLO_IN_ENTRATA:
                addNewAttribute(attributi, InfocertAttributesEnum.TIPOLOGIA_DI_FLUSSO, "E")
                        .addNewAttribute(attributi, InfocertAttributesEnum.RUOLO_N, 3, "mittente")
                        .addNewAttribute(attributi, InfocertAttributesEnum.TIPO_SOGGETTO_N, 3, "PAI")
                        .addNewAttribute(attributi, InfocertAttributesEnum.DENOMINAZIONE_N, 3, docDetail.getIdStrutturaRegistrazione().getNome());

                break;
        }

        try {
            GenericDocumentService iss = new GenericDocumentService(new URL(infocertVersatoreServiceEndPointUri));

            GenericDocument is = iss.getGenericDocumentPort();
            BindingProvider bp = (BindingProvider) is;
            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                    infocertVersatoreServiceEndPointUri);
            
            MinIOWrapper minIOWrapper = versatoreRepositoryConfiguration.getVersatoreRepositoryManager().getMinIOWrapper();
            
            ObjectMapper objectMapper = new ObjectMapper();
            
            List<Allegato> allegati = doc.getAllegati();
            List<DocumentAttribute> fileAttributes;
            List<DocumentAttribute> mergedAttributes;
            List<VersamentoAllegatoInformation> versamentiAllegatiInfo = new ArrayList();
            int i = 1;
            
            for (Allegato allegato: allegati) {
                for (Allegato.DettagliAllegato.TipoDettaglioAllegato tipoDettaglioAllegato : Allegato.DettagliAllegato.TipoDettaglioAllegato.values()) {
                    Allegato.DettaglioAllegato dettaglioAllegato = allegato.getDettagli().getByKey(tipoDettaglioAllegato);
                    
                    if (dettaglioAllegato == null) continue;
           
                    fileAttributes = new ArrayList();
                    String mimeType = "application/pdf";
                    if (dettaglioAllegato.getMimeType() != null) {
                        mimeType = dettaglioAllegato.getMimeType();
                    }

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
                            mergedAttributes = new ArrayList();
                            mergedAttributes.addAll(attributi);
                            mergedAttributes.addAll(fileAttributes);
                            
                            VersamentoAllegatoInformation allegatoInformation = new VersamentoAllegatoInformation();
                            allegatoInformation.setIdAllegato(allegato.getId());
                            allegatoInformation.setDataVersamento(ZonedDateTime.now());
                            allegatoInformation.setTipoDettaglioAllegato(tipoDettaglioAllegato);
                            allegatoInformation.setMetadatiVersati(objectMapper.writeValueAsString(mergedAttributes));
                            
                            String hash = is.submitDocument(docID, mergedAttributes, data);
                            allegatoInformation.setRapporto(hash);
                            allegatoInformation.setStatoVersamento(Versamento.StatoVersamento.VERSATO);
                            versamentiAllegatiInfo.add(allegatoInformation);
                        } else {
                            log.error("file stream is null");
                        }
                    } catch (MinIOWrapperException | IOException ex) {
                        log.error(ex.getMessage());
                    }
                }                
            }
            versamentoDocInformation.setVeramentiAllegatiInformations(versamentiAllegatiInfo);
        } catch (MalformedURLException e) {
            log.error(e.getMessage());
            throw new VersatoreConfigurationException(e.getMessage());
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
     * Restituisce la concatenazione degli attori del doc filtrati per il ruolo passato.
     * @param doc Il documento.
     * @param ruolo Il ruolo.
     * @return La stringa concatenata degli attori, descrizione (nome e cognome) separati da ','.
     */
    private String getAttoriDoc(final Doc doc, final String ruolo) {
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
        List<String> attori = new ArrayList();
        attoriDoc.stream().forEach(a -> attori.add(a.getIdPersona().getDescrizione()));
        return String.join(",", attori);
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
