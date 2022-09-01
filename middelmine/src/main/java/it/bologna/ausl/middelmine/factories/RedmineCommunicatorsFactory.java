/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.middelmine.factories;

import com.taskadapter.redmineapi.RedmineManager;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;

/**
 *
 * @author Salo
 */
public class RedmineCommunicatorsFactory {

    public static RedmineManager getRedmineManager(ParametersManagerInterface parametersManager) {
        return com.taskadapter.redmineapi.RedmineManagerFactory.createWithApiKey(parametersManager.getRedmineBaseUrl(), parametersManager.getAdminApiKey());
    }
}
