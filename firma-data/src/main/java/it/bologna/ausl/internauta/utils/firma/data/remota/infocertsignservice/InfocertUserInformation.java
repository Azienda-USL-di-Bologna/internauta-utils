package it.bologna.ausl.internauta.utils.firma.data.remota.infocertsignservice;

import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class InfocertUserInformation extends UserInformation {

    public static enum ModalitaFirma {
        OTP, AUTOMATICA
    };

    private String alias;
    private String pin;
    private String token;
    private ModalitaFirma modalitaFirma;
    private Boolean useSavedCredential;

    public InfocertUserInformation() {
    }

    public InfocertUserInformation(final String alias, final String pin, final String token, final ModalitaFirma modalitaFirma, final Boolean useSavedCredential) {
        this.alias = alias;
        this.pin = pin;
        this.token = token;
        this.modalitaFirma = modalitaFirma;
        this.useSavedCredential = useSavedCredential;
    }
    
       @Override
    public String getUsername() {
        return alias;
    }

    @Override
    public String getPassword() {
        return pin;
    }

    @Override
    public Boolean useSavedCredential() {
        return useSavedCredential;
    }

    public void setUsername(final String username) {
        this.alias = username;
    }

    public void setPassword(final String pin) {
        this.pin = pin;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public ModalitaFirma getModalitaFirma() {
        return modalitaFirma;
    }

    public void setModalitaFirma(final ModalitaFirma modalitaFirma) {
        this.modalitaFirma = modalitaFirma;
    }

    public Boolean getUseSavedCredential() {
        return useSavedCredential;
    }

    public void setUseSavedCredential(Boolean useSavedCredential) {
        this.useSavedCredential = useSavedCredential;
    }
}
