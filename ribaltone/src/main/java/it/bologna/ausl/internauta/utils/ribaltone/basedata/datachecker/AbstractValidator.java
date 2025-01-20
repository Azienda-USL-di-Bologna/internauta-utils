package it.bologna.ausl.internauta.utils.ribaltone.basedata.datachecker;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportareInterface;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Top
 */
public abstract class AbstractValidator {

    protected List<? extends DatiDaImportareInterface> datiDaImportare;

    protected AbstractValidator(List<? extends DatiDaImportareInterface> datiDaImportare) {
        this.datiDaImportare = datiDaImportare;
    }

    public Map<String, Integer> generateIndex() {
        Map<String, Integer> indexToImport = new HashMap<>();

        for (int i = 0; i < datiDaImportare.size(); i++) {
            indexToImport.put(datiDaImportare.get(i).getKey(), i);
        }
        return indexToImport;
    }

    public abstract <T extends DatiDaImportareInterface> List<T> validate();

}
