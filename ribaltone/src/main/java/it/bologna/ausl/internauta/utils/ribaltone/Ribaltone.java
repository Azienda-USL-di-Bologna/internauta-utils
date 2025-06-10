package it.bologna.ausl.internauta.utils.ribaltone;

import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.CsvImportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Top
 * oggetto che contiene tutte le istanze settate da ribaltone factory
 * specializzato nel plugin letto dalla conf
 */
public class Ribaltone {

    private static final Logger log = LoggerFactory.getLogger(Ribaltone.class);

    public static enum TipologiaTabellaBaborg {
        APPARTENENTI,
        STRUTTURE,
        ANAGRAFICHE,
        TRASFORMAZIONI
    }

}
