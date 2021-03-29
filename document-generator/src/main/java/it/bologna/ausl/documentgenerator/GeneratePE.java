package it.bologna.ausl.documentgenerator;

import it.bologna.ausl.documentgenerator.exceptions.Http400ResponseException;
import it.bologna.ausl.documentgenerator.exceptions.Http403ResponseException;
import it.bologna.ausl.documentgenerator.exceptions.Http500ResponseException;
import it.bologna.ausl.documentgenerator.exceptions.HttpInternautaResponseException;
import it.bologna.ausl.documentgenerator.utils.AziendaParamsManager;
import it.bologna.ausl.documentgenerator.utils.BabelUtils;
import it.bologna.ausl.documentgenerator.utils.GeneratorUtils;
import it.bologna.ausl.documentgenerator.utils.GeneratorUtils.SupportedArchiveTypes;
import it.bologna.ausl.documentgenerator.utils.GeneratorUtils.SupportedMimeTypes;
import it.bologna.ausl.estrattore.ExtractorCreator;
import it.bologna.ausl.estrattore.ExtractorResult;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author spritz
 */
@Component
public class GeneratePE {

    @Autowired
    BabelUtils babelUtils;

    @Autowired
    AziendaParamsManager aziendaParamsManager;

    @Autowired
    GeneratorUtils generatorUtils;

    @Value("${babel-suite.webapi.genera-protocollo-url}")
    private String generaProtocolloUrl;

    private static final Logger log = LoggerFactory.getLogger(GeneratePE.class);

    private JSONObject jSONparametri;
    private MultipartFile documentoPrincipale;
    private Optional<List<MultipartFile>> allegati;
    private String applicazioneChiamante;
    private MongoWrapper mongo;
    private List<String> uuidAllegati = new ArrayList<>();
    private Integer annoDocumentoOrigineInt = null;
    private String responsabileProcedimento;
    private String codiceAzienda;
    private String numeroDocumentoOrigine;
    private String annoDocumentoOrigineString;
    private JSONParser jSONParser;

    public GeneratePE() {
    }

    public void init(String cfUser, String properties, MultipartFile documentoPrincipale, Optional<List<MultipartFile>> allegati) throws HttpInternautaResponseException {
        this.jSONParser = new JSONParser();

        try {
            this.jSONparametri = (JSONObject) jSONParser.parse(properties);
        } catch (ParseException ex) {
            log.error("properties passate non hanno il formato JSON corretto");
            throw new Http400ResponseException("400", "i parametri passati non hanno il formato JSON corretto");
        }

        this.applicazioneChiamante = (String) jSONparametri.get("applicazione_chiamante");
        if (applicazioneChiamante == null) {
            throw new Http400ResponseException("400", "il parametro del body applicazione_chiamante è obbligatorio");
        }

        String azienda = (String) jSONparametri.get("azienda");
        if (azienda == null) {
            throw new Http400ResponseException("400", "il parametro del body azienda è obbligatorio");
        }
        this.codiceAzienda = azienda.substring(3);
        this.mongo = aziendaParamsManager.getStorageConnection(codiceAzienda);
        if (this.mongo == null) {
            throw new Http400ResponseException("400", "storage non trovato");
        }

        this.numeroDocumentoOrigine = (String) jSONparametri.get("numero_documento_origine");
        if (numeroDocumentoOrigine == null) {
            throw new Http400ResponseException("400", "il parametro del body numero_documento_origine è obbligatorio");
        }

        Object annoDocumentoOrigine = jSONparametri.get("anno_documento_origine");

        if (annoDocumentoOrigine == null) {
            throw new Http400ResponseException("400", "il parametro del body anno_documento_origine è obbligatorio");
        }
        this.annoDocumentoOrigineString = annoDocumentoOrigine.toString();

        try {
            annoDocumentoOrigineInt = Integer.parseInt(annoDocumentoOrigineString);
        } catch (Exception ex) {
            throw new Http400ResponseException("400", "il parametro del body anno_documento_origine non è un intero");
        }

        this.responsabileProcedimento = (String) jSONparametri.get("responsabile_procedimento");
        if (this.responsabileProcedimento == null) {
            this.responsabileProcedimento = cfUser;
        }

        this.documentoPrincipale = documentoPrincipale;

        //il principale allegato non può essere di tipo estraibile
        if (!generatorUtils.isAcceptedMimeType(documentoPrincipale)) {

            throw new Http400ResponseException("400", "Attenzione: l'allegato '" + documentoPrincipale.getName()
                    + "' ha un mime-type non supportato dal sistema");
        }
    }

