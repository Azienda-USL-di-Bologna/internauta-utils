package it.bologna.ausl.middelmine.managers.requests;

import java.util.Map;
import it.bologna.ausl.middelmine.managers.RedmineRequestManager;
import okhttp3.HttpUrl;
import okhttp3.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import java.io.IOException;
import java.util.HashMap;

/**
 *
 * @author Salo
 */
public class RedMineNewAttachmentRequestManager extends RedmineRequestManager {

    public RedMineNewAttachmentRequestManager(ParametersManagerInterface parametersManager) {
        setParametersManager(parametersManager);
    }

    @Override
    public void prepareRequest(Object object) {
        MultipartFile allegato = (MultipartFile) object;
        String redmineNewAttachmentPath = pm.getRedmineBaseUrl() + pm.getNewAttachmentPath();
        Map<String, String> defaultHeaders = getNewAttachmentRequestHeaders();
        Map<String, String> queryParams = new HashMap();
        queryParams.put("filename", allegato.getOriginalFilename());

        HttpUrl url = builderFactory.getHttpBuilder().buildHttpUrl(redmineNewAttachmentPath, queryParams);
        RequestBody requestBody = null;
        try {
            requestBody = builderFactory.getRequestBodyBuilder().createOctetStreamRequestBody(allegato.getBytes());
        } catch (IOException ex) {
            //
        }
        request = builderFactory.getRequestBuilder().buildPostRequest(url.toString(), requestBody, defaultHeaders);
    }

}
