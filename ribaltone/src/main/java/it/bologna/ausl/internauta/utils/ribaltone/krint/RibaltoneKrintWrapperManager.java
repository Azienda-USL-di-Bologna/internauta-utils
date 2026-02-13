package it.bologna.ausl.internauta.utils.ribaltone.krint;

import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.configurazione.data.ConfigRibaltoneView;

/**
 *
 * @author Top
 */
public abstract class RibaltoneKrintWrapperManager {

    public abstract void scriviNelKrint(ConfigRibaltoneView configRibaltoneViewNew, ConfigRibaltoneView configRibaltoneViewOld, Utente utenteModificante, Integer idAzienda);

}
