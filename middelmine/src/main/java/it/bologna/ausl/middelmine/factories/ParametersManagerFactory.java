/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.middelmine.factories;

import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.managers.configuration.LoadableParametersManager;
import it.bologna.ausl.middelmine.managers.configuration.ParametersManager;

/**
 *
 * @author Salo
 */
public class ParametersManagerFactory {

    public static ParametersManagerInterface getAutowiredParametersManager() {
        return new ParametersManager();
    }

    public static ParametersManagerInterface getLoadableParametersManager() {
        return new LoadableParametersManager();
    }

}
