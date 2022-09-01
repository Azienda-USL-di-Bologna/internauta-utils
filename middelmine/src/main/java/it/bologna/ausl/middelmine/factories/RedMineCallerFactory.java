package it.bologna.ausl.middelmine.factories;

import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.rest.RedMineCaller;

/**
 *
 * @author Salo
 */
public class RedMineCallerFactory {

    public static RedMineCaller getRedMineCaller() {
        return new RedMineCaller();
    }

    public static RedMineCaller getRedMineCaller(ParametersManagerInterface parametersManager) {
        return new RedMineCaller(parametersManager);
    }
}
