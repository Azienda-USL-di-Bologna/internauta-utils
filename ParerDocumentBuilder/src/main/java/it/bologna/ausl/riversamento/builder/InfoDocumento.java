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


public class InfoDocumento {
    
    private String guid;
    private String tipo;
    private String autore;
    private String descrizione;
    private String uuidMongo;
    private String tipoStruttura;
    private String tipoDocumentoSecondario;


    public InfoDocumento() {}
    
    
    public InfoDocumento(String guid, String tipo, String autore, String descrizione, String uuidMongo, String tipoStruttura, String tipoDocumentoSecondario) {
        this.guid = guid;
        this.tipo = tipo;
        this.autore = autore;
        this.descrizione = descrizione; 
        this.uuidMongo = uuidMongo;
        this.tipoStruttura = tipoStruttura;
        this.tipoDocumentoSecondario = tipoDocumentoSecondario;
    }
    
    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getAutore() {
        return autore;
    }

    public void setAutore(String autore) {
        this.autore = autore;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getUuidMongo() {
        return uuidMongo;
    }

    public void setUuidMongo(String uuidMongo) {
        this.uuidMongo = uuidMongo;
    }

    public String getTipoStruttura() {
        return tipoStruttura;
    }

    public void setTipoStruttura(String tipoStruttura) {
        this.tipoStruttura = tipoStruttura;
    }

    public String getTipoDocumentoSecondario() {
        return tipoDocumentoSecondario;
    }

    public void setTipoDocumentoSecondario(String tipoDocumentoSecondario) {
        this.tipoDocumentoSecondario = tipoDocumentoSecondario;
    }

             
    public JSONObject getJSON(){
        JSONObject json = new JSONObject();
                
        if(guid != null && !guid.equals("")){
            json.put("guid", guid);
        }else{
            json.put("guid", null);
        }
        
        if(tipo != null && !tipo.equals("")){
            json.put("tipo", tipo);
        }else{
            json.put("tipo", null);
        }
        
        if(autore != null && !autore.equals("")){
            json.put("autore", autore);
        }else{
            json.put("autore", null);
        }
        
        if(descrizione != null && !descrizione.equals("")){
            json.put("descrizione", descrizione);
        }else{
            json.put("descrizione", null);
        }
        
        if(uuidMongo != null && !uuidMongo.equals("")){
            json.put("uuidMongo", uuidMongo);
        }else{
            json.put("uuidMongo", null);
        }
        
        if(tipoStruttura != null && !tipoStruttura.equals("")){
            json.put("tipoStruttura", tipoStruttura);
        }else{
            json.put("tipoStruttura", null);
        }
        
        if(tipoDocumentoSecondario != null && !tipoDocumentoSecondario.equals("")){
            json.put("tipoDocumentoSecondario", tipoDocumentoSecondario);
        }else{
            json.put("tipoDocumentoSecondario", null);
        }
        
        return json;
    }
    
    public static InfoDocumento parse(JSONObject json){
        InfoDocumento result = new InfoDocumento();
        
        result.setGuid((String) json.get("guid"));
        result.setTipo((String) json.get("tipo"));
        result.setAutore((String) json.get("autore"));
        result.setDescrizione((String) json.get("descrizione"));
        result.setUuidMongo((String) json.get("uuidMongo"));
        result.setTipoStruttura((String) json.get("tipoStruttura"));
        result.setTipoDocumentoSecondario((String) json.get("tipoDocumentoSecondario"));
        
        return result;
    }
}
