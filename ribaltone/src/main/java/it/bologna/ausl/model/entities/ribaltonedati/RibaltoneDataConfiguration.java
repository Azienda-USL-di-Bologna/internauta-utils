package it.bologna.ausl.model.entities.ribaltonedati;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.nextsw.common.data.annotations.GenerateProjections;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.EntityManager;
import java.util.List;

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

    @Type(JsonBinaryType.class)
    @Column(name = "cacheConfig", columnDefinition = "jsonb")
    HashMap<String, Object> cacheConfig;

    /**
     * Chiavi delle spechiviche che contengono dati sensibili. TO_DO: scegliere quali siano effettivamente
     */
    public enum SpecificheSensibiliKeys {
        classz,
        connessione,
        codiciEntiValidi,
        personeDaSpegnere,
        queryRecuperoDati,
        personeNonSpegnibili,
        progressivoUltimaTrasformazione
    }

    /**
     * Chiavi delle spechiviche che non contengono dati sensibili. TO_DO: scegliere quali siano effettivamente
     */
    public enum SpecificheNonSensibiliKeys {
        codiciEntiValidi,
    }

    public RibaltoneDataConfiguration() {
    }

    public RibaltoneDataConfiguration(String id, String fonte, String cacheOperationToDo, HashMap<String, Object> specifiche, HashMap<String, Object> cacheConfig) {
        this.id = id;
        this.fonte = fonte;
        this.cacheOperationToDo = cacheOperationToDo;
        this.specifiche = specifiche;
        this.cacheConfig = cacheConfig;
    }

    public HashMap<String, Object> getCacheConfig() {
        return cacheConfig;
    }

    public void setCacheConfig(HashMap<String, Object> cacheConfig) {
        this.cacheConfig = cacheConfig;
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
