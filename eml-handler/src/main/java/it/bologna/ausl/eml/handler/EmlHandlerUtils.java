package it.bologna.ausl.eml.handler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import jakarta.activation.DataHandler;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
//import org.eclipse.angus.mail.util.BASE64DecoderStream;
//import org.eclipse.angus.mail.util.QPDecoderStream;

public class EmlHandlerUtils {

    private static final Logger LOG = LoggerFactory.getLogger(EmlHandlerUtils.class);

    public static MimeMessage BuildMailMessageFromString(String s) throws EmlHandlerException {

        ByteArrayInputStream bais = null;
        MimeMessage m;
        try {
            bais = new ByteArrayInputStream(s.getBytes("utf8"));
            m = new MimeMessage(getGenericSmtpSession(), bais);
            return m;
        } catch (UnsupportedEncodingException e) {

            throw new EmlHandlerException("Unable to convert string to input stream", e);
        } catch (MessagingException e) {

            throw new EmlHandlerException("Unable to convert string to MimeMessage", e);
        }

    }

    public static MimeMessage BuildMailMessageFromInputStream(InputStream is) throws EmlHandlerException {
        MimeMessage m;
        try {
            m = new MimeMessage(getGenericSmtpSession(), is);
            return m;
        } catch (MessagingException e) {

            throw new EmlHandlerException("Unable to convert string to MimeMessage", e);
        }

    }

    public static Session getGenericSmtpSession() {
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
//        props.setProperty("mail.mime.base64.ignoreerrors", "true");
        //return  Session.getDefaultInstance(props, null);
        return Session.getInstance(props);
    }

    public static String getText(Part p) throws MessagingException, IOException, EmlHandlerException {
        return getTextPart(p, "text/plain");
    }

    public static String getHtml(Part p) throws MessagingException, IOException, EmlHandlerException {
        return getTextPart(p, "text/html");
    }

