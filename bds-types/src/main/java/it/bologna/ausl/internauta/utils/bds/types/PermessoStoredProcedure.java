package it.bologna.ausl.internauta.utils.bds.types;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author Guido
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PermessoStoredProcedure implements Serializable {

    //private static final long serialVersionUID = 1L;
    private Integer id;
    private String predicato;
    private Boolean propagaSoggetto;
    private Boolean propagaOggetto;
    private Boolean virtuale;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime dataInserimentoRiga;
    private String originePermesso;
    private Integer idPermessoBloccato;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime attivoDal;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime attivoAl;
    private EntitaStoredProcedure entitaVeicolante;

    public PermessoStoredProcedure() {
    }

    public PermessoStoredProcedure(String predicato, Boolean propagaSoggetto, Boolean propagaOggetto, Boolean virtuale, String ambito, LocalDateTime dataInserimentoRiga, String tipo) {
        this.predicato = predicato;
        this.propagaSoggetto = propagaSoggetto;
        this.propagaOggetto = propagaOggetto;
        this.virtuale = virtuale;
        this.dataInserimentoRiga = dataInserimentoRiga;
    }

    public PermessoStoredProcedure(String predicato, Boolean propagaSoggetto, Boolean propagaOggetto, String originePermesso, Integer idPermessoBloccato, LocalDateTime attivoDal, LocalDateTime attivoAl) {
        this.predicato = predicato;
        this.propagaSoggetto = propagaSoggetto;
        this.propagaOggetto = propagaOggetto;
        this.originePermesso = originePermesso;
        this.idPermessoBloccato = idPermessoBloccato;
        this.attivoDal = attivoDal;
        this.attivoAl = attivoAl;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getPredicato() {
        return predicato;
    }

    public void setPredicato(String predicato) {
        this.predicato = predicato;
    }

    @JsonProperty("propaga_soggetto")
    public Boolean getPropagaSoggetto() {
        return propagaSoggetto;
    }

    // @JsonAlias("propaga_soggetto")
    @JsonProperty("propaga_soggetto")
    public void setPropagaSoggetto(Boolean propagaSoggetto) {
        this.propagaSoggetto = propagaSoggetto;
    }

    @JsonProperty("propaga_oggetto")
    public Boolean getPropagaOggetto() {
        return propagaOggetto;
    }

    @JsonProperty("propaga_oggetto")
    public void setPropagaOggetto(Boolean propagaOggetto) {
        this.propagaOggetto = propagaOggetto;
    }

    public Boolean getVirtuale() {
        return virtuale;
    }

    public void setVirtuale(Boolean virtuale) {
        this.virtuale = virtuale;
    }

    @JsonProperty("data_inserimento_riga")
    public LocalDateTime getDataInserimentoRiga() {
        return dataInserimentoRiga;
    }

    @JsonProperty("data_inserimento_riga")
    public void setDataInserimentoRiga(LocalDateTime dataInserimentoRiga) {
        this.dataInserimentoRiga = dataInserimentoRiga;
    }

    @JsonProperty("origine_permesso")
    public String getOriginePermesso() {
        return originePermesso;
    }

    @JsonProperty("origine_permesso")
    public void setOriginePermesso(String originePermesso) {
        this.originePermesso = originePermesso;
    }

    @JsonProperty("id_permesso_bloccato")
    public Integer getIdPermessoBloccato() {
        return idPermessoBloccato;
    }

    @JsonProperty("id_permesso_bloccato")
    public void setIdPermessoBloccato(Integer idPermessoBloccato) {
        this.idPermessoBloccato = idPermessoBloccato;
    }

    @JsonProperty("attivo_dal")
    public LocalDateTime getAttivoDal() {
        return attivoDal;
    }

    @JsonProperty("attivo_dal")
    public void setAttivoDal(LocalDateTime attivoDal) {
        this.attivoDal = attivoDal;
    }

    @JsonProperty("attivo_al")
    public LocalDateTime getAttivoAl() {
        return attivoAl;
    }

    @JsonProperty("attivo_al")
    public void setAttivoAl(LocalDateTime attivoAl) {
        this.attivoAl = attivoAl;
    }

    @JsonProperty("entita_veicolante")
    public EntitaStoredProcedure getEntitaVeicolante() {
        return entitaVeicolante;
    }

    @JsonProperty("entita_veicolante")
    public void setEntitaVeicolante(EntitaStoredProcedure entitaVeicolante) {
        this.entitaVeicolante = entitaVeicolante;
    }

    @Override
    public boolean equals(Object obj) {
        Field[] declaredFields = this.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            try {
                Object val1 = field.get(this);
                Object val2 = obj.getClass().getDeclaredField(field.getName()).get(obj);
                if ((val1 != null && val2 == null) || (val1 == null && val2 != null)) {
                    return false;
                } else if ((val1 != null && val2 != null) && !val1.equals(val2)) {
                    return false;
                }
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
                return false;
            }
        }
        return true;
    }

}
