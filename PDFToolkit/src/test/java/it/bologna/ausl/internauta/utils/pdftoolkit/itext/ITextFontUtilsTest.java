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
import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextFontUtils.getFilePathsWithExtension;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams.ICC_PROFILE_RELATIVE_PATH;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParamsTest.TEST_DIRECTORY_FOLDER_PATH;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        List<Path> fontPaths = new ArrayList<>();
        fontPaths.add(Paths.get(TEST_DIRECTORY_FOLDER_PATH.toString(), FontFamily.TIMES_ROMAN.getPath().toString()));
        fontPaths.add(Paths.get(TEST_DIRECTORY_FOLDER_PATH.toString(), FontFamily.ARIAL.getPath().toString()));
        embedFonts(iTextRenderer, fontPaths);
        iTextRenderer.getFontResolver().getFontFamily(FontFamily.TIMES_ROMAN.getName());
    }

    @Test
    void getFilePathsWithExtensionTest() {
        assertTrue(getFilePathsWithExtension(
                Paths.get(TEST_DIRECTORY_FOLDER_PATH.toString(), ICC_PROFILE_RELATIVE_PATH), ".icc").isEmpty());
    }
}