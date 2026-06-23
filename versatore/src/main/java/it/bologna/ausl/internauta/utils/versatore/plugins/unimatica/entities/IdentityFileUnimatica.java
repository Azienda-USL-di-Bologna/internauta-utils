package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities;

import java.io.File;
import it.bologna.ausl.riversamento.builder.IdentityFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.json.simple.JSONObject;

/**
 * Classe che eredità IdentiFile che mi serve per portarsi dietro l'id dell'allegato originale
 * @author boria
 */
public class IdentityFileUnimatica extends IdentityFile {

    private Integer idAllegato;

    public IdentityFileUnimatica() {
        super();
    }

    public IdentityFileUnimatica(String fileName, String uuidMongo, String hash,
        String formatType, String mime, Integer idAllegato) {
        super(fileName, uuidMongo, hash, formatType, mime);
        this.idAllegato = idAllegato;
    }

    public IdentityFileUnimatica(String fileName, String base64,
        String formatType, String mime, Integer idAllegato) {
        super(fileName, base64, formatType, mime);
        this.idAllegato = idAllegato;
    }

    public IdentityFileUnimatica(String fileName, File file, String id, String hash,
        String formatType, String mime, Integer idAllegato)
        throws FileNotFoundException, IOException {
        super(fileName, file, id, hash, formatType, mime);
        this.idAllegato = idAllegato;
    }

    public Integer getIdAllegato() {
        return idAllegato;
    }

    public void setIdAllegato(Integer idAllegato) {
        this.idAllegato = idAllegato;
    }

    @Override
    public JSONObject getJSON() {
        JSONObject json = super.getJSON();
        json.put("idAllegato", idAllegato);
        return json;
    }

    public static IdentityFileUnimatica parse(JSONObject json) {
        IdentityFileUnimatica result = new IdentityFileUnimatica();

        result.setFileBase64((String) json.get("fileBase64"));
        result.setFileName((String) json.get("fileName"));
        result.setFormatType((String) json.get("formatType"));
        result.setHash((String) json.get("hash"));
        result.setId((String) json.get("id"));
        result.setMime((String) json.get("mime"));
        result.setUuidMongo((String) json.get("uuidMongo"));
        result.setIdAllegato(((Number) json.get("idAllegato")).intValue());

        return result;
    }

}
