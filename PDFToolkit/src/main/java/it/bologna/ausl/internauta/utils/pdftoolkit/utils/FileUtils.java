package it.bologna.ausl.internauta.utils.pdftoolkit.utils;

import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FileUtils.class);

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
