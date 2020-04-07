/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.middelmine.builders;

import it.bologna.ausl.middelmine.interfaces.BuilderInterface;
import java.util.Map;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 *
 * @author Salo
 */
public class RequestBuilder implements BuilderInterface {

    public Request buildPostRequest(String url, RequestBody body, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder().url(url);

        // ciclo gli headers da aggiungere
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            builder.addHeader(key, val);
        }

        builder = builder.post(body);

        return builder.build();
    }

    public Request buildGetRequest(HttpUrl url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            builder = builder.header(key, val);
        }
        Request request = builder.url(url).build();
        return request;
    }

    @Override
    public Object build(Object... object) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
