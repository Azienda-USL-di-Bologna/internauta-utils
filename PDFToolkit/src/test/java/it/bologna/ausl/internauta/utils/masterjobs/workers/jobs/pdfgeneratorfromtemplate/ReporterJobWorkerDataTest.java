package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate;

import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate.ReporterJobWorkerData.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ferri
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReporterJobWorkerDataTest {
    private static final String COMPANY_CODE = "codiceAzienda";
    private static final String TEMPLATE_NAME = "templateName.xhtml";
    private static final String FILE_NAME = "fileName";
    private static final String[] EXTENSIONS = {".ftlh", "xhtml"};
    private static final String[] WRONG_EXTENSIONS = {".ftlx", ".ftl"};
    private static final Map<String, Object> DATA_MODEL_EMPTY = new HashMap<>();
    private static final Map<String, Object> DATA_MODEL_WITH_KEY_N_VALUE = new HashMap<String, Object>() {{
        put("something", "something");
    }};
    private static final Map<String, Object> DATA_MODEL_WITH_KEY = new HashMap<String, Object>() {{
        put("something", null);
    }};
    private static final Map<String, Object> DATA_MODEL_WITH_VALUE = new HashMap<String, Object>() {{
        put(null, "something");
    }};
    private static final Map<String, Object> DATA_MODEL_WITH_TITLE = new HashMap<String, Object>() {{
        put("title", "titleTest");
    }};
    private static final Map<String, Object> DATA_MODEL_NULL_TITLE = new HashMap<String, Object>() {{
        put("title", null);
    }};
    private static final Map<String, Object> DATA_MODEL_EMPTY_TITLE = new HashMap<String, Object>() {{
        put("title", StringUtils.EMPTY);
    }};
    private static final Map<String, Object> DATA_MODEL_BLANK_TITLE = new HashMap<String, Object>() {{
        put("title", StringUtils.SPACE);
    }};
    static ReporterJobWorkerData workerData1;
    static ReporterJobWorkerData workerData2;
    static ReporterJobWorkerData workerData3;
    static ReporterJobWorkerData workerData4;
    static ReporterJobWorkerData workerData5;
    static ReporterJobWorkerData workerData6;
    static ReporterJobWorkerData workerData7;
    static ReporterJobWorkerData workerData8;
    static ReporterJobWorkerData workerData9;
    static ReporterJobWorkerData workerData10;
    static ReporterJobWorkerData workerData11;
    static ReporterJobWorkerData workerData12;
    static ReporterJobWorkerData workerData13;
    static ReporterJobWorkerData workerData14;
    static ReporterJobWorkerData workerData15;
    static ReporterJobWorkerData workerData16;
    static ReporterJobWorkerData workerData17;

    @BeforeAll
    static void beforeAll() {
        workerData1 = new ReporterJobWorkerData(null, TEMPLATE_NAME, FILE_NAME, DATA_MODEL_WITH_KEY_N_VALUE);
        workerData2 = new ReporterJobWorkerData(COMPANY_CODE, null, FILE_NAME, DATA_MODEL_WITH_KEY_N_VALUE);
        workerData3 = new ReporterJobWorkerData(COMPANY_CODE, TEMPLATE_NAME, null, DATA_MODEL_WITH_KEY_N_VALUE);
        workerData4 = new ReporterJobWorkerData(COMPANY_CODE, TEMPLATE_NAME, FILE_NAME, null);

        workerData5 = new ReporterJobWorkerData(StringUtils.EMPTY, TEMPLATE_NAME, FILE_NAME, DATA_MODEL_WITH_KEY_N_VALUE);
        workerData6 = new ReporterJobWorkerData(COMPANY_CODE, StringUtils.EMPTY, FILE_NAME, DATA_MODEL_WITH_KEY_N_VALUE);
        workerData7 = new ReporterJobWorkerData(COMPANY_CODE, TEMPLATE_NAME, StringUtils.EMPTY, DATA_MODEL_WITH_KEY_N_VALUE);
        workerData8 = new ReporterJobWorkerData(COMPANY_CODE, TEMPLATE_NAME, FILE_NAME, DATA_MODEL_WITH_VALUE);

        workerData9 = new ReporterJobWorkerData(StringUtils.SPACE, TEMPLATE_NAME, FILE_NAME, DATA_MODEL_WITH_KEY_N_VALUE);
        workerData10 = new ReporterJobWorkerData(COMPANY_CODE, StringUtils.SPACE, StringUtils.SPACE, DATA_MODEL_WITH_KEY_N_VALUE);
        workerData11 = new ReporterJobWorkerData(COMPANY_CODE, TEMPLATE_NAME, StringUtils.SPACE, DATA_MODEL_WITH_KEY_N_VALUE);
        workerData12 = new ReporterJobWorkerData(COMPANY_CODE, TEMPLATE_NAME, FILE_NAME, DATA_MODEL_WITH_KEY);

        workerData13 = new ReporterJobWorkerData(COMPANY_CODE, TEMPLATE_NAME, FILE_NAME, DATA_MODEL_WITH_TITLE);
        workerData14 = new ReporterJobWorkerData(COMPANY_CODE, TEMPLATE_NAME, FILE_NAME, DATA_MODEL_NULL_TITLE);
        workerData15 = new ReporterJobWorkerData(COMPANY_CODE, TEMPLATE_NAME, FILE_NAME, DATA_MODEL_EMPTY_TITLE);
        workerData16 = new ReporterJobWorkerData(COMPANY_CODE, TEMPLATE_NAME, FILE_NAME, DATA_MODEL_BLANK_TITLE);
        workerData17 = new ReporterJobWorkerData(COMPANY_CODE, TEMPLATE_NAME, FILE_NAME, DATA_MODEL_EMPTY);
    }

    @Test
    void validateInputTest() {
        assertAll(
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData1.validateInput(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData2.validateInput(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData3.validateInput(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData4.validateInput(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData5.validateInput(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData6.validateInput(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData7.validateInput(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData8.validateInput(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData9.validateInput(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData10.validateInput(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData11.validateInput(EXTENSIONS)),
                () -> assertDoesNotThrow(() -> workerData12.validateInput(EXTENSIONS)),
                () -> assertDoesNotThrow(() -> workerData13.validateInput(EXTENSIONS)),
                () -> assertDoesNotThrow(() -> workerData14.validateInput(EXTENSIONS)),
                () -> assertDoesNotThrow(() -> workerData15.validateInput(EXTENSIONS)),
                () -> assertDoesNotThrow(() -> workerData16.validateInput(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData17.validateInput(EXTENSIONS))
        );
    }

    @Test
    void validateInputForPdfTest() {
        assertAll(
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData1.validateInputForPdfA(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData2.validateInputForPdfA(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData3.validateInputForPdfA(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData4.validateInputForPdfA(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData5.validateInputForPdfA(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData6.validateInputForPdfA(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData7.validateInputForPdfA(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData8.validateInputForPdfA(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData9.validateInputForPdfA(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData10.validateInputForPdfA(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData11.validateInputForPdfA(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData12.validateInputForPdfA(EXTENSIONS)),
                () -> assertDoesNotThrow(() -> workerData13.validateInputForPdfA(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData14.validateInputForPdfA(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData15.validateInputForPdfA(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData16.validateInputForPdfA(EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> workerData17.validateInputForPdfA(EXTENSIONS))
        );
    }

    @Test
    void nullCheckTest() {
        assertAll(
                () -> assertThrows(MasterjobsWorkerException.class, () -> nullCheck(workerData1)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> nullCheck(workerData2)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> nullCheck(workerData3)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> nullCheck(workerData4)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> nullCheck(workerData5)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> nullCheck(workerData6)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> nullCheck(workerData7)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> nullCheck(workerData8)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> nullCheck(workerData9)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> nullCheck(workerData10)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> nullCheck(workerData11)),
                () -> assertDoesNotThrow(() -> nullCheck(workerData12)),
                () -> assertDoesNotThrow(() -> nullCheck(workerData13)),
                () -> assertDoesNotThrow(() -> nullCheck(workerData14)),
                () -> assertDoesNotThrow(() -> nullCheck(workerData15)),
                () -> assertDoesNotThrow(() -> nullCheck(workerData16)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> nullCheck(workerData17))
        );
    }

    @Test
    void dataModelTitleCheckTest() {
        assertAll(
                () -> assertThrows(MasterjobsWorkerException.class, () -> dataModelTitleCheck(workerData1)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> dataModelTitleCheck(workerData12)),
                () -> assertDoesNotThrow(() -> dataModelTitleCheck(workerData13)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> dataModelTitleCheck(workerData14)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> dataModelTitleCheck(workerData15)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> dataModelTitleCheck(workerData16)),
                () -> assertThrows(NullPointerException.class, () -> dataModelTitleCheck(workerData17))
        );
    }

    @Test
    void extensionCheckTest() {
        assertAll(
                () -> assertDoesNotThrow(() -> extensionCheck(workerData1, EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData2, EXTENSIONS)),
                () -> assertDoesNotThrow(() -> extensionCheck(workerData3, EXTENSIONS)),
                () -> assertDoesNotThrow(() -> extensionCheck(workerData4, EXTENSIONS)),
                () -> assertDoesNotThrow(() -> extensionCheck(workerData5, EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData6, EXTENSIONS)),
                () -> assertDoesNotThrow(() -> extensionCheck(workerData7, EXTENSIONS)),
                () -> assertDoesNotThrow(() -> extensionCheck(workerData8, EXTENSIONS)),
                () -> assertDoesNotThrow(() -> extensionCheck(workerData9, EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData10, EXTENSIONS)),
                () -> assertDoesNotThrow(() -> extensionCheck(workerData11, EXTENSIONS)),
                () -> assertDoesNotThrow(() -> extensionCheck(workerData12, EXTENSIONS)),
                () -> assertDoesNotThrow(() -> extensionCheck(workerData13, EXTENSIONS)),
                () -> assertDoesNotThrow(() -> extensionCheck(workerData14, EXTENSIONS)),
                () -> assertDoesNotThrow(() -> extensionCheck(workerData15, EXTENSIONS)),
                () -> assertDoesNotThrow(() -> extensionCheck(workerData16, EXTENSIONS)),
                () -> assertDoesNotThrow(() -> extensionCheck(workerData17, EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData1, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData2, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData3, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData4, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData5, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData6, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData7, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData8, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData9, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData10, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData11, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData12, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData13, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData14, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData15, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData16, WRONG_EXTENSIONS)),
                () -> assertThrows(MasterjobsWorkerException.class, () -> extensionCheck(workerData17, WRONG_EXTENSIONS))
        );
    }
}