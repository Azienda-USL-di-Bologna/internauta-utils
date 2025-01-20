package it.bologna.ausl.internauta.utils.ribaltone;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportare;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiDaImportareInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.datachecker.OperationsManager;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneConfiguration;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
public class RibaltoneTotaleManager {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private static RibaltoneConfiguration ribaltoneConfiguration;

    public void ribaltaWithOutUserReport(String codiceAzienda, String idConfiguration, boolean userReportRequired) throws RibaltoneHttpException {
        RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(entityManager, idConfiguration);
        DatiDaImportare validateSourceData = RibaltoneManagerUtils.validateSourceData(ribaltoneConfiguration.getObjectMapper(), codiceAzienda, ribaltoneConf);
        ribaltaTutto(validateSourceData);
    }
    
    private OperationsManager getOperationFromList (List<? extends DatiDaImportareInterface> listaDiDatiDaImportare){
         throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
            

      
            
            
    private void ribaltaTutto(DatiDaImportare datiDaImportareValidated) {
        OperationsManager operationsManager = new OperationsManager(datiDaImportareValidated);
        Operations operations = operationsManager.buildOperations();
        
//        
//        ribaltaStrutture(datiDaImportareValidated.getStruttureDaImportare());
//        RibaltoneManagerUtils.ribaltaAppartenenti(datiDaImportareValidated.getAppartenentiDaImportare(), datiDaImportareValidated.getAnagraficheDaImportare());
//        ribaltaTrasformazioni(datiDaImportareValidated.getStruttureDaImportare(), datiDaImportareValidated.getTrasformazioniDaImportare());
//        unificaStruttureDaUnificare();
    }

    private void ribaltaStrutture(Operations struttureCheckedData) {
        spegniStrutture(struttureCheckedData);
        accendiStrutture(struttureCheckedData);
    }

    private void ribaltaTrasformazioni(Operations struttureCheckedData, Operations trasformazioniCheckedData) {
        capisciTrasformazioniCapibili(struttureCheckedData, trasformazioniCheckedData);
        spostaStrutture(struttureCheckedData, trasformazioniCheckedData);
        spegniPermessiStruttureChiuse(struttureCheckedData);
    }

    private void spegniStrutture(Operations struttureCheckedData) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void accendiStrutture(Operations struttureCheckedData) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void capisciTrasformazioniCapibili(Operations struttureCheckedData, Operations trasformazioniCheckedData) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void spostaStrutture(Operations struttureCheckedData, Operations trasformazioniCheckedData) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void spegniPermessiStruttureChiuse(Operations struttureCheckedData) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void unificaStruttureDaUnificare() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
