package it.bologna.ausl.internauta.utils.firma.remota.data;

import java.util.List;

/**
 *
 * @author gdm
 */
public class FirmaRemotaInformation {

    public static enum FirmaRemotaProviders {
        ARUBA, INFOCERT
    };

    private List<FirmaRemotaFile> files;
    private UserInformation userInformation;
    private FirmaRemotaProviders provider; // legacy, da tenere fino a quando si usano le app inde, poi toglierlo

    public FirmaRemotaInformation() {
    }

    public FirmaRemotaInformation(List<FirmaRemotaFile> files) {
        this.files = files;
    }
    
    public FirmaRemotaInformation(List<FirmaRemotaFile> files, UserInformation userInformation) {
        this.files = files;
        this.userInformation = userInformation;
//        this.provider = provider;
    }
    
    public FirmaRemotaInformation(List<FirmaRemotaFile> files, UserInformation userInformation, FirmaRemotaProviders provider) {
        this.files = files;
        this.userInformation = userInformation;
        this.provider = provider;
    }

    public List<FirmaRemotaFile> getFiles() {
        return files;
    }

    public void setFiles(List<FirmaRemotaFile> files) {
        this.files = files;
    }

    public UserInformation getUserInformation() {
        return userInformation;
    }

    public void setUserInformation(UserInformation userInformation) {
        this.userInformation = userInformation;
    }

    public FirmaRemotaProviders getProvider() {
        return provider;
    }

    public void setProvider(FirmaRemotaProviders provider) {
        this.provider = provider;
    }

}
