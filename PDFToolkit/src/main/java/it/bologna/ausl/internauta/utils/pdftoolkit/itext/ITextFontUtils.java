package it.bologna.ausl.internauta.utils.pdftoolkit.itext;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.BaseFont;
import it.bologna.ausl.internauta.utils.pdftoolkit.enums.FontFamily;
import org.slf4j.Logger;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.FileUtils.getFilePathsWithExtension;

/**
 * @author ferri
 */
public class ITextFontUtils {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ITextFontUtils.class);

    public static void embedFonts(ITextRenderer iTextRenderer, List<Path> fontsFolderPath) {
        for (Path path : fontsFolderPath) {
            getFilePathsWithExtension(path, ".ttf").forEach(fontPath -> {
                try {
                    iTextRenderer.getFontResolver().addFont(fontPath.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                } catch (DocumentException | IOException e) {
                    log.error("Failed to add font files to embed in PDF in path: " + fontPath);
                }
            });
        }
    }

    public static List<Path> getFontFilePaths(List<String> fontsNameToInclude, Path directoryFolderPath) {
        List<Path> listPaths = new ArrayList<>();
        if (!fontsNameToInclude.isEmpty()) {
            for (String font : fontsNameToInclude) {
                listPaths.add(Paths.get(directoryFolderPath.toString(),
                        FontFamily.getFolderRelativePath(font).toString()));
            }
        }
        return listPaths;
    }
}
