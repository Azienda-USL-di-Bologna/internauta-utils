package it.bologna.ausl.internauta.utils.pdftoolkit.itext;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.xml.xmp.DublinCoreProperties;
import com.itextpdf.xmp.XMPConst;
import com.itextpdf.xmp.XMPException;
import com.itextpdf.xmp.XMPMeta;
import com.itextpdf.xmp.XMPMetaFactory;
import com.itextpdf.xmp.impl.XMPSerializerRDF;
import com.itextpdf.xmp.options.SerializeOptions;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.DateUtils.getItaCurrentIsoOffsetDate;

/**
 * @author ferri
 */
public class ITextMetadataUtils {
    public static void setMetadata(ITextRenderer iTextRenderer, String fileTitle)
            throws XMPException {
        // bare minimum for standard B
        iTextRenderer.getWriter().createXmpMetadata();

        DublinCoreProperties.setTitle(iTextRenderer.getWriter().getXmpWriter().getXmpMeta(),
                fileTitle, Locale.US.getLanguage(), Locale.ITALY.getLanguage());
        DublinCoreProperties.addAuthor(iTextRenderer.getWriter().getXmpWriter().getXmpMeta(), "Babel");

        // plus bare minimum for standard A
        PdfDictionary markInfo = new PdfDictionary();
        markInfo.put(PdfName.MARKED, PdfBoolean.PDFTRUE);
        iTextRenderer.getWriter().getExtraCatalog().put(PdfName.MARKINFO, markInfo);
        iTextRenderer.getWriter().getStructureTreeRoot();
    }

    public static XMPMeta createXMPMetadata(String fileTitle, PdfAConformanceLevel conformanceLevel)
            throws XMPException {
        try {
            XMPMeta xmp = XMPMetaFactory.create();
            //writer.createXmpMetadata(); // align dates with producer
            String currentDate = getItaCurrentIsoOffsetDate();

            xmp.setProperty(XMPConst.NS_XMP, "CreateDate", currentDate);
            xmp.setProperty(XMPConst.NS_XMP, "ModifyDate", currentDate);

            DublinCoreProperties.addAuthor(xmp, "Babel");
            DublinCoreProperties.setTitle(xmp, fileTitle, Locale.US.getLanguage(), Locale.ITALY.getLanguage());

            xmp.setProperty(XMPConst.NS_PDFA_ID, "part", conformanceLevel.name().charAt(conformanceLevel.name().length() - 1));
            xmp.setProperty(XMPConst.NS_PDFA_ID, "conformance", conformanceLevel.name().charAt(conformanceLevel.name().length() - 2));

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
            stamper.getWriter().setXmpMetadata(getSerializedMetadata(createXMPMetadata(fileTitle, conformanceLevel)));
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
}
