package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateModelException;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.pdftoolkit.exceptions.PdfToolkitHttpException;
import it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams;
import it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitDownloaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.FreeMarkerUtils.getDefaultConfiguration;
import static it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.FreeMarkerUtils.getTemplateOutput;
import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextPdfUtils.formatPathForTemplate;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams.DIRECTORY_FOLDER_PATH;

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
        workerData.getParametriTemplate().put("resourcePath", formatPathForTemplate(DIRECTORY_FOLDER_PATH));
        ReporterJobWorkerResult reporterWorkerResult = new ReporterJobWorkerResult();

        workerData.validateInput(".ftlx");

        try {
            Configuration configuration = getDefaultConfiguration();

            Template template = configuration.getTemplate(workerData.getTemplateName());

            try (ByteArrayOutputStream xmlStream = getTemplateOutput(template, workerData.getParametriTemplate())) {

                pdfToolkitDownloaderUtils.uploadToUploader(
                        new ByteArrayInputStream(xmlStream.toByteArray()),
                        workerData.getFileName(),
                        MediaType.TEXT_XML_VALUE,
                        false,
                        pdfToolkitConfigParams.getDownloaderUrl(),
                        pdfToolkitConfigParams.getUploaderUrl(),
                        tokenExpirationInSeconds);

                return reporterWorkerResult;
            }

        } catch (TemplateModelException | IOException eTemplate) {
            throw new MasterjobsWorkerException("Failed Freemarker process with template: " + workerData.getTemplateName(), eTemplate);
        } catch (PdfToolkitHttpException eHTTP) {
            throw new MasterjobsWorkerException("Failed to send xml using template: " + workerData.getTemplateName(), eHTTP);
        } catch (Throwable throwable) {
            throw new MasterjobsWorkerException(
                    String.format("Error, unmanaged throwable in %s using company code: %s, file name: %s, template: %s, data model: %s",
                            this.name, workerData.getCodiceAzienda(), workerData.getFileName(), workerData.getTemplateName(),
                            workerData.getParametriTemplate()), throwable);
        }
    }
}
