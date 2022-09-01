package it.bologna.ausl.documentgenerator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import it.bologna.ausl.documentgenerator.exceptions.Http400ResponseException;
import it.bologna.ausl.documentgenerator.exceptions.Http403ResponseException;
import it.bologna.ausl.documentgenerator.exceptions.Http500ResponseException;
import it.bologna.ausl.documentgenerator.exceptions.HttpInternautaResponseException;
import it.bologna.ausl.documentgenerator.exceptions.Sql2oSelectException;
import it.bologna.ausl.documentgenerator.utils.AziendaParamsManager;
import it.bologna.ausl.documentgenerator.utils.BabelUtils;
import it.bologna.ausl.documentgenerator.utils.GeneratorUtils;
import it.bologna.ausl.documentgenerator.utils.GeneratorUtils.SupportedArchiveTypes;
import it.bologna.ausl.estrattore.ExtractorCreator;
import it.bologna.ausl.estrattore.ExtractorResult;
import it.bologna.ausl.estrattore.exception.ExtractorException;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

/**
 *
 * @author spritz
 */
@Component
public class GeneratePE {

    private BabelUtils babelUtils;

    AziendaParamsManager aziendaParamsManager;

    @Autowired
    GeneratorUtils generatorUtils;

    @Autowired
    ObjectMapper objectMapper;

    @Value("${babel-suite.webapi.genera-protocollo-url}")
    private String generaProtocolloUrl;

    private static final Logger log = LoggerFactory.getLogger(GeneratePE.class);

    private MultipartFile documentoPrincipale;
    private Optional<List<MultipartFile>> allegati;
    private String applicazioneChiamante;
    private MongoWrapper mongo;
    private List<String> uuidAllegati = new ArrayList<>();
    private Integer annoDocumentoOrigineInt = null;
    private String responsabileProcedimento;
    private String codiceAzienda;
    private Integer idDocEsterno;
    private String numeroDocumentoOrigine;
    private String annoDocumentoOrigineString;
    private Map<String, Object> params;
    private boolean skipRecursiveExtraction = false;

    public GeneratePE() {
    }

    /**
     * setting dei parametri passati e controlli associati, prima di create il protocollo
     * @param cfUser
     * @param params
     * @param documentoPrincipale
     * @param allegati
     * @param aziendeParams
     * @param minIOActive
     * @param minIOConfig
     * @param skipRecursiveExtraction
     * @throws HttpInternautaResponseException
     * @throws IOException
     * @throws UnknownHostException
     * @throws MongoException
     * @throws MongoWrapperException
     */
    public void init(
            String cfUser,
            Map<String, Object> params,
            MultipartFile documentoPrincipale,
            Optional<List<MultipartFile>> allegati,
            Map<String, AziendaParametriJson> aziendeParams,
            Boolean minIOActive,
            Map<String, Object> minIOConfig,
            boolean skipRecursiveExtraction) throws HttpInternautaResponseException, IOException, UnknownHostException, MongoException, MongoWrapperException {
        this.allegati = allegati;
        this.aziendaParamsManager = new AziendaParamsManager(objectMapper, aziendeParams, minIOActive, minIOConfig);
        this.babelUtils = new BabelUtils(aziendaParamsManager, objectMapper);
        this.params = params;
        this.applicazioneChiamante = (String) params.get("applicazione_chiamante");
        this.idDocEsterno = (Integer) params.get("id_doc_esterno");
        if (applicazioneChiamante == null) {
            throw new Http400ResponseException("400", "il parametro del body applicazione_chiamante è obbligatorio");
        }

        String azienda = (String) params.get("azienda");
        if (azienda == null) {
            throw new Http400ResponseException("400", "il parametro del body azienda è obbligatorio");
        }
        this.codiceAzienda = azienda.substring(3);
        this.mongo = aziendaParamsManager.getStorageConnection(codiceAzienda);
        if (this.mongo == null) {
            throw new Http400ResponseException("400", "storage non trovato");
        }

        if (params != null && !params.isEmpty() && params.containsKey("responsabile_procedimento")) {
            this.responsabileProcedimento = params.get("responsabile_procedimento").toString();
        }
        if (this.responsabileProcedimento == null) {
            this.responsabileProcedimento = cfUser;
        }

        this.documentoPrincipale = documentoPrincipale;

        //il principale allegato non può essere di tipo estraibile
        if (!generatorUtils.isAcceptedMimeType(documentoPrincipale)) {

            throw new Http400ResponseException("400", "Attenzione: l'allegato '" + documentoPrincipale.getName()
                    + "' ha un mime-type non supportato dal sistema" + documentoPrincipale.getContentType());
        }
        this.skipRecursiveExtraction = skipRecursiveExtraction;
    }

