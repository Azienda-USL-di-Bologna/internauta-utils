package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatorePluginException;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatorePluginExceptionRitentabile;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders.AllegatiBuilderUnimatica;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities.AllegatoUnimatica;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders.IndiceJsonBuilder;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders.MetadatiBuilder;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities.ErroreUnimatica;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities.ResponseUnimatica;
import it.bologna.ausl.internauta.utils.versatore.utils.UnimaticaVersatoreUtils;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.versatore.QVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import it.bologna.ausl.riversamento.builder.IdentityFile;
import it.bologna.ausl.riversamento.sender.PaccoFile;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author boria
 */
@Component
@Scope("prototype")
public class UnimaticaVersatoreService extends VersatoreDocs {

    private static final Logger log = LoggerFactory.getLogger(UnimaticaVersatoreService.class);
    private static final String UNIMATICA_VERSATORE_SERVICE = "UnimaticaVersatoreService";
    private static final String OK = "OK";
    private static final String CANCELLATO = "CANCELLATO";
    private static final String ERRORE_PLUG_IN = "ERRORE_PLUG_IN";
    private static final String ERRORE_PLUG_IN_RITENTABILE = "ERRORE_PLUG_IN_RITENTABILE";

    private String unimaticaServizioVersamentoURI;

    @Autowired
    VersatoreHttpClientConfiguration versatoreHttpClientConfiguration;

    @Override
    public void init(VersatoreConfiguration versatoreConfiguration) {
        super.init(versatoreConfiguration);
        Map<String, Object> versatoreConfigurationMap = this.versatoreConfiguration.getParams();
        Map<String, Object> unimaticaServiceConfiguration = (Map<String, Object>) versatoreConfigurationMap.get(UNIMATICA_VERSATORE_SERVICE);
        unimaticaServizioVersamentoURI = unimaticaServiceConfiguration.get("UnimaticaServizioVersamentoURI").toString();
        log.info("Unimatica servizio versamento URI: {}", unimaticaServizioVersamentoURI);
    }

