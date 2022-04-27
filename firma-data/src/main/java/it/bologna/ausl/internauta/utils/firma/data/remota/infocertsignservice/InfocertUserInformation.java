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

    public InfocertUserInformation() {
    }

    public InfocertUserInformation(final String alias, final String pin, final String token, final ModalitaFirma modalitaFirma) {
        this.alias = alias;
        this.pin = pin;
        this.token = token;
        this.modalitaFirma = modalitaFirma;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(final String username) {
        this.alias = username;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(final String pin) {
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
}
