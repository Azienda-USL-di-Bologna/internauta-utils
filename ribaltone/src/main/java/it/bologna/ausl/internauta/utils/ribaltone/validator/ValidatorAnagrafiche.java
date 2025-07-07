package it.bologna.ausl.internauta.utils.ribaltone.validator;

import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
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
public class ValidatorAnagrafiche extends AbstractValidator {

    public ValidatorAnagrafiche(List<DatiDaImportareAnagrafica> datiDaImportare) {
        super(datiDaImportare);
    }

    @Override
    public List<DatiDaImportareAnagrafica> validate(RepositoryFactory repositoryFactory) {
        List<DatiDaImportareAnagrafica> datiPuliti = new ArrayList<DatiDaImportareAnagrafica>();
        List<DatiDaImportareAnagrafica> datiDaNonImportare = new ArrayList<DatiDaImportareAnagrafica>();
        List<? extends DatiRibaltoneInterface> datiDaImportareAnagrafiche = this.datiDaImportare;
        for (DatiRibaltoneInterface datoDaImportare : datiDaImportareAnagrafiche) {
            DatiDaImportareAnagrafica anagrafica = (DatiDaImportareAnagrafica) datoDaImportare;
            boolean tuttoOk = true;
            String motivoInvalidita = "";
            if (!StringUtils.hasText(anagrafica.getCodiceAzienda())) {
                tuttoOk = false;
                motivoInvalidita = motivoInvalidita + "manca codice azienda";
            }
            if (!StringUtils.hasText(anagrafica.getCodiceEnte())) {
                tuttoOk = false;
                motivoInvalidita = motivoInvalidita + "manca codice ente";
            }
            if (!StringUtils.hasText(anagrafica.getCodiceMatricola())) {
                tuttoOk = false;
                motivoInvalidita = motivoInvalidita + "manca codice matricola";
            }
            if (!StringUtils.hasText(anagrafica.getCodiceFiscale())) {
                tuttoOk = false;
                motivoInvalidita = motivoInvalidita + "manca codice fiscale";
            }
            if (!StringUtils.hasText(anagrafica.getCognome())) {
                tuttoOk = false;
                motivoInvalidita = motivoInvalidita + "manca codice cognome";
            }
            if (!StringUtils.hasText(anagrafica.getNome())) {
                tuttoOk = false;
                motivoInvalidita = motivoInvalidita + "manca codice nome";
            }
            if (!StringUtils.hasText(anagrafica.getEmail())) {

            }
            if (!StringUtils.hasText(anagrafica.getPasswordHash())) {

            }
            anagrafica.setErrore(motivoInvalidita);
            if (tuttoOk) {
                datiPuliti.add(anagrafica);
            } else {
                datiDaNonImportare.add(anagrafica);
            }

        }
        datiInvalidi = datiDaNonImportare;
        return datiPuliti;
    }

}
