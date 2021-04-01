package it.bologna.ausl.documentgenerator.utils;

import it.bologna.ausl.mongowrapper.MongoWrapper;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author spritz
 */
@Component
public class GeneratorUtils {

    private static final Logger log = LoggerFactory.getLogger(GeneratorUtils.class);

    public boolean isAcceptedMimeType(MultipartFile allegato) {
        boolean accepted = false;
        log.info("verifico allegato " + allegato.getName() + " -> " + allegato.getContentType());
        try {
            accepted = SupportedMimeTypes.contains(allegato.getContentType());
            log.info("E' supportato? " + accepted);
        } catch (Exception e) {
            log.error("Il tipo di allegato non è supportato");
        }
        return accepted;
    }

    public enum SupportedArchiveTypes {
        EML("message/rfc822"),
        MSG("vnd.ms-outlook"),
        PKCS_7_SIGNATURE("pkcs7-signature"),
        PKCS_7_MIME("pkcs7-mime"),
        MBOX("mbox"),
        PDF("application/pdf");

        private String mimeType;

        SupportedArchiveTypes(String mimeType) {
            this.mimeType = mimeType;
        }

        @Override
        public String toString() {
            return mimeType;
        }

        public static boolean contains(String givenMime) {
            for (SupportedArchiveTypes mime : SupportedArchiveTypes.values()) {
                if (mime.toString().equals(givenMime)) {
                    return true;
                }
            }
            return false;
        }
    }

    public enum SupportedMimeTypes {
        BMP("image/x-ms-bmp"),
        DOC("application/msword"),
        DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        GIF("image/gif"),
        JPG("image/jpeg"),
        PDF("application/pdf"),
        PNG("image/png"),
        RTF("application/rtf"),
        TIFF("image/tiff"),
        TXT("text/plain"),
        HTML("text/html"),
        XLS("application/vnd.ms-excel"),
        XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        private String mimeType;

        SupportedMimeTypes(String mimeType) {
            this.mimeType = mimeType;
        }

        @Override
        public String toString() {
            return mimeType;
        }

        public static boolean contains(String givenMime) {
            for (SupportedMimeTypes mime : SupportedMimeTypes.values()) {
                if (mime.toString().equals(givenMime)) {
                    return true;
                }
            }
            return false;
        }
    }

    public enum SupportedSignatureType {
        ZIP7("37 7A BC AF 27 1C "),
        MSG("D0 CF 11 E0 A1 B1 ");

        private String hexType;

        SupportedSignatureType(String hexType) {
            this.hexType = hexType;
        }

        @Override
        public String toString() {
            return hexType;
        }

        public static boolean contains(String giveHexType) {
            System.out.println("giveHexType " + giveHexType);

            for (SupportedSignatureType mime : SupportedSignatureType.values()) {
                if (mime.toString().equals(giveHexType)) {
                    return true;
                }
            }
            return false;
        }
    }

    @SuppressWarnings("empty-statement")
    public boolean signatureFileAccepted(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        log.info("QWERTY signatureFileAccepted " + sb.toString() + " con questa condizione " + SupportedSignatureType.contains(sb.toString()));
        //verifico che sia tra gli enum accettati 7z msg
        if (SupportedSignatureType.contains(sb.toString())) {
            return true;

        }
        return false;
    }

    public boolean isPdf(MultipartFile allegato) {
        return allegato.getContentType().equals(SupportedMimeTypes.PDF.toString());
    }

    public Map<String, Object> uploadMongoISandJsonAllegato(MongoWrapper mongo, InputStream inputStreamAllegato, String allegatoFileName, Boolean principale, String allegatoContentType, Boolean isPDF) throws Exception {
        Map<String, Object> mapAllegato = new HashMap();
        try {
            log.info("sto per caricare su mongo il file --> " + allegatoFileName);
            String uuidAllegato = mongo.put(inputStreamAllegato, allegatoFileName, "/temp/generazione_documenti_da_ext/" + UUID.randomUUID(), false);
            log.info("ho caricato il file UIID ritornato --> " + uuidAllegato);

            mapAllegato.put("nome_file", allegatoFileName);
            mapAllegato.put("uuid_file", uuidAllegato);
            mapAllegato.put("principale", principale);
            mapAllegato.put("mime_type", allegatoContentType);
            mapAllegato.put("da_convertire", isPDF);
        } catch (Exception ex) {
            log.error("Errore nel caricamento su mongo", ex);
            ex.printStackTrace();
            throw new Exception("Errore caricamento su mongo");
        }

        return mapAllegato;
    }

    public void svuotaCartella(String dirDaSvuotareAbsolutePath) {
        File directory = new File(dirDaSvuotareAbsolutePath);
        File[] files = directory.listFiles();
        for (File f : files) {
            f.delete();
        }
    }
}
