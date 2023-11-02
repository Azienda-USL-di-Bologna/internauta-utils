package it.bologna.ausl.internauta.utils.pdftoolkit.itext;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.BaseFont;
import org.slf4j.Logger;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    public static List<Path> getFilePathsWithExtension(Path directoryToSearch, String extension) {
        if (extension.charAt(0) != '.') {
            extension = '.' + extension;
        }
        List<Path> pathList = new ArrayList<>();

        try {
            File directory = new File(directoryToSearch.toUri());
            if (directory.exists()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().toLowerCase().endsWith(extension)) {
                            pathList.add(file.toPath());
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            log.error("Error when detecting files with extension: {} inside the path: {}", extension, directoryToSearch);
        }

        return pathList;
    }
}
