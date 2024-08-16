package it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.functions;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.FreeMarkerUtils.getConfiguration;
import static it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.FreeMarkerUtils.getTemplateOutput;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams.TEMPLATES_RELATIVE_PATH;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParamsTest.TEST_DIRECTORY_FOLDER_PATH;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ferri
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FormatFloatToCurrencyTest {
    private final Version defaultVersion = Configuration.VERSION_2_3_20;

    @Test
    void execTest() throws TemplateModelException, IOException {
        String templateName = "format-float-to-currency.ftlh";
        assertTrue(Paths.get(TEST_DIRECTORY_FOLDER_PATH.toString(), TEMPLATES_RELATIVE_PATH, templateName).toFile().exists());

        Map<String, Object> dataModel = new HashMap<String, Object>() {{
            put("aFloat1", 3.000F);
            put("aFloat2", 3.005F);
            put("aFloat3", 3.009F);
            put("aFloat4", 3.999F);
        }};
        Map<String, Object> customFunction = new HashMap<String, Object>() {{
            put("formatFloatToCurrency", new FormatFloatToCurrency());
        }};

        Configuration configuration = getConfiguration(defaultVersion, customFunction,
                Paths.get(TEST_DIRECTORY_FOLDER_PATH.toString(), TEMPLATES_RELATIVE_PATH));

        Template template = configuration.getTemplate(templateName);

        File file = File.createTempFile("test", ".html", new File(System.getProperty("java.io.tmpdir")));

        try (ByteArrayOutputStream templateOutput = getTemplateOutput(template, dataModel);
             FileOutputStream writer = new FileOutputStream(file);
             BufferedReader reader = new BufferedReader(new FileReader(file))) {

            assertAll(
                    () -> assertDoesNotThrow(() -> writer.write(templateOutput.toByteArray())),
                    () -> assertTrue(reader.lines().anyMatch(line -> line.trim().equals("3,00"))),
                    () -> assertTrue(reader.lines().anyMatch(line -> line.trim().equals("3,99"))),
                    () -> assertFalse(reader.lines().anyMatch(line -> line.trim().equals("3,01"))),
                    () -> assertFalse(reader.lines().anyMatch(line -> line.trim().equals("4,00")))
            );
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }
}