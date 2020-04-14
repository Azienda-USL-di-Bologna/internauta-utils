package it.bologna.ausl.middelmine.managers;

import it.bologna.ausl.middelmine.exceptions.HttpCallException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 *
 * @author Salo
 */
public class HttpClientManager {

    public OkHttpClient getNewHttpClient() {
        OkHttpClient client = new OkHttpClient();
        return client;
    }

    public Response call(Request requestg) throws HttpCallException {
        OkHttpClient client = getNewHttpClient();
        Response responseg = null;
        try {
            responseg = client.newCall(requestg).execute();
        } catch (Exception ex) {
            throw new HttpCallException("ERRORE in HttpClientManager.call(): " + ex.getMessage());
        }
        return responseg;
    }
}
