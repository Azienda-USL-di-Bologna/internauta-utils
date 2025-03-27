/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package it.bologna.ausl.model.entities.ribaltonedati.projections;

import it.bologna.ausl.model.entities.ribaltonedati.projections.generated.RibaltoneDataConfigurationWithPlainFields;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author Tommaso
 */
@Projection(name = "CustomRibaltoneDataConfigurationWithoutDatiSensibili", types = RibaltoneDataConfigurationWithPlainFields.class)
public interface CustomRibaltoneDataConfigurationWithoutDatiSensibili extends RibaltoneDataConfigurationWithPlainFields {

    @Override
    public String getId();

    @Override
    public String getFonte();

    @Value("#{@RibaltoneDatiUtils.getSpecificheNonSensibili(target)}")
    @Override
    public HashMap<String, Object> getSpecifiche();

}
