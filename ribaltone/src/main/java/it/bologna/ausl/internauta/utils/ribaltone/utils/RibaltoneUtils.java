package it.bologna.ausl.internauta.utils.ribaltone.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 * @author Top
 */
public class RibaltoneUtils {

    public static String formatStringsWithCommasAndQuotes(List<String> strings) {
        // Utilizza Stream per unire le stringhe con le virgole
        return strings.stream()
                      .map(s -> "'" + s + "'") // Aggiunge gli apici singoli
                      .collect(Collectors.joining(", ")); // Intervalla con le virgole
    }
    
//    public static Map<String, Integer> generateIndex(List<? extends DatiRibaltoneInterface> datiDaImportare) {
//        Map<String, Integer> indexToImport = new HashMap<>();
//
//        for (int i = 0; i < datiDaImportare.size(); i++) {
//            indexToImport.put(datiDaImportare.get(i).getKey(), i);
//        }
//        return indexToImport;
//    }
    
    public static <T extends DatiRibaltoneInterface> Map<String, Integer> generateIndex(List<? extends DatiRibaltoneInterface> datiDaImportare,  Function<T, Object> fn) {
        Map<String, Integer> indexToImport = new HashMap<>();
        for (int i = 0; i < datiDaImportare.size(); i++) {
            T datiRibaltoneInterface =(T) datiDaImportare.get(i);
            indexToImport.put(String.valueOf(fn.apply(datiRibaltoneInterface)), i);
        }
        return indexToImport;
    }

    
    
}