    private void recursiveExtractAndUpload(MultipartFile allegato,
            List<Map<String, Object>> mapAllegati,
            File folderToSave)
            throws IOException, ExtractorException, ExtractorException,
            ExtractorException, Throwable {
        String nome = allegato.getOriginalFilename();
        File tmp = new File(folderToSave.getAbsolutePath()
                + System.getProperty("file.separator")
                + nome);

        log.info("nome path nuovo " + folderToSave.getAbsolutePath() + System.getProperty("file.separator"));
        log.info("allegato.getOriginalFilename " + allegato.getOriginalFilename());

        FileUtils.copyInputStreamToFile(allegato.getInputStream(), tmp);

        ExtractorCreator ec = new ExtractorCreator(tmp);
        if (ec.isExtractable()) {
            log.info("chiamo la extractAll su -->" + folderToSave.getAbsolutePath() + System.getProperty("file.separator") + allegato.getOriginalFilename());
            /* creo i file contenuti dagli archivi nella cartella temporanea
            del sistema new File(folderToSave+allegato.getOriginalFilename())
            NB: ec.extractAll() è ricorsiva */
            ArrayList<ExtractorResult> ecAll = ec.extractAll(folderToSave);

            for (ExtractorResult er : ecAll) {
                log.info(er.toString());
                try {
                    File file = new File(er.getPath());
                    InputStream fileDaPassare = new FileInputStream(file);
//                    log.info("upload del file? " + (SupportedArchiveTypes.contains(er.getMimeType())
//                            || SupportedMimeTypes.contains(er.getMimeType())));
                    log.info("upload del file? " + (SupportedArchiveTypes.contains(er.getMimeType())
                            || babelUtils.isSupportedMimeType(codiceAzienda, er.getMimeType())));
                    log.info("fileName: " + er.getFileName());
                    log.info("getMimeType: " + er.getMimeType());

                    // è di tipo accettato per il salvataggio sul db il contenuto del db?
                    Boolean ispdf = er.getMimeType().equals("application/pdf");
                    //file caricabili sul DB
                    if (SupportedArchiveTypes.contains(er.getMimeType())) {
                        mapAllegati.add(generatorUtils.uploadMongoISandJsonAllegato(mongo, fileDaPassare, er.getFileName(), false, er.getMimeType(), !ispdf));
                    } else if (babelUtils.isSupportedMimeType(codiceAzienda, er.getMimeType())) {
                        mapAllegati.add(generatorUtils.uploadMongoISandJsonAllegato(mongo, fileDaPassare, er.getFileName(), false, er.getMimeType(), !ispdf));
                    } else if (!ExtractorCreator.isSupportedMimyType(er.getMimeType())) {
                        throw new Http400ResponseException("400", "Attenzione: il file '" + er.getAntenati() + "\\" + er.getFileName() + " non ha un minetype accettablie.");
                    }
                } catch (Http400ResponseException q) {
                    generatorUtils.svuotaCartella(folderToSave.getAbsolutePath());
                    log.error("Il file non è di tipo accettato: " + er.getFileName(), q);
                    throw q;
                } catch (Exception e) {
                    generatorUtils.svuotaCartella(folderToSave.getAbsolutePath());
                    log.error("Errore nel caricamento su mongo  del file " + er.getFileName(), e);
                    throw new Http400ResponseException("400", "Attenzione: errore nel caricamento su mongo dell'allegato '" + er.getFileName() + ".");
                }
            }
        }
    }

