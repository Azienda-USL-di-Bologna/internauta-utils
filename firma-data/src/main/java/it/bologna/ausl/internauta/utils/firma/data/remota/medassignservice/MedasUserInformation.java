package it.bologna.ausl.internauta.utils.firma.data.remota.medassignservice;

import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaModalitaFirma;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;

/**
 *
 * @author gdm
 */
public class MedasUserInformation extends UserInformation {
    
    public static enum ModalitaFirma implements FirmaRemotaModalitaFirma {
        AUTO_DETECT;

        @Override
        public FirmaRemotaModalitaFirma getAutodetectValue() {
            return AUTO_DETECT;
        }
    };
    
    private ModalitaFirma modalitaFirma;
    private String username;
    private String password;
    private String nome;
    private String cognome;
    private String codiceFiscale;
    private String otp;
    private Boolean useSavedCredential;
    
    public MedasUserInformation() {
    }

    public MedasUserInformation(ModalitaFirma modalitaFirma, String username, String password, String nome, String cognome, String codiceFiscale, String otp, Boolean useSavedCredential) {
        this.modalitaFirma = modalitaFirma;
        this.username = username;
        this.password = password;
        this.nome = nome;
        this.cognome = cognome;
        this.codiceFiscale = codiceFiscale;
        this.otp = otp;
        this.useSavedCredential = useSavedCredential;
    }
    
    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
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

    @Override
    public FirmaRemotaModalitaFirma getModalitaFirma() {
        return modalitaFirma;
    }

    public void setModalitaFirma(final ModalitaFirma modalitaFirma) {
        this.modalitaFirma = modalitaFirma;
    }

}
