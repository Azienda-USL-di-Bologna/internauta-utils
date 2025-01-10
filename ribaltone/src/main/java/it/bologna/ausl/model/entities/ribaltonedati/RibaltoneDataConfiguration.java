/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.model.entities.ribaltonedati;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.ribaltone.SpecificData;
import it.nextsw.common.data.annotations.GenerateProjections;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.springframework.beans.factory.annotation.Autowired;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

/**
 *
 * @author Top
 */
@Entity
@Table(name = "configuration", catalog = "internauta", schema = "ribaltone_dati")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@GenerateProjections({})
@DynamicUpdate
public class RibaltoneDataConfiguration {
    
    @Id
    @Column(name = "id")
    String id;
    
    @Column(name = "fonte")
    String fonte;
    
    @Column(name = "cache_operation_to_do")
    String cacheOperationToDo;
    
    
    @Type(JsonBinaryType.class)
    @Column(name = "specifiche", columnDefinition = "jsonb")
    HashMap<String, Object> specifiche;

    public RibaltoneDataConfiguration() {
    }

    public RibaltoneDataConfiguration(String id, String fonte, String cacheOperationToDo, HashMap<String, Object> specifiche) {
        this.id = id;
        this.fonte = fonte;
        this.cacheOperationToDo = cacheOperationToDo;
        this.specifiche = specifiche;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFonte() {
        return fonte;
    }

    public void setFonte(String fonte) {
        this.fonte = fonte;
    }

    public String getCacheOperationToDo() {
        return cacheOperationToDo;
    }

    public void setCacheOperationToDo(String cacheOperationToDo) {
        this.cacheOperationToDo = cacheOperationToDo;
    }

    public Map<String, Object> getSpecifiche() {
        return specifiche;
    }

    public void setSpecifiche(HashMap<String, Object> specifiche) {
        this.specifiche = specifiche;
    }
    
}
