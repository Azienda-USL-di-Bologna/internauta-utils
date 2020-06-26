package it.bologna.ausl.estrattore;

import it.bologna.ausl.estrattoremaven.exception.ExtractorException;
import it.bologna.ausl.mimetypeutilitymaven.Detector;
import java.io.*;
import java.util.ArrayList;
import org.apache.commons.io.IOUtils;
import org.apache.tika.mime.MediaType;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataParser;
import org.bouncycastle.mime.encoding.Base64InputStream;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.tsp.cms.CMSTimeStampedDataParser;

/**
 *
 * @author Giuseppe De Marco (gdm)
 */
public class P7mExtractor extends Extractor {

    private final MediaType[] mimeTypes = {Detector.MEDIA_TYPE_PKCS_7_MIME};

    public P7mExtractor(File file) {
        super(file);
    }

    @Override
    public MediaType[] getMediaTypesSupported() {
        return mimeTypes;
    }

    @Override
    public boolean isExtractable() throws ExtractorException {
        return true;
    }

    @Override
    public ArrayList<ExtractorResult> extract(File outputDir, String nameForCreatedFile) throws ExtractorException, FileNotFoundException, IOException {

        ArrayList<ExtractorResult> res = new ArrayList<ExtractorResult>();
        InputStream fis = new FileInputStream(file);
        FileOutputStream tempExtractedFileOs = null;

        if (!outputDir.exists()) {
            outputDir.mkdir();
        }
        if (!outputDir.isDirectory()) {
            throw new ExtractorException(outputDir.getAbsolutePath() + " non è una directory", file.getName(), null);
        }

        // il file che sarà creato dal contenuto del p7m. Verrà creato come file temporaneo e successivamente rinominato.
//        File fileToCreate = File.createTempFile(getClass().getSimpleName() + "_", null, outputDir);
        File tempExtractedFile = File.createTempFile(getClass().getSimpleName() + "_", null, outputDir);

        // estrazione del file contenuto nella busta
        ASN1InputStream asn1 = null;
        CMSSignedData cms = null;
        FileReader reader = null;
        PEMParser pemParser = null;
        FileInputStream newFis = null;
        try {
            try {
                asn1 = new ASN1InputStream(fis);
                cms = new CMSSignedData(asn1);
            } catch (CMSException ex) {
                try {
                    IOUtils.closeQuietly(fis);
                    reader = new FileReader(file);
                    pemParser = new PEMParser(reader);
                    byte[] bytes = pemParser.readPemObject().getContent();
                    fis = new ByteArrayInputStream(bytes);
                } catch (Exception subEx) {
                    try {
                        IOUtils.closeQuietly(fis);
                        newFis = new FileInputStream(file);
    //                    CMSTimeStampedDataParser tsd = new CMSTimeStampedDataParser(newFis);
                        CMSSignedDataParser tsd = new CMSSignedDataParser(new BcDigestCalculatorProvider(), newFis);
                        fis = tsd.getSignedContent().getContentStream();
                    }
                    catch (Exception subSubEx) {
                        IOUtils.closeQuietly(newFis);
                        newFis = new FileInputStream(file);
                        fis = new Base64InputStream(newFis);
//                        asn1 = new ASN1InputStream(streambase64);
//                        cms = new CMSSignedData(asn1);
                    }
                }
                asn1 = new ASN1InputStream(fis);
                cms = new CMSSignedData(asn1);
            }
            if (cms.getSignedContent() != null) {
                tempExtractedFileOs = new FileOutputStream(tempExtractedFile);
                cms.getSignedContent().write(tempExtractedFileOs);
            }
        } catch (Exception ex) {
            try {
                tempExtractedFile.delete();
            } catch (Exception subEx) {
            }
            throw new ExtractorException(ex, file.getName(), null);
        } finally {
            IOUtils.closeQuietly(asn1);
            IOUtils.closeQuietly(fis);
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(pemParser);
            IOUtils.closeQuietly(newFis);
            IOUtils.closeQuietly(tempExtractedFileOs);
        }

        // il file è stato estratto, ora calcolo l'estensione del file e lo rinomino.
        // Prima: Il nome sarà: nome del file p7m.estensione calcolata a partire dal mimeType del file estratto
        // Ora : il nome del file sarà il nome passato in input al metodo depurato da eventuali "." + estensione calcolata a partire dal mimeType del file estratto
        String filename = null;
        if (nameForCreatedFile == null) {
            filename = removeExtensionFromFileName(file.getName());
        } else {
            filename = nameForCreatedFile;
        }
        String ext = null;
        try {
            fis = new FileInputStream(tempExtractedFile);
            ext = getFileExtension(fis);
        } catch (Exception ex) {
            throw new ExtractorException("errore nel calcolo dell'estensione del file", ex, file.getName(), null);
        } finally {
            IOUtils.closeQuietly(fis);
        }
        filename = filename.replace("." + ext, "");

        File renamedFile = null;
        try {
            renamedFile = atomicRenameToNotExistingFile(tempExtractedFile, new File(tempExtractedFile.getParent(), filename + "." + ext));
        } catch (Exception ex) {
            tempExtractedFile.delete();
            throw new ExtractorException("errore nella rinomina del file", ex, file.getName(), null);
        }
        tempExtractedFile = null;

        // creo il risultato da restituire
        ExtractorResult extractorResult = null;
        try {
            extractorResult = new ExtractorResult(renamedFile.getName(), getMimeType(renamedFile), renamedFile.length(), getHashFromFile(renamedFile, "SHA-256"), renamedFile.getAbsolutePath(), -1,null,null);
            res.add(extractorResult);
        } catch (Exception ex) {
            throw new ExtractorException("errore nella creazione del risultato.", ex, file.getName(), null);
        }
        return res;
    }
}
