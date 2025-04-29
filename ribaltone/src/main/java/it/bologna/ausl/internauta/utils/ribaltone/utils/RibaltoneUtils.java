package it.bologna.ausl.internauta.utils.ribaltone.utils;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import java.util.ArrayList;
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
    public static <T extends DatiRibaltoneInterface> Map<String, Integer> generateIndex(List<? extends DatiRibaltoneInterface> datiDaImportare, Function<T, Object> fn) {
        Map<String, Integer> indexToImport = new HashMap<>();
        for (int i = 0; i < datiDaImportare.size(); i++) {
            T datiRibaltoneInterface = (T) datiDaImportare.get(i);
            indexToImport.put(String.valueOf(fn.apply(datiRibaltoneInterface)), i);
        }
        return indexToImport;
    }
    
    public static List<UtenteStruttura> differenza(List<UtenteStruttura> list1, List<UtenteStruttura> list2) {
        List<UtenteStruttura> result = new ArrayList<>();
        for (UtenteStruttura u1 : list1) {
            boolean trovato = false;
            for (UtenteStruttura u2 : list2) {
                if (u1.getIdUtente().getIdPersona().getId().equals(u2.getIdUtente().getIdPersona().getId())) {
                    trovato = true;
                    break;
                }
            }
            if (!trovato) {
                result.add(u1);
            }
        }
        return result;
    }
}
