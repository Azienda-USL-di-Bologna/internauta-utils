package it.bologna.ausl.internauta.utils.versatore.services;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.versatore.VersamentoInformation;
import it.bologna.ausl.internauta.utils.versatore.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.enums.AttributesEnum;
import it.bologna.ausl.internauta.utils.versatore.utils.VersatoreConfigParams;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.QAllegato;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QTitolo;
import it.bologna.ausl.model.entities.scripta.Titolo;
import it.bologna.ausl.model.entities.versatore.Configuration;
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

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class InfocertVersatoreService extends VersatoreDocs {

    private static final Logger log = LoggerFactory.getLogger(InfocertVersatoreService.class);
    private static final String INFOCERT_VERSATORE_SERVICE = "InfocertVersatoreService";

    private final String infocertVersatoreServiceEndPointUri;
    private List<DocumentAttribute> attributes;

    public InfocertVersatoreService(EntityManager entityManager, VersatoreConfigParams versatoreConfigParams, Configuration configuration) {
        super(entityManager, versatoreConfigParams, configuration);

        Map<String, Object> firmaRemotaConfiguration = configuration.getParams();
        Map<String, Object> infocertServiceConfiguration = (Map<String, Object>) firmaRemotaConfiguration.get(INFOCERT_VERSATORE_SERVICE);
        infocertVersatoreServiceEndPointUri = infocertServiceConfiguration.get("InfocertVersatoreServiceEndPointUri").toString();
        log.info(String.format("URI: %s", infocertVersatoreServiceEndPointUri));
    }

    @Override
    public VersamentoInformation versa(VersamentoInformation versamentoInformation) {

        Integer idDoc = versamentoInformation.getIdDoc();

        Doc doc = entityManager.find(Doc.class, idDoc);
        DocDetail docDetail = entityManager.find(DocDetail.class, idDoc);
        Struttura strutturaRegistrazione = entityManager.find(Struttura.class, docDetail.getIdStrutturaRegistrazione().getId());
        
        List<Allegato> allegati = getAllegati(doc);
//        Allegato testo = allegati.stream().filter(a -> a.getTipo().equals(Allegato.TipoAllegato.TESTO)).findFirst().get();
        GenericDocumentService iss;
        for (Allegato allegato : allegati) {
            try {
                iss = new GenericDocumentService(new URL(infocertVersatoreServiceEndPointUri));

                GenericDocument is = iss.getGenericDocumentPort();
                BindingProvider bp = (BindingProvider) is;
                bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        infocertVersatoreServiceEndPointUri);
                attributes = new ArrayList();

                addNewAttribute(AttributesEnum.IDENTIFICATIVO_DOCUMENTO, docDetail.getId().toString());
                addNewAttribute(AttributesEnum.MODALITA_DI_FORMAZIONE, "b");
                addNewAttribute(AttributesEnum.DATA_REGISTRAZIONE, docDetail.getDataRegistrazione().format(DateTimeFormatter.BASIC_ISO_DATE));
                addNewAttribute(AttributesEnum.NUMERO_DOCUMENTO,
                        String.join("/", docDetail.getNumeroRegistrazione().toString(), docDetail.getAnnoRegistrazione().toString()));
                addNewAttribute(AttributesEnum.OGGETTO, docDetail.getOggetto());
                addNewAttribute(AttributesEnum.RUOLO_1, "Amministrazione che effettua la registrazione");
                addNewAttribute(AttributesEnum.TIPO_SOGGETTO_1, "PAI");
                addNewAttribute(AttributesEnum.DENOMINAZIONE_1, strutturaRegistrazione.getNome());
                addNewAttribute(AttributesEnum.RUOLO_2, "assegnatario");
                addNewAttribute(AttributesEnum.TIPO_SOGGETTO_2, "PAI");
                addNewAttribute(AttributesEnum.DENOMINAZIONE_2, "//TODO");

                addNewAttribute(AttributesEnum.CLASSIFICAZIONE, getClassificazioni(doc));
                addNewAttribute(AttributesEnum.RISERVATO, docDetail.getRiservato().toString());
                addNewAttribute(AttributesEnum.VERSIONE_DEL_DOCUMENTO, "booh");
                addNewAttribute(AttributesEnum.FASCICOLO, getArchivi(doc));    
                
                addNewAttribute(AttributesEnum.TEMPO_DI_CONSERVAZIONE, "b"); //TODO: Da chiarire
                addNewAttribute(AttributesEnum.SEGNATURA, "b"); //TODO: Da chiarire
                
                addNewAttribute(AttributesEnum.IDENTIFICATIVO_DEL_FORMATO, "application/pdf");
                //TODO: Gestire le tipologie di allegati. TESTO
                addNewAttribute(AttributesEnum.NOME_FILE, allegato.getDettagli().getOriginaleFirmato().getNome());

                addNewAttribute(AttributesEnum.DATA_DOCUMENTO, docDetail.getDataRegistrazione().format(DateTimeFormatter.BASIC_ISO_DATE));
                addNewAttribute(AttributesEnum.IMPRONTA, allegato.getDettagli().getOriginaleFirmato().getHashMd5());
                addNewAttribute(AttributesEnum.ALGORITMO, "md5");

                //TODO: Questa parte è tutta da rivedere
                switch (docDetail.getTipologia()) {
                    case PROTOCOLLO_IN_ENTRATA:
                        addNewAttribute(AttributesEnum.TIPOLOGIA_DOCUMENTALE, "Protocollo");
                        addNewAttribute(AttributesEnum.TIPOLOGIA_DI_FLUSSO, "E");
                        addNewAttribute(AttributesEnum.TIPO_REGISTRO, docDetail.getRiservato() ? "Protocollo Riservato" : "Protocollo Ordinario");
                        break;
                    case PROTOCOLLO_IN_USCITA:
                        addNewAttribute(AttributesEnum.TIPOLOGIA_DOCUMENTALE, "Protocollo");
                        addNewAttribute(AttributesEnum.TIPOLOGIA_DI_FLUSSO, "U");
                        addNewAttribute(AttributesEnum.TIPO_REGISTRO, docDetail.getRiservato() ? "Protocollo Riservato" : "Protocollo Ordinario");
                        break;
                    case RGPICO:
                        addNewAttribute(AttributesEnum.TIPOLOGIA_DOCUMENTALE, "Registro");
                        addNewAttribute(AttributesEnum.TIPOLOGIA_DI_FLUSSO, "I");
                        addNewAttribute(AttributesEnum.TIPO_REGISTRO, "Registro");
                        break;
                    case DELIBERA:
                    case DETERMINA:
                        addNewAttribute(AttributesEnum.TIPOLOGIA_DOCUMENTALE, docDetail.getTipologia().toString());
                        addNewAttribute(AttributesEnum.TIPOLOGIA_DI_FLUSSO, "U");
                        addNewAttribute(AttributesEnum.TIPO_REGISTRO, "Registro");
                        break;
                    default:
                        throw new AssertionError();
                }
                addNewAttribute(AttributesEnum.CODICE_REGISTRO, "boooooooooooh"); // Optional

                addNewAttribute(AttributesEnum.ALLEGATI_NUMERO, "b");
                addNewAttribute(AttributesEnum.ID_DOC_INDICE_ALLEGATI, "b");
                addNewAttribute(AttributesEnum.DESCRIZIONE_ALLEGATI, "b");

                String docID = "0123456";

//            DataHandler data = new DataHandler(new FileDataSource(arg[1]));
                DataHandler data = null;
//            is.submitDocument(docID, attributes, data);

            } catch (MalformedURLException e) {
                log.error(e.getMessage());
            }

        }

        return versamentoInformation;
    }

    private void addNewAttribute(final AttributesEnum name, final String value) {
        DocumentAttribute attr = new DocumentAttribute();
        attr.setName(name.toString());
        attr.setValue(value);
        attributes.add(attr);
    }
    
    private List<Allegato> getAllegati(final Doc doc) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QAllegato qAllegato = QAllegato.allegato;
        
        final List<Allegato> allegati = queryFactory
                .select(qAllegato)
                .from(qAllegato)
                .where(qAllegato.idDoc.eq(doc))
                .fetch();
        
        return allegati;
    }

    /**
     * Restituisce la concatenazione delle classificazioni del documento passato.
     * @param doc Il Documento.
     * @return La stringa concatenata nel formato [CODICE] NOME, separate da ",".
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
        List<String> classificazioni = new ArrayList<>();
        titoli.stream().forEach(t -> classificazioni.add(String.format("[%s] %s", t.getClassificazione(), t.getNome().replace(",", ""))));
        
        return String.join(",", classificazioni);
    }

    /**
     * Restituisce la concatenazione degli archivi del documento passato.
     * @param doc Il Documento.
     * @return La stringa concatenata nel formato [CODICE] NOME, separate da ",".
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
        List<String> archiviazioni = new ArrayList<>();
        archivi.stream().forEach(t -> archiviazioni.add(String.format("[%s] %s", t.getNumerazioneGerarchica(), t.getOggetto().replace(",", " "))));
        
        return String.join(",", archiviazioni);
    }
}
