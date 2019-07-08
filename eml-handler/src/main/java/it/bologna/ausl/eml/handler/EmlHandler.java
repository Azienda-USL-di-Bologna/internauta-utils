package it.bologna.ausl.eml.handler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.mail.Address;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author andrea zucchelli
 * <br>
 * <p>
 * Questa classe prende in carico un file eml contentente una email. Il file
 * viene parsato, vengono estratti il contenuto testuale sia in formato testo
 * che html, le meta informazioni piu' significative e vengono estratti gli
 * allegati. Questi vengono salvati su filesystem, ogni allegao e' descritto da
 * un oggetto di tipo {@link EmlHandlerAttachment}
 * </p>
 */
public class EmlHandler {

    private String rawMessage;
    private File workingDir;

    public EmlHandler() {

    }

    public void setParameters(String message, String dir) {
        rawMessage = message;
        if (dir != null) {
            workingDir = new File(dir);
        } else {
            dir = null;
        }
    }

    public EmlHandlerResult handleRawEml() throws EmlHandlerException, UnsupportedEncodingException {
        return handleRawEml(rawMessage, workingDir);
    }

    public static EmlHandlerResult handleEml(String filePath)
            throws EmlHandlerException, UnsupportedEncodingException {
        FileInputStream is = null;
        MimeMessage m = null;
        EmlHandlerResult res = null;

        try {
            File in = new File(filePath);
            File dir = new File(in.getParent());

            try {
                is = new FileInputStream(filePath);
            } catch (FileNotFoundException e) {
                throw new EmlHandlerException(
                        "Unable to open file " + filePath, e);
            }
            m = EmlHandlerUtils.BuildMailMessageFromInputStream(is);
            res = processEml(m, dir);
            return res;
        } finally {
            try {
                is.close();
            } catch (IOException e) {

            }
        }

    }

    public static EmlHandlerResult handleEml(String filePath, String workingDir)
            throws EmlHandlerException, UnsupportedEncodingException {

        FileInputStream is = null;
        MimeMessage m = null;
        EmlHandlerResult res = null;

        File dir = null;

        try {
            if (workingDir != null) {
                dir = new File(workingDir);
                if (!dir.exists() || !dir.canWrite()) {
                    throw new EmlHandlerException("Working dir: '" + workingDir
                            + "' must exists and be writeable :F");
                }
            }
            try {
                is = new FileInputStream(filePath);
            } catch (FileNotFoundException e) {
                throw new EmlHandlerException(
                        "Unable to open file " + filePath, e);
            }
            m = EmlHandlerUtils.BuildMailMessageFromInputStream(is);
            res = processEml(m, dir);
            return res;

        } finally {
            try {
                is.close();
            } catch (IOException e) {

            }
        }

    }

    public static EmlHandlerResult handleRawEml(String rawMessage, File working_dir) throws EmlHandlerException, UnsupportedEncodingException {

        MimeMessage m = null;
        EmlHandlerResult res = null;

        m = EmlHandlerUtils.BuildMailMessageFromString(rawMessage);
        //TODO: decidere cosa fare con il path
        res = processEml(m, working_dir);
        return res;

    }

