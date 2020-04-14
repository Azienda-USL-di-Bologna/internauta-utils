package it.bologna.ausl.middelmine.controllers;

import it.bologna.ausl.middelmine.builders.LoadableParametersManagerBuilder;
import it.bologna.ausl.middelmine.factories.ParametersManagerFactory;
import it.bologna.ausl.middelmine.managers.configuration.ParametersManager;
import it.bologna.ausl.middelmine.rest.RedMineCaller;
import java.io.IOException;
import okhttp3.Response;
import it.bologna.ausl.middelmine.factories.RedMineCallerFactory;
import it.bologna.ausl.middelmine.factories.RedMineCallerManagerFactory;
import it.bologna.ausl.middelmine.factories.RedMineRequestManagerFactory;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.interfaces.RequestManagerInterface;
import it.bologna.ausl.middelmine.managers.HttpClientManager;
import it.bologna.ausl.middelmine.managers.configuration.LoadableParametersManager;
import it.bologna.ausl.middelmine.rest.RedMineCallManager;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Salo
 */
//@Controller
public class NewIssueController {

    //@Autowired
    ParametersManager pm;

    //@RequestMapping(value = "/newissue", method = RequestMethod.POST)
    public ResponseEntity<String> newIssue(@RequestPart("issueParams") String issue,
            @RequestPart("allegati") Optional<List<MultipartFile>> allegati, HttpServletRequest request) throws IOException, Exception {
        System.out.println("Entrato dentro newIssue()");
        System.out.println(issue);
        List<MultipartFile> allegatiList = allegati.orElse(Collections.emptyList());
        ParametersManagerInterface loadableParametersManager = ParametersManagerFactory.getLoadableParametersManager();
        LoadableParametersManagerBuilder.buildParams((LoadableParametersManager) loadableParametersManager);
        RedMineCallManager redMineCallerManager = RedMineCallerManagerFactory.getRedMineCallerManager(loadableParametersManager);
        ResponseEntity<String> postNewIssue = redMineCallerManager.postNewIssue(issue, allegatiList);
        return postNewIssue;
    }

    //@RequestMapping(value = "/newtest", method = RequestMethod.GET)
    public ResponseEntity<String> newTest() throws IOException, Exception {
        System.out.println("dentro ResponseEntity newTest() ");
        RequestManagerInterface requestManager = (RequestManagerInterface) RedMineRequestManagerFactory.getGetCustomFieldsRequestManager(pm);
        requestManager.prepareRequest();
        HttpClientManager httpClientManager = new HttpClientManager();
        Response response = httpClientManager.call(requestManager.getRequest());
        JSONObject jo = new JSONObject();
        if (response.isSuccessful()) {
            jo.append("Result", "Success!!!");
            jo.append("CustomFields", new JSONObject(response.body().string()));
            System.out.println("Oh yeah!");
        } else {
            throw new Exception("errore nella chiamata a redmine " + new JSONObject(response.body().string()).toString(4));
        }
        return new ResponseEntity(jo.toString(4), HttpStatus.OK);

    }

    //@RequestMapping(value = "/newattachmenttest", method = RequestMethod.POST)
    public ResponseEntity<String> newattachmenttest(@RequestPart("allegati") Optional<List<MultipartFile>> allegati, HttpServletRequest request) throws IOException, Exception {
        List<MultipartFile> allegatiList = allegati.orElse(Collections.emptyList());
        JSONObject jo = new JSONObject();
        if (allegatiList != null) {
            for (MultipartFile allegato : allegatiList) {
                JSONObject singleAttchmentJsonResponse = new JSONObject();
                try {
                    RedMineCaller redMineCaller = RedMineCallerFactory.getRedMineCaller(pm);
                    RequestManagerInterface requestManager = RedMineRequestManagerFactory.getNewAttachmentRequestManager(pm);
                    requestManager.prepareRequest(allegato);
                    singleAttchmentJsonResponse.put("name", allegato.getOriginalFilename());
                    singleAttchmentJsonResponse.put("size", allegato.getSize());
                    singleAttchmentJsonResponse.put("contentType", allegato.getContentType());
                    singleAttchmentJsonResponse.put("redmineFile", redMineCaller.doCall(requestManager));
                } catch (Exception ex) {
                    throw ex;
                }
                System.out.println("un allegato:\n" + singleAttchmentJsonResponse.toString(4));
            }
        }
        jo.put("Res", "tutto ok");
        return new ResponseEntity(null, HttpStatus.OK);
    }
}
