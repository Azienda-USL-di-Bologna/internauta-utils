package it.bologna.ausl.internauta.utils.firma.data.remota;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.bologna.ausl.internauta.utils.firma.data.remota.arubasignservice.ArubaUserInformation;

/**
 *
 * @author gdm
 * 
 * Questa classe astratta deve rappresentare le informazioni dell'utente che deve firmare.
 * Bisogna creare una classe concreta per ogni provider, perché le informazioni utente potrebbe essere diverse per ogni provider
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "classz")

// Per permettere la generazione corretta del JSON bisogna inserire un @JsonSubTypes.Type per ogni implementazione di questa classe astratta
@JsonSubTypes({
    @JsonSubTypes.Type(value = ArubaUserInformation.class, name = "ArubaUserInformation"),})
public abstract class UserInformation {

}
