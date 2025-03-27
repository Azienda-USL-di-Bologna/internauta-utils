/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.model.entities.ribaltonedati.projections;

import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.lang.Object;
        
        /**
         *
         * @author Tommaso
         */
import java.util.List;

public class RibaltoneDatiUtils {

    @Autowired
    private EntityManager entityManager;

    public HashMap<String, Object> getOnlyRequestedSpecifiche(List<RibaltoneDataConfiguration.SpecificheNonSensibiliKeys> requestedSpecificheKeys, String idConfiguration) {
        RibaltoneDataConfiguration ribaltoneConf = entityManager.find(RibaltoneDataConfiguration.class, idConfiguration);
        HashMap<String, Object> specifiche = (HashMap<String, Object>) ribaltoneConf.getSpecifiche();
        HashMap<String, Object> specificheRicheste = new HashMap<>();
        for (RibaltoneDataConfiguration.SpecificheNonSensibiliKeys specificaRichiestaKey : requestedSpecificheKeys) {
            specificheRicheste.put(specificaRichiestaKey.toString(), specifiche.get(specificaRichiestaKey.toString()));
        }
        return specificheRicheste;
    }
}
