package it.bologna.ausl.model.entities.masterjobs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.nextsw.common.data.annotations.GenerateProjections;
import java.io.Serializable;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.DynamicUpdate;

/**
 *
 * @author gdm
 */
//@TypeDefs({
//    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
//})
@Entity
@Table(name = "objects_status", catalog = "internauta", schema = "masterjobs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
@GenerateProjections({})
@DynamicUpdate
public class ObjectStatus implements Serializable {

    public static enum ObjectState {
        IDLE,
        PENDING,
        ERROR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "object_id")
    private String objectId;
    
    @Basic(optional = true)
    @Column(name = "object_type")
    private String objectType;
    
    @Basic(optional = true)
    @Column(name = "app")
    private String app;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "state")
    private String state;

    public ObjectStatus() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public ObjectState getState() {
        if (state != null) {
            return ObjectState.valueOf(state);
        } else {
            return null;
        }
    }

    public void setState(ObjectState state) {
        if (state != null) {
            this.state = state.toString();
        } else {
            this.state = null;
        }
    }
    
}
