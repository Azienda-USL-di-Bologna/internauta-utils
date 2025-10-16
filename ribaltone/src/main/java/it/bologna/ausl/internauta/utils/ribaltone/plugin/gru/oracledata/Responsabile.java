package it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.oracledata;

import java.io.Serializable;

/**
 *
 * @author Top
 */
public class Responsabile implements Serializable {

    private String codiceEnte;

    private String codiceMatricola;

    private Integer idCasella;

//    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    private Date datain;
//
//    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
//    private Date datafi;
    private String tipo;

    public Responsabile() {
    }

    public String getCodiceEnte() {
        return codiceEnte;
    }

    public void setCodiceEnte(String codiceEnte) {
        this.codiceEnte = codiceEnte;
    }

    public String getCodiceMatricola() {
        return codiceMatricola;
    }

    public void setCodiceMatricola(String codiceMatricola) {
        this.codiceMatricola = codiceMatricola;
    }

    public Integer getIdCasella() {
        return idCasella;
    }

    public void setIdCasella(Integer idCasella) {
        this.idCasella = idCasella;
    }

//    public Date getDatain() {
//        return datain;
//    }
//
//    public void setDatain(Date datain) {
//        this.datain = datain;
//    }
//
//    public Date getDatafi() {
//        return datafi;
//    }
//
//    public void setDatafi(Date datafi) {
//        this.datafi = datafi;
//    }
    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    @Override
    public String toString() {
        return "it.bologna.ausl.internauta.model.entities.ribaltone.entita.plugin.gru.oracledata.Responsabile[ codiceMatricola=" + codiceMatricola + " ]";
    }

}
