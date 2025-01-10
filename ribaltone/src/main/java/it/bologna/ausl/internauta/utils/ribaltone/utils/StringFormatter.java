package it.bologna.ausl.internauta.utils.ribaltone.utils;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Top
 */
public class StringFormatter {

    public static String formatStringsWithCommasAndQuotes(List<String> strings) {
        // Utilizza Stream per unire le stringhe con le virgole
        return strings.stream()
                      .map(s -> "'" + s + "'") // Aggiunge gli apici singoli
                      .collect(Collectors.joining(", ")); // Intervalla con le virgole
    }

    
    
}
