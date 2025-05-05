package it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.data;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.data.ColonneImportazioneCSVRibaltoneEnums.ColonneAnagrafica;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.data.ColonneImportazioneCSVRibaltoneEnums.ColonneStruttura;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.data.ColonneImportazioneCSVRibaltoneEnums.ColonneAppartenente;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.data.ColonneImportazioneCSVRibaltoneEnums.ColonneTrasformazione;
import it.bologna.ausl.model.entities.tip.data.KeyValueEnum;
import java.util.List;

/**
 *
 * @author gdm
 *
 * Interfaccia che accomuna i vari enum che descrivono i campi delle entità
 * delle tabelle di importazione (importazioni_documenti/importazioni_archivi)
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "classz")

// Per permettere la generazione corretta del JSON bisogna inserire un @JsonSubTypes.Type per ogni implementazione di questa classe astratta
@JsonSubTypes({
    @JsonSubTypes.Type(value = ColonneAppartenente.class, name = "ColonneAppartenente"),
    @JsonSubTypes.Type(value = ColonneStruttura.class, name = "ColonneStruttura"),
    @JsonSubTypes.Type(value = ColonneAnagrafica.class, name = "ColonneAnagrafica"),
    @JsonSubTypes.Type(value = ColonneTrasformazione.class, name = "ColonneTrasformazione")})
public interface ColonneImportazioneCSVRibaltone extends KeyValueEnum<List<String>> {

    /**
     * torna l'enum corretto in base alla tipologia
     *
     * @param tipologia
     * @return
     */
    public static Class<? extends ColonneImportazioneCSVRibaltone> getColumnsEnum(TipologiaCsv tipologia) {
        switch (tipologia) {
            case APPARTENENTI:
                return ColonneAppartenente.class;
            case STRUTTURE:
                return ColonneStruttura.class;
            case ANAGRAFICA:
                return ColonneAnagrafica.class;
            case TRASFORMAZIONI:
                return ColonneTrasformazione.class;
            default:
                throw new AssertionError(String.format("tipologia %s non valida", tipologia));
        }
    }

    /**
     * Reperisce l'enum delle colonne specifiche dell'oggetto che si vuole
     * importare e ne trova il singolo valore corrispondente a seconda del
     * header. Per farlo viene usato un enum che ha come chiave il nome del
     * campo della classe e come valori i possibili nomi degli header associati
     *
     * @param value il valore del valore enum da cercare (corrisponde al nome
     * dell'header del csv)
     * @param tipologia la tipologia di importazione. Serve per capire in quale
     * enum cercare, dato che ce n'è uno per ogni tipologia
     * @return il singolo valore enum che si chiama come il campo sull'entità,
     * null se non la trova
     */
    public static ColonneImportazioneCSVRibaltone findKey(String value, TipologiaCsv tipologia) {
        String toFind = value.toLowerCase();
        ColonneImportazioneCSVRibaltone foundKey = null;
        Class aEnum = getColumnsEnum(tipologia);

        Object[] enumConstants = aEnum.getEnumConstants();
        ColonneImportazioneCSVRibaltone[] values = (ColonneImportazioneCSVRibaltone[]) enumConstants;
        for (ColonneImportazioneCSVRibaltone key : values) {
            List<String> keyValues = key.getValue();
            if (toFind.equals(key.toString().toLowerCase()) || keyValues.contains(toFind)) {
                foundKey = key;
                break;
            }
        }
        return foundKey;
    }

    public ColonneImportazioneCSVRibaltone getErroriColumn();
}
