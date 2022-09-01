package it.bologna.ausl.model.entities.firma;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;

/**
 *
 * @author gdm
 */
@Entity
@Table(name = "domini_aruba", catalog = "internauta", schema = "firma")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Cacheable(false)
@DynamicUpdate
public class DominioAruba implements Serializable {

    private static final long serialVersionUID = 1L;

    public static enum DominiAruba {
        frAUSLBO, frAUSLPR, frAOPR, frASLImola, frAUSLFE, firma
    }

    @Id
    @Basic(optional = false)
    @Column(name = "codice", columnDefinition = "text")
    private String codice;

    @Basic(optional = false)
    @Column(name = "nome", columnDefinition = "text")
    private String nome;

    public DominioAruba() {
    }

    public DominioAruba(String codice) {
        this.codice = codice;
    }

    public DominioAruba(String codice, String nome) {
        this.codice = codice;
        this.nome = nome;
    }

    public String getCodice() {
        return codice;
    }

    public void setCodice(String codice) {
        this.codice = codice;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (codice != null ? codice.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DominioAruba)) {
            return false;
        }
        DominioAruba other = (DominioAruba) object;
        return !((this.codice == null && other.codice != null) || (this.codice != null && !this.codice.equals(other.codice)));
    }

    @Override
    public String toString() {
        return getClass().getCanonicalName() + "[ codice=" + codice + " ]";
    }
}
