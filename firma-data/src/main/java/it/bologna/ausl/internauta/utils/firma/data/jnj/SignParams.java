package it.bologna.ausl.internauta.utils.firma.data.jnj;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.firma.data.exceptions.SignParamsException;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParamsComponent.EndSign;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParamsComponent.SignDocument;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author gdm
 */
public class SignParams {
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    private String serverUrl;
    private String signSessionId;
    private String userId;
    private Boolean testMode;
    private String signedFileUploaderUrl;
    private EndSign endSign;
    private List<SignDocument> signFileList;

    public SignParams() {
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getSignSessionId() {
        return signSessionId;
    }

    public void setSignSessionId(String signSessionId) {
        this.signSessionId = signSessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean getTestMode() {
        return testMode;
    }

    public void setTestMode(Boolean testMode) {
        this.testMode = testMode;
    }

    public String getSignedFileUploaderUrl() {
        return signedFileUploaderUrl;
    }

    public void setSignedFileUploaderUrl(String signedFileUploaderUrl) {
        this.signedFileUploaderUrl = signedFileUploaderUrl;
    }

    public EndSign getEndSign() {
        return endSign;
    }

    public void setEndSign(EndSign endSign) {
        this.endSign = endSign;
    }

    public List<SignDocument> getSignFileList() {
        return signFileList;
    }

    public void setSignFileList(List<SignDocument> signFileList) {
        this.signFileList = signFileList;
    }
    
    @JsonIgnore
    public Map<String, Object> toMap() throws SignParamsException {
        Map<String, Object> res = new HashMap<>();
        try {
            Field[] declaredFields = getClass().getDeclaredFields();
            for (Field declaredField : declaredFields) {
                res.put(declaredField.getName(), declaredField.get(this));
            }
        } catch (Exception ex) {
            throw new SignParamsException("errore nella trasformazione dei signparams in mappa", ex);
        }
        return res;
    }
    
    public String toJsonString() throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(this);
    }
    
    public byte[] toJsonByte() throws JsonProcessingException {
        return this.objectMapper.writeValueAsBytes(this);
    }
}
