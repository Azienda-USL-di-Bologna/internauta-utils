package it.bologna.ausl.mongowrapper;

/**
 *
 * @author spritz
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;

public class WrapperMinIO extends MongoWrapper {

    private final MinIOWrapper minIOWrapper;
    private String codiceAzienda;
    private static final Logger log = LogManager.getLogger(WrapperMinIO.class);

    public WrapperMinIO(String mongoUri, String minIODBDriver, String minIODBUrl, String minIODBUsername, String minIODBPassword, String codiceAzienda, Integer maxPoolSize, ObjectMapper objectMapper) throws UnknownHostException, MongoException, MongoWrapperException {
        super(mongoUri);
        if (codiceAzienda == null) {
            throw new MongoWrapperException("il codiceAzienda è obbligatorio!");
        }
        this.codiceAzienda = codiceAzienda;
        if (objectMapper != null) {
            minIOWrapper = new MinIOWrapper(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, maxPoolSize, objectMapper);
        } else {
            minIOWrapper = new MinIOWrapper(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, maxPoolSize);
        }
    }

    private String[] splitPath(String pathWithFileName) {
        pathWithFileName = StringUtils.cleanPath(pathWithFileName);
        String filename = StringUtils.getFilename(pathWithFileName);
        String path = StringUtils.trimTrailingCharacter(pathWithFileName.substring(0, pathWithFileName.length() - filename.length()), '/');
        return new String[]{path, filename};
    }

    @Override
    public Calendar getUploadDateByPath(String filePath) throws MongoWrapperException {
        String[] splittedPath = splitPath(filePath);
        try {
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByPathAndFileName(splittedPath[0], splittedPath[1], codiceAzienda);
            if (fileInfo != null) {
                return GregorianCalendar.from(fileInfo.getUploadDate());
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
        return null;
    }

    @Override
    public Calendar getUploadDateByUuid(String uuid) throws MongoWrapperException {
        try {
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid);
            if (fileInfo != null) {
                return GregorianCalendar.from(fileInfo.getUploadDate());
            } else {
                fileInfo = minIOWrapper.getFileInfoByFileId(uuid);
                if (fileInfo != null) {
                    return GregorianCalendar.from(fileInfo.getUploadDate());
                }
                return null;
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public Long getSizeByUuid(String uuid) throws MongoWrapperException {
        try {
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid);
            if (fileInfo != null) {
                return fileInfo.getSize().longValue();
            } else {
                fileInfo = minIOWrapper.getFileInfoByFileId(uuid);
                if (fileInfo != null) {
                    return fileInfo.getSize().longValue();
                }
                return null;
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public Long getSizeByPath(String filepath) throws MongoWrapperException {
        try {
            String[] splittedPath = splitPath(filepath);
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByPathAndFileName(splittedPath[0], splittedPath[1], codiceAzienda);
            if (fileInfo != null) {
                return fileInfo.getSize().longValue();
            }
            return null;
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public String getFilePathByUuid(String uuid) throws MongoWrapperException {
        try {
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid);
            if (fileInfo != null) {
                return fileInfo.getPath() + "/" + fileInfo.getFileName();
            } else {
                fileInfo = minIOWrapper.getFileInfoByFileId(uuid);
                if (fileInfo != null) {
                    return fileInfo.getPath() + "/" + fileInfo.getFileName();
                }
                return null;
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public String getFileName(String uuid) throws MongoWrapperException {
        try {
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid);
            if (fileInfo != null) {
                return fileInfo.getFileName();
            } else {
                fileInfo = minIOWrapper.getFileInfoByFileId(uuid);
                if (fileInfo != null) {
                    return fileInfo.getFileName();
                }
                return null;
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public Boolean existsObjectbyUUID(String uuid) throws MongoWrapperException {
        try {
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid);
            if (fileInfo != null) {
                return true;
            } else {
                fileInfo = minIOWrapper.getFileInfoByFileId(uuid);
                return (fileInfo != null);
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public Boolean existsObjectbyPath(String filepath) throws MongoWrapperException {
        try {
            String[] splittedPath = splitPath(filepath);
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByPathAndFileName(splittedPath[0], splittedPath[1], codiceAzienda);
            return (fileInfo != null);
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public void move(String uuid, String newFilepath) throws MongoWrapperException {
        try {
            String[] splittedPath = splitPath(newFilepath);
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid);
            if (fileInfo != null) {
                minIOWrapper.renameByFileId(fileInfo.getFileId(), splittedPath[0], splittedPath[1]);
            } else {
                fileInfo = minIOWrapper.getFileInfoByFileId(uuid);
                if (fileInfo != null) {
                    minIOWrapper.renameByFileId(fileInfo.getFileId(), splittedPath[0], splittedPath[1]);
                }
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public void rename(String uuid, String newName) throws MongoWrapperException {
        try {
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid);
            if (fileInfo != null) {
                minIOWrapper.renameByFileId(fileInfo.getFileId(), newName);
            } else {
                fileInfo = minIOWrapper.getFileInfoByFileId(uuid);
                if (fileInfo != null) {
                    minIOWrapper.renameByFileId(fileInfo.getFileId(), newName);
                }
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public void delDirFilesAndFolders(String dirname) throws MongoWrapperException {
        try {
            // non sapendo se sono stati spostati tutti i  file della cartella, cancello da entrambe le parti
            minIOWrapper.delFilesInPath(dirname, true, codiceAzienda);
            //super.delDirFilesAndFolders(dirname);
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public void delDirFiles(String dirname) throws MongoWrapperException {
        try {
            // non sapendo se sono stati spostati tutti i  file della cartella, cancello da entrambe le parti
            minIOWrapper.delFilesInPath(dirname, false, codiceAzienda);
            //super.delDirFiles(dirname);
        } catch (Exception ex) {
            log.error("errore", ex);
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public List<String> getFilesInfo(List<String> uuids, boolean includeDeleted) throws MongoWrapperException {
        try {
            List<String> res = new ArrayList();
            for (String uuid : uuids) {
                MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid, includeDeleted);
                if (fileInfo != null) {
                    res.add(fileInfo.toString());
                } else {
                    fileInfo = minIOWrapper.getFileInfoByFileId(uuid, includeDeleted);
                    if (fileInfo != null) {
                        res.add(fileInfo.toString());
                    }
                }
            }
            return res;
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public List<String> getDeleted() throws MongoWrapperException {
        try {
            List<String> deletedFromMinIO = collectionToStreamNullSafe(minIOWrapper.getDeleted(codiceAzienda, null)).map(fileInfo -> fileInfo.getFileId()).collect(Collectors.toList());
            return deletedFromMinIO;
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public List<String> getDeletedLessThan(long timeInMillis) throws MongoWrapperException {
        try {
            ZonedDateTime lessThanZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeInMillis), ZoneId.systemDefault());
            List<String> deletedFromMinIO = collectionToStreamNullSafe(minIOWrapper.getDeleted(codiceAzienda, lessThanZdt)).map(fileInfo -> fileInfo.getFileId()).collect(Collectors.toList());
            return deletedFromMinIO;
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public List<String> getFilesLessThan(ZonedDateTime time) throws MongoWrapperException {
        try {
            List<String> files = collectionToStreamNullSafe(minIOWrapper.getFilesLessThan(codiceAzienda, time, true)).map(fileInfo -> fileInfo.getFileId()).collect(Collectors.toList());
            return files;
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public void unDelete(String uuid) throws MongoWrapperException {
        try {
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid, true);
            if (fileInfo != null) {
                minIOWrapper.restoreByFileId(fileInfo.getFileId());
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public void erase(String uuid) throws MongoWrapperException {
        try {
            log.info("erase " + uuid);
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid, true);
            if (fileInfo != null) {
                log.info("fileInfo uuid: " + fileInfo.toString());
                minIOWrapper.removeByFileId(fileInfo.getFileId(), false);
            } else {
                fileInfo = minIOWrapper.getFileInfoByFileId(uuid, true);
                if (fileInfo != null) {
                    log.info("fileInfo file_id: " + fileInfo.toString());
                    minIOWrapper.removeByFileId(fileInfo.getFileId(), false);
                }
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public void deleteByPath(String filepath) throws MongoWrapperException {
        try {
            String[] splittedPath = splitPath(filepath);
            minIOWrapper.deleteByPathAndFileName(splittedPath[0], splittedPath[1], codiceAzienda);
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public void delete(String uuid) throws MongoWrapperException {
        try {
            minIOWrapper.deleteByFileUuid(uuid);
            minIOWrapper.deleteByFileId(uuid);
        } catch (Throwable ex) {
            log.error("errore eliminazione file: ", ex);
            throw new MongoWrapperException("errore eliminazione file: ", ex);
        }
    }

    @Override
    public boolean isTemp() {
        return codiceAzienda.toLowerCase().endsWith("t");
    }

    @Override
    public List<String> getDirFilesAndFolders(String dirname) throws MongoWrapperException {
        return getDirFiles(dirname, false, true);
    }

    @Override
    public List<String> getDirFiles(String dirname) throws MongoWrapperException {
        return getDirFiles(dirname, false, false);
    }

    public List<String> getDirFiles(String dirname, boolean includeDeleted, boolean includeSubDir) throws MongoWrapperException {
        try {
            List<String> filesFromMinIO = collectionToStreamNullSafe(minIOWrapper.getFilesInPath(dirname, includeDeleted, includeSubDir, codiceAzienda)).map(fileInfo -> fileInfo.getMongoUuid()).collect(Collectors.toList());
            return filesFromMinIO;
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public String getMD5ByPath(String filepath) throws MongoWrapperException {
        String[] splittedPath = splitPath(filepath);
        try {
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByPathAndFileName(splittedPath[0], splittedPath[1], codiceAzienda);
            if (fileInfo != null) {
                return fileInfo.getMd5();
            }
            return null;
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public String getFidByPath(String filepath) throws MongoWrapperException {
        String[] splittedPath = splitPath(filepath);
        try {
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByPathAndFileName(splittedPath[0], splittedPath[1], codiceAzienda);
            if (fileInfo != null) {
                return fileInfo.getMongoUuid();
            }
            return null;
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public String getMD5ByUuid(String uuid) throws MongoWrapperException {
        try {
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid);
            if (fileInfo != null) {
                return fileInfo.getMd5();
            } else {
                fileInfo = minIOWrapper.getFileInfoByFileId(uuid);
                if (fileInfo != null) {
                    return fileInfo.getMd5();
                }
                return null;
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public InputStream getByPath(String filepath) throws MongoWrapperException {
        String[] splittedPath = splitPath(filepath);
        try {
            InputStream file = minIOWrapper.getByPathAndFileName(splittedPath[0], splittedPath[1], codiceAzienda);
            if (file != null) {
                return file;
            }
            return file;
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public InputStream get(String uuid) throws MongoWrapperException {
        try {
            InputStream file = minIOWrapper.getByUuid(uuid);
            if (file != null) {
                return file;
            } else {
                file = minIOWrapper.getByFileId(uuid);
                if (file != null) {
                    return file;
                }
                return null;
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    /**
     * Carica il file su minIO e torna il fileId caricato
     *
     * @param f
     * @param uuid
     * @param filename
     * @param dirname
     * @param creator
     * @param category
     * @param overwrite
     * @return
     * @throws IOException
     * @throws MongoWrapperException
     */
    @Override
    public String put(File f, String uuid, String filename, String dirname, String creator, String category, boolean overwrite) throws IOException, MongoWrapperException {
        return put(f, null, uuid, filename, creator, category, dirname, overwrite);
    }

    /**
     * Carica il file su minIO e torna il fileId caricato
     *
     * @param is
     * @param uuid
     * @param filename
     * @param creator
     * @param category
     * @param dirname
     * @param overwrite
     * @return
     * @throws IOException
     * @throws MongoWrapperException
     */
    @Override
    public String put(InputStream is, String uuid, String filename, String creator, String category, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        return put(null, is, uuid, filename, creator, category, dirname, overwrite);
    }

    private String put(File f, InputStream is, String uuid, String filename, String creator, String category, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        try {
            Map<String, Object> metadata = null;
            if (creator != null || category != null) {
                metadata = new HashMap<>();
            }
            if (creator != null) {
                metadata.put("creator", creator);
            }
            if (category != null) {
                metadata.put("category", category);
            }
//            String mongoUuid = super.getFidByPath(StringUtils.trimTrailingCharacter(StringUtils.cleanPath(dirname), '/') + "/" + filename);
//            if (StringUtils.hasText(mongoUuid)) {
//                if (!overwrite) {
//                    filename = minIOWrapper.getFileNameForNotOverwrite(filename);
//                }
//            }
            MinIOWrapperFileInfo fileInfo;
            if (f != null) {
                fileInfo = minIOWrapper.putWithBucket(f, codiceAzienda, dirname, filename, metadata, overwrite, uuid, null);
            } else {
                fileInfo = minIOWrapper.putWithBucket(is, codiceAzienda, dirname, filename, metadata, overwrite, uuid, null);
            }
//            if (overwrite) {
//                super.delete(mongoUuid);
//            }
            return fileInfo.getMongoUuid();
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public String put(File f, String filename, String dirname, String creator, String category, boolean overwrite) throws IOException, MongoWrapperException {
        String uuid = UUID.randomUUID().toString();
        return put(f, uuid, filename, dirname, creator, category, overwrite);
    }

    @Override
    public String put(InputStream is, String filename, String dirname, String creator, String category, boolean overwrite) throws IOException, MongoWrapperException {
        String uuid = UUID.randomUUID().toString();
        return put(is, uuid, filename, creator, category, dirname, overwrite);
    }

    @Override
    public String put(File f, String uuid, String filename, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        return put(f, uuid, filename, dirname, null, null, overwrite);
    }

    @Override
    public String put(InputStream is, String uuid, String filename, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        return put(is, uuid, filename, null, null, dirname, overwrite);
    }

    @Override
    public String put(File f, String filename, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        String uuid = UUID.randomUUID().toString();
        return put(f, uuid, filename, dirname, null, null, overwrite);
    }

    @Override
    public String put(InputStream is, String filename, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        String uuid = UUID.randomUUID().toString();
        return put(is, uuid, filename, null, null, dirname, overwrite);
    }

    @Override
    public Map<String, Object> getMetadataByUuid(String uuid) throws MongoWrapperException {
        try {
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid);
            if (fileInfo != null) {
                return fileInfo.getMetadata();
            } else {
                fileInfo = minIOWrapper.getFileInfoByFileId(uuid);
                if (fileInfo != null) {
                    return fileInfo.getMetadata();
                } else {
                    return null;
                }
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public Map<String, Object> getMetadataByPath(String path) throws MongoWrapperException {
        String[] splittedPath = splitPath(path);
        try {
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByPathAndFileName(splittedPath[0], splittedPath[1], codiceAzienda);
            if (fileInfo != null) {
                return fileInfo.getMetadata();
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    private <T extends Object> Stream<T> collectionToStreamNullSafe(Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }
}
