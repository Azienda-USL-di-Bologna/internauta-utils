package it.bologna.ausl.internauta.utils.ribaltone.validator;

import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;

/**
 *
 * @author Top
 */
public class ValidatorAnagrafiche extends AbstractValidator{

    public ValidatorAnagrafiche(List<DatiDaImportareAnagrafica> datiDaImportare) {
        super(datiDaImportare);
    }

    @Override
    public List<DatiDaImportareAnagrafica> validate() {
        List<DatiDaImportareAnagrafica> datiPuliti = new ArrayList<DatiDaImportareAnagrafica>();
        List<DatiDaImportareAnagrafica> datiDaNonImportare = new ArrayList<DatiDaImportareAnagrafica>();
        List<? extends DatiRibaltoneInterface> datiDaImportareAnagrafiche = this.datiDaImportare;  
        for (DatiRibaltoneInterface datoDaImportare : datiDaImportareAnagrafiche) {
           DatiDaImportareAnagrafica anagrafica = (DatiDaImportareAnagrafica) datoDaImportare;
           boolean tuttoOk = true;
           if (!StringUtils.hasText(anagrafica.getCodiceAzienda())){
               tuttoOk = false;
           }
           if(!StringUtils.hasText(anagrafica.getCodiceEnte())){
            tuttoOk = false;
           }
           if(!StringUtils.hasText(anagrafica.getCodiceMatricola())){
            tuttoOk = false;
           }
           if(!StringUtils.hasText(anagrafica.getCodiceFiscale())){
            tuttoOk = false;
           }
           if(!StringUtils.hasText(anagrafica.getCognome())){
            tuttoOk = false;
           }
           if(!StringUtils.hasText(anagrafica.getNome())){
            tuttoOk = false;
           }
           if(!StringUtils.hasText(anagrafica.getEmail())){
           
           }
           if(!StringUtils.hasText(anagrafica.getPasswordHash())){
           
           }
           if (tuttoOk){
               datiPuliti.add(anagrafica);
           }else {
               datiDaNonImportare.add(anagrafica);
           }
           
        }
        datiInvalidi = datiDaNonImportare;
        return datiPuliti;
    }

    


    



    
    
}
