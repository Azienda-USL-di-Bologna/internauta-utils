package it.bologna.ausl.estrattore;

import it.bologna.ausl.estrattoremaven.exception.ExtractorException;
import it.bologna.ausl.mimetypeutilitymaven.Detector;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.apache.tika.mime.MediaType;

/**
 *
 * @author Giuseppe De Marco (gdm)
 */
public class ZipExtractor extends Extractor {

    private final MediaType[] mediaTypes = {Detector.MEDIA_TYPE_APPLICATION_ZIP, Detector.MEDIA_TYPE_APPLICATION_ZIP_COMPRESSED};

    public ZipExtractor(File file) {
        super(file);
    }

    @Override
    public MediaType[] getMediaTypesSupported() {
        return mediaTypes;
    }

    @Override
    public boolean isExtractable() throws ExtractorException {

        ZipFile zip = null;
        try {
            //get the zip file content
            zip = new ZipFile(file);
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry zipEntry = entries.nextElement();
                InputStream is = null;
                try {
                    is = zip.getInputStream(zipEntry);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        } finally {
            try {
                zip.close();
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public ArrayList<ExtractorResult> extract(File outputDir, String nameForCreatedFile) throws ExtractorException, FileNotFoundException, IOException {
        ArrayList<ExtractorResult> res = new ArrayList<ExtractorResult>();
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }
        if (!outputDir.isDirectory()) {
            throw new ExtractorException(outputDir.getAbsolutePath() + " non è una directory", file.getName(), null);
        }
        ZipFile zip = null;
        try {
            zip = new ZipFile(file);
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();

            String fileName = null;
            byte[] buffer = new byte[1024];

            ExtractorResult extractedFile = null;
            ZipArchiveEntry zipEntry = null;
            while (entries.hasMoreElements()) {
                zipEntry = entries.nextElement();
                fileName = zipEntry.getName().replace("\ufffd", "");

                // caso degli zip strani che non hanno le directory come entries, se trovo una barra nel nome dell'entry creo la struttura di cartelle
                if (fileName.contains("/")) {
                    File dirs = new File(outputDir, fileName.substring(0, fileName.lastIndexOf("/")));
                    dirs.mkdirs();
//                    fileName = new File(outputDir, fileName).getName();
                }

                InputStream zipEntryInputStream = null;
                try {
                    zipEntryInputStream = zip.getInputStream(zipEntry);

                    File newFile = null;

                    if (zipEntry.isDirectory()) {
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
                            while ((len = zipEntryInputStream.read(buffer)) > 0) {
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
                        //                System.out.println("added: " + extractedFile);
                        res.add(extractedFile);
                    }
                } catch (Exception ex) {
                    throw new ExtractorException(ex, file.getName(), fileName);
                } finally {
                    IOUtils.closeQuietly(zipEntryInputStream);
                }
            }
        } catch (ExtractorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExtractorException(ex, file.getName(), null);
        } finally {
            try {
                zip.close();
            } catch (Exception ex) {
            }
        }
//        System.out.println("Done");
        return res;
    }

}
