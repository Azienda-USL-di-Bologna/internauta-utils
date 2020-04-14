/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.middelmine.rest;

import it.bologna.ausl.middelmine.exceptions.PostNewIssueException;
import it.bologna.ausl.middelmine.exceptions.UploadNewAttachmentError;
import it.bologna.ausl.middelmine.factories.RedMineCallerFactory;
import it.bologna.ausl.middelmine.factories.RedMineRequestManagerFactory;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.interfaces.RequestManagerInterface;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Salo
 */
public class RedMineCallManager {

    private ParametersManagerInterface pm;

    public RedMineCallManager(ParametersManagerInterface parametersManager) {
        pm = parametersManager;
    }

    private String getFileTokenByFullResponse(JSONObject res) {
        JSONArray arr = res.getJSONArray("FullResponse");
        JSONObject o0 = arr.getJSONObject(0);
        JSONObject upload = o0.getJSONObject("upload");
        String token = upload.getString("token");
        return token;
    }

    public JSONObject uploadNewAttachment(MultipartFile allegato) throws Exception {
        JSONObject singleAttchmentJsonResponse = new JSONObject();
        try {
            RedMineCaller redMineCaller = RedMineCallerFactory.getRedMineCaller(pm);
            RequestManagerInterface requestManager = RedMineRequestManagerFactory.getNewAttachmentRequestManager(pm);
            requestManager.prepareRequest(allegato);
            JSONObject res = redMineCaller.doCall(requestManager);
            String token = getFileTokenByFullResponse(res);
            singleAttchmentJsonResponse.put("name", allegato.getOriginalFilename());
            singleAttchmentJsonResponse.put("size", allegato.getSize());
            singleAttchmentJsonResponse.put("contentType", allegato.getContentType());
            singleAttchmentJsonResponse.put("token", token);
            singleAttchmentJsonResponse.put("redmineFile", res);
        } catch (Exception ex) {
            throw new UploadNewAttachmentError("Errore nel caricare l'allegato: " + allegato.getOriginalFilename(), ex);
        }
        return singleAttchmentJsonResponse;
    }

    public JSONArray uploadNewAttachments(List<MultipartFile> allegati) throws Exception {
        JSONArray newAttachmentsResponseArray = new JSONArray();
        try {
            for (MultipartFile allegato : allegati) {
                newAttachmentsResponseArray.put(uploadNewAttachment(allegato));
            }
        } catch (Exception ex) {
            throw ex;
        }
        return newAttachmentsResponseArray;
    }

    JSONArray getUploadsJsonArrayFromAttachmentRes(JSONArray newAttachmentsArray) {
        JSONArray uploads = new JSONArray();
        for (Object obj : newAttachmentsArray) {
            JSONObject jo = new JSONObject();
            JSONObject allegato = (JSONObject) obj;
            System.out.println("Allegato\n" + allegato.toString(4));
            jo.put("token", allegato.get("token"));
            jo.put("filename", allegato.get("name"));
            jo.put("content_type", allegato.get("contentType"));
            uploads.put(jo);
        }
        return uploads;
    }

    public String syncrhonizeNewIssueWithAttachments(String issue, JSONArray newAttachmentsArray) throws Exception {
        JSONObject issueJson = new JSONObject(issue);
        JSONObject newIssueJson = new JSONObject(issue);
        JSONObject issueParamsObject = (JSONObject) issueJson.get("issue");
        JSONArray uploads = getUploadsJsonArrayFromAttachmentRes(newAttachmentsArray);
        issueParamsObject.put("uploads", uploads);
        newIssueJson.put("issue", issueParamsObject);
        System.out.println("New issueJson\n" + newIssueJson.toString(4));
        issue = newIssueJson.toString();
        System.out.println(issue);
        return issue;
    }

    private String uploadAttachmentAndGetSynchronizeIssueParams(String issue, List<MultipartFile> allegati) throws Exception {
        JSONArray uploadedAttArray = uploadNewAttachments(allegati);
        return syncrhonizeNewIssueWithAttachments(issue, uploadedAttArray);
    }

    public ResponseEntity<String> postNewIssue(String issue, List<MultipartFile> allegati) throws Exception {
        JSONObject postNewIssueJsonResponse = null;
        try {
            if (allegati != null) {
                issue = uploadAttachmentAndGetSynchronizeIssueParams(issue, allegati);
            }
            RequestManagerInterface requestManager = RedMineRequestManagerFactory.getNewIssueRequestManager(pm);
            requestManager.prepareRequest(issue);
            RedMineCaller redMineCaller = RedMineCallerFactory.getRedMineCaller(pm);
            postNewIssueJsonResponse = redMineCaller.doCall(requestManager);
        } catch (Exception ex) {
            throw new PostNewIssueException("Errore nella creazione della nuova segnalazione", ex);
        }

        int resultCode = postNewIssueJsonResponse.getInt("Result");
        HttpStatus httpStatusCode = HttpStatus.valueOf(resultCode);
        return new ResponseEntity(postNewIssueJsonResponse.toString(4), httpStatusCode);
    }
}
