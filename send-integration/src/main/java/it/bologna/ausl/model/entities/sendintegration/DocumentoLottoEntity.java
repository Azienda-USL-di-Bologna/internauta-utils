package it.bologna.ausl.model.entities.sendintegration;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author gdm
 */
@Entity
@Table(name = "documenti_lotto", catalog = "internauta", schema = "send_integration")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "authorities"})
@Cacheable(false)
public class DocumentoLottoEntity implements Serializable {
    
    public static enum DocumentiLottoStatus {
        SCARICATO_DA_COMUNICARE, 
        SCARICATO_COMUNICATO, 
        ELABORATO_DA_COMUNICARE, 
        ELABORATO_COMUNICATO,
        COMPLETATO
    }
    
    @Id
    @Basic(optional = false)
    @Column(name = "id")
    private UUID id = UUID.randomUUID();

    @NotNull
    @Basic(optional = false)
    @Column(name = "pa_id", columnDefinition = "text")
    private String paId;

    @NotNull
    @Basic(optional = false)
    @Column(name = "lotto_id", columnDefinition = "text")
    private String lottoId;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "timestamp")
    private ZonedDateTime timestamp;

    @NotNull
    @Basic(optional = false)
    @Column(name = "numero_documenti", columnDefinition = "integer")
    private Integer numeroDocumenti;

    @NotNull
    @Basic(optional = false)
    @Column(name = "firmatario", columnDefinition = "text")
    private String firmatario;
  
    @NotNull
    @Basic(optional = false)
    @Column(name = "documento_id", columnDefinition = "text")    
    private String documentoId;
    
    @NotNull
    @Basic(optional = false)
    @Column(name = "bucket", columnDefinition = "text")    
    private String bucket;

    // dati per il file da firmare
    @NotNull
    @Basic(optional = false)
    @Column(name = "input_base_path", columnDefinition = "text")
    private String inputBasePath;
 
    @NotNull
    @Basic(optional = false)
    @Column(name = "input_file_name", columnDefinition = "text")    
    private String inputFileName;
    
    @NotNull
    @Basic(optional = false)
    @Column(name = "input_file_sha_256", columnDefinition = "text")    
    private String inputFileSha256;
    
    @NotNull
    @Basic(optional = false)
    @Column(name = "input_file_md5", columnDefinition = "text")    
    private String inputFileMd5;    
     
    @NotNull
    @Basic(optional = false)
    @Column(name = "input_file_mime_type", columnDefinition = "text")    
    private String inputFileMimeType;
    
    @NotNull
    @Basic(optional = false)
    @Column(name = "input_file_repo_id", columnDefinition = "text")    
    private String inputFileRepoId;
    
    // dati per il file firmato
    @NotNull
    @Basic(optional = false)
    @Column(name = "output_base_path", columnDefinition = "text")
    private String outputBasePath; // questo viene passato dal Fruitore e quindi popolato in fase di scaricamento del lotto
    
    @Basic(optional = true)
    @Column(name = "output_file_name", columnDefinition = "text")    
    private String outputFileName;
    
    @Basic(optional = true)
    @Column(name = "output_file_sha_256", columnDefinition = "text")    
    private String outputFileSha256;
    
    @Basic(optional = true)
    @Column(name = "output_file_md5", columnDefinition = "text")    
    private String outputFileMd5;    
     
    @Basic(optional = true)
    @Column(name = "output_file_mime_type", columnDefinition = "text")    
    private String outputFileMimeType;
    
    @Basic(optional = true)
    @Column(name = "output_file_repo_id", columnDefinition = "text")    
    private String outputFileRepoId;
    
    // dati di registrazione
    @Basic(optional = true)
    @Column(name = "id_esterno", columnDefinition = "text")
    private String idEsterno; // qui ci mettiamo l'id del doc, magari può servire
    
    @Basic(optional = true)
    @Column(name = "numero_registrazione", columnDefinition = "text")    
    private String numeroRegistrazione;
    
    @Basic(optional = true)
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "data_registrazione")
    private ZonedDateTime dataRegistrazione;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DocumentiLottoStatus status;
    
    @NotNull
    @Basic(optional = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "insert_ts")
    private ZonedDateTime insertTs = ZonedDateTime.now();

    public DocumentoLottoEntity() {}

    public DocumentoLottoEntity(String paId, String lottoId, ZonedDateTime timestamp, Integer numeroDocumenti, String firmatario, String documentoId, String bucket, String inputBasePath, String inputFileName, String inputFileSha256, String inputFileMd5, String inputMimeType, String inputFileRepoId, String outputBasePath, String outputFileName, String outputFileSha256, String outputFileMd5, String outputMimeType, String outputFileRepoId, String idEsterno, String numeroRegistrazione, ZonedDateTime dataRegistrazione, DocumentiLottoStatus status) {
        this.paId = paId;
        this.lottoId = lottoId;
        this.timestamp = timestamp;
        this.numeroDocumenti = numeroDocumenti;
        this.firmatario = firmatario;
        this.documentoId = documentoId;
        this.bucket = bucket;
        this.inputBasePath = inputBasePath;
        this.inputFileName = inputFileName;
        this.inputFileSha256 = inputFileSha256;
        this.inputFileMd5 = inputFileMd5;
        this.inputFileMimeType = inputMimeType;
        this.inputFileRepoId = inputFileRepoId;
        this.outputBasePath = outputBasePath;
        this.outputFileName = outputFileName;
        this.outputFileSha256 = outputFileSha256;
        this.outputFileMd5 = outputFileMd5;
        this.outputFileMimeType = outputMimeType;
        this.outputFileRepoId = outputFileRepoId;
        this.idEsterno = idEsterno;
        this.numeroRegistrazione = numeroRegistrazione;
        this.dataRegistrazione = dataRegistrazione;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPaId() {
        return paId;
    }

    public void setPaId(String paId) {
        this.paId = paId;
    }

    public String getLottoId() {
        return lottoId;
    }

    public void setLottoId(String lottoId) {
        this.lottoId = lottoId;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getNumeroDocumenti() {
        return numeroDocumenti;
    }

    public void setNumeroDocumenti(Integer numeroDocumenti) {
        this.numeroDocumenti = numeroDocumenti;
    }

    public String getFirmatario() {
        return firmatario;
    }

    public void setFirmatario(String firmatario) {
        this.firmatario = firmatario;
    }

    public String getInputBasePath() {
        return inputBasePath;
    }

    public void setInputBasePath(String inputBasePath) {
        this.inputBasePath = inputBasePath;
    }

    public String getOutputBasePath() {
        return outputBasePath;
    }

    public void setOutputBasePath(String outputBasePath) {
        this.outputBasePath = outputBasePath;
    }

    public String getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(String documentoId) {
        this.documentoId = documentoId;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getInputFileName() {
        return inputFileName;
    }

    public void setInputFileName(String inputFileName) {
        this.inputFileName = inputFileName;
    }

    public String getInputFileSha256() {
        return inputFileSha256;
    }

    public void setInputFileSha256(String inputFileSha256) {
        this.inputFileSha256 = inputFileSha256;
    }

    public String getInputFileMd5() {
        return inputFileMd5;
    }

    public void setInputFileMd5(String inputFileMd5) {
        this.inputFileMd5 = inputFileMd5;
    }

    public String getInputFileMimeType() {
        return inputFileMimeType;
    }

    public void setInputFileMimeType(String inputFileMimeType) {
        this.inputFileMimeType = inputFileMimeType;
    }

    public String getInputFileRepoId() {
        return inputFileRepoId;
    }

    public void setInputFileRepoId(String inputFileRepoId) {
        this.inputFileRepoId = inputFileRepoId;
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }

    public String getOutputFileSha256() {
        return outputFileSha256;
    }

    public void setOutputFileSha256(String outputFileSha256) {
        this.outputFileSha256 = outputFileSha256;
    }

    public String getOutputFileMd5() {
        return outputFileMd5;
    }

    public void setOutputFileMd5(String outputFileMd5) {
        this.outputFileMd5 = outputFileMd5;
    }

    public String getOutputFileMimeType() {
        return outputFileMimeType;
    }

    public void setOutputFileMimeType(String outputFileMimeType) {
        this.outputFileMimeType = outputFileMimeType;
    }

    public String getOutputFileRepoId() {
        return outputFileRepoId;
    }

    public void setOutputFileRepoId(String outputFileRepoId) {
        this.outputFileRepoId = outputFileRepoId;
    }

    public String getIdEsterno() {
        return idEsterno;
    }

    public void setIdEsterno(String idEsterno) {
        this.idEsterno = idEsterno;
    }

    public String getNumeroRegistrazione() {
        return numeroRegistrazione;
    }

    public void setNumeroRegistrazione(String numeroRegistrazione) {
        this.numeroRegistrazione = numeroRegistrazione;
    }

    public ZonedDateTime getDataRegistrazione() {
        return dataRegistrazione;
    }

    public void setDataRegistrazione(ZonedDateTime dataRegistrazione) {
        this.dataRegistrazione = dataRegistrazione;
    }

    public DocumentiLottoStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentiLottoStatus status) {
        this.status = status;
    }

    public ZonedDateTime getInsertTs() {
        return insertTs;
    }

    public void setInsertTs(ZonedDateTime insertTs) {
        this.insertTs = insertTs;
    }
    
    @JsonIgnore
    public Builder getBuilder() {
        return new Builder(this);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.id);
        hash = 29 * hash + Objects.hashCode(this.paId);
        hash = 29 * hash + Objects.hashCode(this.lottoId);
        hash = 29 * hash + Objects.hashCode(this.timestamp);
        hash = 29 * hash + Objects.hashCode(this.numeroDocumenti);
        hash = 29 * hash + Objects.hashCode(this.firmatario);
        hash = 29 * hash + Objects.hashCode(this.documentoId);
        hash = 29 * hash + Objects.hashCode(this.bucket);
        hash = 29 * hash + Objects.hashCode(this.inputBasePath);
        hash = 29 * hash + Objects.hashCode(this.inputFileName);
        hash = 29 * hash + Objects.hashCode(this.inputFileSha256);
        hash = 29 * hash + Objects.hashCode(this.inputFileMd5);
        hash = 29 * hash + Objects.hashCode(this.inputFileMimeType);
        hash = 29 * hash + Objects.hashCode(this.inputFileRepoId);
        hash = 29 * hash + Objects.hashCode(this.outputBasePath);
        hash = 29 * hash + Objects.hashCode(this.outputFileName);
        hash = 29 * hash + Objects.hashCode(this.outputFileSha256);
        hash = 29 * hash + Objects.hashCode(this.outputFileMd5);
        hash = 29 * hash + Objects.hashCode(this.outputFileMimeType);
        hash = 29 * hash + Objects.hashCode(this.outputFileRepoId);
        hash = 29 * hash + Objects.hashCode(this.idEsterno);
        hash = 29 * hash + Objects.hashCode(this.numeroRegistrazione);
        hash = 29 * hash + Objects.hashCode(this.dataRegistrazione);
        hash = 29 * hash + Objects.hashCode(this.insertTs);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DocumentoLottoEntity other = (DocumentoLottoEntity) obj;
        if (!Objects.equals(this.paId, other.paId)) {
            return false;
        }
        if (!Objects.equals(this.lottoId, other.lottoId)) {
            return false;
        }
        if (!Objects.equals(this.firmatario, other.firmatario)) {
            return false;
        }
        if (!Objects.equals(this.documentoId, other.documentoId)) {
            return false;
        }
        if (!Objects.equals(this.bucket, other.bucket)) {
            return false;
        }
        if (!Objects.equals(this.inputBasePath, other.inputBasePath)) {
            return false;
        }
        if (!Objects.equals(this.inputFileName, other.inputFileName)) {
            return false;
        }
        if (!Objects.equals(this.inputFileSha256, other.inputFileSha256)) {
            return false;
        }
        if (!Objects.equals(this.inputFileMd5, other.inputFileMd5)) {
            return false;
        }
        if (!Objects.equals(this.inputFileMimeType, other.inputFileMimeType)) {
            return false;
        }
        if (!Objects.equals(this.inputFileRepoId, other.inputFileRepoId)) {
            return false;
        }
        if (!Objects.equals(this.outputBasePath, other.outputBasePath)) {
            return false;
        }
        if (!Objects.equals(this.outputFileName, other.outputFileName)) {
            return false;
        }
        if (!Objects.equals(this.outputFileSha256, other.outputFileSha256)) {
            return false;
        }
        if (!Objects.equals(this.outputFileMd5, other.outputFileMd5)) {
            return false;
        }
        if (!Objects.equals(this.outputFileMimeType, other.outputFileMimeType)) {
            return false;
        }
        if (!Objects.equals(this.outputFileRepoId, other.outputFileRepoId)) {
            return false;
        }
        if (!Objects.equals(this.idEsterno, other.idEsterno)) {
            return false;
        }
        if (!Objects.equals(this.numeroRegistrazione, other.numeroRegistrazione)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.timestamp, other.timestamp)) {
            return false;
        }
        if (!Objects.equals(this.numeroDocumenti, other.numeroDocumenti)) {
            return false;
        }
        if (!Objects.equals(this.dataRegistrazione, other.dataRegistrazione)) {
            return false;
        }
        return Objects.equals(this.insertTs, other.insertTs);
    }


    @Override
    public String toString() {
        return getClass().getCanonicalName() + "[ id=" + id + " ]";
    }
    
    /*
    Builder
    */

    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        
        private final DocumentoLottoEntity entity;
        
        public Builder() {
            this.entity = new DocumentoLottoEntity();
        }
        
        private Builder(DocumentoLottoEntity entity) {
            this.entity = entity;
        }
        
        public Builder id(UUID id) {
            entity.setId(id);
            return this;
        }
        
        public Builder paId(String paId) {
            entity.setPaId(paId);
            return this;
        }
        
        public Builder lottoId(String lottoId) {
            entity.setLottoId(lottoId);
            return this;
        }
        
        public Builder timestamp(ZonedDateTime timestamp) {
            entity.setTimestamp(timestamp);
            return this;
        }
        
        public Builder numeroDocumenti(Integer numeroDocumenti) {
            entity.setNumeroDocumenti(numeroDocumenti);
            return this;
        }
        
        public Builder firmatario(String firmatario) {
            entity.setFirmatario(firmatario);
            return this;
        }
        
        public Builder documentoId(String documentoId) {
            entity.setDocumentoId(documentoId);
            return this;
        }
        
        public Builder bucket(String bucket) {
            entity.setBucket(bucket);
            return this;
        }
        
        // ---- input ----
        
        public Builder inputBasePath(String inputBasePath) {
            entity.setInputBasePath(inputBasePath);
            return this;
        }
        
        public Builder inputFileName(String inputFileName) {
            entity.setInputFileName(inputFileName);
            return this;
        }
        
        public Builder inputFileSha256(String inputFileSha256) {
            entity.setInputFileSha256(inputFileSha256);
            return this;
        }
        
        public Builder inputFileMd5(String inputFileMd5) {
            entity.setInputFileMd5(inputFileMd5);
            return this;
        }
        
        public Builder inputMimeType(String inputMimeType) {
            entity.setInputFileMimeType(inputMimeType);
            return this;
        }
        
        public Builder inputFileRepoId(String inputFileRepoId) {
            entity.setInputFileRepoId(inputFileRepoId);
            return this;
        }
        
        // ---- output ----
        
        public Builder outputBasePath(String outputBasePath) {
            entity.setOutputBasePath(outputBasePath);
            return this;
        }
        
        public Builder outputFileName(String outputFileName) {
            entity.setOutputFileName(outputFileName);
            return this;
        }
        
        public Builder outputFileSha256(String outputFileSha256) {
            entity.setOutputFileSha256(outputFileSha256);
            return this;
        }
        
        public Builder outputFileMd5(String outputFileMd5) {
            entity.setOutputFileMd5(outputFileMd5);
            return this;
        }
        
        public Builder outputFileMimeType(String outputFileMimeType) {
            entity.setOutputFileMimeType(outputFileMimeType);
            return this;
        }
        
        public Builder outputFileRepoId(String outputFileRepoId) {
            entity.setOutputFileRepoId(outputFileRepoId);
            return this;
        }
        
        // ---- registrazione ----
        
        public Builder idEsterno(String idEsterno) {
            entity.setIdEsterno(idEsterno);
            return this;
        }
        
        public Builder numeroRegistrazione(String numeroRegistrazione) {
            entity.setNumeroRegistrazione(numeroRegistrazione);
            return this;
        }
        
        public Builder dataRegistrazione(ZonedDateTime dataRegistrazione) {
            entity.setDataRegistrazione(dataRegistrazione);
            return this;
        }
        
        public Builder status(DocumentiLottoStatus status) {
            entity.setStatus(status);
            return this;
        }
        
        public Builder insertTs(ZonedDateTime insertTs) {
            entity.setInsertTs(insertTs);
            return this;
        }
        
        public DocumentoLottoEntity build() {
            return entity;
        }
    }    

}
