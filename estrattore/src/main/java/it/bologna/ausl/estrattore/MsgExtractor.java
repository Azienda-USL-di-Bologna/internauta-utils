package it.bologna.ausl.estrattore;

import it.bologna.ausl.estrattore.exception.ExtractorException;
import it.bologna.ausl.mimetypeutilities.Detector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.poi.hsmf.MAPIMessage;
import org.apache.poi.hsmf.datatypes.AttachmentChunks;
import org.apache.poi.hsmf.datatypes.Chunk;
import org.apache.poi.hsmf.datatypes.ChunkBasedPropertyValue;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.MessagePropertiesChunk;
import org.apache.poi.hsmf.datatypes.NameIdChunks;
import org.apache.poi.hsmf.datatypes.PropertiesChunk;
import org.apache.poi.hsmf.datatypes.PropertyValue;
import org.apache.poi.hsmf.datatypes.RecipientChunks;
import org.apache.poi.hsmf.datatypes.Types;
import org.apache.poi.hsmf.datatypes.Types.MAPIType;
import org.apache.poi.hsmf.exceptions.ChunkNotFoundException;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.EntryUtils;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.tika.mime.MediaType;

/**
 *
 * @author Giuseppe De Marco (gdm)
 */
public class MsgExtractor extends Extractor {

    public class MsgExtractorResult {

        private ArrayList<ExtractorResult> attachments;
        private String messageId;
        private String text;
        private MediaType textMediaType;
        private String subject;
        private String to;
        private String cc;
        private Date sendDate;
        private String from;

