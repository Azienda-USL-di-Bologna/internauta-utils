package it.bologna.ausl.internauta.utils.versatore.plugins.sdico;

import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.FormBody;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Andrea
 */
@Component
public class SdicoVersatoreService extends VersatoreDocs{
    
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
        OkHttpClient okHttpClient = versatoreHttpClientConfiguration.getHttpClientManager().getOkHttpClient();
        return null;
    }
    
    public void versa() throws FileNotFoundException, IOException {
        
        String token = getJWT();
        
        FileInputStream fstream = new FileInputStream("C:\\tmp\\new_metadati_provvedimento.xml");
        FileInputStream fstreamAllegato = new FileInputStream("C:\\tmp\\Documento_di_prova.pdf");
        String result = IOUtils.toString(fstream, StandardCharsets.UTF_8);
        log.info(result);
        
        // inizializzazione http client
        OkHttpClient okHttpClient = new OkHttpClient()
            .newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .build();
        
        // Conversione file metadati.xml da inputstream to byte[]
        byte[] fileMetadati = IOUtils.toByteArray(fstream);
        
        // Conversione file pdf da inputstream to byte[]
        byte[] allegato = IOUtils.toByteArray(fstreamAllegato);
        
        // creazione di body multipart
        MultipartBody.Builder buildernew = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "metadati.xml",RequestBody.create(MediaType.parse("application/xml"), fileMetadati)); 
               
        //buildernew.addFormDataPart("file", "Documento_di_prova.pdf", RequestBody.create(MediaType.parse("application/pdf"), allegato));
    
        MultipartBody requestBody = buildernew.build();  
        
        // richiesta
        Request request = new Request.Builder()
            .url("https://par.collaudo.regione.veneto.it/serviziPar/rest/versamento")
            .addHeader("Authorization", "Bearer " + token)
            .post(requestBody)
            .build();
        
        Response response = okHttpClient.newCall(request).execute();
        log.info(response.body().string());
    }


    public void getDete() {
        
        DeteBuilder db = new DeteBuilder();
        db.build();
    }

    
    
    public String getJWT() throws IOException {
        
        /**
         * ora si usa una istanza di okhttp fissa per i test, poi si dovrà usare questo:
         * OkHttpClient okHttpClient = versatoreHttpClientConfiguration.getHttpClientManager().getOkHttpClient();
         * 
         * non viene usata dall'interno di Versatore perchè questa deve essere settata dall'applicazione nella quale 
         * il modulo è inserito (attualmente internauta) tramite il metodo setHttpClientManager
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
