package it.bologna.ausl.internauta.utils.firma.data.remota.namirialsignservice;

import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;

/**
 *
 * @author gdm
 */
public class NamirialUserInformation extends UserInformation {
    
    public static enum ModalitaFirma {
        OTP, AUTOMATICA
    };
    
    private ModalitaFirma modalitaFirma;
    private String username;
    private String password;
    private String otp;
    private Boolean useSavedCredential;
    
    public NamirialUserInformation() {
    }
    
    public NamirialUserInformation(final String username, final String password, final String otp, final ModalitaFirma modalitaFirma, final Boolean useSavedCredential) {
        this.username = username;
        this.password = password;
        this.otp = otp;
        this.modalitaFirma = modalitaFirma;
        this.useSavedCredential = useSavedCredential;
    }
    
    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Boolean useSavedCredential() {
        return useSavedCredential;
    }
 
    public void setUseSavedCredential(Boolean useSavedCredential) {
        this.useSavedCredential = useSavedCredential;
    }
    
    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public ModalitaFirma getModalitaFirma() {
        return modalitaFirma;
    }

    public void setModalitaFirma(final ModalitaFirma modalitaFirma) {
        this.modalitaFirma = modalitaFirma;
    }

}
