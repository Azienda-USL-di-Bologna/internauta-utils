package it.bologna.ausl.internauta.utils.pdftoolkit.itext;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfAConformanceLevel;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.xmp.XMPConst;
import com.itextpdf.xmp.XMPMeta;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate.ReporterJobWorkerData;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.FreeMarkerUtils.*;
import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextFontUtils.getFontFilePaths;
import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextMetadataUtils.getDeserializedMetadata;
import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextMetadataUtils.setMetadata;
import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextPdfUtils.formatPathForTemplate;
import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextPdfUtils.getPdfA;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.HtmlUtils.getFontFamilies;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams.TEMPLATES_RELATIVE_PATH;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParamsTest.TEST_DIRECTORY_FOLDER_PATH;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ferri
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ITextPdfUtilsTest {
    private static final Version defaultVersion = Configuration.VERSION_2_3_20;

    @Test
    void MetadataPdfStamperAndPdfATest() throws TemplateModelException, DocumentException, IOException, MasterjobsWorkerException {
        String documentTitle = "Test";
        Map<String, Object> dataModel = TestData.getTestInitialized();
        dataModel.put("title", documentTitle);
        dataModel.put("resourcePath", formatPathForTemplate(TEST_DIRECTORY_FOLDER_PATH));

        ReporterJobWorkerData workerData = new ReporterJobWorkerData("toolkit",
                "data-and-functions-test.xhtml", "data-and-functions-test", dataModel);

        workerData.validateInputForPdfA(".ftlh", ".xhtml");

        Configuration configuration = getConfiguration(defaultVersion, getCustomFunctions(),
                Paths.get(TEST_DIRECTORY_FOLDER_PATH.toString(), TEMPLATES_RELATIVE_PATH));

        Template template = configuration.getTemplate(workerData.getTemplateName());

        try (ByteArrayOutputStream templateOutput = getTemplateOutput(template, dataModel)) {
            String htmlContent = templateOutput.toString(StandardCharsets.UTF_8.name());
            List<String> listFont = getFontFamilies(htmlContent);
            List<Path> listFontFilePaths = getFontFilePaths(listFont, TEST_DIRECTORY_FOLDER_PATH);
            assertAll(
                    () -> assertEquals(2, listFontFilePaths.size()),
                    () -> assertTrue(StringUtils.contains(htmlContent, "yes")),
                    () -> assertTrue(StringUtils.contains(htmlContent, "110")),
                    () -> assertTrue(StringUtils.contains(htmlContent, "2.220,34")),
                    () -> assertTrue(StringUtils.contains(htmlContent, "220,009")),
                    () -> assertTrue(StringUtils.contains(htmlContent, "TEST")),
                    () -> assertTrue(StringUtils.contains(htmlContent, "TEST-list0")),
                    () -> assertTrue(StringUtils.contains(htmlContent, "\"\t\\\n")),
                    () -> assertTrue(StringUtils.contains(htmlContent, "42")),
                    () -> assertTrue(StringUtils.contains(htmlContent, "1")),
                    () -> assertTrue(StringUtils.contains(htmlContent, "2")),
                    () -> assertTrue(StringUtils.contains(htmlContent, "https://example.com/ftp/test.xml")),
                    () -> assertTrue(StringUtils.contains(htmlContent, ".")),
                    () -> assertTrue(StringUtils.contains(htmlContent, "2025-01-15")),
                    () -> assertTrue(StringUtils.contains(htmlContent, "2023-10-20")),
                    () -> assertTrue(StringUtils.contains(htmlContent, "2023-10-20T15:12")),
                    () -> assertTrue(StringUtils.contains(htmlContent, "2023-10-20T15:12:00.001234567")),
                    () -> assertTrue(StringUtils.contains(htmlContent, "test.com"))
            );

            Path iccFile = Paths.get(TEST_DIRECTORY_FOLDER_PATH.toString(), "AdobeRGB1998.icc");
            PdfACreationListener listener = new PdfACreationListener(
                    workerData.getParametriTemplate().get("title").toString(), listFontFilePaths, iccFile);

            File filePDFA1A = File.createTempFile("testPdfA1", ".pdf", new File(System.getProperty("java.io.tmpdir")));
            File fileZUGFeRD = File.createTempFile("testPdfAZUGFeRDStamper", ".pdf", new File(System.getProperty("java.io.tmpdir")));
            File filePDFA2A = File.createTempFile("testPdfA2Stamper", ".pdf", new File(System.getProperty("java.io.tmpdir")));

            try (ByteArrayOutputStream pdfOutputStream = getPdfA(templateOutput, listener);
                 OutputStream fileZUGFeRDOutputStream = Files.newOutputStream(fileZUGFeRD.toPath());
                 OutputStream filePDFA2OutputStream = Files.newOutputStream(filePDFA2A.toPath());
                 FileOutputStream file2OutputStream = new FileOutputStream(filePDFA1A)) {

                PdfStamper pdfStamper = new PdfStamper(new PdfReader(pdfOutputStream.toByteArray()), fileZUGFeRDOutputStream);
                XMPMeta xmpMetaStamper = getDeserializedMetadata(pdfStamper.getReader().getMetadata());
                assertAll(
                        () -> assertDoesNotThrow(() -> file2OutputStream.write(pdfOutputStream.toByteArray())),
                        () -> assertNotNull(pdfOutputStream),
                        () -> assertNull(pdfStamper.getReader().getJavaScript()),
                        () -> assertEquals(0, pdfStamper.getReader().getCertificationLevel()),
                        () -> assertEquals(-1, pdfStamper.getReader().getCryptoMode()),
                        () -> assertEquals(0, pdfStamper.getReader().getPermissions()),
                        () -> assertTrue(xmpMetaStamper.getArrayItem(XMPConst.NS_DC, "title", 1).getOptions().getHasLanguage()),
                        () -> assertTrue(xmpMetaStamper.getArrayItem(XMPConst.NS_DC, "title", 1).getOptions().isExactly(80)),
                        () -> assertTrue(StringUtils.startsWith(xmpMetaStamper.getProperty(XMPConst.NS_PDF, "Producer").getValue(), "iText®")),
                        () -> assertTrue(StringUtils.contains(xmpMetaStamper.getProperty(XMPConst.NS_PDF, "Producer").getValue(), "AGPL-version")),
                        () -> assertEquals("A", xmpMetaStamper.getProperty(XMPConst.NS_PDFA_ID, "conformance").getValue()),
                        () -> assertEquals("1", xmpMetaStamper.getProperty(XMPConst.NS_PDFA_ID, "part").getValue()),
                        () -> assertEquals(documentTitle, xmpMetaStamper.getArrayItem(XMPConst.NS_DC, "title", 1).getValue()),
                        () -> assertEquals("Babel", xmpMetaStamper.getArrayItem(XMPConst.NS_DC, "creator", 1).getValue()),
                        () -> assertEquals("begin=\"\uFEFF\" id=\"W5M0MpCehiHzreSzNTczkc9d\"", xmpMetaStamper.getPacketHeader()),
                        () -> assertEquals(documentTitle, pdfStamper.getReader().getInfo().get("Title")),
                        () -> assertDoesNotThrow(() -> setMetadata(pdfStamper, documentTitle, PdfAConformanceLevel.ZUGFeRDBasic)),
                        () -> assertDoesNotThrow(pdfStamper::close)
                );

                PdfStamper pdfStamperFirstResult = new PdfStamper(new PdfReader(Files.newInputStream(fileZUGFeRD.toPath())), filePDFA2OutputStream);
                XMPMeta xmpMetaResult = getDeserializedMetadata(pdfStamperFirstResult.getReader().getMetadata());
                assertAll(
                        () -> assertTrue(xmpMetaResult.getArrayItem(XMPConst.NS_DC, "title", 1).getOptions().getHasLanguage()),
                        () -> assertTrue(xmpMetaResult.getArrayItem(XMPConst.NS_DC, "title", 1).getOptions().isExactly(80)),
                        () -> assertTrue(StringUtils.startsWith(xmpMetaResult.getProperty(XMPConst.NS_PDF, "Producer").getValue(), "iText®")),
                        () -> assertTrue(StringUtils.contains(xmpMetaResult.getProperty(XMPConst.NS_PDF, "Producer").getValue(), "AGPL-version")),
                        () -> assertEquals("A", xmpMetaResult.getProperty(XMPConst.NS_PDFA_ID, "conformance").getValue()),
                        () -> assertEquals("3", xmpMetaResult.getProperty(XMPConst.NS_PDFA_ID, "part").getValue()),
                        () -> assertEquals(documentTitle, xmpMetaResult.getArrayItem(XMPConst.NS_DC, "title", 1).getValue()),
                        () -> assertEquals("BABEL", xmpMetaResult.getArrayItem(XMPConst.NS_DC, "creator", 1).getValue()),
                        () -> assertEquals("begin=\"\uFEFF\" id=\"W5M0MpCehiHzreSzNTczkc9d\"", xmpMetaResult.getPacketHeader()),
                        () -> assertDoesNotThrow(() -> setMetadata(pdfStamperFirstResult, documentTitle, PdfAConformanceLevel.PDF_A_2A)),
                        () -> assertDoesNotThrow(pdfStamperFirstResult::close)
                );

                PdfReader pdfResultFinalReader = new PdfReader(Files.newInputStream(filePDFA2A.toPath()));
                XMPMeta xmpMetaFinal = getDeserializedMetadata(pdfResultFinalReader.getMetadata());
                assertAll(
                        () -> assertEquals("A", xmpMetaFinal.getProperty(XMPConst.NS_PDFA_ID, "conformance").getValue()),
                        () -> assertEquals("2", xmpMetaFinal.getProperty(XMPConst.NS_PDFA_ID, "part").getValue()),
                        () -> assertDoesNotThrow(pdfResultFinalReader::close)
                );
            } finally {
                Files.deleteIfExists(fileZUGFeRD.toPath());
                Files.deleteIfExists(filePDFA2A.toPath());
                Files.deleteIfExists(filePDFA1A.toPath());
            }
        }
    }

    @Test
    void setICCProfileTest() {
        assertTrue(Paths.get(TEST_DIRECTORY_FOLDER_PATH.toString(), "/AdobeRGB1998.icc").toFile().exists());
    }
}