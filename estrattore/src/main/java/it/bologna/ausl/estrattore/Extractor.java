package it.bologna.ausl.estrattore;

import it.bologna.ausl.estrattoremaven.exception.ExtractorException;
import it.bologna.ausl.mimetypeutilitymaven.Detector;
import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;

/**
 *
 * @author Giuseppe De Marco (gdm)
 */
public abstract class Extractor {

    protected File file;

    public Extractor(File file) {
        this.file = file;
    }

    public abstract ArrayList<ExtractorResult> extract(File outputDir, String nameForCreatedFile) throws ExtractorException, FileNotFoundException, IOException;

    public abstract MediaType[] getMediaTypesSupported();

    public abstract boolean isExtractable() throws ExtractorException;

    public ArrayList<ExtractorResult> extract(File outputDir) throws ExtractorException, IOException {
        return extract(outputDir, null);
    }

    public boolean isOpenable() throws ExtractorException {
        try {
            boolean openable = false;
            String mimeType = getMimeType(file);
            MediaType mediaType = MediaType.parse(mimeType);
            if (Arrays.stream(getMediaTypesSupported()).anyMatch(mt -> mt.equals(mediaType))) {
                openable = true;
            }
            return openable;
        } catch (Exception ex) {
            throw new ExtractorException(ex, file.getName(), null);
        }
    }

    protected static String getMimeType(File file) throws UnsupportedEncodingException, IOException, MimeTypeException {
        Detector d = new Detector();
        return d.getMimeType(file.getAbsolutePath());
    }

    protected static String getMimeType(InputStream is) throws UnsupportedEncodingException, IOException, MimeTypeException {
        Detector d = new Detector();
        return d.getMimeType(is);
    }

    protected static String removeExtensionFromFileName(String fileName) {
        String res = fileName;
        int pos = fileName.lastIndexOf(".");
        if (pos > 0) {
            res = res.substring(0, pos);
        }
        return res;
    }

    protected static String getExtensionFromFileName(String fileName) {
        String res = "";
        int pos = fileName.lastIndexOf(".");
        if (pos > 0) {
            res = fileName.substring(pos + 1, fileName.length());
        }
        return res;
    }

    protected static String getFileExtension(File file) throws FileNotFoundException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return getFileExtension(fis);
        } finally {
            try {
                fis.close();
            } catch (Exception ex) {
            }
        }
    }

    protected static File atomicCreateNotExistingFile(File parentDir, String fileName) throws IOException {

        String ext = getExtensionFromFileName(fileName);
        String basename = removeExtensionFromFileName(fileName);
        File file = new File(parentDir, fileName);

        // se esista già aggiungiamo numeri in fondo.
        int i = 1;
        while (!file.createNewFile()) {
            file = new File(parentDir, basename + "_" + i++ + "." + ext);
        }

        return file;
    }

    protected static File atomicRenameToNotExistingFile(File sourceFile, File destinationFile) throws IOException {
        boolean success = false;
        File renamedFile = destinationFile;

        String parentDestinationDir = destinationFile.getParent();
        String basename = removeExtensionFromFileName(renamedFile.getName());
        String ext = getExtensionFromFileName(renamedFile.getName());
        int i = 1;
        while (!success) {
            try {
                FileUtils.moveFile(sourceFile, renamedFile);
                success = true;
            } catch (FileExistsException ex) {
                renamedFile = new File(parentDestinationDir, basename + "_" + i++ + "." + ext);
            }
        }
        return renamedFile;
    }

    protected static String getFileExtension(InputStream is) {
        String fileType = null;
        try {
            TikaConfig config = TikaConfig.getDefaultConfig();
            MediaType mediaType = MediaType.parse(getMimeType(is));
            fileType = getFileExtension(mediaType);
        } catch (Exception ex) {
            fileType = "txt";
        }
        return fileType;
    }

    protected static String getFileExtension(MediaType mediaType) throws MimeTypeException {
        TikaConfig config = TikaConfig.getDefaultConfig();
        MimeType mimeType = config.getMimeRepository().forName(mediaType.toString());
        String fileType = mimeType.getExtension().substring(1);

        return fileType;
    }

    protected static String getHashFromFile(File file, String algorithmName) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        MessageDigest algorithm = MessageDigest.getInstance(algorithmName);
        FileInputStream fis = null;
        DigestInputStream dis = null;
        try {
            fis = new FileInputStream(file);
            dis = new DigestInputStream(fis, algorithm);
            byte[] buffer = new byte[8192];
            while ((dis.read(buffer)) != -1) {
            }
            byte[] messageDigest = algorithm.digest();
            Formatter fmt = new Formatter();
            for (byte b : messageDigest) {
                fmt.format("%02X", b);
            }
            String hashString = fmt.toString();
            return hashString;
        } finally {
            try {
                dis.close();
            } catch (Exception ex) {
            }
            try {
                fis.close();
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Crea un file contente il testo passato
     *
     * @param fileToCreate il file da creare
     * @param text il testo da scrivere nel file
     * @throws FileNotFoundException
     */
    protected static void writeFileFromString(String text, File fileToCreate) throws FileNotFoundException {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(fileToCreate);
            pw.print(text);
            pw.flush();
        } finally {
            try {
                pw.close();
            } catch (Exception ex) {
            }
        }
    }
}
