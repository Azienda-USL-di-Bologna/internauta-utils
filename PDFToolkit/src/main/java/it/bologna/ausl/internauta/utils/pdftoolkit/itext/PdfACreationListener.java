package it.bologna.ausl.internauta.utils.pdftoolkit.itext;

import com.itextpdf.text.pdf.PdfAConformanceLevel;
import com.itextpdf.xmp.XMPException;
import it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.PDFCreationListener;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextMetadataUtils.setMetadata;
import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextPdfUtils.setICCProfile;

/**
 * @author ferri
 */
public class PdfACreationListener implements PDFCreationListener {
    private static final Logger log = LoggerFactory.getLogger(PdfACreationListener.class);
    private final String title; // must be identical to html metadata title (PDF/A requirement)

    private final List<Path> fontsDirectory;

    private Path fileIcc = Paths.get(PdfToolkitConfigParams.DIRECTORY_FOLDER_PATH.toString(), "AdobeRGB1998.icc");

    private PdfAConformanceLevel pdfAConformanceLevel = PdfAConformanceLevel.PDF_A_1A;

    public PdfACreationListener(String title, List<Path> fontsDirectory, Path fileIcc, PdfAConformanceLevel pdfAConformanceLevel) {
        this.title = title;
        this.fontsDirectory = fontsDirectory;
        this.fileIcc = fileIcc;
        this.pdfAConformanceLevel = pdfAConformanceLevel;
    }

    public PdfACreationListener(String title, List<Path> fontsDirectory, Path fileIcc) {
        this.title = title;
        this.fontsDirectory = fontsDirectory;
        this.fileIcc = fileIcc;
    }

    public PdfACreationListener(String title, List<Path> fontsDirectory) {
        this.title = title;
        this.fontsDirectory = fontsDirectory;
    }

    @Override
    public void preOpen(ITextRenderer iTextRenderer) {
        // using PDF/A auto-set pdf version (PDF/A1 and PDF/B1 = version 4, others version 7)
        iTextRenderer.getWriter().setTagged();
    }

    @Override
    public void preWrite(ITextRenderer iTextRenderer, int pageCount) {
        try {
            iTextRenderer.getWriter().setLanguage(Locale.ITALY.getLanguage());
            setMetadata(iTextRenderer, title);
            setICCProfile(iTextRenderer, fileIcc);
        } catch (XMPException e) {
            log.error("ITextRenderer's pre writer failed to set up metadata", e);
        } catch (IOException e) {
            log.error("ITextRenderer's pre writer failed to set up icc profile", e);
        }
    }

    @Override
    public void onClose(ITextRenderer iTextRenderer) {
    }

    public String getTitle() {
        return title;
    }

    public List<Path> getFontsDirectory() {
        return fontsDirectory;
    }

    public PdfAConformanceLevel getPdfAConformanceLevel() {
        return pdfAConformanceLevel;
    }
}
