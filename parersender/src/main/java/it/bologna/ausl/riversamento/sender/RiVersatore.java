package it.bologna.ausl.riversamento.sender;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.XPathContext;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.Nullable;

/**
 *
 * @author andrea
 */
public class RiVersatore {
//http://parer.ibc.regione.emilia-romagna.it/servizi/documenti-servizi/specifiche-tecniche-dei-servizi-di-versamento/at_download/file/Specifiche%20tecniche%20versione%201punto3%20errata%20corrige.zip
//
//indirizzo web service: https://parerlab.regione.emilia-romagna.it/sacerpre/VersamentoSync
//
//Ambiente: PARER_TEST
//Ente: AUSL_BO
//Struttura: ASL_BO

    private static final Logger log = LogManager.getLogger(RiVersatore.class);

    /**
     * @param args the command line arguments
     */
//    private HttpClient c;
    private String uri, username, password;

    static final String usr = "gedi_ausl_bo";
    static final String psw = "mt14AS02bo10";

    private Pacco pacco;

    private OkHttpClient client;

    public RiVersatore() {
        this.uri = "https://gedi_ausl_bo:mt14AS02bo10@parer-pre.regione.emilia-romagna.it/sacer/VersamentoSync";
        this.username = usr;
        this.password = psw;
        // HttpClientBuilder cb = HttpClientBuilder.create();
        //this.c = new DefaultHttpClient();//cb.build();
//        c.getConnectionManager().getSchemeRegistry().register(
//            new Scheme("https", SSLSocketFactory.getSocketFactory(), 443)
//        );
        buildNewClient();

    }

    private void buildNewClient() {
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
            log.error(ex);
        } catch (KeyManagementException ex) {
            log.error(ex);
        }
        this.client = this.client.newBuilder()
                .sslSocketFactory(socketFactory, x509TrustManager)
                .connectTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();
        log.info("Client builded with SSLContext " + tlsString);
    }

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

//    private static void trustAllSSL(HttpClient c) {
//        SSLContext sslContext;
//        try {
//            sslContext = SSLContext.getInstance("TLS");
//
//            // set up a TrustManager that trusts everything
//            try {
//                sslContext.init(null,
//                        new TrustManager[]{new X509TrustManager() {
//                        @Override
//                        public X509Certificate[] getAcceptedIssuers() {
//                            log.info("getAcceptedIssuers =============");
//                            return null;
//                        }
//
//                        @Override
//                        public void checkClientTrusted(
//                                X509Certificate[] certs, String authType) {
//                            log.info("checkClientTrusted =============");
//                        }
//
//                        @Override
//                        public void checkServerTrusted(
//                                X509Certificate[] certs, String authType) {
//                            log.info("checkServerTrusted =============");
//                        }
//                    }}, new SecureRandom());
//            } catch (KeyManagementException e) {
//                log.error(e);
//            }
//            SSLSocketFactory ssf = new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//            ClientConnectionManager ccm = c.getConnectionManager();
//            SchemeRegistry sr = ccm.getSchemeRegistry();
//            sr.register(new Scheme("https", 443, ssf));
//
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }
//
//    }
    public RiVersatore(String uri, String username, String password, String versione) {
        this.uri = uri;
        this.username = username;
        this.password = password;

        this.pacco = new Pacco();
        //VECCHIO LOGIN - pacco.setLoginName("versatore_aslbo");
        pacco.setLoginName(this.username);
        // VECCHIA PASSWORD - pacco.setPassword("password");
        pacco.setPassword(this.password);
        pacco.setVersione(versione);
//        HttpClientBuilder cb = HttpClientBuilder.create();
//        this.c = new DefaultHttpClient();//cb.build();
//        c.getConnectionManager().getSchemeRegistry().register(
//                new Scheme("https", SSLSocketFactory.getSocketFactory(), 443)
//        );
//        trustAllSSL(c);
        this.client = new OkHttpClient();
        buildNewClient();
    }

    public String riversaNuovo(Pacco p) throws IOException {
        System.out.println("XXXXXX    Questo lo loggo     XXXXXX");
        log.info("Entrato in riversaNuovo...");
        String response = null;
//        OkHttpClient okHttpClient = this.client
//            .newBuilder()
//            .connectTimeout(360, TimeUnit.SECONDS)
//            .build();
        List<NameValuePair> formparams = p.getFormValues();
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        log.info("Cliclo le chiavi del form...");
        for (NameValuePair nvp : formparams) {
            if (nvp.getValue() != null) {
                log.info(nvp.getName() + " : " + nvp.getValue());
                builder.addFormDataPart(nvp.getName(), nvp.getValue());
            } else {
                log.info(nvp.getName() + " non valorizzato!");
            }
        }

        log.info("Cliclo i file...");
        if (p.getFiles() != null) {
            for (PaccoFile a : p.getFiles()) {
                log.info("Inserisco nel body il file " + a.getId() + ", " + a.getFileName());
                InputStream is = a.getInputStream();
                byte[] bytes = IOUtils.toByteArray(is);
                is.close();
                builder.addFormDataPart(a.getId(), a.getFileName(),
                        RequestBody.create(bytes, okhttp3.MediaType.parse(a.getMime())));
            }
        }
        log.info("Buildo il MultiPart...");
        MultipartBody multipartBody = builder.build();

        log.info("Buildo la request...");
        Request request = new Request.Builder()
                .url(this.uri) // pass the url endpoint of api
                .post(multipartBody) // pass the mulipart object we just created having data
                .build();
        log.info("Uri " + this.uri);
        log.info("Effettuo chiamata.... ");
        try (Response resp = this.client.newCall(request).execute()) {
            if (resp.isSuccessful()) {
                log.info("Message" + resp.message());
                String resBodyString = resp.body().string();
                log.info(resBodyString);
                response = resBodyString;
            } else {
                String message = resp.message();
                log.error("ERROR: message = " + resp.message());
                String resBodyString = resp.body().string();
                log.error(resBodyString);
                log.error(resp.toString());
            }
            resp.close(); // Close respons
        } catch (Throwable ex) {
            log.error("Errore chiamata riversamento", ex);
            ex.printStackTrace();
        }
        return response;
    }

