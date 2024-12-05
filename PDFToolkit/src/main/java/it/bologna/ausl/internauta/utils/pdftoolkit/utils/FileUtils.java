package it.bologna.ausl.internauta.utils.pdftoolkit.utils;

import it.bologna.ausl.estrattore.ExtractorCreator;
import it.bologna.ausl.estrattore.ExtractorResult;
import it.bologna.ausl.estrattore.exception.ExtractorException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
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
    
    
    public static File getCartellaTemporanea(String nomeTemp) {
        return new File(System.getProperty("java.io.tmpdir")
                + nomeTemp
                + System.getProperty("file.separator"));
    }

    public static ArrayList<ExtractorResult> estraiTuttoDalFile(File folderToSave,
            File tmp,
            String nomeFile) throws ExtractorException, IOException {

        ArrayList<ExtractorResult> extractAllResult = null;
        ExtractorCreator ec = new ExtractorCreator(tmp);

        if (ec.isExtractable()) {
            extractAllResult = ec.extractAll(folderToSave);
        }

        return extractAllResult;
    }
}
