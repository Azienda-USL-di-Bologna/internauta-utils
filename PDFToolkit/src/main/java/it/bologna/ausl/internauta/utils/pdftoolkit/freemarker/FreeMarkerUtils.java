package it.bologna.ausl.internauta.utils.pdftoolkit.freemarker;

import freemarker.template.*;
import it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.functions.*;
import it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author ferri
 */
public class FreeMarkerUtils {
    public static Configuration getDefaultConfiguration()
            throws TemplateModelException, IOException {

        return getConfiguration(Configuration.VERSION_2_3_20, getCustomFunctions(),
                Paths.get(PdfToolkitConfigParams.DIRECTORY_FOLDER_PATH.toString(), PdfToolkitConfigParams.TEMPLATES_RELATIVE_PATH));
    }

    public static Configuration getConfiguration(Version version, Map<String, Object> customFunctions, Path templateDirectory)
            throws TemplateModelException, IOException {

        final Configuration configuration = new Configuration(version);

        configuration.setDateFormat("yyyy-MM-dd");
        configuration.setTimeFormat("HH:mm:ss");
        configuration.setDateTimeFormat("yyyy-MM-dd HH:mm:ss");
        configuration.setBooleanFormat("yes,no");

        configuration.setDirectoryForTemplateLoading(templateDirectory.toFile());
        configuration.setRecognizeStandardFileExtensions(true);

        configuration.setNamingConvention(Configuration.CAMEL_CASE_NAMING_CONVENTION); // Java convection
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setIncompatibleImprovements(version);

        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configuration.setEncoding(Locale.ITALY, StandardCharsets.UTF_8.name());
        configuration.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
        configuration.setSQLDateAndTimeTimeZone(TimeZone.getDefault()); // JVM default time zone because that's what most JDBC drivers will use

        if (customFunctions != null && !customFunctions.isEmpty()) {
            configuration.setAllSharedVariables(new SimpleHash(customFunctions, configuration.getObjectWrapper()));
        }

        return configuration;
    }

    public static Map<String, Object> getCustomFunctions() {
        Map<String, Object> customFunctions = new HashMap<>();
        customFunctions.put("convertZonedDateTimeToDate", new ConvertZonedDateTimeToDate());
        customFunctions.put("formatFloatToCurrency", new FormatFloatToCurrency());
        customFunctions.put("formatDateWIthSlashes", new FormatDateWIthSlashes());
        customFunctions.put("formatDateYMDToDMY", new FormatDateYMDToDMY());
        customFunctions.put("invertTextOrder", new InvertTextOrder());
        return customFunctions;
    }

    public static ByteArrayOutputStream getTemplateOutput(Template template, Map<String, Object> dataModel)
            throws TemplateModelException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            template.process(dataModel, writer);
            return outputStream;
        } catch (TemplateException | IOException e) {
            throw new TemplateModelException("Invalid template " + template.getSourceName() + " when process it", e);
        } catch (NullPointerException e) {
            throw new TemplateModelException("Error when processing template " + template.getSourceName(), e);
        }
    }
}
