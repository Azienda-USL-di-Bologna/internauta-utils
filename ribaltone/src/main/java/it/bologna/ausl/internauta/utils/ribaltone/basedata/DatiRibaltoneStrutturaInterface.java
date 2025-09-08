package it.bologna.ausl.internauta.utils.ribaltone.basedata;

/**
 *
 * @author Top
 */
public interface DatiRibaltoneStrutturaInterface extends DatiRibaltoneInterface {

    public Integer getIdCasella();

    @Override
    public Integer getIdAzienda();

    public Integer getIdCasellaPadre();

    public void setIdCasellaPadre(Integer idCasellaPadre);

}
