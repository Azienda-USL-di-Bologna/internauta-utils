/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.middelmine.managers.requests;

import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.managers.RedmineRequestManager;
import java.util.Map;
import okhttp3.HttpUrl;

/**
 *
 * @author Salo
 */
public class RedMineGetTrackersRequestManager extends RedmineRequestManager {

    public RedMineGetTrackersRequestManager() {

    }

    public RedMineGetTrackersRequestManager(ParametersManagerInterface parametersManager) {
        setParametersManager(parametersManager);
    }

    public void prepareRequest() {
        String redmineTrackersPath = pm.getRedmineBaseUrl() + pm.getGetTrackersFieldPath();
        HttpUrl url = builderFactory.getHttpBuilder().buildHttpUrl(redmineTrackersPath, null);
        Map<String, String> defaultHeaders = getRequestAdminDefaultHeaders();
        request = builderFactory.getRequestBuilder().buildGetRequest(url, defaultHeaders);
    }
}
