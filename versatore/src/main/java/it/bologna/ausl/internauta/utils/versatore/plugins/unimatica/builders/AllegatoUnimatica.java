package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders;

import java.nio.charset.StandardCharsets;

/**
 *
 * @author boria
 */
public class AllegatoUnimatica {

    private Integer idFile;
    private String nomeFile;
    private byte[] impronta;
    private Boolean firmato;
    private String formato;

    public AllegatoUnimatica() {
    }

    public AllegatoUnimatica(Integer idFile, String nomeFile, String impronta, Boolean firmato, String formato) {
        this.idFile = idFile;
        this.nomeFile = nomeFile;
        this.impronta = impronta.getBytes(StandardCharsets.UTF_8);
        this.firmato = firmato;
        this.formato = formato;

    }

    public Integer getIdFile() {
        return idFile;
    }

    public void setIdFile(Integer idFile) {
        this.idFile = idFile;
    }

    public String getNomeFile() {
        return nomeFile;
    }

    public void setNomeFile(String nomeFile) {
        this.nomeFile = nomeFile;
    }

    public byte[] getImpronta() {
        return impronta;
    }

    public void setImpronta(byte[] impronta) {
        this.impronta = impronta;
    }

    public Boolean getFirmato() {
        return firmato;
    }

    public void setFirmato(Boolean firmato) {
        this.firmato = firmato;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

}
