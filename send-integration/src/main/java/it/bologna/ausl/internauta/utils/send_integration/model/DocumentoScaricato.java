package it.bologna.ausl.internauta.utils.send_integration.model;

/**
 *
 * @author gdm
 */
public class DocumentoScaricato extends Documento {
    private String repositoryId;

    public DocumentoScaricato(String repositoryId) {
        this.repositoryId = repositoryId;
    }
    
    public DocumentoScaricato(Documento documento, String repositoryId) {
        super(documento.getDocumentoId(), documento.getInputFileName(), documento.getInputFileHash());
        this.repositoryId = repositoryId;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }
}
