package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate;

import com.itextpdf.text.pdf.ICC_Profile;
import com.itextpdf.text.pdf.PdfAConformanceLevel;
import com.itextpdf.text.pdf.PdfAWriter;
import com.itextpdf.text.pdf.PdfBoolean;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfWriter;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate.result.ReporterJobWorkerResult;
import org.apache.commons.io.IOUtils;
import freemarker.template.Template;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Version;
import freemarker.template.TemplateExceptionHandler;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.pdftoolkit.exceptions.PdfToolkitHttpException;
import it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams;
import it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitDownloaderUtils;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.PDFCreationListener;

/**
 *
 * @author Top
 */
@MasterjobsWorker
public class ReporterJobWorker extends JobWorker<ReporterJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(ReporterJobWorker.class);
    
    @Autowired
    private PdfToolkitDownloaderUtils pdfToolkitDownloaderUtils;
    
    @Autowired
    private PdfToolkitConfigParams pdfToolkitConfigParams;
    
    // durata del token
    @Value("${pdf-toolkit.downloader.token-expire-seconds:60}")
    private Integer tokenExpireSeconds;
    
    private final String name = ReporterJobWorker.class.getSimpleName();

    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {

        pdfToolkitConfigParams.downloadFilesFromMinIO();
        
        ReporterJobWorkerData workerData = getWorkerData();
        File adobeProfileFile = new File(PdfToolkitConfigParams.WORKDIR, PdfToolkitConfigParams.RESOURCES_RELATIVE_PATH + "/AdobeRGB1998.icc");
        
        Template temp = null;
        Map<String, Object> parametri = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writer out = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
            ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
            InputStream iccProfileStream = new FileInputStream(adobeProfileFile)) {
            String templateName = (String) workerData.getTemplateName();
            
            if (templateName == null || templateName.equals("")) {
                throw new MasterjobsWorkerException("TemplateName mancante nei worker data.");
            }
            
            parametri = workerData.getParametriTemplate();
            if (parametri == null) {
                throw new MasterjobsWorkerException("Parametri mancanti nel worker data.");
            }
            /* TODO: Qui c'era la parte del QR Code, rimossa per tenere pulito il codice */
            
            final File resourcePath = 
                    new File(PdfToolkitConfigParams.WORKDIR, PdfToolkitConfigParams.RESOURCES_RELATIVE_PATH + PdfToolkitConfigParams.INTERNAUTA_RELATIVE_PATH);
            
            final Version version = new Version(2, 3, 20);
            final Configuration cfg = new Configuration(version);
            log.info("resourcePath: " + resourcePath);
            cfg.setDirectoryForTemplateLoading(resourcePath);
            cfg.setObjectWrapper(new DefaultObjectWrapper(version));
            cfg.setDefaultEncoding("UTF-8");
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
            cfg.setIncompatibleImprovements(version);

            parametri.put("resourcePath", resourcePath.getAbsolutePath().replace("\\", "/"));
            
            Path templateFilePath = Paths.get(resourcePath.getAbsolutePath(), PdfToolkitConfigParams.TEMPLATES_RELATIVE_PATH, templateName);
            String templateString = IOUtils.toString(templateFilePath.toUri(), StandardCharsets.UTF_8.name());
            
            StringTemplateLoader stl = new StringTemplateLoader();
            stl.putTemplate("template", templateString);
            cfg.setTemplateLoader(stl);

            temp = cfg.getTemplate("template");
            temp.process(parametri, out);

            ITextRenderer renderer = new ITextRenderer();
            renderer.setListener(new PDFCreationListener() {
                
                @Override
                public void preOpen(ITextRenderer itr) {
                    PdfAWriter writer = (PdfAWriter) itr.getWriter();
                    writer.setPdfVersion(PdfWriter.PDF_VERSION_1_4);
                }

                @Override
                public void onClose(ITextRenderer itr) {
                    PdfAWriter writer = (PdfAWriter) itr.getWriter();
                    try {

                        PdfDictionary structureTreeRoot = new PdfDictionary();
                        structureTreeRoot.put(PdfName.TYPE, PdfName.STRUCTTREEROOT);
                        writer.getExtraCatalog().put(PdfName.STRUCTTREEROOT, structureTreeRoot);

                        PdfDictionary markInfo = new PdfDictionary(PdfName.MARKINFO);
                        markInfo.put(PdfName.MARKED, new PdfBoolean(true));
                        writer.getExtraCatalog().put(PdfName.MARKINFO, markInfo);

                        PdfDictionary l = new PdfDictionary(PdfName.LANG);
                        l.put(PdfName.LANG, new PdfBoolean("true"));
                        writer.getExtraCatalog().put(PdfName.LANG, l);

                        ICC_Profile icc = ICC_Profile.getInstance(iccProfileStream);
                        writer.setOutputIntents("Custom", "", "http://www.color.org", "sRGB IEC61966-2.1", icc);

                        writer.createXmpMetadata();
                    } catch (Exception ex) {
//                        log.error("", ex);
                    }
                }

                @Override
                public void preWrite(ITextRenderer itr, int i) {
                    
                }

            });

            renderer.setDocumentFromString(baos.toString("UTF-8"));
            renderer.layout();
          
            renderer.createPDF(pdfOut, true, true, PdfAConformanceLevel.PDF_A_1A);
            String tempFileName = workerData.getFileName() != null ?  workerData.getFileName() : String.format("reporter_%s.pdf", UUID.randomUUID().toString());
            ReporterJobWorkerResult reporterWorkerResult = new ReporterJobWorkerResult();
            try (ByteArrayInputStream bis = new ByteArrayInputStream(pdfOut.toByteArray())) {
                final String downloaderUrl = pdfToolkitConfigParams.getDownloaderUrl();
                final String uploaderUrl = pdfToolkitConfigParams.getUploaderUrl();
                // Carica e ottiene l'url per il download, con token valido per un minuto
                String urlToDownload;
                try {
                    urlToDownload = pdfToolkitDownloaderUtils
                            .uploadToUploader(bis, tempFileName, "application/pdf", false, downloaderUrl, uploaderUrl, tokenExpireSeconds)
                            .get("url").toString();
                    reporterWorkerResult.setUrl(urlToDownload);
                } catch (PdfToolkitHttpException ex) {
                    log.error("Errore nell'upload e generazione dell'url per il download", ex);
                }
            }
            return reporterWorkerResult;

                
        } catch (Exception ex) {
            File forTest = new File("output_wrong.html");
            try (Writer fileWriter = new FileWriter(forTest)) {
                if (temp != null && workerData != null) {
                    temp.process(workerData, fileWriter);
                }
            } catch (Exception subEx) {
                log.error("Errore nella scrittura del file di errore.", subEx); // LMAO
            }
            
            if (ex instanceof MasterjobsWorkerException) {
                throw (MasterjobsWorkerException) ex;
            } else {
                throw new MasterjobsWorkerException( ex);
            }
        }
    }

    @Override
    public String getName() {
        return this.name;
    }
    
    /** Converte un InputStream in una stringa
     * 
     * @param is l'InputStream da convertire
     * @return L'inputStream convertito in stringa
     * @throws UnsupportedEncodingException
     * @throws IOException 
     */
    public static String inputStreamToString(InputStream is) throws UnsupportedEncodingException, IOException {
         Writer stringWriter = new StringWriter();
         char[] buffer = new char[1024];
         try {
             Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
             int n;
             while ((n = reader.read(buffer)) != -1) {
                 stringWriter.write(buffer, 0, n);
             }
         }
         finally {
         }
         return stringWriter.toString();
    }
}
