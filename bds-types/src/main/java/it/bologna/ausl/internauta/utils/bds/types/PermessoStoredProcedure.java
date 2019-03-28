package it.bologna.ausl.internauta.utils.bds.types;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author Guido
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PermessoStoredProcedure implements Serializable {
//    private Integer id;
    private String predicato;
    private Boolean propagaSoggetto;
    private Boolean propagaOggetto;
    private Boolean virtuale;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataInserimentoRiga;
    private String originePermesso;
    private Integer idPermessoBloccato;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime attivoDal;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime attivoAl;

    public PermessoStoredProcedure() {
    }

    public PermessoStoredProcedure(String predicato, Boolean propagaSoggetto, Boolean propagaOggetto, Boolean virtuale, String ambito, LocalDateTime dataInserimentoRiga, String tipo) {
        this.predicato = predicato;
        this.propagaSoggetto = propagaSoggetto;
        this.propagaOggetto = propagaOggetto;
        this.virtuale = virtuale;
        this.dataInserimentoRiga = dataInserimentoRiga;
    }
    
//    public PermessoStoredProcedure(String predicato, Boolean propagaSoggetto, Boolean propagaOggetto, String originePermesso, Integer idPermessoBloccato) {
//        this.predicato = predicato;
//        this.propagaSoggetto = propagaSoggetto;
//        this.propagaOggetto = propagaOggetto;
//        this.originePermesso = originePermesso;
//        this.idPermessoBloccato = idPermessoBloccato;
//    }
//    
    
    public PermessoStoredProcedure(String predicato, Boolean propagaSoggetto, Boolean propagaOggetto, String originePermesso, Integer idPermessoBloccato, LocalDateTime attivoDal, LocalDateTime attivoAl) {
        this.predicato = predicato;
        this.propagaSoggetto = propagaSoggetto;
        this.propagaOggetto = propagaOggetto;
        this.originePermesso = originePermesso;
        this.idPermessoBloccato = idPermessoBloccato;
        this.attivoDal = attivoDal;
        this.attivoAl = attivoAl;
    }
//    public Integer getId() {
//        return id;
//    }
//
//    public void setId(Integer id) {
//        this.id = id;
//    }

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

    public LocalDateTime getDataInserimentoRiga() {
        return dataInserimentoRiga;
    }

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

}
