package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica;

import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.RecuperoRapportoDiVersamento;
import it.bologna.ausl.model.entities.versatore.RapportoDiVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author boria
 */
@Component
@Scope("prototype")
public class UnimaticaRecuperoRapportoDiVersamentoService extends RecuperoRapportoDiVersamento {

    private static final Logger log = LoggerFactory.getLogger(UnimaticaRecuperoRapportoDiVersamentoService.class);

    @Override
    public RapportoDiVersamento recuperaRapportiDiVersamento() throws VersatoreProcessingException {
        RapportoDiVersamento rapportoDiVersamento = new RapportoDiVersamento();
        Versamento versamento = entityManager.find(Versamento.class, 233712);
        rapportoDiVersamento.setIdVersamento(versamento);
        rapportoDiVersamento.setInConservazione(Boolean.FALSE);
        rapportoDiVersamento.setRapporto("male per Dio");
        log.info("rapporto di versamento: " + rapportoDiVersamento.getRapporto() + " con rapporto di presa in carico " + versamento.getRapporto());
        return rapportoDiVersamento;
    }

}
