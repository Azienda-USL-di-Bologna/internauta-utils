package it.bologna.ausl.internauta.utils.pdftoolkit.itext;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfAConformanceLevel;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.xmp.XMPConst;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;
import it.bologna.ausl.internauta.utils.pdftoolkit.enums.FontFamily;
import it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams;
import it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParamsTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.FreeMarkerUtils.*;
import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextMetadataUtils.setMetadata;
import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextPdfUtils.getPdfA;
import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextPdfUtils.setICCProfile;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.HtmlUtils.getFontFamilies;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams.ICC_PROFILE_RELATIVE_PATH;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams.TEMPLATES_RELATIVE_PATH;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ferri
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ITextPdfUtilsTest {
    private final Version defaultVersion = Configuration.VERSION_2_3_20;
    private final PdfAConformanceLevel defaultConformanceLevel = PdfAConformanceLevel.PDF_A_1A;
    private ITextRenderer iTextRenderer;

    @BeforeEach
    void setUp() {
        iTextRenderer = new ITextRenderer();
    }

    @Test
    void getPdfAOutputStreamTest() throws TemplateModelException, DocumentException, IOException {
        String documentTitle = "Test";
        Map<String, Object> dataModel = TestData.getTestInitialized();
        dataModel.put("title", documentTitle);
        dataModel.put("resourcePath", PdfToolkitConfigParamsTest.TEST_DIRECTORY_FOLDER_PATH.toString()
                .replace("\\", "/"));

        File file = File.createTempFile("test", ".pdf", new File(System.getProperty("java.io.tmpdir")));
        Configuration configuration = getConfiguration(defaultVersion, getAvCpFunctions(),
                Paths.get(PdfToolkitConfigParamsTest.TEST_DIRECTORY_FOLDER_PATH.toString(), TEMPLATES_RELATIVE_PATH));

        Template template = configuration.getTemplate("data-and-functions-test.xhtml");

        Path iccFile = Paths.get(PdfToolkitConfigParamsTest.TEST_DIRECTORY_FOLDER_PATH.toString(), "AdobeRGB1998.icc");

        try (ByteArrayOutputStream templateOutput = getTemplateOutput(template, dataModel)) {

            String htmlContent = templateOutput.toString(StandardCharsets.UTF_8.name());
            List<String> list = getFontFamilies(htmlContent);

            List<Path> listPaths = new ArrayList<>();
            if (!list.isEmpty()) {
                for (String font : list) {
                    listPaths.add(Paths.get(PdfToolkitConfigParamsTest.TEST_DIRECTORY_FOLDER_PATH.toString(),
                            FontFamily.getFolderRelativePath(font).toString()));
                }
            }

            PdfACreationListener listener = new PdfACreationListener(dataModel.get("title").toString(), listPaths, iccFile);

            try (ByteArrayOutputStream pdfOutputStream = getPdfA(templateOutput, listener);
                 FileOutputStream writer = new FileOutputStream(file)) {
                PdfReader pdfReader = new PdfReader(pdfOutputStream.toByteArray());
                PdfStamper pdfStamper = new PdfStamper(pdfReader, pdfOutputStream);
                assertAll(
                        () -> assertNotNull(pdfOutputStream),
                        () -> assertNull(pdfReader.getJavaScript()),
                        () -> assertEquals(0, pdfReader.getCertificationLevel()),
                        () -> assertEquals(-1, pdfReader.getCryptoMode()),
                        () -> assertEquals(0, pdfReader.getPermissions()),
                        () -> assertEquals("Test", pdfReader.getInfo().get("Title")),
                        () -> assertEquals(36265, pdfReader.getFileLength()), // ofc, at minimum variation it fails
                        () -> assertDoesNotThrow(() -> setMetadata(pdfStamper, documentTitle, defaultConformanceLevel)),
                        () -> assertDoesNotThrow(() -> writer.write(pdfOutputStream.toByteArray())),
                        () -> assertDoesNotThrow(pdfReader::close)
                );
            } finally {
                Files.deleteIfExists(file.toPath());
            }
        }
    }

    @Test
    void setICCProfileTest() {
        assertTrue(Paths.get(PdfToolkitConfigParamsTest.TEST_DIRECTORY_FOLDER_PATH.toString(), "/AdobeRGB1998.icc").toFile().exists());
        assertThrows(IOException.class, () -> setICCProfile(iTextRenderer,
                Paths.get(PdfToolkitConfigParams.DIRECTORY_FOLDER_PATH.toString(),
                        ICC_PROFILE_RELATIVE_PATH, "/AdobeRGB1998.icc")));
    }
}