package it.bologna.ausl.internauta.utils.ribaltone;

import it.bologna.ausl.internauta.utils.ribaltone.SpecificData;

/**
 *
 * @author Top
 */
public class RibaltoneParamConf<T extends SpecificData>{
    private String fonte;
    private  T specifiche ;
    private String cacheOperationToDo;

    public RibaltoneParamConf(String fonte, T specifiche, String cacheOperationToDo) {
        this.fonte = fonte;
        this.specifiche = specifiche;
        this.cacheOperationToDo = cacheOperationToDo;
    }

    public String getFonte() {
        return fonte;
    }

    public void setFonte(String fonte) {
        this.fonte = fonte;
    }

    public T getSpecifiche() {
        return specifiche;
    }

    public void setSpecifiche(T specifiche) {
        this.specifiche = specifiche;
    }

    public String getCacheOperationToDo() {
        return cacheOperationToDo;
    }

    public void setCacheOperationToDo(String cacheOperationToDo) {
        this.cacheOperationToDo = cacheOperationToDo;
    }    
    
}
