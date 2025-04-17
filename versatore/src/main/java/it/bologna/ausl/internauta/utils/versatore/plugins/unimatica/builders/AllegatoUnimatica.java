package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders;

/**
 *
 * @author boria
 */
public class AllegatoUnimatica {

    private Integer idFile;
    private String nomeFile;

    public AllegatoUnimatica() {
    }

    public AllegatoUnimatica(Integer idFile, String nomeFile) {
        this.idFile = idFile;
        this.nomeFile = nomeFile;
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

}
