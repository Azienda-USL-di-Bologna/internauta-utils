package it.bologna.ausl.internauta.utils.sendintegration.model;

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
        super();
        this
            .documentoId(documento.getDocumentoId())
            .inputFileName(documento.getInputFileName())
            .inputFileHash(documento.getInputFileHash());
        this.repositoryId = repositoryId;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }
}
