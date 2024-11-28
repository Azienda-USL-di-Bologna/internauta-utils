package it.bologna.ausl.internauta.utils.pdftoolkit.itext;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.ICC_Profile;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfObject;
import org.slf4j.Logger;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextFontUtils.embedFonts;
import java.awt.color.ICC_Profile;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

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

//    public static void setICCProfile(ITextRenderer iTextRenderer, Path fileIcc) throws IOException {
//        try (InputStream fileIccStream = new FileInputStream(fileIcc.toFile())) {
//            iTextRenderer.getWriter().setOutputIntents("Adobe RGB (1998)", "PDFA/A",
//                    "http://www.color.org", "IEC 61966-2-5:1999", ICC_Profile.getInstance(fileIccStream));
//        } catch (IOException e) {
//            throw new IOException("Failed to set ICC profile to pdf with path: " + fileIcc, e);
//        }
//    }
    
    public static void writeExtraCatalog(com.lowagie.text.pdf.PdfWriter writer, InputStream iccProfileStream) throws IOException{
                        
        com.lowagie.text.pdf.PdfDictionary structureTreeRoot = new com.lowagie.text.pdf.PdfDictionary();
        structureTreeRoot.put(com.lowagie.text.pdf.PdfName.TYPE, com.lowagie.text.pdf.PdfName.STRUCTTREEROOT);
        writer.getExtraCatalog().put(com.lowagie.text.pdf.PdfName.STRUCTTREEROOT, structureTreeRoot);

        com.lowagie.text.pdf.PdfDictionary markInfo = new com.lowagie.text.pdf.PdfDictionary(com.lowagie.text.pdf.PdfName.MARKINFO);
        markInfo.put(com.lowagie.text.pdf.PdfName.MARKED, new com.lowagie.text.pdf.PdfBoolean(true));
        writer.getExtraCatalog().put(com.lowagie.text.pdf.PdfName.MARKINFO, markInfo);

        com.lowagie.text.pdf.PdfDictionary l = new com.lowagie.text.pdf.PdfDictionary(com.lowagie.text.pdf.PdfName.LANG);
        l.put(com.lowagie.text.pdf.PdfName.LANG, new com.lowagie.text.pdf.PdfBoolean("true"));
        writer.getExtraCatalog().put(com.lowagie.text.pdf.PdfName.LANG, l);

        java.awt.color.ICC_Profile icc = java.awt.color.ICC_Profile.getInstance(iccProfileStream);
        writer.setOutputIntents("Custom", "", "http://www.color.org", "sRGB IEC61966-2.1", icc);
    }
    
    public static void writeXmpMetadata(com.lowagie.text.pdf.PdfWriter writer, InputStream iccProfileStream) {
        try {
            java.text.SimpleDateFormat dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            String modifyDatePlaceHolder = "xmp:ModifyDate=\"[ModifyDate]\"";
            String xmpMetadata = """
                <?xpacket begin="\u00ef\u00bb\u00bf" id="W5M0MpCehiHzreSzNTczkc9d"?>
                    <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">
                        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                            <rdf:Description 
                                rdf:about=""
                                xmlns:dc="http://purl.org/dc/elements/1.1/"
                                xmlns:pdf="http://ns.adobe.com/pdf/1.3/"
                                xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                                xmlns:pdfaid="http://www.aiim.org/pdfa/ns/id/"
                                dc:format="application/pdf"
                                pdf:Producer="[Producer]"
                                xmp:CreateDate="[CreateDate]"
                                [ModifyDatePlaceHolder]
                                pdfaid:part="1"
                                pdfaid:conformance="A">
                                <dc:title>
                                    <rdf:Alt>
                                        <rdf:li xml:lang="x-default">[title]</rdf:li>
                                    </rdf:Alt>
                                </dc:title>
                            </rdf:Description>
                        </rdf:RDF>
                    </x:xmpmeta>
                <?xpacket end="w"?>
            """;

            // id
            xmpMetadata = xmpMetadata.replace("[id]", generateRandomString(24));

            // CreateDate
            Date creationDate = com.lowagie.text.pdf.PdfDate.decode(writer.getInfo().get(com.lowagie.text.pdf.PdfName.CREATIONDATE).toString()).getTime();
            String creationDateString = dateFormatter.format(creationDate);
            xmpMetadata = xmpMetadata.replace("[CreateDate]", creationDateString);

            // ModifyDate
            com.lowagie.text.pdf.PdfObject modifyDateObj = writer.getInfo().get(com.lowagie.text.pdf.PdfName.MODDATE);
            if (modifyDateObj != null) {
                com.lowagie.text.pdf.PdfDate.decode(modifyDateObj.toString()).getTime();
                String modifyDateString = dateFormatter.format(modifyDateObj);
                xmpMetadata = xmpMetadata
                    .replace("[ModifyDatePlaceHolder]", modifyDatePlaceHolder)
                    .replace("[ModifyDate]", modifyDateString);

            } else {
                xmpMetadata = xmpMetadata.replaceAll(".*\\[ModifyDatePlaceHolder\\]\n", "");
            }

            // title
            PdfObject titleObject = writer.getInfo().get(com.lowagie.text.pdf.PdfName.TITLE);
            String title = titleObject != null ? titleObject.toString(): "NO TITLE";
            xmpMetadata = xmpMetadata.replace("[title]", title);

            // producer
            PdfObject producerObject = writer.getInfo().get(com.lowagie.text.pdf.PdfName.PRODUCER);
            String producer = producerObject != null ? producerObject.toString(): "OpenPdf";
            xmpMetadata = xmpMetadata.replace("[Producer]", producer);

            writer.setXmpMetadata(xmpMetadata.getBytes());    
        } catch (Exception ex) {
            log.error("errore nella crezione degli XMPMetadata", ex);
        }
    }
    
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
    
    public static String generateRandomString(int lenght) {
        String randomString = new Random().ints(lenght, 0, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".length())
        .mapToObj(i -> "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i))
        .collect(StringBuilder::new, StringBuilder::append, (stringBuilder, s) -> stringBuilder.append(s))
        .toString();
        return randomString;
    }
}
