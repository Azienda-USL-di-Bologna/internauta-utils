package it.bologna.ausl.internauta.utils.authorizationutils.utils;

import it.bologna.ausl.model.entities.baborg.Ruolo;
import it.bologna.ausl.model.entities.baborg.Utente;
import java.util.List;
import java.util.Map;

/**
 *
 * @author gdm
 */
public class UserInfoUtilities {
    public static boolean isSD(Utente user) {
        Map<String, Map<String, List<String>>> ruoliUtentiPersona = user.getRuoliUtentiPersona();
        return ruoliUtentiPersona.containsKey(Ruolo.CodiciRuolo.SD.toString());
    }
}
