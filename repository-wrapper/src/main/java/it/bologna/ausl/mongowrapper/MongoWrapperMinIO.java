package it.bologna.ausl.mongowrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;
import org.sql2o.Connection;

/**
 *
 * @author gdm
 */
public class MongoWrapperMinIO extends MongoWrapper {

    private final MinIOWrapper minIOWrapper;
    private String codiceAzienda;
    private ThreadLocal<Connection> conn = new ThreadLocal<>();
    private static final Logger log = LogManager.getLogger(MongoWrapperMinIO.class);

    public MongoWrapperMinIO(String mongoUri, String minIODBDriver, String minIODBUrl, String minIODBUsername, String minIODBPassword,
            String codiceAzienda, Integer maxPoolSize, ObjectMapper objectMapper) throws UnknownHostException, MongoException, MongoWrapperException {
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

    private boolean lockByUuid(String mongoUuid) throws MongoWrapperException {
        return lock(mongoUuid, null, null);
    }

    private boolean lockByPath(String mongoPath) throws MongoWrapperException {
        String pathWithSlash = mongoPath;
        if (!mongoPath.startsWith("/")) {
            pathWithSlash = "/" + pathWithSlash;
        }
        return lock(null, pathWithSlash, null);
    }

    private boolean lockByDir(String mongoDir) throws MongoWrapperException {
        String dirNameWithSlash = mongoDir;
        if (!mongoDir.startsWith("/")) {
            dirNameWithSlash = "/" + dirNameWithSlash;
        }
        return lock(null, null, dirNameWithSlash);
    }

    private boolean lock(String mongoUuid, String mongoPath, String mongoDir) throws MongoWrapperException {
//        if (true) return false;
        if (conn.get() != null) {
            throw new MongoWrapperException("oggetto conn non è null, ma dovrebbe esserlo");
        }
        conn.set((Connection) MinIOWrapper.getSql2oConnection().beginTransaction());
        log.info("opening connection " + conn.toString() + " ...");
        log.info("mongoUuid " + mongoUuid);
        log.info("mongoPath " + mongoPath);
        log.info("mongoDir " + mongoDir);
        String query;
        String id;
        if (mongoUuid != null) {
            query = "select id from repo.trasferimenti where mongo_uuid = :id for update";
            id = mongoUuid;
        } else if (mongoPath != null) {
            query = "select id from repo.trasferimenti where mongo_path = :id for update";
            id = mongoPath;
        } else {
            id = mongoDir;
            query = "select id from repo.trasferimenti where mongo_path like :id || '%' for update";
        }
        List<Map<String, Object>> res = conn.get().createQuery(query).addParameter("id", id).executeAndFetchTable().asList();
        return res != null && !res.isEmpty();
    }

    private void unlock() {
//        if (true) return;
//        System.out.println("conn: " + conn.get());
        if (conn.get() != null) {
            log.info("closing connection " + conn.get().toString() + " with rollback ...");
            conn.get().close();
            conn.set(null);
        }
    }

    public MinIOWrapper getMinIOWrapper() {
        return minIOWrapper;
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
            } else {
                return super.getUploadDateByPath(filePath);
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
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
                } else {
                    return super.getUploadDateByUuid(uuid);
                }
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
                } else {
                    return super.getSizeByUuid(uuid);
                }
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
            } else {
                return super.getSizeByPath(filepath);
            }
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
                } else {
                    return super.getFilePathByUuid(uuid);
                }
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
                } else {
                    return super.getFileName(uuid);
                }
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
                if (fileInfo != null) {
                    return true;
                } else {
                    return super.existsObjectbyUUID(uuid);
                }
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
            if (fileInfo != null) {
                return true;
            } else {
                return super.existsObjectbyPath(filepath);
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public void move(String uuid, String newFilepath) throws MongoWrapperException {
        try {
            lockByUuid(uuid);
            String[] splittedPath = splitPath(newFilepath);
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid);
            if (fileInfo != null) {
                minIOWrapper.renameByFileId(fileInfo.getFileId(), splittedPath[0], splittedPath[1]);
            } else {
                fileInfo = minIOWrapper.getFileInfoByFileId(uuid);
                if (fileInfo != null) {
                    minIOWrapper.renameByFileId(fileInfo.getFileId(), splittedPath[0], splittedPath[1]);
                } else {
                    super.move(uuid, newFilepath);
                }
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        } finally {
            unlock();
        }
    }

    @Override
    public void rename(String uuid, String newName) throws MongoWrapperException {
        try {
            lockByUuid(uuid);
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid);
            if (fileInfo != null) {
                minIOWrapper.renameByFileId(fileInfo.getFileId(), newName);
            } else {
                fileInfo = minIOWrapper.getFileInfoByFileId(uuid);
                if (fileInfo != null) {
                    minIOWrapper.renameByFileId(fileInfo.getFileId(), newName);
                } else {
                    super.rename(uuid, newName);
                }
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        } finally {
            unlock();
        }
    }

    @Override
    public void delDirFilesAndFolders(String dirname) throws MongoWrapperException {
        try {
            lockByDir(dirname);
            // non sapendo se sono stati spostati tutti i  file della cartella, cancello da entrambe le parti
            minIOWrapper.delFilesInPath(dirname, true, codiceAzienda);
            super.delDirFilesAndFolders(dirname);
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        } finally {
            unlock();
        }
    }

    @Override
    public void delDirFiles(String dirname) throws MongoWrapperException {
        try {
            lockByDir(dirname);
            // non sapendo se sono stati spostati tutti i  file della cartella, cancello da entrambe le parti
            minIOWrapper.delFilesInPath(dirname, false, codiceAzienda);
            super.delDirFiles(dirname);
        } catch (Throwable ex) {
            ex.printStackTrace();
            log.error("errore", ex);
            throw new MongoWrapperException("errore", ex);
        } finally {
            unlock();
        }
    }

    @Override
    public List<String> getFilesInfo(List<String> uuids, boolean includeDeleted) throws MongoWrapperException {
        try {
            List<String> res = new ArrayList();
            List<String> notFoundInMinIO = new ArrayList();
            for (String uuid : uuids) {
                MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid, includeDeleted);
                if (fileInfo != null) {
                    res.add(fileInfo.toString());
                } else {
                    fileInfo = minIOWrapper.getFileInfoByFileId(uuid, includeDeleted);
                    if (fileInfo != null) {
                        res.add(fileInfo.toString());
                    } else {
                        notFoundInMinIO.add(uuid);
                    }
                }
            }
            if (!notFoundInMinIO.isEmpty()) {
                List<String> filesInfo = super.getFilesInfo(notFoundInMinIO, includeDeleted);
                if (filesInfo != null) {
                    res.addAll(filesInfo);
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
//            List<String> deletedFromMinIO = minIOWrapper.getDeleted(codiceAzienda, null).stream().map(fileInfo -> fileInfo.getFileId()).collect(Collectors.toList());
            List<String> deletedFromMinIO = collectionToStreamNullSafe(minIOWrapper.getDeleted(codiceAzienda, null)).map(fileInfo -> fileInfo.getFileId()).collect(Collectors.toList());
            List<String> deletedFromMongo = super.getDeleted();
            if (deletedFromMongo != null) {
                deletedFromMinIO.addAll(deletedFromMongo);
            }
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
            List<String> deletedFromMongo = super.getDeletedLessThan(timeInMillis);
            if (deletedFromMongo != null) {
                deletedFromMinIO.addAll(deletedFromMongo);
            }
            return deletedFromMinIO;
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public List<String> getFilesLessThan(ZonedDateTime time) throws MongoWrapperException {
        try {
            List<String> files = collectionToStreamNullSafe(minIOWrapper.getFilesLessThan(codiceAzienda, time, true)).map(fileInfo -> fileInfo.getFileId()).collect(Collectors.toList());
            List<String> filesFromMongo = super.getFilesLessThan(time);
            if (filesFromMongo != null) {
                files.addAll(filesFromMongo);
            }
            return files;
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public void unDelete(String uuid) throws MongoWrapperException {
        try {
            lockByUuid(uuid);
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid, true);

            if (fileInfo != null) {
                minIOWrapper.restoreByFileId(fileInfo.getFileId());
            } else {
                super.unDelete(uuid);
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        } finally {
            unlock();
        }
    }

    @Override
    public void erase(String uuid) throws MongoWrapperException {
        try {
            lockByUuid(uuid);
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
                } else {
                    log.info("fileInfo not found");
                    super.erase(uuid);
                }
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        } finally {
            unlock();
        }
    }

    @Override
    public void deleteByPath(String filepath) throws MongoWrapperException {
        try {
            lockByPath(filepath);
            String[] splittedPath = splitPath(filepath);
            minIOWrapper.deleteByPathAndFileName(splittedPath[0], splittedPath[1], codiceAzienda);
            super.deleteByPath(filepath);
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        } finally {
            unlock();
        }
    }

    @Override
    public void delete(String uuid) throws MongoWrapperException {
        try {
            lockByUuid(uuid);
            // siccome non so se il file è su minio o su mongo e se l'uuid passato è l'uuid di mongo oppure il fileId(nel caso è stato caricato direttamente su minIO),
            // provo a cancellare da tutte le parti
            minIOWrapper.deleteByFileUuid(uuid);
            minIOWrapper.deleteByFileId(uuid);
            super.delete(uuid);
        } catch (Throwable ex) {
            log.error("errore eliminazione file: ", ex);
            throw new MongoWrapperException("errore eliminazione file: ", ex);
        } finally {
            unlock();
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
            lockByDir(dirname);
            List<String> filesFromMinIO = collectionToStreamNullSafe(minIOWrapper.getFilesInPath(dirname, includeDeleted, includeSubDir, codiceAzienda)).map(fileInfo -> fileInfo.getMongoUuid()).collect(Collectors.toList());
            List<String> filesFromMongo;
            if (includeSubDir) {
                filesFromMongo = super.getDirFilesAndFolders(dirname);
            } else {
                filesFromMongo = super.getDirFiles(dirname);
            }

            if (filesFromMongo != null) {
                filesFromMinIO.addAll(filesFromMongo);
            }
            return filesFromMinIO;
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        } finally {
            unlock();
        }
    }

    @Override
    public String getMD5ByPath(String filepath) throws MongoWrapperException {
        String[] splittedPath = splitPath(filepath);
        try {
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByPathAndFileName(splittedPath[0], splittedPath[1], codiceAzienda);
            if (fileInfo != null) {
                return fileInfo.getMd5();
            } else {
                return super.getMD5ByPath(filepath);
            }
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
            } else {
                return super.getFidByPath(filepath);
            }
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
                } else {
                    return super.getMD5ByUuid(uuid);
                }
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
            } else {
                return super.getByPath(filepath);
            }
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
                } else {
                    return super.get(uuid);
                }
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
            String mongoUuid = super.getFidByPath(StringUtils.trimTrailingCharacter(StringUtils.cleanPath(dirname), '/') + "/" + filename);
            if (StringUtils.hasText(mongoUuid)) {
                if (!overwrite) {
                    filename = minIOWrapper.getFileNameForNotOverwrite(filename);
                }
            }
            MinIOWrapperFileInfo fileInfo;
            try {
                lockByPath(StringUtils.trimTrailingCharacter(StringUtils.cleanPath(dirname), '/') + "/" + filename);
                if (f != null) {
                    fileInfo = minIOWrapper.put(f, codiceAzienda, dirname, filename, metadata, overwrite, uuid, null);
                } else {
                    fileInfo = minIOWrapper.put(is, codiceAzienda, dirname, filename, metadata, overwrite, uuid, null);
                }
                if (overwrite) {
                    super.delete(mongoUuid);
                }
                return fileInfo.getMongoUuid();
            } finally {
                unlock();
            }
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
                    return super.getMetadataByUuid(uuid);
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
                return super.getMetadataByPath(path);
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
