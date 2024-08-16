package it.bologna.ausl.estrattore;

import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerAttachment;
import it.bologna.ausl.eml.handler.EmlHandlerResult;
import it.bologna.ausl.estrattore.exception.ExtractorException;
import it.bologna.ausl.mimetypeutilities.Detector;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import org.apache.commons.io.IOUtils;
import org.apache.tika.mime.MediaType;

/**
 *
 * @author Giuseppe De Marco (gdm)
 */
public class EmlExtractor extends Extractor {

    public class EmlExtractorResult {

        private ArrayList<ExtractorResult> attachments;
        private String messageId;
        private String text;
        private MediaType textMediaType;
        private String subject;
        private String[] to;
        private String[] cc;
        private Date sendDate;
        private String from;

        public EmlExtractorResult(ArrayList<ExtractorResult> attachments, String text, MediaType textMediaType, String messageId, String subject, String[] to, String[] cc, Date sendDate, String from) {
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

        public String[] getCc() {
            return cc;
        }

        public void setCc(String[] cc) {
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

        public String[] getTo() {
            return to;
        }

        public void setTo(String[] to) {
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
            return "From: " + from + "\nTo: " + Arrays.toString(to) + "\nCc: " + Arrays.toString(cc) + "\nSubject: " + subject + "\nSendDate: " + sendDate + "\nMessageId: " + messageId;
        }
    }

    private final MediaType[] mimeTypes = {Detector.MEDIA_TYPE_MESSAGE_RFC822, Detector.MEDIA_TYPE_APPLICATION_MBOX};

    public EmlExtractor(File file) {
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
            EmlExtractorResult extractedEml = extractEml(outputDir);

            // estraggo gli allegati dal risultato dell'estrazione dell'eml
            ArrayList<ExtractorResult> attachments = extractedEml.getAttachments();

            String text = extractedEml.getText();
            MediaType textMediaType = extractedEml.getTextMediaType();
            //System.err.println("aaaa\n:" + text);

            // se trovo del testo creo un file contenente il testo trovato, se anche questo è nullo l'eml è senza testo e quindi passo direttamente
            // agli allegati
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
                // ExtractorResult extractorResult = new ExtractorResult(textFileName, getMimeType(textFile), textFile.length(), getHashFromFile(textFile, "SHA-256"), textFile.getAbsolutePath(),-1);
                ExtractorResult extractorResult = new ExtractorResult(textFileName, textMediaType.toString(), textFile.length(), getHashFromFile(textFile, "SHA-256"), textFile.getAbsolutePath(), -1, null, null);
                attachments.add(extractorResult);
            }
            return attachments;
        } catch (ExtractorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExtractorException(ex, file.getName(), "eml text file");
        }
    }

    public EmlExtractorResult extractEml(File outputDir) throws ExtractorException {

        if (!outputDir.exists()) {
            outputDir.mkdir();
        }
        if (!outputDir.isDirectory()) {
            throw new ExtractorException(outputDir.getAbsolutePath() + " non è una directory", file.getName(), null);
        }

        EmlExtractorResult res;
        ArrayList<ExtractorResult> emlAttachments = new ArrayList<>();
        ExtractorResult extractorResult;
        EmlHandlerResult emlHandlerResult;
        try {
            emlHandlerResult = EmlHandler.handleEml(file.getAbsolutePath(), outputDir.getAbsolutePath(), true);

            MediaType textMediaType = null;
            // estraggo il testo dal file, prima provo estraendolo in formato html
            String text = emlHandlerResult.getHtmlText();

            if (text != null && !text.isEmpty()) {
                textMediaType = MediaType.TEXT_HTML;
            } else { // se è nullo potrebbe essere non html, riprovo estraendo il plain/text
                text = emlHandlerResult.getPlainText();
                textMediaType = MediaType.TEXT_PLAIN;
            }

            // gestisco gli allegati. Sono già stati creati dall'emlHandler, quindi devo solo aggiungere i file creati al risultato
            EmlHandlerAttachment[] attachments = emlHandlerResult.getAttachments();
            if (attachments != null) {
                for (EmlHandlerAttachment attachment : attachments) {
                    File attFile = new File(attachment.getFilePath());
                    if (!attFile.exists()) {
                        throw new ExtractorException("file allegato atteso non trovato: " + attFile.getName() + "\n" + attFile.getAbsolutePath(), file.getName(), attachment.getFileName());
                    }

                    extractorResult = new ExtractorResult(attFile.getName(), getMimeType(attFile), attFile.length(), getHashFromFile(attFile, "SHA-256"), attFile.getAbsolutePath(), -1, null, null);
                    emlAttachments.add(extractorResult);
                }
            }

            res = new EmlExtractorResult(emlAttachments, text, textMediaType, emlHandlerResult.getMessageId(), emlHandlerResult.getSubject(), emlHandlerResult.getTo(), emlHandlerResult.getCc(), emlHandlerResult.getSendDate(), emlHandlerResult.getFrom());
        } catch (Exception ex) {
            throw new ExtractorException("errore nell'estrazione del messaggio o degli allegati", ex, file.getName(), null);
        }
        return res;
    }

    public EmlExtractorResult extractWithCumulativeText(File outputDir, String separator) throws ExtractorException {
        EmlExtractorResult extractedEml = extractEml(outputDir);
        String text = extractedEml.getText();
        if (text == null) {
            text = "";
        } else {
            text += "\n" + separator + "\n";
        }
        text += recursiveAppendText(extractedEml.getAttachments(), separator, outputDir, 0);
        extractedEml.setText(text);
        return extractedEml;
    }

//    public EmlExtractorResult extractWithCumulativeText(String separator) throws ExtractorException {
//        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
//        return extractWithCumulativeText(tmpDir, separator);
//    }
    private String recursiveAppendText(ArrayList<ExtractorResult> files, String separator, File workingDir, int level) throws ExtractorException {
        String res = "";
        try {

            ArrayList<File> emlFiles = new ArrayList<File>();
            for (ExtractorResult attachment : files) {
                File attFile = new File(attachment.getPath());
                String mimeType = attachment.getMimeType();
                MediaType mediaType = MediaType.parse(mimeType);
                if (mediaType == Detector.MEDIA_TYPE_MESSAGE_RFC822) {
                    emlFiles.add(attFile);
                } else {
                    if (level > 0) {
                        attFile.delete();
                    }
                }
            }

            if (emlFiles != null && emlFiles.size() > 0) {
                int i = 0;
                for (File emlFile : emlFiles) {
                    EmlExtractor emlExtractor = new EmlExtractor(emlFile);
                    EmlExtractorResult extractedEml = null;
                    try {
                        extractedEml = emlExtractor.extractEml(workingDir);
                    } catch (ExtractorException ex) {
                        ex.printStackTrace(System.out);
                    }
                    if (extractedEml != null) {
                        String text = extractedEml.getText();
                        if (text == null) {
                            text = "";
                        }
                        if (i > 0) {
                            text = "\n" + separator + "\n" + text;
                        }
                        String innerText = recursiveAppendText(extractedEml.getAttachments(), separator, workingDir, level + 1);
                        if (innerText != null && !innerText.equals("")) {
                            res += text + "\n" + separator + "\n" + innerText;
                        } else {
                            res += text + innerText;
                        }
                        i++;
                    }
                    if (level > 0) {
                        emlFile.delete();
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
