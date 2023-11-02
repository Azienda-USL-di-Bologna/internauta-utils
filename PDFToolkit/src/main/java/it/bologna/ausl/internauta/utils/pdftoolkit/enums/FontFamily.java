package it.bologna.ausl.internauta.utils.pdftoolkit.enums;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author ferri
 */
public enum FontFamily {
    ARIAL("Arial", Paths.get("/fonts", "/Arial")),
    TIMES_ROMAN("Times New Roman", Paths.get("/fonts", "/Times-Roman"));

    private final String name;
    private final Path path;

    FontFamily(String name, Path path) {
        this.name = name;
        this.path = path;
    }


    public static Path getFolderRelativePath(String name) {

        for (FontFamily value : FontFamily.values()) {
            if (value.getName().equalsIgnoreCase(name)) {
                return value.getPath();
            }
        }

        return TIMES_ROMAN.getPath();
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }
}
