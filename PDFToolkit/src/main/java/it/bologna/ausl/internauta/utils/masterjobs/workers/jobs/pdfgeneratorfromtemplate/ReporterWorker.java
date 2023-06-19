package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate;

import java.awt.color.ICC_Profile;
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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import freemarker.template.Template;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Version;
import freemarker.template.TemplateExceptionHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;



/**
 *
 * @author Top
 */
public class ReporterWorker extends JobWorker<ReporterWorkerData, JobWorkerResult> {

//    private final static Logger log = LoggerFactory.getLogger(ReporterWorker.class);
    private final String name = ReporterWorker.class.getSimpleName();
    
    private InputStream iccProfileStream;
    private static final String TEMPLATES_PATH = "templates";

    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        ReporterWorkerData data = getWorkerData();
        ByteArrayOutputStream pdfOut = null;
        ByteArrayOutputStream baos = null;
        FileOutputStream qrCodeOutputStream = null;
//        File qrcodeTmpFile = null;

        iccProfileStream = getClass().getResourceAsStream("/it/bologna/ausl/mavenchef/AdobeRGB1998.icc");
        if (iccProfileStream == null) {
//            log.info("iccprofile è null cazoooo");

        }
        Template temp = null;
        Map<String, Object> parametri = null;
        try {
            String templateName = (String) data.getNomeTemplate();

            // TODO: lasciato per retrocompatibilità, è da togliere quando finiamo il refactoring dei template, il parametro giusto è templateName
            //MIDO: ora è il momento di togliere
//            String templatePath = (String) in.get("templatePath");

//            log.info("templateName: " + templateName);
//            log.info("TemplatePath: " + templatePath);

            if ((templateName == null || templateName.equals("")) && (templatePath == null || templatePath.equals(""))) {
                throw new MasterjobsWorkerException( "templateName o templatePath mancante");
            }

            parametri = data.getParametriTemplate();
            if (parametri == null) {
                throw new MasterjobsWorkerException( "data mancante");
            }

            // generazione e inserimento del QrCode
//            String titleQrcode = "Azienda USL di Bologna Codice AOO:ASL_BO:protocollo generale- numero data ora";
//            JSONObject qrCodeData = null;
            // il QrCode non è obbligatorio; se non viene passato ottengo un NullPointerException per cui lo ignoro
//            try {
//                qrCodeData = (JSONObject) in.get("qrCodeData");
//            }
//            catch (NullPointerException ex) {
//            }
//            if (qrCodeData != null) {
//                String qrCodeFieldName = (String) qrCodeData.get("fieldName");
//                if (qrCodeFieldName == null || qrCodeFieldName.equals("")){
//                    throw new WorkerException(getJobType(), "campo fieldName del QrCode mancante");
//                }
//                String qrCodeValue = (String) qrCodeData.get("value");
//                if (qrCodeValue == null || qrCodeValue.equals("")){
//                    throw new WorkerException(getJobType(), "campo value del QrCode mancante");
//                }
////                BufferedImage bi = QRgenerator.generate(qrCodeValue, 200, 200);
//                qrcodeTmpFile = File.createTempFile("reporter_qrcode_", null, tmpDir);
//                qrCodeOutputStream = new FileOutputStream(qrcodeTmpFile);
//                if (!ImageIO.write(bi, "png", qrCodeOutputStream)) {
//                    throw new WorkerException(getJobType(), "Errore nella creazione del qrCodeFile");
//                }
//                qrCodeOutputStream.flush();
//                data.put(qrCodeFieldName, qrcodeTmpFile.getAbsolutePath()); 
//            }
            File resourcePath = new File(workDir, resourcePathRelative);

            Configuration cfg = new Configuration();
//            log.info("resourcePath: " + resourcePath);
            cfg.setDirectoryForTemplateLoading(resourcePath);
            cfg.setObjectWrapper(new DefaultObjectWrapper());
            cfg.setDefaultEncoding("UTF-8");
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
            cfg.setIncompatibleImprovements(new Version(2, 3, 20));

            data.put("resourcePath", resourcePath.getAbsolutePath().replace("\\", "/"));
            String templateString;
            if (templateName != null && !templateName.equals("")) {
//                log.info("uso templateName");
                Path templateFilePath = Paths.get(resourcePath.getAbsolutePath(), TEMPLATES_PATH, templateName);
                templateString = IOUtils.toString(templateFilePath.toUri(), StandardCharsets.UTF_8.name());
            } else {
//                log.info("uso templatePath");
                InputStream tempalteIs = mongoWrapper.getByPath(templatePath);
                templateString = inputStreamToString(tempalteIs);
            }
            StringTemplateLoader stl = new StringTemplateLoader();
            stl.putTemplate("template", templateString);
            cfg.setTemplateLoader(stl);

            temp = cfg.getTemplate("template");

            baos = new ByteArrayOutputStream();
            Writer out = new OutputStreamWriter(baos);
            temp.process(data, out);

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
            pdfOut = new ByteArrayOutputStream();
            renderer.createPDF(pdfOut, true, true, PdfAConformanceLevel.PDF_A_1A);
            String tempFileName = String.format("reporter_%s.pdf", UUID.randomUUID().toString());
            String outPdfUuidTemp = md.put(new ByteArrayInputStream(pdfOut.toByteArray()), tempFileName, "/tmp", false);
            JSONObject res = new JSONObject();
            res.put("pdf", outPdfUuidTemp);
            return res;
        } catch (Exception ex) {

            File forTest = new File("output_wrong.html");
            Writer fileWriter = null;
            try {
                fileWriter = new FileWriter(forTest);
                if (temp != null && data != null) {
                    temp.process(data, fileWriter);
                }
            } catch (Exception subEx) {
//                log.error("", subEx);
            } finally {
                org.apache.commons.io.IOUtils.closeQuietly(fileWriter);
            }

            if (ex instanceof MasterjobsWorkerException) {
                throw (MasterjobsWorkerException) ex;
            } else {
                throw new MasterjobsWorkerException( ex);
            }
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(pdfOut);
            org.apache.commons.io.IOUtils.closeQuietly(baos);
            org.apache.commons.io.IOUtils.closeQuietly(qrCodeOutputStream);
            // cancellazione file temporaneo di qrcode
//            if (qrcodeTmpFile != null) {
//                boolean deleteted = qrcodeTmpFile.delete();
//            }
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
