/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.middelmine.managers.requests;

import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.managers.RedmineRequestManager;
import java.util.HashMap;
import java.util.Map;
import okhttp3.HttpUrl;

/**
 *
 * @author Salo
 */
public class RedmineGetIssueInfoRequest extends RedmineRequestManager {

    public RedmineGetIssueInfoRequest(ParametersManagerInterface parametersManager) {
        setParametersManager(parametersManager);
    }

    @Override
    public void prepareRequest(Object object) {
        Integer idIssue = (Integer) object;
        String redmineIssueInfoPath = pm.getRedmineBaseUrl() + pm.getIssueInfoPath() + idIssue.toString() + ".json";
        Map<String, String> defaultHeaders = getRequestDefaultHeaders();
        HttpUrl url = builderFactory.getHttpBuilder().buildHttpUrl(redmineIssueInfoPath, null);
        request = builderFactory.getRequestBuilder().buildGetRequest(url, defaultHeaders);
    }

}
