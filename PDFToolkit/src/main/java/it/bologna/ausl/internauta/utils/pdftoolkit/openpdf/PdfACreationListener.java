package it.bologna.ausl.internauta.utils.pdftoolkit.openpdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.PDFCreationListener;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams.*;

/**
 * @author ferri
 */
public class PdfACreationListener implements PDFCreationListener {
    private static final Logger log = LoggerFactory.getLogger(PdfACreationListener.class);
    private final String title; // must be identical to html metadata title (PDF/A requirement)

    private final List<Path> fontsDirectory;

    private Path fileIcc = Paths.get(WORKDIR, RESOURCES_RELATIVE_PATH, "/AdobeRGB1998.icc");

    private int pdfAConformanceLevel = com.lowagie.text.pdf.PdfWriter.PDFA1A;

    public PdfACreationListener(String title, List<Path> fontsDirectory, Path fileIcc, int pdfAConformanceLevel) {
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
        iTextRenderer.getWriter().setTagged(); // a che serve?
        com.lowagie.text.pdf.PdfWriter writer = iTextRenderer.getWriter();
        writer.setPDFXConformance(com.lowagie.text.pdf.PdfWriter.PDFA1A);
        writer.setPdfVersion(com.lowagie.text.pdf.PdfWriter.PDF_VERSION_1_7);
        
    }

    @Override
    public void preWrite(ITextRenderer iTextRenderer, int pageCount) {
//        try {
//            iTextRenderer.getWriter().setLanguage(Locale.ITALY.getLanguage());
//            setMetadata(iTextRenderer, title);
//            setICCProfile(iTextRenderer, fileIcc);
//        } catch (XMPException e) {
//            log.error("ITextRenderer's pre writer failed to set up metadata", e);
//        } catch (IOException e) {
//            log.error("ITextRenderer's pre writer failed to set up icc profile", e);
//        }
    }

    @Override
    public void onClose(ITextRenderer iTextRenderer) {
        com.lowagie.text.pdf.PdfWriter writer = iTextRenderer.getWriter();
        try {
            writer.getInfo().put(com.lowagie.text.pdf.PdfName.TITLE, new com.lowagie.text.pdf.PdfString(title));
//            iTextRenderer.getWriter().setLanguage(Locale.ITALY.getLanguage());
            OpenPdfMetadataUtils.writeExtraCatalog(writer, fileIcc.toFile());
            OpenPdfMetadataUtils.writeXmpMetadata(writer);
        } catch (Exception ex) {
            log.error("ITextRenderer's pre writer failed close rhe renderer", ex);
        }
    }

    public String getTitle() {
        return title;
    }

    public List<Path> getFontsDirectory() {
        return fontsDirectory;
    }

    public int getPdfAConformanceLevel() {
        return pdfAConformanceLevel;
    }
}
