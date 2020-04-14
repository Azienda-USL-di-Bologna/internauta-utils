package it.bologna.ausl.middelmine.factories;

import it.bologna.ausl.middelmine.builders.HttpUrlBuilder;
import it.bologna.ausl.middelmine.builders.RequestBodyBuilder;
import it.bologna.ausl.middelmine.builders.RequestBuilder;

/**
 *
 * @author Salo
 */
public class BuilderFactory {

    public static HttpUrlBuilder getHttpBuilder() {
        return new HttpUrlBuilder();
    }

    public static RequestBuilder getRequestBuilder() {
        return new RequestBuilder();
    }

    public static RequestBodyBuilder getRequestBodyBuilder() {
        return new RequestBodyBuilder();
    }
}
