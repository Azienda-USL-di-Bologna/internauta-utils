package it.bologna.ausl.middelmine.factories;

import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.rest.RedMineCallManager;

/**
 *
 * @author Salo
 */
public class RedMineCallerManagerFactory {

    public static RedMineCallManager getRedMineCallerManager(ParametersManagerInterface parametersManager) {
        return new RedMineCallManager(parametersManager);
    }
}
