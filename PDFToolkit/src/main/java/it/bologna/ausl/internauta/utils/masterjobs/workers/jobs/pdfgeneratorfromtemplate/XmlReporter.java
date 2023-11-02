package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate;

import freemarker.template.Configuration;
import freemarker.template.TemplateModelException;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams;
import it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitDownloaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static it.bologna.ausl.internauta.utils.pdftoolkit.DataValidator.validateInput;
import static it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.FreeMarkerUtils.getDefaultConfiguration;
import static it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.FreeMarkerUtils.getTemplateOutput;

/**
 * @author ferri
 */
@MasterjobsWorker
public class XmlReporter extends JobWorker<ReporterJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(PdfAReporter.class);
    private final String name = PdfAReporter.class.getSimpleName();
    @Autowired
    private PdfToolkitDownloaderUtils pdfToolkitDownloaderUtils;
    @Autowired
    private PdfToolkitConfigParams pdfToolkitConfigParams;
    @Value("${pdf-toolkit.downloader.token-expire-seconds:60}")
    private Integer tokenExpirationInSeconds;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        pdfToolkitConfigParams.downloadFilesFromMinIO();

        ReporterJobWorkerData workerData = getWorkerData();
        ReporterJobWorkerResult reporterWorkerResult = new ReporterJobWorkerResult();
        validateInput(workerData, ".ftlx");

        try {
            Configuration configuration = getDefaultConfiguration();
            try (ByteArrayOutputStream outputStream = getTemplateOutput(configuration.getTemplate(workerData.getFileName()),
                    workerData.getParametriTemplate())) {

                reporterWorkerResult.setUrl(outputStream.toString());

                return reporterWorkerResult;
            }
        } catch (TemplateModelException e) {
            throw new MasterjobsWorkerException("Failed to retrieve template: " + workerData.getTemplateName() + " for PDF generation", e);
        } catch (IOException e) {
            throw new MasterjobsWorkerException("Failed to generate XML with template: " + workerData.getTemplateName(), e);
        }
    }
}
