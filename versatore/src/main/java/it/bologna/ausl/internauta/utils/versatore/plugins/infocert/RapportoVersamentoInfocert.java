package it.bologna.ausl.internauta.utils.versatore.plugins.infocert;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.bologna.ausl.utils.versatore.infocert.wsclient.DocumentStatus;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
 @JsonInclude(JsonInclude.Include.NON_NULL)
public class RapportoVersamentoInfocert {
    
    private String hash;
    
    private DocumentStatus documentStatus;

    public RapportoVersamentoInfocert() {
    }
    
    public RapportoVersamentoInfocert(String hash) {
        this.hash = hash;
    }

    public RapportoVersamentoInfocert(String hash, DocumentStatus documentStatus) {
        this.hash = hash;
        this.documentStatus = documentStatus;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public DocumentStatus getDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(DocumentStatus documentStatus) {
        this.documentStatus = documentStatus;
    }    
}
