package it.bologna.ausl.estrattore;

import it.bologna.ausl.estrattoremaven.exception.ExtractorException;
import it.bologna.ausl.mimetypeutilitymaven.Detector;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.IOUtils;
import org.apache.tika.mime.MediaType;

/**
 *
 * @author Salo
 */
public class SevenZipExtractor extends Extractor {

    private final MediaType[] mediaTypes = {Detector.MEDIA_TYPE_APPLICATION_7ZIP};

    public SevenZipExtractor(File file) {
        super(file);
    }

    @Override
    public MediaType[] getMediaTypesSupported() {
        return mediaTypes;
    }

    @Override
    public boolean isExtractable() throws ExtractorException {
        try {
            try (SevenZFile sevenZFile = new SevenZFile(file)) {
                SevenZArchiveEntry entry = sevenZFile.getNextEntry();
                while (entry != null) {
                    entry = sevenZFile.getNextEntry();
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public ArrayList<ExtractorResult> extract(File outputDir, String nameForCreatedFile) throws ExtractorException, FileNotFoundException, IOException {
        ArrayList<ExtractorResult> res = new ArrayList<>();
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }
        if (!outputDir.isDirectory()) {
            throw new ExtractorException(outputDir.getAbsolutePath() + " non è una directory", file.getName(), null);
        }
        SevenZFile sevenZFile = null;
        try {
            sevenZFile = new SevenZFile(file);

            String fileName = null;
            byte[] buffer = new byte[1024];
            ExtractorResult extractedFile = null;
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            while (entry != null) {
                fileName = entry.getName().replace("\ufffd", "");

                if (fileName.contains("/")) {
                    File dirs = new File(outputDir, fileName.substring(0, fileName.lastIndexOf("/")));
                    dirs.mkdirs();
                }

                try {
                    File newFile = null;
                    if (entry.isDirectory()) {
                        newFile = new File(outputDir, fileName);
                        if (!newFile.exists()) {
                            newFile.mkdir();
                        }
                    } else {
                        newFile = atomicCreateNotExistingFile(outputDir, fileName);
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(newFile);
                            int len;
                            while ((len = sevenZFile.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        } finally {
                            IOUtils.closeQuietly(fos);
                        }
                        try {
                            extractedFile = new ExtractorResult(newFile.getName(), getMimeType(newFile), newFile.length(), getHashFromFile(newFile, "SHA-256"), newFile.getAbsolutePath(), -1,null,null);
                        } catch (Exception ex) {
                            throw new ExtractorException(ex, file.getName(), fileName);
                        }

                        res.add(extractedFile);
                    }
                } catch (Exception ex) {
                    throw new ExtractorException(ex, file.getName(), fileName);
                }

                entry = sevenZFile.getNextEntry();
            }
        } catch (ExtractorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExtractorException(ex, file.getName(), null);
        } finally {
            try {
                sevenZFile.close();
            } catch (Exception ex) {
            }
        }
        return res;
    }
}
