package it.bologna.ausl.internauta.utils.sendintegration.model;

import java.util.List;

/**
 *
 * @author gdm
 */
public class LottoScaricato extends Lotto {
    private List<DocumentoScaricato> documentiScaricati;

    public LottoScaricato(List<DocumentoScaricato> documentiScaricati) {
        this.documentiScaricati = documentiScaricati;
    }
    
    public LottoScaricato(Lotto lotto, List<DocumentoScaricato> documentiScaricati) {
        super();
        this
            .paId(lotto.getPaId())
            .lottoId(lotto.getLottoId())
            .timestamp(lotto.getTimestamp())
            .numeroDocumenti(lotto.getNumeroDocumenti())
            .firmatario(lotto.getFirmatario())
            .outputBasePath(lotto.getOutputBasePath())
            .inputBasePath(lotto.getInputBasePath())
            .documenti(lotto.getDocumenti());
        this.documentiScaricati = documentiScaricati;
    }

    public List<DocumentoScaricato> getDocumentiScaricati() {
        return documentiScaricati;
    }

    public void setDocumentiScaricati(List<DocumentoScaricato> documentiScaricati) {
        this.documentiScaricati = documentiScaricati;
    }
}
