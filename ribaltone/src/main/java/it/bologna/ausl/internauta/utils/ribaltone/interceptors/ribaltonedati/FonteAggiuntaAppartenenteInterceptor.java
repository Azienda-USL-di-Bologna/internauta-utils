package it.bologna.ausl.internauta.utils.ribaltone.interceptors.ribaltonedati;

import it.bologna.ausl.internauta.utils.ribaltone.interceptors.RibaltoneBaseInterceptor;
import it.bologna.ausl.model.entities.ribaltonedati.FonteAggiuntaAppartenente;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
@NextSdrInterceptor(name = "fonteaggiuntaappartenente-interceptor")
public class FonteAggiuntaAppartenenteInterceptor extends RibaltoneBaseInterceptor {

    @Override
    public Class getTargetEntityClass() {
        return FonteAggiuntaAppartenente.class;
    }

}
