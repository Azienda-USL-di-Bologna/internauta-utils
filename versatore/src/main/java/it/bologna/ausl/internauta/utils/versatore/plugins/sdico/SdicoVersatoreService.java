package it.bologna.ausl.internauta.utils.versatore.plugins.sdico;

import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders.SdicoResponse;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders.VersamentoBuilder;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders.RgPicoBuilder;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders.PicoBuilder;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders.DocumentoGEDIBuilder;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders.DeteBuilder;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders.DeliBuilder;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders.AllegatiBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.model.entities.scripta.Registro;
import it.bologna.ausl.model.entities.scripta.RegistroDoc;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreSdicoException;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QDocDetail;
import it.bologna.ausl.model.entities.versatore.QVersamento;
import it.bologna.ausl.riversamento.builder.IdentityFile;
import it.bologna.ausl.riversamento.sender.PaccoFile;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Andrea
 */
@Component
public class SdicoVersatoreService extends VersatoreDocs {

    private static final Logger log = LoggerFactory.getLogger(SdicoVersatoreService.class);
    private static final String SDICO_VERSATORE_SERVICE = "SdicoVersatoreService";
    private static final String WS_OK = "WS_OK";
    private static final String CANCELLATO = "CANCELLATO";
    private static final String ERRORE_PLUG_IN = "ERRORE_PLUG_IN";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private String sdicoLoginURI, sdicoServizioVersamentoURI;

    @Autowired
    VersatoreHttpClientConfiguration versatoreHttpClientConfiguration;

    @Override
    public void init(VersatoreConfiguration versatoreConfiguration) {
        super.init(versatoreConfiguration);
        Map<String, Object> versatoreConfigurationMap = this.versatoreConfiguration.getParams();
        Map<String, Object> sdicoServiceConfiguration = (Map<String, Object>) versatoreConfigurationMap.get(SDICO_VERSATORE_SERVICE);
        sdicoLoginURI = sdicoServiceConfiguration.get("SdicoLoginURI").toString();
        sdicoServizioVersamentoURI = sdicoServiceConfiguration.get("SdicoServizioVersamentoURI").toString();
        log.info("SDICO login URI: {}", sdicoLoginURI);
        log.info("SDICO servizio versamento URI: {}", sdicoServizioVersamentoURI);
    }

