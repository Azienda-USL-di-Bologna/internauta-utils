package it.bologna.ausl.internauta.utils.bdm.core;

import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess.BdmStatus;
import it.bologna.ausl.internauta.utils.bdm.utilities.Bag;



/**
 *
 * @author andrea
 */
public class Result {

    private BdmStatus status;
    private Bag bag;
    private String message;

    public Result() {
    }

    public Result(BdmStatus status, Bag bag, String message) {
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

    public Bag getBag() {
        return bag;
    }

    public void setBag(Bag bag) {
        this.bag = bag;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
