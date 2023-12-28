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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author ferri
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvertTextOrderTest {
    private final Version defaultVersion = Configuration.VERSION_2_3_20;

    @Test
    void execTest() throws TemplateModelException, IOException {
        String templateName = "invert-text-order.ftlh";
        assertTrue(Paths.get(TEST_DIRECTORY_FOLDER_PATH.toString(), TEMPLATES_RELATIVE_PATH, templateName).toFile().exists());

        Map<String, Object> dataModel = new HashMap<String, Object>() {{
            put("aString", "@test-1234");
        }};
        Map<String, Object> customFunction = new HashMap<String, Object>() {{
            put("invertTextOrder", new InvertTextOrder());
        }};

        Configuration configuration = getConfiguration(defaultVersion, customFunction,
                Paths.get(TEST_DIRECTORY_FOLDER_PATH.toString(), TEMPLATES_RELATIVE_PATH));

        Template template = configuration.getTemplate(templateName);

        File file = File.createTempFile("test", ".html", new File(System.getProperty("java.io.tmpdir")));

        try (ByteArrayOutputStream templateOutput = getTemplateOutput(template, dataModel);
             FileOutputStream writer = new FileOutputStream(file);
             BufferedReader reader = new BufferedReader(new FileReader(file))) {

            assertDoesNotThrow(() -> writer.write(templateOutput.toByteArray()));
            assertTrue(reader.lines().anyMatch(line -> line.trim().equals("4321-tset@")));
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }
}