package it.bologna.ausl.internauta.utils.bdm.core;

import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess.BdmStatus;
import java.util.Map;



/**
 *
 * @author andrea
 */
public class Result {

    private BdmStatus status;
    private Map<String, Object> bag;
    private String message;

    public Result() {
    }

    public Result(BdmStatus status, Map<String, Object> bag, String message) {
        this.status = status;
        this.bag = bag;
        this.message = message;
    }

    public BdmStatus getStatus() {
        return status;
    }

    public void setStatus(BdmStatus status) {
        this.status = status;
    }

    public Map<String, Object> getBag() {
        return bag;
    }

    public void setBag(Map<String, Object> bag) {
        this.bag = bag;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
