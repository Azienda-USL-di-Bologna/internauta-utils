package it.bologna.ausl.minio.manager;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 *
 * @author gdm
 */
public class MinIOWrapperFileInfo {
    private Integer tableId;
    private String fileId;
    private String path;
    private String fileName;
    private Integer size;
    private String md5;
    private Integer serverId;
    private String generatedUuid;
    private Integer codiceAzienda;
    private String bucketName;
    private Map<String, Object> metadata;
    private Boolean deleted;
    private ZonedDateTime uploadDate;
    private ZonedDateTime deleteDate;
    private ZonedDateTime modifiedDate;

    public MinIOWrapperFileInfo() {
    }

    public MinIOWrapperFileInfo(Integer tableId, String fileId, String path, String fileName, Integer size, String md5, Integer serverId, String generatedUuid, Integer codiceAzienda, String bucketName, Map<String, Object> metadata, Boolean deleted, ZonedDateTime uploadDate, ZonedDateTime modifiedDate, ZonedDateTime deleteDate) {
        this.tableId = tableId;
        this.fileId = fileId;
        this.path = path;
        this.fileName = fileName;
        this.size = size;
        this.md5 = md5;
        this.serverId = serverId;
        this.generatedUuid = generatedUuid;
        this.codiceAzienda = codiceAzienda;
        this.bucketName = bucketName;
        this.metadata = metadata;
        this.deleted = deleted;
        this.uploadDate = uploadDate;
        this.deleteDate = deleteDate;
        this.modifiedDate = modifiedDate;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Integer getTableId() {
        return tableId;
    }

    public void setTableId(Integer tableId) {
        this.tableId = tableId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Integer getServerId() {
        return serverId;
    }

    public void setServerId(Integer serverId) {
        this.serverId = serverId;
    }

    public String getGeneratedUuid() {
        return generatedUuid;
    }

    public void setGeneratedUuid(String generatedUuid) {
        this.generatedUuid = generatedUuid;
    }

    public Integer getCodiceAzienda() {
        return codiceAzienda;
    }

    public void setCodiceAzienda(Integer codiceAzienda) {
        this.codiceAzienda = codiceAzienda;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public ZonedDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(ZonedDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public ZonedDateTime getDeleteDate() {
        return deleteDate;
    }

    public void setDeleteDate(ZonedDateTime deleteDate) {
        this.deleteDate = deleteDate;
    }

    public ZonedDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(ZonedDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @Override
    public String toString() {
        Field[] declaredFields = getClass().getDeclaredFields();
        StringBuilder sb = new StringBuilder();
        for (Field f: declaredFields) {
            try {
                sb.append(String.format("%s: %s, ", f.getName(), f.get(this)));
            } catch (Throwable t) {
            }
        }
        return sb.toString();
    }
}
