package it.bologna.ausl.internauta.utils.ribaltone.validator;

import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;

/**
 *
 * @author Top
 */
public class ValidatorTrasformazioni extends AbstractValidator {

    Map<String, Integer> indexStrutture;
    Map<String, Integer> indexTrasformazioni;
    Integer progressivoTrasformazione;

    public ValidatorTrasformazioni(List<DatiDaImportareTrasformazione> datiDaImportare, Map<String, Integer> indexStrutture, Map<String, Integer> indexTrasformazioni, Integer progressivoTrasformazione) {
        super(datiDaImportare);
        this.indexStrutture = indexStrutture;
        this.indexTrasformazioni = indexTrasformazioni;
        this.progressivoTrasformazione = progressivoTrasformazione;
    }

    public Integer getProgressivoTrasformazione() {
        return progressivoTrasformazione;
    }

    public void setProgressivoTrasformazione(Integer progressivoTrasformazione) {
        this.progressivoTrasformazione = progressivoTrasformazione;
    }

    @Override
    public List<DatiDaImportareTrasformazione> validate(RepositoryFactory repositoryFactory) {
        List<DatiDaImportareTrasformazione> datiDaImportareTrasformazioni = (List<DatiDaImportareTrasformazione>) this.datiDaImportare;
        List<DatiDaImportareTrasformazione> datiDaImportareTrasformazioniValide = new ArrayList<DatiDaImportareTrasformazione>();
        List<DatiDaImportareTrasformazione> trasformazioniNonValide = new ArrayList<DatiDaImportareTrasformazione>();
        for (DatiDaImportareTrasformazione datiDaImportareTrasformazione : datiDaImportareTrasformazioni) {
            boolean trasformazioneValida = true;
            if (datiDaImportareTrasformazione.getProgressivoRiga() == null || datiDaImportareTrasformazione.getProgressivoRiga() <= progressivoTrasformazione) {
                if (!StringUtils.hasText(datiDaImportareTrasformazione.getMotivo())) {
                    trasformazioneValida = false;
                    //non puo essere black
                } else {
                    if (datiDaImportareTrasformazione.getMotivo().equalsIgnoreCase("X")) {
                        if (indexStrutture.containsKey(datiDaImportareTrasformazione.getIdCasellaPartenza().toString())) {
                            trasformazioneValida = false;
                        }
                        if (!indexStrutture.containsKey(datiDaImportareTrasformazione.getIdCasellaArrivo().toString()) //la casella di arrivo deve essere valida
                                ) {
                            //confluenza non valida perche o sulla radice o in nessuna casella valida
                            trasformazioneValida = false;
                        }
                    }
                }
            }
            if (trasformazioneValida) {
                datiDaImportareTrasformazioniValide.add(datiDaImportareTrasformazione);
            } else {
                trasformazioniNonValide.add(datiDaImportareTrasformazione);
            }
        }
        datiInvalidi = trasformazioniNonValide;
        return datiDaImportareTrasformazioniValide;

    }

}
