package it.bologna.ausl.internauta.utils.bds.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Gus
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoriaPermessiStoredProcedure implements Serializable {
    private String ambito;
    private String tipo;
    private List<PermessoStoredProcedure> permessi;

    public CategoriaPermessiStoredProcedure() {
    }
    
    public CategoriaPermessiStoredProcedure(String ambito, String tipo, List<PermessoStoredProcedure> permessi) {
        this.ambito = ambito;
        this.tipo = tipo;
        this.permessi = permessi;
    }

    public String getAmbito() {
        return ambito;
    }

    public void setAmbito(String ambito) {
        this.ambito = ambito;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public List<PermessoStoredProcedure> getPermessi() {
        return permessi;
    }

    public void setPermessi(List<PermessoStoredProcedure> permessi) {
        this.permessi = permessi;
    }
        
}
