/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.sendintegration.model;

import java.time.ZonedDateTime;

/**
 *
 * @author gdm
 */
public class InfoRegistrazioneLotto {
    private String numeroRegistrazione;
    private ZonedDateTime dataRegistrazione;
    private Integer numeroDocumentiRegistrati;
    private Integer numeroErrori;

    public InfoRegistrazioneLotto() {
    }

    public InfoRegistrazioneLotto(String numeroRegistrazione, ZonedDateTime dataRegistrazione, Integer numeroDocumentiRegistrati, Integer numeroErrori) {
        this.numeroRegistrazione = numeroRegistrazione;
        this.dataRegistrazione = dataRegistrazione;
        this.numeroDocumentiRegistrati = numeroDocumentiRegistrati;
        this.numeroErrori = numeroErrori;
    }

    public String getNumeroRegistrazione() {
        return numeroRegistrazione;
    }

    public void setNumeroRegistrazione(String numeroRegistrazione) {
        this.numeroRegistrazione = numeroRegistrazione;
    }

    public ZonedDateTime getDataRegistrazione() {
        return dataRegistrazione;
    }

    public void setDataRegistrazione(ZonedDateTime dataRegistrazione) {
        this.dataRegistrazione = dataRegistrazione;
    }

    public Integer getNumeroDocumentiRegistrati() {
        return numeroDocumentiRegistrati;
    }

    public void setNumeroDocumentiRegistrati(Integer numeroDocumentiRegistrati) {
        this.numeroDocumentiRegistrati = numeroDocumentiRegistrati;
    }

    public Integer getNumeroErrori() {
        return numeroErrori;
    }

    public void setNumeroErrori(Integer numeroErrori) {
        this.numeroErrori = numeroErrori;
    }
}
