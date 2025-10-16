package it.bologna.ausl.model.entities.ribaltonedati.checks;

/**
 *
 * @author Top
 */
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RisultatiErrati implements Serializable {

    private Map<String, List<Map<String, Object>>> risultati = new HashMap<>();

    public RisultatiErrati() {
    }

    // Getter per accedere ai risultati
    public Map<String, List<Map<String, Object>>> getRisultati() {
        return risultati;
    }

    public void setRisultati(Map<String, List<Map<String, Object>>> risultati) {
        this.risultati = risultati;
    }

    // Metodo per aggiungere risultati per un'azienda
    public void addRisultatiAzienda(String codiceAzienda, List<Map<String, Object>> righe) {
        this.risultati.put(codiceAzienda, righe);
    }

    // Metodo per ottenere i risultati di una specifica azienda
    public List<Map<String, Object>> getRisultatiAzienda(String codiceAzienda) {
        return this.risultati.get(codiceAzienda);
    }

    // Metodo per rimuovere i risultati di un'azienda
    public void removeRisultatiAzienda(String codiceAzienda) {
        this.risultati.remove(codiceAzienda);
    }

    // Deserializzazione personalizzata - permette chiavi Integer dal JSON
    @JsonAnySetter
    public void setRisultato(String key, List<Map<String, Object>> value) {
        try {

            this.risultati.put(key, value);
        } catch (NumberFormatException e) {
            // Ignora chiavi non numeriche
        }
    }

    @JsonIgnore
    public boolean isEmpty() {
        return risultati.isEmpty();
    }

    @JsonIgnore
    public int getNumeroAziende() {
        return risultati.size();
    }
}