//    public String riversa(Pacco p) throws ClientProtocolException, IOException {
//
//        //MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
//        //entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
//        MultipartEntity me = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
//        List<NameValuePair> formparams = p.getFormValues();
//
//        log.info("Building name values pairs to post:");
//        for (NameValuePair nvp : formparams) {
//            if (nvp.getValue() != null) {
//                log.info(nvp.getName() + " : " + nvp.getValue());
//                //entityBuilder.addPart(nvp.getName(), new StringBody(nvp.getValue(), ContentType.TEXT_PLAIN.getCharset()));
//                me.addPart(nvp.getName(), new StringBody(nvp.getValue()));
//            } else {
//                log.info(nvp.getName() + " non valorizzato!");
//            }
//        }
//
//        if (p.getFiles() != null) {
//            for (PaccoFile a : p.getFiles()) {
//                //entityBuilder.addPart(a.getId().toString(), new InputStreamBody(a.getInputStream(),a.getFileName()));
//                //entityBuilder.addPart(a.getId().toString(), new FileBody(a.getFile()));
//                me.addPart(a.getId().toString(), new InputStreamBody(a.getInputStream(), a.getFileName()));
//                log.info("Attached file: " + a.getFileName());
//
//            }
//        }
//
//        HttpPost httppost = new HttpPost(this.uri);
//        //httppost.setEntity(entityBuilder.build());
//        httppost.setEntity(me);
//        try {
//            log.info("Sending mail to: " + this.uri);
//            HttpResponse response = c.execute(httppost);
//            HttpEntity res = response.getEntity();
//            if (handleStatus(response)) {
//                return EntityUtils.toString(res);
//            } else {
//                httppost.abort();
//                return null;
//            }
//        } catch (IOException e) {
//            log.warn(e);
//        }
//
//        return null;
//    }
    private boolean handleStatus(HttpResponse response) throws IOException {
        //check status
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode < 300 && statusCode >= 200) {
            return true;
        } else if (statusCode == 404) {
            response.getEntity().getContent().close();
            return false;
        } else if (statusCode < 500 && statusCode >= 400) {
            throw new IllegalArgumentException("Bad request:\nStatus: " + statusCode + "Error:\n" + EntityUtils.toString(response.getEntity()));
        } else if (statusCode >= 500 && statusCode < 600) {
            response.getEntity().getContent().close();
            return false;
        }
        throw new IllegalArgumentException("Unhandled statusCode :" + statusCode);
    }

    public static boolean checkEsito(String res) {
        Builder parser = new Builder();
        Document resd = null;
        try {
            resd = parser.build(res, null);
        } catch (ParsingException ex) {
        } catch (IOException ex) {
            log.error(ex);
            return false;
        }
        // parsare con XOM l'esito
        XPathContext context = new XPathContext("EsitoVersamento", "");
        Nodes risultato = resd.query("//CodiceEsito/text()", context);

        return risultato.get(0).toXML().equals("POSITIVO");
    }
}
