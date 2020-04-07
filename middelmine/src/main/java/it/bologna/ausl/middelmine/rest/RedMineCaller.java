package it.bologna.ausl.middelmine.rest;

import it.bologna.ausl.middelmine.exceptions.RedmineCallerException;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.interfaces.RequestManagerInterface;
import it.bologna.ausl.middelmine.managers.HttpClientManager;
import okhttp3.Response;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 *
 * @author Salo
 */
public class RedMineCaller {
    
    private ParametersManagerInterface pm;
    
    public RedMineCaller() {
        
    }
    
    public RedMineCaller(ParametersManagerInterface parametersManager) {
        pm = parametersManager;
    }
    
    public JSONObject doCall(RequestManagerInterface requestManager) throws RedmineCallerException {
        JSONObject jo = new JSONObject();
        try {
            HttpClientManager httpClientManager = new HttpClientManager();
            Response response = httpClientManager.call(requestManager.getRequest());
            if (response.isSuccessful()) {
                System.out.println("OK!");
            } else {
                System.out.println("Errore!");
                throw new Exception("Errore nell'eseguire la richiesta: "
                        + HttpStatus.valueOf(response.code()) + "\n"
                        + response.message() + "\n"
                        + new JSONObject(response.body().string()).toString(4));
            }
            jo.put("Result", response.code());
            jo.append("Message", response.message());
            jo.append("FullResponse", new JSONObject(response.body().string()));
            System.out.println(jo.toString(4));
        } catch (Exception ex) {
            throw new RedmineCallerException("Errore nel RedMineCaller: " + ex.getMessage());
        }
        return jo;
    }
    
}
