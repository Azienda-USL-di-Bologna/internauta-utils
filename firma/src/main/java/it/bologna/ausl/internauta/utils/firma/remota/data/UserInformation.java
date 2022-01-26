package it.bologna.ausl.internauta.utils.firma.remota.data;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.ArubaUserInformation;

/**
 *
 * @author guido
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "classz")

@JsonSubTypes({
    @JsonSubTypes.Type(value = ArubaUserInformation.class, name = "ArubaUserInformation"),})
public abstract class UserInformation {

}
