package it.bologna.ausl.estrattore;

import it.bologna.ausl.estrattoremaven.exception.ExtractorException;
import java.io.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import org.apache.tika.mime.MediaType;

/**
 *
 * @author Giuseppe De Marco (gdm)
 */
public class ExtractorCreator {

    public Object stream() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public enum MimeTypeSetOperation {
        ALLOWED, DENIED
    };

    private enum SupportedExtractorsClass {
        ZipExtractor, EmlExtractor, P7mExtractor, SevenZipExtractor, MsgExtractor
    };

    private final File file;
    private final Set<String> mimeTypeSet;
    private MimeTypeSetOperation mimeTypeSetOperation = MimeTypeSetOperation.ALLOWED;

    public ExtractorCreator(File file) {
        this.file = file;
        this.mimeTypeSet = null;
        this.mimeTypeSetOperation = null;
    }

    public ExtractorCreator(File file, Set<String> mimeTypeSet, MimeTypeSetOperation mimeTypeSetOperation) {
        this.file = file;
        this.mimeTypeSet = mimeTypeSet;
        this.mimeTypeSetOperation = mimeTypeSetOperation;
    }
    
    public static boolean isSupportedMimyType(String mimeType) throws Exception {
        System.out.println("isSupportedMimyType: " + mimeType);
        SupportedExtractorsClass[] extractors = SupportedExtractorsClass.values();
        for (SupportedExtractorsClass ec: extractors) {
            try {
                Class<Extractor> extractorClass = (Class<Extractor>) Class.forName("it.bologna.ausl.estrattore." + ec.name());
                Constructor<Extractor> constructor = extractorClass.getConstructor(File.class);
                Extractor extractor = constructor.newInstance((Object) null);
                MediaType[] mediaTypesSupported = extractor.getMediaTypesSupported();
                if (Arrays.stream(mediaTypesSupported).anyMatch(m -> m.compareTo(MediaType.parse(mimeType)) == 0)) {
                    return true;
                }
            } catch (Exception ex) {
                throw ex;
            }
        }
        return false;
    }

    public ArrayList<ExtractorResult> extractAll(File outputDir) throws ExtractorException {
        return extractAll(outputDir, null);
    }

    public ArrayList<ExtractorResult> extractAll(File outputDir, String nameForCreatedFile) throws ExtractorException {
        try {
            ArrayList<ExtractorResult> res = new ArrayList<>();
            if (nameForCreatedFile == null) {
                nameForCreatedFile = Extractor.removeExtensionFromFileName(file.getName());
            }
            if (isExtractable(file)) {
                return recursiveExtract(null, file, outputDir, res, nameForCreatedFile, 0, null,null);
            } else {
                throw new ExtractorException("il file non è estraibile", file.getName(), null);
            }
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            throw new ExtractorException(t.getMessage(), t, null, null);
        }
    }

