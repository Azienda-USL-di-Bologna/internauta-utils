package it.bologna.ausl.internauta.utils.pdftoolkit.freemarker;

import freemarker.core.UndefinedOutputFormat;
import freemarker.template.AttemptExceptionReporter;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModelException;
import it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams;
import it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParamsTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import static freemarker.template.Configuration.VERSION_2_3_20;
import static it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.FreeMarkerUtils.getConfiguration;
import static it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.FreeMarkerUtils.getCustomFunctions;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ferri
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FreeMarkerUtilsTest {
    @Test
    void getDefaultConfigurationTest() throws TemplateModelException, IOException {
        Configuration configuration = getConfiguration(Configuration.VERSION_2_3_20, getCustomFunctions(),
                Paths.get(PdfToolkitConfigParamsTest.TEST_DIRECTORY_FOLDER_PATH.toString(), PdfToolkitConfigParams.TEMPLATES_RELATIVE_PATH));
        assertAll(
                () -> assertTrue(configuration.isDateFormatSet()),
                () -> assertTrue(configuration.isTimeFormatSet()),
                () -> assertTrue(configuration.isDateTimeFormatSet()),
                () -> assertTrue(configuration.isBooleanFormatSet()),
                () -> assertTrue(configuration.isRecognizeStandardFileExtensionsExplicitlySet()),
                () -> assertFalse(configuration.isTemplateNameFormatExplicitlySet()),
                () -> assertTrue(configuration.isTemplateExceptionHandlerExplicitlySet()),
                () -> assertNull(configuration.getIncompatibleImprovements().isGAECompliant()),
                () -> assertTrue(configuration.isDefaultEncodingExplicitlySet()),
                () -> assertTrue(configuration.isTimeZoneExplicitlySet()),
                () -> assertTrue(configuration.isSQLDateAndTimeTimeZoneSet()),
                () -> assertTrue(configuration.isNumberFormatSet()),
                () -> assertTrue(configuration.isTemplateLoaderExplicitlySet()),
                () -> assertFalse(configuration.isTemplateLookupStrategyExplicitlySet()),
                () -> assertFalse(configuration.isOutputFormatExplicitlySet()),
                () -> assertFalse(configuration.isAttemptExceptionReporterExplicitlySet()),
                () -> assertFalse(configuration.isObjectWrapperExplicitlySet()),
                () -> assertEquals("yyyy-MM-dd", configuration.getDateFormat()),
                () -> assertEquals("HH:mm:ss", configuration.getTimeFormat()),
                () -> assertEquals("yyyy-MM-dd HH:mm:ss", configuration.getDateTimeFormat()),
                () -> assertEquals("yes,no", configuration.getBooleanFormat()),
                () -> assertTrue(configuration.getRecognizeStandardFileExtensions()),
                () -> assertEquals(Configuration.CAMEL_CASE_NAMING_CONVENTION, configuration.getNamingConvention()),
                () -> assertEquals(TemplateExceptionHandler.RETHROW_HANDLER, configuration.getTemplateExceptionHandler()),
                () -> assertEquals(VERSION_2_3_20, configuration.getIncompatibleImprovements()),
                () -> assertEquals(StandardCharsets.UTF_8.name(), configuration.getDefaultEncoding()),
                () -> assertEquals(StandardCharsets.UTF_8.name(), configuration.getEncoding(Locale.ITALY)),
                () -> assertEquals(TimeZone.getTimeZone(ZoneId.of("Europe/Rome")), configuration.getTimeZone()),
                () -> assertEquals(TimeZone.getDefault(), configuration.getSQLDateAndTimeTimeZone()),
                () -> assertEquals("number", configuration.getNumberFormat()),
                () -> assertEquals(UndefinedOutputFormat.INSTANCE, configuration.getOutputFormat()),
                () -> assertEquals(AttemptExceptionReporter.LOG_ERROR_REPORTER, configuration.getAttemptExceptionReporter()),
                () -> assertEquals(Configuration.getDefaultObjectWrapper(VERSION_2_3_20), configuration.getObjectWrapper())
        );
    }
}