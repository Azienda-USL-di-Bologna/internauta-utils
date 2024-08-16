package it.bologna.ausl.riversamento.builder;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;

/**
 *
 * @author utente
 */


public class IdentityFile {
    
    private String fileName;
    private String uuidMongo;
    private String id;
    private String fileBase64;
    private String hash;
    private String formatType;
    private String mime;


    public IdentityFile() {}
    
    
    public IdentityFile(String fileName, String uuidMongo, String hash, String formatType, String mime) {
        this.fileName = fileName;
        this.uuidMongo = uuidMongo;
        this.fileBase64 = null;
        this.id = generateID();
        this.hash = hash;
        this.formatType = formatType;
        this.mime = mime;
    }
    
    // IdentityFile passando direttamente Base64
    public IdentityFile(String fileName, String base64, String formatType, String mime) {
        this.fileName = fileName;
        this.uuidMongo = null;
        this.fileBase64 = base64;
        this.id = generateID();
        this.hash = "hash";
        this.formatType = formatType;
        this.mime = mime;
    }
    
    public IdentityFile(String fileName, File file, String id, String hash, String formatType, String mime) throws FileNotFoundException, IOException {
        this.fileName = fileName;
        DataInputStream datais=null;
        FileInputStream fis= null;
        try{
            fis = new FileInputStream(file);
            datais = new DataInputStream(fis);

            byte[] bytes = new byte[(int)file.length()];
            datais.readFully(bytes);
            datais.close();
            this.fileBase64 = Base64.encodeBase64String(bytes);
            this.id = id;
            this.hash = hash;
            this.formatType = formatType;
            this.mime = mime;
        }
        finally{
            try{
                fis.close();
            }
            catch(Exception ex){
            }
            
            try{
                datais.close();
            }
            catch(Exception ex){
            }
        }
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    public String getFileName() {
        return fileName;
    }

    public String getUuidMongo() {
        return uuidMongo;
    }

    public String getBase64() {
        return fileBase64;
    }

    public String getId() {
        return id;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setUuidMongo(String uuidMongo) {
        this.uuidMongo = uuidMongo;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    private String generateRandomID(int lenght){
        char[] chars = "abcdefghijklmnopqrstuvwxyzABSDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
        Random r = new Random();
        char[] id = new char[lenght];
        for (int i = 0;  i < id.length;  i++) {
            id[i] = chars[r.nextInt(chars.length)];
        }
        return new String(id);
    }
    
    public String generateID(){
         return String.valueOf(System.currentTimeMillis()) +"-"+ generateRandomID(10);
    }

    public String getFileBase64() {
        return fileBase64;
    }

    public void setFileBase64(String fileBase64) {
        this.fileBase64 = fileBase64;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getFormatType() {
        return formatType;
    }

    public void setFormatType(String formatType) {
        this.formatType = formatType;
    }
       
    public JSONObject getJSON(){
        JSONObject json = new JSONObject();
        json.put("fileName", fileName);
        
        if(uuidMongo != null){
            json.put("uuidMongo", uuidMongo);
        }else{
            json.put("uuidMongo", -1);
        }
        
        json.put("id", id);
        
        if(fileBase64 != null){
            json.put("fileBase64", fileBase64);
        }else{
            json.put("fileBase64", null);
        }
                
        json.put("hash", hash);
        json.put("formatType", formatType);
        json.put("mime", mime);
        return json;
    }
    
    public static IdentityFile parse(JSONObject json){
        IdentityFile result = new IdentityFile();
        
        result.setFileBase64((String) json.get("fileBase64"));
        result.setFileName((String) json.get("fileName"));
        result.setFormatType((String) json.get("formatType"));
        result.setHash((String) json.get("hash"));
        result.setId((String) json.get("id"));
        result.setMime((String) json.get("mime"));
        result.setUuidMongo((String) json.get("uuidMongo"));
        
        return result;
    }
}