    private static String getTextPart(Part p, String mime) throws MessagingException, IOException, EmlHandlerException {
        String text;
        String disposition = p.getDisposition();
        if (disposition != null && disposition.equalsIgnoreCase(Part.ATTACHMENT)) {
            return null;
        }
        if (p.isMimeType(mime) && (mime.equals("text/html") || mime.equals("text/plain"))) {
            return (String) getTextPartProcessingByteArray(p, mime);
        } else if (p.isMimeType(mime)) {
            if (String.class.isAssignableFrom(p.getContent().getClass())) {
                return (String) p.getContent();
            } else if (InputStream.class.isAssignableFrom(p.getContent().getClass())) {
                InputStream is = (InputStream) p.getContent();
                int n = is.available();
                byte[] bytes = new byte[n];
                is.read(bytes, 0, n);
                return new String(bytes);
            } else {
                throw new EmlHandlerException(String.format("la parte %s è del tipo %s che non è gestito", mime, p.getClass().getName()));
            }
        }

        if (p.isMimeType("multipart/alternative")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType(mime)) {
                    text = getTextPart(bp, mime);
                    if (text != null) {
                        return text;
                    }
                }
            }
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                text = getTextPart(bp, mime);
                if (text != null) {
                    return text;
                }
            }
        }

        return null;

    }

    private static String getTextPartProcessingByteArray(Part p, String mime) throws MessagingException, IOException {
        Charset charSet = StandardCharsets.UTF_8;
        String[] splitArray = p.getContentType().split(" *(,|=>| ) *");
        for (int i = 0; i < splitArray.length; i++) {
            if (splitArray[i].startsWith("charset=")) {
                int lastIdx = splitArray[i].endsWith(";") ? splitArray[i].length() - 1 : splitArray[i].length();
                String charsetName = splitArray[i].substring("charset=".length(), lastIdx);
                try {
                    // tolgo le virgolette se ci sono
                    charSet = Charset.forName(charsetName.replace("\"", ""));
                    System.out.println("Setto il charset " + charsetName);
                } catch (Throwable t) {
                    System.out.println("Errore nella lookup del charset " + charsetName + ": impossibile settarlo.");
                    System.out.println(t.getMessage());
                    System.out.println(t.getCause());
                    System.out.println("Setto il charset " + StandardCharsets.UTF_8.toString());
                    charSet = StandardCharsets.UTF_8;
                }
            }
        }
        //la codifica dei caratteri funziona meglio mettendo fisso utf 8
        InputStream in = new ByteArrayInputStream(p.getContent().toString().getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int reads = in.read();

        while (reads != -1) {
            baos.write(reads);
            reads = in.read();
        }
        try {
            in.close();
        } catch (Throwable t) {
            System.out.println("Errore nella chiusura dell'InputStream");
            System.out.println(t.getMessage());
            System.out.println(t.getCause());
        }
        byte[] bytes = baos.toByteArray();
        baos.close();
        return (String) new String(bytes);
    }

    public static String getHtmlWithImg(Part p, EmlHandlerResult ehr) throws MessagingException, IOException {
        Document doc = Jsoup.parse(ehr.getHtmlText());
        String src = null, id = null;
        Elements imgs = doc.select("img");

        for (Element img : imgs) {
            src = img.attr("src");
            if (src.startsWith("cid:")) {
                id = src.substring(4); // Rimuovo i primi 4 caratteri "cid:"
                for (EmlHandlerAttachment a : ehr.getAttachments()) {
                    if (a.getContentId() != null && a.getContentId().equals(id)) {
                        if (!a.getMimeType().startsWith("image")) {
                            break;
                        }
                        Object attachmentContent = EmlHandlerUtils.getAttachmentContent(p, a.getId());
                        String imageBase64 = null;
                        try {
                            InputStream b64ds = (InputStream) attachmentContent;
//                            BASE64DecoderStream b64ds = (BASE64DecoderStream) attachmentContent;
                            imageBase64 = BaseEncoding.base64().encode(ByteStreams.toByteArray(b64ds));
                        } catch (ClassCastException e) {
                            InputStream b64ds = (InputStream) attachmentContent;
//                            QPDecoderStream b64ds = (QPDecoderStream) attachmentContent;
                            imageBase64 = BaseEncoding.base64().encode(ByteStreams.toByteArray(b64ds));
                        }
                        doc.select("img[src*=" + src + "]").attr("src", "data:" + a.getMimeType().split(";")[0] + ";base64, " + imageBase64);
                        a.setForHtmlAttribute(true);
                        break;
                    }
                }
            }
        }

        return doc.outerHtml();
    }

    private static File saveFile(String fileName, File dir, InputStream is) throws IOException {

        int extIndex = fileName.lastIndexOf(".");
        String ext = null;
        String basename = null;

        //cerco di estrarre l'esetensione
        if (extIndex != -1 && fileName.length() > extIndex) {
            ext = fileName.substring(extIndex);
            basename = fileName.substring(0, extIndex);
        } else {
            ext = "";
            basename = fileName;
        }
        File fileorig = new File(dir.getAbsolutePath(), basename);
        File file = new File(fileorig.getAbsolutePath() + ext);

        //se esista gia' aggiungiamo numeri in fondo.
        int i = 1;
        while (!file.createNewFile()) {
            file = new File(fileorig.getAbsolutePath() + "_" + i + ext);
            i++;
        }

        FileOutputStream out = new FileOutputStream(file);
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.flush();
        out.close();
        is.close();
        return file;
    }

    private static ArrayList<EmlHandlerAttachment> getEmlAttachment(Part p, Boolean saveBytes) throws MessagingException, IOException {
        return getEmlAttachment(p, saveBytes, false, null);
    }

    private static ArrayList<EmlHandlerAttachment> getEmlAttachment(Part p, Boolean saveBytes, Boolean saveFile, File dirPath) throws MessagingException, IOException {

        ArrayList<EmlHandlerAttachment> res = new ArrayList<EmlHandlerAttachment>();

        String mime = p.getContentType();
        String fname = p.getFileName();
        Integer size = p.getSize();
        try {
            fname = MimeUtility.decodeText(fname);
        } catch (Exception e) {
        }

        if (fname != null || (!mime.startsWith("text/html") && !mime.startsWith("text/plain"))) {
            EmlHandlerAttachment a = new EmlHandlerAttachment();
            if (fname == null) {
                fname = "allegato_senza_nome";
            }
            fname = fname.replaceAll("[^a-zA-Z0-9\\.\\-_\\+ ]", "_");
            a.setFileName(fname);
            a.setMimeType(mime);
            a.setSize(size);
            a.setId(0);
            if (saveBytes) {
                a.setInputStream(p.getInputStream());
//                    a.setFileBytes(getBytesFromInputStream(p.getInputStream()));
            }
            if (saveFile) {
                File attachemntFile = saveFile(fname, dirPath, p.getInputStream());
                a.setFilePath(attachemntFile.getAbsolutePath());
            }
            res.add(a);
        }
        return res;
    }

    private static ArrayList<EmlHandlerAttachment> getEmlAttachments(Part p, Integer[] idAttachments, Boolean saveBytes) throws MessagingException, IOException {
        return getEmlAttachments(p, idAttachments, saveBytes, false, null);
    }

    private static ArrayList<EmlHandlerAttachment> getEmlAttachments(Part p, Integer[] idAttachments, Boolean saveBytes, Boolean saveFile, File dirPath) throws MessagingException, IOException {

        ArrayList<EmlHandlerAttachment> res = new ArrayList();

        ArrayList<Part> parts = getAllParts(p);

        if (idAttachments != null && idAttachments.length > 0) {
            ArrayList<Part> tempParts = new ArrayList<>();
            for (Integer j : idAttachments) {
                tempParts.add(parts.get(j));
            }
            parts = tempParts;
        }

        Integer i = 0;
        String[] contentId = null;

        for (Part part : parts) {
            //Part part = mp.getBodyPart(i);
            String disposition = null;
            try {
                disposition = part.getDisposition();
            } catch (jakarta.mail.internet.ParseException e) {
                // Pattern rp= Pattern.compile("^.*filename=(.*);?$",Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
                disposition = cleanHeaderAndGetDisposition(part.getHeader("Content-Disposition")[0], part);

            }
//tolto                                                                           ||(disposition.equals(Part.INLINE))
            if (((disposition != null) && ((disposition.equals(Part.ATTACHMENT)))) || (part.getFileName() != null)) {
                EmlHandlerAttachment a = new EmlHandlerAttachment();
                String filename = part.getFileName();
                Integer size = part.getSize();
                try {
                    filename = MimeUtility.decodeText(filename);
                } catch (Exception e) {
                }
                if (filename == null) {
                    filename = "allegato_senza_nome";
                }
                filename = filename.replaceAll("[^a-zA-Z0-9\\.\\-_\\+ ]", "_");
                a.setFileName(filename);
                a.setSize(size);
                /* Correggiamo i casi dove dopo il primo punto e virgola del mimetype ci sono altri caratteri prima
                 * di name="...", dopo invece correggiamo i casi in cui nel name ci sono virgolette duplicate */
                String contentType = part.getContentType().replaceAll("^(.*?);(.*)(name=.*$)", "$1; $3");
                // Modificata espressione per considerare il caso del charset
                a.setMimeType(contentType.replaceAll("^(.*)\\s*;\\s*name=\"?(.*?)\"?\\s*(;?\\s*charset\\s*=\\s*\"?([^\"]+?)\"?)?\\s*$", "$1; name=\\\"$2\\\"$3;"));
                a.setId(i);
                contentId = part.getHeader("Content-Id");
                if (contentId != null && contentId.length > 0 && (contentId[0] == null || contentId[0].equals(""))) {
                    a.setContentId(part.getHeader("Content-Id")[0].replaceAll("[<>]", ""));
                }
                if (saveBytes) {
//                    a.setFileBytes(getBytesFromInputStream(part.getInputStream()));
                    a.setInputStream(part.getInputStream());
                }
                if (saveFile) {
                    File attachemntFile = saveFile(filename, dirPath, part.getInputStream());
                    a.setFilePath(attachemntFile.getAbsolutePath());
                }
                res.add(a);
            }
            i++;
        }
        return res;
    }

    public static EmlHandlerAttachment[] getAttachments(Part p, File dirPath, Boolean setAttachmentsStream) throws MessagingException, IOException {
        return getAttachments(p, dirPath, setAttachmentsStream, false);
    }

    public static EmlHandlerAttachment[] getAttachments(Part p, File dirPath, Boolean setAttachmentsStream, Boolean saveAttachments) throws MessagingException, IOException {
        ArrayList<EmlHandlerAttachment> res;
//forse qui
        if (!p.isMimeType("multipart/*")) {
            if (saveAttachments) {
                res = getEmlAttachment(p, setAttachmentsStream, true, dirPath);
            } else {
                res = getEmlAttachment(p, setAttachmentsStream);
            }
            return res.toArray(new EmlHandlerAttachment[res.size()]);
        }
        if (saveAttachments) {
            res = getEmlAttachments(p, null, setAttachmentsStream, true, dirPath);
        } else {
            res = getEmlAttachments(p, null, setAttachmentsStream);
        }
        return res.toArray(new EmlHandlerAttachment[res.size()]);
    }

    private static ArrayList<Part> getAllParts(Part in) throws IOException, MessagingException {
        ArrayList<Part> res = new ArrayList<Part>();
        if (!in.isMimeType("multipart/*")) {
            res.add(in);
            return res;
        } else {
            Multipart mp = (Multipart) in.getContent();
            for (int i = 0, n = mp.getCount(); i < n; i++) {
                Part part = mp.getBodyPart(i);
                if (!part.isMimeType("multipart/*")) {
                    res.add(part);
                } else {
                    res.addAll(getAllParts(part));
                }

            }
            return res;

        }
    }

    private static byte[] getBytesFromInputStream(InputStream is) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] target = buffer.toByteArray();
            return target;
        }
    }

    public static InputStream getAttachment(Part p, Integer idAllegato) throws MessagingException, IOException {

        if (!p.isMimeType("multipart/*")) {
            if (idAllegato.equals(0)) {
                return p.getInputStream();
            } else {
                // TODO: Lancio eccezione
            }
        }

        ArrayList<Part> parts = getAllParts(p);
        Integer i = 0;
        for (Part part : parts) {
            String disposition = null;
            try {
                disposition = part.getDisposition();
            } catch (jakarta.mail.internet.ParseException e) {
//                String disp = part.getHeader("Content-Disposition")[0];
//                LOG.info("Original disp: " + disp);
                ////                disp = disp.replaceAll("^(.*)name=\"?(.*?)\"?$", "$1name=\"$2\"");
//                Pattern pattern = Pattern.compile("^(.*)name=\"*(.*[^\"*])(\"*;)$", Pattern.MULTILINE);
//                Matcher matcher = pattern.matcher(disp);
//                disp = matcher.replaceAll("$1name=\"$2\";");
//
//                pattern = Pattern.compile(";\\s*?=.*?\"?;?", Pattern.MULTILINE);
//                matcher = pattern.matcher(disp);
//                disp = matcher.replaceAll(";");
//
//                LOG.info("New disp disp: " + disp);
//                part.setHeader("Content-Disposition", disp);
                disposition = cleanHeaderAndGetDisposition(part.getHeader("Content-Disposition")[0], part);
//                disposition = part.getDisposition();
            }

            if (((disposition != null) && ((disposition.equals(Part.ATTACHMENT)) || (disposition.equals(Part.INLINE)))) || (part.getFileName() != null)) {
                if (idAllegato.equals(i)) {
                    return part.getInputStream();
                }
            }
            i++;
        }
        // TODO: Non ho trovato l'allegato richiesto. Lancio eccezione
        return null;
    }

    public static Object getAttachmentContent(Part p, Integer idAllegato) throws MessagingException, IOException {

        if (!p.isMimeType("multipart/*")) {
            if (idAllegato.equals(0)) {
                return p.getContent();
            } else {
                // TODO: Lancio eccezione
            }
        }

        ArrayList<Part> parts = getAllParts(p);
        Integer i = 0;
        for (Part part : parts) {
            String disposition = null;
            try {
                disposition = part.getDisposition();
            } catch (jakarta.mail.internet.ParseException e) {
                String disp = part.getHeader("Content-Disposition")[0];
                LOG.info("Original disp: " + disp);
                Pattern pattern = Pattern.compile("^(.*)name=\"?(.*?)\"?$", Pattern.MULTILINE);
                Matcher matcher = pattern.matcher(disp);
                disp = matcher.replaceAll("$1name=\"$2\"");

                pattern = Pattern.compile(";\\s*?=.*?\"?;?", Pattern.MULTILINE);
                matcher = pattern.matcher(disp);
                disp = matcher.replaceAll(";");
                LOG.info("New Disp: " + disp);
                part.setHeader("Content-Disposition", disp);
                disposition = part.getDisposition();
            }

            if (((disposition != null) && ((disposition.equals(Part.ATTACHMENT)) || (disposition.equals(Part.INLINE)))) || (part.getFileName() != null)) {
                if (idAllegato.equals(i)) {
                    return part.getContent();
                }
            }
            i++;
        }
        // TODO: Non ho trovato l'allegato richiesto. Lancio eccezione
        return null;
    }

    public static List<Pair> getAttachments(Part p) throws MessagingException, IOException {
        String filename;
        List<Pair> pairs = new ArrayList();
        if (!p.isMimeType("multipart/*")) {
            filename = p.getFileName();
            try {
                filename = MimeUtility.decodeText(filename);
            } catch (Exception e) {
            }
            pairs.add(new ImmutablePair(filename.replaceAll("[^a-zA-Z0-9\\.\\-_\\+ ]", "_"), p.getInputStream()));
            return pairs;
        }

        ArrayList<Part> parts = getAllParts(p);
        for (Part part : parts) {
            String disposition = null;
            try {
                disposition = part.getDisposition();
            } catch (jakarta.mail.internet.ParseException e) {
                String disp = part.getHeader("Content-Disposition")[0];
                LOG.info("Original disp: " + disp);
                Pattern pattern = Pattern.compile("^(.*)name=\"?(.*?)\"?$", Pattern.MULTILINE);
                Matcher matcher = pattern.matcher(disp);
                disp = matcher.replaceAll("$1name=\"$2\"");

                pattern = Pattern.compile(";\\s*?=.*?\"?;?", Pattern.MULTILINE);
                matcher = pattern.matcher(disp);
                disp = matcher.replaceAll(";");
                LOG.info("New disp disp: " + disp);
                part.setHeader("Content-Disposition", disp);
                disposition = part.getDisposition();
            }

            if (((disposition != null) && ((disposition.equals(Part.ATTACHMENT)) || (disposition.equals(Part.INLINE)))) || (part.getFileName() != null)) {
                filename = part.getFileName();
                try {
                    filename = MimeUtility.decodeText(filename);
                } catch (Exception e) {
                }
                pairs.add(new ImmutablePair(filename.replaceAll("[^a-zA-Z0-9\\.\\-_\\+ ]", "_"), part.getInputStream()));
            }
        }

        return pairs;
    }

    public static ArrayList<EmlHandlerAttachment> retrieveAttachments(Part p, Integer[] idAttachments) throws MessagingException, IOException {
        if (!p.isMimeType("multipart/*")) {
            return getEmlAttachment(p, Boolean.TRUE);
        } else {
            return getEmlAttachments(p, idAttachments, Boolean.TRUE);
        }
    }

    public static MimeMessage buildDraftMessage(String message, String subject, Address from, Address[] to, Address[] cc,
        ArrayList<EmlHandlerAttachment> attachments, Properties props) throws MessagingException, IOException {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        if (props != null) {
            mailSender.setJavaMailProperties(props);
        }
        MimeMessage m = mailSender.createMimeMessage();
        m.setFrom(from);

        if (to != null) {
            m.setRecipients(MimeMessage.RecipientType.TO, to);
        }

        if (cc != null) {
            m.setRecipients(MimeMessage.RecipientType.CC, cc);
        }

        if (subject != null) {
            m.setSubject(subject);
        }

        if (attachments == null) {
            m.setContent(message, "text/html");
        } else {
            // create the message part
            MimeBodyPart messageBodyPart = new MimeBodyPart();

            //fill message
            messageBodyPart.setText(message, "utf-8", "html");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            // Part two is attachment
            for (EmlHandlerAttachment attachment : attachments) {
                messageBodyPart = new MimeBodyPart();
//                byte[] fileBytes = attachment.getFileBytes();
                String attachmentName = attachment.getFileName();
                ByteArrayDataSource source;
                if (attachment.getInputStream() != null) {
                    source = new ByteArrayDataSource(attachment.getInputStream(), attachment.getMimeType());
                } else {
                    source = new ByteArrayDataSource(attachment.getFileBytes(), attachment.getMimeType());
                }
                messageBodyPart.setDataHandler(
                    new DataHandler(source));
                messageBodyPart.setFileName(attachmentName);
                multipart.addBodyPart(messageBodyPart);
            }
            // Put parts in message
            m.setContent(multipart);
        }
        return m;
    }

    private static String cleanHeaderAndGetDispositionOld(String disp, Part part) throws MessagingException {
        LOG.info("Original disp: " + disp);

        Pattern pattern = Pattern.compile("^(.*)name=\"*(.*[^\"*])(\"*;)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(disp);
        disp = matcher.replaceAll("$1name=\"$2\";");

        pattern = Pattern.compile(";\\s*?=.*?\"?;?", Pattern.MULTILINE);
        matcher = pattern.matcher(disp);
        disp = matcher.replaceAll(";");

//                pattern =Pattern.compile(";\\s*?.*=.*\"?;\\s*.*\\s.*",Pattern.MULTILINE);
//                matcher = pattern.matcher(disp);
//                disp = matcher.replaceAll(";");
//                disp = disp.replaceAll("^(.*)name=\"?(.*?)\"?$", "$1name=\"$2\"");
        LOG.info("New disp disp: " + disp);

        part.setHeader("Content-Disposition", disp);

        return part.getDisposition();
    }

    private static String cleanHeaderAndGetDisposition(String disp, Part part) throws MessagingException {
        LOG.info("Original disp: " + disp);

        try {
            // Normalizza le newline
            String normalizedDisp = disp.replaceAll("\\r\\n\\s+", " ").trim();

            // Controlla se ci sono parametri RFC 2231
            Pattern rfc2231Pattern = Pattern.compile("FileName\\*\\d+\\*?=", Pattern.CASE_INSENSITIVE);
            Matcher rfc2231Matcher = rfc2231Pattern.matcher(normalizedDisp);

            if (rfc2231Matcher.find()) {
                // CASO RFC 2231: usa la nuova logica
                LOG.info("Detected RFC 2231 format, reconstructing filename");

                Pattern paramPattern = Pattern.compile("FileName\\*\\d+\\*?=([^;]+)", Pattern.CASE_INSENSITIVE);
                Matcher paramMatcher = paramPattern.matcher(normalizedDisp);

                StringBuilder reconstructedFilename = new StringBuilder();
                String charset = "utf-8";

                while (paramMatcher.find()) {
                    String value = paramMatcher.group(1).trim();

                    if (reconstructedFilename.length() == 0 && value.contains("''")) {
                        String[] parts = value.split("''", 2);
                        charset = parts[0];
                        value = parts.length > 1 ? parts[1] : "";
                    }

                    reconstructedFilename.append(value);
                }

                // Rimuovi i parametri RFC 2231
                disp = normalizedDisp.replaceAll("FileName\\*\\d+\\*?=[^;]+;?", "").trim();

                // Decodifica e aggiungi il filename
                String decodedFilename = java.net.URLDecoder.decode(
                    reconstructedFilename.toString(),
                    charset
                );

                if (!disp.endsWith(";")) {
                    disp += ";";
                }
                disp += " filename=\"" + decodedFilename + "\";";

            } else {
                // CASO STANDARD: usa la logica originale
                LOG.info("Standard format, applying original cleaning");

                Pattern pattern = Pattern.compile("^(.*)name=\"*(.*[^\"*])(\"*;)$", Pattern.MULTILINE);
                Matcher matcher = pattern.matcher(disp);
                disp = matcher.replaceAll("$1name=\"$2\";");

                pattern = Pattern.compile(";\\s*?=.*?\"?;?", Pattern.MULTILINE);
                matcher = pattern.matcher(disp);
                disp = matcher.replaceAll(";");
            }

            // Pulizia finale comune
            disp = disp.replaceAll(";\\s*;", ";").replaceAll(";\\s*$", "");

            LOG.info("New disp: " + disp);

            part.setHeader("Content-Disposition", disp);
            try {
                return part.getDisposition();
            } catch (MessagingException e) {
                disp = disp.replaceAll("(?i)filename\\s*=\\s*([^\";\\r\\n]+)(;?)", "filename=\"$1\";");
                part.setHeader("Content-Disposition", disp);
            }
            return part.getDisposition();

        } catch (MessagingException | UnsupportedEncodingException e) {
            LOG.error("Error cleaning disposition header", e);
            if (disp.toLowerCase().startsWith("attachment")) {
                return Part.ATTACHMENT;
            } else if (disp.toLowerCase().startsWith("inline")) {
                return Part.INLINE;
            }
            throw new MessagingException("Cannot parse Content-Disposition", e);
        }
    }

}
