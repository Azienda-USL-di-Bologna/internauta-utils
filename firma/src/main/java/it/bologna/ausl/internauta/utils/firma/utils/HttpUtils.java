package it.bologna.ausl.internauta.utils.firma.utils;

import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class HttpUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
    
    /**
     * Costruisce un okHttpClient, partendo dalla proprietà impostante in quello passato come parametro, inserendo la chiave nel keystore di java.Il formato della chiave deve essere p12
     *
     * @param okHttpClient l'okHttpClient di partenza
     * @param sslCertPath il path del file p12 con la chiave
     * @param sslCertPswd  la password del p12
     * @return l'okHttpClient con il context ssl configurato
     */
    public static OkHttpClient buildNewOkHttpClientWithSSLContext(OkHttpClient okHttpClient, String sslCertPath, String sslCertPswd) throws FirmaRemotaConfigurationException {
        OkHttpClient newOkHttpClient = null;
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            FileInputStream clientCertificateContent = new FileInputStream(sslCertPath);
            keyStore.load(clientCertificateContent, sslCertPswd.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, sslCertPswd.toCharArray());

            KeyStore trustedStore = CommonUtils.loadJavaKeyStore();

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustedStore);

            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, new SecureRandom());
            
            newOkHttpClient = okHttpClient.newBuilder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0])
                .build();
        } catch (Throwable ex) {
            String errorMessage = "errore buildNewOkHttpClientWithSSLContext";
            logger.error(errorMessage, ex);
            throw new FirmaRemotaConfigurationException(errorMessage, ex);
        }
        return newOkHttpClient;
    }

    /**
     * Costruisce un okHttpClient, partendo dalla proprietà impostante in quello passato come parametro, ma configurandolo in modo da non fare il controllo del
     * certificato
     * @param okHttpClient l'okHttpClient di partenza
     * @return l'okHttpClient configurato in modo da non fare il controllo del certificato
     * @throws FirmaRemotaConfigurationException 
     */
    public static OkHttpClient buildNewOkHttpClientWithNoCertificateValidation(OkHttpClient okHttpClient) throws FirmaRemotaConfigurationException {
        OkHttpClient newOkHttpClient = null;
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
            };
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            newOkHttpClient = okHttpClient.newBuilder()
                    .hostnameVerifier((String hostname, SSLSession session) -> true)
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
        } catch (Throwable ex) {
            String errorMessage = "errore buildNewOkHttpClientWithNoCertificateValidation";
            logger.error(errorMessage, ex);
            throw new FirmaRemotaConfigurationException(errorMessage, ex);
        }
        return newOkHttpClient;
    }
            
}
