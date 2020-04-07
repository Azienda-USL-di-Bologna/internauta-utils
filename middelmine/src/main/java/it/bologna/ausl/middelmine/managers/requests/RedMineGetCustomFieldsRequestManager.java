package it.bologna.ausl.middelmine.managers.requests;

import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.managers.RedmineRequestManager;
import java.util.Map;
import okhttp3.HttpUrl;

/**
 *
 * @author Salo
 */
public class RedMineGetCustomFieldsRequestManager extends RedmineRequestManager {

    public RedMineGetCustomFieldsRequestManager() {
    }

    public RedMineGetCustomFieldsRequestManager(ParametersManagerInterface parametersManager) {
        setParametersManager(parametersManager);
    }

    public void prepareRequest() {
        String redmineCustomFieldPath = pm.getRedmineBaseUrl() + pm.getGetCustomFieldPath();
        Map<String, String> defaultHeaders = getRequestDefaultHeaders();
        HttpUrl url = builderFactory.getHttpBuilder().buildHttpUrl(redmineCustomFieldPath, null);
        request = builderFactory.getRequestBuilder().buildGetRequest(url, defaultHeaders);
    }

}
