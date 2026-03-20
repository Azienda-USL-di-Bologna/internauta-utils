package it.bologna.ausl.internauta.utils.parameters.manager;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanTemplate;
import com.querydsl.core.types.dsl.Expressions;
import it.bologna.ausl.internauta.utils.parameters.manager.repositories.ParametroAziendeRepository;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.configurazione.QParametroAziende;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Classe che contiene tutti i metodi per scrivere sui parametri_aziende.
 *
 *
 * @author tom
 */
@Component
public class ParametriAziendeWriter {

    /**
     * TO DO: Popolare questo enum mano a mano che vengono utilizzati i vari
     * parametri, così da averli tutti raccolti in un punto.
     */
    public enum ParametriAzienda {
        ribaltoneConf,
    }

    @Autowired
    @Qualifier(value = "ParametroAziendeRepositoryParametersManager")
    private ParametroAziendeRepository parametroAziendeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public ParametriAziendeWriter() {
    }

    public void editParametersValue(String nuovoValore,String nome) {
         editParametersValue(nuovoValore,nome, null, null);
    }

    public void editParametersValue(String nuovoValore,ParametriAzienda nome) {
         editParametersValue(nuovoValore,nome.toString(), null, null);
    }

    public void editParametersValue(String nuovoValore,String nome, Integer[] idAziende) {
         editParametersValue(nuovoValore,nome, idAziende, null);
    }

    public void editParametersValue(String nuovoValore,ParametriAzienda nome, Integer[] idAziende) {
         editParametersValue(nuovoValore,nome.toString(), idAziende, null);
    }

    public void editParametersValue(String nuovoValore,String nome, String[] idApplicazioni) {
         editParametersValue(nuovoValore,nome, null, idApplicazioni);
    }

   

    /**
     * Metodo che scrive un nuovo valore nei parametri
     *
     * @param nuovoValore stringa che verrà inserita come nuovo valore del parametro
     * @param nome
     * @param idAziende
     * @param idApplicazioni e restituisce
     * @return List di ParametroAziende Se non si esplicitano le aziende o le
     * applicazioni, non viene applicato il filtro su quei due campi.
     */
    public  void editParametersValue(String nuovoValore, String nome, Integer[] idAziende, String[] idApplicazioni) {
        if (nuovoValore != null) {
            BooleanExpression filter = QParametroAziende.parametroAziende.nome.eq(nome);
            if (idAziende != null) {
                BooleanTemplate filterAzienda = Expressions.booleanTemplate("cast(tools.array_overlap({0}, tools.string_to_integer_array({1}, ',')) as boolean)=true",
                        QParametroAziende.parametroAziende.idAziende, org.apache.commons.lang3.StringUtils.join(idAziende, ","));
                filter = filter.and(filterAzienda.or(QParametroAziende.parametroAziende.idAziende.isNull()));
            }
            if (idApplicazioni != null) {
                BooleanTemplate filterApplicazioni = Expressions.booleanTemplate("cast(tools.array_overlap({0}, string_to_array({1}, ',')) as boolean)=true",
                        QParametroAziende.parametroAziende.idApplicazioni, org.apache.commons.lang3.StringUtils.join(idApplicazioni, ","));
                filter = filter.and(filterApplicazioni.or(QParametroAziende.parametroAziende.idApplicazioni.isNull()));
            }

            Iterable<ParametroAziende> parametriFound = parametroAziendeRepository.findAll(filter);

            List<ParametroAziende> res = new ArrayList();

            for (ParametroAziende par : parametriFound) {
                par.setValore(nuovoValore);
                    }
            
            parametroAziendeRepository.saveAll(parametriFound);
            
        }
    }

   
   
}
