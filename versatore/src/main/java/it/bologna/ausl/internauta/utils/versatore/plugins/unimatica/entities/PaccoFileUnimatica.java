package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities;

import it.bologna.ausl.riversamento.sender.PaccoFile;

public class PaccoFileUnimatica extends PaccoFile {

    private Integer idAllegato;

    public PaccoFileUnimatica() {
        super();
    }

    public PaccoFileUnimatica(Integer idAllegato) {
        super();
        this.idAllegato = idAllegato;
    }

    public Integer getIdAllegato() {
        return idAllegato;
    }

    public void setIdAllegato(Integer idAllegato) {
        this.idAllegato = idAllegato;
    }
}
