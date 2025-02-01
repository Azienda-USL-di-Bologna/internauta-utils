package it.bologna.ausl.internauta.utils.ribaltone.validator;

import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.DatiImportatiStrutturaRepository;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;

/**
 *
 * @author Top
 */
public class ValidatorStrutture extends AbstractValidator {
    
    @Autowired
    private DatiImportatiStrutturaRepository datiImportatiStrutturaRepository;
    
    private Map<String, Integer> indexStrutture;
    public ValidatorStrutture(List<DatiDaImportareStruttura> datiDaImportare, Map<String, Integer> indexStrutture) {
        super(datiDaImportare);
        this.indexStrutture = indexStrutture;
    }

    @Override
    public List<DatiDaImportareStruttura> validate() throws RibaltoneHttpException {
        List<DatiDaImportareStruttura> struttureValide = new ArrayList<DatiDaImportareStruttura>();
        List<DatiDaImportareStruttura> struttureNonValide = new ArrayList<DatiDaImportareStruttura>();
        List<DatiDaImportareStruttura> datiDaImportareStrutture = (List<DatiDaImportareStruttura>) this.datiDaImportare;
        for (DatiDaImportareStruttura strutturaDaImportare : datiDaImportareStrutture) {
            String motivoInvalidita="";
            
            Boolean isValida = true;

            Integer nRadici = 0;
            //caso della radice
            if (strutturaDaImportare.getIdPadre() == null) {
                nRadici = nRadici + 1;
                if (nRadici > 1) {
                    throw new RibaltoneHttpException("piu di una radice trovata questo non puo accadere");
                }
                //caso della non radice che deve avere tutti gli antenati vivi
            } else {
                Integer antenatoMorto = getAntenatoMorto(indexStrutture, datiDaImportareStrutture, strutturaDaImportare);
                if (antenatoMorto != null) {
                    
                    isValida=false;
                    motivoInvalidita = "la struttura " + getDatiAntenatoMorto(antenatoMorto) + 
                            " è spenta e impedisce l'importazione di " + 
                            strutturaDaImportare.getIdCasella() + 
                            " " + strutturaDaImportare.getDescrizione() +
                            " si prega di correggere i dati alla fonte";
                }
            }

            if (!StringUtils.hasText(strutturaDaImportare.getDescrizione())) {
                isValida=false;
                motivoInvalidita = motivoInvalidita + " la struttura con chiave " + strutturaDaImportare.getKey() + " non ha la descrizione";
            }

            if (isValida) {
                struttureValide.add(strutturaDaImportare);
            }else{
                struttureNonValide.add(strutturaDaImportare);
                
            }

        }
        datiInvalidi=struttureNonValide;
        return struttureValide;
    }

    private Integer getAntenatoMorto(Map<String, Integer> indexStrutture, List<DatiDaImportareStruttura> datiDaImportareStrutture, DatiDaImportareStruttura datiDaImportareStruttura) {
        Integer idPadre = datiDaImportareStruttura.getIdPadre();
        boolean isRadice = idPadre == null || idPadre.equals(0);
        

        while (!isRadice && idPadre != null) {
            if (idPadre == null || idPadre.equals(0)) {
                isRadice = true;
            }
            if (!indexStrutture.containsKey(idPadre.toString())) {
                return idPadre;
            } else {
                idPadre = datiDaImportareStrutture.get(indexStrutture.get(idPadre.toString())).getIdPadre();
            }
        }
        return null;
    }

    private String getDatiAntenatoMorto(Integer antenatoMorto) {
        Optional<DatiImportatiStruttura> findById = datiImportatiStrutturaRepository.findById(antenatoMorto);
        if (findById.isPresent()){
            DatiImportatiStruttura strutturaMorta = findById.get();
            return " id casella " + strutturaMorta.getIdCasella() + " con nome " + strutturaMorta.getDescrizione();
        }
        return " non trovata anche nella precedente importazione";
    }

}
