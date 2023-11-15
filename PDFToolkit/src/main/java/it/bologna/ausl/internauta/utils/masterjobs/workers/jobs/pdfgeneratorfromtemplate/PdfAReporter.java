package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate;

import com.itextpdf.text.DocumentException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateModelException;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.pdftoolkit.exceptions.PdfToolkitHttpException;
import it.bologna.ausl.internauta.utils.pdftoolkit.itext.PdfACreationListener;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.FreeMarkerUtils.getDefaultConfiguration;
import static it.bologna.ausl.internauta.utils.pdftoolkit.freemarker.FreeMarkerUtils.getTemplateOutput;
import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextFontUtils.getFontFilePaths;
import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextPdfUtils.formatPathForTemplate;
import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextPdfUtils.getPdfA;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.HtmlUtils.getFontFamilies;
import static it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams.DIRECTORY_FOLDER_PATH;

/**
 * @author ferri
 */
@MasterjobsWorker
public class PdfAReporter extends JobWorker<ReporterJobWorkerData, JobWorkerResult> {
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

        workerData.validateInputForPdfA(".ftlh", ".xhtml");

        try {
            Configuration configuration = getDefaultConfiguration();
            Template template = configuration.getTemplate(workerData.getTemplateName());

            try (ByteArrayOutputStream templateOutput = getTemplateOutput(template, workerData.getParametriTemplate())) {

                String htmlContent = templateOutput.toString(StandardCharsets.UTF_8.name());
                List<String> listFont = getFontFamilies(htmlContent);
                List<Path> listFontFilePaths = getFontFilePaths(listFont, DIRECTORY_FOLDER_PATH);

                PdfACreationListener listener =
                        new PdfACreationListener(workerData.getParametriTemplate().get("title").toString(), listFontFilePaths);

                try (ByteArrayOutputStream pdfStream = getPdfA(templateOutput, listener)) {
                    reporterWorkerResult.setUrl(
                            pdfToolkitDownloaderUtils.uploadToUploader(
                                    new ByteArrayInputStream(pdfStream.toByteArray()),
                                    workerData.getFileName(),
                                    MediaType.APPLICATION_PDF_VALUE,
                                    false,
                                    pdfToolkitConfigParams.getDownloaderUrl(),
                                    pdfToolkitConfigParams.getUploaderUrl(),
                                    tokenExpirationInSeconds));

                    return reporterWorkerResult;
                }
            }
        } catch (TemplateModelException e) {
            throw new MasterjobsWorkerException("Failed Freemarker process with template: " + workerData.getTemplateName(), e);
        } catch (IOException | DocumentException e) {
            throw new MasterjobsWorkerException("Failed template directory or PDF generation with template: " + workerData.getTemplateName(), e);
        } catch (PdfToolkitHttpException e) {
            throw new MasterjobsWorkerException("Failed to upload PDF using upload url: " + pdfToolkitConfigParams.getUploaderUrl(), e);
        } catch (Throwable throwable) {
            throw new MasterjobsWorkerException(
                    String.format("Error, unmanaged throwable in %s using company code: %s, file name: %s, template: %s, data model: %s",
                            this.name, workerData.getCodiceAzienda(), workerData.getFileName(), workerData.getTemplateName(),
                            workerData.getParametriTemplate()), throwable);
        }
    }
}
