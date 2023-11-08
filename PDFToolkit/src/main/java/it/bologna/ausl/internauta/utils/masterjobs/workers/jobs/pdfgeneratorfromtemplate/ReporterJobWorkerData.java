package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate;

import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * Classe che contiene i dati necessari al worker del reporter per generare i PDF.
 *
 * @author Giuseppe Russo <g.russo@dilaxia.com>
 */
public class ReporterJobWorkerData extends JobWorkerData {
    Map<String, Object> parametriTemplate;
    //
    private String codiceAzienda;
    private String templateName;
    private String fileName;

    public ReporterJobWorkerData() {
    }

    /**
     * Costruttore con tutti i parametri
     *
     * @param codiceAzienda     Il codice azienda per generare il pdf. Verrà utilizzato per identificare il giusto template.
     * @param templateName      Il nome del template eg. gd_frontespizio
     * @param fileName          Il nome con il quale verrà generato il pdf.
     * @param parametriTemplate I parametri che andranno a compilare il template.
     */
    public ReporterJobWorkerData(String codiceAzienda, String templateName, String fileName, Map<String, Object> parametriTemplate) {
        setCodiceAzienda(codiceAzienda);
        setTemplateName(templateName);
        setFileName(fileName);
        setParametriTemplate(parametriTemplate);
    }

    public static void nullCheck(ReporterJobWorkerData workerData) throws MasterjobsWorkerException {
        if (workerData.getParametriTemplate() == null ||
                workerData.getCodiceAzienda() == null ||
                workerData.getTemplateName() == null ||
                workerData.getFileName() == null ||
                workerData.getParametriTemplate().containsKey(null)) {
            throw new MasterjobsWorkerException("ReporterJobWorkerData has at least one null value");
        }
    }

    public static void dataModelTitleCheck(ReporterJobWorkerData workerData) throws MasterjobsWorkerException {
        if (workerData.getParametriTemplate().get("title") == null ||
                StringUtils.isBlank(workerData.getParametriTemplate().get("title").toString())) {
            throw new MasterjobsWorkerException("ReporterJobWorkerData.parametriTemplate doesn't have the key 'title' " +
                    "(PDF/A requirement as html metadata title)");
        }
    }

    public static void extensionCheck(ReporterJobWorkerData workerData, String... expectedTemplateExtensions)
            throws MasterjobsWorkerException {
        boolean match = false;

        for (String extension : expectedTemplateExtensions) {
            if (extension.charAt(0) != '.') {
                extension = '.' + extension;
            }

            if (StringUtils.endsWithIgnoreCase(workerData.getTemplateName(), extension)) {
                match = true;
                break;
            }
        }

        if (!(match)) {
            throw new MasterjobsWorkerException("Invalid template extension");
        }
    }

    public String getCodiceAzienda() {
        return codiceAzienda;
    }

    public void setCodiceAzienda(String codiceAzienda) {
        if (StringUtils.isNotBlank(codiceAzienda)) {
            this.codiceAzienda = codiceAzienda;
        }
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        if (StringUtils.isNotBlank(templateName) && StringUtils.contains(templateName, '.')) {
            this.templateName = templateName;
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        if (StringUtils.isNotBlank(fileName)) {
            this.fileName = fileName;
        }
    }

    public Map<String, Object> getParametriTemplate() {
        return parametriTemplate;
    }

    public void setParametriTemplate(Map<String, Object> parametriTemplate) {
        if (parametriTemplate != null && !parametriTemplate.isEmpty()) {
            this.parametriTemplate = parametriTemplate;
        }
    }

    public void validateInput(String... expectedTemplateExtensions) throws MasterjobsWorkerException {
        nullCheck(this);
        extensionCheck(this, expectedTemplateExtensions);
    }

    public void validateInputForPdfA(String... expectedTemplateExtensions) throws MasterjobsWorkerException {
        nullCheck(this);
        dataModelTitleCheck(this);
        extensionCheck(this, expectedTemplateExtensions);
    }
}
