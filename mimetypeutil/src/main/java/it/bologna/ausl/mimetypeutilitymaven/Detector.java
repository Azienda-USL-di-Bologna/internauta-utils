package it.bologna.ausl.mimetypeutilitymaven;

import java.io.*;
import java.security.Security;
import java.util.Scanner;
import static java.util.stream.DoubleStream.builder;
import javax.xml.XMLConstants;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.io.IOUtils;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.*;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.tsp.cms.CMSTimeStampedDataParser;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import static org.bouncycastle.asn1.x500.style.RFC4519Style.name;
import org.bouncycastle.mime.encoding.Base64InputStream;
import org.bouncycastle.util.encoders.Base64;
import org.xml.sax.SAXException;

/**
 *
 * @author Giuseppe De Marco (gdm)
 */
public class Detector {

    public static final MediaType MEDIA_TYPE_TEXT_PLAIN = MediaType.TEXT_PLAIN;
    public static final MediaType MEDIA_TYPE_OCTET_STREAM = MediaType.OCTET_STREAM;
    public static final MediaType MEDIA_TYPE_APPLICATION_XML = MediaType.APPLICATION_XML;
    public static final MediaType MEDIA_TYPE_TEXT_HTML = MediaType.TEXT_HTML;
    public static final MediaType MEDIA_TYPE_TEXT_XHTML_XML = MediaType.application("xhtml+xml");
    public static final MediaType MEDIA_TYPE_APPLICATION_PDF = MediaType.application("pdf");
    public static final MediaType MEDIA_TYPE_PKCS_7_SIGNATURE = MediaType.application("pkcs7-signature");
    public static final MediaType MEDIA_TYPE_PKCS_7_MIME = MediaType.application("pkcs7-mime");
    public static final MediaType MEDIA_TYPE_MESSAGE_RFC822 = MediaType.parse("message/rfc822");
    public static final MediaType MEDIA_TYPE_APPLICATION_MBOX = MediaType.application("mbox");
    public static final MediaType MEDIA_TYPE_APPLICATION_ZIP = MediaType.application("zip");
    public static final MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.application("json");
    public static final MediaType MEDIA_TYPE_APPLICATION_MSG = MediaType.application("vnd.ms-outlook");
    public static final MediaType MEDIA_TYPE_APPLICATION_7ZIP = MediaType.application("x-7z-compressed");
    public static final MediaType MEDIA_TYPE_APPLICATION_XDBF = MediaType.application("x-dbf");
    public static final MediaType MEDIA_TYPE_APPLICATION_ZIP_COMPRESSED = MediaType.application("x-zip-compressed");

    private final int PKCS_7_TYPE_DETACHED = 0;
    private final int PKCS_7_TYPE_NOT_DETACHED = 1;
    
