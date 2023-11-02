package it.bologna.ausl.internauta.utils.pdftoolkit;

import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate.ReporterJobWorkerData;

/**
 * @author ferri
 */
public class DataValidator {
    public static void validateInput(ReporterJobWorkerData workerData, String... expectedTemplateExtension)
            throws MasterjobsWorkerException {
        if (workerData == null || workerData.getParametriTemplate().isEmpty()) {
            throw new MasterjobsWorkerException("No ReporterJobWorkerData");
        }

        boolean match = false;

        for (String extension : expectedTemplateExtension) {
            if (extension.charAt(0) != '.') {
                extension = '.' + extension;
            }

            if (workerData.getTemplateName().toLowerCase().endsWith(extension.toLowerCase())) {
                match = true;
                break;
            }
        }
        if (!(match)) {
            throw new MasterjobsWorkerException("Invalid template extension");
        }
    }
}