package it.bologna.ausl.model.entities.ribaltonedati.projections;

import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import it.bologna.ausl.model.entities.ribaltonedati.projections.generated.RibaltoneDataConfigurationWithPlainFields;
import it.bologna.ausl.model.entities.ribaltonedati.projections.RibaltoneDatiUtils;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author tommaso
 */
@Projection(name = "CustomRibaltoneDataConfigurationWithoutDatiSensibili", types = {RibaltoneDataConfiguration.class})
public interface CustomRibaltoneDataConfigurationWithoutDatiSensibili extends RibaltoneDataConfigurationWithPlainFields {

    @Override
    @Value("#{@ribaltoneDatiUtils.getOnlyRequestedSpecifiche(target)}")
    public HashMap<String, Object> getSpecifiche();

}
