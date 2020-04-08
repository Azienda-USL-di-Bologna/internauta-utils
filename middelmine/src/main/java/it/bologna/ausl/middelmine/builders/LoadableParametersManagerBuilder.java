/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.middelmine.builders;

import it.bologna.ausl.middelmine.configuration.ApplicationParamsReader;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.managers.configuration.LoadableParametersManager;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author Salo
 */
public class LoadableParametersManagerBuilder {

    public static void buildParams(LoadableParametersManager pm) throws Exception {
        try {
            ApplicationParamsReader apr = new ApplicationParamsReader();
            Properties propValues = apr.getPropValues();
            System.out.println(propValues.toString());
            pm.setTestMode(Boolean.parseBoolean(propValues.getProperty("redmine-test-mode")));
            pm.setRedmineBaseUrl((String) propValues.get("redmine-base-url"));
            pm.setPrivateApiKey((String) propValues.get("api-key"));
            pm.setAdminApiKey((String) propValues.get("admin-api-key"));
            pm.setNewIssuePaht((String) propValues.get("new-issue-path"));
            pm.setNewAttachmentPath((String) propValues.get("new-attachment-path"));
            pm.setGetCustomFieldPath((String) propValues.get("get-custom-fields-path"));
            pm.setGetTrackersPath((String) propValues.get("get-trackers-path"));
            pm.setIssueInfoPath((String) propValues.get("issue-info-path"));
            pm.setRedmineApiKeyHeader((String) propValues.get("x-redmine-api-key-header"));
            pm.setContentTypeHeader((String) propValues.get("content-type-header"));
            pm.setApplicationJsonHeaderValue((String) propValues.get("application-json-header-value"));
            pm.setApplicationOctetHeaderValue((String) propValues.get("application-octet-stream-header-value"));
        } catch (Exception ex) {
            throw new Exception("Errore nel build dei parametri: " + ex.getMessage(), ex);
        }
    }

}
