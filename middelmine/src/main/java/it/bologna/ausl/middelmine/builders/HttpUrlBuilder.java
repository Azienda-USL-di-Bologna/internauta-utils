/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.middelmine.builders;

import it.bologna.ausl.middelmine.interfaces.BuilderInterface;
import java.util.Map;
import okhttp3.HttpUrl;

/**
 *
 * @author Salo
 */
public class HttpUrlBuilder implements BuilderInterface {

    public HttpUrl buildHttpUrl(String url, Map<String, String> queryParameters) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (queryParameters != null) {
            for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                urlBuilder = urlBuilder.addQueryParameter(key, val);
            }
        }
        return urlBuilder.build();
    }

    @Override
    public Object build(Object... object) {
        return new Object();
    }
}
