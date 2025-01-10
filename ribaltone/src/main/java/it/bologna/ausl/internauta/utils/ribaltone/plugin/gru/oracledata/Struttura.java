package it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.oracledata;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.nextsw.common.data.annotations.GenerateProjections;
import java.io.Serializable;
import java.time.ZonedDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.format.annotation.DateTimeFormat;
import org.hibernate.annotations.DynamicUpdate;

/**
 *
 * @author Top
 */
@Entity
@Table(name = "gru_strutture", catalog = "internauta", schema = "colonia")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@GenerateProjections({})
@DynamicUpdate
public class Struttura implements Serializable {

    private Integer idCasella;

    private Integer idPadre;

    private String descrizione;

//    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    private ZonedDateTime datain;
//
//    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    private ZonedDateTime datafi;

    private String tipoLegame;

    private String codiceEnte;

    public Struttura() {
    }

    public Integer getIdCasella() {
        return idCasella;
    }

    public void setIdCasella(Integer idCasella) {
        this.idCasella = idCasella;
    }

    public Integer getIdPadre() {
        return idPadre;
    }

    public void setIdPadre(Integer idPadre) {
        this.idPadre = idPadre;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

//    public ZonedDateTime getDatain() {
//        return datain;
//    }
//
//    public void setDatain(ZonedDateTime datain) {
//        this.datain = datain;
//    }
//
//    public ZonedDateTime getDatafi() {
//        return datafi;
//    }
//
//    public void setDatafi(ZonedDateTime datafi) {
//        this.datafi = datafi;
//    }

    public String getTipoLegame() {
        return tipoLegame;
    }

    public void setTipoLegame(String tipoLegame) {
        this.tipoLegame = tipoLegame;
    }

    public String getCodiceEnte() {
        return codiceEnte;
    }

    public void setCodiceEnte(String codiceEnte) {
        this.codiceEnte = codiceEnte;
    }

    @Override
    public String toString() {
        return "it.bologna.ausl.internauta.model.entities.ribaltone.entita.plugin.gru.oracledata.Struttura[ idCasella=" + idCasella + "_idPadre_" + idPadre + " ]";
    }

    public DatiDaImportareStruttura toFonteIntermedia(String codiceAzienda) {
        DatiDaImportareStruttura fonteIntermediaStruttura = new DatiDaImportareStruttura();
        fonteIntermediaStruttura.setCodiceEnte(codiceEnte);
//        fonteIntermediaStruttura.setDatain(datain);
//        fonteIntermediaStruttura.setDatafi(datafi);
        fonteIntermediaStruttura.setDescrizione(descrizione);
        fonteIntermediaStruttura.setIdCasella(idCasella);
        fonteIntermediaStruttura.setIdPadre(idPadre);
        fonteIntermediaStruttura.setTipoLegame(tipoLegame);
        fonteIntermediaStruttura.setCodiceAzienda(codiceAzienda);

        return fonteIntermediaStruttura;
    }

}
