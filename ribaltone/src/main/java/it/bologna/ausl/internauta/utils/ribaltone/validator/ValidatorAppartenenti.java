package it.bologna.ausl.internauta.utils.ribaltone.validator;

import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
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
    public List<DatiDaImportareAppartenente> validate(RepositoryFactory repositoryFactory) {
        List<DatiDaImportareAppartenente> datiDaImportareAppartenenti = (List<DatiDaImportareAppartenente>) this.datiDaImportare;
        List<DatiDaImportareAppartenente> datiDaImportareAppartenentiValidi = new ArrayList<DatiDaImportareAppartenente>();
        List<DatiDaImportareAppartenente> appartenentiNonValidi = new ArrayList<DatiDaImportareAppartenente>();
        for (DatiDaImportareAppartenente datiDaImportareAppartenente : datiDaImportareAppartenenti) {
            boolean isValido = true;
            String motivoInvalidita = "";
            if (!StringUtils.hasText(datiDaImportareAppartenente.getCodiceAzienda())) {
                isValido = false;
                motivoInvalidita = motivoInvalidita + "manca codice azienda; ";
            }
            if (!StringUtils.hasText(datiDaImportareAppartenente.getCodiceEnte())) {
                isValido = false;
                motivoInvalidita = motivoInvalidita + "manca codice ente; ";
            }
            if (!StringUtils.hasText(datiDaImportareAppartenente.getCodiceFiscale())) {
                isValido = false;
                motivoInvalidita = motivoInvalidita + "manca codice fiscale; ";
            }
            if (!StringUtils.hasText(datiDaImportareAppartenente.getCodiceMatricola())) {
                isValido = false;
                motivoInvalidita = motivoInvalidita + "manca matricola; ";
            }
            if (!StringUtils.hasText(datiDaImportareAppartenente.getCognome())) {
                isValido = false;
                motivoInvalidita = motivoInvalidita + "manca il cognome; ";
            }
            if (!StringUtils.hasText(datiDaImportareAppartenente.getNome())) {
                isValido = false;
                motivoInvalidita = motivoInvalidita + "manca il nome; ";
            }
            if (datiDaImportareAppartenente.getIdCasella() == null
                || !indexStrutture.containsKey(datiDaImportareAppartenente.getIdCasella().toString())) {
                isValido = false;
                motivoInvalidita = motivoInvalidita + "id casella dell'utente non trovato o inesistente; ";
            }
            datiDaImportareAppartenente.setErrore(motivoInvalidita);
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
