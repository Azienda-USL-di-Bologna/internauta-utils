package it.bologna.ausl.internauta.utils.send_integration.model;

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
        super(lotto.getPaId(), lotto.getLottoId(), lotto.getTimestamp(), lotto.getNumeroDocumenti(), lotto.getFirmatario(), lotto.getOutputBasePath(), lotto.getInputBasePath(), lotto.getDocumenti());
        this.documentiScaricati = documentiScaricati;
    }

    public List<DocumentoScaricato> getDocumentiScaricati() {
        return documentiScaricati;
    }

    public void setDocumentiScaricati(List<DocumentoScaricato> documentiScaricati) {
        this.documentiScaricati = documentiScaricati;
    }
}
