package it.bologna.ausl.internauta.utils.firma.data.remota.medassignservice;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaUserSign;
import java.util.List;

/**
 *
 * @author gdm
 */
public class MedasUserSign extends FirmaRemotaUserSign {
    public static enum SignType {
        FEA, FD, FDA
    }

    public static enum OTPType {
        SMS, ARUBACALL, C100, YUBICO8, APP
    }

    private String signPowerCode;
    private String certificateId;
    private String description;
    private String processId;
    private List<String> docTypes;
    private boolean active;
    private SignType signType;
    private List<OTPType> otpType;
    private boolean defaultSelection;

    public MedasUserSign() {
    }

    public MedasUserSign(String signPowerCode, String certificateId, String description, String processId, List<String> docTypes, boolean active, SignType signType, List<OTPType> otpType, boolean defaultSelection) {
        this.signPowerCode = signPowerCode;
        this.certificateId = certificateId;
        this.description = description;
        this.processId = processId;
        this.docTypes = docTypes;
        this.active = active;
        this.signType = signType;
        this.otpType = otpType;
        this.defaultSelection = defaultSelection;
    }

    public String getSignPowerCode() {
        return signPowerCode;
    }

    public void setSignPowerCode(String signPowerCode) {
        this.signPowerCode = signPowerCode;
    }

    public String getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public List<String> getDocTypes() {
        return docTypes;
    }

    public void setDocTypes(List<String> docTypes) {
        this.docTypes = docTypes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public SignType getSignType() {
        return signType;
    }

    public void setSignType(SignType signType) {
        this.signType = signType;
    }

    public List<OTPType> getOtpType() {
        return otpType;
    }

    public void setOtpType(List<OTPType> otpType) {
        this.otpType = otpType;
    }

    public boolean isDefaultSelection() {
        return defaultSelection;
    }

    public void setDefaultSelection(boolean defaultSelection) {
        this.defaultSelection = defaultSelection;
    }

    @Override
    @JsonIgnore
    public String getId() {
        return String.format("%s - %s", getCertificateId(), getSignPowerCode());
    }
}
