package it.bologna.ausl.internauta.utils.downloader.authorization;

import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author gdm
 */
@Configuration
public class KeysCollector {
    // la mappa contiene come chiave lo SHA-256 della chiave pubblica contenuta usata per firmare il token e come valore il nome del certificato che la contiene
    public Map<String, String> hashPublicKeyMap;
    
    // viene inizializzata hardcoded all'avvio.
    //TODO: mettere la configurazione su DB?
    @PostConstruct
    public void initilize() {
        hashPublicKeyMap.put("FDB1F11965344A44DB32C4FE1D53C4A5104453BAEFB58F106BD6ABDD4736537B", "DOWNLOADER_TEST.crt"); // certificato di test
        hashPublicKeyMap.put("819EE5A635FA45FBCED93BEE6ED0B9721C02A9B933328806DDFF84CD5AA9DD42", "DOWNLOADER_BABEL.crt");// certificato interno nostro per prod
    }
}
