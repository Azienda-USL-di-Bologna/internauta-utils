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
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QDocDetail;
import it.bologna.ausl.model.entities.scripta.QRegistro;
import it.bologna.ausl.riversamento.builder.IdentityFile;
import it.bologna.ausl.riversamento.sender.PaccoFile;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;

//TODO togliere import inutili
/**
 *
 * @author Andrea
 */
@Component
public class SdicoVersatoreService extends VersatoreDocs {

    private static final Logger log = LoggerFactory.getLogger(SdicoVersatoreService.class);
    private static final String SDICO_VERSATORE_SERVICE = "SdicoVersatoreService";
    private static final String WS_OK = "WS_OK";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private String sdicoLoginURI, sdicoServizioVersamentoURI;

    //TODO chiedere se funziona così
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
        String response = (String) mappaResultAndAllegati.get("response");
        String xmlVersato = (String) mappaResultAndAllegati.get("xmlVersato");
        List<VersamentoAllegatoInformation> versamentiAllegatiInformationList = (List<VersamentoAllegatoInformation>) mappaResultAndAllegati.get("versamentiAllegatiInformation");

        //TODO capire se sono da vagliare le casisitiche degli altri statoVersamento sia di VersamentoDocInformation che dei VersamentoAllegatoInformation
        //Imposto i dati del DocInformation con i risultati
        versamentoDocInformation.setMetadatiVersati(xmlVersato);
        versamentoDocInformation.setDataVersamento(ZonedDateTime.now());
        SdicoResponse sdicoResponse = new SdicoResponse();
        if (response != null && !response.equals("")) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                sdicoResponse = objectMapper.readValue(response, SdicoResponse.class);
                // Controllo l'esito del versamento
                if (sdicoResponse.getResponseCode().equals(WS_OK)) {
                    versamentoDocInformation.setRapporto(response);
                    versamentoDocInformation.setStatoVersamentoPrecedente(versamentoDocInformation.getStatoVersamento());
                    versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.VERSATO);
                    for (VersamentoAllegatoInformation versamentoAllegatoInformation : versamentiAllegatiInformationList) {
                        versamentoAllegatoInformation.setStatoVersamento(Versamento.StatoVersamento.VERSATO);
                    }
                } else {
                    versamentoDocInformation.setRapporto(response);
                    versamentoDocInformation.setCodiceErrore(sdicoResponse.getResponseCode());
                    versamentoDocInformation.setDescrizioneErrore(sdicoResponse.getErrorMessage());
                    versamentoDocInformation.setStatoVersamentoPrecedente(versamentoDocInformation.getStatoVersamento());
                    versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
                    //TODO cosa mettere per forzabile???
                    for (VersamentoAllegatoInformation versamentoAllegatoInformation : versamentiAllegatiInformationList) {
                        versamentoAllegatoInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
                    }
                    log.error("SDICO ha risposto con il seguente errore: " + sdicoResponse.getErrorMessage());
                }
                versamentoDocInformation.setVersamentiAllegatiInformations(versamentiAllegatiInformationList);
            } catch (JsonProcessingException ex) {
                log.error("Errore nel parsing della response arrivata da SDICO");
            }
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
    public Map<String, Object> versaDocumentoSDICO(VersamentoDocInformation versamentoDocInformation) {
        log.info("Inizio con il versamento del doc: " + Integer.toString(versamentoDocInformation.getIdDoc()));
        //preparo i dati
        Integer idDoc = versamentoDocInformation.getIdDoc();
        Map<String, Object> risultatoEVersamentiAllegati = new HashMap<>();
        Doc doc = entityManager.find(Doc.class, idDoc);
        DocDetail docDetail = entityManager.find(DocDetail.class, idDoc);
        Archivio archivio = new Archivio();
        if (doc.getTipologia() != DocDetailInterface.TipologiaDoc.RGPICO) {
            archivio = entityManager.find(Archivio.class, versamentoDocInformation.getIdArchivio());
        }

        List<RegistroDoc> listaRegistri = doc.getRegistroDocList();
        Registro registro = new Registro();
        for (RegistroDoc reg : listaRegistri) {
            if (reg.getIdRegistro().getAttivo() && reg.getIdRegistro().getUfficiale()) {
                registro = reg.getIdRegistro();
            }
        }

        List<DocDetailInterface.Firmatario> listaFirmatari = docDetail.getFirmatari();
        List<Persona> firmatari = new ArrayList<>();
        if (listaFirmatari
                != null) {
            for (DocDetailInterface.Firmatario firmatario : listaFirmatari) {
                Persona p = entityManager.find(Persona.class, firmatario.getIdPersona());
                firmatari.add(p);
            }
        }
        Map<String, Object> parametriVersamento = versamentoDocInformation.getParams();
        String username = (String) parametriVersamento.get("username");
        String password = (String) parametriVersamento.get("password");

        //in basa alla tipologia di documento instanzio la relativa classe che ne costruisce i metadati
        VersamentoBuilder versamentoBuilder = new VersamentoBuilder();

        log.info(
                "Creo i metadati di: " + doc.getTipologia() + " id " + doc.getId() + ", " + doc.getOggetto());
        //TODO si potrebbe catchare un errore di nullpointer
        //TODO levare parametri passati alle funzioni che non servono
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
                ZonedDateTime dataIniziale = getDataRegistrazioneDaNumeroDiRegistrazioneEAnno(numeroIniziale);
                ZonedDateTime dataFinale = getDataRegistrazioneDaNumeroDiRegistrazioneEAnno(numeroFinale);
                RgPicoBuilder rb = new RgPicoBuilder(doc, docDetail, registro, firmatari, parametriVersamento, numeroIniziale, numeroFinale, dataIniziale, dataFinale);
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
                DocumentoGEDIBuilder dgb = new DocumentoGEDIBuilder(doc, docDetail, archivio, registro, firmatari, parametriVersamento);
                versamentoBuilder = dgb.build();
                break;
            }
            default:
                //TODO da togliere?
                throw new AssertionError("Tipologia documentale non presente");
        }
        
        log.info("accedo ai dati degli allegati e li inserisco nell'XML");
        List<Allegato> allegati = doc.getAllegati();
        AllegatiBuilder allegatiBuild = new AllegatiBuilder(versatoreRepositoryConfiguration);
        Map<String, Object> mappaDatiAllegati = allegatiBuild.buildMappaAllegati(doc, docDetail, allegati, versamentoBuilder);
        List<IdentityFile> identityFiles = (List<IdentityFile>) mappaDatiAllegati.get("identityFiles");
        List<VersamentoAllegatoInformation> versamentiAllegatiInformationList = (List<VersamentoAllegatoInformation>) mappaDatiAllegati.get("versamentiAllegatiInfo");
        String metadati = versamentoBuilder.toString();

        log.warn("XML da versare: \n" + metadati);
        risultatoEVersamentiAllegati.put("xmlVersato", metadati);
        risultatoEVersamentiAllegati.put("versamentiAllegatiInformation", versamentiAllegatiInformationList);

        // --Sezione di collegamento con SDICO e versamento--
        try {
            List<PaccoFile> paccoFiles = creazionePaccoFile(identityFiles);
            //effettuo il login a SDICO per ricevere il token
            log.info("Effettuo il login");
            String token = "";
            token = getJWT(username, password, sdicoLoginURI);

            // inizializzazione http client
            //TODO si dovrà poi usare la configurazione già instanziata
//            OkHttpClient okHttpClient = new OkHttpClient()
//                    .newBuilder()
//                    .connectTimeout(60, TimeUnit.SECONDS)
//                    .build();
//TODO istanziazione di HTTPClient ereditata vedere se funziona
            OkHttpClient okHttpClient = versatoreHttpClientConfiguration.getHttpClientManager().getOkHttpClient();

            // Conversione file metadati.xml da inputstream to byte[] e aggiungo al multipart
            byte[] fileMetadati = IOUtils.toByteArray(metadati);
            MultipartBody.Builder buildernew = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "metadati.xml", RequestBody.create(MediaType.parse("application/xml"), fileMetadati));

            // Conversione degli allegati da inputstream to byte[] e aggiungo al multipart
            log.info("Ciclo i file...");
            if (paccoFiles.size() > 0) {
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
                    risultatoEVersamentiAllegati.put("response", resBodyString);
                } else {
                    log.error("ERROR: message = " + resp.message());
                    String resBodyString = resp.body().string();
                    log.error("Body: " + resBodyString);
                    log.error(resp.toString());
                    risultatoEVersamentiAllegati.put("response", null);
                }
                resp.close(); // chiudo la response
            } catch (Throwable ex) {
                log.error("Errore chiamata riversamento", ex);
                ex.printStackTrace();
                risultatoEVersamentiAllegati.put("response", null);
            }

            return risultatoEVersamentiAllegati;

        } catch (IOException ex) {
            log.error("Errore nell'invio del versamento", ex);
        }
        
        return risultatoEVersamentiAllegati;
    }

    public String getJWT(String username, String password, String sdicoLoginURI) throws IOException {

        /**
         * ora si usa una istanza di okhttp fissa per i test, poi si dovrà usare
         * questo: OkHttpClient okHttpClient =
         * versatoreHttpClientConfiguration.getHttpClientManager().getOkHttpClient();
         *
         * non viene usata dall'interno di Versatore perchè questa deve essere
         * settata dall'applicazione nella quale il modulo è inserito
         * (attualmente internauta) tramite il metodo setHttpClientManager
         */
        // 
//        OkHttpClient okHttpClient = new OkHttpClient();
//TODO istanziazione di HTTPClient ereditata vedere se funziona
        OkHttpClient okHttpClient = versatoreHttpClientConfiguration.getHttpClientManager().getOkHttpClient();
//        okHttpClient = buildNewClient(okHttpClient);
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

    // TODO: metodo da togliere quando si userà versatoreHttpClientConfiguration
//    private OkHttpClient buildNewClient(OkHttpClient client) {
//        X509TrustManager x509TrustManager = new X509TrustManager() {
//            @Override
//            public X509Certificate[] getAcceptedIssuers() {
//                log.info("getAcceptedIssuers =============");
//                X509Certificate[] empty = {};
//                return empty;
//            }
//
//            @Override
//            public void checkClientTrusted(
//                    X509Certificate[] certs, String authType) {
//                log.info("checkClientTrusted =============");
//            }
//
//            @Override
//            public void checkServerTrusted(
//                    X509Certificate[] certs, String authType) {
//                log.info("checkServerTrusted =============");
//            }
//        };
//        SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
//        String tlsString = "TLSv1.2";               //  "TLS"
//        try {
//            SSLContext sslContext = SSLContext.getInstance(tlsString);
//            sslContext.init(null, new TrustManager[]{getX509TrustManager()}, new SecureRandom());
//            socketFactory = sslContext.getSocketFactory();
//        } catch (NoSuchAlgorithmException ex) {
//            log.error(ex.getMessage());
//        } catch (KeyManagementException ex) {
//            log.error(ex.getMessage());
//        }
//        client = client.newBuilder()
//                .sslSocketFactory(socketFactory, x509TrustManager)
//                .connectTimeout(600, TimeUnit.SECONDS)
//                .readTimeout(600, TimeUnit.SECONDS)
//                .writeTimeout(600, TimeUnit.SECONDS)
//                .build();
//        log.info("Client builded with SSLContext " + tlsString);
//        return client;
//    }
    // TODO: metodo da togliere quando si userà versatoreHttpClientConfiguration
//    private X509TrustManager getX509TrustManager() {
//        return new X509TrustManager() {
//            @Override
//            public X509Certificate[] getAcceptedIssuers() {
//                log.info("getAcceptedIssuers =============");
//                return null;
//            }
//
//            @Override
//            public void checkClientTrusted(
//                    X509Certificate[] certs, String authType) {
//                log.info("checkClientTrusted =============");
//            }
//
//            @Override
//            public void checkServerTrusted(
//                    X509Certificate[] certs, String authType) {
//                log.info("checkServerTrusted =============");
//            }
//        };
//    }
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
    private ZonedDateTime getDataRegistrazioneDaNumeroDiRegistrazioneEAnno(String numeroEAnno) {
        String[] parts = numeroEAnno.split("/");
        Integer numeroRegistrazione = Integer.parseInt(parts[0].replaceAll("^0+", ""));
        Integer anno = Integer.parseInt(parts[1]);
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        ZonedDateTime dataRegistrazione = jPAQueryFactory
                .select(QDocDetail.docDetail.dataRegistrazione)
                .from(QDocDetail.docDetail)
                .where(QDocDetail.docDetail.numeroRegistrazione.eq(numeroRegistrazione)
                        .and(QDocDetail.docDetail.annoRegistrazione.eq(anno))
                        .and(QDocDetail.docDetail.tipologia.eq(DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA)
                                .or(QDocDetail.docDetail.tipologia.eq(DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA))))
                .fetchOne();

        return dataRegistrazione;
    }

    //TODO se serve
    private Archivio getArchivioVarsato(Doc documentoVersato, Archivio archivioRadice) {
        for (ArchivioDoc archivioDoc : documentoVersato.getArchiviDocList()) {
            if (archivioDoc.getIdArchivio().getIdArchivioRadice().equals(archivioRadice.getId())) {
                return archivioDoc.getIdArchivio();
            }
        }
        return archivioRadice;
    }

}
