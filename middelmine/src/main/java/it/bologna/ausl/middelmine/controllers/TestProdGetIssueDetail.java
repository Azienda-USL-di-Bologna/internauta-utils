package it.bologna.ausl.middelmine.controllers;

import it.bologna.ausl.middelmine.builders.LoadableParametersManagerBuilder;
import it.bologna.ausl.middelmine.factories.ParametersManagerFactory;
import it.bologna.ausl.middelmine.factories.RedMineRequestManagerFactory;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.interfaces.RequestManagerInterface;
import it.bologna.ausl.middelmine.managers.HttpClientManager;
import it.bologna.ausl.middelmine.managers.configuration.LoadableParametersManager;
import java.io.IOException;
import okhttp3.Response;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Salo
 */
//@Controller
public class TestProdGetIssueDetail {

    //@RequestMapping(value = "/getIssue", method = RequestMethod.GET)
    public ResponseEntity<String> getIssue(@RequestParam("idIssue") Integer idIssue) throws IOException, Exception {
        System.out.println("dentro ResponseEntity newTest() ");
        ParametersManagerInterface loadableParametersManager = ParametersManagerFactory.getLoadableParametersManager();
        LoadableParametersManagerBuilder.buildParams((LoadableParametersManager) loadableParametersManager);
        RequestManagerInterface requestManager = (RequestManagerInterface) RedMineRequestManagerFactory.getIssueInfo(loadableParametersManager);
        requestManager.prepareRequest(idIssue);
        HttpClientManager httpClientManager = new HttpClientManager();
        Response response = httpClientManager.call(requestManager.getRequest());
        JSONObject jo = new JSONObject();
        if (response.isSuccessful()) {
            jo.append("Result", "Success!!!");
            jo.append("CustomFields", new JSONObject(response.body().string()));
            System.out.println("Oh yeah!");
        } else {
            throw new Exception("errore nella creazione della segnalazione redmine");
        }
        return new ResponseEntity(jo.toString(4), HttpStatus.OK);
    }
}
