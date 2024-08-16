package it.bologna.ausl.internauta.utils.pdftoolkit.itext;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.xml.xmp.DublinCoreProperties;
import com.itextpdf.xmp.*;
import com.itextpdf.xmp.impl.XMPSerializerRDF;
import com.itextpdf.xmp.options.ParseOptions;
import com.itextpdf.xmp.options.SerializeOptions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * @author ferri
 */
public class ITextMetadataUtils {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ITextMetadataUtils.class);

    public static void setMetadata(ITextRenderer iTextRenderer, String fileTitle)
            throws XMPException {
        iTextRenderer.getWriter().createXmpMetadata();

        DublinCoreProperties.setTitle(iTextRenderer.getWriter().getXmpWriter().getXmpMeta(),
                fileTitle, Locale.US.getLanguage(), Locale.ITALY.getLanguage());
        DublinCoreProperties.addAuthor(iTextRenderer.getWriter().getXmpWriter().getXmpMeta(), "Babel");

        PdfDictionary markInfo = new PdfDictionary();
        markInfo.put(PdfName.MARKED, PdfBoolean.PDFTRUE);
        iTextRenderer.getWriter().getExtraCatalog().put(PdfName.MARKINFO, markInfo);
        iTextRenderer.getWriter().getStructureTreeRoot();
    }

    public static XMPMeta createXMPMetadata(PdfWriter writer, String fileTitle, PdfAConformanceLevel conformanceLevel)
            throws XMPException {
        try {
            XMPMeta xmp = XMPMetaFactory.create();
            writer.createXmpMetadata();
            String currentDate = XMPUtils.convertFromDate(writer.getXmpWriter().getXmpMeta().getPropertyDate(XMPConst.NS_XMP, "CreateDate"));

            xmp.setProperty(XMPConst.NS_XMP, "CreateDate", currentDate);
            xmp.setProperty(XMPConst.NS_XMP, "ModifyDate", currentDate);

            DublinCoreProperties.addAuthor(xmp, "BABEL");
            DublinCoreProperties.setTitle(xmp, fileTitle, Locale.US.getLanguage(), Locale.ITALY.getLanguage());

            String conformance;
            String part;
            if (StringUtils.contains(conformanceLevel.name(), PdfAConformanceLevel.ZUGFeRD.name())) {
                part = "3";
                conformance = "A";
            } else {
                part = String.valueOf(conformanceLevel.name().charAt(conformanceLevel.name().length() - 2));
                conformance = String.valueOf(conformanceLevel.name().charAt(conformanceLevel.name().length() - 1));
            }
            xmp.setProperty(XMPConst.NS_PDFA_ID, "part", part);
            xmp.setProperty(XMPConst.NS_PDFA_ID, "conformance", conformance);

            return xmp;
        } catch (XMPException e) {
            throw new XMPException("Failed to create XMP Metadata", e.getErrorCode(), e.getCause());
        }
    }

    public static SerializeOptions getSerializeOptions() {
        SerializeOptions serializeOptions = new SerializeOptions();
        serializeOptions.setSort(true);
        serializeOptions.setReadOnlyPacket(true);
        serializeOptions.setUseCompactFormat(true);
        serializeOptions.setExactPacketLength(true);
        return serializeOptions;
    }

    public static void setMetadata(PdfStamper stamper, String fileTitle, PdfAConformanceLevel conformanceLevel)
            throws DocumentException {
        try {
            stamper.setXmpMetadata(getSerializedMetadata(createXMPMetadata(stamper.getWriter(), fileTitle, conformanceLevel)));
        } catch (XMPException e) {
            throw new DocumentException("Failed to set Metadata to pdf using PdfStamper", e);
        }
    }

    public static byte[] getSerializedMetadata(XMPMeta xmp)
            throws DocumentException {
        try (ByteArrayOutputStream metadataOutputStream = new ByteArrayOutputStream()) {
            XMPSerializerRDF serializer = new XMPSerializerRDF();
            serializer.serialize(xmp, metadataOutputStream, getSerializeOptions());
            return metadataOutputStream.toByteArray();
        } catch (IOException | XMPException e) {
            throw new DocumentException("Failed to serialize Metadata for pdf", e);
        }
    }

    public static XMPMeta getDeserializedMetadata(byte[] xmp)
            throws DocumentException {
        try (ByteArrayInputStream metadataInputStream = new ByteArrayInputStream(xmp)) {
            return XMPMetaFactory.parse(metadataInputStream, new ParseOptions().setRequireXMPMeta(true));
        } catch (IOException | XMPException e) {
            throw new DocumentException("Failed to deserialize Metadata for pdf", e);
        }
    }
}
