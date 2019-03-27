package it.bologna.ausl.eml.handler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

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
                a.setFilePath(saveFile(fname, dirPath, p.getInputStream()).getAbsolutePath());
                res.add(a);
                return res.toArray(new EmlHandlerAttachment[res.size()]);
            }
            return null;
        }

        ArrayList<Part> parts = getAllParts(p);

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
                try {
                    filename = MimeUtility.decodeText(filename);
                } catch (Exception e) {
                }
                if (filename == null) {
                    filename = "allegato_senza_nome";
                }
                filename = filename.replaceAll("[^a-zA-Z0-9\\.\\-_\\+ ]", "_");
                a.setFileName(filename);
                a.setMimeType(part.getContentType());
                a.setFilePath(saveFile(filename, dirPath, part.getInputStream()).getAbsolutePath());
                res.add(a);
            }
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
}
