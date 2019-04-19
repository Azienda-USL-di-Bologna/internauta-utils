package it.bologna.ausl.eml.handler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.sun.mail.util.BASE64DecoderStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.javamail.JavaMailSenderImpl;

public class EmlHandlerUtils {

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
        //return  Session.getDefaultInstance(props, null);
        return Session.getInstance(props);
    }

    public static String getText(Part p) throws MessagingException, IOException {
        return getTextPart(p, "text/plain");
    }

    public static String getHtml(Part p) throws MessagingException, IOException {
        return getTextPart(p, "text/html");
    }

    private static String getTextPart(Part p, String mime) throws MessagingException, IOException {
        String text = null;
        if (p.isMimeType(mime)) {

            return (String) p.getContent();
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
    
    public static String getHtmlWithImg(Part p, EmlHandlerResult ehr) throws MessagingException, IOException {
        Document doc = Jsoup.parse(ehr.getHtmlText());
        String src = null, id = null;
        Elements imgs = doc.select("img");
        
        for(Element img : imgs) {
            src = img.attr("src");
            id = src.substring(4); // Rimuovo i primi 4 caratteri "cid:"
            for (EmlHandlerAttachment a : ehr.getAttachments()) {
                if (a.getContentId().equals(id)) {
                    if (!a.getMimeType().startsWith("image")) {
                        break;
                    }
                    Object attachmentContent = EmlHandlerUtils.getAttachmentContent(p, a.getId());
                    BASE64DecoderStream b64ds = (BASE64DecoderStream) attachmentContent;
                    String imageBase64 = BaseEncoding.base64().encode(ByteStreams.toByteArray(b64ds));
                    doc.select("img[src*=" + src + "]").attr("src", "data:" + a.getMimeType().split(";")[0] + ";base64, " + imageBase64);
                    a.setForHtmlAttribute(true);
                    break;
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

    public static EmlHandlerAttachment[] getAttachments(Part p, File dirPath) throws MessagingException, IOException {
        ArrayList<EmlHandlerAttachment> res = new ArrayList<EmlHandlerAttachment>();

        if (!p.isMimeType("multipart/*")) {
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
                a.setSize(size);
                a.setFileName(fname);
                a.setMimeType(mime);
                a.setId(0);
//                a.setFilePath(saveFile(fname, dirPath, p.getInputStream()).getAbsolutePath());
                res.add(a);
                return res.toArray(new EmlHandlerAttachment[res.size()]);
            }
            return null;
        }

        ArrayList<Part> parts = getAllParts(p);
        Integer i = 0;
        String[] contentId = null;
        for (Part part : parts) {
            //Part part = mp.getBodyPart(i);
            String disposition = null;
            try {
                disposition = part.getDisposition();
            } catch (javax.mail.internet.ParseException e) {
                // Pattern rp= Pattern.compile("^.*filename=(.*);?$",Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
                String disp = part.getHeader("Content-Disposition")[0];
                System.out.println("Original disp: " + disp);
                disp = disp.replaceAll("^(.*)filename=(.*)(;)?(.*)$", "$1filename=\"$2\"$3$4");
                System.out.println("New disp disp: " + disp);
                part.setHeader("Content-Disposition", disp);
                disposition = part.getDisposition();
            }

            if (((disposition != null) && ((disposition.equals(Part.ATTACHMENT)) || (disposition.equals(Part.INLINE)))) || (part.getFileName() != null)) {
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
                a.setMimeType(part.getContentType());
                a.setId(i);
                contentId = part.getHeader("Content-Id");
                if (contentId != null && contentId.length > 0 && !StringUtils.isEmpty(contentId[0])) {
                    a.setContentId(part.getHeader("Content-Id")[0].replaceAll("[<>]", ""));
                }
                
                //a.setFilePath(saveFile(filename, dirPath, part.getInputStream()).getAbsolutePath());
                res.add(a);
            }
            i++;
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
            } catch (javax.mail.internet.ParseException e) {
                String disp = part.getHeader("Content-Disposition")[0];
                System.out.println("Original disp: " + disp);
                disp = disp.replaceAll("^(.*)filename=(.*)(;)?(.*)$", "$1filename=\"$2\"$3$4");
                System.out.println("New disp disp: " + disp);
                part.setHeader("Content-Disposition", disp);
                disposition = part.getDisposition();
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
            } catch (javax.mail.internet.ParseException e) {
                String disp = part.getHeader("Content-Disposition")[0];
                System.out.println("Original disp: " + disp);
                disp = disp.replaceAll("^(.*)filename=(.*)(;)?(.*)$", "$1filename=\"$2\"$3$4");
                System.out.println("New disp disp: " + disp);
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
        List<Pair> pairs = new ArrayList();
        if (!p.isMimeType("multipart/*")) {
            pairs.add(new ImmutablePair(p.getFileName(), p.getInputStream()));
            return pairs;
        }
        
        ArrayList<Part> parts = getAllParts(p);

        for (Part part : parts) {
            String disposition = null;
            try {
                disposition = part.getDisposition();
            } catch (javax.mail.internet.ParseException e) {
                String disp = part.getHeader("Content-Disposition")[0];
                System.out.println("Original disp: " + disp);
                disp = disp.replaceAll("^(.*)filename=(.*)(;)?(.*)$", "$1filename=\"$2\"$3$4");
                System.out.println("New disp disp: " + disp);
                part.setHeader("Content-Disposition", disp);
                disposition = part.getDisposition();
            }

            if (((disposition != null) && ((disposition.equals(Part.ATTACHMENT)) || (disposition.equals(Part.INLINE)))) || (part.getFileName() != null)) {
                pairs.add(new ImmutablePair(part.getFileName(), part.getInputStream()));
            }
        }
        
        return pairs;
    }
    
    /**
     * Il metodo costruisce la mail, può essere utilizzato sia per il salvataggio della bozza che per la spedizione della mail
     * 
     * @param message Il testo della mail
     * @param subject L'oggetto della mail
     * @param from L'indirizzo del mittente
     * @param to Array degli indirizzi a cui spedire la mail
     * @param cc Array degli indirizzi da inserire come Copia Carbone
     * @param attachments Gli allegati
     * @return MimeMessage Ritorna la mail
     * @throws MessagingException
     */
    public MimeMessage buildDraftMessage(String message, String subject, Address from, Address[] to, Address[] cc, ArrayList<EmlHandlerAttachment> attachments) throws MessagingException {
        
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
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
                byte[] fileBytes = attachment.getFileBytes();
                String attachmentName = attachment.getFileName();
                ByteArrayDataSource source
                        = new ByteArrayDataSource(fileBytes, attachment.getMimeType());
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
}
