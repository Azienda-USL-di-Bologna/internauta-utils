package it.bologna.ausl.internauta.utils.ribaltone.validator;

import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 *
 * @author Top
 */
public class ValidatorAppartenenti extends AbstractValidator {

    Map<String, Integer> indexStrutture;
    Map<String, Integer> indexAppartenenti;

    public ValidatorAppartenenti(List<DatiDaImportareAppartenente> datiDaImportare, Map<String, Integer> indexStrutture, Map<String, Integer> indexAppartenenti) {
        super(datiDaImportare);
        this.indexStrutture = indexStrutture;
        this.indexAppartenenti = indexAppartenenti;
    }

    @Override
    public List<DatiDaImportareAppartenente> validate() {
        List<DatiDaImportareAppartenente> datiDaImportareAppartenenti = (List<DatiDaImportareAppartenente>) this.datiDaImportare;
        List<DatiDaImportareAppartenente> datiDaImportareAppartenentiValidi = new ArrayList<DatiDaImportareAppartenente>();
        List<DatiDaImportareAppartenente> appartenentiNonValidi = new ArrayList<DatiDaImportareAppartenente>();
        for (DatiDaImportareAppartenente datiDaImportareAppartenente : datiDaImportareAppartenenti) {
            boolean isValido = true;
            if (!StringUtils.hasText(datiDaImportareAppartenente.getCodiceAzienda())) {
                isValido = false;
            }
            if (!StringUtils.hasText(datiDaImportareAppartenente.getCodiceEnte())) {
                isValido = false;
            }
            if (!StringUtils.hasText(datiDaImportareAppartenente.getCodiceFiscale())) {
                isValido = false;
            }
            if (!StringUtils.hasText(datiDaImportareAppartenente.getCodiceMatricola())) {
                isValido = false;
            }
            if (!StringUtils.hasText(datiDaImportareAppartenente.getCognome())) {
                isValido = false;
            }
            if (!StringUtils.hasText(datiDaImportareAppartenente.getNome())) {
                isValido = false;
            }
            if (datiDaImportareAppartenente.getIdCasella() == null
                    || !indexStrutture.containsKey(datiDaImportareAppartenente.getIdCasella().toString())
                ) {
                isValido = false;
            }


            if (isValido) {
                datiDaImportareAppartenentiValidi.add(datiDaImportareAppartenente);
            } else {
                appartenentiNonValidi.add(datiDaImportareAppartenente);
            }
        }
        datiInvalidi = appartenentiNonValidi;
        return datiDaImportareAppartenentiValidi;
    }

}