    private ArrayList<ExtractorResult> recursiveExtract(String mimeType, File file, File outputDir, ArrayList<ExtractorResult> partialRes, String nameForCreatedFile, int level, String padre,String antenati) throws ExtractorException {
       
        try {
            level++;
            Class<Extractor> extractorClass = getSupportedExtractorClass(file);
            if (extractorClass != null) {
                Constructor<Extractor> constructor = extractorClass.getConstructor(File.class);
                Extractor extractor = constructor.newInstance(file);
                if (!isAllowed(extractor.getMediaTypesSupported())) {
                    extractorClass = null;
                }
            }

//            if (extractorClass == null) {
            String resultFileName = null;
            if (level == 1) {
                String ext = Extractor.getExtensionFromFileName(file.getName());
                resultFileName = nameForCreatedFile + "." + ext;
            } else {
                nameForCreatedFile = null;
                resultFileName = file.getName();
            }
            
            ExtractorResult extractorResult = new ExtractorResult(resultFileName, mimeType != null ? mimeType : Extractor.getMimeType(file), file.length(), Extractor.getHashFromFile(file, "SHA-256"), file.getAbsolutePath(), level,padre ,antenati);
            partialRes.add(extractorResult);
//            }
            if (extractorClass != null) {
                Constructor<Extractor> constructor = extractorClass.getConstructor(File.class);
                Extractor extractor = constructor.newInstance(file);

                try {
                    ArrayList<ExtractorResult> res = extractor.extract(outputDir, nameForCreatedFile);
                    for (ExtractorResult er : res) {
                        File fileToExtract = new File(er.getPath());
                        if (antenati != null && antenati != "null" && antenati != ""){
                            antenati = antenati + "\\";
                        }
                        else
                            antenati = "";
                        partialRes = recursiveExtract(er.getMimeType(), fileToExtract, outputDir, partialRes, nameForCreatedFile, level,Extractor.getHashFromFile(file, "SHA-256"), antenati + file.getName());
                        fileToExtract = null;
                    }
                } catch (ExtractorException ex) {
                    ex.printStackTrace(System.out);
                }
//                if (level > 1) {
//                    if (!file.delete())
//                        System.out.println("file: " + file.getAbsolutePath() + " non cancellato");
//                }

            }
            return partialRes;
        } catch (ExtractorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExtractorException(ex, file.getName(), null);
        }
    }

    public Class<Extractor> getSupportedExtractorClass() throws ExtractorException {
        return getSupportedExtractorClass(file);
    }

    private Class<Extractor> getSupportedExtractorClass(File file) throws ExtractorException {
        try {
            Class<Extractor> res = null;
            SupportedExtractorsClass[] extractors = SupportedExtractorsClass.values();
            for (SupportedExtractorsClass ec : extractors) {
                Class<Extractor> extractorClass = (Class<Extractor>) Class.forName("it.bologna.ausl.estrattore." + ec.name());
                Constructor<Extractor> constructor = extractorClass.getConstructor(File.class);
                Extractor extractor = constructor.newInstance(file);
                if (extractor.isOpenable()) {
                    res = extractorClass;
                    break;
                }
                //            System.out.println(Arrays.toString(mimeTypeSupported));
            }
            return res;
        } catch (ExtractorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExtractorException(ex, file.getName(), null);
        }
    }

    public boolean isExtractable() throws ExtractorException {
        return isExtractable(file);
    }

    private boolean isExtractable(File file) throws ExtractorException {
        try {
            Class<Extractor> extractorClass = getSupportedExtractorClass(file);
            Constructor<Extractor> constructor = extractorClass.getConstructor(File.class);
            Extractor extractor = constructor.newInstance(file);

            MediaType[] mediaTypesSupported = extractor.getMediaTypesSupported();
            return isAllowed(mediaTypesSupported) && extractor.isExtractable();
        } catch (Exception ex) {
//            ex.printStackTrace();
            return false;
        }
    }

    private boolean isAllowed(MediaType mediaType) {
        boolean isAllowed = true;
        if (mimeTypeSet != null) {
            String mimeType = mediaType.toString();
            if (mimeTypeSetOperation == MimeTypeSetOperation.ALLOWED) {
                isAllowed = mimeTypeSet.contains(mimeType);
            } else if (mimeTypeSetOperation == MimeTypeSetOperation.DENIED) {
                isAllowed = !mimeTypeSet.contains(mimeType);
            }
        }
        return isAllowed;
    }

    private boolean isAllowed(MediaType[] mediaTypes) {
        boolean isAllowed = true;
        for (MediaType mediaType : mediaTypes) {
            isAllowed = isAllowed(mediaType);
            if (isAllowed == true && mimeTypeSetOperation == MimeTypeSetOperation.ALLOWED) {
                break;
            } else if (isAllowed == false && mimeTypeSetOperation == MimeTypeSetOperation.DENIED) {
                break;
            }
        }
        return isAllowed;
    }
}
