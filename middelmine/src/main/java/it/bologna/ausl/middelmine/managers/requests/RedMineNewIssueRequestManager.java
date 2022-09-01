package it.bologna.ausl.middelmine.managers.requests;

import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.managers.RedmineRequestManager;
import java.util.Map;
import okhttp3.HttpUrl;
import okhttp3.RequestBody;

/**
 *
 * @author Salo
 */
public class RedMineNewIssueRequestManager extends RedmineRequestManager {

    public RedMineNewIssueRequestManager() {

    }

    public RedMineNewIssueRequestManager(ParametersManagerInterface parametersManager) {
        setParametersManager(parametersManager);
    }

    public void prepareRequest(Object issue) {
        String redmineNewIssuePath = pm.getRedmineBaseUrl() + pm.getNewIssuePaht();
        Map<String, String> defaultHeaders = getRequestDefaultHeaders();
        HttpUrl url = builderFactory.getHttpBuilder().buildHttpUrl(redmineNewIssuePath, null);
        RequestBody requestBody = builderFactory.getRequestBodyBuilder().createJSONRequestBody((String) issue);
        request = builderFactory.getRequestBuilder().buildPostRequest(redmineNewIssuePath, requestBody, defaultHeaders);
    }

}
