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
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
public class MongoWrapperMinIO extends MongoWrapper {
    
    private final MinIOWrapper minIOWrapper;
    private Integer codiceAzienda;
    
    public MongoWrapperMinIO(String mongoUri, String minIODBDriver, String minIODBUrl, String minIODBUsername, String minIODBPassword, Integer codiceAzienda, ObjectMapper objectMapper) throws UnknownHostException, MongoException, MongoWrapperException {
        super(mongoUri);
        this.codiceAzienda = codiceAzienda;
        if (objectMapper != null) {
            minIOWrapper = new MinIOWrapper(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, objectMapper);
        } else {
            minIOWrapper = new MinIOWrapper(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword);
        }
    }
    
    public static void main(String[] args) {
        test();
    }
    
    public static void test() {
        String[] splitPath1 = splitPath("/gdm/dg/asdfgasfa/asfasd/filename.etx");
        String[] splitPath2 = splitPath("/gdm/dg/asdfgasfa/filename");
        String[] splitPath3 = splitPath("/gdm/dg/asdfgasfa/asfasd/");
        String[] splitPath4 = splitPath("/gdm/dg/asdfgasfa/asfasd");
        System.out.println(Arrays.toString(splitPath1));
        System.out.println(Arrays.toString(splitPath2));
        System.out.println(Arrays.toString(splitPath3));
        System.out.println(Arrays.toString(splitPath4));
    }
    
    private static String[] splitPath(String pathWithFileName) {
        pathWithFileName = StringUtils.cleanPath(pathWithFileName);
        String filename = StringUtils.getFilename(pathWithFileName);
        String path = StringUtils.trimTrailingCharacter(pathWithFileName.substring(0, pathWithFileName.length() - filename.length()), '/');
        return new String[] {path, filename};
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
                }            }
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
                return false;
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
                } else {
                    super.rename(uuid, newName);
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
            minIOWrapper.delFilesInPath(dirname, true);
            super.delDirFilesAndFolders(dirname);
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public void delDirFiles(String dirname) throws MongoWrapperException {
        try {
            // non sapendo se sono stati spostati tutti i  file della cartella, cancello da entrambe le parti
            minIOWrapper.delFilesInPath(dirname, false);
            super.delDirFiles(dirname);
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
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
    public void unDelete(String uuid) throws MongoWrapperException {
        try {            
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid, true);
            
            if (fileInfo != null) {
                minIOWrapper.restoreByFileId(fileInfo.getFileId());
            } else {
                super.unDelete(uuid);
            }
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public void erase(String uuid) throws MongoWrapperException {
        try {            
            MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByUuid(uuid);
            if (fileInfo != null) {
                minIOWrapper.removeByFileId(fileInfo.getFileId(), false);
            } else {
                fileInfo = minIOWrapper.getFileInfoByFileId(uuid);
                if (fileInfo != null) {
                    minIOWrapper.removeByFileId(fileInfo.getFileId(), false);                    
                } else {
                    super.erase(uuid);
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
            super.deleteByPath(filepath);
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public void delete(String uuid) throws MongoWrapperException {
        try {
            // siccome non so se il file è su minio o su mongo e se l'uuid passato è l'uuid di mongo oppure il fileId(nel caso è stato caricato direttamente su minIO), 
            // provo a cancellare da tutte le parti
            minIOWrapper.deleteByFileUuid(uuid);
            minIOWrapper.deleteByFileId(uuid);
            super.delete(uuid); 
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
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
            List<String> filesFromMinIO = collectionToStreamNullSafe(minIOWrapper.getFilesInPath(dirname, includeDeleted, includeSubDir)).map(fileInfo -> fileInfo.getFileId()).collect(Collectors.toList());
            List<String> filesFromMongo = super.getDirFilesAndFolders(dirname);
            if (filesFromMongo != null) {
                filesFromMinIO.addAll(filesFromMongo);
            }
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
                return fileInfo.getFileId();
            } else {
                fileInfo = minIOWrapper.getFileInfoByFileId(uuid);
                if (fileInfo != null) {
                    return fileInfo.getFileId();
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
        Map<String, Object> metadata = null;
        try {
            if (creator != null || category != null) {
                metadata = new HashMap<>();
            }
            if (creator != null) {
                metadata.put("creator", creator);
            }
            if (category != null) {
                metadata.put("category", category);
            }
            MinIOWrapperFileInfo fileInfo = minIOWrapper.put(f, codiceAzienda, dirname, filename, metadata, overwrite, uuid);
            return fileInfo.getMongoUuid();
        } catch (Exception ex) {
            throw new MongoWrapperException("errore", ex);
        }
    }

    @Override
    public String put(InputStream is, String uuid, String filename, String creator, String category, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        Map<String, Object> metadata = null;
        try {
            if (creator != null || category != null) {
                metadata = new HashMap<>();
            }
            if (creator != null) {
                metadata.put("creator", creator);
            }
            if (category != null) {
                metadata.put("category", category);
            }
            MinIOWrapperFileInfo fileInfo = minIOWrapper.put(is, codiceAzienda, dirname, filename, metadata, overwrite, uuid);
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
        return put(is, uuid, filename, dirname, creator, category, overwrite);
    }

    @Override
    public String put(File f, String uuid, String filename, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        return put(f, uuid, filename, dirname, null, null, overwrite); 
    }

    @Override
    public String put(InputStream is, String uuid, String filename, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        return put(is, uuid, filename, dirname, null, null, overwrite); 
    }

    @Override
    public String put(File f, String filename, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        String uuid = UUID.randomUUID().toString();
        return put(f, uuid, filename, dirname, null, null, overwrite); 
    }

    @Override
    public String put(InputStream is, String filename, String dirname, boolean overwrite) throws IOException, MongoWrapperException {
        String uuid = UUID.randomUUID().toString();
        return put(is, uuid, filename, dirname, null, null, overwrite); 
    }

    private <T extends Object> Stream<T> collectionToStreamNullSafe(Collection<T> collection) {
    return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
    }
    
}
