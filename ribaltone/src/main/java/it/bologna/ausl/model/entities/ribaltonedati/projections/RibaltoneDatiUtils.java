/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.model.entities.ribaltonedati.projections;

import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
        
        /**
         *
         * @author Tommaso
         */
import org.springframework.stereotype.Component;

@Component
public class RibaltoneDatiUtils {

    @Autowired
    private EntityManager entityManager;

    public HashMap<String, Object> getOnlySpecificheNonSensibili(RibaltoneDataConfiguration dataConfiguration) {
        RibaltoneDataConfiguration ribaltoneConf = entityManager.find(RibaltoneDataConfiguration.class, dataConfiguration.getId());
        HashMap<String, Object> specifiche = (HashMap<String, Object>) ribaltoneConf.getSpecifiche();
        HashMap<String, Object> specificheRicheste = new HashMap<>();
        for (RibaltoneDataConfiguration.SpecificheNonSensibiliKeys specificaRichiestaKey : RibaltoneDataConfiguration.SpecificheNonSensibiliKeys.values()) {
            specificheRicheste.put(specificaRichiestaKey.toString(), specifiche.get(specificaRichiestaKey.toString()));
        }
        return specificheRicheste;
    }
}
