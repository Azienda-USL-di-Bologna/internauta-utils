package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities;

import java.util.List;

/**
 *
 * @author boria
 */
public class EsitoConsegnaUnimatica {

    private String id;
    private List<ErroreUnimatica> erroriList;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<ErroreUnimatica> getErroriList() {
        return erroriList;
    }

    public void setErroriList(List<ErroreUnimatica> erroriList) {
        this.erroriList = erroriList;
    }

}
