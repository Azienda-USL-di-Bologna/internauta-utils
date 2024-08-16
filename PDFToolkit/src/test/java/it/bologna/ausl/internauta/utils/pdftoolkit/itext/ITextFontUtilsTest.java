package it.bologna.ausl.internauta.utils.pdftoolkit.itext;

import it.bologna.ausl.internauta.utils.pdftoolkit.enums.FontFamily;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextFontUtils.embedFonts;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.FileUtils.getFilePathsWithExtension;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams.RESOURCES_RELATIVE_PATH;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParamsTest.TEST_DIRECTORY_FOLDER_PATH;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParamsTest.TEST_WORKDIR;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author ferri
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ITextFontUtilsTest {

    private ITextRenderer iTextRenderer;

    @BeforeEach
    void setUp() {
        iTextRenderer = new ITextRenderer();
    }

    @Test
    void embedFontsTest() {
        assertFalse(getFilePathsWithExtension(Paths.get(TEST_DIRECTORY_FOLDER_PATH.toString(), FontFamily.TIMES_ROMAN.getPath().toString()), ".ttf").isEmpty());
        assertFalse(getFilePathsWithExtension(Paths.get(TEST_DIRECTORY_FOLDER_PATH.toString(), FontFamily.ARIAL.getPath().toString()), ".ttf").isEmpty());
        List<Path> fontPaths = new ArrayList<>();
        fontPaths.add(Paths.get(TEST_DIRECTORY_FOLDER_PATH.toString(), FontFamily.TIMES_ROMAN.getPath().toString()));
        fontPaths.add(Paths.get(TEST_DIRECTORY_FOLDER_PATH.toString(), FontFamily.ARIAL.getPath().toString()));
        embedFonts(iTextRenderer, fontPaths);
        assertNotNull(iTextRenderer.getFontResolver().getFontFamily(FontFamily.TIMES_ROMAN.getName()));
        assertNotNull(iTextRenderer.getFontResolver().getFontFamily(FontFamily.ARIAL.getName()));
    }

    @Test
    void getFilePathsWithExtensionTest() {
        assertFalse(getFilePathsWithExtension(Paths.get(TEST_WORKDIR, RESOURCES_RELATIVE_PATH), ".icc").isEmpty());
    }
}