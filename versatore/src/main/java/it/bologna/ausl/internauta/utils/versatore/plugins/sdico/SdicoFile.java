package it.bologna.ausl.internauta.utils.versatore.plugins.sdico;

import java.io.File;
import java.io.InputStream;

/**
 *
 * @author Andrea
 */
public class SdicoFile {
    private File file;
    private String fileName;
    private String id;
    private InputStream inputStream;
    private String mime = "binary/octet-stream";

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    public SdicoFile() {
        id = "";
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