    /**
     *
     * @param filePath patj del file contenente il file eml da parsare e gestire
     * @return EmlHandlerResult contenente il risultato dell'elaborazione
     * @throws EmlHandlerException
     */
    private static EmlHandlerResult processEml(MimeMessage m, File working_dir) throws EmlHandlerException, UnsupportedEncodingException {

        File dir = working_dir;
        EmlHandlerResult res = new EmlHandlerResult();

        try {
            Address[] from = m.getFrom();
            if (from != null && from[0] != null) {
                res.setFrom(MimeUtility.decodeText(from[0].toString()));
            }
        } catch (MessagingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            Address[] recipients = m.getRecipients(Message.RecipientType.TO);
            if (recipients != null) {
                String[] rec = new String[recipients.length];
                for (int i = 0; i < recipients.length; i++) {
                    rec[i] = MimeUtility.decodeText(recipients[i].toString());
                }
                res.setTo(rec);
            }
        } catch (MessagingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            Address[] recipients = m.getRecipients(Message.RecipientType.CC);
            if (recipients != null) {
                String[] rec = new String[recipients.length];
                for (int i = 0; i < recipients.length; i++) {
                    rec[i] = MimeUtility.decodeText(recipients[i].toString());
                }
                res.setCc(rec);
            }
        } catch (MessagingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            res.setSubject(m.getSubject());
        } catch (MessagingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            res.setSendDate(m.getSentDate());
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            res.setMessageId(m.getMessageID());
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            res.setPlainText(EmlHandlerUtils.getText(m));
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            res.setHtmlText(EmlHandlerUtils.getHtml(m));
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            if (dir != null) {
                res.setAttachments(EmlHandlerUtils.getAttachments(m, dir));
            }
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            if (res.getHtmlText() != null) {
                res.setHtmlTextImgEmbedded(EmlHandlerUtils.getHtmlWithImg(m, res));
            }
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return res;

    }
    
    /**
     * Metodo che restituisce l'allegato richiesto
     * @param emlPath
     * @param idAllegato
     * @return
     * @throws EmlHandlerException
     * @throws MessagingException
     * @throws IOException 
     */
    public static InputStream getAttachment(FileInputStream is, Integer idAllegato) throws EmlHandlerException, MessagingException, IOException {
//        FileInputStream is = null;
        MimeMessage m = null;
        try {
//            try {
//                is = new FileInputStream(emlPath);
//            } catch (FileNotFoundException e) {
//                throw new EmlHandlerException(
//                        "Unable to open file " + emlPath, e);
//            }
            m = EmlHandlerUtils.BuildMailMessageFromInputStream(is);
            return EmlHandlerUtils.getAttachment(m, idAllegato);
        } finally {
            try {
                is.close();
            } catch (IOException e) {

            }
        }
    }

    /**
     * Metodo utile al ritorno degli allegati dell'eml per metterli in uno zip
     * @param emlPath
     * @return
     * @throws EmlHandlerException
     * @throws MessagingException
     * @throws IOException 
     */
    public static List<Pair> getAttachments(FileInputStream is) throws EmlHandlerException, MessagingException, IOException {
//        FileInputStream is = null;
        MimeMessage m = null;
        try {
//            try {
//                is = new FileInputStream(emlPath);
//            } catch (FileNotFoundException e) {
//                throw new EmlHandlerException(
//                        "Unable to open file " + emlPath, e);
//            }
            m = EmlHandlerUtils.BuildMailMessageFromInputStream(is);
            return EmlHandlerUtils.getAttachments(m);
        } finally {
            try {
                is.close();
            } catch (IOException e) {

            }
        }
    }
    
    /**
     * Il metodo estrae gli allegati richiesti dall'eml recuperato dal path passato in ingresso
     * @param emlPath Il path dove si trova l'eml
     * @param idAttachments Array con gli id degli allegati che devono essere scaricati
     * @return La lista degli allegati estratti
     * @throws EmlHandlerException
     * @throws MessagingException
     * @throws IOException
     */
    public static ArrayList<EmlHandlerAttachment> getListAttachments(String emlPath, byte[] emlBytes, Integer[] idAttachments) throws EmlHandlerException, MessagingException, IOException {
        InputStream is = null;
        MimeMessage m = null;
        try {
            try {
                if (emlPath != null) {
                    is = new FileInputStream(emlPath);
                } else if (emlBytes != null) {
                    is = new ByteArrayInputStream(emlBytes);
                }
            } catch (FileNotFoundException e) {
                throw new EmlHandlerException(
                        "Unable to open file " + emlPath, e);
            }
            m = EmlHandlerUtils.BuildMailMessageFromInputStream(is);
            return EmlHandlerUtils.retrieveAttachments(m, idAttachments);
        } finally {
            try {
                is.close();
            } catch (IOException e) {

            }
        }
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
    * @param props JavaMail properties opzionale, se non si vuole passare settare null
    * @return MimeMessage Ritorna la mail
    * @throws MessagingException
    */
    public static MimeMessage buildDraftMessage(String message, String subject, Address from, Address[] to, Address[] cc, 
            ArrayList<EmlHandlerAttachment> attachments, Properties props) throws MessagingException {
        return EmlHandlerUtils.buildDraftMessage(message, subject, from, to, cc, attachments, props);
    }
}
