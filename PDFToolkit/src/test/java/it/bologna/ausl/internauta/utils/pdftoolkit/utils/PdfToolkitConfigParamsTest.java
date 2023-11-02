package it.bologna.ausl.internauta.utils.pdftoolkit.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams.INTERNAUTA_RELATIVE_PATH;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams.RESOURCES_RELATIVE_PATH;

/**
 * @author ferri
 */
public class PdfToolkitConfigParamsTest {
    public static final String WORKDIR =
            Paths.get("src/test/").toAbsolutePath().toString(); // same as class loader but static as long this class isn't moved
    public static final Path TEST_DIRECTORY_FOLDER_PATH = Paths.get(WORKDIR, RESOURCES_RELATIVE_PATH, INTERNAUTA_RELATIVE_PATH);
}