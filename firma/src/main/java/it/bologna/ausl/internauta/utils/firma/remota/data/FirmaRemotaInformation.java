package it.bologna.ausl.internauta.utils.firma.remota.data;

import java.util.List;

/**
 *
 * @author gdm
 * 
 * Questa classe contiene le informazioni che servono per una sessioni di firma
 */
public class FirmaRemotaInformation {

    // elenco dei vari provider supportati
    public static enum FirmaRemotaProviders {
        ARUBA, INFOCERT
    };

    // elenco dei file da firmare
    private List<FirmaRemotaFile> files;
    
    // informazioni relative all'utenza di firma
    private UserInformation userInformation;
    
    // l'informazione del provider di firma deve essere rimosso da questa classe, perché non più necessario, 
    // per motivi legacy però, bisogna tenerli fino a quando si usano le app inde
    private FirmaRemotaProviders provider; 

    public FirmaRemotaInformation() {
    }

    public FirmaRemotaInformation(List<FirmaRemotaFile> files) {
        this.files = files;
    }
    
    public FirmaRemotaInformation(List<FirmaRemotaFile> files, UserInformation userInformation) {
        this.files = files;
        this.userInformation = userInformation;//        this.provider = provider;
    }
    
    public FirmaRemotaInformation(List<FirmaRemotaFile> files, UserInformation userInformation, FirmaRemotaProviders provider) {
        this(files, userInformation);
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
