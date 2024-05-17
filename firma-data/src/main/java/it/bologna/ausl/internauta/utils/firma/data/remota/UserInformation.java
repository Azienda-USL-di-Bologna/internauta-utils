package it.bologna.ausl.internauta.utils.firma.data.remota;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.bologna.ausl.internauta.utils.firma.data.remota.arubasignservice.ArubaUserInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.infocertsignservice.InfocertUserInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.medassignservice.MedasUserInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.namirialsignservice.NamirialUserInformation;

/**
 *
 * @author gdm
 * 
 * Questa classe astratta deve rappresentare le informazioni dell'utente che deve firmare.
 * Bisogna creare una classe concreta per ogni provider, perché le informazioni utente potrebbe essere diverse per ogni provider
 * @param <T>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "classz")

// Per permettere la generazione corretta del JSON bisogna inserire un @JsonSubTypes.Type per ogni implementazione di questa classe astratta
@JsonSubTypes({
    @JsonSubTypes.Type(value = ArubaUserInformation.class, name = "ArubaUserInformation"),
    @JsonSubTypes.Type(value = InfocertUserInformation.class, name = "InfocertUserInformation"),
    @JsonSubTypes.Type(value = NamirialUserInformation.class, name = "NamirialUserInformation"),
    @JsonSubTypes.Type(value = MedasUserInformation.class, name = "MedasUserInformation")
})
public abstract class UserInformation {
    
    public abstract String getUsername();
    public abstract String getPassword();

    public abstract <T extends FirmaRemotaModalitaFirma> T getModalitaFirma();
   
    @JsonProperty
    public abstract Boolean useSavedCredential();
    
    public boolean isAutodetectMode() {
        return false;
    }
}
