package it.bologna.ausl.internauta.utils.pdftoolkit.openpdf;

import com.lowagie.text.pdf.PdfObject;
import java.io.File;
import java.io.FileInputStream;
import org.slf4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Random;

/**
 * @author ferri
 */
public class OPenPdfMetadataUtils {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(OPenPdfMetadataUtils.class);

    public static void writeExtraCatalog(com.lowagie.text.pdf.PdfWriter writer, File iccProfileStream) throws IOException {
                        
        try (InputStream is = new FileInputStream(iccProfileStream)) {
            writeExtraCatalog(writer, is);
        }
    }
    
    public static void writeExtraCatalog(com.lowagie.text.pdf.PdfWriter writer, InputStream iccProfileStream) throws IOException {
                        
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
    
    public static void writeXmpMetadata(com.lowagie.text.pdf.PdfWriter writer) {
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
    
    public static String generateRandomString(int lenght) {
        String randomString = new Random().ints(lenght, 0, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".length())
        .mapToObj(i -> "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i))
        .collect(StringBuilder::new, StringBuilder::append, (stringBuilder, s) -> stringBuilder.append(s))
        .toString();
        return randomString;
    }
    
    
//    public static void setMetadata(ITextRenderer iTextRenderer, String fileTitle)
//            throws XMPException {
//        iTextRenderer.getWriter().createXmpMetadata();
//
//        DublinCoreProperties.setTitle(iTextRenderer.getWriter().getXmpWriter().getXmpMeta(),
//                fileTitle, Locale.US.getLanguage(), Locale.ITALY.getLanguage());
//        DublinCoreProperties.addAuthor(iTextRenderer.getWriter().getXmpWriter().getXmpMeta(), "Babel");
//
//        PdfDictionary markInfo = new PdfDictionary();
//        markInfo.put(PdfName.MARKED, PdfBoolean.PDFTRUE);
//        iTextRenderer.getWriter().getExtraCatalog().put(PdfName.MARKINFO, markInfo);
//        iTextRenderer.getWriter().getStructureTreeRoot();
//    }
//
//    public static XMPMeta createXMPMetadata(PdfWriter writer, String fileTitle, PdfAConformanceLevel conformanceLevel)
//            throws XMPException {
//        try {
//            XMPMeta xmp = XMPMetaFactory.create();
//            writer.createXmpMetadata();
//            String currentDate = XMPUtils.convertFromDate(writer.getXmpWriter().getXmpMeta().getPropertyDate(XMPConst.NS_XMP, "CreateDate"));
//
//            xmp.setProperty(XMPConst.NS_XMP, "CreateDate", currentDate);
//            xmp.setProperty(XMPConst.NS_XMP, "ModifyDate", currentDate);
//
//            DublinCoreProperties.addAuthor(xmp, "BABEL");
//            DublinCoreProperties.setTitle(xmp, fileTitle, Locale.US.getLanguage(), Locale.ITALY.getLanguage());
//
//            String conformance;
//            String part;
//            if (StringUtils.contains(conformanceLevel.name(), PdfAConformanceLevel.ZUGFeRD.name())) {
//                part = "3";
//                conformance = "A";
//            } else {
//                part = String.valueOf(conformanceLevel.name().charAt(conformanceLevel.name().length() - 2));
//                conformance = String.valueOf(conformanceLevel.name().charAt(conformanceLevel.name().length() - 1));
//            }
//            xmp.setProperty(XMPConst.NS_PDFA_ID, "part", part);
//            xmp.setProperty(XMPConst.NS_PDFA_ID, "conformance", conformance);
//
//            return xmp;
//        } catch (XMPException e) {
//            throw new XMPException("Failed to create XMP Metadata", e.getErrorCode(), e.getCause());
//        }
//    }

//    public static SerializeOptions getSerializeOptions() {
//        SerializeOptions serializeOptions = new SerializeOptions();
//        serializeOptions.setSort(true);
//        serializeOptions.setReadOnlyPacket(true);
//        serializeOptions.setUseCompactFormat(true);
//        serializeOptions.setExactPacketLength(true);
//        return serializeOptions;
//    }

//    public static void setMetadata(PdfStamper stamper, String fileTitle, PdfAConformanceLevel conformanceLevel)
//            throws DocumentException {
//        try {
//            stamper.setXmpMetadata(getSerializedMetadata(createXMPMetadata(stamper.getWriter(), fileTitle, conformanceLevel)));
//        } catch (XMPException e) {
//            throw new DocumentException("Failed to set Metadata to pdf using PdfStamper", e);
//        }
//    }

//    public static byte[] getSerializedMetadata(XMPMeta xmp)
//            throws DocumentException {
//        try (ByteArrayOutputStream metadataOutputStream = new ByteArrayOutputStream()) {
//            XMPSerializerRDF serializer = new XMPSerializerRDF();
//            serializer.serialize(xmp, metadataOutputStream, getSerializeOptions());
//            return metadataOutputStream.toByteArray();
//        } catch (IOException | XMPException e) {
//            throw new DocumentException("Failed to serialize Metadata for pdf", e);
//        }
//    }

//    public static XMPMeta getDeserializedMetadata(byte[] xmp)
//            throws DocumentException {
//        try (ByteArrayInputStream metadataInputStream = new ByteArrayInputStream(xmp)) {
//            return XMPMetaFactory.parse(metadataInputStream, new ParseOptions().setRequireXMPMeta(true));
//        } catch (IOException | XMPException e) {
//            throw new DocumentException("Failed to deserialize Metadata for pdf", e);
//        }
//    }
}
