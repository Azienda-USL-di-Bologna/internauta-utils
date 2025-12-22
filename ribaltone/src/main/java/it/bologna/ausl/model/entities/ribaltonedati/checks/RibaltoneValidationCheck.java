package it.bologna.ausl.model.entities.ribaltonedati.checks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.nextsw.common.data.annotations.GenerateProjections;
import jakarta.persistence.*;
import java.io.Serializable;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ribaltone_validation_checks", catalog = "internauta", schema = "ribaltone_dati")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@GenerateProjections({})
@DynamicUpdate
public class RibaltoneValidationCheck implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String query;

    @Column(name = "query_sanante", nullable = true, columnDefinition = "TEXT")
    private String querySanante;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risultati_errati", columnDefinition = "jsonb")
    private RisultatiErrati risultatiErrati;

    @Column(nullable = false)
    private Boolean attivo = true;

    @Column(name = "id_aziende", nullable = false, columnDefinition = "int4[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private Integer[] idAziende;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descrizione;

    @Enumerated(EnumType.STRING)
    @Column(name = "regola_di_successo", nullable = false, columnDefinition = "diagnostica.query_check_detection_rule")
    private RegolaDiSuccesso regolaDiSuccesso = RegolaDiSuccesso.ZERO_ROWS;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // Enum per la regola di successo
    public enum RegolaDiSuccesso {
        ZERO_ROWS
    }

    // Constructors
    public RibaltoneValidationCheck() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getQuerySanante() {
        return querySanante;
    }

    public void setQuerySanante(String querySanante) {
        this.querySanante = querySanante;
    }

    public RisultatiErrati getRisultatiErrati() {
        return risultatiErrati;
    }

    public void setRisultatiErrati(RisultatiErrati risultatiErrati) {
        this.risultatiErrati = risultatiErrati;
    }

    public Boolean getAttivo() {
        return attivo;
    }

    public void setAttivo(Boolean attivo) {
        this.attivo = attivo;
    }

    public Integer[] getIdAziende() {
        return idAziende;
    }

    public void setIdAziende(Integer[] idAziende) {
        this.idAziende = idAziende;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public RegolaDiSuccesso getRegolaDiSuccesso() {
        return regolaDiSuccesso;
    }

    public void setRegolaDiSuccesso(RegolaDiSuccesso regolaDiSuccesso) {
        this.regolaDiSuccesso = regolaDiSuccesso;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
