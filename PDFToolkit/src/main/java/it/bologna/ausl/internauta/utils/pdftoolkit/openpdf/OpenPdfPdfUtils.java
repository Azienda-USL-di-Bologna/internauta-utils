package it.bologna.ausl.internauta.utils.pdftoolkit.openpdf;

import org.openpdf.text.DocumentException;
import org.openpdf.text.exceptions.BadPasswordException;
import org.openpdf.text.pdf.PdfEncryption;
import org.openpdf.text.pdf.PdfReader;
import org.slf4j.Logger;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static it.bologna.ausl.internauta.utils.pdftoolkit.openpdf.OpenPdfFontUtils.embedFonts;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

/**
 * @author ferri
 */
public class OpenPdfPdfUtils {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(OpenPdfPdfUtils.class);

    public static enum CheckPdfStatus {
        CORRUPTED, PROTECTED, OK
    }
    
    public static ByteArrayOutputStream getPdfA(ByteArrayOutputStream templateOutput, PdfACreationListener listener)
            throws DocumentException {

        ITextRenderer renderer = basicInitialization(templateOutput, listener);

        ByteArrayOutputStream pdfAOutput = new ByteArrayOutputStream();

        try {
            renderer.createPDF(pdfAOutput, true);

            return pdfAOutput;
        } catch (DocumentException e) {
            log.error("Failed to create PDF in ITextRenderer with template title " + listener.getTitle());
            throw e;
        }
    }

    public static String formatPathForTemplate(Path resourcePath) {
        return resourcePath.toString().replace("\\", "/");
    }

    public static ITextRenderer basicInitialization(ByteArrayOutputStream template, PdfACreationListener creationListener)
            throws DocumentException {
        try {
            ITextRenderer renderer = new ITextRenderer();
//            renderer.setScaleToFit(true);
            embedFonts(renderer, creationListener.getFontsDirectory());
            renderer.setListener(creationListener);
            renderer.setDocumentFromString(template.toString("UTF-8"));
            renderer.layout();

            return renderer;
        } catch (IOException e) {
            log.error("Failed to create PDF in ITextRenderer with template " + creationListener.getTitle());
            throw new DocumentException(e);
        } finally {
            try {
                if (template != null) {
                    template.close();
                }
            } catch (IOException e) {
                log.error("Failed to close template output stream" + creationListener.getTitle());
                throw new DocumentException(e);
            }
        }
    }

//    public static void setICCProfile(ITextRenderer iTextRenderer, Path fileIcc) throws IOException {
//        try (InputStream fileIccStream = new FileInputStream(fileIcc.toFile())) {
//            iTextRenderer.getWriter().setOutputIntents("Adobe RGB (1998)", "PDFA/A",
//                    "http://www.color.org", "IEC 61966-2-5:1999", ICC_Profile.getInstance(fileIccStream));
//        } catch (IOException e) {
//            throw new IOException("Failed to set ICC profile to pdf with path: " + fileIcc, e);
//        }
//    }
    
