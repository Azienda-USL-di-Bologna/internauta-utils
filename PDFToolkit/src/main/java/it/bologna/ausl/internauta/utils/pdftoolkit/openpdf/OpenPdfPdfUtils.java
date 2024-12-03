package it.bologna.ausl.internauta.utils.pdftoolkit.openpdf;

import com.lowagie.text.DocumentException;
import com.lowagie.text.exceptions.BadPasswordException;
import com.lowagie.text.pdf.PdfReader;
import org.slf4j.Logger;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static it.bologna.ausl.internauta.utils.pdftoolkit.openpdf.PenPdfFontUtils.embedFonts;
import java.util.ArrayList;

/**
 * @author ferri
 */
public class OpenPdfPdfUtils {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(OpenPdfPdfUtils.class);

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
    
    public static boolean isPdfAMarked(com.lowagie.text.pdf.PdfReader pdfReader) {
        try {
            return pdfReader.getMetadata() != null && new String(pdfReader.getMetadata()).contains("pdfaid:conformance=\"A\"");
        } catch (Exception ex) {
            return false;
        }
    }
    
    public static boolean isAllPdfAMarked(ArrayList<InputStream> files) {
        boolean isAllPdfAMarked = false;
        try {
            for (InputStream file : files) {
                try (com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(file)) {
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
    
    public static boolean isPdfOpenable(InputStream pdf) throws IOException {
        try (PdfReader reader = new PdfReader(pdf);) {
            return true;
        } catch (Exception ex) {
            log.error("errore nell'aprire il pdf", ex);
            return false;
        }
    }
    
    public static boolean isPdfProtected(InputStream pdf) throws IOException {
        try (PdfReader reader = new PdfReader(pdf);) {
            return true;
        } catch (BadPasswordException ex) {
            log.error("errore nell'aprire il pdf", ex);
            return false;
        }
    }
}
