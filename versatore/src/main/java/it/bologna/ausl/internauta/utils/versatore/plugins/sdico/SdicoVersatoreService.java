package it.bologna.ausl.internauta.utils.versatore.plugins.sdico;

import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.model.entities.scripta.Registro;
import it.bologna.ausl.model.entities.scripta.RegistroDoc;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.persistence.EntityManager;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Andrea
 */
@Component
public class SdicoVersatoreService extends VersatoreDocs {

    private static final Logger log = LoggerFactory.getLogger(SdicoVersatoreService.class);
    private static final String SDICO_VERSATORE_SERVICE = "SdicoVersatoreService";
    private static final String SDICO_LOGIN_URI = "SdicoLoginURI";
    private static final String SDICO_SERVIZIO_VERSAMENTO_URI = "sdicoServizioVersamentoURI";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Autowired
    VersatoreHttpClientConfiguration versatoreHttpClientConfiguration;

    private String sdicoLoginURI, sdicoServizioVersamentoURI;

    @Override
    public void init(VersatoreConfiguration versatoreConfiguration) {
        super.init(versatoreConfiguration);
        Map<String, Object> versatoreConfigurationMap = this.versatoreConfiguration.getParams();
        Map<String, Object> sdicoServiceConfiguration = (Map<String, Object>) versatoreConfigurationMap.get(SDICO_VERSATORE_SERVICE);
        sdicoLoginURI = sdicoServiceConfiguration.get(SDICO_LOGIN_URI).toString();
        sdicoServizioVersamentoURI = sdicoServiceConfiguration.get(SDICO_SERVIZIO_VERSAMENTO_URI).toString();
        log.info("SDICO login URI: {}", sdicoLoginURI);
        log.info("SDICO servizio versamento URI: {}", sdicoServizioVersamentoURI);
    }

    @Override
    protected VersamentoDocInformation versaImpl(VersamentoDocInformation versamentoInformation) throws VersatoreProcessingException {
        //OkHttpClient okHttpClient = versatoreHttpClientConfiguration.getHttpClientManager().getOkHttpClient();
        //versamentoInformation.setMetadatiVersati(versaDocumentoSDICO(versamentoInformation));
        
        return versamentoInformation;
    }

   /* public String versa(String documento) throws IOException {

        log.info("Sono entrato in versa");

        String token = getJWT();

        //FileInputStream fstream = new FileInputStream("C:\\tmp\\metadati.xml");
        FileInputStream fstreamAllegato = new FileInputStream("C:\\tmp\\Documento_di_prova.pdf");
        //qui creo il documentBuilder
        //String result = IOUtils.toString(fstream, StandardCharsets.UTF_8);
        log.info("XML:\n" + documento);
        
        // inizializzazione http client
        OkHttpClient okHttpClient = new OkHttpClient()
                .newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();
        
        // Conversione file metadati.xml da inputstream to byte[]
        byte[] fileMetadati = IOUtils.toByteArray(documento);
        // Conversione file pdf da inputstream to byte[]
        byte[] allegato = IOUtils.toByteArray(fstreamAllegato);
        // creazione di body multipart
        MultipartBody.Builder buildernew = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "metadati.xml", RequestBody.create(MediaType.parse("application/xml"), fileMetadati));
        buildernew.addFormDataPart("file", "Documento_di_prova.pdf", RequestBody.create(MediaType.parse("application/pdf"), allegato));
        MultipartBody requestBody = buildernew.build();
        // richiesta
        Request request = new Request.Builder()
                .url("https://par.collaudo.regione.veneto.it/serviziPar/rest/versamento")
                .addHeader("Authorization", token)
                //.addHeader("Content Type", "multipart/form-data")
                .post(requestBody)
                .build();
        Response response = okHttpClient.newCall(request).execute();
        
         
        log.info(Integer.toString(response.code()));
        log.info(response.message());

        JSONObject jsonObject = new JSONObject(response.body().string());

        return jsonObject.toString();

    }*/