    @Override
    public VersamentoDocInformation versaImpl(VersamentoDocInformation versamentoDocInformation) throws VersatoreProcessingException {

        Map<String, Object> mappaResultAndAllegati = new HashMap<>();
        //Reperisoco i risultati del versamento
        mappaResultAndAllegati = versaDocumentoSDICO(versamentoDocInformation);
        SdicoResponse response = (SdicoResponse) mappaResultAndAllegati.get("response");
        String responseJson = (String) mappaResultAndAllegati.get("responseJson");
        String xmlVersato = (String) mappaResultAndAllegati.get("xmlVersato");
        List<VersamentoAllegatoInformation> versamentiAllegatiInformationList = (List<VersamentoAllegatoInformation>) mappaResultAndAllegati.get("versamentiAllegatiInformation");

        //Imposto i dati del DocInformation con i risultati
        versamentoDocInformation.setMetadatiVersati(xmlVersato);
        versamentoDocInformation.setDataVersamento(ZonedDateTime.now());
        if (response != null && !response.equals("")) {
            switch (response.getResponseCode()) {
                case CANCELLATO: {
                    versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.ANNULLATO);
                    versamentoDocInformation.setRapporto(response.getErrorMessage());
                    log.warn("Il versamento del documento " + versamentoDocInformation.getIdDoc() + " è stato annullato, in quanto: " + response.getErrorMessage());
                    break;
                }
                case WS_OK: {
                    versamentoDocInformation.setRapporto(responseJson);
                    versamentoDocInformation.setStatoVersamentoPrecedente(versamentoDocInformation.getStatoVersamento());
                    versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.VERSATO);
                    for (VersamentoAllegatoInformation versamentoAllegatoInformation : versamentiAllegatiInformationList) {
                        versamentoAllegatoInformation.setStatoVersamento(Versamento.StatoVersamento.VERSATO);
                    }
                    break;
                }
                case ERRORE_PLUG_IN: {
                    versamentoDocInformation.setRapporto(responseJson);
                    versamentoDocInformation.setCodiceErrore(response.getResponseCode());
                    versamentoDocInformation.setDescrizioneErrore(response.getErrorMessage());
                    versamentoDocInformation.setStatoVersamentoPrecedente(versamentoDocInformation.getStatoVersamento());
                    versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
                    if (versamentiAllegatiInformationList != null) {
                        for (VersamentoAllegatoInformation versamentoAllegatoInformation : versamentiAllegatiInformationList) {
                            versamentoAllegatoInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
                        }
                    }
                    log.error("SDICO ha risposto con il seguente errore: " + response.getErrorMessage());
                    break;
                }
                default: {
                    versamentoDocInformation.setRapporto(responseJson);
                    if (response.getResponseCode() != null && response.getResponseCode() != "") {
                        versamentoDocInformation.setCodiceErrore(response.getResponseCode());
                    } else {
                        versamentoDocInformation.setCodiceErrore(ERRORE_PLUG_IN);
                    }
                    if (response.getErrorMessage() != null && response.getErrorMessage() != "") {
                        versamentoDocInformation.setDescrizioneErrore(response.getErrorMessage());
                    } else {
                        versamentoDocInformation.setDescrizioneErrore("Errore non definito");
                    }
                    versamentoDocInformation.setStatoVersamentoPrecedente(versamentoDocInformation.getStatoVersamento());
                    versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
                    if (versamentiAllegatiInformationList != null) {
                        for (VersamentoAllegatoInformation versamentoAllegatoInformation : versamentiAllegatiInformationList) {
                            versamentoAllegatoInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
                        }
                    }
                    log.error("Il plug-in SIDCO ha risposto con il seguente errore: " + versamentoDocInformation.getDescrizioneErrore());
                    break;
                }
            }
            versamentoDocInformation.setVersamentiAllegatiInformations(versamentiAllegatiInformationList);
        } else {
            versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE_RITENTABILE);
            versamentoDocInformation.setCodiceErrore("SERVIZIO");
        }
        return versamentoDocInformation;
    }

    /**
     * Metodo che si occupa di fare la chiamata a SDICO per versare il documento
     * indicato in versamentoDocInformation. Restituisce una mappa contenete i
     * metadati versati, la response proveniente da SDICO e la lista dei
     * VersamentoAllegatoInformation dei file versati.
     *
     * @param versamentoDocInformation
     * @return
     */
    public Map<String, Object> versaDocumentoSDICO(VersamentoDocInformation versamentoDocInformation) throws VersatoreProcessingException {
        log.info("Inizio con il versamento del doc: " + Integer.toString(versamentoDocInformation.getIdDoc()));
        //preparo i dati
        Integer idDoc = versamentoDocInformation.getIdDoc();
        Map<String, Object> risultatoEVersamentiAllegati = new HashMap<>();
        Map<String, Object> parametriVersamento = versamentoDocInformation.getParams();
        SdicoResponse response = new SdicoResponse();
        try {
            Doc doc = entityManager.find(Doc.class, idDoc);
            DocDetail docDetail = entityManager.find(DocDetail.class, idDoc);
            try {
                Archivio archivio = new Archivio();
                List<ArchivioDoc> listaArchivioDocs = doc.getArchiviDocList();
                Persona responsabileGestioneDocumentale = new Persona();
                if (doc.getTipologia() != DocDetailInterface.TipologiaDoc.RGPICO) {
                    //controllo che il documento non sia già stato versato per questo archivio radice
                    if (!(numeroVersamentiDocPerArchivio(idDoc, versamentoDocInformation.getIdArchivio()) > 0)) {
                        archivio = entityManager.find(Archivio.class, versamentoDocInformation.getIdArchivio());
                        if (archivio.getId() != null && !listaArchivioDocs.isEmpty()) {
                            Archivio archivioDaLista = new Archivio();
                            for (ArchivioDoc archivioDoc : listaArchivioDocs) {
                                //controllo se il documento appartiene a un sottofasicolo o a un inserto anziché al fasciolo radice 
                                //e controllo che non sia stato eliminato logicamente
                                if (archivioDoc.getIdArchivio().getIdArchivioRadice().getId().equals(archivio.getId()) && archivioDoc.getDataEliminazione() == null) {
                                    archivioDaLista = archivioDoc.getIdArchivio();
                                }
                            }
                            if (archivioDaLista.getId() != null) {
                                archivio = archivioDaLista;
                            } else {
                                //se archivioDaLista è vuoto allora il documento è stato eliminato logicamente dal fasicolo, e non è collegato ad altri fascicoli, 
                                //in quel caso non verso il documento
                                response.setErrorMessage("Il documento è stato cancellato logicamente dal fascicolo");
                                response.setResponseCode(CANCELLATO);
                                risultatoEVersamentiAllegati.put("response", response);
                                return risultatoEVersamentiAllegati;
                            }
                        } else {
                            throw new VersatoreSdicoException("Il documento non è collegato ad alcun fasciolo");
                        }
                    } else {
                        //se il documento è già stato versato per questo fascicolo radice non proseguo con il versamento
                        response.setErrorMessage("Il documento è già stato versato per questa fasicolazione");
                        response.setResponseCode(CANCELLATO);
                        risultatoEVersamentiAllegati.put("response", response);
                        return risultatoEVersamentiAllegati;
                    }
                } else {
                    if (listaArchivioDocs.get(0).getId() != null) {
                        archivio = listaArchivioDocs.get(0).getIdArchivio();
                    } else {
                        throw new VersatoreSdicoException("Il documento non è collegato ad alcun fasciolo");
                    }
                    String codiceFiscaleResponsabileGestioneDocumentale = (String) parametriVersamento.get("codiceFiscaleResponsabileGestioneDocumentale");
                    if (!codiceFiscaleResponsabileGestioneDocumentale.isEmpty()
                            && codiceFiscaleResponsabileGestioneDocumentale != null
                            && codiceFiscaleResponsabileGestioneDocumentale != "") {
                        responsabileGestioneDocumentale = personaDaCodiceFiscaleEAzienda(codiceFiscaleResponsabileGestioneDocumentale, doc.getIdAzienda().getId());
                    } else {
                        throw new VersatoreSdicoException("Non è stato indicato il Responsabile della Gestione Documentale");
                    }
                }
                List<RegistroDoc> listaRegistri = doc.getRegistroDocList();
                Registro registro = new Registro();
                if (listaRegistri != null) {
                    for (RegistroDoc reg : listaRegistri) {
                        if (reg.getIdRegistro().getAttivo() && reg.getIdRegistro().getUfficiale()) {
                            registro = reg.getIdRegistro();
                        }
                    }
                }
                List<DocDetailInterface.Firmatario> listaFirmatari = docDetail.getFirmatari();
                List<Persona> firmatari = new ArrayList<>();
                if (listaFirmatari != null) {
                    for (DocDetailInterface.Firmatario firmatario : listaFirmatari) {
                        Persona p = entityManager.find(Persona.class, firmatario.getIdPersona());
                        firmatari.add(p);
                    }
                }
                String username = (String) parametriVersamento.get("username");
                String password = (String) parametriVersamento.get("password");

                //in base alla tipologia di documento instanzio la relativa classe che ne costruisce i metadati
                VersamentoBuilder versamentoBuilder = new VersamentoBuilder();

                try {
                    log.info("Creo i metadati di: " + doc.getTipologia() + " id " + doc.getId() + ", " + doc.getOggetto());
                    switch (doc.getTipologia()) {
                        case DETERMINA: {
                            DeteBuilder db = new DeteBuilder(doc, docDetail, archivio, registro, firmatari, parametriVersamento);
                            versamentoBuilder = db.build();
                            break;
                        }
                        case DELIBERA: {
                            DeliBuilder db = new DeliBuilder(doc, docDetail, archivio, registro, firmatari, parametriVersamento);
                            versamentoBuilder = db.build();
                            break;
                        }
                        case RGPICO: {
                            Pattern pattern = Pattern.compile("(.*)n\\.\\s(.*)\\sal\\sn\\.\\s(.*)\\sdel\\s(.*)", Pattern.MULTILINE);
                            Matcher matcher = pattern.matcher(doc.getOggetto());
                            matcher.matches();
                            matcher.groupCount();
                            String numeroIniziale = matcher.group(2);
                            String numeroFinale = matcher.group(3);
                            ZonedDateTime dataIniziale = getDataRegistrazioneDaNumeroDiRegistrazioneEAnno(numeroIniziale, doc.getIdAzienda().getId());
                            ZonedDateTime dataFinale = getDataRegistrazioneDaNumeroDiRegistrazioneEAnno(numeroFinale, doc.getIdAzienda().getId());
                            RgPicoBuilder rb = new RgPicoBuilder(doc, docDetail, archivio, registro, firmatari, parametriVersamento, numeroIniziale, numeroFinale, dataIniziale, dataFinale, responsabileGestioneDocumentale);
                            versamentoBuilder = rb.build();
                            break;
                        }
                        case PROTOCOLLO_IN_ENTRATA:
                        case PROTOCOLLO_IN_USCITA: {
                            PicoBuilder pb = new PicoBuilder(doc, docDetail, archivio, registro, firmatari, parametriVersamento);
                            versamentoBuilder = pb.build();
                            break;
                        }
                        case DOCUMENT_UTENTE: {
                            ArchivioDetail archivioDetail = entityManager.find(ArchivioDetail.class, archivio.getId());
                            DocumentoGEDIBuilder dgb = new DocumentoGEDIBuilder(doc, docDetail, archivio, archivioDetail, registro, parametriVersamento);
                            versamentoBuilder = dgb.build();
                            break;
                        }
                        default:
                            throw new VersatoreSdicoException("Tipologia documentale non presente");
                    }
                } catch (NullPointerException e) {
                    throw new VersatoreSdicoException("Trovato un valore nullo nella costruzione dei metadati");
                }

                log.info("accedo ai dati degli allegati e li inserisco nell'XML");
                List<Allegato> allegati = doc.getAllegati();
                AllegatiBuilder allegatiBuild = new AllegatiBuilder(versatoreRepositoryConfiguration);
                Map<String, Object> mappaDatiAllegati = allegatiBuild.buildMappaAllegati(doc, docDetail, allegati, versamentoBuilder);
                List<IdentityFile> identityFiles = (List<IdentityFile>) mappaDatiAllegati.get("identityFiles");
                List<VersamentoAllegatoInformation> versamentiAllegatiInformationList = (List<VersamentoAllegatoInformation>) mappaDatiAllegati.get("versamentiAllegatiInfo");
                String metadati = versamentoBuilder.toString();
                risultatoEVersamentiAllegati.put("xmlVersato", metadati);
                risultatoEVersamentiAllegati.put("versamentiAllegatiInformation", versamentiAllegatiInformationList);

                // --Sezione di collegamento con SDICO e versamento--
                List<PaccoFile> paccoFiles = creazionePaccoFile(identityFiles);
                //effettuo il login a SDICO per ricevere il token
                log.info("Effettuo il login");
                String token = "";
                try {
                    token = getJWT(username, password, sdicoLoginURI);
                } catch (IOException e) {
                    throw new VersatoreSdicoException("Errore nell'effettuare il login per la ricezione del token");
                }
                if (token.equals(null) || token.isEmpty()) {
                    throw new VersatoreSdicoException("Non è stato ottenuto il token necessario per l'autenticazione");
                }

                // inizializzazione http client
                OkHttpClient okHttpClient = versatoreHttpClientConfiguration.getHttpClientManager().getOkHttpClient();

                // Conversione file metadati.xml da inputstream to byte[] e aggiungo al multipart
                byte[] fileMetadati = metadati.getBytes(StandardCharsets.UTF_8);
                MultipartBody.Builder buildernew = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "metadati.xml", RequestBody.create(MediaType.parse("application/xml"), fileMetadati));

                // Conversione degli allegati da inputstream to byte[] e aggiungo al multipart
                log.info("Ciclo i file...");
                if (paccoFiles != null) {
                    for (PaccoFile paccoFile : paccoFiles) {
                        log.info("Inserisco nel body il file " + paccoFile.getId() + ", " + paccoFile.getFileName());
                        byte[] bytes;
                        try (InputStream is = paccoFile.getInputStream()) {
                            bytes = IOUtils.toByteArray(is);
                            buildernew.addFormDataPart(paccoFile.getId(), paccoFile.getFileName(), RequestBody.create(MediaType.parse(paccoFile.getMime()), bytes));
                        } catch (Exception ex) {
                            log.error("Problemi con l'inputstream dei file", ex);
                        }
                    }
                }

                // creazione di body multipart
                log.info("Costruisco il MultiPart");
                MultipartBody requestBody = buildernew.build();
                // richiesta
                log.info("Costruisco la request");
                Request request = new Request.Builder()
                        .url(sdicoServizioVersamentoURI)
                        .addHeader("Authorization", token)
                        .post(requestBody)
                        .build();
                log.info("Uri: " + sdicoServizioVersamentoURI);
                log.info("Effettuo la chiamata a SDICO");
                try (Response resp = okHttpClient.newCall(request).execute()) {
                    if (resp.isSuccessful()) {
                        log.info("Message: " + resp.message());
                        String resBodyString = resp.body().string();
                        log.info("Body: " + resBodyString);
                        risultatoEVersamentiAllegati.put("responseJson", resBodyString);
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            response = objectMapper.readValue(resBodyString, SdicoResponse.class);

                        } catch (JsonProcessingException ex) {
                            log.error("Errore nel parsing della response arrivata da SDICO");
                        }
                    } else {
                        log.error("ERROR: message = " + resp.message());
                        String resBodyString = resp.body().string();
                        log.error("Body: " + resBodyString);
                        log.error(resp.toString());
                        response.setErrorMessage(resp.toString());
                    }
                    resp.close(); // chiudo la response
                } catch (Throwable ex) {
                    log.error("Errore nella chiamata di riversamento", ex);
                    response.setErrorMessage("Errore nella chiamata di riversamento");
                    response.setResponseCode(ERRORE_PLUG_IN);
                }
            } catch (VersatoreSdicoException e) {
                log.error(e.getMessage());
                response.setErrorMessage(e.getMessage());
                response.setResponseCode(ERRORE_PLUG_IN);
            }
        } catch (Exception e) {
            response.setErrorMessage("Causa errore: " + e.getCause() + ", messaggio: " + e.getMessage());
            response.setResponseCode(ERRORE_PLUG_IN);
            log.error("Causa errore: " + e.getCause() + ", messaggio: " + e.getMessage());
        }
        risultatoEVersamentiAllegati.put("response", response);
        return risultatoEVersamentiAllegati;
    }

    /**
     * Metodo che effettua una chiamata a SDICO per aver il token necessario per
     * l'autenticazione
     *
     * @param username
     * @param password
     * @param sdicoLoginURI
     * @return
     * @throws IOException
     */
    public String getJWT(String username, String password, String sdicoLoginURI) throws IOException {

        OkHttpClient okHttpClient = versatoreHttpClientConfiguration.getHttpClientManager().getOkHttpClient();
        String json = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(this.sdicoLoginURI)
                .post(body)
                .build();
        Response response = okHttpClient.newCall(request).execute();
        JSONObject jsonObject = new JSONObject(response.body().string());

        return (String) jsonObject.get("token");
    }

    /**
     * Metodo che impacchetta i dati degli allegati, reperisce i file e li
     * prepara per essere versati
     *
     * @param identityFiles
     * @return
     */
    private List<PaccoFile> creazionePaccoFile(List<IdentityFile> identityFiles) {
        List<PaccoFile> filesList = new ArrayList<>();
        for (IdentityFile identityFile : identityFiles) {
            log.info("Cerco l'allegato: " + identityFile.getFileName());
            PaccoFile paccoFile = new PaccoFile();
            try {
                InputStream is = identityFile.getUuidMongo() != null
                        ? minIOWrapper.getByUuid(identityFile.getUuidMongo())
                        : minIOWrapper.getByFileId(identityFile.getFileBase64());
                paccoFile.setInputStream(is);
                paccoFile.setMime(identityFile.getMime());
                paccoFile.setFileName(identityFile.getFileName());
                paccoFile.setId(identityFile.getId());
                filesList.add(paccoFile);
            } catch (MinIOWrapperException ex) {
                log.error("Errore nel reperire il file da MinIO", ex);
            }
        }
        return filesList;
    }

    /**
     * Metodo che, dato il numero di registrazione e l'anno di un protocollo, mi
     * restituisce la data di registrazione
     *
     * @param numeroEAnno
     * @return
     */
    private ZonedDateTime getDataRegistrazioneDaNumeroDiRegistrazioneEAnno(String numeroEAnno, Integer idAzienda) {
        String[] parts = numeroEAnno.split("/");
        Integer numeroRegistrazione = Integer.parseInt(parts[0].replaceAll("^0+", ""));
        Integer anno = Integer.parseInt(parts[1]);
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        ZonedDateTime dataRegistrazione = jPAQueryFactory
                .select(QDocDetail.docDetail.dataRegistrazione)
                .from(QDocDetail.docDetail)
                .where(QDocDetail.docDetail.numeroRegistrazione.eq(numeroRegistrazione)
                        .and(QDocDetail.docDetail.annoRegistrazione.eq(anno))
                        .and(QDocDetail.docDetail.idAzienda.id.eq(idAzienda))
                        .and(QDocDetail.docDetail.tipologia.eq(DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA)
                                .or(QDocDetail.docDetail.tipologia.eq(DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA))))
                .fetchOne();

        return dataRegistrazione;
    }

    /**
     * Metodo che conta quante volte un documento è stato versato per uno stesso
     * archivio
     *
     * @param idDoc
     * @param idArchivio
     * @return
     */
    private Long numeroVersamentiDocPerArchivio(Integer idDoc, Integer idArchivio) {
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        Long numeroDocVersati = jPAQueryFactory.select(QVersamento.versamento.count())
                .from(QVersamento.versamento)
                .where(QVersamento.versamento.idDoc.id.eq(idDoc)
                        .and(QVersamento.versamento.idArchivio.id.eq(idArchivio))
                        .and(QVersamento.versamento.stato.eq(Versamento.StatoVersamento.VERSATO.toString())))
                .fetchOne();
        return numeroDocVersati;
    }

    /**
     * Metodo che dato un codice fiscale ti restituisce la persona
     *
     * @param codiceFiscale
     * @return
     */
    private Persona personaDaCodiceFiscaleEAzienda(String codiceFiscale, Integer idAzienda) {
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        Persona persona = jPAQueryFactory.select(QPersona.persona)
                .from(QPersona.persona)
                .where(QPersona.persona.codiceFiscale.eq(codiceFiscale)
                        .and(QPersona.persona.idAziendaDefault.id.eq(idAzienda)))
                .fetchOne();
        return persona;
    }
}
