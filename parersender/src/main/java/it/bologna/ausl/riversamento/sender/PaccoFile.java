package it.bologna.ausl.riversamento.sender;

import java.io.File;
import java.io.InputStream;
import java.util.UUID;

/**
 *
 * @author andrea
 */
public class PaccoFile {

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

    public PaccoFile() {
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
