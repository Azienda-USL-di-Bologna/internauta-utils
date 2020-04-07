/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.middelmine.factories;

import com.taskadapter.redmineapi.RedmineManager;
import it.bologna.ausl.middelmine.builders.LoadableParametersManagerBuilder;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.managers.configuration.LoadableParametersManager;
import java.io.IOException;

/**
 *
 * @author Salo
 */
public class RedmineManagerFactory {

    public static RedmineManager getRedmineManager() throws IOException {
        ParametersManagerInterface loadableParametersManager = ParametersManagerFactory.getLoadableParametersManager();
        LoadableParametersManagerBuilder.buildParams((LoadableParametersManager) loadableParametersManager);
        return new RedmineManager(loadableParametersManager.getRedmineBaseUrl(), loadableParametersManager.getPrivateApiKey());
    }
}
