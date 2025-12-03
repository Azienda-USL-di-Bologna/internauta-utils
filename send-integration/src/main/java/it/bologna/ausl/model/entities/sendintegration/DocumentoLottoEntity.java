package it.bologna.ausl.model.entities.sendintegration;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    @Column(name = "input_base_path", columnDefinition = "text")
    private String inputBasePath;
    
    @NotNull
    @Basic(optional = false)
    @Column(name = "output_base_path", columnDefinition = "text")
    private String outputBasePath;
    
    @NotNull
    @Basic(optional = false)
    @Column(name = "documento_id", columnDefinition = "text")    
    private String documentoId;
    
    @NotNull
    @Basic(optional = false)
    @Column(name = "input_file_name", columnDefinition = "text")    
    private String inputFileName;
    
    @NotNull
    @Basic(optional = false)
    @Column(name = "input_file_hash", columnDefinition = "text")    
    private String inputFileHash;
    
    @NotNull
    @Basic(optional = false)
    @Column(name = "repo_file_id", columnDefinition = "text")    
    private String repoFileId;
    
    @NotNull
    @Basic(optional = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "insert_ts")
    private ZonedDateTime insertTs = ZonedDateTime.now();

    public DocumentoLottoEntity() {
    }

    public DocumentoLottoEntity( String paId, String lottoId, ZonedDateTime timestamp, Integer numeroDocumenti, String firmatario, String inputBasePath, String outputBasePath, String documentoId, String inputFileName, String inputFileHash, String repoFileId) {
        this.paId = paId;
        this.lottoId = lottoId;
        this.timestamp = timestamp;
        this.numeroDocumenti = numeroDocumenti;
        this.firmatario = firmatario;
        this.inputBasePath = inputBasePath;
        this.outputBasePath = outputBasePath;
        this.documentoId = documentoId;
        this.inputFileName = inputFileName;
        this.inputFileHash = inputFileHash;
        this.repoFileId = repoFileId;
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

    public String getInputFileName() {
        return inputFileName;
    }

    public void setInputFileName(String inputFileName) {
        this.inputFileName = inputFileName;
    }

    public String getInputFileHash() {
        return inputFileHash;
    }

    public void setInputFileHash(String inputFileHash) {
        this.inputFileHash = inputFileHash;
    }

    public String getRepoFileId() {
        return repoFileId;
    }

    public void setRepoFileId(String repoFileId) {
        this.repoFileId = repoFileId;
    }

    public ZonedDateTime getInsertTs() {
        return insertTs;
    }

    public void setInsertTs(ZonedDateTime insertTs) {
        this.insertTs = insertTs;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.paId);
        hash = 89 * hash + Objects.hashCode(this.lottoId);
        hash = 89 * hash + Objects.hashCode(this.timestamp);
        hash = 89 * hash + Objects.hashCode(this.numeroDocumenti);
        hash = 89 * hash + Objects.hashCode(this.firmatario);
        hash = 89 * hash + Objects.hashCode(this.inputBasePath);
        hash = 89 * hash + Objects.hashCode(this.outputBasePath);
        hash = 89 * hash + Objects.hashCode(this.documentoId);
        hash = 89 * hash + Objects.hashCode(this.inputFileName);
        hash = 89 * hash + Objects.hashCode(this.inputFileHash);
        hash = 89 * hash + Objects.hashCode(this.insertTs);
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
        if (!Objects.equals(this.inputBasePath, other.inputBasePath)) {
            return false;
        }
        if (!Objects.equals(this.outputBasePath, other.outputBasePath)) {
            return false;
        }
        if (!Objects.equals(this.documentoId, other.documentoId)) {
            return false;
        }
        if (!Objects.equals(this.inputFileName, other.inputFileName)) {
            return false;
        }
        if (!Objects.equals(this.inputFileHash, other.inputFileHash)) {
            return false;
        }
        if (!Objects.equals(this.timestamp, other.timestamp)) {
            return false;
        }
        if (!Objects.equals(this.numeroDocumenti, other.numeroDocumenti)) {
            return false;
        }
        return Objects.equals(this.insertTs, other.insertTs);
    }

    @Override
    public String toString() {
        return getClass().getCanonicalName() + "[ id=" + id + " ]";
    }
}