    public static boolean isPdfAMarked(org.openpdf.text.pdf.PdfReader pdfReader) {
        try {
            if (pdfReader.getMetadata() != null) {
                String regex = "pdfaid:conformance\\s*(>?\\s*|=\\s*\\\"?)\\s*A";
                Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                Matcher matcher = pattern.matcher(new String(pdfReader.getMetadata()));
                return matcher.find();
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }
    
    public static boolean isAllPdfAMarked(ArrayList<InputStream> files) {
        boolean isAllPdfAMarked = false;
        try {
            for (InputStream file : files) {
                try (org.openpdf.text.pdf.PdfReader reader = new org.openpdf.text.pdf.PdfReader(file)) {
                    isAllPdfAMarked = isPdfAMarked(reader);
                    if (!isAllPdfAMarked) {
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            isAllPdfAMarked = false;
        }
        return isAllPdfAMarked;
    }
    
    public static CheckPdfStatus checkPdf(InputStream pdf) throws IOException {
        try (PdfReader reader = new PdfReader(pdf);) {
            return CheckPdfStatus.OK;
        }  catch (BadPasswordException ex) {
            log.error("errore nell'aprire il pdf", ex);
            return CheckPdfStatus.PROTECTED;
        }
        catch (Exception ex) {
            log.error("errore nell'aprire il pdf", ex);
            return CheckPdfStatus.CORRUPTED;
        }
    }
    
    public static byte[] mergePdfOpenPdf(ArrayList<File> inputFiles, InputStream iccProfileStream, String title) throws IOException {
        boolean pdfa = true;
        InputStream is = null;
        org.openpdf.text.pdf.PdfReader reader = null;
        org.openpdf.text.Document document = null;
        org.openpdf.text.Document documentPdfA = null;
        org.openpdf.text.pdf.PdfCopy cp = null;
        org.openpdf.text.pdf.PdfCopy cpPdfA = null;
        ByteArrayOutputStream tempos = new ByteArrayOutputStream();
        ByteArrayOutputStream temposPdfA = new ByteArrayOutputStream();
        try {
            is = new FileInputStream(inputFiles.get(0));
            reader = new org.openpdf.text.pdf.PdfReader(is);
            removeSignsOpenPdf(reader);
            pdfa = pdfa && isPdfAMarked(reader);
            
            if (pdfa) {
                documentPdfA = new org.openpdf.text.Document(reader.getPageSizeWithRotation(1));
                cpPdfA = new org.openpdf.text.pdf.PdfCopy(documentPdfA, temposPdfA);
                cpPdfA.setPDFXConformance(org.openpdf.text.pdf.PdfWriter.PDFA1A);
                cpPdfA.setPdfVersion(org.openpdf.text.pdf.PdfWriter.PDF_VERSION_1_7);
                documentPdfA.open();
            } else {
                IOUtils.close(temposPdfA);
                temposPdfA = null;
            }
            document = new org.openpdf.text.Document(reader.getPageSizeWithRotation(1));
            cp = new org.openpdf.text.pdf.PdfCopy(document, tempos);
            document.open();
            for (int i = 0; i < inputFiles.size(); i++) {
                if (i > 0) {
                    is = new FileInputStream(inputFiles.get(i));
                    reader = new org.openpdf.text.pdf.PdfReader(is);
                }
                pdfa = pdfa && isPdfAMarked(reader);
                if (!pdfa && temposPdfA != null) {
                    IOUtils.close(temposPdfA);
                    temposPdfA = null;
                }
                removeSignsOpenPdf(reader);
                for (int k = 1; k <= reader.getNumberOfPages(); ++k) {
                    cp.addPage(cp.getImportedPage(reader, k));
                    if (pdfa) {
                        cpPdfA.addPage(cpPdfA.getImportedPage(reader, k));
                    }
                }
                cp.freeReader(reader);
                if (pdfa) {
                    cpPdfA.freeReader(reader);
                }
                IOUtils.closeQuietly(is);
            }
            if (title == null) {
                title = reader.getInfo().get(org.openpdf.text.pdf.PdfName.decodeName(org.openpdf.text.pdf.PdfName.TITLE.toString()));
            }
            cp.getInfo().put(org.openpdf.text.pdf.PdfName.TITLE, new org.openpdf.text.pdf.PdfString(title));
            if (pdfa) {
                cpPdfA.getInfo().put(org.openpdf.text.pdf.PdfName.TITLE, new org.openpdf.text.pdf.PdfString(title));
            }
            if (pdfa) {
                OpenPdfMetadataUtils.writeExtraCatalog(cpPdfA, iccProfileStream);
                OpenPdfMetadataUtils.writeXmpMetadata(cpPdfA);
            }
            cp.close();
            if (pdfa) {
                cpPdfA.close();
                documentPdfA.close();
            }
            document.close();
            if (pdfa) {
                return temposPdfA.toByteArray();
            } else {
                return tempos.toByteArray();
            }
        } finally {
            IOUtils.closeQuietly(is);
            try {
                reader.close();
            } catch (Exception ex) {
            }
            try {
                document.close();
            } catch (Exception ex) {
            }
            try {
                cp.close();
            } catch (Exception ex) {
            }
            if (pdfa) {
                try {
                    cpPdfA.close();
                } catch (Exception e) {
                }
                try {
                    documentPdfA.close();
                } catch (Exception e) {
                }
                IOUtils.closeQuietly(tempos);
            } else {
                IOUtils.closeQuietly(temposPdfA);
            }
        }
    }
    
    /**
     * Rimuove i campi firma dal reader. Utile da usare in caso di merge per evitare di avere i campi firma nel risultato
     * @param reader 
     */
    public static void removeSignsOpenPdf(org.openpdf.text.pdf.PdfReader reader) {
        try {
            List<String> signatureNames = reader.getAcroFields().getSignedFieldNames();
            org.openpdf.text.pdf.AcroFields acroFields = reader.getAcroFields();
            for (String signatureName : signatureNames) {
                // se la larghezza e l'altezza del rettangolo in cui la firma è posizionata sono diverso da 0 allora il campo firma è visibile.
                // se è visibile posso rimuovere il campo
                float[] fieldPosition = reader.getAcroFields().getFieldPositions(signatureName);
                if (fieldPosition != null && fieldPosition.length > 0) {
                    acroFields.removeField(signatureName);
                }
            }
        } catch (Exception ex) {
//            ex.printStackTrace();
            log.error("errore nella rimozione dei campi firma", ex);
        }

    }
}
