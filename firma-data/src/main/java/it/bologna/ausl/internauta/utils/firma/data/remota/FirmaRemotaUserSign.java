package it.bologna.ausl.internauta.utils.firma.data.remota;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.bologna.ausl.internauta.utils.firma.data.remota.medassignservice.MedasUserSign;

/**
 *
 * @author gdm
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "classz")

// Per permettere la generazione corretta del JSON bisogna inserire un @JsonSubTypes.Type per ogni implementazione di questa classe astratta
@JsonSubTypes({
    @JsonSubTypes.Type(value = MedasUserSign.class, name = "MedasUserSign")
})
public abstract class FirmaRemotaUserSign {
        public abstract String getId();
        public abstract String getDescription();
}
