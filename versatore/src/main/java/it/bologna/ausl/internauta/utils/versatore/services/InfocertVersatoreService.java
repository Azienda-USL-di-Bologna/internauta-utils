package it.bologna.ausl.internauta.utils.versatore.services;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.versatore.VersamentoInformation;
import it.bologna.ausl.internauta.utils.versatore.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.enums.AttributesEnum;
import it.bologna.ausl.internauta.utils.versatore.utils.VersatoreConfigParams;
import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.AttoreDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.QAllegato;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QAttoreDoc;
import it.bologna.ausl.model.entities.scripta.QRegistro;
import it.bologna.ausl.model.entities.scripta.QRegistroDoc;
import it.bologna.ausl.model.entities.scripta.QTitolo;
import it.bologna.ausl.model.entities.scripta.RegistroDoc;
import it.bologna.ausl.model.entities.scripta.Titolo;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import it.bologna.ausl.utils.versatore.infocert.wsclient.DocumentAttribute;
import it.bologna.ausl.utils.versatore.infocert.wsclient.GenericDocument;
import it.bologna.ausl.utils.versatore.infocert.wsclient.GenericDocumentService;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.activation.DataHandler;
import javax.xml.ws.BindingProvider;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class InfocertVersatoreService extends VersatoreDocs {

    private static final Logger log = LoggerFactory.getLogger(InfocertVersatoreService.class);
    private static final String INFOCERT_VERSATORE_SERVICE = "InfocertVersatoreService";

    private final String infocertVersatoreServiceEndPointUri;

    public InfocertVersatoreService(EntityManager entityManager, VersatoreConfigParams versatoreConfigParams, VersatoreConfiguration configuration) {
        super(entityManager, versatoreConfigParams, configuration);

        Map<String, Object> versatoreConfiguration = configuration.getParams();
        Map<String, Object> infocertServiceConfiguration = (Map<String, Object>) versatoreConfiguration.get(INFOCERT_VERSATORE_SERVICE);
        infocertVersatoreServiceEndPointUri = infocertServiceConfiguration.get("InfocertVersatoreServiceEndPointUri").toString();
        log.info(String.format("URI: %s", infocertVersatoreServiceEndPointUri));
    }

    @Override
    public VersamentoInformation versa(VersamentoInformation versamentoInformation) {

        Integer idDoc = versamentoInformation.getIdDoc();

        Doc doc = entityManager.find(Doc.class, idDoc);
        DocDetail docDetail = entityManager.find(DocDetail.class, idDoc);
        Struttura strutturaRegistrazione = docDetail.getIdStrutturaRegistrazione();

        List<DocumentAttribute> attributi = new ArrayList();
        addNewAttribute(attributi, AttributesEnum.IDENTIFICATIVO_DOCUMENTO, docDetail.getId().toString())
                .addNewAttribute(attributi, AttributesEnum.MODALITA_DI_FORMAZIONE, "b")
                .addNewAttribute(attributi, AttributesEnum.TIPOLOGIA_DOCUMENTALE, "Protocolli") // BOOOOH
                .addNewAttribute(attributi, AttributesEnum.DATA_REGISTRAZIONE, docDetail.getDataRegistrazione().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .addNewAttribute(attributi, AttributesEnum.TIPO_REGISTRO, "Registro_PG") // Protocollo emergenza??
                .addNewAttribute(attributi, AttributesEnum.CODICE_REGISTRO, getNumeroRegistro(doc)) // Protocollo emergenza??
                .addNewAttribute(attributi, AttributesEnum.NUMERO_DOCUMENTO,
                        String.join("/", docDetail.getNumeroRegistrazione().toString(), docDetail.getAnnoRegistrazione().toString()))
                .addNewAttribute(attributi, AttributesEnum.OGGETTO, docDetail.getOggetto())
                .addNewAttribute(attributi, AttributesEnum.RUOLO_N, 1, "Amministrazione che effettua la registrazione")
                .addNewAttribute(attributi, AttributesEnum.TIPO_SOGGETTO_N, 1, "PAI")
                .addNewAttribute(attributi, AttributesEnum.DENOMINAZIONE_N, 1, strutturaRegistrazione.getNome())
                .addNewAttribute(attributi, AttributesEnum.RUOLO_N, 2, "assegnatario")
                .addNewAttribute(attributi, AttributesEnum.TIPO_SOGGETTO_N, 2, "PAI")
                .addNewAttribute(attributi, AttributesEnum.DENOMINAZIONE_N, 2, "//TODO")
                .addNewAttribute(attributi, AttributesEnum.CLASSIFICAZIONE, getClassificazioni(doc))
                .addNewAttribute(attributi, AttributesEnum.RISERVATO, docDetail.getRiservato().toString())
                .addNewAttribute(attributi, AttributesEnum.VERSIONE_DEL_DOCUMENTO, "booh")
                .addNewAttribute(attributi, AttributesEnum.FASCICOLO, getArchivi(doc))
                .addNewAttribute(attributi, AttributesEnum.TEMPO_DI_CONSERVAZIONE, "999") //TODO: Da chiarire
                .addNewAttribute(attributi, AttributesEnum.SEGNATURA, "b"); //TODO: Da chiarire

        //TODO: Questa parte è tutta da rivedere
        switch (docDetail.getTipologia()) {
            case PROTOCOLLO_IN_USCITA:
                addNewAttribute(attributi, AttributesEnum.DATA_DOCUMENTO, docDetail.getDataRegistrazione().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        .addNewAttribute(attributi, AttributesEnum.TIPOLOGIA_DI_FLUSSO, "U")
                        .addNewAttribute(attributi, AttributesEnum.NATURA, "PROTOCOLLO")
                        .addNewAttribute(attributi, AttributesEnum.RUOLO_N, 3, "mittente")
                        .addNewAttribute(attributi, AttributesEnum.TIPO_SOGGETTO_N, 3, "PAI")
                        .addNewAttribute(attributi, AttributesEnum.FIRMATARIO, getAttoriDoc(doc, "FIRMA"));
                String pareratori = getAttoriDoc(doc, "PARERI");
                if (pareratori != null) {
                    addNewAttribute(attributi, AttributesEnum.PARERE, pareratori);
                }
                // Se il PU ha il campo mittente è in smistamento
                if (docDetail.getMittente() == null || !StringUtils.hasLength(docDetail.getMittente())) {
                   addNewAttribute(attributi, AttributesEnum.DENOMINAZIONE_N, 3, doc.getIdAzienda().getDescrizione()); // Codice IPA?
                } else {
                    addNewAttribute(attributi, AttributesEnum.DENOMINAZIONE_N, 3, docDetail.getMittente()) // Codice IPA?
                            .addNewAttribute(attributi, AttributesEnum.RUOLO_N, 4, "redattore")
                            .addNewAttribute(attributi, AttributesEnum.TIPO_SOGGETTO_N, 4, "PF")
                            .addNewAttribute(attributi, AttributesEnum.COGNOME_N, 4, docDetail.getIdPersonaRedattrice().getCognome())
                            .addNewAttribute(attributi, AttributesEnum.NOME_N, 4, docDetail.getIdPersonaRedattrice().getNome())
                            .addNewAttribute(attributi, AttributesEnum.RUOLO_N, 5, "destinatario")
                            .addNewAttribute(attributi, AttributesEnum.TIPO_SOGGETTO_N, 5, "PG")
                            .addNewAttribute(attributi, AttributesEnum.DENOMINAZIONE_N, 5, getDestinatari(docDetail))
                            .addNewAttribute(attributi, AttributesEnum.RUOLO_N, 6, "responsabile procedimento")
                            .addNewAttribute(attributi, AttributesEnum.TIPO_SOGGETTO_N, 6, "PF")
                            .addNewAttribute(attributi, AttributesEnum.COGNOME_N, 6, docDetail.getIdPersonaResponsabileProcedimento().getCognome())
                            .addNewAttribute(attributi, AttributesEnum.NOME_N, 6, docDetail.getIdPersonaResponsabileProcedimento().getNome())
                            .addNewAttribute(attributi, AttributesEnum.RUOLO_N, 7, "direttore uo mittente")
                            .addNewAttribute(attributi, AttributesEnum.TIPO_SOGGETTO_N, 7, "PF")
                            .addNewAttribute(attributi, AttributesEnum.COGNOME_N, 7, "Direttore uo")
                            .addNewAttribute(attributi, AttributesEnum.NOME_N, 7, "Direttore uo");
                }
                break;

            case PROTOCOLLO_IN_ENTRATA:
                addNewAttribute(attributi, AttributesEnum.TIPOLOGIA_DI_FLUSSO, "E")
                        .addNewAttribute(attributi, AttributesEnum.RUOLO_N, 3, "mittente")
                        .addNewAttribute(attributi, AttributesEnum.TIPO_SOGGETTO_N, 3, "PAI")
                        .addNewAttribute(attributi, AttributesEnum.DENOMINAZIONE_N, 3, docDetail.getIdStrutturaRegistrazione().getNome());
                
                break;
        }

        List<Allegato> allegati = doc.getAllegati();
        List<DocumentAttribute> fileAttributes;
        GenericDocumentService iss;
        for (Allegato allegato : allegati) {
            try {
                fileAttributes = new ArrayList();

                addNewAttribute(attributi, AttributesEnum.IDENTIFICATIVO_DEL_FORMATO, "application/pdf")
                        //TODO: Gestire le tipologie di allegati. TESTO
                        .addNewAttribute(attributi, AttributesEnum.NOME_FILE, allegato.getDettagli().getOriginaleFirmato().getNome());

                addNewAttribute(fileAttributes, AttributesEnum.IMPRONTA, allegato.getDettagli().getOriginaleFirmato().getHashMd5());
                addNewAttribute(fileAttributes, AttributesEnum.ALGORITMO, "md5");

                addNewAttribute(fileAttributes, AttributesEnum.ALLEGATI_NUMERO, "b");
                addNewAttribute(fileAttributes, AttributesEnum.ID_DOC_INDICE_ALLEGATI, "b");
                addNewAttribute(fileAttributes, AttributesEnum.DESCRIZIONE_ALLEGATI, "b");

                String docID = doc.getId().toString();

                iss = new GenericDocumentService(new URL(infocertVersatoreServiceEndPointUri));

                GenericDocument is = iss.getGenericDocumentPort();
                BindingProvider bp = (BindingProvider) is;
                bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        infocertVersatoreServiceEndPointUri);
//            DataHandler data = new DataHandler(new FileDataSource(arg[1]));
                DataHandler data = null;
//            is.submitDocument(docID, fileAttributes, data);

            } catch (MalformedURLException e) {
                log.error(e.getMessage());
            }

        }

        return versamentoInformation;
    }
    
    public InfocertVersatoreService addNewAttribute(List<DocumentAttribute> fileAttributes, final AttributesEnum name, final String value) {
        return addNewAttribute(fileAttributes, name, null, value);
    } 

    public InfocertVersatoreService addNewAttribute(List<DocumentAttribute> fileAttributes, 
            final AttributesEnum name, final Integer index, final String value) {
        DocumentAttribute attr = new DocumentAttribute();
        attr.setName(index != null ? name.toString().replace("x", index.toString()) : name.toString());
        attr.setValue(value);
        fileAttributes.add(attr);
        return this;
    }

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

    private String getAttoriDoc(final Doc doc, String ruolo) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QAttoreDoc qAttoreDoc = QAttoreDoc.attoreDoc;

        List<AttoreDoc> attoriDoc = queryFactory
                .select(qAttoreDoc)
                .from(qAttoreDoc)
                .where(qAttoreDoc.idDoc.eq(doc).and(qAttoreDoc.ruolo.eq(ruolo)))
                .fetch();
        if (attoriDoc.isEmpty()) {
            return null;
        }
        List<String> attori = new ArrayList();
        attoriDoc.stream().forEach(a -> attori.add(a.getIdPersona().getDescrizione()));
        return String.join(",", attori);
    }

    /**
     * Restituisce la concatenazione delle classificazioni del documento passato.
     *
     * @param doc Il Documento.
     * @return La stringa concatenata nel formato [CODICE] NOME separate da ",".
     */
    private String getClassificazioni(final Doc doc) {

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QTitolo qTitoli = QTitolo.titolo;
        QArchivioDoc qArchivioDoc = QArchivioDoc.archivioDoc;
        QArchivio qArchivio = QArchivio.archivio;

        List<Titolo> titoli = queryFactory
                .select(qTitoli)
                .from(qArchivioDoc)
                .innerJoin(qArchivioDoc.idArchivio, qArchivio)
                .innerJoin(qArchivio.idTitolo, qTitoli)
                .where(qArchivioDoc.idDoc.eq(doc))
                .fetch();

        if (titoli.isEmpty()) {
            //TODO: Handle exception
            log.error("Titoli not found");
        }
        List<String> classificazioni = new ArrayList();
        titoli.stream().forEach(t -> classificazioni.add(String.format("[%s] %s", t.getClassificazione(), t.getNome().replace(",", ""))));

        return String.join(",", classificazioni);
    }

    /**
     * Restituisce la concatenazione degli archivi del documento passato.
     *
     * @param doc Il Documento.
     * @return La stringa concatenata nel formato [CODICE] NOME, separate da
     * ",".
     */
    private String getArchivi(final Doc doc) {

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QArchivioDoc qArchivioDoc = QArchivioDoc.archivioDoc;
        QArchivio qArchivio = QArchivio.archivio;

        List<Archivio> archivi = queryFactory
                .select(qArchivio)
                .from(qArchivioDoc)
                .innerJoin(qArchivioDoc.idArchivio, qArchivio)
                .where(qArchivioDoc.idDoc.eq(doc))
                .fetch();

        if (archivi.isEmpty()) {
            //TODO: Handle exception
            log.error("Archivi not found");
        }
        List<String> archiviazioni = new ArrayList();
        archivi.stream().forEach(t -> archiviazioni.add(String.format("[%s] %s", t.getNumerazioneGerarchica(), t.getOggetto().replace(",", " "))));

        return String.join(",", archiviazioni);
    }
    
    /**
     * Restituisce la concatenazione dei destinatari del documento detail passato.
     * @param docDetail Il documento.
     * @return La stringa concatenata dei destinatari separati da ','.
     */
    private String getDestinatari(DocDetail docDetail) {
        List<String> destinatari = new ArrayList();
        docDetail.getDestinatari().stream().forEach(d -> destinatari.add(d.getNome() != null ? d.getNome() : d.getIndirizzo()));
        
        return String.join(",", destinatari);
    }
}
