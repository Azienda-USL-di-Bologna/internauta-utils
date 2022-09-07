package it.bologna.ausl.estrattore;

import java.lang.reflect.Field;

/**
 *
 * @author Giuseppe De Marco (gdm)
 */
public class ExtractorResult {
    private String fileName;
    private String mimeType;
    private long size;
    private String hash;
    private String path;
    private int level;
    private String padre;
    private String antenati;
    private boolean isExtractable;

    public ExtractorResult(String fileName, String mimeType, long size, String hash, String path, int level, String padre,String antenati) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.size = size;
        this.hash = hash;
        this.path = path;
        this.level = level;
        this.padre = padre;
        this.antenati = antenati;
    }
    
    public ExtractorResult(String fileName, String mimeType, long size, String hash, String path, int level, String padre,String antenati, boolean isExtractable) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.size = size;
        this.hash = hash;
        this.path = path;
        this.level = level;
        this.padre = padre;
        this.antenati = antenati;
        this.isExtractable = isExtractable;
    }

    public String getAntenati() {
        return antenati;
    }

    public void setAntenati(String antenati) {
        this.antenati = antenati;
    }

    public String getPadre() {
        return padre;
    }

    public void setPadre(String padre) {
        this.padre = padre;
    }

    
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean getIsExtractable() {
        return isExtractable;
    }

    public void setIsExtractable(boolean isExtractable) {
        this.isExtractable = isExtractable;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Field[] fields = getClass().getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            try {
                String fieldValue = null;
                try {
                    fieldValue = getClass().getMethod("get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1)).invoke(this).toString();
                }
                catch (Exception ex) {
                }
                if (fieldValue != null) {
                    sb.append(fieldName).append(": ").append(fieldValue).append("; ");
                }
            }
            catch (Exception ex) {
            }
        }
        if (sb.length() > 0)
            sb.delete(sb.length() - 2, sb.length());
        return sb.toString();
    }
}
