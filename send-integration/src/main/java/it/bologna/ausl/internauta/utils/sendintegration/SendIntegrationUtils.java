package it.bologna.ausl.internauta.utils.sendintegration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.openpdf.text.pdf.PdfReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class SendIntegrationUtils {
    private static final Logger log = LoggerFactory.getLogger(SendIntegrationUtils.class);
    
    public static String getSha256Base64Encoded(File file) throws IOException, NoSuchAlgorithmException {
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] sha256 = digest.digest(fileBytes);

        String base64Sha256 = Base64.getEncoder().encodeToString(sha256);

        return base64Sha256;
    }
    
    public static boolean isPdf(File file) {
        try (PdfReader reader = new PdfReader(file.getAbsolutePath())) {
            return true;
        } catch (IOException ex) {
            log.error("errore nella lettura del file pdf", ex);
            return false;
        }
    }
}
