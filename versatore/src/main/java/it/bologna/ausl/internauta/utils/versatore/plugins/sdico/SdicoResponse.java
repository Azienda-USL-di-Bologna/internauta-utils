/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.sdico;

/**
 *
 * @author boria
 */
public class SdicoResponse {
    
    private String responseCode;
    
    private String errorMessage;
    
    private String idConservazione;
    
    private String numDocVersati;
    
    private String numDocNonVersati;
    
    private String sizeTot;
    
    private String stackTrace;
    
    private String timeTot;

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getIdConservazione() {
        return idConservazione;
    }

    public void setIdConservazione(String idConservazione) {
        this.idConservazione = idConservazione;
    }

    public String getNumDocVersati() {
        return numDocVersati;
    }

    public void setNumDocVersati(String numDocVersati) {
        this.numDocVersati = numDocVersati;
    }

    public String getNumDocNonVersati() {
        return numDocNonVersati;
    }

    public void setNumDocNonVersati(String numDocNonVersati) {
        this.numDocNonVersati = numDocNonVersati;
    }

    public String getSizeTot() {
        return sizeTot;
    }

    public void setSizeTot(String sizeTot) {
        this.sizeTot = sizeTot;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getTimeTot() {
        return timeTot;
    }

    public void setTimeTot(String timeTot) {
        this.timeTot = timeTot;
    }
    
    

    @Override
    public String toString() {
        return "SdicoResponse{" + "responseCode=" + responseCode + ", errorMessage=" + errorMessage + ", idConservazione=" + idConservazione + ", numDocVersati=" + numDocVersati + ", numDocNonVersati=" + numDocNonVersati + '}';
    }
    
    
    
}
