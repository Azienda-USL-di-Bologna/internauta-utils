package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.recuperorapportodiversamento;

import it.bologna.ausl.internauta.utils.versatore.plugins.RecuperoRapportoDiVersamento;
import it.bologna.ausl.model.entities.versatore.RapportoDiVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe del thread che richiama il recupero del rapporto di versamento vero e proprio
 *
 * @author boria
 */
public class RecuperoRapportoDiVersamentoThread implements Callable<RapportoDiVersamento> {

    private static final Logger log = LoggerFactory.getLogger(RecuperoRapportoDiVersamentoThread.class);

    private final Versamento versamento;
    private final RecuperoRapportoDiVersamento recuperoRapportoDiVersamentoInstance;
    private final Map<String, Object> params;

    public RecuperoRapportoDiVersamentoThread(Versamento versamento, RecuperoRapportoDiVersamento recuperoRapportoDiVersamentoInstance, Map<String, Object> params) {
        this.versamento = versamento;
        this.recuperoRapportoDiVersamentoInstance = recuperoRapportoDiVersamentoInstance;
        this.params = params;
    }

    @Override
    public RapportoDiVersamento call() throws Exception {
        RapportoDiVersamento rapportoDiVersamento = new RapportoDiVersamento();
        if (versamento != null) {
            try {
                rapportoDiVersamento = recuperoRapportoDiVersamentoInstance.recuperaRapportiDiVersamento(versamento, params);
            } catch (Throwable ex) {
                log.error("errore nel recupero del rapporto di versamento", ex);
                rapportoDiVersamento.setIdVersamento(versamento);
                rapportoDiVersamento.setRapporto("Errore tecnico durante il processo di recupero del rapporto di versamento");
                rapportoDiVersamento.setStato(RapportoDiVersamento.StatoRapportoDiVersamento.ERRORE_INTERNO);
            }
        }
        return rapportoDiVersamento;
    }

}
