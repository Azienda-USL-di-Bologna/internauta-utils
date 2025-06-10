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
    @JsonSubTypes.Type(value = GruSpecificData.class, name = "GruSpecificData"),})
public abstract class SpecificData {

    private List<String> personeNonSpegnibili;
    private List<String> personeDaSpegnere;

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

}