    public String getMimeType(String filePath) throws UnsupportedEncodingException, IOException, MimeTypeException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            String mimeType = getMimeType(fis);
            MediaType mediaType = MediaType.parse(mimeType);
            if (mediaType == MEDIA_TYPE_TEXT_PLAIN) {
                String ext = getExtensionFromFileName(new File(filePath).getName());
                if (ext.equalsIgnoreCase("xml") || ext.equalsIgnoreCase("xmlx"))
                    mimeType = MEDIA_TYPE_APPLICATION_XML.toString();
                else if (ext.equalsIgnoreCase("html") || ext.equalsIgnoreCase("xhtml")) {
                    mimeType = MEDIA_TYPE_TEXT_HTML.toString();
                }
            }
            return mimeType;
        }
        finally {
            IOUtils.closeQuietly(fis);
        }
    }
    
    public String getMimeType(InputStream is) throws UnsupportedEncodingException, IOException, MimeTypeException {
//        byte[] inputStreamBytes = inputStreamToBytes(is);
        TikaInputStream tikais = null;
//        File tempFile = File.createTempFile(getClass().getSimpleName() + "_", null, new File("c:/tmp/test"));
        File tempFile = File.createTempFile(getClass().getSimpleName() + "_", null);
        inputStreamToFile(is, tempFile);
        try {
            tikais = TikaInputStream.get(tempFile.toPath());
            MediaType mediaType;
            TikaConfig config = TikaConfig.getDefaultConfig();
            if (tikais.getLength() == 0) {
                mediaType = MEDIA_TYPE_TEXT_PLAIN;
            }
            else {
                org.apache.tika.detect.Detector detector = new DefaultDetector(MimeTypes.getDefaultMimeTypes());
                mediaType = detector.detect(tikais, new Metadata());
            }
            IOUtils.closeQuietly(tikais);
            
            // spesso tika sbaglia nel rilevare i p7m, a volte li vede come "application/octet-stream" altre come "p7s".
            // in questo caso controllo il file col le librerie di BouncyCastle
//            System.out.println(mediaType.toString());
            if (mediaType == MEDIA_TYPE_APPLICATION_XDBF || mediaType == MEDIA_TYPE_APPLICATION_PDF || mediaType == MEDIA_TYPE_TEXT_PLAIN || 
                    mediaType == MEDIA_TYPE_OCTET_STREAM || mediaType == MEDIA_TYPE_PKCS_7_SIGNATURE) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(tempFile);
                    int pkcs7Type = whatP7m(fis);
                    if (pkcs7Type == PKCS_7_TYPE_DETACHED)
                        mediaType = MEDIA_TYPE_PKCS_7_SIGNATURE;
                    else if (pkcs7Type == PKCS_7_TYPE_NOT_DETACHED)
                        mediaType = MEDIA_TYPE_PKCS_7_MIME;
                    //else
                        //mediaType = MEDIA_TYPE_OCTET_STREAM;
                }
                finally {
                    IOUtils.closeQuietly(fis);
                }
                
                if (mediaType == MEDIA_TYPE_TEXT_PLAIN && isXML(tempFile)){
                        mediaType = MEDIA_TYPE_APPLICATION_XML;
                }
            }
            // caso per cercare di individuare i casi di file p7m PEM senza il -----BEGIN CERTIFICATE-----
            if (mediaType == MEDIA_TYPE_TEXT_PLAIN) {
                // provo ad aggiundere al file la riga -----BEGIN CERTIFICATE----- all'inizio e -----END CERTIFICATE----- alla fine
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(tempFile);
                    int pkcs7Type = detectPemMalformed(fis);
                    if (pkcs7Type == PKCS_7_TYPE_DETACHED)
                        mediaType = MEDIA_TYPE_PKCS_7_SIGNATURE;
                    else if (pkcs7Type == PKCS_7_TYPE_NOT_DETACHED)
                        mediaType = MEDIA_TYPE_PKCS_7_MIME;
                    //else
                        //mediaType = MEDIA_TYPE_OCTET_STREAM;
                }
                finally {
                    IOUtils.closeQuietly(fis);
                }
            }
            if (mediaType == MEDIA_TYPE_TEXT_PLAIN) {
                // provo ad aggiundere al file la riga -----BEGIN CERTIFICATE----- all'inizio e -----END CERTIFICATE----- alla fine
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(tempFile);
                    int pkcs7Type = detectStrangeP7mFormat(fis);
                    if (pkcs7Type == PKCS_7_TYPE_DETACHED)
                        mediaType = MEDIA_TYPE_PKCS_7_SIGNATURE;
                    else if (pkcs7Type == PKCS_7_TYPE_NOT_DETACHED)
                        mediaType = MEDIA_TYPE_PKCS_7_MIME;
                    //else
                        //mediaType = MEDIA_TYPE_OCTET_STREAM;
                }
                finally {
                    IOUtils.closeQuietly(fis);
                }
            }
            
            if (mediaType == MEDIA_TYPE_TEXT_PLAIN || mediaType == MEDIA_TYPE_TEXT_HTML || mediaType == MEDIA_TYPE_TEXT_XHTML_XML) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(tempFile);
                    if (isEml(fis))
                        mediaType = MEDIA_TYPE_MESSAGE_RFC822;
                }
                finally {
                    IOUtils.closeQuietly(fis);
                }
                
            }
            
            MimeType mimeType = config.getMimeRepository().forName(mediaType.toString());
            return mimeType.getName();
        }
        finally {
            IOUtils.closeQuietly(tikais);
            try {
                tempFile.delete();
            }
            catch (Exception ex) {
            }
        }
    }
    
    public int detectPemMalformed(InputStream is) {
        File tempFile = null;
        FileReader reader = null;
        PEMParser pemParser = null;
        ByteArrayInputStream bis = null;
        try {
            tempFile = File.createTempFile(Detector.class.getSimpleName() + "_retrying_p7m_pem_malformed_", null);
            fixPemInputStream(is, tempFile);
//            IOUtils.closeQuietly(is);
//            is = new FileInputStream(tempFile);
            reader = new FileReader(tempFile);
            pemParser = new PEMParser(reader);
            byte[] bytes = pemParser.readPemObject().getContent();
            bis = new ByteArrayInputStream(bytes);
            return whatP7m(bis);
        }
        catch (Exception ex) {
        }
        finally {
            IOUtils.closeQuietly(pemParser);
            IOUtils.closeQuietly(reader);
//            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(bis);
            tempFile.delete();
        }
        return -1;
    }
    
    /**
     * ci sono alcuni p7m in base 64 con mixato a xml, per questo provo a trattarlo con il Base64InputStream in modo da convertirlo in binario
     * @param is
     * @return 
     */
    public int detectStrangeP7mFormat(InputStream is) {
        File tempFile = null;
        Base64InputStream bis = null;
        try {
            tempFile = File.createTempFile(Detector.class.getSimpleName() + "_retrying_p7m_pem_malformed_", null);
            bis = new Base64InputStream(is);
            inputStreamToFile(bis, tempFile);
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(is);
            is = new FileInputStream(tempFile);
            return whatP7m(is);
        }
        catch (Exception ex) {
        }
        finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(bis);
            tempFile.delete();
        }
        return -1;
    }
    
    private static void fixPemInputStream(InputStream inputStream, File fileToCreate) throws FileNotFoundException, IOException {
        try (OutputStream os = new FileOutputStream(fileToCreate)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            os.write("-----BEGIN CERTIFICATE-----\n".getBytes());
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.write("\n-----END CERTIFICATE-----".getBytes());
        }
    }
    
    
    public String getMimeTypeProParer(InputStream is) throws UnsupportedEncodingException, IOException, MimeTypeException {
        TikaInputStream tikais = null;
//        File tempFile = File.createTempFile(getClass().getSimpleName() + "_", null, new File("c:/tmp/test"));
        File tempFile = File.createTempFile(getClass().getSimpleName() + "_", null);
        inputStreamToFile(is, tempFile);
        try {
            tikais = TikaInputStream.get(tempFile.toPath());
            MediaType mediaType;
            TikaConfig config = TikaConfig.getDefaultConfig();
            if (tikais.getLength() == 0) {
                mediaType = MEDIA_TYPE_TEXT_PLAIN;
            }
            else {
                org.apache.tika.detect.Detector detector = new DefaultDetector(MimeTypes.getDefaultMimeTypes());
                mediaType = detector.detect(tikais, new Metadata());
            }
            
            // spesso tika sbaglia nel rilevare i p7m, a volte li vede come "application/octet-stream" altre come "p7s".
            // in questo caso controllo il file col le librerie di BouncyCastle
            if (mediaType == MEDIA_TYPE_APPLICATION_XDBF || mediaType == MEDIA_TYPE_TEXT_PLAIN || 
                    mediaType == MEDIA_TYPE_OCTET_STREAM || mediaType == MEDIA_TYPE_PKCS_7_SIGNATURE) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(tempFile);
                    int pkcs7Type = whatP7m(fis);
                    if (pkcs7Type == PKCS_7_TYPE_DETACHED)
                        mediaType = MEDIA_TYPE_PKCS_7_SIGNATURE;
                    else if (pkcs7Type == PKCS_7_TYPE_NOT_DETACHED)
                        mediaType = MEDIA_TYPE_PKCS_7_MIME;
                }
                finally {
                    IOUtils.closeQuietly(fis);
                }
                
                if (mediaType == MEDIA_TYPE_TEXT_PLAIN && isXML(tempFile)){
                        mediaType = MEDIA_TYPE_APPLICATION_XML;
                }
            }
                        
            MimeType mimeType = config.getMimeRepository().forName(mediaType.toString());
            return mimeType.getName();
        }
        finally {
            IOUtils.closeQuietly(tikais);
            try {
                tempFile.delete();
            }
            catch (Exception ex) {
            }
        }
    }
    
    private boolean isXML(File file){
        Document document = null;
        DocumentBuilder builder = null;
        FileInputStream fis = null;
        try{
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();
            // devo passare dall'InputStream per poterlo chiudere, altrimenti il file rimane occupato e non si riesce a cancellare
            fis = new FileInputStream(file);
            document = builder.parse(fis);
        } catch(Exception ex) {
            return false;
        }
        finally {
         IOUtils.closeQuietly(fis);
        }
        return true;
        
    }
    
    /** Legge un InputStream e ne ritorna i bytes
     * 
     * @param is l'InputStream da leggere
     * @return i bytes letti
     * @throws UnsupportedEncodingException
     * @throws IOException 
     */
    private byte[] inputStreamToBytes(InputStream is) throws UnsupportedEncodingException, IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] readData = new byte[1024];
            int bytesRead = is.read(readData);
            while (bytesRead > 0) {
                baos.write(readData, 0, bytesRead);
                bytesRead = is.read(readData);
            }
            return baos.toByteArray();
        }
        finally {
            is.close();
            baos.close();
        }
    }
    
    /** Scrive un InputStream in un file
     * 
     * @param inputStream l'InpurStream da scrivere
     * @param fileToCreate il file da creare
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void inputStreamToFile(InputStream inputStream, File fileToCreate) throws FileNotFoundException, IOException {
        try (OutputStream os = new FileOutputStream(fileToCreate)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }

    private String getExtensionFromFileName(String fileName) {
        String res = "";
        int pos = fileName.lastIndexOf(".");
        if (pos > 0) {
            res = fileName.substring(pos + 1, fileName.length());
        }
        return res;
    }

     /** indica se il file è un p7m e eventualmente di che tipo
     *
     * @param is l'InputStream del file da controllare
     * @return "1" se il file è un p7m, "0" se il file è un pkcs-7 detached (p7s), "-1" se il file non è un p7m
     */
    public static int whatP7m(InputStream is) {
    ASN1InputStream asn1 = null;
    File tempFile = null;
    FileReader reader = null;
    PEMParser pemParser = null;
    InputStream newIs = null;
    FileInputStream newFis = null;
        try {
            try {
//                tempFile = File.createTempFile(Detector.class.getSimpleName() + "_retrying_p7m_", null, new File("c:/tmp/test"));
                tempFile = File.createTempFile(Detector.class.getSimpleName() + "_retrying_p7m_", null);
                inputStreamToFile(is, tempFile);
//                IOUtils.closeQuietly(is);
                newIs = new FileInputStream(tempFile);
                asn1 = new ASN1InputStream(newIs);
                CMSSignedData cms = new CMSSignedData(asn1);
                if (cms.getSignedContent() == null)
                    return 0;
                else
                    return 1;
            }
            catch (CMSException ex) {
//                ex.printStackTrace();
                IOUtils.closeQuietly(newIs);
                //is = new FileInputStream(tempFile);
                try {
                    reader = new FileReader(tempFile);
                    pemParser = new PEMParser(reader);
                    byte[] bytes = pemParser.readPemObject().getContent();
                    newIs = new ByteArrayInputStream(bytes);
                }
                catch (Exception subEx) {
                    IOUtils.closeQuietly(newIs);
                    newFis = new FileInputStream(tempFile);
                    CMSTimeStampedDataParser tsd = new CMSTimeStampedDataParser(newFis);
                    newIs = tsd.getContent();
                }
                return whatP7m(newIs);
            }
        }
        catch (Exception ex) {
//            ex.printStackTrace();
            return -1;
        }
        finally {
//            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(asn1);
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(pemParser);
            IOUtils.closeQuietly(newFis);
            try {
                tempFile.delete();
            }
            catch (Exception ex) {
            }
        }
    }
    
    public boolean isEml(InputStream is) {
        try {
//            MimeBodyPart mimeBodyPart = SMIMEUtil.toMimeBodyPart(is);
//            String messageId = mimeBodyPart.getHeader("Message-Id", "");
            Scanner scanner = new Scanner(is);
            boolean messageIdFound = false;
            boolean toFound = false;
            while (scanner.hasNextLine() && !(messageIdFound && toFound)) {
                String line = scanner.nextLine();
                if (line.trim().equals("")) {
                    break;
                }
                else if(line.trim().toLowerCase().startsWith("message-id:")) {
                    messageIdFound = true;
                }
                else if(line.trim().toLowerCase().startsWith("to:")) {
                    toFound = true;
                }
            }
            return messageIdFound && toFound;
        }
        catch (Exception ex) {
            return false;
        }
    }
    
    public static void main(String args[]) throws UnsupportedEncodingException, IOException, MimeTypeException, ParserConfigurationException, SAXException {

//        System.out.println(MEDIA_TYPE_TEXT_PLAIN);
//        System.out.println(MEDIA_TYPE_OCTET_STREAM);
//        System.out.println(MEDIA_TYPE_APPLICATION_XML);
//        System.out.println(MEDIA_TYPE_TEXT_HTML);
//        System.out.println(MEDIA_TYPE_TEXT_XHTML_XML);
//        System.out.println(MEDIA_TYPE_APPLICATION_PDF);
//        System.out.println(MEDIA_TYPE_PKCS_7_SIGNATURE);
//        System.out.println(MEDIA_TYPE_PKCS_7_MIME);
//        System.out.println(MEDIA_TYPE_MESSAGE_RFC822);
//        System.out.println(MEDIA_TYPE_APPLICATION_ZIP);
//        System.out.println(MEDIA_TYPE_APPLICATION_JSON);

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

//        FileInputStream a = new FileInputStream("c:/tmp/PG0130655_2013_AA FONDAROLI CORAZZA.pdf.p7m");
//        System.out.println(whatP7m(a));
        Detector detector = new Detector();
//        System.out.println(detector.getMimeType(a));
        File file1 = new File("C:\\Users\\Top\\Downloads\\Contatore PEC.doc.p7m");
//        File file2 = new File("C:/ciao.txt");
//        File file3 = new File("c:/tmp/gdm/NSI_AS400.mdb");
//        File file4 = new File("c:/tmp/gdm/gdm.7z");
//        File file5 = new File("c:/tmp/gdm/PROSA.MDB");
//        File file6 = new File("C:/downloads/doc2.eml");
//        File file7 = new File("c:/tmp/Convocazione IV CdS VIA Montieco S r l -_Anzola_dell_Emilia _BO_.pdf.p7m");
//        System.out.println(isEml(new FileInputStream(file3)));
//                System.exit(0);
//        System.out.println(file1.getName() + " - " + detector.getMimeType(file1.getAbsolutePath()));
//        System.out.println(file1.getName() + " - " + detector.getMimeTypeProParer(new FileInputStream(file1)));
        System.out.println(file1.getName() + " - " + detector.getMimeType(file1.getAbsolutePath()));
        
        System.out.println(file1.getName() + " - " + detector.getMimeTypeProParer(new FileInputStream(file1)));

        
       

        
//        System.out.println(file3.getName() + " - " + detector.getMimeType(file3.getAbsolutePath()));
//        System.out.println(file4.getName() + " - " + detector.getMimeType(file4.getAbsolutePath()));
//        System.out.println(file5.getName() + " - " + detector.getMimeType(file5.getAbsolutePath()));
//        System.out.println(file6.getName() + " - " + detector.getMimeType(file6.getAbsolutePath()));
//        System.out.println(file6.getName() + " - " + detector.getMimeTypeProParer(new FileInputStream(file6)));
        System.exit(0);
//        System.out.println(ExtractTimeStampInfo(new File(file2)));
//        System.exit(0);

        
//        System.out.println(detector.getMimeType("c:/tmp/_Segnalazione_pdf_00566156-0.graffetta.pdf.p7m"));
//        System.out.println(detector.getMimeType("c:/tmp/PG0107754_2013_VEQ2014Opuscolo(firmato).pdf.p7m"));
//        TikaInputStream tikais = TikaInputStream.get(new FileInputStream("c:/tmp/frontespizio.doc"));
//        TikaConfig config = TikaConfig.getDefaultConfig();
//        org.apache.tika.detect.Detector detector = new DefaultDetector(MimeTypes.getDefaultMimeTypes());
//        MediaType mediaType = detector.detect(tikais, new Metadata());
//        MimeType mimeType = config.getMimeRepository().forName(mediaType.toString());
//        System.out.println(mimeType.getName());
        

//        TikaInputStream tikais = TikaInputStream.get(new File("c:/tmp/pdfMoltoGrande.pdf"));
//        TikaConfig config = TikaConfig.getDefaultConfig();
//        org.apache.tika.detect.Detector detector = new DefaultDetector(MimeTypes.getDefaultMimeTypes());
//        MediaType mediaType = detector.detect(tikais, new Metadata());
//        MimeType mimeType = config.getMimeRepository().forName(mediaType.toString());
//        System.out.println(mimeType.getName());
        
    }

}