        public MsgExtractorResult(ArrayList<ExtractorResult> attachments, String text, MediaType textMediaType, String messageId, String subject, String to, String cc, Date sendDate, String from) {
            this.textMediaType = textMediaType;
            this.attachments = attachments;
            this.text = text;
            this.messageId = messageId;
            this.subject = subject;
            this.to = to;
            this.cc = cc;
            this.sendDate = sendDate;
            this.from = from;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getCc() {
            return cc;
        }

        public void setCc(String cc) {
            this.cc = cc;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public Date getSendDate() {
            return sendDate;
        }

        public void setSendDate(Date sendDate) {
            this.sendDate = sendDate;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public ArrayList<ExtractorResult> getAttachments() {
            return attachments;
        }

        public void setAttachments(ArrayList<ExtractorResult> attachments) {
            this.attachments = attachments;
        }

        public String getText() {
            return text;
        }

        public void setText(String cumulativeText) {
            this.text = cumulativeText;
        }

        public MediaType getTextMediaType() {
            return textMediaType;
        }

        public void setTextMediaType(MediaType textMediaType) {
            this.textMediaType = textMediaType;
        }

        @Override
        public String toString() {
            return "From: " + from + "\nTo: " + to + "\nCc: " + cc + "\nSubject: " + subject + "\nSendDate: " + sendDate + "\nMessageId: " + messageId;
        }
    }

    private final MediaType[] mimeTypes = {Detector.MEDIA_TYPE_APPLICATION_MSG};

    public MsgExtractor(File file) {
        super(file);
    }

    @Override
    public MediaType[] getMediaTypesSupported() {
        return mimeTypes;
    }

    @Override
    public boolean isExtractable() throws ExtractorException {
        return true;
    }

    @Override
    public ArrayList<ExtractorResult> extract(File outputDir, String nameForCreatedFile) throws ExtractorException {

        try {
            MsgExtractorResult extractedMsg = extractMsg(outputDir);

            // estraggo gli allegati dal risultato dell'estrazione dell file msg
            ArrayList<ExtractorResult> attachments = extractedMsg.getAttachments();

            String text = extractedMsg.getText();
            MediaType textMediaType = extractedMsg.getTextMediaType();
            //System.err.println("aaaa\n:" + text);

            // se trovo del testo creo un file contenente il testo trovato, se anche questo è nullo, il file msg è senza testo e quindi passo direttamente agli allegati
            if (text != null) {
                String textFileExt = null;
                if (textMediaType != null) {
                    textFileExt = Extractor.getFileExtension(textMediaType);
                } else {
                    // calcolo l'estensione del file di testo (potrebbe essere html o txt)
                    ByteArrayInputStream bais = null;
                    try {
                        bais = new ByteArrayInputStream(text.getBytes("UTF-8"));
                        textFileExt = getFileExtension(bais);
                    } finally {
                        IOUtils.closeQuietly(bais);
                    }
                }

                // creo il file assicurandomi prima che non esista già un file con lo stesso nome
                String textFileName;
                if (nameForCreatedFile == null) {
                    textFileName = Extractor.removeExtensionFromFileName(file.getName()) + "_testo." + textFileExt;
                } else {
                    textFileName = nameForCreatedFile + "_testo." + textFileExt;
                }
                File textFile = atomicCreateNotExistingFile(outputDir, textFileName);
                writeFileFromString(text, textFile);

                // aggiungo agli allegati estratti il file creato
                ExtractorResult extractorResult = new ExtractorResult(textFileName, getMimeType(textFile), textFile.length(), getHashFromFile(textFile, "SHA-256"), textFile.getAbsolutePath(), -1, null,null);
                attachments.add(extractorResult);
            }
            return attachments;
        } catch (ExtractorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExtractorException(ex, file.getName(), "msg text file");
        }
    }

    public MsgExtractorResult extractMsg(File outputDir) throws ExtractorException {

        if (!outputDir.exists()) {
            outputDir.mkdir();
        }
        if (!outputDir.isDirectory()) {
            throw new ExtractorException(outputDir.getAbsolutePath() + " non è una directory", file.getName(), null);
        }

        MsgExtractorResult res;
        ArrayList<ExtractorResult> msgAttachments = new ArrayList<>();
        ExtractorResult extractorResult;
        try {
            MAPIMessage msg = new MAPIMessage(file.getAbsolutePath());
            // estraggo il testo dal file, prima provo estraendolo in formato html
            String text = null;
            MediaType textMediaType = null;
            try {
                text = msg.getHtmlBody();
                textMediaType = MediaType.TEXT_HTML;
            } catch (ChunkNotFoundException e) {
//               try {
//                text = msg.getRtfBody();
//                textMediaType = MediaType.application("rtf");
//                } catch (ChunkNotFoundException subEx) {
//                
//                } 
            }

            // se è nullo potrebbe essere non html, riprovo estraendo il plain/text
            if (text != null && !text.isEmpty()) {
                textMediaType = MediaType.TEXT_HTML;
            } else { // se è nullo potrebbe essere non html, riprovo estraendo il plain/text
                text = msg.getTextBody();
            }

            // gestisco gli allegati
            AttachmentChunks[] attachments = msg.getAttachmentFiles();
            if (attachments.length > 0) {
                if (!outputDir.exists() || !outputDir.canWrite()) {
                    throw new ExtractorException("Working dir: '" + outputDir.getAbsolutePath() + "' must exists and be writeable :F", file.getName(), file.getName());
                }
                for (AttachmentChunks attachment : attachments) {
                    File attachmentFile = null;
                    try {
                        String fileName = null;
                        byte[] attachmentBytes;
                        if (attachment.isEmbeddedMessage()) {
                            //try (FileOutputStream out = new FileOutputStream("D:\\tmp\\testAll\\gdm.txt")) {
                            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                                attachment.getAll()[0].writeValue(out);
                                fileName = out.toString() + ".msg";
                            }
                            attachmentBytes = attachment.getEmbeddedAttachmentObject();
                        } else {
                            if (attachment.getAttachLongFileName() != null) {
                                fileName = attachment.getAttachLongFileName().toString();
                            } else if (attachment.getAttachFileName() != null) {
                                fileName = attachment.getAttachFileName().toString();
                            }
                            attachmentBytes = attachment.getEmbeddedAttachmentObject();
                        }
                        if (fileName == null) {
                            fileName = attachment.getPOIFSName();
                        }
                        fileName = fileName.replaceAll("[^(\\x20-\\x7F)]", "").replaceAll("[^a-zA-Z0-9\\.\\-_\\+ ]", "_");

                        //se esista gia' aggiungiamo numeri in fondo.
                        attachmentFile = new File(outputDir, fileName);
                        int i = 1;
                        while (!attachmentFile.createNewFile()) {
                            attachmentFile = new File(Extractor.removeExtensionFromFileName(attachmentFile.getAbsolutePath()) + "_" + i + "." + Extractor.getExtensionFromFileName(attachmentFile.getAbsolutePath()));
                            i++;
                        }

                        if (!attachment.isEmbeddedMessage()) {
                            try (OutputStream fileOut = new FileOutputStream(attachmentFile)) {
                                fileOut.write(attachmentBytes);

                            } catch (Exception ex) {
                                throw new ExtractorException("errore nell'estrazione del file " + fileName, ex, file.getName(), fileName);
                            }
                        } else {
                            try (OutputStream fileOut = new FileOutputStream(attachmentFile)) {
                                POIFSFileSystem fs = rebuildFromAttached(attachment.getEmbeddedMessage());

                                fs.writeFilesystem(fileOut);

                            } catch (Exception ex) {
                                throw new ExtractorException("errore nell'estrazione del file " + fileName, ex, file.getName(), fileName);
                            }
                        }
                        extractorResult = new ExtractorResult(fileName, getMimeType(attachmentFile), attachmentFile.length(), getHashFromFile(attachmentFile, "SHA-256"), attachmentFile.getAbsolutePath(), -1,null,null);
                        msgAttachments.add(extractorResult);
                    } catch (Exception ex) {
                        if (attachmentFile != null && attachmentFile.exists()) {
                            try {
                                attachmentFile.delete();
                            } catch (Exception subEx) {
                            }
                        }
                        throw ex;
                    }
                }
            }

            String messageId = msg.getMainChunks().getMessageId() != null ? msg.getMainChunks().getMessageId().getValue() : null;
            res = new MsgExtractorResult(msgAttachments, text, textMediaType, messageId, msg.getSubject(), msg.getDisplayTo(), msg.getDisplayCC(), msg.getMessageDate().getTime(), msg.getDisplayFrom());
        } catch (Exception ex) {
            throw new ExtractorException("errore nell'estrazione del messaggio o degli allegati", ex, file.getName(), null);
        }
        return res;
    }

    /**
     * funzione trovata su internet che non capisco cosa faccia, ma funziona.
     * Dovrebbe creare un oggetto POIFSFileSystem che poi permette di scrivere
     * il file msg su disco
     *
     * @param attachedMsg
     * @return
     * @throws IOException
     */
    private POIFSFileSystem rebuildFromAttached(MAPIMessage attachedMsg) throws IOException {
        // Create new MSG and copy properties.
        POIFSFileSystem newDoc = new POIFSFileSystem();
        MessagePropertiesChunk topLevelChunk = new MessagePropertiesChunk(null);
        // Copy attachments and recipients.
        int recipientscount = 0;
        int attachmentscount = 0;
        for (Entry entry : attachedMsg.getDirectory()) {
            if (entry.getName().startsWith(RecipientChunks.PREFIX)) {
                recipientscount++;
                DirectoryEntry newDir = newDoc.createDirectory(entry.getName());
                for (Entry e : ((DirectoryEntry) entry)) {
                    EntryUtils.copyNodeRecursively(e, newDir);
                }
            } else if (entry.getName().startsWith(AttachmentChunks.PREFIX)) {
                attachmentscount++;
                DirectoryEntry newDir = newDoc.createDirectory(entry.getName());
                for (Entry e : ((DirectoryEntry) entry)) {
                    EntryUtils.copyNodeRecursively(e, newDir);
                }
            }
        }
        // Copy properties from properties stream.
        MessagePropertiesChunk mpc = attachedMsg.getMainChunks().getMessageProperties();
        for (Map.Entry<MAPIProperty, PropertyValue> p : mpc.getRawProperties().entrySet()) {
            PropertyValue val = p.getValue();
            if (!(val instanceof ChunkBasedPropertyValue)) {
                MAPIType type = val.getActualType();
                if (type != null && type != Types.UNKNOWN) {
                    topLevelChunk.setProperty(val);
                }
            }
        }
        // Create nameid entries.
        DirectoryEntry nameid = newDoc.getRoot().createDirectory(NameIdChunks.NAME);
        // GUID stream
        nameid.createDocument(PropertiesChunk.DEFAULT_NAME_PREFIX + "00020102", new ByteArrayInputStream(new byte[0]));
        // Entry stream
        nameid.createDocument(PropertiesChunk.DEFAULT_NAME_PREFIX + "00030102", new ByteArrayInputStream(new byte[0]));
        // String stream
        nameid.createDocument(PropertiesChunk.DEFAULT_NAME_PREFIX + "00040102", new ByteArrayInputStream(new byte[0]));
        // Base properties.
        // Attachment/Recipient counter.
        topLevelChunk.setAttachmentCount(attachmentscount);
        topLevelChunk.setRecipientCount(recipientscount);
        topLevelChunk.setNextAttachmentId(attachmentscount);
        topLevelChunk.setNextRecipientId(recipientscount);
        // Unicode string format.
        byte[] storeSupportMaskData = new byte[4];
        PropertyValue.LongPropertyValue storeSupportPropertyValue = new PropertyValue.LongPropertyValue(MAPIProperty.STORE_SUPPORT_MASK,
                MessagePropertiesChunk.PROPERTIES_FLAG_READABLE | MessagePropertiesChunk.PROPERTIES_FLAG_WRITEABLE,
                storeSupportMaskData);
        storeSupportPropertyValue.setValue(0x00040000);
        topLevelChunk.setProperty(storeSupportPropertyValue);
        topLevelChunk.setProperty(new PropertyValue(MAPIProperty.HASATTACH,
                MessagePropertiesChunk.PROPERTIES_FLAG_READABLE | MessagePropertiesChunk.PROPERTIES_FLAG_WRITEABLE,
                attachmentscount == 0 ? new byte[]{0} : new byte[]{1}));
        // Copy properties from MSG file system.
        for (Chunk chunk : attachedMsg.getMainChunks().getChunks()) {
            if (!(chunk instanceof MessagePropertiesChunk)) {
                String entryName = chunk.getEntryName();
                String entryType = entryName.substring(entryName.length() - 4);
                int iType = Integer.parseInt(entryType, 16);
                MAPIType type = Types.getById(iType);
                if (type != null && type != Types.UNKNOWN) {
                    MAPIProperty mprop = MAPIProperty.createCustom(chunk.getChunkId(), type, chunk.getEntryName());
                    ByteArrayOutputStream data = new ByteArrayOutputStream();
                    chunk.writeValue(data);
                    PropertyValue pval = new PropertyValue(mprop, MessagePropertiesChunk.PROPERTIES_FLAG_READABLE
                            | MessagePropertiesChunk.PROPERTIES_FLAG_WRITEABLE, data.toByteArray(), type);
                    topLevelChunk.setProperty(pval);
                }
            }
        }
        topLevelChunk.writeProperties(newDoc.getRoot());
        return newDoc;
    }

    public MsgExtractorResult extractWithCumulativeText(File outputDir, String separator) throws ExtractorException {
        MsgExtractorResult extractedMsg = extractMsg(outputDir);
        String text = extractedMsg.getText();
        if (text == null) {
            text = "";
        } else {
            text += "\n" + separator + "\n";
        }
        text += recursiveAppendText(extractedMsg.getAttachments(), separator, outputDir, 0);
        extractedMsg.setText(text);
        return extractedMsg;
    }

    private String recursiveAppendText(ArrayList<ExtractorResult> files, String separator, File workingDir, int level) throws ExtractorException {
        String res = "";
        try {

            ArrayList<File> msgFiles = new ArrayList();
            for (ExtractorResult attachment : files) {
                File attFile = new File(attachment.getPath());
                String mimeType = attachment.getMimeType();
                MediaType mediaType = MediaType.parse(mimeType);
                if (mediaType == Detector.MEDIA_TYPE_APPLICATION_MSG) {
                    msgFiles.add(attFile);
                } else {
                    if (level > 0) {
                        attFile.delete();
                    }
                }
            }

            if (msgFiles.size() > 0) {
                int i = 0;
                for (File msgFile : msgFiles) {
                    MsgExtractor msgExtractor = new MsgExtractor(msgFile);
                    MsgExtractorResult extractedMsg = null;
                    try {
                        extractedMsg = msgExtractor.extractMsg(workingDir);
                    } catch (ExtractorException ex) {
                        ex.printStackTrace(System.out);
                    }
                    if (extractedMsg != null) {
                        String text = extractedMsg.getText();
                        if (text == null) {
                            text = "";
                        }
                        if (i > 0) {
                            text = "\n" + separator + "\n" + text;
                        }
                        String innerText = recursiveAppendText(extractedMsg.getAttachments(), separator, workingDir, level + 1);
                        if (innerText != null && !innerText.equals("")) {
                            res += text + "\n" + separator + "\n" + innerText;
                        } else {
                            res += text + innerText;
                        }
                        i++;
                    }
                    if (level > 0) {
                        msgFile.delete();
                    }
                }
            }
            return res;
        } catch (ExtractorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExtractorException(ex, file.getName(), null);
        }
    }

    private void deepDeleteAllFiles(File dir, int level) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    deepDeleteAllFiles(f, ++level);
                } else {
                    f.delete();
                }
            }
            if (level > 0) {
                dir.delete();
            }
        }
    }
}