    private List<Map<String, Object>> uploadAllegatiAndGetMap() throws IOException, Exception, Throwable {
        List<Map<String, Object>> mapAllegati = new ArrayList();
        // trasformo MultipartFile in InputStream
        InputStream inputStreamPrincipale = new BufferedInputStream(documentoPrincipale.getInputStream());
        //aggiungo al json
        mapAllegati.add(generatorUtils.uploadMongoISandJsonAllegato(mongo, inputStreamPrincipale, documentoPrincipale.getOriginalFilename(), true, documentoPrincipale.getContentType(), generatorUtils.isPdf(documentoPrincipale)));

        List<MultipartFile> allegatiList = allegati.orElse(Collections.emptyList());
        log.info("Allegati: " + allegatiList.size());

        File folderToSave = new File(System.getProperty("java.io.tmpdir") + "EstrazioneTemp" + System.getProperty("file.separator"));
        for (MultipartFile allegato : allegatiList) {
            log.info("Allegato = " + allegato.getName());

            // in questo punto verifico se posso estrarre i file da dentro ad alcuni tipo di file che ne possono contenenre altri
            //se il file singature non è accettato allora errore altrimenti continuo e setto il minetype perche non voglio che ci siano altri errori più avanti
            Boolean entra = false;
            if (allegato.getContentType().equals("application/octet-stream")) {
                byte[] bytes = allegato.getBytes();
                if (generatorUtils.signatureFileAccepted(bytes)) {
                    entra = true;
                }
            }

            if (!this.skipRecursiveExtraction && ExtractorCreator.isSupportedMimyType(allegato.getContentType())
                    || entra) {
                log.info("è estraibile: " + allegato.getName());
                recursiveExtractAndUpload(allegato, mapAllegati, folderToSave);

                // è un tipo di allegato accettabile?
            } else if (!generatorUtils.isAcceptedMimeType(allegato)) {
                log.error("allegato con formato non supportato: " + allegato.getName());
                throw new Http400ResponseException("400", "Attenzione: l'allegato '" + allegato.getName()
                        + "' ha un mime-type non supportato dal sistema" + allegato.getContentType());
            } else {
                // ci sono solo se il tipo di file non è estraibile ed è accettabile
                log.info("file da uploadare, normale file " + allegato.getContentType());
                log.info(allegato.getName());
                InputStream inputStreamAllegato = new BufferedInputStream(allegato.getInputStream());
                String allegatoFileName = allegato.getOriginalFilename();
                Boolean principale = false;
                String allegatoContentType = allegato.getContentType();
                // così creiamo il json da aggiungere alla lista degli allegati e carichiamo su mongo il file
                Map<String, Object> jsonAllegato = generatorUtils.uploadMongoISandJsonAllegato(mongo, inputStreamAllegato, allegatoFileName, principale, allegatoContentType, !generatorUtils.isPdf(allegato));
                mapAllegati.add(jsonAllegato);
                uuidAllegati.add((String) jsonAllegato.get("uuid_file"));
            }
            generatorUtils.svuotaCartella(folderToSave.getAbsolutePath());
        }
        return mapAllegati;
    }

    private String generateRandomUUIDString() {
        return UUID.randomUUID().toString();
    }

    private boolean isDocumentoGiaPresente() throws Sql2oSelectException {
        switch (applicazioneChiamante) {
            case "SIRER":
                return babelUtils.isDocumentoGiaPresenteByDocumentoEsterno(codiceAzienda,
                    applicazioneChiamante,
                    numeroDocumentoOrigine,
                    annoDocumentoOrigineInt);
            default:
                return babelUtils.isDocumentoGiaPresenteByIdDocEsterno(codiceAzienda,idDocEsterno);
        }

    }