    public String create() throws Throwable {
        // restituiamo n_protocollo_generato/anno_protocollo_generato di babel
        String result = "";

        // TODO: passare ID_CHIAMATA come parametro e se è NULL usare UUID random
        String ID_CHIAMATA = UUID.randomUUID().toString() + "_";
        log.info("ID_CHIAMATA" + ID_CHIAMATA);

        try {
            // verifichiamo che il doc non sia già protocollato
            if (babelUtils.isDocumentoGiaPresente(codiceAzienda, applicazioneChiamante, numeroDocumentoOrigine, annoDocumentoOrigineInt)) {
                String message = String.format("E' già presente un documento con anno_documento_origine = %s e numero_documento_origine = %s"
                        + " e applicazione_chiamante = %s", this.annoDocumentoOrigineString, numeroDocumentoOrigine, applicazioneChiamante);
                throw new Http500ResponseException("500", message);
            }

            JSONArray jsonAllegati = new JSONArray();
            // trasformo MultipartFile in InputStream
            InputStream inputStreamPrincipale = new BufferedInputStream(documentoPrincipale.getInputStream());
            //aggiungo al json
            jsonAllegati.add(generatorUtils.uploadMongoISandJsonAllegato(mongo, inputStreamPrincipale, documentoPrincipale.getOriginalFilename(), true, documentoPrincipale.getContentType(), generatorUtils.isPdf(documentoPrincipale)));

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

                if (ExtractorCreator.isSupportedMimyType(allegato.getContentType()) || entra) {
                    log.info("è estraibile: " + allegato.getName());
                    String nome = allegato.getOriginalFilename();
                    File tmp = new File(folderToSave.getAbsolutePath() + System.getProperty("file.separator") + nome);

                    log.info("nome path nuovo " + folderToSave.getAbsolutePath() + System.getProperty("file.separator"));
                    log.info("allegato.getOriginalFilename " + allegato.getOriginalFilename());

                    FileUtils.copyInputStreamToFile(allegato.getInputStream(), tmp);

                    ExtractorCreator ec = new ExtractorCreator(tmp);
                    if (ec.isExtractable()) {
                        log.info("chiamo la extractAll su -->" + folderToSave.getAbsolutePath() + System.getProperty("file.separator") + allegato.getOriginalFilename());
                        ArrayList<ExtractorResult> ecAll = ec.extractAll(folderToSave); // creo i file contenuti dagli archivi nella cartella temporanea del sistema new File(folderToSave+allegato.getOriginalFilename())

                        for (ExtractorResult er : ecAll) {
                            log.info(er.toString());
                            try {
                                File file = new File(er.getPath());
                                InputStream fileDaPassare = new FileInputStream(file);
                                log.info("upload del file? " + (SupportedArchiveTypes.contains(er.getMimeType())
                                        || SupportedMimeTypes.contains(er.getMimeType())));
                                log.info("fileName: " + er.getFileName());
                                log.info("getMimeType: " + er.getMimeType());

                                // è di tipo accettato per il salvataggio sul db il contenuto del db?
                                Boolean ispdf = er.getMimeType().equals(SupportedMimeTypes.PDF.toString());
                                //file caricabili sul DB
                                if (SupportedArchiveTypes.contains(er.getMimeType())) {
                                    jsonAllegati.add(generatorUtils.uploadMongoISandJsonAllegato(mongo, fileDaPassare, er.getFileName(), false, er.getMimeType(), !ispdf));
                                } else if (SupportedMimeTypes.contains(er.getMimeType())) {
                                    jsonAllegati.add(generatorUtils.uploadMongoISandJsonAllegato(mongo, fileDaPassare, er.getFileName(), false, er.getMimeType(), !ispdf));
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
                    // è un tipo di allegato accettabile?
                } else if (!generatorUtils.isAcceptedMimeType(allegato)) {
                    log.error("allegato con formato non supportato: " + allegato.getName());
                    throw new Http400ResponseException("400", "Attenzione: l'allegato '" + allegato.getName()
                            + "' ha un mime-type non supportato dal sistema");
                } else {
                    // ci sono solo se il tipo di file non è estraibile ed è accettabile
                    log.info("file da uploadare, normale file " + allegato.getContentType());
                    log.info(allegato.getName());
                    InputStream inputStreamAllegato = new BufferedInputStream(allegato.getInputStream());
                    String allegatoFileName = allegato.getOriginalFilename();
                    Boolean principale = false;
                    String allegatoContentType = allegato.getContentType();
                    // così creiamo il json da aggiungere alla lista degli allegati e carichiamo su mongo il file
                    JSONObject jsonAllegato = generatorUtils.uploadMongoISandJsonAllegato(mongo, inputStreamAllegato, allegatoFileName, principale, allegatoContentType, !generatorUtils.isPdf(allegato));
                    jsonAllegati.add(jsonAllegato);
                    uuidAllegati.add((String) jsonAllegato.get("uuid_file"));
                }
                generatorUtils.svuotaCartella(folderToSave.getAbsolutePath());
            }
            log.info(jsonAllegati.toJSONString());
            jSONparametri.put("allegati", jsonAllegati);

            jSONparametri.put("ID_CHIAMATA", ID_CHIAMATA);
            // chiamo la web-api su Pico
            String urlChiamata = "";

            //urlChiamata = "http://localhost:8080/Procton/GeneraProtocolloDaExt"; // local
            // usare questo per i casi reali (non test in locale)
            urlChiamata = aziendaParamsManager.getAziendaParam(codiceAzienda).getBabelSuiteWebApiUrl() + generaProtocolloUrl;  // altri ambienti

            JSONObject o = new JSONObject();
            o.put("idapplicazione", "internauta_bridge");
            o.put("tokenapplicazione", "siamobrigidi");
            o.put("parametri", jSONparametri.toString());

            log.info("JSON = " + o.toString());

            okhttp3.RequestBody body = okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"),
                    o.toString().getBytes("UTF-8"));

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
            JSONObject myResponse = new JSONObject();
            try {
                myResponse = (JSONObject) jSONParser.parse(responseg.body().string());
            } catch (ParseException ex) {
                throw new Http500ResponseException("500", "Errore nel parsing della risposta");
            }

            if (myResponse.get("status").equals("OK")) {
                result = (String) myResponse.get("result");
            } else if (myResponse.get("status").equals("ERROR")) {
                if (myResponse.get("error_code").equals(500L)) {
                    throw new Http500ResponseException("500", (String) myResponse.get("error_message"));
                } else if (myResponse.get("error_code").equals(400L)) {
                    throw new Http400ResponseException("400", (String) myResponse.get("error_message"));
                } else if (myResponse.get("error_code").equals(403L)) {
                    throw new Http403ResponseException("403", (String) myResponse.get("error_message"));
                }
            }
            log.info(ID_CHIAMATA + String.format("L'utente %s ha generato il protocollo %s nell'azienda %s. applicazione_chiamante = %s, numero_documento_origine = %s, anno_documento_origine = %s",
                    responsabileProcedimento, result, codiceAzienda, applicazioneChiamante, numeroDocumentoOrigine, annoDocumentoOrigineString));
            return result;
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
