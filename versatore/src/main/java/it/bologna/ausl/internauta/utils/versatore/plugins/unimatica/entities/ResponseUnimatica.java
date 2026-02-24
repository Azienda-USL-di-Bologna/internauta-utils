package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.ZonedDateTime;
import java.util.List;

/**
 *
 * @author boria
 */
public class ResponseUnimatica {

    private String esitoComplessivo;
    private String pdvUuid;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ssXXX")
    private ZonedDateTime dataDiCarico;

    private List<ErroreUnimatica> erroriGeneraliList;
    private EsitoConsegnaUnimatica esitoConsegnaUnimatica;

    public String getEsitoComplessivo() {
        return esitoComplessivo;
    }

    public void setEsitoComplessivo(String esitoComplessivo) {
        this.esitoComplessivo = esitoComplessivo;
    }

    public String getPdvUuid() {
        return pdvUuid;
    }

    public void setPdvUuid(String pdvUuid) {
        this.pdvUuid = pdvUuid;
    }

    public ZonedDateTime getDataDiCarico() {
        return dataDiCarico;
    }

    public void setDataDiCarico(ZonedDateTime dataDiCarico) {
        this.dataDiCarico = dataDiCarico;
    }

    public List<ErroreUnimatica> getErroriGeneraliList() {
        return erroriGeneraliList;
    }

    public void setErroriGeneraliList(List<ErroreUnimatica> erroriGeneraliList) {
        this.erroriGeneraliList = erroriGeneraliList;
    }

    public EsitoConsegnaUnimatica getEsitoConsegnaUnimatica() {
        return esitoConsegnaUnimatica;
    }

    public void setEsitoConsegnaUnimatica(EsitoConsegnaUnimatica esitoConsegnaUnimatica) {
        this.esitoConsegnaUnimatica = esitoConsegnaUnimatica;
    }

    public String getResponseCode() {
        return erroriGeneraliList.get(0).getCodice();
    }

    public String getErrorMessage() {
        return erroriGeneraliList.get(0).getDescrizione();
    }

}
