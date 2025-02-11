package it.bologna.ausl.internauta.utils.ribaltone.pluginutils;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.GruSpecificData;
import java.util.List;

/**
 *
 * @author Top
 */

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "classz")
@JsonSubTypes({
    @JsonSubTypes.Type(value = GruSpecificData.class, name = "GruSpecificData"),
})
public abstract class SpecificData {
    
    private List<String> personeNonSpegnibili;
    private List<String> personeDaSpegnere;
    private Integer progressivo_ultima_trasformazione;
    private Integer tolleranzaStrutture;
    private Integer tolleranzaAppartenenti;

    public List<String> getPersoneNonSpegnibili() {
        return personeNonSpegnibili;
    }

    public void setPersoneNonSpegnibili(List<String> personeNonSpegnibili) {
        this.personeNonSpegnibili = personeNonSpegnibili;
    }

    public List<String> getPersoneDaSpegnere() {
        return personeDaSpegnere;
    }

    public void setPersoneDaSpegnere(List<String> personeDaSpegnere) {
        this.personeDaSpegnere = personeDaSpegnere;
    }

    public Integer getProgressivo_ultima_trasformazione() {
        return progressivo_ultima_trasformazione;
    }

    public void setProgressivo_ultima_trasformazione(Integer progressivo_ultima_trasformazione) {
        this.progressivo_ultima_trasformazione = progressivo_ultima_trasformazione;
    }

    public Integer getTolleranzaStrutture() {
        return tolleranzaStrutture;
    }

    public void setTolleranzaStrutture(Integer tolleranzaStrutture) {
        this.tolleranzaStrutture = tolleranzaStrutture;
    }

    public Integer getTolleranzaAppartenenti() {
        return tolleranzaAppartenenti;
    }

    public void setTolleranzaAppartenenti(Integer tolleranzaAppartenenti) {
        this.tolleranzaAppartenenti = tolleranzaAppartenenti;
    }
    
}
