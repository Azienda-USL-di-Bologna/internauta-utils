package it.bologna.ausl.internauta.utils.pdftoolkit.itext;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.ICC_Profile;
import org.slf4j.Logger;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextFontUtils.embedFonts;

/**
 * @author ferri
 */
public class ITextPdfUtils {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ITextPdfUtils.class);

    public static ByteArrayOutputStream getPdfA(ByteArrayOutputStream templateOutput, PdfACreationListener listener)
            throws DocumentException {

        ITextRenderer renderer = basicInitialization(templateOutput, listener);

        ByteArrayOutputStream pdfAOutput = new ByteArrayOutputStream();

        try {
            renderer.createPDF(pdfAOutput, true, true, listener.getPdfAConformanceLevel());

            return pdfAOutput;
        } catch (IOException e) {
            throw new DocumentException("Failed to create PDF in ITextRenderer with template title " + listener.getTitle(), e);
        }
    }

    public static String formatPathForTemplate(Path resourcePath) {
        return resourcePath.toString().replace("\\", "/");
    }

    public static ITextRenderer basicInitialization(ByteArrayOutputStream template, PdfACreationListener creationListener)
            throws DocumentException {
        try {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setScaleToFit(true);
            embedFonts(renderer, creationListener.getFontsDirectory());
            renderer.setListener(creationListener);
            renderer.setDocument(template.toByteArray());
            renderer.layout();

            return renderer;
        } catch (IOException e) {
            throw new DocumentException("Failed to create PDF in ITextRenderer with template " + creationListener.getTitle(), e);
        } finally {
            try {
                if (template != null) {
                    template.close();
                }
            } catch (IOException e) {
                throw new DocumentException("Failed to close template output stream", e);
            }
        }
    }

    public static void setICCProfile(ITextRenderer iTextRenderer, Path fileIcc) throws IOException {
        try (InputStream fileIccStream = new FileInputStream(fileIcc.toFile())) {
            iTextRenderer.getWriter().setOutputIntents("Adobe RGB (1998)", "PDFA/A",
                    "http://www.color.org", "IEC 61966-2-5:1999", ICC_Profile.getInstance(fileIccStream));
        } catch (IOException e) {
            throw new IOException("Failed to set ICC profile to pdf with path: " + fileIcc, e);
        }
    }
}
