package it.bologna.ausl.internauta.utils.firma.data.remota.arubasignservice;

import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;

/**
 *
 * @author gdm
 */
public class ArubaUserInformation extends UserInformation {

    public static enum ModalitaFirma {
        ARUBACALL, OTP, APP
    };
    private String username;
    private String password;
    private String token;
    private ModalitaFirma modalitaFirma;
    private String certId; // rappresenta l'id del certificato di firma remota dell'utente. Non sappiamo se servirà
    private String dominioFirma; // rappresenta il dominio della firma (es. frAUSLBO)

    public ArubaUserInformation() {
    }

    public ArubaUserInformation(String username, String password, String token, ModalitaFirma modalitaFirma, String certId, String dominioFirma) {
        this.username = username;
        this.password = password;
        this.token = token;
        this.modalitaFirma = modalitaFirma;
        this.certId = certId;
        this.dominioFirma = dominioFirma;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public ModalitaFirma getModalitaFirma() {
        return modalitaFirma;
    }

    public void setModalitaFirma(ModalitaFirma modalitaFirma) {
        this.modalitaFirma = modalitaFirma;
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
}
