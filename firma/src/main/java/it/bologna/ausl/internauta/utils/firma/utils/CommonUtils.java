package it.bologna.ausl.internauta.utils.firma.utils;

import it.bologna.ausl.mimetypeutilities.Detector;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypeException;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
public class CommonUtils {
    public static String getHostname(HttpServletRequest request) {

        String res;
        String header = request.getHeader("X-Forwarded-Host");
        // TODO: non è detto che vada bene tornare sempre il primo elemento, bisognerebbe controllare che il Path dell'azienda matchi con uno qualsiasi degli elementi
        if (StringUtils.hasText(header)) {
            String[] headerToken = header.split(",");
            res = headerToken[0];
        } else {
            res = request.getServerName();
        }
        return res;
    }
    
    public static KeyStore loadJavaKeyStore() throws FileNotFoundException, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        String relativeCacertsPath = "/lib/security/cacerts".replace("/", File.separator);
        String filename = System.getProperty("java.home") + relativeCacertsPath;
        FileInputStream is = new FileInputStream(filename);

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        String password = "changeit";
        keystore.load(is, password.toCharArray());

        return keystore;
    }
    
     /**
     * indica se il file è un p7m
     *
     * @param file il file da controllare
     * @return "true" se il file è un p7m, "false" altrimenti
     * @throws java.io.IOException
     * @throws java.io.UnsupportedEncodingException
     * @throws org.apache.tika.mime.MimeTypeException
     */
    public static boolean isP7m(File file) throws IOException, UnsupportedEncodingException, MimeTypeException {
        Detector detector = new Detector();
        String mimeType = detector.getMimeType(file.getAbsolutePath());
        MediaType mediaType = MediaType.parse(mimeType);
        return mediaType == Detector.MEDIA_TYPE_PKCS_7_MIME || mediaType == Detector.MEDIA_TYPE_PKCS_7_SIGNATURE;
    }
    
    
}