    @Override
    public VersamentoDocInformation versaImpl(VersamentoDocInformation versamentoDocInformation) throws VersatoreProcessingException {
        //TODO la gestione degli errori deve essere provata, in base a cosa poi unimatica restituisce
        Map<String, Object> mappaResultAndAllegati = new HashMap<>();
        //Reperisoco i risultati del versamento
        //TODO aggiornare la firma della funzione senza miniowrapper ecc.. (vedi SDICO)
        mappaResultAndAllegati = versaDocumentoUnimatica(versamentoDocInformation, entityManager, versatoreRepositoryConfiguration, objectMapper, null, null);
        ResponseUnimatica response = (ResponseUnimatica) mappaResultAndAllegati.get("response");
        String responseJson = (String) mappaResultAndAllegati.get("responseJson");
        String xmlVersato = (String) mappaResultAndAllegati.get("xmlVersato");
        List<VersamentoAllegatoInformation> versamentiAllegatiInformationList = (List<VersamentoAllegatoInformation>) mappaResultAndAllegati.get("versamentiAllegatiInformation");

        //Imposto i dati del DocInformation con i risultati
        versamentoDocInformation.setMetadatiVersati(xmlVersato);
        versamentoDocInformation.setDataVersamento(ZonedDateTime.now());
        if (response != null) {
            if (response.getResponseCode().equals(OK)) {
                versamentoDocInformation.setRapporto(responseJson);
                versamentoDocInformation.setStatoVersamentoPrecedente(versamentoDocInformation.getStatoVersamento());
                versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.VERSATO);
                for (VersamentoAllegatoInformation versamentoAllegatoInformation : versamentiAllegatiInformationList) {
                    versamentoAllegatoInformation.setStatoVersamento(Versamento.StatoVersamento.VERSATO);
                }
            } else {
                switch (response.getResponseCode()) {
                    case CANCELLATO: {
                        versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.ANNULLATO);
                        versamentoDocInformation.setRapporto(response.getErrorMessage());
                        log.warn("Il versamento del documento " + versamentoDocInformation.getIdDoc() + " è stato annullato, in quanto: " + response.getErrorMessage());
                        break;
                    }
                    case ERRORE_PLUG_IN_RITENTABILE:
                    case ERRORE_PLUG_IN: {
                        Versamento.StatoVersamento statoVersamento = response.getResponseCode().equals(ERRORE_PLUG_IN)
                            ? Versamento.StatoVersamento.ERRORE
                            : Versamento.StatoVersamento.ERRORE_RITENTABILE;
                        versamentoDocInformation.setRapporto(responseJson);
                        versamentoDocInformation.setCodiceErrore(response.getResponseCode());
                        versamentoDocInformation.setDescrizioneErrore(response.getErrorMessage());
                        versamentoDocInformation.setStatoVersamentoPrecedente(versamentoDocInformation.getStatoVersamento());
                        versamentoDocInformation.setStatoVersamento(statoVersamento);
                        if (versamentiAllegatiInformationList != null) {
                            for (VersamentoAllegatoInformation versamentoAllegatoInformation : versamentiAllegatiInformationList) {
                                versamentoAllegatoInformation.setStatoVersamento(statoVersamento);
                            }
                        }
                        log.error("Il plug-in Unimatica ha risposto con il seguente errore: " + response.getErrorMessage());
                        break;
                    }
                    default: {
                        versamentoDocInformation.setRapporto(responseJson);
                        if (StringUtils.hasText(response.getResponseCode())) {
                            versamentoDocInformation.setCodiceErrore(response.getResponseCode());
                        } else {
                            versamentoDocInformation.setCodiceErrore(ERRORE_PLUG_IN);
                        }
                        if (StringUtils.hasText(response.getErrorMessage())) {
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
                        log.error("Il plug-in Unimatica ha risposto con il seguente errore: " + versamentoDocInformation.getDescrizioneErrore());
                        break;
                    }
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
     * Metodo che si occupa di fare la chiamata al Gateway di Unimatica per versare il documento
     * indicato in versamentoDocInformation. Restituisce una mappa contenete ((TODO
     * l'indice json e)) i metadati versati, la response proveniente da Unimatica e la lista dei
     * VersamentoAllegatoInformation dei file versati.
     *
     * @param versamentoDocInformation
     * @return
     */
    //TODO togliere entity manager e vers conf e object mapper
    public Map<String, Object> versaDocumentoUnimatica(VersamentoDocInformation versamentoDocInformation, EntityManager entityManager, VersatoreRepositoryConfiguration versatoreRepositoryConfiguration, ObjectMapper objectMapper, MinIOWrapper minIOWrapper, VersatoreHttpClientConfiguration versatoreHttpClientConfiguration) throws VersatoreProcessingException {

        //TODO da togliere
        this.minIOWrapper = minIOWrapper;
        this.versatoreHttpClientConfiguration = versatoreHttpClientConfiguration;
        //

        log.info("Inizio con il versamento del doc: " + Integer.toString(versamentoDocInformation.getIdDoc()));
        //preparo i dati
        Integer idDoc = versamentoDocInformation.getIdDoc();
        Map<String, Object> risultatoEVersamentiAllegati = new HashMap<>();
        Map<String, Object> parametriVersamento = versamentoDocInformation.getParams();
        ResponseUnimatica response = new ResponseUnimatica();
        try {
            Doc doc = entityManager.find(Doc.class, idDoc);
            try {
                //prendo l'archivio e il repsonsabile della gestione documentale
                Archivio archivio = new Archivio();
                List<ArchivioDoc> listaArchivioDocs = doc.getArchiviDocList();
                if (listaArchivioDocs.isEmpty()) {
                    throw new VersatorePluginException("Il documento non è collegato ad alcun fasciolo");
                }
                Persona responsabileGestioneDocumentale = new Persona();
                if (doc.getTipologia() != Doc.TipologiaDoc.RGPICO) {
                    //controllo che il documento non sia già stato versato per questo archivio radice
                    if (!(numeroVersamentiDocPerArchivio(idDoc, versamentoDocInformation.getIdArchivio(), entityManager) > 0)) {
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
                                ErroreUnimatica erroreGenerale = new ErroreUnimatica();
                                erroreGenerale.setDescrizione("Il documento è stato cancellato logicamente dal fascicolo");
                                erroreGenerale.setCodice(CANCELLATO);
                                risultatoEVersamentiAllegati.put("response", response);
                                return risultatoEVersamentiAllegati;
                            }
                        } else {
                            throw new VersatorePluginException("Il documento non è collegato ad alcun fasciolo");
                        }
                    } else {
                        //se il documento è già stato versato per questo fascicolo radice non proseguo con il versamento
                        ErroreUnimatica erroreGenerale = new ErroreUnimatica();
                        erroreGenerale.setDescrizione("Il documento è già stato versato per questa fasicolazione");
                        erroreGenerale.setCodice(CANCELLATO);
                        risultatoEVersamentiAllegati.put("response", response);
                        return risultatoEVersamentiAllegati;
                    }
                } else {
                    if (listaArchivioDocs.get(0).getId() != null) {
                        archivio = listaArchivioDocs.get(0).getIdArchivio();
                    } else {
                        throw new VersatorePluginException("Il documento non è collegato ad alcun fasciolo");
                    }
                    //prendo il responsabile della gestione documentale, inserire se serve
                    /*String usernameResponsabileGestioneDocumentale = (String) parametriVersamento.get("usernameResponsabileGestioneDocumentale");
                    if (!usernameResponsabileGestioneDocumentale.isEmpty()
                        && usernameResponsabileGestioneDocumentale != null
                        && usernameResponsabileGestioneDocumentale != "") {
                        responsabileGestioneDocumentale = personaDaUsernameEAzienda(usernameResponsabileGestioneDocumentale, doc.getIdAzienda().getId());
                    } else {
                        throw new VersatorePluginException("Non è stato indicato il Responsabile della Gestione Documentale");
                    }*/
                }
                log.info("accedo ai dati degli allegati e li inserisco nell'XML");
                List<Allegato> allegati = doc.getAllegati();
                //TODO cambia configuration
                AllegatiBuilderUnimatica allegatiBuild = new AllegatiBuilderUnimatica(versatoreRepositoryConfiguration);
                Map<String, Object> mappaDatiAllegati = allegatiBuild.buildMappaAllegati(doc, allegati);
                AllegatoUnimatica documentoPrincipale = (AllegatoUnimatica) mappaDatiAllegati.get("documentoPrincipale");
                List<AllegatoUnimatica> allegatiSecondariList = (List<AllegatoUnimatica>) mappaDatiAllegati.get("allegatiSecondari");
                //creazione dell xml dei metadati
                MetadatiBuilder metadatiBuilder = new MetadatiBuilder(parametriVersamento, doc, archivio, documentoPrincipale, allegatiSecondariList);
                metadatiBuilder.build();
                String metadati = metadatiBuilder.toString();
                //TODO da togliere
                log.info(metadati);
                risultatoEVersamentiAllegati.put("metadati", metadati);
                byte[] fileMetadati = metadati.getBytes(StandardCharsets.UTF_8);
                //calcolo lo sha 256 del file di metadati
                String sha256HexMetadati = "";
                try (InputStream is = new ByteArrayInputStream(fileMetadati)) {
                    sha256HexMetadati = DigestUtils.sha256Hex(is);
                } catch (IOException ex) {
                    log.error("Errore nel calcoalre l'hashSHA256 del file xml dei metadati", ex);
                    throw new VersatorePluginException("Errore nel calcoalre l'hashSHA256 del file xml dei metadati");
                }
                //creazione dell'indice json
                IndiceJsonBuilder indiceJsonBuilder = new IndiceJsonBuilder(parametriVersamento, doc, documentoPrincipale, allegatiSecondariList, sha256HexMetadati);
                Map<String, Object> indiceJsonMap = indiceJsonBuilder.build();
                String indiceJsonString = objectMapper.writeValueAsString(indiceJsonMap);
                //TODO da togliere
                log.info(indiceJsonString);
                risultatoEVersamentiAllegati.put("indiceJson", indiceJsonString);

                // --Sezione di collegamento con UNIMATICA e versamento--
                //creazione del multipart
                //metadati e json
                //TODO vedere come costruire il nome
                String nomeFileDocumentoPrincipale = UnimaticaVersatoreUtils.removeExtension(documentoPrincipale.getNomeFile());
                //String nomeFileMetadati = doc.getId() + ".xml";
                MultipartBody.Builder buildernew = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("indice", indiceJsonString)
                    .addFormDataPart("metadati", idDoc + "_" + nomeFileDocumentoPrincipale + ".xml", RequestBody.create(MediaType.parse("application/xml"), fileMetadati));

                // Conversione degli allegati da inputstream to byte[] e aggiungo al multipart
                List<IdentityFile> identityFiles = (List<IdentityFile>) mappaDatiAllegati.get("identityFiles");
                List<PaccoFile> paccoFiles = creazionePaccoFile(identityFiles);
                log.info("Ciclo i file...");
                if (paccoFiles != null) {
                    for (PaccoFile paccoFile : paccoFiles) {
                        log.info("Inserisco nel body il file " + paccoFile.getId() + ", " + paccoFile.getFileName());
                        //prendo l'idAllegato per costruire il nome del file da inviare
                        Integer idAllegatoDaVersare = null;
                        if (documentoPrincipale.getNomeFile().equals(paccoFile.getFileName())) {
                            idAllegatoDaVersare = documentoPrincipale.getIdFile();
                        } else {
                            for (AllegatoUnimatica allegatoDaVersare : allegatiSecondariList) {
                                if (allegatoDaVersare.getNomeFile().equals(paccoFile.getFileName())) {
                                    idAllegatoDaVersare = allegatoDaVersare.getIdFile();
                                    break;
                                }
                            }
                        }
                        if (idAllegatoDaVersare == null) {
                            log.error("Non si riesce a individuare l'id dell'allegato da versare, possibile problema di costruzione dell'oggetto PaccoFile");
                            throw new VersatorePluginException("Non si riesce a individuare l'id dell'allegato da versare, possibile problema di costruzione dell'oggetto PaccoFile");
                        }
                        byte[] bytes;
                        try (InputStream is = paccoFile.getInputStream()) {
                            bytes = IOUtils.toByteArray(is);
                            buildernew.addFormDataPart("documenti", String.valueOf(idAllegatoDaVersare), RequestBody.create(MediaType.parse(paccoFile.getMime()), bytes));
                        } catch (Exception ex) {
                            log.error("Problemi con l'inputstream dei file", ex);
                            throw new VersatorePluginExceptionRitentabile("Problemi con l'inputstream dei file");
                        }
                    }
                }
                // creazione di body multipart
                log.info("Costruisco il MultiPart");
                MultipartBody requestBody = buildernew.build();
                //TODO da togliere
                risultatoEVersamentiAllegati.put("requestBody", requestBody);
                //credenziali autorizzazione
                String username = (String) parametriVersamento.get("username");
                String password = (String) parametriVersamento.get("password");

                // richiesta
                log.info("Costruisco la request");

                //TODO da togliere
                unimaticaServizioVersamentoURI = "https://webt.unimaticaspa.it/gwconservazione-test/api/v1/consegna/pdv";
                username = "arpal_umbria_tecnica@fittizia.it";
                password = "arpal_umbria_tecnica";
                //

                Request request = new Request.Builder()
                    .url(unimaticaServizioVersamentoURI)
                    .post(requestBody)
                    .header("Authorization", Credentials.basic(username, password)) // Basic Auth
                    .build();
                log.info("Uri: " + unimaticaServizioVersamentoURI);
                log.info("Effettuo la chiamata a Unimatica");
                // inizializzazione http client
                OkHttpClient okHttpClient = versatoreHttpClientConfiguration.getHttpClientManager().getOkHttpClient();
                try (Response resp = okHttpClient.newCall(request).execute()) {
                    if (resp.isSuccessful()) {
                        log.info("Message: " + resp.message());
                        String resBodyString = resp.body().string();
                        log.info("Body: " + resBodyString);
                        risultatoEVersamentiAllegati.put("responseJson", resBodyString);
                        //TODO ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            response = objectMapper.readValue(resBodyString, ResponseUnimatica.class);

                        } catch (JacksonException ex) {
                            log.error("Errore nel parsing della response arrivata da Unimatica", ex);
                        }
                    } else {
                        log.error("ERROR: message = " + resp.message());
                        String resBodyString = resp.body().string();
                        log.error("Body: " + resBodyString);
                        log.error(resp.toString());
                        ErroreUnimatica erroreGenerale = new ErroreUnimatica();
                        erroreGenerale.setDescrizione(resBodyString);
                        erroreGenerale.setCodice(ERRORE_PLUG_IN);
                        //TODO toglere?
                        //risultatoEVersamentiAllegati.put("responseJson", resBodyString);
                        /*try {
                            response = objectMapper.readValue(resBodyString, ResponseUnimatica.class);

                        } catch (JacksonException ex) {
                            log.error("Errore nel parsing della response arrivata da Unimatica", ex);
                        }*/
 /*TODO response.setErrorMessage(resp.toString());
                        if (resp.code() == 500) {
                            response.setResponseCode(ERRORE_PLUG_IN_RITENTABILE);
                        } else {
                            response.setResponseCode(ERRORE_PLUG_IN);
                        }*/
                    }
                    resp.close(); // chiudo la response
                } catch (Throwable ex) {
                    log.error("Errore nella chiamata di riversamento", ex);
                    ErroreUnimatica erroreGenerale = new ErroreUnimatica();
                    erroreGenerale.setDescrizione("Errore nella chiamata di riversamento");
                    erroreGenerale.setCodice(ERRORE_PLUG_IN_RITENTABILE);
                }
            } catch (VersatorePluginException e) {
                log.error("Errore:", e);
                ErroreUnimatica erroreGenerale = new ErroreUnimatica();
                erroreGenerale.setDescrizione(e.getMessage());
                erroreGenerale.setCodice(ERRORE_PLUG_IN);
            } catch (VersatorePluginExceptionRitentabile e) {
                log.error("Errore:", e);
                ErroreUnimatica erroreGenerale = new ErroreUnimatica();
                erroreGenerale.setDescrizione(e.getMessage());
                erroreGenerale.setCodice(ERRORE_PLUG_IN_RITENTABILE);
            }
        } catch (Exception e) {
            ErroreUnimatica erroreGenerale = new ErroreUnimatica();
            erroreGenerale.setDescrizione("Causa errore: " + e.getCause() + ", messaggio: " + e.getMessage());
            erroreGenerale.setCodice(ERRORE_PLUG_IN);
            log.error("Causa errore: " + e.getCause() + ", messaggio: " + e.getMessage(), e);
        }
        return risultatoEVersamentiAllegati;
    }

    /**
     * Metodo che conta quante volte un documento è stato versato per uno stesso
     * archivio
     *
     * @param idDoc
     * @param idArchivio
     * @return
     */
    //TODO levare entity manager
    private Long numeroVersamentiDocPerArchivio(Integer idDoc, Integer idArchivio, EntityManager entityManager) {
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
     * Metodo che dato uno username ti restituisce la persona
     *
     * @param codiceFiscale
     * @return
     */
    private Persona personaDaUsernameEAzienda(String username, Integer idAzienda) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        QUtente utente = QUtente.utente;
        QPersona persona = QPersona.persona;

        Persona result = queryFactory
            .select(persona)
            .from(utente)
            .join(utente.idPersona, persona)
            .where(utente.idAzienda.id.eq(idAzienda)
                .and(utente.username.eq(username)))
            .fetchOne();
        return result;
    }

    /**
     * Metodo che impacchetta i dati degli allegati, reperisce i file e li
     * prepara per essere versati
     *
     * @param identityFiles
     * @return
     */
    //TODO togliere minio wrapper
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
}
