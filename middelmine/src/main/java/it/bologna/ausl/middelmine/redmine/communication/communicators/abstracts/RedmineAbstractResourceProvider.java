package it.bologna.ausl.middelmine.redmine.communication.communicators.abstracts;

import com.taskadapter.redmineapi.*;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;

/**
 *
 * @author Salo
 */
public abstract class RedmineAbstractResourceProvider {

    protected ParametersManagerInterface pm;

    public RedmineAbstractResourceProvider(ParametersManagerInterface parametersManager) {
        pm = parametersManager;
    }

}
