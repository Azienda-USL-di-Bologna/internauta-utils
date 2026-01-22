package it.bologna.ausl.model.entities.sendintegration;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hypersistence.utils.hibernate.type.range.Range;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 *
 * @author gdm
 */
@Entity
@Table(name = "api_key_store", catalog = "internauta", schema = "sendintegration")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "authorities"})
@Cacheable(false)
public class ApiKeyStoreEntry implements Serializable {
    
    public static enum Chiamante implements UserDetails {
        Lepida, Babel;

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return Arrays.asList(new SimpleGrantedAuthority(Lepida.toString()), new SimpleGrantedAuthority(Babel.toString()));
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public String getUsername() {
            return name();
        }
    };
    
    public static enum TipoChiamante {FRUITORE, EROGATORE};
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 250)
    @Enumerated(EnumType.STRING)
    @Column(name = "chiamante")
    private Chiamante chiamante;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 250)
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_chiamante")
    private TipoChiamante tipoChiamante;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 250)
    @Column(name = "api_key")
    private UUID apiKey;
    
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 250)
    @Column(name = "api_secret")
    private UUID apiSecret;
    
    @Basic(optional = false)
    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "data_inizio")
    private ZonedDateTime dataInizio;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "data_fine")
    private ZonedDateTime dataFine;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "intervallo", columnDefinition = "tstzrange")
    @ReadOnlyProperty
    private Range<ZonedDateTime> intervallo;
    
    @NotNull
    @Basic(optional = false)
    @Column(name = "secondi_validita")
    private Integer secondiValidita = 300;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'")
    @Column(name = "created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public ApiKeyStoreEntry() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Chiamante getChiamante() {
        return chiamante;
    }

    public void setChiamante(Chiamante chiamante) {
        this.chiamante = chiamante;
    }

    public TipoChiamante getTipoChiamante() {
        return tipoChiamante;
    }

    public void setTipoChiamante(TipoChiamante tipoChiamante) {
        this.tipoChiamante = tipoChiamante;
    }

    public UUID getApiKey() {
        return apiKey;
    }

    public void setApiKey(UUID apiKey) {
        this.apiKey = apiKey;
    }

    public UUID getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(UUID apiSecret) {
        this.apiSecret = apiSecret;
    }

    public ZonedDateTime getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(ZonedDateTime dataInizio) {
        this.dataInizio = dataInizio;
    }

    public ZonedDateTime getDataFine() {
        return dataFine;
    }

    public void setDataFine(ZonedDateTime dataFine) {
        this.dataFine = dataFine;
    }

    public Range<ZonedDateTime> getIntervallo() {
        return intervallo;
    }

    public void setIntervallo(Range<ZonedDateTime> intervallo) {
        this.intervallo = intervallo;
    }

    public Integer getSecondiValidita() {
        return secondiValidita;
    }

    public void setSecondiValidita(Integer secondiValidita) {
        this.secondiValidita = secondiValidita;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : super.hashCode());
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
        final ApiKeyStoreEntry other = (ApiKeyStoreEntry) obj;
        if (!Objects.equals(this.apiKey, other.apiKey)) {
            return false;
        }
        if (!Objects.equals(this.apiSecret, other.apiSecret)) {
            return false;
        }
        if (this.chiamante != other.chiamante) {
            return false;
        }
        if (this.tipoChiamante != other.tipoChiamante) {
            return false;
        }
        if (!Objects.equals(this.dataInizio, other.dataInizio)) {
            return false;
        }
        return Objects.equals(this.dataFine, other.dataFine);
    }
    
    @Override
    public String toString() {
        return getClass().getCanonicalName() + "[ id=" + id + " ]";
    }
}
