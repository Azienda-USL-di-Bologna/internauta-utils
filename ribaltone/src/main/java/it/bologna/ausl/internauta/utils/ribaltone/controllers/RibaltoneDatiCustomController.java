package it.bologna.ausl.internauta.utils.ribaltone.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeWriter;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RibaltoneDataConfigurationRepository;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.configurazione.data.ConfigRibaltoneView;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration.SpecificheNonSensibiliKeys;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "${ribaltonedati.mapping.url.root}")
public class RibaltoneDatiCustomController implements ControllerHandledExceptions {

    @Autowired
    private ParametriAziendeWriter parametriWriter;

    @Autowired
    private ParametriAziendeReader parametriReader;

    @Autowired
    private RibaltoneDataConfigurationRepository ribaltoneDataConfigurationRepository;

    /*
     *Servlet che si occupa di salvare i dati di configuraione del ribaltone smistando i dati su più entità: parametroAziende e ribaltoneDataConfiguration
     */
    @RequestMapping(value = "updateRibaltoneConf", method = RequestMethod.POST)
    public ResponseEntity<?> updateRibaltoneConf(
        @RequestBody String configRibaltoneViewDaSalvareString,
        @RequestParam Integer idAzienda,
        HttpServletRequest request) throws IOException {
        Integer[] idAziende = new Integer[]{idAzienda};
        ObjectMapper om = new ObjectMapper();
        ConfigRibaltoneView configRibaltoneViewDaSalvareObject = om.readValue(configRibaltoneViewDaSalvareString, ConfigRibaltoneView.class);

        //salvo prima la parte di dati che va nel parametro azienda. Quindi tutto tranne i codici enti
        List<ParametroAziende> par = parametriReader.getParameters(ParametriAziendeReader.ParametriAzienda.ribaltoneConf.toString(), idAziende);
        Map<String, Object> map = parametriReader.getValue(par.get(0), new TypeReference<Map<String, Object>>() {
        });
        map.put(ConfigRibaltoneView.ConfigKeys.fonteDefault.toString(), configRibaltoneViewDaSalvareObject.getFonteDefault());
        map.put(ConfigRibaltoneView.ConfigKeys.fonti.toString(), configRibaltoneViewDaSalvareObject.getFonti());
        map.put(ConfigRibaltoneView.ConfigKeys.attivo.toString(), configRibaltoneViewDaSalvareObject.isAttivo());
        map.put(ConfigRibaltoneView.ConfigKeys.mailDaNotificare.toString(), configRibaltoneViewDaSalvareObject.getMailDaNotificare());
        map.put(ConfigRibaltoneView.ConfigKeys.tolleranzaStrutture.toString(), configRibaltoneViewDaSalvareObject.getTolleranzaStrutture());
        map.put(ConfigRibaltoneView.ConfigKeys.idPersoneDaNotificare.toString(), configRibaltoneViewDaSalvareObject.getIdPersoneDaNotificare());
        map.put(ConfigRibaltoneView.ConfigKeys.tolleranzaAnagrafica.toString(), configRibaltoneViewDaSalvareObject.getTolleranzaAnagrafica());
        map.put(ConfigRibaltoneView.ConfigKeys.tolleranzaAppartenenti.toString(), configRibaltoneViewDaSalvareObject.getTolleranzaAppartenenti());
        map.put(ConfigRibaltoneView.ConfigKeys.tolleranzaTrasformazioni.toString(), configRibaltoneViewDaSalvareObject.getTolleranzaTrasformazioni());

        try {
            parametriWriter.editParametersValue(om.writeValueAsString(map), ParametriAziendeWriter.ParametriAzienda.ribaltoneConf, idAziende);
        } catch (Exception e) {
            return new ResponseEntity("Errore mentre si tentava di aggiornare il valore del parametro azienda 'ribaltoneConf'", HttpStatus.CONFLICT);
        }

        //salvo la seconda parte che va su tabella ribaltone.configuration, ovvero i codici enti che vanno inseriti all'interno del json contenuto in 'specifiche'
        RibaltoneDataConfiguration r = ribaltoneDataConfigurationRepository.getReferenceById(configRibaltoneViewDaSalvareObject.getFonteDefault().toString());
        HashMap<String, Object> specifiche = (HashMap<String, Object>) r.getSpecifiche();
        specifiche.put(SpecificheNonSensibiliKeys.codiciEntiValidi.toString(), configRibaltoneViewDaSalvareObject.getCodiciEntiValidi());
        try {
            RibaltoneDataConfiguration ribaltoneDataConfigurationSaved = ribaltoneDataConfigurationRepository.save(r);
        } catch (Exception e) {
            return new ResponseEntity("Errore mentre si tentava di salvare ribaltoneDataConfiguration con i nuovi codici enti", HttpStatus.CONFLICT);
        }

        return new ResponseEntity(om.writeValueAsString(configRibaltoneViewDaSalvareObject), HttpStatus.OK);
    }

    private static class TypeReferenceImpl extends TypeReference<Map<String, Object>> {

        public TypeReferenceImpl() {
        }
    }

}
