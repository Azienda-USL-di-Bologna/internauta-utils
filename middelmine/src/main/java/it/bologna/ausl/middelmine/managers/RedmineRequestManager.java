package it.bologna.ausl.middelmine.managers;

import it.bologna.ausl.middelmine.factories.BuilderFactory;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.interfaces.RequestManagerInterface;
import it.bologna.ausl.middelmine.rest.RedMineCaller;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
//@Component
public abstract class RedmineRequestManager implements RequestManagerInterface {

    protected ParametersManagerInterface pm;

    //@Autowired
    protected RedMineCaller rmc;

    protected BuilderFactory builderFactory;

    protected Request request;

    public RedmineRequestManager() {
    }

    public RedmineRequestManager(ParametersManagerInterface parametersManager) {
        pm = parametersManager;
    }

    @Override
    public void prepareRequest() {
        // abstract class: not implemented
    }

    @Override
    public void prepareRequest(Object object) {
        // abstract class: not implemented
    }

    protected Map<String, String> getRequestDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(pm.getRedmineApiKeyHeader(), pm.getPrivateApiKey());
        headers.put(pm.getContentTypeHeader(), pm.getApplicationJsonHeaderValue());
        return headers;
    }

    protected Map<String, String> getRequestAdminDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(pm.getRedmineApiKeyHeader(), pm.getAdminApiKey());
        headers.put(pm.getContentTypeHeader(), pm.getApplicationJsonHeaderValue());
        return headers;
    }

    protected Map<String, String> getNewAttachmentRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(pm.getRedmineApiKeyHeader(), pm.getPrivateApiKey());
        headers.put(pm.getContentTypeHeader(), pm.getApplicationOctetHeaderValue());
        return headers;
    }

    public Request getRequest() {
        return request;
    }

    public void setParametersManager(ParametersManagerInterface pm) {
        this.pm = pm;
    }

}
