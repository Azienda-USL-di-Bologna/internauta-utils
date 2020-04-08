/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.middelmine.redmine.communication.communicators.concrete;

import it.bologna.ausl.middelmine.redmine.communication.communicators.abstracts.RedmineAbstractResourceProvider;
import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.bean.Project;
import it.bologna.ausl.middelmine.factories.RedmineCommunicatorsFactory;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;

/**
 *
 * @author Salo
 */
public class RedmineProjectManagerProvider extends RedmineAbstractResourceProvider {

    public RedmineProjectManagerProvider(ParametersManagerInterface pm) {
        super(pm);
    }

    public Project getProjectByName(String projectName) throws RedmineException {
        RedmineManager rmManager = RedmineCommunicatorsFactory.getRedmineManager(pm);
        return rmManager.getProjectManager().getProjectByKey(projectName);

    }
}
