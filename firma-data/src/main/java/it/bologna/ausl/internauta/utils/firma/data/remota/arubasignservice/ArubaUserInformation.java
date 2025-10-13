package it.bologna.ausl.internauta.utils.firma.data.remota.arubasignservice;

import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaModalitaFirma;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;

/**
 *
 * @author gdm
 */
public class ArubaUserInformation extends UserInformation {

    public static enum ModalitaFirma implements FirmaRemotaModalitaFirma {
        ARUBACALL, OTP, APP;

        @Override
        public FirmaRemotaModalitaFirma getAutodetectValue() {
            return null;
        }
    };
    
    private String username;
    private String password;
    private String token;
    private String certId; // rappresenta l'id del certificato di firma remota dell'utente. Non sappiamo se servirà
    private String dominioFirma; // rappresenta il dominio della firma (es. frAUSLBO)
    private Boolean useSavedCredential;
    private ModalitaFirma modalitaFirma;
    private Boolean firmaDelegata = false; // se true indica che la firma sarà automatica, quindi non sarà richiesto l'otp all'utente
    

    public ArubaUserInformation() {
    }

    public ArubaUserInformation(String username, String password, String token, ModalitaFirma modalitaFirma, String certId, String dominioFirma, Boolean useSavedCredential) {
        this(username, password, token, modalitaFirma, certId, dominioFirma, useSavedCredential, false);
    }
    
    public ArubaUserInformation(String username, String password, String token, ModalitaFirma modalitaFirma, String certId, String dominioFirma, Boolean useSavedCredential, Boolean firmaDelegata) {
        this.username = username;
        this.password = password;
        this.token = token;
        this.modalitaFirma = modalitaFirma;
        this.certId = certId;
        this.dominioFirma = dominioFirma;
        this.useSavedCredential = useSavedCredential;
        this.firmaDelegata = firmaDelegata;
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
    
    
    @Override
    public ModalitaFirma getModalitaFirma() {
        return modalitaFirma;
    }

    public void setModalitaFirma(ModalitaFirma modalitaFirma) {
        this.modalitaFirma = modalitaFirma;
    }

    public void setUseSavedCredential(Boolean useSavedCredential) {
        this.useSavedCredential = useSavedCredential;
    }
    
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCertId() {
        return certId;
    }

    public void setCertId(String certId) {
        this.certId = certId;
    }

    public String getDominioFirma() {
        return dominioFirma;
    }

    public void setDominioFirma(String dominioFirma) {
        this.dominioFirma = dominioFirma;
    }

    public Boolean getFirmaDelegata() {
        return firmaDelegata;
    }

    public void setFirmaDelegata(Boolean firmaDelegata) {
        this.firmaDelegata = firmaDelegata;
    }
}