    //o lo chiamiamo dentro il versa o restituisce il documento xml
    public String versaDocumentoSDICO(VersamentoDocInformation versamentoDocInformation, EntityManager entityManager) {
        log.info("Inizio con il versamento del doc: " + Integer.toString(versamentoDocInformation.getIdDoc()));
        Integer idDoc = versamentoDocInformation.getIdDoc();
        
        Doc doc = entityManager.find(Doc.class, idDoc);
        DocDetail docDetail = entityManager.find(DocDetail.class,idDoc);
        
        
        
        
        log.info(doc.getTipologia().toString());
        
        Archivio archivio = entityManager.find(Archivio.class, versamentoDocInformation.getIdArchivio());
        
        //come arrivo al registro????
        List<RegistroDoc> listaRegistri = doc.getRegistroDocList();
        Registro registro = new Registro();
        if (listaRegistri.size() != 0) {
            registro = listaRegistri.get(0).getIdRegistro();
        } 
        
        List<DocDetailInterface.Firmatario> listaFirmatari = docDetail.getFirmatari();
        List<Persona> firmatari = new ArrayList<>();
        for (DocDetailInterface.Firmatario firmatario : listaFirmatari) {
            Persona p = entityManager.find(Persona.class, firmatario.getIdPersona());
            firmatari.add(p);
        }
        
        Map<String, Object> parametriVersamento = versamentoDocInformation.getParams();

        VersamentoBuilder versamentoBuilder = new VersamentoBuilder();

        switch (doc.getTipologia()) {
            case DETERMINA: {
                DeteBuilder db = new DeteBuilder(docDetail);
                //documento = db.build();
                break;
            }
            case DELIBERA: {
                DeliBuilder db = new DeliBuilder(doc, docDetail, archivio, registro, firmatari, parametriVersamento);
                versamentoBuilder = db.build();
                break;
            }
            case RGPICO: {
                RgPicoBuilder rb = new RgPicoBuilder(docDetail);
                //versamentoBuilder = rb.build();
                break;
            }
            default:
                throw new AssertionError("Tipologia documentale non presente");
        }

        /*
        try {
            FileWriter w;
            w = new FileWriter("C:\\tmp\\metadati.xml");

            BufferedWriter b;
            b = new BufferedWriter(w);

            b.write(versamentoBuilder.toString());
            b.flush();
        } catch (Exception e) {
            System.err.println("File non presente");
        }
         */
        String documento = versamentoBuilder.toString();

        return documento;

    }

    public String getJWT() throws IOException {

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
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient = buildNewClient(okHttpClient);
        String sdicoLoginURI = "https://par.collaudo.regione.veneto.it/serviziPar/rest/login";
        String token = null;
        String json = "{\"username\":\"AZERO01\",\"password\":\"AZERO01\"}";
        RequestBody body = RequestBody.create(JSON, json);

        Request request = new Request.Builder()
                .url(sdicoLoginURI)
                .post(body)
                .build();

        Response response = okHttpClient.newCall(request).execute();
        JSONObject jsonObject = new JSONObject(response.body().string());
        return (String) jsonObject.get("token");
    }

    // TODO: metodo da togliere quando si userà ersatoreHttpClientConfiguration
    private OkHttpClient buildNewClient(OkHttpClient client) {
        X509TrustManager x509TrustManager = new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                log.info("getAcceptedIssuers =============");
                X509Certificate[] empty = {};
                return empty;
            }

            @Override
            public void checkClientTrusted(
                    X509Certificate[] certs, String authType) {
                log.info("checkClientTrusted =============");
            }

            @Override
            public void checkServerTrusted(
                    X509Certificate[] certs, String authType) {
                log.info("checkServerTrusted =============");
            }
        };
        SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        String tlsString = "TLSv1.2";               //  "TLS"
        try {
            SSLContext sslContext = SSLContext.getInstance(tlsString);
            sslContext.init(null, new TrustManager[]{getX509TrustManager()}, new SecureRandom());
            socketFactory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException ex) {
            log.error(ex.getMessage());
        } catch (KeyManagementException ex) {
            log.error(ex.getMessage());
        }
        client = client.newBuilder()
                .sslSocketFactory(socketFactory, x509TrustManager)
                .connectTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();
        log.info("Client builded with SSLContext " + tlsString);
        return client;
    }

    // TODO: metodo da togliere quando si userà ersatoreHttpClientConfiguration
    private X509TrustManager getX509TrustManager() {
        return new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                log.info("getAcceptedIssuers =============");
                return null;
            }

            @Override
            public void checkClientTrusted(
                    X509Certificate[] certs, String authType) {
                log.info("checkClientTrusted =============");
            }

            @Override
            public void checkServerTrusted(
                    X509Certificate[] certs, String authType) {
                log.info("checkServerTrusted =============");
            }
        };
    }

}
