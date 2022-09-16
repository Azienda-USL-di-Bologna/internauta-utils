package it.bologna.ausl.internauta.utils.firma.data.jnj;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    
    public static enum CertificateStatus {GOOD, UNKNOWN, REVOKED, EXPIRED, NOT_YET_VALID};
    
    private String serverUrl;
    private String signSessionId;
    private String userId;
    private Boolean testMode;
    private String signedFileUploaderUrl;
    private Boolean signWithCertificateProblem;
    private String checkCertificateUrl;
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

    public Boolean getSignWithCertificateProblem() {
        return signWithCertificateProblem;
    }

    public void setSignWithCertificateProblem(Boolean signWithCertificateProblem) {
        this.signWithCertificateProblem = signWithCertificateProblem;
    }

    public String getCheckCertificateUrl() {
        return checkCertificateUrl;
    }

    public void setCheckCertificateUrl(String checkCertificateUrl) {
        this.checkCertificateUrl = checkCertificateUrl;
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
                if (declaredField.getAnnotation(JsonIgnore.class) == null) {
                    res.put(declaredField.getName(), declaredField.get(this));
                }
            }
        } catch (Exception ex) {
            throw new SignParamsException("errore nella trasformazione dei signparams in mappa", ex);
        }
        return res;
    }
    
    public static SignParams parse(String str) throws JsonProcessingException {
        return SignParamsComponent.getObjectMapper().readValue(str, SignParams.class);
    }
    
    public String toJsonString() throws JsonProcessingException {
        return SignParamsComponent.getObjectMapper().writeValueAsString(this);
    }
    
    public byte[] toJsonByte() throws JsonProcessingException {
        return SignParamsComponent.getObjectMapper().writeValueAsBytes(this);
    }
}
