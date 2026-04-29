package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica;

import com.querydsl.jpa.impl.JPAQueryFactory;
import tools.jackson.core.JacksonException;
import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatorePluginException;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatorePluginExceptionRitentabile;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders.AllegatiBuilderUnimatica;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities.AllegatoUnimatica;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders.IndiceJsonBuilder;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders.MetadatiBuilder;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities.ErroreUnimatica;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities.IdentityFileUnimatica;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities.PaccoFileUnimatica;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities.ResponseUnimatica;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.tools.QSupportedFile;
import it.bologna.ausl.model.entities.tools.SupportedFile;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import okhttp3.Credentials;
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
    private static final String KO = "KO";
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
        Map<String, Object> mappaResultAndAllegati = new HashMap<>();
        //Reperisoco i risultati del versamento
        mappaResultAndAllegati = versaDocumentoUnimatica(versamentoDocInformation);
        ResponseUnimatica response = (ResponseUnimatica) mappaResultAndAllegati.get("response");
        String responseJson = (String) mappaResultAndAllegati.get("responseJson");
        String xmlVersato = (String) mappaResultAndAllegati.get("xmlVersato");
        ErroreUnimatica errore = (ErroreUnimatica) mappaResultAndAllegati.get("erroreUnimatica");
        List<VersamentoAllegatoInformation> versamentiAllegatiInformationList = (List<VersamentoAllegatoInformation>) mappaResultAndAllegati.get("versamentiAllegatiInformation");

        //Imposto i dati del DocInformation con i risultati
        versamentoDocInformation.setMetadatiVersati(xmlVersato);
        versamentoDocInformation.setDataVersamento(ZonedDateTime.now());
        if (response != null) {
            if (response.getEsitoComplessivo().equals(OK)) {
                versamentoDocInformation.setRapporto(responseJson);
                versamentoDocInformation.setStatoVersamentoPrecedente(versamentoDocInformation.getStatoVersamento());
                versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.VERSATO);
                for (VersamentoAllegatoInformation versamentoAllegatoInformation : versamentiAllegatiInformationList) {
                    versamentoAllegatoInformation.setStatoVersamento(Versamento.StatoVersamento.VERSATO);
                }
            } else if (response.getEsitoComplessivo().equals(KO)) {
                Versamento.StatoVersamento statoVersamento = Versamento.StatoVersamento.ERRORE;
                versamentoDocInformation.setRapporto(responseJson);
                //prendo dal json della risposta gli errori e li concateno
                String codiciErroriGenerali = response.getErroriGeneraliList()
                    .stream()
                    .map(obj -> obj.getCodice())
                    .collect(Collectors.joining("|"));
                String descrizioniErroriGenerali = response.getErroriGeneraliList()
                    .stream()
                    .map(obj -> obj.getDescrizione())
                    .collect(Collectors.joining("|"));
                String codiciErrori = response.getEsitoConsegnaUnimatica().getErroriList()
                    .stream()
                    .map(obj -> obj.getCodice())
                    .collect(Collectors.joining("|"));
                String descrizioniErrori = response.getEsitoConsegnaUnimatica().getErroriList()
                    .stream()
                    .map(obj -> obj.getDescrizione())
                    .collect(Collectors.joining("|"));
                String codiciErroriString = "Errori generali: (" + codiciErroriGenerali + ") - Errori esito consegna: (" + codiciErrori + ")";
                String descrizioniErroriString = "Errori generali: (" + descrizioniErroriGenerali + ") - Errori esito consegna: (" + descrizioniErrori + ")";
                versamentoDocInformation.setCodiceErrore(codiciErroriString);
                versamentoDocInformation.setDescrizioneErrore(descrizioniErroriString);
                versamentoDocInformation.setStatoVersamentoPrecedente(versamentoDocInformation.getStatoVersamento());
                versamentoDocInformation.setStatoVersamento(statoVersamento);
                if (versamentiAllegatiInformationList != null) {
                    for (VersamentoAllegatoInformation versamentoAllegatoInformation : versamentiAllegatiInformationList) {
                        versamentoAllegatoInformation.setStatoVersamento(statoVersamento);
                    }
                }
                log.error("Il plug-in Unimatica ha risposto con il seguente errore: " + descrizioniErroriString);
            } else {
                log.error("Codice response non riconosciuto");
                versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE_RITENTABILE);
                versamentoDocInformation.setCodiceErrore("SERVIZIO");
            }
        } else if (errore != null) {
            switch (errore.getCodice()) {
                case ERRORE_PLUG_IN_RITENTABILE:
                case ERRORE_PLUG_IN: {
                    Versamento.StatoVersamento statoVersamento = errore.getCodice().equals(ERRORE_PLUG_IN)
                        ? Versamento.StatoVersamento.ERRORE
                        : Versamento.StatoVersamento.ERRORE_RITENTABILE;
                    versamentoDocInformation.setRapporto(responseJson);
                    versamentoDocInformation.setCodiceErrore(errore.getCodice());
                    versamentoDocInformation.setDescrizioneErrore(errore.getDescrizione());
                    versamentoDocInformation.setStatoVersamentoPrecedente(versamentoDocInformation.getStatoVersamento());
                    versamentoDocInformation.setStatoVersamento(statoVersamento);
                    if (versamentiAllegatiInformationList != null) {
                        for (VersamentoAllegatoInformation versamentoAllegatoInformation : versamentiAllegatiInformationList) {
                            versamentoAllegatoInformation.setStatoVersamento(statoVersamento);
                        }
                    }
                    log.error("Il plug-in Unimatica ha risposto con il seguente errore: " + errore.getDescrizione());
                    break;
                }
                default: {
                    versamentoDocInformation.setRapporto(responseJson);
                    if (StringUtils.hasText(errore.getCodice())) {
                        versamentoDocInformation.setCodiceErrore(errore.getCodice());
                    } else {
                        versamentoDocInformation.setCodiceErrore(ERRORE_PLUG_IN);
                    }
                    if (StringUtils.hasText(errore.getDescrizione())) {
                        versamentoDocInformation.setDescrizioneErrore(errore.getDescrizione());
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
            versamentoDocInformation.setVersamentiAllegatiInformations(versamentiAllegatiInformationList);
        } else {
            log.error("Response non pervenuta");
            versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE_RITENTABILE);
            versamentoDocInformation.setCodiceErrore("SERVIZIO");
        }

        return versamentoDocInformation;
    }

    /**
     * Metodo che si occupa di fare la chiamata al Gateway di Unimatica per versare il documento
     * indicato in versamentoDocInformation. Restituisce una mappa contenete
     * l'indice json e i metadati versati, la response proveniente da Unimatica (o l'eventuale errore ricevuto) e la lista dei
     * VersamentoAllegatoInformation dei file versati.
     *
     * @param versamentoDocInformation
     * @return
     */
    public Map<String, Object> versaDocumentoUnimatica(VersamentoDocInformation versamentoDocInformation) throws VersatoreProcessingException {

        log.info("Inizio con il versamento del doc: " + Integer.toString(versamentoDocInformation.getIdDoc()));
        //preparo i dati
        Integer idDoc = versamentoDocInformation.getIdDoc();
        Map<String, Object> risultatoEVersamentiAllegati = new HashMap<>();
        Map<String, Object> parametriVersamento = versamentoDocInformation.getParams();
        ResponseUnimatica response = new ResponseUnimatica();
        try {
            Doc doc = entityManager.find(Doc.class, idDoc);
            try {
                //prendo la lista degli archivi
                List<ArchivioDoc> archiviDocList = doc.getArchiviDocList()
                    .stream().filter(archivioListObj -> archivioListObj.getDataEliminazione() == null)
                    .collect(Collectors.toList());
                if (archiviDocList.isEmpty() || archiviDocList == null) {
                    log.error("Il documento non è fascicolato");
                    throw new VersatorePluginException("Il documento non è fascicolato");
                }
                //ulteriori controlli da aggiungere se ce ne sarà bisogno
                //eseguo controlli specificie per le tipologie di documento
                /*switch (doc.getTipologia()) {
                    case DETERMINA:
                    case DELIBERA:
                    //se esiste solo un fasicolo per il documento vuol dire che non è stato fascicolato perché trovo solo il fascicolo speciale
                        if (archiviDocList.size() == 1) {
                            log.error("Il documento non è fascicolato");
                            throw new VersatorePluginException("Il documento non è fascicolato");
                        }
                        break;
                }*/
                log.info("accedo ai dati degli allegati e li inserisco nell'XML");

                //gestisco gli allegati, il principale e gli altri secondari
                List<Allegato> allegati = doc.getAllegati();
                AllegatiBuilderUnimatica allegatiBuild = new AllegatiBuilderUnimatica(versatoreRepositoryConfiguration);
                Map<String, Object> mappaDatiAllegati = allegatiBuild.buildMappaAllegati(doc, allegati, getSupportedFiles());
                AllegatoUnimatica documentoPrincipale = (AllegatoUnimatica) mappaDatiAllegati.get("documentoPrincipale");
                List<AllegatoUnimatica> allegatiSecondariList = (List<AllegatoUnimatica>) mappaDatiAllegati.get("allegatiSecondari");
                List<VersamentoAllegatoInformation> versamentiAllegatiInformationList = (List<VersamentoAllegatoInformation>) mappaDatiAllegati.get("versamentiAllegatiInfo");
                risultatoEVersamentiAllegati.put("versamentiAllegatiInformation", versamentiAllegatiInformationList);

                //creazione dell xml dei metadati
                MetadatiBuilder metadatiBuilder = new MetadatiBuilder(parametriVersamento, doc, archiviDocList, documentoPrincipale, allegatiSecondariList);
                metadatiBuilder.build();
                String metadati = metadatiBuilder.toString();
                risultatoEVersamentiAllegati.put("xmlVersato", metadati);
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
                risultatoEVersamentiAllegati.put("indiceJson", indiceJsonString);
                log.info("Indice JSON:\n" + indiceJsonString);

                // --Sezione di collegamento con UNIMATICA e versamento--
                //creazione del multipart
                //metadati e json
                //il nome sarà l'id allegato
                MultipartBody.Builder buildernew = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("indice", indiceJsonString)
                    .addFormDataPart("metadati", String.valueOf(documentoPrincipale.getIdFile()) + ".xml", RequestBody.create(MediaType.parse("application/xml"), fileMetadati));

                // Conversione degli allegati da inputstream to byte[] e aggiungo al multipart
                List<IdentityFileUnimatica> identityFiles = (List<IdentityFileUnimatica>) mappaDatiAllegati.get("identityFiles");
                List<PaccoFileUnimatica> paccoFiles = creazionePaccoFile(identityFiles);
                log.info("Ciclo i file...");
                if (paccoFiles != null) {
                    for (PaccoFileUnimatica paccoFile : paccoFiles) {
                        log.info("Inserisco nel body il file " + paccoFile.getId() + ", " + paccoFile.getFileName());
                        Integer idAllegatoDaVersare = paccoFile.getIdAllegato();
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
                //credenziali autorizzazione
                String username = (String) parametriVersamento.get("username");
                String password = (String) parametriVersamento.get("password");

                // richiesta
                log.info("Costruisco la request");

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
                        try {
                            response = objectMapper.readValue(resBodyString, ResponseUnimatica.class);
                        } catch (JacksonException ex) {
                            log.error("Errore nel parsing della response arrivata da Unimatica", ex);
                            ErroreUnimatica erroreGenerale = new ErroreUnimatica();
                            erroreGenerale.setDescrizione("Errore nel parsing della response arrivata da Unimatica");
                            erroreGenerale.setCodice(ERRORE_PLUG_IN_RITENTABILE);
                            risultatoEVersamentiAllegati.put("erroreUnimatica", erroreGenerale);
                        }
                        risultatoEVersamentiAllegati.put("response", response);
                    } else {
                        log.error("ERROR: message = " + resp.message());
                        String resBodyString = resp.body().string();
                        log.error("Body: " + resBodyString);
                        log.error(resp.toString());
                        ErroreUnimatica erroreGenerale = new ErroreUnimatica();
                        erroreGenerale.setDescrizione(resp.toString());
                        if (resp.code() == 500) {
                            erroreGenerale.setCodice(ERRORE_PLUG_IN_RITENTABILE);
                        } else {
                            erroreGenerale.setCodice(ERRORE_PLUG_IN);
                        }
                        risultatoEVersamentiAllegati.put("erroreUnimatica", erroreGenerale);
                    }
                    resp.close(); // chiudo la response
                } catch (Throwable ex) {
                    log.error("Errore nella chiamata di riversamento", ex);
                    ErroreUnimatica erroreGenerale = new ErroreUnimatica();
                    erroreGenerale.setDescrizione("Errore nella chiamata di riversamento");
                    erroreGenerale.setCodice(ERRORE_PLUG_IN_RITENTABILE);
                    risultatoEVersamentiAllegati.put("erroreUnimatica", erroreGenerale);
                }
            } catch (VersatorePluginException e) {
                log.error("Errore:", e);
                ErroreUnimatica erroreGenerale = new ErroreUnimatica();
                erroreGenerale.setDescrizione(e.getMessage());
                erroreGenerale.setCodice(ERRORE_PLUG_IN);
                risultatoEVersamentiAllegati.put("erroreUnimatica", erroreGenerale);
            } catch (VersatorePluginExceptionRitentabile e) {
                log.error("Errore:", e);
                ErroreUnimatica erroreGenerale = new ErroreUnimatica();
                erroreGenerale.setDescrizione(e.getMessage());
                erroreGenerale.setCodice(ERRORE_PLUG_IN_RITENTABILE);
                risultatoEVersamentiAllegati.put("erroreUnimatica", erroreGenerale);
            }
        } catch (Exception e) {
            ErroreUnimatica erroreGenerale = new ErroreUnimatica();
            erroreGenerale.setDescrizione("Causa errore: " + e.getCause() + ", messaggio: " + e.getMessage());
            erroreGenerale.setCodice(ERRORE_PLUG_IN);
            log.error("Causa errore: " + e.getCause() + ", messaggio: " + e.getMessage(), e);
            risultatoEVersamentiAllegati.put("erroreUnimatica", erroreGenerale);
        }
        return risultatoEVersamentiAllegati;
    }

    /**
     * Metodo che impacchetta i dati degli allegati, reperisce i file e li
     * prepara per essere versati
     *
     * @param identityFiles
     * @return
     */
    private List<PaccoFileUnimatica> creazionePaccoFile(List<IdentityFileUnimatica> identityFiles) {
        List<PaccoFileUnimatica> filesList = new ArrayList<>();
        for (IdentityFileUnimatica identityFile : identityFiles) {
            log.info("Cerco l'allegato: " + identityFile.getFileName());
            PaccoFileUnimatica paccoFile = new PaccoFileUnimatica();
            try {
                InputStream is = identityFile.getUuidMongo() != null
                    ? minIOWrapper.getByUuid(identityFile.getUuidMongo())
                    : minIOWrapper.getByFileId(identityFile.getFileBase64());
                paccoFile.setInputStream(is);
                paccoFile.setMime(identityFile.getMime());
                paccoFile.setFileName(identityFile.getFileName());
                paccoFile.setId(identityFile.getId());
                paccoFile.setIdAllegato(identityFile.getIdAllegato());
                filesList.add(paccoFile);
            } catch (MinIOWrapperException ex) {
                log.error("Errore nel reperire il file da MinIO", ex);
            }
        }
        return filesList;
    }

    /**
     *Ritorna la lisata dei file supportati
    @param entityManager
    @return
     */
    public List<SupportedFile> getSupportedFiles() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        return queryFactory.selectFrom(QSupportedFile.supportedFile).fetch();
    }
}