    /**
     * creazione di un protocollo in entrata
     * @param idChiamata
     * @return n_protocollo_generato/anno_protocollo_generato di babel
     * @throws Throwable
     */
    public String create(String idChiamata) throws Throwable {
        // restituiamo n_protocollo_generato/anno_protocollo_generato di babel
        String result = "";
        String resultJson = "";
        String ID_CHIAMATA = idChiamata != null
                ? idChiamata : generateRandomUUIDString() + "_";
        log.info("ID_CHIAMATA" + ID_CHIAMATA);

        try {
            // verifichiamo che il doc non sia già protocollato
            if (isDocumentoGiaPresente()) {
                String message = String.format("E' già presente un documento con id_doc_esterno = %s"
                       ,  idDocEsterno);
                throw new Http500ResponseException("500", message);
            }

            List<Map<String, Object>> mapAllegati = uploadAllegatiAndGetMap();

            //log.info(mapAllegati.toJSONString());
            params.put("allegati", mapAllegati);

            params.put("ID_CHIAMATA", ID_CHIAMATA);
            // chiamo la web-api su Pico
            String urlChiamata = "";

            urlChiamata = aziendaParamsManager.getAziendaParam(codiceAzienda).getBabelSuiteWebApiUrl() + generaProtocolloUrl;  // altri ambienti
            // decommentare questo per i test in locale
//            urlChiamata = "http://localhost:8080/Procton/GeneraProtocolloDaExt"; // local

            Map<String, String> o = new HashMap();
            o.put("idapplicazione", "internauta_bridge");
            o.put("tokenapplicazione", "siamobrigidi");
            o.put("parametri", objectMapper.writeValueAsString(params));
            String bodyParam = objectMapper.writeValueAsString(o);
            log.info("JSON = " + bodyParam);

            okhttp3.RequestBody body = okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"),
                    bodyParam.getBytes("UTF-8"));

            log.info(urlChiamata);

            OkHttpClient client = new OkHttpClient.Builder()
                    .readTimeout(90, TimeUnit.SECONDS)
                    .build();

            Request requestg = new Request.Builder()
                    .url(urlChiamata)
                    .addHeader("X-HTTP-Method-Override", "nuovoPE")
                    .post(body)
                    .build();

            Response responseg = client.newCall(requestg).execute();
            log.info("reponse -> " + responseg.message());

            if (!responseg.isSuccessful()) {
                throw new Http500ResponseException("500", "Errore nella chiamata alla Web-api");
            }
            Map<String, Object> myResponse = new HashMap();
            try {
                myResponse = objectMapper.readValue(responseg.body().string(), new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception ex) {
                throw new Http500ResponseException("500", "Errore nel parsing della risposta");
            }

            if (myResponse.get("status").equals("OK")) {
                result = (String) myResponse.get("result");
                resultJson = (String) myResponse.get("resultJson");

            } else if (myResponse.get("status").equals("ERROR")) {
                if (myResponse.get("error_code").equals(500L)) {
                    throw new Http500ResponseException("500", (String) myResponse.get("error_message"));
                } else if (myResponse.get("error_code").equals(400L)) {
                    throw new Http400ResponseException("400", (String) myResponse.get("error_message"));
                } else if (myResponse.get("error_code").equals(403L)) {
                    throw new Http403ResponseException("403", (String) myResponse.get("error_message"));
                }
            }
            log.info(ID_CHIAMATA + String.format("L'utente %s ha generato il protocollo %s nell'azienda %s. applicazione_chiamante = %s",
                    responsabileProcedimento, result, codiceAzienda, applicazioneChiamante));
            return resultJson;
        } catch (HttpInternautaResponseException ex) {
            log.error(ex.getMessage());
            // qualsiasi errore viene dato, devo cancellare gli allegati
            log.error(ID_CHIAMATA + "Errore della Web-api. Cancello gli allegati che avevo salvato su Mongo");
            for (String currentUuid : uuidAllegati) {
                log.info(ID_CHIAMATA + "Cancellazione allegato con uuid: " + currentUuid);
                mongo.erase(currentUuid);
            }
            // rilancio l'eccezione
            throw ex;
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
            throw ex;
        }
    }
}
