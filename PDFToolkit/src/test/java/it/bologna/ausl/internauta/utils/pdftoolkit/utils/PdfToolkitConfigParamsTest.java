package it.bologna.ausl.internauta.utils.pdftoolkit.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams.INTERNAUTA_RELATIVE_PATH;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams.RESOURCES_RELATIVE_PATH;

/**
 * @author ferri
 */
public class PdfToolkitConfigParamsTest {
    public static final String TEST_WORKDIR = Paths.get("src/test/").toAbsolutePath().toString();
    public static final Path TEST_DIRECTORY_FOLDER_PATH = Paths.get(TEST_WORKDIR, RESOURCES_RELATIVE_PATH, INTERNAUTA_RELATIVE_PATH);
}