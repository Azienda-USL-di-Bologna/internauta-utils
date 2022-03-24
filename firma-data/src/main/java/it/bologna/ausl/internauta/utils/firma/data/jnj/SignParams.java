package it.bologna.ausl.internauta.utils.firma.data.jnj;

import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParamsComponent.EndSign;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParamsComponent.SignDocument;
import java.util.List;

/**
 *
 * @author gdm
 */
public class SignParams {
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
}
